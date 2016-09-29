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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.log.core.LogStatus;

/**
 * @author arian
 *
 */
public class AssetLookupTable {
	private List<Entry> _entries;
	private WorldModel _worldModel;
	private int _counter;
	private HashMap<String, IAssetKey> _map;

	private static class Entry {

		public Entry(String id, IAssetKey asset) {
			this.id = id;
			this.asset = asset;
		}

		public String id;
		public IAssetKey asset;
	}

	public AssetLookupTable(WorldModel worldModel) {
		_worldModel = worldModel;
		_entries = new ArrayList<>();
		_map = new HashMap<>();
	}

	public String registerAsset(IAssetKey key) {
		IAssetKey key1 = key.findFreshVersion();
		for (Entry entry : _entries) {
			IAssetKey key2 = entry.asset.findFreshVersion();
			if (key1.equals(key2)) {
				return entry.id;
			}
		}

		String id = Integer.toString(_counter++);
		_entries.add(new Entry(id, key1));
		_map.put(id, key1);

		return id;
	}

	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		for (Entry entry : _entries) {
			obj.put(entry.id, AssetPackCore.getAssetJSONReference(entry.asset));
		}
		return obj;
	}

	public IStatus read(JSONObject obj) {
		if (obj == null) {
			out.println("Cannot load the asset table, probably it is an older version of the canvas file.");
			return Status.OK_STATUS;
		}

		_entries = new ArrayList<>();
		_map = new HashMap<>();

		LogStatus status = new LogStatus(CanvasCore.PLUGIN_ID, "Failed to load the assets.");

		for (String id : obj.keySet()) {
			JSONObject refObj = obj.getJSONObject(id);
			Object asset = AssetPackCore.findAssetElement(_worldModel.getProject(), refObj);
			if (asset instanceof IAssetKey) {
				IAssetKey assetKey = (IAssetKey) asset;
				_entries.add(new Entry(id, assetKey));
				_map.put(id, assetKey);
			} else {
				out.println("Cannot find " + refObj.toString());
				String msg = "section=" + refObj.optString("section") + ", key=" + refObj.optString("asset")
						+ ", frame=" + refObj.optString("sprite", "");
				status.add(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, "Not found: " + msg));
			}
		}

		return status;
	}

	public IAssetKey lookup(String id) {
		return _map.get(id);
	}
	
	public void reset() {
		_entries = new ArrayList<>();
		_map = new HashMap<>();
	}
}
