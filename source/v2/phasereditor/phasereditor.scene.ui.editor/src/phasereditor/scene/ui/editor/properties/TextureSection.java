// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.scene.ui.editor.properties;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.preview.SingleFrameCanvas;
import phasereditor.scene.core.TextureComponent;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class TextureSection extends ScenePropertySection {

	public TextureSection(ScenePropertyPage page) {
		super("Texture", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TextureComponent;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(new Action("Select All Objects With This Texture",
				EditorSharedImages.getImageDescriptor(IMG_SELECT_OBJECTS)) {
			@Override
			public void run() {

				var currentFrame = getFrame();

				if (currentFrame == null) {
					return;
				}

				var finder = getAssetFinder();

				var list = getSceneModel().getDisplayList().stream()

						.filter(TextureComponent::is)

						.filter(model -> {
							var frame = TextureComponent.utils_getTexture(model, finder);
							return frame == currentFrame;
						})

						.collect(toList());

				getEditor().setSelection(list);
				getEditor().updatePropertyPagesContentWithSelection();
			}
		});

		manager.add(new Action("Edit this texture key in the Asset Pack editor.",
				EditorSharedImages.getImageDescriptor(IMG_PACKAGE_GO)) {
			@Override
			public void run() {
				AssetPackUI.openElementInEditor(getFrame());
			}
		});
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));

		{
			var canvas = new SingleFrameCanvas(comp, SWT.BORDER);
			canvas.setToolTipText("Click to change the texture.");
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.heightHint = 100;
			gd.minimumWidth = 100;
			canvas.setLayoutData(gd);
			canvas.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					selectFrame();
				}
			});

			addUpdate(() -> {
				var frame = getFrame();
				canvas.setModel(frame);
				canvas.resetZoom();
			});
		}

		{
			Button btn = new Button(comp, 0);
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> selectFrame()));
			btn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Label label = new Label(comp, SWT.WRAP);
			label.setText("Frame");
			label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			addUpdate(() -> {

				var frame = getFrame();

				if (frame == null) {
					label.setText("Frame");
					btn.setText("Select Texture");
				} else {
					var sb = new StringBuilder();

					if (frame instanceof ImageAssetModel.Frame) {
						sb.append(frame.getKey());
					} else {
						sb.append(frame.getKey() + " @ " + frame.getAsset().getKey());
					}

					btn.setText(sb.toString());

					var fd = frame.getFrameData();

					sb.setLength(0);

					sb.append("size: " + fd.srcSize.x + "x" + fd.srcSize.y);

					IFile file = frame.getImageFile();

					if (file != null) {
						sb.append(" / file: " + file.getName());
					}

					label.setText(sb.toString());

				}
			});
		}

		return comp;
	}

	private IAssetFrameModel getFrame() {
		var models = getModels();

		var frame = (IAssetFrameModel) flatValues_to_Object(
				models.stream().map(model -> TextureComponent.utils_getTexture(model, getAssetFinder())));
		return frame;
	}

	private void selectFrame() {

		var list = new ArrayList<>();

		for (var pack : AssetPackCore.getAssetPackModels(getEditor().getProject())) {
			for (var asset : pack.getAssets()) {
				if (asset instanceof ImageAssetModel || asset instanceof AtlasAssetModel
						|| asset instanceof MultiAtlasAssetModel) {
					list.addAll(asset.getAllFrames());
				}
			}
		}

		var dlg = new QuickSelectAssetDialog(getEditor().getEditorSite().getShell());
		dlg.setTitle("Select Texture");

		dlg.setInput(list);

		if (dlg.open() == Window.OK) {
			var frame = (IAssetFrameModel) dlg.getSingleResult();

			wrapOperation(() -> {

				getModels().forEach(model -> {
					TextureComponent.utils_setTexture(model, frame);
				});

			}, true);

			getEditor().setDirty(true);

			update_UI_from_Model();
		}
	}

}
