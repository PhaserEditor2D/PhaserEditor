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

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONException;
import org.json.JSONObject;

public class PhysicsAssetModel extends AssetModel {

	public static final String LIME_CORONA_JSON = "LIME_CORONA_JSON";
	private String _url;
	private String _data;
	private String _format;
	private List<SpriteData> _sprites;

	{
		_format = LIME_CORONA_JSON;
	}

	public PhysicsAssetModel(JSONObject jsonDoc, AssetSectionModel section) throws JSONException {
		super(jsonDoc, section);
		_url = jsonDoc.optString("url", null);
		{
			JSONObject data = jsonDoc.optJSONObject("data");
			if (data != null) {
				_data = data.toString(4);
			}
		}
		_format = jsonDoc.optString("format", LIME_CORONA_JSON);
	}

	public PhysicsAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.physics, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("url", _url);
		{
			String strData = normalizeString(_data);
			JSONObject data = null;
			if (strData != null) {
				data = new JSONObject(strData);
			}
			obj.put("data", data);
		}
		obj.put("format", _format);
	}

	public class SpriteData implements IAssetElementModel {
		private String _name;

		@Override
		public AssetModel getAsset() {
			return PhysicsAssetModel.this;
		}

		@Override
		public String getName() {
			return _name;
		}

		public void setName(String name) {
			_name = name;
		}

	}

	public List<SpriteData> getSprites() {
		if (_sprites == null) {
			buildSprites();
		}
		return _sprites;
	}

	@Override
	public List<? extends IAssetElementModel> getSubElements() {
		return getSprites();
	}

	private void buildSprites() {
		List<SpriteData> sprites = new ArrayList<>();
		try {
			String data = normalizeString(_data);
			if (data == null) {
				IFile file = getFileFromUrl(_url);
				if (file != null && file.exists()) {
					data = new String(Files.readAllBytes(file.getLocation().toFile().toPath()));
				}
			}
			if (data != null) {
				JSONObject obj = new JSONObject(data);
				for (String key : obj.keySet()) {
					SpriteData sprite = new SpriteData();
					sprite.setName(key);
					sprites.add(sprite);
				}
			}
		} catch (Exception e) {
			// do nothing, we cannot understand the format.
			e.printStackTrace();
		}
		_sprites = sprites;
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
		firePropertyChange("url");
		buildSprites();
	}

	public String getData() {
		return _data;
	}

	public void setData(String data) {
		_data = data;
		firePropertyChange("data");
		buildSprites();
	}

	public String getFormat() {
		return _format;
	}

	public void setFormat(String format) {
		_format = format;
		firePropertyChange("format");
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrlAndData(problems, "url", _url, "data", _data);
		buildSprites();
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		if (url.equals(_url)) {
			_url = getUrlFromFile(newFile);
		}
	}
}
