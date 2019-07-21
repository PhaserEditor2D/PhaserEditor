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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class PluginAssetModel extends AssetModel {
	private String _url;
	private boolean _start;
	private String _mapping;

	public PluginAssetModel(JSONObject jsonDoc, AssetSectionModel section) throws JSONException {
		super(jsonDoc, section);
		readInfo(jsonDoc);
	}

	@Override
	public void readInfo(JSONObject jsonDoc) {
		_url = jsonDoc.optString("url", null);
		_start = jsonDoc.optBoolean("start", false);
		_mapping = jsonDoc.optString("mapping", null);
	}

	public PluginAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.plugin, section);
	}

	@Override
	public IFile[] computeUsedFiles() {
		return new IFile[] { getFileFromUrl(_url) };
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);

		obj.put("url", _url);
		obj.put("start", _start, false);
		obj.put("mapping", _mapping);

	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
	}

	public boolean isStart() {
		return _start;
	}

	public void setStart(boolean start) {
		_start = start;
	}

	public String getMapping() {
		return _mapping;
	}

	public void setMapping(String mapping) {
		_mapping = mapping;
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		if (url.equals(_url)) {
			_url = getUrlFromFile(newFile);
		}
	}
}
