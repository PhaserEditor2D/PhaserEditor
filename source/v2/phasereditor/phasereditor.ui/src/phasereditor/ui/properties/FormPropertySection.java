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

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public abstract class FormPropertySection implements IEditorSharedImages {
	private Object[] _models;
	private String _name;

	public FormPropertySection(String name) {
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

	protected static String flatValues_to_String(Stream<?> values) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			var value = set.toArray()[0];

			if (value == null) {
				return "";
			}

			return value.toString();
		}

		return "";
	}

	protected static Object flatValues_to_Object(Stream<?> values) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			var value = set.toArray()[0];
			return value;
		}

		return null;
	}

	protected static Boolean flatValues_to_Boolean(Stream<Boolean> values) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			var value = set.toArray()[0];
			return (Boolean) value;
		}

		return null;
	}

	protected static boolean flatValues_to_boolean(Stream<Boolean> values) {
		Boolean value = flatValues_to_Boolean(values);
		return value != null && value.booleanValue();
	}

	@SuppressWarnings({ "boxing", "static-method" })
	protected void listen(Button check, Consumer<Boolean> listener) {

		var oldListener = check.getData("-prop-listener");
		if (oldListener != null) {
			check.removeSelectionListener((SelectionListener) oldListener);
		}

		var newListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listener.accept(check.getSelection());
			}
		};

		check.addSelectionListener(newListener);

		check.setData("-prop-listener", newListener);
	}

	@SuppressWarnings("static-method")
	protected void listen(Text text, Consumer<String> listener) {

		class TextListener implements FocusListener, KeyListener {
			private String _initial;

			@Override
			public void focusLost(FocusEvent e) {
				fireChanged();
			}

			private void fireChanged() {
				var value = text.getText();

				if (!value.equals(_initial)) {
					listener.accept(value);
					_initial = value;
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				_initial = text.getText();
				e.display.asyncExec(text::selectAll);
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
		}

		var oldListener = text.getData("-prop-listener");
		if (oldListener != null) {
			text.removeFocusListener((FocusListener) oldListener);
			text.removeKeyListener((KeyListener) oldListener);
		}

		var textListener = new TextListener();

		text.addFocusListener(textListener);
		text.addKeyListener(textListener);

		text.setData("-prop-listener", textListener);
	}

	@SuppressWarnings({ "boxing" })
	protected void listenFloat(Text text, Consumer<Float> listener) {
		listen(text, str -> {

			try {
				float value = Float.parseFloat(str);

				listener.accept(value);
			} catch (NumberFormatException e) {
				// noting
			}

		});
	}

	@SuppressWarnings({ "boxing" })
	protected void listenInt(Text text, Consumer<Integer> listener) {
		listen(text, str -> {

			try {
				var value = Integer.parseInt(str);

				listener.accept(value);
			} catch (NumberFormatException e) {
				// noting
			}

		});
	}

	@SuppressWarnings("unchecked")
	protected static <T> void setValues_to_Text(Text text, List<Object> models, Function<T, Object> get) {
		text.setText(flatValues_to_String(models.stream().map(model -> get.apply((T) model))));
	}

	public abstract boolean canEdit(Object obj);

	public abstract Control createContent(Composite parent);

	public abstract void update_UI_from_Model();
}
