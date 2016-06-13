// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.core;

import org.json.JSONObject;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;

/**
 * @author arian
 *
 */
public class ButtonSpriteModel extends AssetSpriteModel<IAssetKey> {

	public static final String TYPE_NAME = "button";

	private static final String DEF_FRAME = null;

	private IAssetFrameModel _overFrame;
	private IAssetFrameModel _outFrame;
	private IAssetFrameModel _downFrame;
	private IAssetFrameModel _upFrame;

	public ButtonSpriteModel(GroupModel parent, JSONObject obj) {
		super(parent, TYPE_NAME, obj);
	}

	public ButtonSpriteModel(GroupModel parent, IAssetFrameModel frame) {
		super(parent, frame, TYPE_NAME);
	}

	@Override
	protected void writeInfo(JSONObject jsonInfo) {
		super.writeInfo(jsonInfo);

		jsonInfo.put("overFrame", _overFrame == null ? null : _overFrame.getKey(), DEF_FRAME);
		jsonInfo.put("outFrame", _outFrame == null ? null : _outFrame.getKey(), DEF_FRAME);
		jsonInfo.put("downFrame", _downFrame == null ? null : _downFrame.getKey(), DEF_FRAME);
		jsonInfo.put("upFrame", _upFrame == null ? null : _upFrame.getKey(), DEF_FRAME);

	}

	@Override
	protected void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);

		_overFrame = findFrame(jsonInfo, "overFrame");
		_outFrame = findFrame(jsonInfo, "outFrame");
		_downFrame = findFrame(jsonInfo, "downFrame");
		_upFrame = findFrame(jsonInfo, "upFrame");

	}

	private IAssetFrameModel findFrame(JSONObject jsonInfo, String propKey) {
		String frameKey = jsonInfo.optString(propKey, DEF_FRAME);

		if (frameKey == null) {
			return null;
		}

		AssetModel asset = getAssetKey().getAsset();

		for (IAssetElementModel elem : asset.getSubElements()) {
			if (elem.getKey().equals(frameKey)) {
				return (IAssetFrameModel) elem;
			}
		}

		return null;
	}

	public void setFrame(IAssetFrameModel frame) {
		setAssetKey(frame);
	}

	public IAssetFrameModel getFrame() {
		return (IAssetFrameModel) getAssetKey();
	}

	public IAssetFrameModel getOverFrame() {
		return _overFrame;
	}

	public void setOverFrame(IAssetFrameModel overFrame) {
		_overFrame = overFrame;
	}

	public IAssetFrameModel getOutFrame() {
		return _outFrame;
	}

	public void setOutFrame(IAssetFrameModel outFrame) {
		_outFrame = outFrame;
	}

	public IAssetFrameModel getDownFrame() {
		return _downFrame;
	}

	public void setDownFrame(IAssetFrameModel downFrame) {
		_downFrame = downFrame;
	}

	public IAssetFrameModel getUpFrame() {
		return _upFrame;
	}

	public void setUpFrame(IAssetFrameModel upFrame) {
		_upFrame = upFrame;
	}

}
