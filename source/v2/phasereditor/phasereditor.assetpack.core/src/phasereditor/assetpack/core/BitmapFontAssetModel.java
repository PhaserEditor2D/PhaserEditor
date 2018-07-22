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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.atlas.core.FrameData;
import phasereditor.bmpfont.core.BitmapFontModel;
import phasereditor.ui.PhaserEditorUI;

public class BitmapFontAssetModel extends AssetModel {

	private String _textureURL;
	private String _fontDataURL;
	private String _normalMap;

	public BitmapFontAssetModel(JSONObject jsonData, AssetSectionModel section) throws JSONException {
		super(jsonData, section);
		_textureURL = jsonData.optString("textureURL", null);
		_fontDataURL = jsonData.optString("fontDataURL", null);
		_normalMap = jsonData.optString("normalMap", null);
	}

	public BitmapFontAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.bitmapFont, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("textureURL", _textureURL);
		obj.put("fontDataURL", _fontDataURL);
		obj.put("normalMap", _normalMap);
	}

	@Override
	public IFile[] computeUsedFiles() {
		if (_normalMap == null) {
			return new IFile[] { getTextureFile(), getFontDataURLFile() };
		}

		return new IFile[] { getTextureFile(), getFontDataURLFile(), getNormalMapFile() };
	}

	public String getNormalMap() {
		return _normalMap;
	}

	public void setNormalMap(String normalMap) {
		_normalMap = normalMap;
		firePropertyChange("normalMap");
	}

	public IFile getNormalMapFile() {
		return getFileFromUrl(_normalMap);
	}

	public IFile getFontDataURLFile() {
		return getFileFromUrl(_fontDataURL);
	}

	public String getTextureURL() {
		return _textureURL;
	}

	public void setTextureURL(String textureURL) {
		_textureURL = textureURL;
		firePropertyChange("textureURL");
	}

	public IFile getTextureFile() {
		return getFileFromUrl(getTextureURL());
	}

	public String getFontDataURL() {
		return _fontDataURL;
	}

	public void setFontDataURL(String fontDataURL) {
		_fontDataURL = fontDataURL;
		firePropertyChange("fontDataURL");
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "textureURL", _textureURL);
		validateUrl(problems, "fontDataURL", _fontDataURL);
		if (_normalMap != null) {
			validateUrl(problems, "normalMap", _normalMap);
		}

		try {
			buildFrame();
		} catch (Exception e) {
			problems.add(errorStatus(e.getMessage()));
		}
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		String newUrl = getUrlFromFile(newFile);

		if (url.equals(_textureURL)) {
			_textureURL = newUrl;
		}

		if (url.equals(_fontDataURL)) {
			_fontDataURL = newUrl;
		}

		if (url.equals(_normalMap)) {
			_normalMap = newUrl;
		}
	}

	private Frame _frame;
	private ArrayList<IAssetElementModel> _elements;

	public final class Frame implements IAssetFrameModel, IAssetElementModel {
		private int _index;

		public Frame(int index) {
			_index = index;
		}

		@Override
		public String getKey() {
			return BitmapFontAssetModel.this.getKey();
		}

		@Override
		public AssetModel getAsset() {
			return BitmapFontAssetModel.this;
		}

		@Override
		public IFile getImageFile() {
			return BitmapFontAssetModel.this.getFileFromUrl(getFontDataURL());
		}

		@Override
		public FrameData getFrameData() {
			Rectangle b = PhaserEditorUI.getImageBounds(getImageFile());
			FrameData fd = new FrameData(_index);
			fd.src = b;
			fd.dst = b;
			fd.srcSize = new Point(b.width, b.height);
			return fd;
		}

		@Override
		public String getName() {
			return getKey();
		}
	}

	public Frame getFrame() {
		if (_frame == null) {
			buildFrame();
		}
		return _frame;
	}

	private synchronized void buildFrame() {
		_frame = new Frame(0);
		_elements = new ArrayList<>();
		_elements.add(_frame);
	}

	@Override
	public List<? extends IAssetElementModel> getSubElements() {
		if (_elements == null) {
			buildFrame();
		}

		return _elements;
	}

	public BitmapFontModel createFontModel() {
		IFile file = getFontDataURLFile();
		if (file == null) {
			return null;
		}
		try {
			return BitmapFontModel.createFromFile(file);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
