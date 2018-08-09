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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Rectangle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

import phasereditor.atlas.core.AtlasCore;
import phasereditor.atlas.core.AtlasFrame;
import phasereditor.ui.FrameData;
import phasereditor.ui.PhaserEditorUI;

public class AtlasAssetModel extends AssetModel {
	private String _textureURL;
	private String _normalMap;
	private String _atlasURL;
	private String _format;
	private List<Frame> _frames;
	private Rectangle _imageSize;

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

		IFile textureFile = getFileFromUrl(_textureURL);
		IFile atlasFile = getFileFromUrl(_atlasURL);

		if (_normalMap == null || _normalMap.length() == 0) {
			return new IFile[] { textureFile, atlasFile };
		}

		return new IFile[] { textureFile, atlasFile, getFileFromUrl(_normalMap) };
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

	public class Frame extends AtlasFrame implements IAssetElementModel, IAssetFrameModel {
		private final AtlasAssetModel _asset;

		public Frame(AtlasAssetModel asset, int index) {
			super(index);
			_asset = asset;
		}

		@Override
		public AtlasAssetModel getAsset() {
			return _asset;
		}

		@Override
		public FrameData getFrameData() {

			FrameData data = super.getFrameData();

			if (isBottomUp()) {
				var size = getImageSize();
				if (size != null) {
					data.src.y = size.height - data.src.y - data.src.height;
				}
			}

			return data;
		}

		@Override
		public IFile getImageFile() {
			return _asset.getTextureFile();
		}
	}

	public Rectangle getImageSize() {
		if (getType() == AssetType.unityAtlas) {
			var file = getTextureFile();
			if (file != null && file.exists()) {
				Rectangle size = PhaserEditorUI.getImageBounds(file);
				_imageSize = size;
			}
		}
		return _imageSize;
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

	@SuppressWarnings("rawtypes")
	private synchronized void buildFrames() {

		_imageSize = null;
		getImageSize();

		// TODO: use AtlasCore.readAtlasFrames(..) method.

		List<Frame> list = new ArrayList<>();
		try {
			IFile file = getFileFromUrl(_atlasURL);
			if (file == null || !file.exists()) {
				_frames = list;
				return;
			}

			File ioFile = file.getLocation().toFile();

			JSONObject obj;
			switch (getType()) {
			case atlas: {
				if (AtlasCore.TEXTURE_ATLAS_JSON_ARRAY.equals(_format)) {
					obj = new JSONObject(new JSONTokener(ioFile));
					JSONArray array = obj.getJSONArray("frames");
					for (int i = 0; i < array.length(); i++) {
						JSONObject item = array.getJSONObject(i);
						Frame fi = new Frame(this, i);
						fi.update(AtlasFrame.fromArrayItem(i, item));
						list.add(fi);
					}
				} else {
					obj = new JSONObject(new JSONTokener(ioFile));
					JSONObject frames = obj.getJSONObject("frames");
					int i = 0;
					for (String k : frames.keySet()) {
						JSONObject item = frames.getJSONObject(k);
						Frame fi = new Frame(this, i);
						fi.update(AtlasFrame.fromHashItem(i, k, item));
						list.add(fi);
						i++;
					}
				}
				break;
			}
			case atlasXML: {
				try (FileInputStream input = new FileInputStream(ioFile)) {
					Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
					NodeList elems = doc.getElementsByTagName("SubTexture");
					for (int i = 0; i < elems.getLength(); i++) {
						Node elem = elems.item(i);
						if (elem.getNodeType() == Node.ELEMENT_NODE) {
							Frame fi = new Frame(this, i);
							fi.update(AtlasFrame.fromXMLItem(i, (Element) elem));
							list.add(fi);
						}
					}
				}
				break;
			}
			case unityAtlas: {
				try (var input = new FileInputStream(ioFile)) {
					var yaml = new Yaml();
					var data = (Map) yaml.load(input);
					Map data1 = (Map) data.get("TextureImporter");
					Map data2 = (Map) data1.get("spriteSheet");
					var data3 = (List) data2.get("sprites");
					for (int i = 0; i < data3.size(); i++) {
						var item = (Map) data3.get(i);
						Frame fi = new Frame(this, i);
						fi.update(AtlasFrame.fromUnitySprite(i, item));
						list.add(fi);
					}
				}
				break;
			}
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// sort frames
		
		_frames = AssetPackCore.sortAssets(list);

		{
			IFile file = getTextureFile();
			if (file != null && file.exists()) {
				Rectangle size = PhaserEditorUI.getImageBounds(file);
				_imageSize = size;
			}
		}
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
