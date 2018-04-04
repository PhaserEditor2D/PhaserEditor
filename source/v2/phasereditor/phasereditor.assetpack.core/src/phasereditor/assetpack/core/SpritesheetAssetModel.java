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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.json.JSONException;
import org.json.JSONObject;

public class SpritesheetAssetModel extends AssetModel {

	private String _url;
	private int _frameWidth;
	private int _frameHeight;
	private int _frameMax;
	private int _margin;
	private int _spacing;
	private List<FrameModel> _frames;

	{
		_frameMax = -1;
		_margin = 0;
		_spacing = 0;
		_frames = null;
	}

	public SpritesheetAssetModel(JSONObject definition, AssetSectionModel section) throws JSONException {
		super(definition, section);
		_url = definition.optString("url", null);
		_frameWidth = definition.optInt("frameWidth", 0);
		_frameHeight = definition.optInt("frameHeight", 0);
		_frameMax = definition.optInt("frameMax", -1);
		_margin = definition.optInt("margin", 0);
		_spacing = definition.optInt("spacing", 0);
	}

	public SpritesheetAssetModel(String key, AssetSectionModel section) throws JSONException {
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
	public IFile[] computeUsedFiles() {
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

	public List<FrameModel> getFrames() {
		if (_frames == null) {
			buildFrames();
		}
		return _frames;
	}

	@Override
	public List<? extends IAssetElementModel> getSubElements() {
		return getFrames();
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);

		buildFrames();
	}

	public static class FrameModel implements IAssetElementModel, IAssetFrameModel {
		public Rectangle _bounds;
		private SpritesheetAssetModel _asset;
		private int _index;

		public FrameModel(SpritesheetAssetModel asset, int index, Rectangle bounds) {
			super();
			_asset = asset;
			_index = index;
			_bounds = bounds;
		}

		@Override
		public FrameData getFrameData() {
			FrameData data = new FrameData();
			data.src = _bounds;
			data.dst = new Rectangle(0, 0, _bounds.width, _bounds.height);
			data.srcSize = new Point(_bounds.width, _bounds.height);
			return data;
		}

		@Override
		public IFile getImageFile() {
			return _asset.getUrlFile();
		}

		public int getIndex() {
			return _index;
		}

		public Rectangle getBounds() {
			return _bounds;
		}

		@Override
		public String getName() {
			return Integer.toString(_index);
		}

		@Override
		public SpritesheetAssetModel getAsset() {
			return _asset;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}
	}

	private void buildFrames() {
		// taken from AssetPackUI.generateSpriteSheetRects(...) method.

		List<FrameModel> list = new ArrayList<>();
		try {
			int w = getFrameWidth();
			int h = getFrameHeight();
			int margin = getMargin();
			int spacing = getSpacing();

			if (w <= 0 || h <= 0 || spacing < 0 || margin < 0) {
				// invalid parameters
				return;
			}

			IFile file = getUrlFile();
			if (file == null) {
				return;
			}

			Rectangle b;
			try (InputStream contents = file.getContents()) {
				ImageData data = new ImageData(contents);
				b = new Rectangle(0, 0, data.width, data.height);
			} catch (IOException | CoreException e) {
				e.printStackTrace();
				return;
			}

			int max = getFrameMax();
			if (max <= 0) {
				max = Integer.MAX_VALUE;
			}

			int i = 0;
			int x = margin;
			int y = margin;
			while (true) {
				if (i >= max || y >= b.height) {
					break;
				}

				FrameModel frame = new FrameModel(this, i, new Rectangle(x, y, w, h));

				list.add(frame);

				x += w + spacing;
				if (x >= b.width) {
					x = margin;
					y += h + spacing;
				}
				i++;
			}
		} finally {
			_frames = list;
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
