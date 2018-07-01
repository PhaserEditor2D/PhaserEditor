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
package phasereditor.ui.properties;

import java.util.function.Supplier;

/**
 * @author arian
 *
 */
public class PGridInfoProperty extends PGridStringProperty {

	private Supplier<? extends Object> _getter;

	public PGridInfoProperty(String controlId, String name, String tooltip, Supplier<? extends Object> getter) {
		super(controlId, name, tooltip);
		_getter = getter;
	}

	public PGridInfoProperty(String name, String tooltip, Supplier<String> getter) {
		super(name, name, tooltip);
		_getter = getter;
	}

	public PGridInfoProperty(String name, Supplier<String> getter) {
		super(name, name, name);
		_getter = getter;
	}

	@Override
	public String getValue() {
		Object value = _getter.get();
		return value == null ? "" : value.toString();
	}

	@Override
	public void setValue(String value, boolean notify) {
		//
	}

	@Override
	public boolean isModified() {
		return false;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

}
