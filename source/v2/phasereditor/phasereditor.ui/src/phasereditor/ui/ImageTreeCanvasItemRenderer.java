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
package phasereditor.ui;

import java.util.function.Supplier;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class ImageTreeCanvasItemRenderer extends BaseImageTreeCanvasItemRenderer {

	private Supplier<Image> _getImage;

	public ImageTreeCanvasItemRenderer(TreeCanvasItem item, Supplier<Image> getImage) {
		super(item);
		_getImage = getImage;
	}
	
	public ImageTreeCanvasItemRenderer(TreeCanvasItem item, Image image) {
		this(item, () -> image);
	}

	@Override
	protected void paintScaledInArea(GC gc, Rectangle area, boolean b) {
		var _image = _getImage.get();
		if (_image != null && !_image.isDisposed()) {
			PhaserEditorUI.paintScaledImageInArea(gc, _image, FrameData.fromImage(_image), area);
		}
	}

	@Override
	public ImageProxy get_DND_Image() {
		// TODO: hum! at the end, we should eliminate this class and always use an
		// ImageProxy. An ImageProxy that can be custom painted.
		return null;
	}

}
