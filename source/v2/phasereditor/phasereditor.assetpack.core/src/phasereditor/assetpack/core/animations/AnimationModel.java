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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class AnimationModel {
	private String _key;
	private List<AnimationFrameModel> _frames;
	
	public AnimationModel() {
		_frames = new ArrayList<>();
	}
	
	public AnimationModel(JSONObject jsonData) {
		this();
		
		_key = jsonData.getString("key");
		
		var defaultTextureKey = jsonData.optString("defaultTextureKey", null);
		
		//TODO: missing parse frames provided by a string literal (spritesheet).
		
		var jsonFrames = jsonData.getJSONArray("frames");
		
		for(int i = 0; i < jsonFrames.length(); i++) {
			var jsonFrame = jsonFrames.getJSONObject(i);
			if (!jsonFrame.has("key") && defaultTextureKey != null) {
				jsonFrame.put("key", defaultTextureKey);
			}
			
			var frame = new AnimationFrameModel(jsonFrame);
			_frames.add(frame);
		}
	}
	
	public String getKey() {
		return _key;
	}

	public List<AnimationFrameModel> getFrames() {
		return _frames;
	}
}