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
package phasereditor.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * @author arian
 *
 */
public abstract class BackNextActions {
	private LinkedList<Object> _backList;
	private LinkedList<Object> _nextList;
	private Action _backAction;
	private Action _nextAction;
	private Object _currentObj;

	public BackNextActions() {

		_backList = new LinkedList<>();
		_nextList = new LinkedList<>();

		try {
			_backAction = new Action("Back", ImageDescriptor
					.createFromURL(new URL("platform:/plugin/org.eclipse.ui/icons/full/elcl16/backward_nav.png"))) {
				@Override
				public void run() {
					move(-1);
				}
			};

			_nextAction = new Action("Next", ImageDescriptor
					.createFromURL(new URL("platform:/plugin/org.eclipse.ui/icons/full/elcl16/forward_nav.png"))) {
				@Override
				public void run() {
					move(1);
				}
			};
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		updateEnableState();

	}

	protected void move(int dir) {
		if (dir == -1) {
			if (!_backList.isEmpty()) {
				var obj = _backList.removeLast();

				_nextList.addFirst(_currentObj);

				_currentObj = obj;

				display(obj);
			}
		} else {
			if (!_nextList.isEmpty()) {
				var obj = _nextList.removeFirst();

				_backList.addLast(_currentObj);

				_currentObj = obj;

				display(obj);
			}
		}

		updateEnableState();
	}

	private void updateEnableState() {
		_backAction.setEnabled(!_backList.isEmpty());
		_nextAction.setEnabled(!_nextList.isEmpty());
	}

	public Action getBackAction() {
		return _backAction;
	}

	public Action getNextAction() {
		return _nextAction;
	}

	public void showNewObject(Object obj) {

		if (_currentObj != null) {
			_backList.add(_currentObj);
		}

		_currentObj = obj;

		_nextList.clear();

		display(obj);

		updateEnableState();
	}

	protected abstract void display(Object obj);

}
