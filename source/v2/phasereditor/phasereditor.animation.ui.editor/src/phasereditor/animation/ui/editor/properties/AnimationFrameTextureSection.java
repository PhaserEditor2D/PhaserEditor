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
package phasereditor.animation.ui.editor.properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import phasereditor.animation.ui.editor.AnimationFrameModel_in_Editor;
import phasereditor.animation.ui.editor.AnimationsEditor;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.ui.preview.SingleFrameCanvas;
import phasereditor.inspect.core.InspectCore;

/**
 * @author arian
 *
 */
public class AnimationFrameTextureSection extends BaseAnimationSection<AnimationFrameModel_in_Editor> {

	private SingleFrameCanvas _frameCanvas;
	private Label _frameLabel;

	public AnimationFrameTextureSection(AnimationsEditor editor) {
		super(editor, "Texture");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AnimationFrameModel_in_Editor;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));

		{
			_frameCanvas = new SingleFrameCanvas(comp, SWT.BORDER);
			_frameCanvas.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationFrameConfig.frame"));
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.heightHint = 200;
			gd.minimumWidth = 200;
			_frameCanvas.setLayoutData(gd);
		}

		{
			_frameLabel = new Label(comp, SWT.WRAP);
			_frameLabel.setText("");
			_frameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		}

		update_UI_from_Model();

		return comp;
	}

	@Override
	public void update_UI_from_Model() {
		var models = getModels();

		var frame = (IAssetFrameModel) flatValues_to_Object(
				models.stream().map(model -> ((AnimationFrameModel) model).getFrameAsset()));

		if (frame == null) {
			_frameLabel.setText("Frame");
		} else {
			var sb = new StringBuilder();
			if (frame instanceof ImageAssetModel.Frame) {
				sb.append("key: " + frame.getKey());
			} else {
				sb.append("key: " + frame.getAsset().getKey() + "\nframe: " + frame.getKey());
			}

			var fd = frame.getFrameData();

			sb.append("\n");
			sb.append("size: " + fd.srcSize.x + "x" + fd.srcSize.y);
			sb.append("\n");

			IFile file = frame.getImageFile();

			if (file != null) {
				sb.append("file: " + file.getName());
			}

			_frameLabel.setText(sb.toString());
		}

		_frameCanvas.setModel(frame);
		_frameCanvas.resetZoom();

	}

}
