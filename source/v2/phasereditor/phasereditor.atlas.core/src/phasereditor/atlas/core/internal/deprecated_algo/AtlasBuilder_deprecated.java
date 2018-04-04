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
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Image;

public class AtlasBuilder_deprecated implements AutoCloseable {
	private List<Rect> _input;
	private int _depth;

	public AtlasBuilder_deprecated() {
		_input = new ArrayList<>();
		_depth = 32;
	}

	public void addImage(Image img) {
		_input.add(new Rect(img));
	}

	public Atlas buildAtlas() {
		Collections.sort(_input, (a, b) -> {
			return -(a.area() - b.area());
		});

		AtlasList alist = new AtlasList();
		// int i = 0;
		for (Rect r : _input) {
			alist.combine(r);
			// if (i < 8) {
			// alist.reduce(Integer.MAX_VALUE);
			// } else if (i == 8) {
			// alist.reduce(1);
			// } else {
			// alist.reduce(_depth);
			// }
			alist.reduce(_depth);
			// i++;
		}
		alist.reduce();
		return alist.getList().get(0);
	}

	@Override
	public void close() throws Exception {
		for (Rect r : _input) {
			r.getImage().dispose();
		}
	}
}
