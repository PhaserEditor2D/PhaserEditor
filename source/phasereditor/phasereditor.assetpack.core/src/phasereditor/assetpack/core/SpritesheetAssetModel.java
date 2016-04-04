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

public class SpritesheetAssetModel extends AssetModel {

	private String _url;
	private int _frameWidth;
	private int _frameHeight;
	private int _frameMax;
	private int _margin;
	private int _spacing;

	{
		_frameMax = -1;
		_margin = 0;
		_spacing = 0;
	}

	public SpritesheetAssetModel(JSONObject definition,
			AssetSectionModel section) throws JSONException {
		super(definition, section);
		_url = definition.optString("url", null);
		_frameWidth = definition.optInt("frameWidth", 0);
		_frameHeight = definition.optInt("frameHeight", 0);
		_frameMax = definition.optInt("frameMax", -1);
		_margin = definition.optInt("margin", 0);
		_spacing = definition.optInt("spacing", 0);
	}

	public SpritesheetAssetModel(String key, AssetSectionModel section)
			throws JSONException {
		super(key, AssetType.spritesheet, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("url", _url);
		obj.put("frameWidth", _frameWidth);
		obj.put("frameHeight", _frameHeight);
		obj.put("frameMax", _frameMax);
		obj.put("margin", _margin);
		obj.put("spacing", _spacing);
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

	@Override
	public IFile[] getUsedFiles() {
		return new IFile[] { getUrlFile() };
	}

	public int getFrameWidth() {
		return _frameWidth;
	}

	public void setFrameWidth(int frameWidth) {
		_frameWidth = frameWidth;
		firePropertyChange("frameWidth");
	}

	public int getFrameHeight() {
		return _frameHeight;
	}

	public void setFrameHeight(int frameHeight) {
		_frameHeight = frameHeight;
		firePropertyChange("frameHeight");
	}

	public int getFrameMax() {
		return _frameMax;
	}

	public void setFrameMax(int frameMax) {
		_frameMax = frameMax;
		firePropertyChange("margin");
	}

	public int getMargin() {
		return _margin;
	}

	public void setMargin(int margin) {
		_margin = margin;
		firePropertyChange("margin");
	}

	public int getSpacing() {
		return _spacing;
	}

	public void setSpacing(int spacing) {
		_spacing = spacing;
		firePropertyChange("spacing");
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);
	}

}
