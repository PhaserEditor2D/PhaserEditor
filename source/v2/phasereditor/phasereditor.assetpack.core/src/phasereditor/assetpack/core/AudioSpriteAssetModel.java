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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.project.core.ProjectCore;

public class AudioSpriteAssetModel extends AudioAssetModel {

	private String _jsonURL;
	private List<AssetAudioSprite> _spritemap;

	public AudioSpriteAssetModel(JSONObject definition,
			AssetSectionModel section) throws JSONException {
		super(definition, section);
		readInfo(definition);
	}

	@Override
	public void readInfo(JSONObject data) {
		super.readInfo(data);
		
		_jsonURL = data.optString("jsonURL", null);
	}

	public AudioSpriteAssetModel(String key, AssetSectionModel section)
			throws JSONException {
		super(key, AssetType.audioSprite, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("jsonURL", normalizeString(_jsonURL));
	}

	public String getJsonURL() {
		return _jsonURL;
	}

	public void setJsonURL(String jsonURL) {
		_jsonURL = jsonURL;
	}

	public IFile getJsonURLFile() {
		return getFileFromUrl(_jsonURL);
	}

	public void setJsonURLFile(IFile file) {
		setJsonURL(ProjectCore.getAssetUrl(file));
	}

	/**
	 * Create the list of the url listed in the "resources" section of the JSON
	 * file ({@link #getJsonURL()}) or json data ({@link #getJsonData()}).
	 * 
	 * @return The urls listed in the "resources" section of the JSON.
	 */
	public List<String> getResourcesInJSON() {
		JSONObject obj = getJsonDataObject();
		if (obj != null) {
			// ok, make a list with the resources listed in the file
			JSONArray array = obj.optJSONArray("resources");

			if (array != null && array.length() > 0) {
				// on only build the list if there are more than one url.
				List<String> list = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					list.add(array.getString(i));
				}
				return list;
			}
		}
		// return null mean there is not any resource listed in the file.
		return null;
	}

	public class AssetAudioSprite extends
			phasereditor.audiosprite.core.AudioSprite implements
			IAssetElementModel {

		@Override
		public AudioSpriteAssetModel getAsset() {
			return AudioSpriteAssetModel.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(getEnd());
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result
					+ ((getName() == null) ? 0 : getName().hashCode());
			temp = Double.doubleToLongBits(getStart());
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AssetAudioSprite other = (AssetAudioSprite) obj;
			if (Double.doubleToLongBits(getEnd()) != Double
					.doubleToLongBits(other.getEnd()))
				return false;
			if (getName() == null) {
				if (other.getName() != null)
					return false;
			} else if (!getName().equals(other.getName()))
				return false;
			if (Double.doubleToLongBits(getStart()) != Double
					.doubleToLongBits(other.getStart()))
				return false;
			return true;
		}
	}

	public List<AssetAudioSprite> getSpriteMap() {
		if (_spritemap == null) {
			buildSpriteMap();
		}
		return _spritemap;
	}

	private void buildSpriteMap() {
		List<AssetAudioSprite> list = new ArrayList<>();
		JSONObject obj = getJsonDataObject();
		if (obj != null) {
			JSONObject spritemap = obj.optJSONObject("spritemap");
			if (spritemap != null) {
				for (String k : spritemap.keySet()) {
					JSONObject sprite = spritemap.getJSONObject(k);
					AssetAudioSprite sd = new AssetAudioSprite();
					sd.setName(k);
					sd.setStart(sprite.getDouble("start"));
					sd.setEnd(sprite.getDouble("end"));
					list.add(sd);
				}
			}
		}
		Collections.sort(list, new Comparator<AssetAudioSprite>() {

			@Override
			public int compare(AssetAudioSprite o1, AssetAudioSprite o2) {
				return Double.compare(o1.getStart(), o2.getStart());
			}
		});
		_spritemap = list;
	}

	public JSONObject getJsonDataObject() {
		JSONObject obj = null;
		IFile file = getJsonURLFile();
		if (file != null && file.exists()) {
			// load the json from the file
			try (InputStream input = file.getContents()) {
				obj = new JSONObject(new JSONTokener(input));
			} catch (IOException | CoreException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		// return null mean there is not any resource listed in the file.
		return obj;
	}

	/**
	 * Update the "urls" parameter with those resources (if it exists) listed in
	 * the "resources" section of the given JSON (through "jsonURL" or
	 * "jsonData").
	 * 
	 * @return If the 'urls' was updated or not.
	 */
	public boolean setUrlsFromJsonResources() {
		List<String> urls = getResourcesInJSON();

		if (urls != null) {

			List<String> urls2 = new ArrayList<>();
			for (String url : urls) {
				IFile file = getFileFromUrl(url);
				if (file != null) {
					urls2.add(url);
				} else {
					IFile jsonFile = getFileFromUrl(_jsonURL);
					if (jsonFile != null) {
						IPath path = jsonFile.getParent().getFullPath()
								.append(url);
						file = ResourcesPlugin.getWorkspace().getRoot()
								.getFile(path);
						if (file.exists()) {
							urls2.add(ProjectCore.getAssetUrl(file));
						}
					}
				}
			}

			setUrls(urls2);
			return true;
		}
		return false;
	}

	@Override
	public IFile[] computeUsedFiles() {
		IFile[] superFiles = super.computeUsedFiles();
		List<IFile> list = new ArrayList<>(Arrays.asList(superFiles));
		list.addAll(getFilesFromUrls(_jsonURL));
		return list.toArray(new IFile[list.size()]);
	}

	@Override
	public List<AssetAudioSprite> getSubElements() {
		return getSpriteMap();
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "jsonURL", _jsonURL);

		buildSpriteMap();
	}
}
