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
package phasereditor.atlas.ui.editors;

import java.util.ArrayList;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.atlas.ui.AtlasCanvas;
import phasereditor.ui.properties.PGridInfoProperty;
import phasereditor.ui.properties.PGridModel;
import phasereditor.ui.properties.PGridSection;

public class EditorPage extends ArrayList<AtlasEditorFrame> implements IAdaptable {
	private static final long serialVersionUID = 1L;
	private int _index;
	private AtlasGeneratorEditorModel _model;
	private PGridModel _gridModel;
	private AtlasGeneratorEditor _editor;

	public EditorPage(AtlasGeneratorEditor editor, int index) {
		super();
		_editor = editor;
		_model = editor.getModel();
		_index = index;
	}

	public int getIndex() {
		return _index;
	}

	public String getName() {
		return _model.getAtlasImageName(_index);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == PGridModel.class) {
			if (_gridModel == null) {
				_gridModel = createGridModel();
			}
			return _gridModel;
		}
		return null;
	}

	private PGridModel createGridModel() {
		PGridModel model = new PGridModel();

		PGridSection section = new PGridSection("tab");
		model.getSections().add(section);

		section.add(new PGridInfoProperty("name", this::getName));

		section = new PGridSection("texture");
		model.getSections().add(section);

		section.add(new PGridInfoProperty("size", () -> {
			AtlasCanvas canvas = _editor.getAtlasCanvas(_index);
			Image img = canvas.getImage();
			if (img == null) {
				return "";
			}
			Rectangle b = img.getBounds();
			return b.width + "," + b.height;
		}));

		section = new PGridSection("file");
		model.getSections().add(section);

		section.add(new PGridInfoProperty("filename", () -> _model.getAtlasImageName(getIndex())));

		return model;
	}
}