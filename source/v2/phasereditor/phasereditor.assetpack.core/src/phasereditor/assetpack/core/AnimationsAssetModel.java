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
package phasereditor.assetpack.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;

/**
 * @author arian
 *
 */
public class AnimationsAssetModel extends AssetModel {
	private String _url;
	private String _dataKey;
	private AnimationsModel_in_AssetPack _animationsModel;
	private List<AnimationModel_in_AssetPack> _animations;

	public AnimationsAssetModel(JSONObject jsonData, AssetSectionModel section) throws JSONException {
		super(jsonData, section);
		_url = jsonData.optString("url", null);
		_dataKey = jsonData.optString("dataKey", null);
	}

	protected AnimationsAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.animation, section);
	}

	@Override
	public IFile[] computeUsedFiles() {
		return new IFile[] { getFileFromUrl(_url) };
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);

		obj.put("url", _url);
		obj.put("dataKey", _dataKey);
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
		firePropertyChange("url");
	}

	public String getDataKey() {
		return _dataKey;
	}

	public void setDataKey(String dataKey) {
		_dataKey = dataKey;
		firePropertyChange("dataKey");
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);

		_animationsModel = null;
		_animations = List.of();

		IFile file = getUrlFile();

		if (file != null) {
			try (InputStream input = file.getContents()) {
				var jsonData = new JSONObject(new JSONTokener(input));

				try {

					if (_dataKey != null && _dataKey.trim().length() > 0) {
						var keys = _dataKey.split("\\.");
						for (var key : keys) {
							jsonData = jsonData.getJSONObject(key);
						}
					}
				} catch (Exception e) {
					problems.add(errorStatus(
							"The data key '" + _dataKey + "' is not found in the animation file '" + _url + "'."));
					throw e;
				}

				_animationsModel = new AnimationsModel_in_AssetPack(jsonData);
				_animationsModel.build(problems);
				_animations = _animationsModel.getAnimations_in_AssetPack();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void buildSecondPass(List<IStatus> problems) {
		if (_animationsModel != null) {
			_animationsModel.build(problems);
		}
	}

	@Override
	public List<AnimationModel_in_AssetPack> getSubElements() {
		if (_animationsModel == null) {
			build(new ArrayList<>());
		}

		return _animations;
	}

	public class AnimationsModel_in_AssetPack extends AnimationsModel {

		private List<AnimationModel_in_AssetPack> _animations_in_AssetPack;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public AnimationsModel_in_AssetPack(JSONObject jsonData) {
			super(jsonData);
			_animations_in_AssetPack = (List) getAnimations();
		}

		@Override
		protected AnimationModel createAnimation(JSONObject jsonData) {
			return new AnimationModel_in_AssetPack(jsonData);
		}

		public List<AnimationModel_in_AssetPack> getAnimations_in_AssetPack() {
			return _animations_in_AssetPack;
		}

		public void build(List<IStatus> problems) {
			for (var anim : getAnimations()) {
				Map<String, IAssetFrameModel> cache = new HashMap<>();

				for (var animFrame : anim.getFrames()) {

					var textureKey = animFrame.getTextureKey();
					var frameName = animFrame.getFrameName();

					var cacheKey = frameName + "@" + textureKey;
					var frame = cache.get(cacheKey);

					if (frame != null) {
						animFrame.setFrame(frame);
						continue;
					}

					frame = getPack().findFrame(textureKey, frameName);

					if (frame == null) {
						var packs = AssetPackCore.getAssetPackModels(getPack().getFile().getProject());
						for (var pack : packs) {
							frame = pack.findFrame(textureKey, frameName);
						}
					}

					if (frame == null) {
						problems.add(errorStatus(
								"Cannot find the frame '" + frameName + "' in the texture '" + textureKey + "'."));
					} else {
						cache.put(cacheKey, frame);
					}

					animFrame.setFrame(frame);

				}
			}
		}
	}

	public class AnimationModel_in_AssetPack extends AnimationModel implements IAssetElementModel {

		public AnimationModel_in_AssetPack(JSONObject jsonData) {
			super(jsonData);
		}

		@Override
		protected AnimationFrameModel createAnimationFrame(JSONObject jsonData) {
			return new AnimationFrameModel_in_AssetPack(jsonData);
		}

		@Override
		public String getName() {
			return this.getKey();
		}

		@Override
		public AssetModel getAsset() {
			return AnimationsAssetModel.this;
		}
	}

	public class AnimationFrameModel_in_AssetPack extends AnimationFrameModel implements IAssetElementModel {

		public AnimationFrameModel_in_AssetPack(JSONObject jsonData) {
			super(jsonData);
		}

		@Override
		public String getName() {
			return this.getFrameName() + "." + this.getTextureKey();
		}

		@Override
		public AssetModel getAsset() {
			return AnimationsAssetModel.this;
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
