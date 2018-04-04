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
package phasereditor.atlas.core.internal.deprecated_algo;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

public class Rect implements Comparable<Rect> {
	public int x;
	public int y;
	public int width;
	public int height;
	public Cell[] cells;
	private final Image _image;

	public Rect(Image image) {
		_image = image;
		Rectangle b = image.getBounds();
		this.width = b.width;
		this.height = b.height;
		cells = generateCells();
	}

	public Rect(Image img, int x, int y) {
		this(img);
		this.x = x;
		this.y = y;
	}

	public Rect(Rect r) {
		this(r.getImage(), r.x, r.y);
	}

	public Image getImage() {
		return _image;
	}

	public int right() {
		return x + width;
	}

	public int bottom() {
		return y + height;
	}

	public int area() {
		return width * height;
	}

	public boolean intersects(Rect rect) {
		return rect == this
				|| intersects(rect.x, rect.y, rect.width, rect.height);
	}

	public boolean intersects(int aX, int aY, int aWidth, int aHeight) {
		return (aX < x + width) && (aY < y + height) && (aX + aWidth > x)
				&& (aY + aHeight > y);
	}

	@Override
	public String toString() {
		return "Rect [x=" + x + ", y=" + y + ", width=" + width + ", height="
				+ height + "]";
	}

	private static Cell[] generateCells() {
		return new Cell[] {
				// .7.8.
				// 6***1
				// 5***2
				// .4.3.

				// 1
				(b, p) -> new Rect(p.getImage(), b.right(), b.y),

				// // 2
				// (b, p) -> new Rect(p.getImage(), b.right(), b.bottom()
				// - p.height),

				// // 3
				// (b, p) -> new Rect(p.getImage(), b.right() - p.width,
				// b.bottom()),
				//
				// 4
				(b, p) -> new Rect(p.getImage(), b.x, b.bottom()),

		// // 5
		// (b, p) -> new Rect(p.getImage(), b.x - p.width, b.bottom()
		// - p.height),
		// //
		// // 6
		// (b, p) -> new Rect(p.getImage(), b.x - p.width, b.y),
		//
		// // 7
		// (b, p) -> new Rect(p.getImage(), b.x, b.y - p.height),
		//
		// // 8
		// (b, p) -> new Rect(p.getImage(), b.right() - p.width, b.y
		// - p.height),

		};
	}

	@Override
	public int compareTo(Rect o) {
		return width * height - o.width * o.height;
	}

}
