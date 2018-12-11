// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.core;

import static java.lang.System.err;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.json.JSONArray;
import org.json.JSONObject;

public class AssetSectionModel implements IAdaptable, IAssetPackEelement, IEditableKey {
	private JSONArray _definition;
	private String _key;
	private List<AssetModel> _assets;
	private AssetPackModel _pack;
	private Map<AssetType, AssetGroupModel> _groupMap;

	public AssetSectionModel(String key, AssetPackModel pack) {
		_key = key;
		_assets = new ArrayList<>();
		_groupMap = new HashMap<>();
		_pack = pack;
	}

	public AssetSectionModel(String key, JSONArray jsonData, AssetPackModel pack) throws Exception {
		this(key, pack);
		_definition = jsonData;
		_assets = new ArrayList<>();
		for (int i = 0; i < jsonData.length(); i++) {
			JSONObject jsonAsset = jsonData.getJSONObject(i);
			String name = jsonAsset.getString("type");
			if (AssetType.isTypeSupported(name)) {
				AssetType type = AssetModel.readAssetType(jsonAsset);
				AssetFactory factory = AssetFactory.getFactory(type);
				AssetModel asset = factory.createAsset(jsonAsset, this);
				_assets.add(asset);
			} else {
				err.println("AssetSectionModel: AssetType '" + name + "' is not supported.");
			}
		}
	}

	public AssetPackModel getPack() {
		return _pack;
	}

	public void setPack(AssetPackModel pack) {
		_pack = pack;
	}

	public void addAsset(int index, AssetModel asset, boolean notify) {
		asset.setSection(this, notify);
		_assets.add(index, asset);
		if (notify) {
			getPack().setDirty(true);
		}
	}

	public void addAllAssets(int index, List<AssetModel> assets, boolean notify) {
		for (var asset : assets) {
			asset.setSection(this, false);
		}

		_assets.addAll(index, assets);
		
		if (notify) {
			getPack().setDirty(true);
		}
	}

	public void addAsset(AssetModel asset, boolean notify) {
		this.addAsset(_assets.size(), asset, notify);
	}

	@Override
	public String getKey() {
		return _key;
	}

	@Override
	public void setKey(String key) {
		setKey(key, true);
	}

	public void setKey(String key, boolean notify) {
		_key = key;
		if (notify) {
			firePropertyChange("key");
			getPack().firePropertyChange(AssetPackModel.PROP_ASSET_KEY);
		}
	}

	public JSONArray getDefinition() {
		return _definition;
	}

	public List<AssetModel> getAssets() {
		return Collections.unmodifiableList(_assets);
	}

	public AssetModel findAsset(String key) {
		for (AssetModel asset : _assets) {
			String key2 = asset.getKey();
			if (key2 != null && key2.equals(key)) {
				return asset;
			}
		}
		return null;
	}

	public AssetGroupModel getGroup(AssetType type) {
		if (!_groupMap.containsKey(type)) {
			_groupMap.put(type, new AssetGroupModel(type, this));
		}
		return _groupMap.get(type);
	}

	public void removeAsset(AssetModel asset) {
		removeAsset(asset, true);
	}

	public void removeAsset(AssetModel asset, boolean notify) {
		_assets.remove(asset);
		if (notify) {
			getPack().setDirty(true);
		}
	}

	public void removeAllAssets(List<AssetModel> assets, boolean notify) {
		_assets.removeAll(assets);
		if (notify) {
			getPack().setDirty(true);
		}
	}

	public void removeGroup(AssetGroupModel group) {
		ArrayList<AssetModel> list = new ArrayList<>(_assets);
		for (AssetModel asset : list) {
			if (asset.getType() == group.getType()) {
				removeAsset(asset);
			}
		}
	}

	public void writeSection(JSONObject pack) {
		JSONObject jsonSection = new JSONObject();
		JSONArray jsonFiles = new JSONArray();
		jsonSection.put("files", jsonFiles);

		pack.put(_key, jsonSection);

		for (AssetModel asset : _assets) {
			jsonFiles.put(asset.toJSON());
		}
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		support.firePropertyChange(property, true, false);
		getPack().setDirty(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public AssetSectionModel copy() {
		return new AssetSectionModel(_key, _pack);
	}

	public boolean isSharedVersion() {
		AssetPackModel pack = getPack();
		return pack.isSharedVersion() && pack.getSections().contains(this);
	}

	public final AssetSectionModel getSharedVersion() {
		if (isSharedVersion()) {
			return this;
		}

		try {
			AssetPackModel pack = getPack().getSharedVersion();

			if (pack == null) {
				return null;
			}

			AssetSectionModel section = pack.findSection(_key);

			return section;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}