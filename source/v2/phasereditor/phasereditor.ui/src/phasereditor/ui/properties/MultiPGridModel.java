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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * This is a model used when the user selects multiple objects, so multiple
 * models can be edited through this one.
 * 
 * @author arian
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MultiPGridModel extends PGridModel {
	private List<PGridModel> _models;

	private ArrayList<PGridProperty> _properties;

	public MultiPGridModel(List<PGridModel> models) {
		_models = models;
		_properties = new ArrayList<>();

		var templateProperties = new PGridSection("Selection");

		for (var model : _models) {
			for (var section : model.getSections()) {
				for (var prop : section) {
					_properties.add(prop);

					if (templateProperties.isEmpty()) {

						templateProperties.add(prop);

					} else {
						var include = true;

						for (var templateProp : templateProperties) {
							if (prop.matches(templateProp)) {
								include = false;
								break;
							}
						}

						if (include) {
							templateProperties.add(prop);
						}
					}
				}
			}
		}

		getSections().add(templateProperties);
	}

	@Override
	public Object getPropertyValue(PGridProperty templateProperty) {

		var values = getPropertyValues(templateProperty);

		if (values.size() == 1) {
			return values.iterator().next();
		}

		try {
			return templateProperty.getDefaultValue();
		} catch (UnsupportedOperationException e) {
			return null;
		}
	}

	public Set<Object> getPropertyValues(PGridProperty templateProperty) {
		var set = new HashSet<>();

		for (var prop : _properties) {
			if (prop.matches(templateProperty)) {
				if (prop.getValue() != null) {
					set.add(prop.getValue());
				}
			}
		}
		return set;
	}

	@Override
	public void setPropertyValue(PGridProperty templateProperty, Object value, boolean notify) {
		for (var prop : _properties) {
			if (prop.matches(templateProperty)) {
				prop.setValue(value, notify);
			}
		}
	}
}
