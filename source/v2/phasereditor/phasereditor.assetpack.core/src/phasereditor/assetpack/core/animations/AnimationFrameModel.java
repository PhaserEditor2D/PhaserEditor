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
package phasereditor.assetpack.core.animations;

import org.json.JSONObject;

import phasereditor.assetpack.core.IAssetFrameModel;

public class AnimationFrameModel{
	private IAssetFrameModel _frame;
	private String _textureKey;
	private Object _frameName;
	private int _duration;
	private double _computedFraction;

	public AnimationFrameModel() {

	}

	public AnimationFrameModel(JSONObject jsonData) {
		this();
		_textureKey = jsonData.getString("key");
		// a frame based on an image only needs the key
		_frameName = jsonData.opt("frame");
		_duration = jsonData.optInt("duration");
	}

	public IAssetFrameModel getFrameAsset() {
		return _frame;
	}

	public void setFrameAsset(IAssetFrameModel frame) {
		_frame = frame;
	}

	public Object getFrameName() {
		return _frameName;
	}

	public void setFrameName(Object frameName) {
		_frameName = frameName;
	}

	public String getTextureKey() {
		return _textureKey;
	}

	public void setTextureKey(String textureKey) {
		_textureKey = textureKey;
	}
	
	public int getDuration() {
		return _duration;
	}
	
	public void setDuration(int duration) {
		_duration = duration;
	}

	public void setComputedFraction(double fraction) {
		_computedFraction = fraction;
	}
	
	public double getComputedFraction() {
		return _computedFraction;
	}
}