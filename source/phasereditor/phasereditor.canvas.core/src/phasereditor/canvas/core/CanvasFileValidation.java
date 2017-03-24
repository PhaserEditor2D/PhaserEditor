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

import static java.lang.System.out;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.project.core.ProjectCore;

/**
 * 
 * This class can be used to perform fast validations on Canvas files and
 * models. The main use case is to use in the Project build participation.
 * 
 * @author arian
 *
 */
public class CanvasFileValidation {

	private IFile _file;
	private List<IStatus> _problems;
	private Map<String, IAssetKey> _assetTable;
	private HashMap<String, IFile> _prefabTable;
	private JSONObject _data;
	private Set<String> _usedRefError;
	private Set<String> _usedNames;

	public CanvasFileValidation(IFile file) throws Exception {
		super();
		_file = file;
		_problems = new ArrayList<>();
		_usedNames = new HashSet<>();
		try (InputStream contents = file.getContents()) {
			_data = new JSONObject(new JSONTokener(contents));
		}
		_usedRefError = new HashSet<>();
	}

	public List<IStatus> validate() {

		out.println("Validating " + _file.getProjectRelativePath());

		validateVersion();
		validateAssetTable();
		validatePrefabTable();
		validateWorld();

		return _problems;
	}

	private void validateVersion() {
		if (_data.optInt("canvas-version", 1) != CanvasModel.CURRENT_VERSION) {
			_problems.add(
					new Status(IStatus.WARNING, CanvasCore.PLUGIN_ID, "This canvas file has an old syntax version."));
		}
	}

	private void validateWorld() {
		JSONObject world = _data.getJSONObject("world");
		validateObject(world);
	}

	private void validateObject(JSONObject obj) {
		String type = obj.getString("type");
		JSONObject info = obj.getJSONObject("info");

		if (type.equals("group")) {
			JSONArray list = info.getJSONArray("children");
			for (int i = 0; i < list.length(); i++) {
				JSONObject child = list.getJSONObject(i);
				validateObject(child);
			}
		} else if (type.equals("prefab")) {
			validatePrefabInstance(obj);
		} else {

			if (obj.has("asset-ref")) {
				JSONObject ref = obj.getJSONObject("asset-ref");
				// a null reference is added when it is not found, but that
				// error is reported in the table validation.
				if (ref != null) {
					validateRef(info.optString("editorName", "?"), ref);
				}
			} else if (obj.has("asset")) {
				String id = obj.getString("asset");
				if (_assetTable.getOrDefault(id, null) == null) {
					String spriteId = info.optString("editorName", "?");
					_problems.add(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID,
							"Wrong asset-table reference in sprite '" + spriteId + "'"));
				}
			}
		}

		{
			String name = info.getString("editorName");
			if (_usedNames.contains(name)) {
				_problems.add(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID,
						"The name '" + name + "' is used by other object."));
			} else {
				_usedNames.add(name);
			}
		}
	}

	private void validatePrefabInstance(JSONObject obj) {
		IFile file;
		if (obj.has("prefabFile")) {
			file = _file.getProject().getFile(obj.getString("prefabFile"));
		} else {
			file = _prefabTable.get(obj.getString("prefab"));
		}
		if (file == null || !file.exists()) {
			JSONObject info = obj.getJSONObject("info");
			String name = info.optString("editorName", "?");
			_problems.add(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, "Missing prefab of '" + name + "'."));
		}
	}

	private void validateRef(String spriteId, JSONObject ref) {
		Object asset = AssetPackCore.findAssetElement(_file.getProject(), ref);

		boolean problem = false;

		if (asset == null) {
			problem = true;
		} else {
			if (asset instanceof IAssetKey) {
				IFile file = ((IAssetKey) asset).getAsset().getPack().getFile();
				problem = ProjectCore.hasProblems(file);
			} else {
				problem = true;
			}
		}

		if (problem) {
			postMissingRefError(ref);
			postMissingSpriteAsset(spriteId);
		}
	}

	private void postMissingSpriteAsset(String spriteId) {
		String msg = "Missing asset of sprite '" + spriteId + "'.";
		_problems.add(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, msg));
	}

	private void postMissingRefError(JSONObject ref) {
		String msg = getAssetRefLabel(ref);

		msg = "Asset not found: " + msg;

		if (!_usedRefError.contains(msg)) {
			_problems.add(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, msg));
			_usedRefError.add(msg);
		}
	}

	private static String getAssetRefLabel(JSONObject ref) {
		String msg = "section=" + ref.optString("section") + ", key=" + ref.optString("asset");

		if (ref.has("frame")) {
			msg += ", frame=" + ref.optString("sprite", "");
		}
		return msg;
	}

	private void validateAssetTable() {
		_assetTable = new HashMap<>();
		JSONObject tableData = _data.optJSONObject("asset-table");
		if (tableData == null) {
			return;
		}

		IProject project = _file.getProject();

		for (String id : tableData.keySet()) {
			JSONObject refObj = tableData.getJSONObject(id);
			Object asset = AssetPackCore.findAssetElement(project, refObj);
			if (asset != null && asset instanceof IAssetKey) {
				IAssetKey assetKey = (IAssetKey) asset;
				_assetTable.put(id, assetKey);
			} else {
				_assetTable.put(id, null);
				postMissingRefError(refObj);
			}
		}
	}

	private void validatePrefabTable() {
		_prefabTable = new HashMap<>();
		JSONObject tableData = _data.optJSONObject("prefab-table");
		if (tableData == null) {
			return;
		}

		IProject project = _file.getProject();

		for (String id : tableData.keySet()) {
			String filepath = tableData.getString(id);
			IFile file = project.getFile(filepath);

			if (!file.exists()) {
				_assetTable.put(id, null);
				_problems.add(
						new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, "Prefab file not found '" + filepath + "'."));
				continue;
			}

			_prefabTable.put(id, file);
		}
	}
}
