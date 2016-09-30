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
package phasereditor.canvas.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONObject;

import phasereditor.canvas.core.WorldModel;

/**
 * @author arian
 *
 */
public class CanvasEditorModel {
	private SceneSettings _settings;
	private WorldModel _world;

	public CanvasEditorModel(IFile file) {
		_settings = new SceneSettings();
		_world = new WorldModel(file);
	}

	public void read(JSONObject data) {
		_settings.read(data.getJSONObject("settings"));
		IStatus status = _world.getAssetTable().read(data.optJSONObject("asset-table"));

		if (!status.isOK()) {
			StatusManager.getManager().handle(status, StatusManager.BLOCK | StatusManager.LOG);
		}

		_world.read(data.getJSONObject("world"));

		// if (status.isOK()) {
		// _world.read(data.getJSONObject("world"));
		// } else {
		// StatusManager.getManager().handle(status, StatusManager.BLOCK |
		// StatusManager.LOG);
		// }
	}

	public void write(JSONObject data) {
		{
			JSONObject data2 = new JSONObject();
			data.put("settings", data2);
			_settings.write(data2);
		}

		{
			JSONObject data2 = new JSONObject();
			data.put("world", data2);
			_world.write(data2, true);
		}

		{
			data.put("asset-table", _world.getAssetTable().toJSON());
		}
	}

	public SceneSettings getSettings() {
		return _settings;
	}

	public WorldModel getWorld() {
		return _world;
	}

}
