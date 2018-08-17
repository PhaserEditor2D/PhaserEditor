// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.ui;

import org.eclipse.jface.action.Action;

/**
 * @author arian
 *
 */
public class ImageCanvas_Zoom_1_1_Action extends Action {

	private IZoomable _canvas;

	public ImageCanvas_Zoom_1_1_Action() {
		this(null);
	}

	public ImageCanvas_Zoom_1_1_Action(IZoomable canvas) {
		super("Reset Zoom (1:1)");
		_canvas = canvas;
		setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_ZOOM_RESTORE));
		setToolTipText("Reset the image to the original size.");
	}

	@Override
	public void run() {
		IZoomable canvas = getImageCanvas();
		canvas.setScale(1);
		canvas.setPanOffsetX(0);
		canvas.setOffsetY(0);
		canvas.redraw();
	}

	public IZoomable getImageCanvas() {
		return _canvas;
	}
}
