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

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.AssetPackCore;

/**
 * @author arian
 *
 */
public class AnimationsModel {

	/**
	 * 
	 */
	public static final String PHASER_V3_ANIMATIONS_CONTENT_TYPE = "Phaser v3 Animations";
	private List<AnimationModel> _animations;
	private IFile _file;
	private String _dataKey;

	public AnimationsModel() {
		_animations = new ArrayList<>();
	}

	public AnimationsModel(JSONObject jsonData, String dataKey) {
		this();

		_dataKey = dataKey;

		var jsonData2 = jsonData;

		if (dataKey != null && dataKey.trim().length() > 0) {
			var keys = dataKey.split("\\.");
			for (var key : keys) {
				jsonData2 = jsonData2.getJSONObject(key);
			}
		}

		read(jsonData2);
	}

	public AnimationsModel(JSONObject jsonData) {
		this(jsonData, null);
	}

	public AnimationsModel(IFile file) throws Exception {
		this(file, null);
	}

	public AnimationsModel(IFile file, String dataKey) throws Exception {
		this(JSONObject.read(file), dataKey);
		_file = file;
	}

	public String getDataKey() {
		return _dataKey;
	}

	public IFile getFile() {
		return _file;
	}

	public void setFile(IFile file) {
		_file = file;
	}

	public AnimationModel createAnimation(JSONObject jsonData) {
		return new AnimationModel(this, jsonData);
	}

	public List<AnimationModel> getAnimations() {
		return _animations;
	}

	public void read(JSONObject data) {
		_animations = new ArrayList<>();

		var jsonAnims = data.getJSONArray("anims");
		for (int i = 0; i < jsonAnims.length(); i++) {
			var jsonAnim = jsonAnims.getJSONObject(i);
			var anim = createAnimation(jsonAnim);
			_animations.add(anim);
		}
	}

	public JSONObject toJSON() {
		var jsonData = new JSONObject();
		var jsonAnimations = new JSONArray();
		jsonData.put("anims", jsonAnimations);

		for (var anim : _animations) {
			var jsonAnim = anim.toJSON();
			jsonAnimations.put(jsonAnim);
		}

		var jsonMeta = new JSONObject();
		jsonMeta.put("app", "Phaser Editor v2");
		jsonMeta.put("contentType", PHASER_V3_ANIMATIONS_CONTENT_TYPE);
		jsonData.put("meta", jsonMeta);

		return jsonData;
	}

	public AnimationModel getAnimation(String key) {
		for (var anim : _animations) {
			if (anim.getKey().equals(key)) {
				return anim;
			}
		}
		return null;
	}

	public String getNewAnimationName(String name) {
		var names = _animations.stream().map(a -> a.getKey()).collect(toSet());

		var newName = name;

		int i = 1;

		while (names.contains(newName)) {
			newName = name + "_" + i;
			i++;
		}

		return newName;
	}

	public void build() {
		for (var anim : getAnimations()) {
			anim.build();
		}
	}

	public Set<IFile> computeUsedFiles() {
		var result = new HashSet<IFile>();

		if (_file != null) {
			result.add(_file);
		}

		for (var anim : _animations) {
			var animFiles = anim.computeUsedFiles();
			result.addAll(animFiles);
		}

		return result;
	}

	public AssetFinder getFinder() {
		return AssetPackCore.getAssetFinder(_file.getProject());
	}
}
