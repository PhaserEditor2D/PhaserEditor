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

import org.eclipse.core.resources.IFile;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class CanvasModel {
	private EditorSettings _settings;
	private StateSettings _stateSettings;
	private WorldModel _world;
	private IFile _file;
	private CanvasType _type;

	public CanvasModel(IFile file) {
		_file = file;
		_settings = new EditorSettings();
		_stateSettings = new StateSettings();
		_world = new WorldModel(file);

		// set Group just for backward compatibility
		_type = CanvasType.GROUP;
	}

	public CanvasType getType() {
		return _type;
	}

	public void setType(CanvasType type) {
		_type = type;
	}

	public String getClassName() {
		if (_file == null) {
			return "Canvas";
		}

		String name = _file.getName();
		String ext = _file.getFileExtension();
		int end = name.length() - ext.length() - 1;
		return name.substring(0, end);
	}

	public void read(JSONObject data) {
		_settings.read(data.getJSONObject("settings"));
		{
			JSONObject data2 = data.optJSONObject("stateSettings");
			if (data2 == null) {
				data2 = new JSONObject();
			}
			_stateSettings.read(data2);
		}
		_world.getAssetTable().read(data.optJSONObject("asset-table"));
		_world.read(data.getJSONObject("world"));

		{
			String name = data.optString("type", CanvasType.GROUP.name());
			_type = CanvasType.valueOf(name);
		}
	}

	public void write(JSONObject data) {
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
			_world.write(data2, true);
		}

		{
			data.put("type", _type.name());
		}

		{
			data.put("asset-table", _world.getAssetTable().toJSON());
		}
	}

	public EditorSettings getSettings() {
		return _settings;
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
}
