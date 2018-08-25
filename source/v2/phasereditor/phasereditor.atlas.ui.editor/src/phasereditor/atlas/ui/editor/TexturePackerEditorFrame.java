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
package phasereditor.atlas.ui.editor;

import java.util.function.Supplier;

import org.eclipse.core.runtime.IAdaptable;

import phasereditor.atlas.core.AtlasFrame;
import phasereditor.ui.properties.PGridInfoProperty;
import phasereditor.ui.properties.PGridModel;
import phasereditor.ui.properties.PGridProperty;
import phasereditor.ui.properties.PGridSection;

/**
 * @author arian
 *
 */
public class TexturePackerEditorFrame extends AtlasFrame implements IAdaptable {

	private PGridModel _gridModel;
	private String _regionFilename;

	public TexturePackerEditorFrame(String regionFilename, int regionIndex) {
		super(regionIndex);
		_regionFilename = regionFilename;
	}
	
	public String getRegionFilename() {
		return _regionFilename;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
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

		PGridSection section = new PGridSection("Frame");
		model.getSections().add(section);

		section.add(createProperty("Name", this::getName));
		section.add(createProperty("Frame X", this::getFrameX));
		section.add(createProperty("Frame Y", this::getFrameY));
		section.add(createProperty("Frame Width", this::getFrameW));
		section.add(createProperty("Frame Height", this::getFrameH));

		section = new PGridSection("Sprite");
		model.getSections().add(section);

		section.add(createProperty("Sprite X", this::getSpriteX));
		section.add(createProperty("Sprite Y", this::getSpriteY));
		section.add(createProperty("Sprite Width", this::getSpriteW));
		section.add(createProperty("Sprite Height", this::getSpriteH));

		section = new PGridSection("Source");
		model.getSections().add(section);

		section.add(createProperty("Source Width", this::getSourceW));
		section.add(createProperty("Source Height", this::getSourceH));

		return model;
	}

	private static PGridProperty<?> createProperty(String name, Supplier<Object> getter) {
		return new PGridInfoProperty(name, getter);
	}
}
