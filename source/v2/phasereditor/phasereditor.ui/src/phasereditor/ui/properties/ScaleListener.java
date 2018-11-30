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

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Scale;

/**
 * @author arian
 *
 */
public abstract class ScaleListener extends MouseAdapter implements SelectionListener {

	private int _value;
	private int _initial;
	private Scale _scale;

	public ScaleListener(Scale scale) {
		super();
		_scale = scale;
		_scale.addSelectionListener(this);
		_scale.addMouseListener(this);
		_initial = -1;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		_value = ((Scale) e.widget).getSelection();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		_initial = ((Scale) e.widget).getSelection();
	}

	@Override
	public void mouseDown(MouseEvent e) {
		//
	}

	@Override
	public void mouseUp(MouseEvent e) {
		applyValue();
	}

	private void applyValue() {
		if (_value != _initial) {
			_initial = _value;
			accept((float) _value / 100);
		}
	}

	protected abstract void accept(float value);
}
