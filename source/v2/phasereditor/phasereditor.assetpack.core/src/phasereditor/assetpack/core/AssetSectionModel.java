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

public class AssetSectionModel implements IAdaptable, IAssetPackEelement {
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

	public AssetSectionModel(String key, JSONArray definition, AssetPackModel pack) throws Exception {
		this(key, pack);
		_definition = definition;
		_assets = new ArrayList<>();
		for (int i = 0; i < definition.length(); i++) {
			JSONObject jsonAsset = definition.getJSONObject(i);
			AssetType type = AssetModel.readAssetType(jsonAsset);
			AssetFactory factory = AssetFactory.getFactory(type);
			AssetModel asset = factory.createAsset(jsonAsset, this);
			_assets.add(asset);
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

	public void addAsset(AssetModel asset, boolean notify) {
		this.addAsset(_assets.size(), asset, notify);
	}

	public String getKey() {
		return _key;
	}

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

	public void removeGroup(AssetGroupModel group) {
		ArrayList<AssetModel> list = new ArrayList<>(_assets);
		for (AssetModel asset : list) {
			if (asset.getType() == group.getType()) {
				removeAsset(asset);
			}
		}
	}

	public void writeSection(JSONObject pack) {
		JSONArray array = new JSONArray();
		pack.put(_key, array);
		for (AssetModel asset : _assets) {
			array.put(asset.toJSON());
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