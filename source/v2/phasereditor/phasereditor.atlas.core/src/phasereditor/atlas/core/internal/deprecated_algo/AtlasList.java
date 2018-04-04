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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.graphics.Image;

public class AtlasList {
	private List<Atlas> _list;

	public AtlasList() {
		_list = new ArrayList<>();
	}

	public List<Atlas> getList() {
		return _list;
	}

	public void add(Atlas a) {
		_list.add(a);
	}

	public void combine(Rect r) {
		System.out.println("combine " + _list.size());
		Image img = r.getImage();
		if (_list.isEmpty()) {
			_list.add(new Atlas(img));
		} else {
			List<Atlas> list = new ArrayList<>();
			for (Atlas a : _list) {
				list.addAll(a.combination(new Rect(img)));
			}
			_list = list;
		}
	}

	/**
	 * Clear all atlas but not the one with the minimum area.
	 */
	public void reduce() {
		if (_list.isEmpty()) {
			return;
		}

		Optional<Atlas> min = _list.stream().min(Atlas::compareArea);
		_list = new ArrayList<>(Arrays.asList(min.get()));
	}

	public void reduce(int n) {
		Atlas[] atlas = _list.stream().sorted(Atlas::compareArea).limit(n)
				.toArray(Atlas[]::new);
		_list = new ArrayList<>(Arrays.asList(atlas));
	}
}
