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
package phasereditor.canvas.ui.editors.config;

import java.util.UUID;

import phasereditor.canvas.core.CanvasMainSettings;
import phasereditor.canvas.ui.editors.grid.PGridModel;

/**
 * @author arian
 *
 */
public abstract class ConfigItem {
	private String _name;
	private PGridModel _gridModel;
	private String _id;
	private CanvasMainSettings _settings;

	public ConfigItem(CanvasMainSettings settings, String name) {
		super();
		_name = name;
		_settings = settings;
		_gridModel = new PGridModel();
		_id = UUID.randomUUID().toString();
		buildProperties();
	}
	
	public String getId() {
		return _id;
	}
	
	public CanvasMainSettings getSettings() {
		return _settings;
	}
	
	public PGridModel getGridModel() {
		return _gridModel;
	}

	public String getName() {
		return _name;
	}
	
	public abstract void buildProperties();
}
