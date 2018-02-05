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
package phasereditor.canvas.ui.editors.grid;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * @author arian
 *
 */
public class NumberCellEditor extends TextCellEditor {

	private static ScriptEngine _engine;

	static {
		ScriptEngineManager m = new ScriptEngineManager();
		_engine = m.getEngineByName("nashorn");
	}

	public NumberCellEditor(Composite parent) {
		super(parent);
		setValidator(new ICellEditorValidator() {

			@Override
			public String isValid(Object value) {
				if (value instanceof String) {
					String script = (String) value;
					try {
						Double.parseDouble(script);
					} catch (NumberFormatException e) {
						// try a javascript expression
						try {
							@SuppressWarnings("synthetic-access")
							Object result = _engine.eval(script);
							if (!(result instanceof Number)) {
								return "Invalid expression result.";
							}
						} catch (ScriptException e1) {
							return "Invalid number or script format.";
						}
					}
				}
				return null;
			}
		});
	}

	@Override
	protected void doSetValue(Object value) {
		super.doSetValue(value == null ? null : value.toString());
	}

	@Override
	protected Object doGetValue() {
		String value = (String) super.doGetValue();
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException e) {
			try {
				Object result = _engine.eval(value);
				return Double.valueOf(((Number)result).doubleValue());
			} catch (ScriptException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

}
