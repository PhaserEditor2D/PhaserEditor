// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.atlas.core.AtlasFrame;

/**
 * @author arian
 *
 */
public class MultiAtlasAssetModel extends AssetModel {

	private String _url;
	private String _path;
	private List<Frame> _frames;

	public MultiAtlasAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.multiatlas, section);
	}

	public MultiAtlasAssetModel(JSONObject jsonData, AssetSectionModel section) throws JSONException {
		super(jsonData, section);

		_url = jsonData.optString("url", null);
		_path = jsonData.optString("path", null);
		_frames = null;
	}

	@Override
	public IFile[] computeUsedFiles() {
		return new IFile[] { getUrlFile() };
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);

		obj.put("url", _url);
		obj.put("path", _path);
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String atlasURL) {
		_url = atlasURL;
		firePropertyChange("url");
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
	}

	public String getPath() {
		return _path;
	}

	public void setPath(String path) {
		_path = path;
		firePropertyChange("path");
	}

	@Override
	public List<Frame> getSubElements() {
		if (_frames == null) {
			buildFrames(new ArrayList<>());
		}
		return _frames;
	}

	@Override
	protected void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);

		buildFrames(problems);
	}

	private void buildFrames(List<IStatus> problems) {
		List<Frame> list = new ArrayList<>();
		try {
			String content = null;
			{
				IFile file = getFileFromUrl(_url);
				if (file != null && file.exists()) {
					content = new String(Files.readAllBytes(file.getLocation().toFile().toPath()));
				}
			}

			if (content == null) {
				_frames = list;
				return;
			}

			var jsonContent = new JSONObject(content);
			var jsonTextures = jsonContent.getJSONArray("textures");
			int j = 0;
			for (var jsonTexture : jsonTextures.iterJSON()) {
				String textureFilename = jsonTexture.getString("image");
				JSONArray jsonFrames = jsonTexture.getJSONArray("frames");
				for (int i = 0; i < jsonFrames.length(); i++) {
					JSONObject jsonFrame = jsonFrames.getJSONObject(i);

					Frame frame = new Frame(i, textureFilename);
					frame.update(AtlasFrame.fromArrayItem(i, jsonFrame));

					String path = getPath();
					if (path == null) {
						path = "";
					} else {
						if (!path.endsWith("/")) {
							path += "/";
						}
					}

					String finalUrl = path + textureFilename;
					frame.setTextureUrl(finalUrl);
					IFile file = getFileFromUrl(finalUrl);
					frame.setImageFile(file);

					validateUrl(problems, "texture[" + j + "].frame[" + i + "].image", finalUrl);

					list.add(frame);
				}
				j++;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// sort list
		
		
		_frames = AssetPackCore.sortAssets(list);
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		String newUrl = getUrlFromFile(newFile);

		if (url.equals(_url)) {
			_url = newUrl;
		}
	}

	@SuppressWarnings("unchecked")
	public class Frame extends AtlasFrame implements IAssetElementModel, IAssetFrameModel {

		private IFile _imageFile;
		private String _textureFilename;
		private String _textureUrl;

		public Frame(int index, String textureFilename) {
			super(index);
			_textureFilename = textureFilename;
		}

		public void setTextureUrl(String textureUrl) {
			_textureUrl = textureUrl;
		}

		public String getTextureUrl() {
			return _textureUrl;
		}

		public String getTextureFilename() {
			return _textureFilename;
		}

		public void setTextureFilename(String textureFilename) {
			_textureFilename = textureFilename;
		}

		@Override
		public MultiAtlasAssetModel getAsset() {
			return MultiAtlasAssetModel.this;
		}

		@Override
		public IFile getImageFile() {
			return _imageFile;
		}

		public void setImageFile(IFile imageFile) {
			_imageFile = imageFile;
		}
	}

}
