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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.phasereditor2d.json.JSONUtils;

public class AudioAssetModel extends AssetModel {

	private List<String> _urls;

	{
		_urls = new ArrayList<>();
	}

	public AudioAssetModel(JSONObject jsonData, AssetSectionModel section) throws JSONException {
		super(jsonData, section);
		JSONArray urls = jsonData.getJSONArray("urls");
		_urls = JSONUtils.toList(urls);
	}

	public AudioAssetModel(String key, AssetSectionModel section) throws JSONException {
		this(key, AssetType.audio, section);
	}

	protected AudioAssetModel(String key, AssetType type, AssetSectionModel section) throws JSONException {
		super(key, type, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		JSONArray urls = null;
		if (_urls != null) {
			urls = new JSONArray();
			for (String url : _urls) {
				urls.put(url);
			}
		}
		obj.put("urls", urls);
	}

	public List<String> getUrls() {
		return _urls;
	}

	public void setUrls(List<String> urls) {
		_urls = urls;
		firePropertyChange("urls");
	}

	public void setUrlsJSONString(String urlsJSONString) throws JSONException {
		JSONArray urls = parseUrlsJSONArray(urlsJSONString);
		setUrls(JSONUtils.toList(urls));
		firePropertyChange("urlsJSONString");
	}

	public static JSONArray parseUrlsJSONArray(String json) throws JSONException {
		try {
			JSONArray array = new JSONArray(new JSONTokener(new StringReader(json)));
			return array;
		} catch (JSONException e) {
			// ok, if we cannot parse it maybe it is a single url
			if (json.contains(",") || json.contains("\"") || json.contains("'") || json.contains("[")
					|| json.contains("]")) {
				// hum! with those chars we are sure it is a bad format, so
				// re-send the error.
				throw e;
			}
			JSONArray array = new JSONArray();
			array.put(json);
			return array;
		}
	}

	public String getUrlsJSONString() {
		JSONArray array = new JSONArray();
		for (String url : _urls) {
			array.put(url);
		}
		return array.toString(2);
	}

	@Override
	public IFile[] computeUsedFiles() {
		List<IFile> files = getFilesFromUrls(_urls);
		return files.toArray(new IFile[files.size()]);
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrlList(problems, "url", _urls);
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		String newUrl = getUrlFromFile(newFile);
		for (int i = 0; i < _urls.size(); i++) {
			String url2 = _urls.get(i);
			if (url.equals(url2)) {
				_urls.set(i, newUrl);
			}
		}
	}
}
