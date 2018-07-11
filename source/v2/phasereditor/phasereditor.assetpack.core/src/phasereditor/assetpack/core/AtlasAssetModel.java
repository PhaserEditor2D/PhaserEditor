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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
	private String _normalMap;
	private String _atlasURL;
	private String _format;
	private List<Frame> _frames;

	{
		_format = AtlasCore.TEXTURE_ATLAS_JSON_ARRAY;
	}

	public AtlasAssetModel(JSONObject jsonData, AssetSectionModel section) throws JSONException {
		super(jsonData, section);
		_textureURL = jsonData.optString("textureURL", null);
		_normalMap = jsonData.optString("normalMap", null);
		_atlasURL = jsonData.optString("atlasURL", null);
		_format = jsonData.optString("format", AtlasCore.TEXTURE_ATLAS_JSON_ARRAY);
	}

	public AtlasAssetModel(AssetType type, String key, AssetSectionModel section) throws JSONException {
		super(key, type, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("textureURL", _textureURL);
		obj.put("normalMap", _normalMap);
		obj.put("atlasURL", _atlasURL);
		obj.put("format", _format);
	}

	@Override
	public IFile[] computeUsedFiles() {
		return new IFile[] { getFileFromUrl(_textureURL), getFileFromUrl(_normalMap), getFileFromUrl(_atlasURL) };
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

	public IFile getNormalMapFile() {
		return getFileFromUrl(_normalMap);
	}

	public String getNormalMap() {
		return _normalMap;
	}

	public void setNormalMap(String normalMap) {
		_normalMap = normalMap;
		firePropertyChange("normalMap");
	}

	public String getAtlasURL() {
		return _atlasURL;
	}

	public void setAtlasURL(String atlasURL) {
		_atlasURL = atlasURL;
		firePropertyChange("atlasURL");
	}

	public String getFormat() {
		return _format;
	}

	public void setFormat(String format) {
		_format = format;
		firePropertyChange("format");
	}

	public static class Frame extends AtlasFrame implements IAssetElementModel, IAssetFrameModel {
		private final AtlasAssetModel _asset;
		private int _index;

		public Frame(AtlasAssetModel asset, int index) {
			_asset = asset;
			_index = index;
		}

		@Override
		public AtlasAssetModel getAsset() {
			return _asset;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public FrameData getFrameData() {
			FrameData data = new FrameData(_index);
			data.src = new Rectangle(getFrameX(), getFrameY(), getFrameW(), getFrameH());
			data.dst = new Rectangle(getSpriteX(), getSpriteY(), getSpriteW(), getSpriteH());
			data.srcSize = new Point(getSourceW(), getSourceH());
			return data;
		}

		@Override
		public IFile getImageFile() {
			return _asset.getTextureFile();
		}
	}

	public List<Frame> getAtlasFrames() {
		if (_frames == null) {
			buildFrames();
		}
		return _frames;
	}

	@Override
	public List<Frame> getSubElements() {
		return getAtlasFrames();
	}

	private synchronized void buildFrames() {

		// TODO: use AtlasCore.readAtlasFrames(..) method.

		List<Frame> list = new ArrayList<>();
		try {
			String data = null;
			IFile file = getFileFromUrl(_atlasURL);
			if (file != null && file.exists()) {
				try (InputStream input = file.getContents()) {
					data = PhaserEditorUI.readString(input);
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
					Frame fi = new Frame(this, i);
					fi.update(AtlasFrame.fromArrayItem(item));
					list.add(fi);
				}
				break;
			case AtlasCore.TEXTURE_ATLAS_JSON_HASH:
				obj = new JSONObject(data);
				JSONObject frames = obj.getJSONObject("frames");
				int i = 0;
				for (String k : frames.keySet()) {
					JSONObject item = frames.getJSONObject(k);
					Frame fi = new Frame(this, i);
					fi.update(AtlasFrame.fromHashItem(k, item));
					list.add(fi);
					i++;
				}
				break;
			case AtlasCore.TEXTURE_ATLAS_XML_STARLING:
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
						.parse(new ByteArrayInputStream(data.getBytes()));
				NodeList elems = doc.getElementsByTagName("SubTexture");
				for (int j = 0; j < elems.getLength(); j++) {
					Node elem = elems.item(j);
					if (elem.getNodeType() == Node.ELEMENT_NODE) {
						Frame fi = new Frame(this, j);
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
		validateUrl(problems, "atlasURL", _atlasURL);
		validateUrl(problems, "textureURL", _textureURL);
		if (_normalMap != null && _normalMap.length() > 0) {
			validateUrl(problems, "normalMap", _normalMap);
		}

		buildFrames();
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		String newUrl = getUrlFromFile(newFile);

		if (url.equals(_textureURL)) {
			_textureURL = newUrl;
		}

		if (url.equals(_normalMap)) {
			_normalMap = newUrl;
		}

		if (url.equals(_atlasURL)) {
			_atlasURL = newUrl;
		}
	}

}
