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

	/**
	 * 
	 */
	private static final String DEF_CALLBACK_CONTEXT = "this";

	public static final String TYPE_NAME = "button";

	private static final String DEF_FRAME = null;

	private IAssetFrameModel _overFrame;
	private IAssetFrameModel _downFrame;
	private IAssetFrameModel _upFrame;
	private String _callback;
	private String _callbackContext;

	public ButtonSpriteModel(GroupModel parent, JSONObject obj) {
		super(parent, TYPE_NAME, obj);
	}

	public ButtonSpriteModel(GroupModel parent, IAssetFrameModel frame) {
		super(parent, frame, TYPE_NAME);
		_callbackContext = DEF_CALLBACK_CONTEXT;
	}

	@Override
	protected void writeInfo(JSONObject jsonInfo, boolean saving) {
		super.writeInfo(jsonInfo, saving);

		jsonInfo.put("callback", _callback, null);
		jsonInfo.put("callbackContext", _callbackContext, "this");
		
		if (isOverriding("texture")) {
			jsonInfo.put("overFrame", _overFrame == null ? null : _overFrame.getKey(), DEF_FRAME);
			jsonInfo.put("downFrame", _downFrame == null ? null : _downFrame.getKey(), DEF_FRAME);
			jsonInfo.put("upFrame", _upFrame == null ? null : _upFrame.getKey(), DEF_FRAME);
		}

	}

	@Override
	protected void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);

		_callback = jsonInfo.optString("callback", null);
		_callbackContext = jsonInfo.optString("callbackContext", "this");
		_overFrame = findFrame(jsonInfo, "overFrame");
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
		// the out frame is the sprite frame
		return (IAssetFrameModel) getAssetKey();
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

	public String getCallback() {
		return _callback;
	}

	public void setCallback(String callback) {
		_callback = callback;
	}

	public String getCallbackContext() {
		return _callbackContext;
	}

	public void setCallbackContext(String callbackContext) {
		_callbackContext = callbackContext;
	}

	@Override
	public void build() {
		super.build();

		_overFrame = (IAssetFrameModel) buildAssetKey(_overFrame);
		_downFrame = (IAssetFrameModel) buildAssetKey(_downFrame);
		_upFrame = (IAssetFrameModel) buildAssetKey(_upFrame);
	}

}
