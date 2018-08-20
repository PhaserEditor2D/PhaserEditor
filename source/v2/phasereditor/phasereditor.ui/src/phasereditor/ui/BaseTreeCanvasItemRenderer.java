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

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Image;

import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public abstract class BaseTreeCanvasItemRenderer {

	public static final int ICON_AND_TEXT_SPACE = 5;

	protected final TreeCanvasItem _item;

	public BaseTreeCanvasItemRenderer(TreeCanvasItem item) {
		super();
		_item = item;
	}

	public abstract void render(PaintEvent e, int index, int x, int y);

	public abstract int computeRowHeight(TreeCanvas canvas);
	
	public abstract Image get_DND_Image();
	
	public abstract FrameData get_DND_Image_FrameData();
}
