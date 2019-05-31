// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * @author arian
 *
 */
public interface IEditorHugeToolbar {
	public void createContent(Composite parent);

	public class ActionButton {
		private IAction _action;
		private Button _btn;
		private boolean _showText;

		public ActionButton(Composite parent, IAction action) {
			this(parent, action, false);
		}

		public ActionButton(Composite parent, IAction action, boolean showText) {
			_showText = showText;
			var btnType = SWT.PUSH;
			switch (action.getStyle()) {
			case IAction.AS_CHECK_BOX:
				btnType = SWT.TOGGLE;
				break;
			default:
				break;
			}

			_btn = new Button(parent, btnType);
			_action = action;

			if (_action.getImageDescriptor() != null) {

				_btn.setImage(_action.getImageDescriptor().createImage());

				_btn.addDisposeListener(e -> _btn.getImage().dispose());
			}

			_action.addPropertyChangeListener(e -> {
				swtRun(ActionButton.this::updateButton);
			});

			_btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> _action.run()));

			updateButton();
		}

		private void updateButton() {
			if (_showText) {
				_btn.setText(_action.getText());
			} else {
				_btn.setText("");
			}
			_btn.setToolTipText(_action.getDescription());
			_btn.setEnabled(_action.isEnabled());
			_btn.setSelection(_action.isChecked());
		}

		public Button getButton() {
			return _btn;
		}

		public IAction getAction() {
			return _action;
		}

	}
}
