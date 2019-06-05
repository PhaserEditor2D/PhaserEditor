// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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

/**
 * @author arian
 *
 */
public class MultiScriptAssetModel extends AssetModel {

	private List<String> _urls;

	public MultiScriptAssetModel(JSONObject jsonDef, AssetSectionModel section) throws JSONException {
		super(jsonDef, section);

		_urls = new ArrayList<>();

		var urlsData = jsonDef.getJSONArray("url");

		for (int i = 0; i < urlsData.length(); i++) {
			var url = urlsData.getString(i);
			_urls.add(url);
		}
	}

	public MultiScriptAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.scripts, section);
		_urls = new ArrayList<>();
	}

	public List<String> getUrls() {
		return _urls;
	}

	public void setUrls(List<String> urls) {
		_urls = urls;
	}

	@Override
	protected void internalBuild(List<IStatus> problems) {
		validateUrlList(problems, "url", _urls);
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		updateUrlsListWithFileChange(file, newFile, _urls);
	}

	@Override
	public IFile[] computeUsedFiles() {
		var files = getFilesFromUrls(_urls);
		return files.toArray(new IFile[files.size()]);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);

		var array = new JSONArray();
		for (var url : _urls) {
			array.put(url);
		}
		obj.put("url", array);
	}

}
