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
public class ImageCanvas_Zoom_FitWindow_Action extends Action {
	private IZoomable _canvas;

	public ImageCanvas_Zoom_FitWindow_Action() {
		this(null);
	}

	public ImageCanvas_Zoom_FitWindow_Action(IZoomable canvas) {
		_canvas = canvas;
		setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_ARROW_OUT));
		setToolTipText("Zoom the image to fit the window.");
	}

	@Override
	public void run() {
		IZoomable canvas = getImageCanvas();
		canvas.resetZoom();
	}

	public IZoomable getImageCanvas() {
		return _canvas;
	}
}
