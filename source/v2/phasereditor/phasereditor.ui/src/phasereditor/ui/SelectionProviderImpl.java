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

import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * @author arian
 *
 */
public class SelectionProviderImpl implements ISelectionProvider {

	private ListenerList<ISelectionChangedListener> _listeners;
	private ISelection _selection;
	private boolean _autoFireSelectionChanged;

	public SelectionProviderImpl(boolean autoFireSelectionChanged) {
		_listeners = new ListenerList<>();
		_autoFireSelectionChanged = autoFireSelectionChanged;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_listeners.add(listener);
	}

	@Override
	public ISelection getSelection() {
		return _selection;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_listeners.remove(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		_selection = selection;
		if (_autoFireSelectionChanged) {
			fireSelectionChanged();
		}
	}

	public void setSelectionList(List<?> list) {
		setSelection(new StructuredSelection(list));
	}

	public void setSelectionObject(Object obj) {
		setSelectionList(List.of(obj));
	}

	public void fireSelectionChanged() {
		var e = new SelectionChangedEvent(this, _selection);
		for (var l : _listeners) {
			l.selectionChanged(e);
		}
	}
	
	public boolean isAutoFireSelectionChanged() {
		return _autoFireSelectionChanged;
	}
	
	public void setAutoFireSelectionChanged(boolean autoFireSelectionChanged) {
		_autoFireSelectionChanged = autoFireSelectionChanged;
	}

}
