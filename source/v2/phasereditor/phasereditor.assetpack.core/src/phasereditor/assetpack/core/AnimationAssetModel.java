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

import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author arian
 *
 */
public class AnimationAssetModel extends AssetModel {
	private String _url;
	private String _dataKey;

	public AnimationAssetModel(JSONObject jsonData, AssetSectionModel section) throws JSONException {
		super(jsonData, section);
		_url = jsonData.optString("url", null);
		_dataKey = jsonData.optString("dataKey", null);
	}

	protected AnimationAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.animation, section);
	}

	@Override
	public IFile[] computeUsedFiles() {
		return new IFile[] { getFileFromUrl(_url) };
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		
		obj.put("url", _url);
		obj.put("dataKey", _dataKey);
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
		firePropertyChange("url");
	}

	public String getDataKey() {
		return _dataKey;
	}

	public void setDataKey(String dataKey) {
		_dataKey = dataKey;
		firePropertyChange("dataKey");
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);

		if (_dataKey != null && _dataKey.trim().length() > 0) {
			IFile file = getUrlFile();
			if (file != null) {
				try (InputStream input = file.getContents()) {
					var obj = new JSONObject(new JSONTokener(input));
					var keys = _dataKey.split("\\.");
					for(var key : keys) {
						obj = obj.getJSONObject(key);
					}
				} catch (Exception e) {
					problems.add(errorStatus(
							"The data key '" + _dataKey + "' is not found in the animation file '" + _url + "'."));
				}
			}
		}
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		if (url.equals(_url)) {
			_url = getUrlFromFile(newFile);
		}
	}
}
