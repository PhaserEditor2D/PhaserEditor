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
package phasereditor.assetexplorer.ui.views;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arian
 *
 */
public class New {
	private Object _node;

	public static Object[] children(Object node, Object[] list) {
		return children(node, List.of(list));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object[] children(Object node, List list) {
		var list2 = new ArrayList(list);
		list2.add(0, new New(node));
		return list2.toArray();
	}

	public New(Object node) {
		super();
		_node = node;
	}

	/**
	 * A node like the {@link AssetExplorer.ATLAS_NODE}.
	 * 
	 * @return
	 */
	public Object getNode() {
		return _node;
	}
	
	@Override
	public String toString() {
		return "New...";
	}

}
