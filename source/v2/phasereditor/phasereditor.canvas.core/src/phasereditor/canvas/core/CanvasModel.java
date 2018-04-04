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
package phasereditor.canvas.core;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class CanvasModel {
	public static final int CURRENT_VERSION = 2;

	private EditorSettings _settings;
	private StateSettings _stateSettings;
	private WorldModel _world;
	private IFile _file;
	private CanvasType _type;
	private int _version;

	public CanvasModel(IFile file) {
		_file = file;
		_settings = new EditorSettings(this);
		_settings.setClassName(CanvasCore.getDefaultClassName(file));
		_stateSettings = new StateSettings();
		_world = new WorldModel(this);

		// set Group just for backward compatibility
		_type = CanvasType.GROUP;
		_version = CURRENT_VERSION;
	}

	public IFile getFile() {
		return _file;
	}

	public void setFile(IFile file) {
		_file = file;
	}

	public CanvasType getType() {
		return _type;
	}

	public void setType(CanvasType type) {
		_type = type;
	}

	public void read(JSONObject data) {
		// version 1 did not write it explicitly
		_version = data.optInt("canvas-version", 1);

		{
			String name = data.optString("type", CanvasType.GROUP.name());
			_type = CanvasType.valueOf(name);
		}
		
		_settings.read(data.getJSONObject("settings"));
		{
			JSONObject data2 = data.optJSONObject("stateSettings");
			if (data2 == null) {
				data2 = new JSONObject();
			}
			_stateSettings.read(data2);

			// just for compatibility with version 1
			if (_settings.getClassName() == null) {
				_settings.setClassName(CanvasCore.getDefaultClassName(_file));
			}
		}

		_world.getAssetTable().read(data.optJSONObject("asset-table"));
		_world.getPrefabTable().read(data.optJSONObject("prefab-table"));
		_world.read(data.getJSONObject("world"));
	}

	public void write(JSONObject data, boolean saving) {
		{
			// always write the current version
			data.put("canvas-version", CURRENT_VERSION);
		}
		{
			JSONObject data2 = new JSONObject();
			data.put("settings", data2);
			_settings.write(data2);
		}

		{
			JSONObject data2 = new JSONObject();
			data.put("stateSettings", data2);
			_stateSettings.write(data2);
		}

		{
			JSONObject data2 = new JSONObject();
			data.put("world", data2);
			_world.write(data2, saving);
		}

		{
			data.put("type", _type.name());
		}

		{
			data.put("asset-table", _world.getAssetTable().toJSON());
		}

		{
			data.put("prefab-table", _world.getPrefabTable().toJSON());
		}
	}

	public int getVersion() {
		return _version;
	}

	public EditorSettings getSettings() {
		return _settings;
	}

	public void setSettings(EditorSettings settings) {
		_settings = settings;
	}

	public StateSettings getStateSettings() {
		return _stateSettings;
	}

	public void setStateSettings(StateSettings stateSettings) {
		_stateSettings = stateSettings;
	}

	public WorldModel getWorld() {
		return _world;
	}

	public void save(IFile file, IProgressMonitor monitor) throws JSONException, CoreException {
		JSONObject data = new JSONObject();

		_world.setAssetTable(new AssetTable(_world));
		_world.setPrefabTable(new PrefabTable(_world));
		
		write(data, true);

		file.setContents(new ByteArrayInputStream(data.toString(2).getBytes()), true, false, monitor);
	}
}
