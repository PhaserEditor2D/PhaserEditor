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
package phasereditor.canvas.ui.shapes;

import org.json.JSONObject;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.SpritesheetShapeModel;

/**
 * A button based on a spritesheet. The frames are indexes of the sheet. It uses
 * a 'frame' property to denote the default index.
 * 
 * @author arian
 */
public class ButtonSpritesheetShapeModel extends SpritesheetShapeModel {

	private String _callback;
	private String _callbackContext;
	private Integer _overFrame;
	private Integer _outFrame;
	private Integer _downFrame;
	private Integer _upFrame;

	public ButtonSpritesheetShapeModel(GroupModel parent, JSONObject obj) {
		super(parent, obj);
	}

	public ButtonSpritesheetShapeModel(GroupModel parent, SpritesheetAssetModel asset) {
		super(parent, asset);
	}
	
	@Override
	public String getLabel() {
		return getEditorName() + "[btn]" ;
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

	public Integer getOverFrame() {
		return _overFrame;
	}

	public void setOverFrame(Integer overFrame) {
		_overFrame = overFrame;
	}

	public Integer getOutFrame() {
		return _outFrame;
	}

	public void setOutFrame(Integer outFrame) {
		_outFrame = outFrame;
	}

	public Integer getDownFrame() {
		return _downFrame;
	}

	public void setDownFrame(Integer downFrame) {
		_downFrame = downFrame;
	}

	public Integer getUpFrame() {
		return _upFrame;
	}

	public void setUpFrame(Integer upFrame) {
		_upFrame = upFrame;
	}

}
