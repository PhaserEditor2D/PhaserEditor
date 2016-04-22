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
package phasereditor.assetpack.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.boniatillo.json.JSONUtils;

/**
 * @author arian
 *
 */
public class VideoAssetModel extends AssetModel {
	private boolean _asBlob;
	private List<String> _urls;

	{
		_urls = new ArrayList<>();
		_asBlob = false;
	}

	public VideoAssetModel(JSONObject definition, AssetSectionModel section) throws JSONException {
		super(definition, section);
		JSONArray urls = definition.getJSONArray("urls");
		_urls = JSONUtils.toList(urls);
		_asBlob = definition.getBoolean("asBlob");
	}

	public VideoAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.video, section);
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
		obj.put("asBlob", _asBlob);
	}

	public void setUrlsJSONString(String urlsJSONString) throws JSONException {
		JSONArray urls = AudioAssetModel.parseUrlsJSONArray(urlsJSONString);
		setUrls(JSONUtils.toList(urls));
		firePropertyChange("urlsJSONString");
	}

	public String getUrlsJSONString() {
		JSONArray array = new JSONArray();
		for (String url : _urls) {
			array.put(url);
		}
		return array.toString(2);
	}

	@Override
	public IFile[] getUsedFiles() {
		List<IFile> files = getFilesFromUrls(_urls);
		return files.toArray(new IFile[files.size()]);
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrlList(problems, "url", _urls);
	}

	public boolean isAsBlob() {
		return _asBlob;
	}

	public void setAsBlob(boolean asBlob) {
		_asBlob = asBlob;
		firePropertyChange("asBlob");
	}

	public List<String> getUrls() {
		return _urls;
	}

	public void setUrls(List<String> urls) {
		_urls = urls;
		firePropertyChange("urls");
	}

	public List<IFile> getUrlFiles() {
		return getFilesFromUrls(_urls);
	}

	public IFile getFirstFile() {
		List<IFile> files = getUrlFiles();
		if (files == null || files.isEmpty()) {
			return null;
		}
		return files.get(0);
	}
}
