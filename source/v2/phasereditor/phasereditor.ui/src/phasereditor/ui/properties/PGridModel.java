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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author arian
 *
 */
public class PGridModel {
	private List<PGridSection> _sections;
	private Map<String, Object> _extraData;

	public PGridModel() {
		_sections = new ArrayList<>();
		_extraData = new HashMap<>();
	}

	public Map<String, Object> getExtraData() {
		return _extraData;
	}

	public List<PGridSection> getSections() {
		return _sections;
	}

	public void addSection(String name, PGridProperty<?>... porperties) {
		_sections.add(new PGridSection(name, porperties));
	}

	public PGridProperty<?> findById(String id) {
		for (PGridSection section : _sections) {
			for (PGridProperty<?> prop : section) {
				if (prop.getName().equals(id)) {
					return prop;
				}
			}
		}
		return null;
	}

	@SuppressWarnings({ "static-method", "rawtypes", "unchecked" })
	public void setPropertyValue(PGridProperty prop, Object value, boolean notify) {
		prop.setValue(value, notify);
	}

	@SuppressWarnings({ "rawtypes", "static-method" })
	public Object getPropertyValue(PGridProperty prop) {
		return prop.getValue();
	}
	
	private PGridPage _propertyPage;

	public void setPropertyPage(PGridPage propertyPage) {
		_propertyPage = propertyPage;
	}

	public PGridPage getPropertyPage() {
		return _propertyPage;
	}

	protected void refreshGrid() {
		_propertyPage.getGrid().refresh();
	}

}
