// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.ui.preview;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.bmpfont.core.BitmapFontModel;
import phasereditor.bmpfont.ui.BitmapFontCanvas;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;

public class BitmapFontAssetPreviewComp extends Composite {

	private BitmapFontAssetModel _model;
	BitmapFontCanvas _bitmapFontCanvas;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public BitmapFontAssetPreviewComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new FillLayout());

		_bitmapFontCanvas = new BitmapFontCanvas(this, SWT.NONE);

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// nothing
	}

	public BitmapFontCanvas getBitmapFontCanvas() {
		return _bitmapFontCanvas;
	}

	public void setModel(BitmapFontAssetModel model) {
		_model = model;

		if (model == null) {
			_bitmapFontCanvas.setImage(null);
			return;
		}

		IFile imgFile = model.getFileFromUrl(model.getTextureURL());
		BitmapFontModel fontModel = model.createFontModel();

		_bitmapFontCanvas.setModel(fontModel);
		_bitmapFontCanvas.setImageFile(imgFile);

	}

	public BitmapFontAssetModel getModel() {
		return _model;
	}

	public void createToolBar(IToolBarManager toolbar) {
		toolbar.add(new Action() {

			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_TEXT_ABC));
				setToolTipText("Set the demo text.");
			}

			@Override
			public void run() {

				InputDialog dlg = new InputDialog(getShell(), "BitmapFont Preview", "Write the text:",
						_bitmapFontCanvas.getText(),
						newText -> newText.trim().length() == 0 ? "Empty text not valid" : null);

				if (dlg.open() == Window.OK) {
					_bitmapFontCanvas.setText(dlg.getValue());
					_bitmapFontCanvas.resetZoom();
				}

			}
		});

		toolbar.add(new Separator());

		toolbar.add(new ImageCanvas_Zoom_1_1_Action(_bitmapFontCanvas));
		toolbar.add(new ImageCanvas_Zoom_FitWindow_Action(_bitmapFontCanvas));

	}

	public void setText(String text) {
		_bitmapFontCanvas.setText(text);
		_bitmapFontCanvas.redraw();
	}
}
