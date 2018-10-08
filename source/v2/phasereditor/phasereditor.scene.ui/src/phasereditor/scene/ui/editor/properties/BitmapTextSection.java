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

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.ui.editor.undo.SceneSnapshotOperation;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvasDialog;
import phasereditor.ui.TreeCanvasViewer;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class BitmapTextSection extends ScenePropertySection {

	private Text _fontSizeText;
	private AlignAction _alignLeftAction;
	private AlignAction _alignMiddleAction;
	private AlignAction _alignRightAction;
	private Text _letterSpacingText;
	private Button _fontNameBtn;

	public BitmapTextSection(FormPropertyPage page) {
		super("Bitmap Text", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof BitmapTextComponent;
	}

	class AlignAction extends Action {
		private int _align;

		public AlignAction(String name, String icon, int align) {
			super(name, AS_CHECK_BOX);

			setImageDescriptor(EditorSharedImages.getImageDescriptor(icon));

			_align = align;
		}

		public int getAlign() {
			return _align;
		}

		@Override
		public void run() {
			var before = SceneSnapshotOperation.takeSnapshot(getEditor());

			var models = List.of(getModels());
			models.forEach(model -> {
				BitmapTextComponent.set_align((ObjectModel) model, _align);
			});

			var after = SceneSnapshotOperation.takeSnapshot(getEditor());

			getEditor().executeOperation(new SceneSnapshotOperation(before, after, "Change bitmap text align."));

			getEditor().setDirty(true);
			getEditor().getScene().redraw();

			update_UI_from_Model();

		}
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Align");

			var manager = new ToolBarManager();

			manager.add(_alignLeftAction = new AlignAction("ALIGN_LEFT", IMG_TEXT_ALIGN_LEFT,
					BitmapTextComponent.ALIGN_LEFT));
			manager.add(_alignMiddleAction = new AlignAction("ALIGN_MIDDLE", IMG_TEXT_ALIGN_CENTER,
					BitmapTextComponent.ALIGN_MIDDLE));
			manager.add(_alignRightAction = new AlignAction("ALIGN_RIGHT", IMG_TEXT_ALIGN_RIGHT,
					BitmapTextComponent.ALIGN_RIGHT));

			var toolbar = manager.createControl(comp);
			toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Font Name");

			_fontNameBtn = new Button(comp, SWT.LEFT);
			_fontNameBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			_fontNameBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::changeFont));
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Font Size");

			_fontSizeText = new Text(comp, SWT.BORDER);
			_fontSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Letter Spacing");

			_letterSpacingText = new Text(comp, SWT.BORDER);
			_letterSpacingText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		}

		update_UI_from_Model();

		return comp;
	}

	private void changeFont(SelectionEvent e) {
		var dlg = new QuickSelectFontDialog(e.display.getActiveShell());

		var editor = getEditor();

		var project = editor.getEditorInput().getFile().getProject();

		var packs = AssetPackCore.getAssetPackModels(project);

		dlg.setInput(packs.stream().flatMap(pack -> pack.getAssets().stream())
				.filter(asset -> asset instanceof BitmapFontAssetModel).toArray());

		if (dlg.open() == Window.OK) {

			var before = SceneSnapshotOperation.takeSnapshot(editor);

			var asset = (BitmapFontAssetModel) dlg.getResult();

			for (var obj : getModels()) {
				BitmapTextComponent.set_font((ObjectModel) obj, asset);
			}

			var after = SceneSnapshotOperation.takeSnapshot(editor);

			editor.executeOperation(new SceneSnapshotOperation(before, after, "Change bitmap text font."));

			editor.getScene().redraw();
			editor.setDirty(true);
		}
	}

	static class QuickSelectFontDialog extends TreeCanvasDialog {

		public QuickSelectFontDialog(Shell shell) {
			super(shell);
		}

		@Override
		protected TreeCanvasViewer createViewer(TreeCanvas tree) {
			var viewer = new AssetsTreeCanvasViewer(tree, new AssetsContentProvider(), AssetLabelProvider.GLOBAL_16);

			tree.addMouseListener(MouseListener.mouseDoubleClickAdapter(e -> {
				setResult(getViewer().getStructuredSelection().getFirstElement());
				close();
			}));

			return viewer;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			// no buttons
		}

	}

	@SuppressWarnings("boxing")
	@Override
	public void update_UI_from_Model() {
		var models = List.of(getModels());

		_fontSizeText.setText(flatValues_to_String(
				models.stream().map(model -> BitmapTextComponent.get_fontSize((ObjectModel) model))));

		_letterSpacingText.setText(flatValues_to_String(
				models.stream().map(model -> BitmapTextComponent.get_letterSpacing((ObjectModel) model))));

		_fontNameBtn.setText(flatValues_to_String(models.stream().map(model -> {
			var asset = BitmapTextComponent.get_font((ObjectModel) model);
			return asset == null ? "<null>" : asset.getKey();
		})));

		listenInt(_fontSizeText, value -> {

			models.stream().forEach(model -> BitmapTextComponent.set_fontSize((ObjectModel) model, value));

			getEditor().setDirty(true);

		}, models);

		listenFloat(_letterSpacingText, value -> {

			models.stream().forEach(model -> BitmapTextComponent.set_letterSpacing((ObjectModel) model, value));

			getEditor().setDirty(true);

		}, models);

		for (var action : new AlignAction[] { _alignLeftAction, _alignMiddleAction, _alignRightAction }) {
			action.setChecked(flatValues_to_boolean(models.stream()
					.map(model -> BitmapTextComponent.get_align((ObjectModel) model) == action.getAlign())));
		}

	}

}
