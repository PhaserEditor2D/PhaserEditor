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
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.ui.FrameData;

public class SvgAssetModel extends AssetModel {

	public final class Frame implements IAssetFrameModel, IAssetElementModel {
		public Frame() {
		}

		@Override
		public String getKey() {
			return SvgAssetModel.this.getKey();
		}

		@Override
		public SvgAssetModel getAsset() {
			return SvgAssetModel.this;
		}

		@Override
		public IFile getImageFile() {
			return SvgAssetModel.this.getUrlFile();
		}

		@Override
		public FrameData getFrameData() {
			throw new UnsupportedOperationException("Not imeplemented yet");
//			Rectangle b = PhaserEditorUI.getImageBounds(getImageFile());
//			FrameData fd = new FrameData(0);
//			fd.src = b;
//			fd.dst = b;
//			fd.srcSize = new Point(b.width, b.height);
//			return fd;
		}

		@Override
		public String getName() {
			return getKey();
		}
	}

	private String _url;
	private Frame _frame;
	private ArrayList<IAssetElementModel> _elements;

	public SvgAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.svg, section);
	}

	public SvgAssetModel(JSONObject data, AssetSectionModel section) throws JSONException {
		super(data, section);
		readInfo(data);
	}

	@Override
	public void readInfo(JSONObject data) {
		_url = data.optString("url");
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("url", _url);
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
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
