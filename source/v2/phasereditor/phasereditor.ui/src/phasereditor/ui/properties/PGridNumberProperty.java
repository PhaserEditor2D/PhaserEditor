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
package phasereditor.ui.properties;

/**
 * @author arian
 *
 */
public abstract class PGridNumberProperty extends PGridProperty<Double> {
	private double _value;
	private boolean _isInteger;
	
	public PGridNumberProperty(String name) {
		this(name, name, name, false);
	}
	
	public PGridNumberProperty(String name, String tooltip) {
		this(name, name, tooltip, false);
	}
	
	public PGridNumberProperty(String name, boolean isInteger) {
		this(name, name, name, isInteger);
	}
	
	public PGridNumberProperty(String name, String tooltip, boolean isInteger) {
		this(name, name, tooltip, isInteger);
	}
	
	public PGridNumberProperty(String controlId, String name, String tootlip) {
		this(controlId, name, tootlip, false);
	}
	
	public PGridNumberProperty(String controlId, String name, String tootlip, boolean isInteger) {
		super(controlId, name, tootlip);
		_isInteger = isInteger;
	}

	@Override
	public Double getValue() {
		return Double.valueOf(_value);
	}

	@Override
	public void setValue(Double value, boolean notify) {
		_value = value.doubleValue();
	}
	
	public boolean isInteger() {
		return _isInteger;
	}
	
	@Override
	public Double getDefaultValue() {
		return Double.valueOf(0);
	}
}
