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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;

/**
 * @author arian
 *
 */
public class AnimationModel implements Cloneable {
	private String _name;
	private List<IAssetFrameModel> _frames;
	private int _frameRate;
	private boolean _loop;

	public AnimationModel(String name) {
		_name = name;
		_frames = new ArrayList<>();
		_frameRate = 60;
		_loop = false;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public List<IAssetFrameModel> getFrames() {
		return _frames;
	}

	public void setFrames(List<IAssetFrameModel> frames) {
		_frames = frames;
	}

	public int getFrameRate() {
		return _frameRate;
	}

	public void setFrameRate(int frameRate) {
		_frameRate = frameRate;
	}

	public boolean isLoop() {
		return _loop;
	}

	public void setLoop(boolean loop) {
		_loop = loop;
	}

	public void write(JSONObject obj) {
		obj.put("name", _name);
		obj.put("frameRate", _frameRate);
		obj.put("loop", _loop);
		{
			JSONArray array = new JSONArray();
			for (IAssetFrameModel frame : _frames) {
				array.put(frame.getKey());
			}
			obj.put("frames", array);
		}
	}

	public void read(IAssetKey key, JSONObject obj) {
		_name = obj.getString("name");
		_frameRate = obj.getInt("frameRate");
		_loop = obj.getBoolean("loop");
		_frames = new ArrayList<>();
		{
			JSONArray array = obj.getJSONArray("frames");
			for (int i = 0; i < array.length(); i++) {
				String fname = array.getString(i);
				for (IAssetElementModel elem : key.getAsset().getSubElements()) {
					if (elem.getName().equals(fname)) {
						_frames.add((IAssetFrameModel) elem);
					}
				}
			}
		}
	}

	@Override
	public AnimationModel clone() {
		AnimationModel model = new AnimationModel(_name);
		model._frameRate = _frameRate;
		model._loop = _loop;
		model._frames = new ArrayList<>(_frames);
		return model;
	}

	public void rebuild(IAssetKey key) {
		List<? extends IAssetElementModel> allframes = key.getAllFrames();
		List<IAssetFrameModel> list = new ArrayList<>();
		for (IAssetFrameModel frame : _frames) {
			for (IAssetElementModel elem : allframes) {
				if (elem.getKey().equals(frame.getKey())) {
					list.add((IAssetFrameModel) elem);
				}
			}
		}
		_frames.clear();
		_frames.addAll(list);
	}
}
