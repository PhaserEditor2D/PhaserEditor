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

import java.util.function.Supplier;

import org.eclipse.core.runtime.IAdaptable;

import phasereditor.atlas.core.AtlasFrame;
import phasereditor.ui.properties.PGridModel;
import phasereditor.ui.properties.PGridNumberProperty;
import phasereditor.ui.properties.PGridSection;
import phasereditor.ui.properties.PGridStringProperty;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class AtlasEditorFrame extends AtlasFrame implements IAdaptable {

	private PGridModel _gridModel;
	private String _regionFilename;
	private int _regionIndex;

	public AtlasEditorFrame(String regionFilename, int regionIndex) {
		_regionFilename = regionFilename;
		_regionIndex = regionIndex;
	}
	
	public int getRegionIndex() {
		return _regionIndex;
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

		section.add(createStringProperty("Name", this::getName));
		section.add(createNumberProperty("Frame X", this::getFrameX));
		section.add(createNumberProperty("Frame Y", this::getFrameY));
		section.add(createNumberProperty("Frame Width", this::getFrameW));
		section.add(createNumberProperty("Frame Height", this::getFrameH));

		section = new PGridSection("Sprite");
		model.getSections().add(section);

		section.add(createNumberProperty("Sprite X", this::getSpriteX));
		section.add(createNumberProperty("Sprite Y", this::getSpriteY));
		section.add(createNumberProperty("Sprite Width", this::getSpriteW));
		section.add(createNumberProperty("Sprite Height", this::getSpriteH));

		section = new PGridSection("Source");
		model.getSections().add(section);

		section.add(createNumberProperty("Source Width", this::getSourceW));
		section.add(createNumberProperty("Source Height", this::getSourceH));

		return model;
	}

	private static PGridNumberProperty createNumberProperty(String name, Supplier<Number> getter) {
		return new PGridNumberProperty(name, name, "") {

			@Override
			public boolean isModified() {
				return false;
			}

			@Override
			public boolean isReadOnly() {
				return true;
			}

			@Override
			public Double getValue() {
				return getter.get().doubleValue();
			}
		};
	}

	private static PGridStringProperty createStringProperty(String name, Supplier<String> getter) {
		return new PGridStringProperty(name, name, "") {

			@Override
			public boolean isModified() {
				return false;
			}

			@Override
			public boolean isReadOnly() {
				return true;
			}

			@Override
			public String getValue() {
				return getter.get();
			}

			@Override
			public void setValue(String value, boolean notify) {
				//
			}
		};
	}
}
