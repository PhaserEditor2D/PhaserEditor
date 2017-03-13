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

import phasereditor.ui.PhaserEditorUI;

public class ImageAssetModel extends AssetModel {

	public final class Frame implements IAssetFrameModel, IAssetElementModel {
		public Frame() {
		}

		@Override
		public String getKey() {
			return ImageAssetModel.this.getKey();
		}

		@Override
		public AssetModel getAsset() {
			return ImageAssetModel.this;
		}

		@Override
		public IFile getImageFile() {
			return ImageAssetModel.this.getUrlFile();
		}

		@Override
		public FrameData getFrameData() {
			Rectangle b = PhaserEditorUI.getImageBounds(getImageFile());
			FrameData fd = new FrameData();
			fd.src = b;
			fd.dst = b;
			fd.srcSize = new Point(b.width, b.height);
			return fd;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public String getName() {
			return getKey();
		}
	}

	private String _url;
	private boolean _overwrite;
	private Frame _frame;
	private ArrayList<IAssetElementModel> _elements;

	{
		_overwrite = false;
	}

	public ImageAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.image, section);
	}

	public ImageAssetModel(JSONObject definition, AssetSectionModel section) throws JSONException {
		super(definition, section);
		_url = definition.optString("url");
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
	public IFile[] computeUsedFiles() {
		return new IFile[] { getUrlFile() };
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);
		try {
			buildFrame();
		} catch (Exception e) {
			problems.add(errorStatus(e.getMessage()));
		}
	}

	public Frame getFrame() {
		if (_frame == null) {
			buildFrame();
		}
		return _frame;
	}

	@Override
	public List<? extends IAssetElementModel> getSubElements() {
		if (_elements == null) {
			buildFrame();
		}

		return _elements;
	}

	private synchronized void buildFrame() {
		_frame = new Frame();
		_elements = new ArrayList<>();
		_elements.add(_frame);
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		if (url.equals(_url)) {
			_url = getUrlFromFile(newFile);
		}
	}
}
