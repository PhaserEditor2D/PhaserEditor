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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import phasereditor.atlas.core.AtlasCore;
import phasereditor.atlas.core.AtlasFrame;
import phasereditor.ui.PhaserEditorUI;

public class AtlasAssetModel extends AssetModel {
	private String _textureURL;
	private String _atlasURL;
	private String _atlasData;
	private String _format;
	private List<FrameItem> _frames;

	{
		_format = AtlasCore.TEXTURE_ATLAS_JSON_ARRAY;
	}

	public AtlasAssetModel(JSONObject jsonDoc, AssetSectionModel section) throws JSONException {
		super(jsonDoc, section);
		_textureURL = jsonDoc.optString("textureURL", null);
		_atlasURL = jsonDoc.optString("atlasURL", null);
		{
			_atlasData = null;
			if (jsonDoc.has("atlasData")) {
				Object data = jsonDoc.get("atlasData");
				if (data != null) {
					if (data instanceof JSONObject) {
						_atlasData = ((JSONObject) data).toString(4);
					} else {
						_atlasData = data.toString();
					}
				}
			}
		}
		_format = jsonDoc.optString("format", AtlasCore.TEXTURE_ATLAS_JSON_ARRAY);
	}

	public AtlasAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.atlas, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("textureURL", _textureURL);
		obj.put("atlasURL", _atlasURL);
		{
			// the data can be a json object, json string, or an xml string.
			String strData = normalizeString(_atlasData);
			Object data = strData;
			if (strData != null) {
				try {
					data = new JSONObject(strData);
				} catch (JSONException e) {
					// nothing
				}
			}
			obj.put("atlasData", data);
		}
		obj.put("format", _format);
	}

	@Override
	public IFile[] getUsedFiles() {
		return new IFile[] { getFileFromUrl(_textureURL), getFileFromUrl(_atlasURL) };
	}

	public IFile getTextureFile() {
		return getFileFromUrl(getTextureURL());
	}

	public String getTextureURL() {
		return _textureURL;
	}

	public void setTextureURL(String textureURL) {
		_textureURL = textureURL;
		firePropertyChange("textureURL");
	}

	public String getAtlasURL() {
		return _atlasURL;
	}

	public void setAtlasURL(String atlasURL) {
		_atlasURL = atlasURL;
		firePropertyChange("atlasURL");
		buildFrames();
	}

	public String getAtlasData() {
		return _atlasData;
	}

	public void setAtlasData(String atlasData) {
		_atlasData = atlasData;
		firePropertyChange("atlasData");
		buildFrames();
	}

	public String getFormat() {
		return _format;
	}

	public void setFormat(String format) {
		_format = format;
		firePropertyChange("format");
		buildFrames();
	}

	public static class FrameItem extends AtlasFrame implements IAssetElementModel {
		private final AssetModel _asset;

		public FrameItem(AssetModel asset) {
			_asset = asset;
		}

		@Override
		public AssetModel getAsset() {
			return _asset;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}
	}

	public List<FrameItem> getAtlasFrames() {
		if (_frames == null) {
			buildFrames();
		}
		return _frames;
	}

	@Override
	public List<? extends IAssetElementModel> getSubElements() {
		return getAtlasFrames();
	}

	private void buildFrames() {

		// TODO: use AtlasCore.readAtlasFrames(..) method.

		List<FrameItem> list = new ArrayList<>();
		try {
			String data = normalizeString(_atlasData);
			if (data == null) {
				IFile file = getFileFromUrl(_atlasURL);
				if (file != null && file.exists()) {
					try (InputStream input = file.getContents()) {
						data = PhaserEditorUI.readString(input);
					}
				}
			}

			if (data == null) {
				_frames = list;
				return;
			}

			JSONObject obj;
			switch (_format) {
			case AtlasCore.TEXTURE_ATLAS_JSON_ARRAY:
				obj = new JSONObject(data);
				JSONArray array = obj.getJSONArray("frames");
				for (int i = 0; i < array.length(); i++) {
					JSONObject item = array.getJSONObject(i);
					FrameItem fi = new FrameItem(this);
					fi.update(AtlasFrame.fromArrayItem(item));
					list.add(fi);
				}
				break;
			case AtlasCore.TEXTURE_ATLAS_JSON_HASH:
				obj = new JSONObject(data);
				JSONObject frames = obj.getJSONObject("frames");
				for (String k : frames.keySet()) {
					JSONObject item = frames.getJSONObject(k);
					FrameItem fi = new FrameItem(this);
					fi.update(AtlasFrame.fromHashItem(k, item));
					list.add(fi);
				}
				break;
			case AtlasCore.TEXTURE_ATLAS_XML_STARLING:
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
						.parse(new ByteArrayInputStream(data.getBytes()));
				NodeList elems = doc.getElementsByTagName("SubTexture");
				for (int i = 0; i < elems.getLength(); i++) {
					Node elem = elems.item(i);
					if (elem.getNodeType() == Node.ELEMENT_NODE) {
						FrameItem fi = new FrameItem(this);
						fi.update(AtlasFrame.fromXMLItem((Element) elem));
						list.add(fi);
					}
				}
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_frames = list;
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrlAndData(problems, "atlasURL", _atlasURL, "atlasData", _atlasData);
		validateUrl(problems, "textureURL", _textureURL);

		buildFrames();
	}
}
