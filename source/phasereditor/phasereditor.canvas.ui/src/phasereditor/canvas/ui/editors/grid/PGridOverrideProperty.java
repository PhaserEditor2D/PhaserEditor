// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
import java.util.List;

import phasereditor.canvas.core.BaseObjectModel;

/**
 * @author arian
 *
 */
public class PGridOverrideProperty extends PGridProperty<List<String>> {

	private List<String> _validProperties;
	private BaseObjectModel _model;

	public PGridOverrideProperty(BaseObjectModel model) {
		super(model.getId(), "override", "The properties to override in this prefab instance.");
		_model = model;
		_validProperties = new ArrayList<>();
	}

	public List<String> getValidProperties() {
		return _validProperties;
	}

	@Override
	public void setValue(List<String> value, boolean notify) {
		_model.setPrefabOverride(value);
	}

	@Override
	public List<String> getValue() {
		return _model.getPrefabOverride();
	}
	
	public BaseObjectModel getModel() {
		return _model;
	}
	
	public void setModel(BaseObjectModel model) {
		_model = model;
	}

	@Override
	public boolean isModified() {
		List<String> override = _model.getPrefabOverride();
		if (override.size() != 1 || !override.get(0).equals("position")) {
			return true;
		}
		return false;
	}
}
