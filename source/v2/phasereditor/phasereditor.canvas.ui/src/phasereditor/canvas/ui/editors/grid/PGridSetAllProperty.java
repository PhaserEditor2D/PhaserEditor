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

import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.GroupModel.SetAllData;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

/**
 * @author arian
 *
 */
public class PGridSetAllProperty extends PGridProperty<SetAllData>{

	private GroupModel _model;

	public PGridSetAllProperty(GroupModel model) {
		super(model.getId(), "setAll", PhaserJSDoc.getInstance().getMemberHelp("Phaser.Group.setAll"));
		_model = model;
	}

	@Override
	public SetAllData getValue() {
		return _model.getSetAll();
	}

	@Override
	public void setValue(SetAllData value, boolean notify) {
		_model.setSetAll(value);
	}

	@Override
	public boolean isModified() {
		return !_model.getSetAll().isEmpty();
	}
	
	@Override
	public boolean isReadOnly() {
		boolean b = _model.isPrefabReadOnly(GroupModel.PROPSET_SET_ALL);
		return b;
	}

}
