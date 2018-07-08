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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * @author arian
 *
 */
public abstract class PGridProperty<T> {
	private String _name;
	private String _tooltip;
	private String _nodeId;
	private PGridSection _section;

	public PGridProperty(String nodeId, String name, String tooltip) {
		super();
		_name = name;
		_nodeId = nodeId;
		_tooltip = tooltip;
	}

	public PGridSection getSection() {
		return _section;
	}

	public void setSection(PGridSection section) {
		_section = section;
	}

	@SuppressWarnings("static-method")
	public boolean isActive() {
		return true;
	}

	public boolean isReadOnly() {
		return getSection().isReadOnly();
	}

	public String getNodeId() {
		return _nodeId;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public abstract T getValue();

	public abstract void setValue(T value, boolean notify);

	public abstract boolean isModified();
	
	public T getDefaultValue() {
		throw new UnsupportedOperationException();
	}

	public String getTooltip() {
		return _tooltip;
	}

	public void setTooltip(String tooltip) {
		_tooltip = tooltip;
	}

	@SuppressWarnings({ "static-method", "unused" })
	public CellEditor createCellEditor(Composite parent, Object element) {
		return null;
	}
}
