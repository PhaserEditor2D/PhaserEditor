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
public class HtmlTextureAssetModel extends AssetModel {
	private String _url;
	private int _width;
	private int _height;

	public HtmlTextureAssetModel(JSONObject jsonData, AssetSectionModel section) throws JSONException {
		super(jsonData, section);
		
		readInfo(jsonData);
	}

	@Override
	public void readInfo(JSONObject jsonData) {
		_url = jsonData.optString("url", null);
		_width = jsonData.optInt("width", 512);
		_height = jsonData.optInt("height", 512);
	}

	public HtmlTextureAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.htmlTexture, section);

		_width = 512;
		_height = 512;
	}

	@Override
	public IFile[] computeUsedFiles() {
		return new IFile[] { getFileFromUrl(_url) };
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);

		obj.put("url", _url);
		obj.put("width", _width, 512);
		obj.put("height", _height, 512);
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
	}

	public int getHeight() {
		return _height;
	}

	public void setHeight(int height) {
		_height = height;
	}

	public int getWidth() {
		return _width;
	}

	public void setWidth(int width) {
		_width = width;
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
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
