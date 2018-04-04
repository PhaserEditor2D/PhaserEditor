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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

public class Atlas {

	private List<Rect> _rects;

	private int _width;

	private int _height;

	private int _area;

	public Atlas() {
		_rects = new ArrayList<>();
	}

	public Atlas(Image img) {
		this();
		_rects.add(new Rect(img));
	}

	public Atlas(Atlas atlas) {
		_rects = new ArrayList<>();
		for (Rect r : atlas.getRects()) {
			_rects.add(new Rect(r));
		}
		updateBounds();
	}

	public List<Rect> getRects() {
		return _rects;
	}

	public int getWidth() {
		return _width;
	}

	public int getHeight() {
		return _height;
	}

	public int getArea() {
		return _area;
	}

	public void addRect(Rect rect) {
		_rects.add(rect);

		int ox = 0;
		int oy = 0;

		if (rect.x < 0) {
			ox = -rect.x;
		}

		if (rect.y < 0) {
			oy = -rect.y;
		}

		if (ox > 0 || oy > 0) {
			for (Rect r : _rects) {
				r.x += ox;
				r.y += oy;
			}
		}

		updateBounds();
	}

	private void updateBounds() {
		_width = width();
		_height = height();
		_area = _width * _height;
	}

	private int width() {
		if (_rects.isEmpty()) {
			return 0;
		}

		int minx = Integer.MAX_VALUE;
		int maxx = Integer.MIN_VALUE;

		for (Rect r : _rects) {
			minx = Math.min(minx, r.x);
			maxx = Math.max(maxx, r.x + r.width);
		}

		return maxx - minx;
	}

	private int height() {
		if (_rects.isEmpty()) {
			return 0;
		}

		int miny = Integer.MAX_VALUE;
		int maxy = Integer.MIN_VALUE;

		for (Rect r : _rects) {
			miny = Math.min(miny, r.y);
			maxy = Math.max(maxy, r.y + r.height);
		}

		return maxy - miny;
	}

	public boolean intersects(Rect rect) {
		for (Rect r : _rects) {
			if (r.intersects(rect)) {
				return true;
			}
		}
		return false;
	}

	public List<Atlas> combination(Rect rect) {
		List<Atlas> list = new ArrayList<>();

		for (int i = _rects.size() - 1; i >= 0; i--) {
			Rect r = _rects.get(i);
			for (Cell cell : r.cells) {
				Atlas test = new Atlas(this);
				Rect r2 = cell.place(r, rect);
				if (!overlaps(r2)) {
					test.addRect(r2);
					list.add(test);
				}
			}
		}

		return list;
	}

	private boolean overlaps(Rect test) {
		for (Rect r : _rects) {
			if (r.intersects(test)) {
				return true;
			}
		}

		return false;
	}

	public static int compareArea(Atlas a, Atlas b) {
		return a.getArea() - b.getArea();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Rect r : _rects) {
			sb.append(r.toString() + "\n");
		}
		return sb.toString();
	}

	public void paint(GC gc) {
		for (Rect r : _rects) {
			gc.drawImage(r.getImage(), r.x, r.y);
		}
	}

	public Image createImage(Device device) {
		Image img = new Image(device, getWidth(), getHeight());
		GC gc = new GC(img);
		paint(gc);
		gc.dispose();
		return img;
	}
}
