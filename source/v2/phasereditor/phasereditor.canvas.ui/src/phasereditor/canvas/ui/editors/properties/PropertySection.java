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
package phasereditor.canvas.ui.editors.properties;

import java.util.HashSet;
import java.util.stream.Stream;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author arian
 *
 */
public abstract class PropertySection {
	private Object[] _models;
	private String _name;

	public PropertySection(String name) {
		_name = name;
	}
	
	public String getName() {
		return _name;
	}

	public Object[] getModels() {
		return _models;
	}

	public void setModels(Object[] models) {
		_models = models;
	}

	protected static String flatStringValues(Stream<String> values) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			return (String) set.toArray()[0];
		}

		return "";
	}

	public abstract boolean canEdit(Object obj);

	public abstract Control createContent(Composite parent);

}
