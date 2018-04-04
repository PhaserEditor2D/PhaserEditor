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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author arian
 *
 */
public class PGridSection extends ArrayList<PGridProperty<?>> {
	private static final long serialVersionUID = 1L;
	private String _name;
	private boolean _active;
	private Map<String, PGridProperty<?>> _map;

	public PGridSection(String name) {
		super();
		_name = name;
		_active = true;
		_map = new HashMap<>();
	}
	
	@Override
	public boolean add(PGridProperty<?> e) {
		e.setSection(this);
		_map.put(e.getName(), e);
		return super.add(e);
	}
	
	@Override
	public void add(int index, PGridProperty<?> e) {
		e.setSection(this);
		_map.put(e.getName(), e);
		super.add(index, e);
	}
	
	@Override
	public boolean addAll(Collection<? extends PGridProperty<?>> c) {
		for(PGridProperty<?> e : c) {
			e.setSection(this);
			_map.put(e.getName(), e);
		}
		return super.addAll(c);
	}

	public boolean isActive() {
		return _active;
	}

	public void setActive(boolean active) {
		_active = active;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	@SuppressWarnings("static-method")
	public boolean isReadOnly() {
		return false;
	}
}
