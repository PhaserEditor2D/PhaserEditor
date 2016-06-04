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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONException;
import org.json.JSONObject;

public class ImageAssetModel extends AssetModel  {

	private String _url;
	private boolean _overwrite;

	{
		_overwrite = false;
	}

	public ImageAssetModel(String key, AssetSectionModel section)
			throws JSONException {
		super(key, AssetType.image, section);
	}

	public ImageAssetModel(JSONObject definition, AssetSectionModel section)
			throws JSONException {
		super(definition, section);
		_url = definition.getString("url");
		_overwrite = definition.getBoolean("overwrite");
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("url", _url);
		obj.put("overwrite", _overwrite);
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
		firePropertyChange("url");
	}

	public boolean isOverwrite() {
		return _overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		_overwrite = overwrite;
		firePropertyChange("overwrite");
	}

	@Override
	public IFile[] getUsedFiles() {
		return new IFile[] { getUrlFile() };
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);
	}
}
