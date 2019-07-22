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

import static phasereditor.ui.PhaserEditorUI.scriptEngineEval;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Text;

/**
 * @author arian
 *
 */
public abstract class TextListener implements FocusListener, KeyListener {

	private String _initial;
	private Text _text;

	public TextListener(Text widget) {
		_text = widget;

		widget.addFocusListener(this);
		widget.addKeyListener(this);
	}

	@Override
	public void focusLost(FocusEvent e) {
		fireChanged();
	}

	private void fireChanged() {
		var value = _text.getText();

		var a = format(_initial);
		var b = format(value);

		if (!a.equals(b)) {

			accept(value);

			_initial = value;
		}
	}

	@Override
	public void focusGained(FocusEvent e) {
		_initial = _text.getText();
		e.display.asyncExec(_text::selectAll);
	}

	@SuppressWarnings("static-method")
	protected String format(String text) {
		return text;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		//
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.character == SWT.LF || e.character == SWT.CR || e.character == 13) {
			fireChanged();
		}
	}

	@SuppressWarnings("boxing")
	Number evalNumberExpression(String value) {
		Number result = null;

		try {
			result = Float.parseFloat(value);
		} catch (NumberFormatException e) {
			// it is not a number, let's try to evaluate it as JavaScript expression:
			try {
				result = scriptEngineEval(value);
				// ok, it is an expression, let's update the text field with result
				_text.setText(result.toString());
			} catch (Exception e1) {
				// nothing, it is not a valid expression
			}

		}
		return result;
	}

	protected abstract void accept(String value);
}
