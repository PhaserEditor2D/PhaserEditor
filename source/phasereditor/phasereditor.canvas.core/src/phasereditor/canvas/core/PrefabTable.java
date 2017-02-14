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
package phasereditor.canvas.core;

import static java.lang.System.out;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class PrefabTable {
	private Map<Object, Object> _map;
	int _counter;
	private WorldModel _worldModel;

	public PrefabTable(WorldModel worldModel) {
		_worldModel = worldModel;
		_map = new HashMap<>();
		_counter = 0;
	}

	public String postPrefab(Prefab prefab) {
		if (_map.containsKey(prefab)) {
			return (String) _map.get(prefab);
		}

		String id = Integer.toString(_counter);

		_map.put(id, prefab);
		_map.put(prefab, id);

		_counter++;

		return id;
	}

	public Prefab lookup(String id) {
		return (Prefab) _map.get(id);
	}

	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		for (Object value : _map.values()) {
			if (value instanceof Prefab) {
				String id = (String) _map.get(value);
				obj.put(id, ((Prefab) value).getFile().getProjectRelativePath().toPortableString());
			}
		}
		return obj;
	}

	public void read(JSONObject data) {
		if (data == null) {
			out.println("Cannot load the prefab table, probably it is an older version of the canvas file.");
			return;
		}

		for (String id : data.keySet()) {
			String filepath = data.getString(id);
			IFile file = _worldModel.getFile().getProject().getFile(filepath);
			Prefab prefab = new Prefab(file);
			_map.put(id, prefab);
			_map.put(prefab, id);
		}
	}

}
