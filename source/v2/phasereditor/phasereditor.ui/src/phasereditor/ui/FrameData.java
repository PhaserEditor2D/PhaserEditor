// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * @author arian
 *
 */
public class FrameData implements Cloneable {
	public int index;
	public boolean visible = true;
	public Rectangle src;
	public Rectangle dst;
	public Point srcSize;

	public FrameData(int index) {
		this.index = index;
	}

	@Override
	public FrameData clone() {

		FrameData fd = new FrameData(index);
		fd.visible = visible;
		fd.src = new Rectangle(src.x, src.y, src.width, src.height);
		fd.dst = new Rectangle(dst.x, dst.y, dst.width, dst.height);
		fd.srcSize = new Point(srcSize.x, srcSize.y);

		return fd;
	}

	public static FrameData fromImage(Image img) {
		if (img == null) {
			return null;
		}
		
		var fd = new FrameData(0);

		fd.src = img.getBounds();
		fd.dst = img.getBounds();
		fd.srcSize = new Point(fd.src.width, fd.src.height);

		return fd;
	}
}
