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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class AssetFactory {

	private static AssetFactory[] _cache;

	static {
		AssetType[] types = AssetType.values();
		_cache = new AssetFactory[types.length];

		cache(new AssetFactory(AssetType.image) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new ImageAssetModel(jsonDoc, section);
			}
		});

		cache(new AssetFactory(AssetType.spritesheet) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new SpritesheetAssetModel(jsonDoc, section);
			}

		});

		cache(new AssetFactory(AssetType.audio) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new AudioAssetModel(jsonDoc, section);
			}

		});

		cache(new AssetFactory(AssetType.video) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new VideoAssetModel(jsonDoc, section);
			}

		});

		cache(new AssetFactory(AssetType.audiosprite) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new AudioSpriteAssetModel(jsonDoc, section);
			}

		});

		cache(new AssetFactory(AssetType.tilemap) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new TilemapAssetModel(jsonDoc, section);
			}

		});

		cache(new AssetFactory(AssetType.bitmapFont) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new BitmapFontAssetModel(jsonDoc, section);
			}

		});

		cache(new AssetFactory(AssetType.physics) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new PhysicsAssetModel(jsonDoc, section);
			}

		});

		cache(new AssetFactory(AssetType.atlas) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				AtlasAssetModel asset = new AtlasAssetModel(jsonDoc, section);
				return asset;
			}

		});

		class TextAssetFactory extends AssetFactory {

			protected TextAssetFactory(AssetType type) {
				super(type);
			}

			public TextAssetFactory() {
				this(AssetType.text);
			}

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new TextAssetModel(jsonDoc, section);
			}

		}

		cache(new TextAssetFactory());

		cache(new TextAssetFactory(AssetType.json) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new JsonAssetModel(jsonDoc, section);
			}

		});

		cache(new TextAssetFactory(AssetType.xml) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new XmlAssetModel(jsonDoc, section);
			}

		});

		class ShaderAssetFactory extends TextAssetFactory {
			public ShaderAssetFactory() {
				super(AssetType.shader);
			}

			@Override
			public AssetModel createAsset(JSONObject jsonDef, AssetSectionModel section) throws Exception {
				return new ShaderAssetModel(jsonDef, section);
			}
		}
		cache(new ShaderAssetFactory());

		cache(new AssetFactory(AssetType.binary) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new BinaryAssetModel(jsonDoc, section);
			}

		});

		cache(new AssetFactory(AssetType.script) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new ScriptAssetModel(jsonDoc, section);
			}

		});
	}

	private static void cache(AssetFactory factory) {
		_cache[factory.getType().ordinal()] = factory;
	}

	public static AssetFactory[] getFactories() {
		return _cache;
	}

	public static AssetFactory getFactory(AssetType type) {
		return _cache[type.ordinal()];
	}

	static void initAudioFiles(AudioAssetModel asset, AssetPackModel pack) throws CoreException {
		List<IFile> files = pack.pickAudioFiles();
		if (!files.isEmpty()) {
			asset.setKey(pack.createKey(files.get(0)));
			List<String> urls = asset.getUrlsFromFiles(files);
			asset.setUrls(urls);
		}
	}

	static void initVideoFiles(VideoAssetModel asset, AssetPackModel pack) throws CoreException {
		List<IFile> files = pack.pickVideoFiles();
		if (!files.isEmpty()) {
			asset.setKey(pack.createKey(files.get(0)));
			List<String> urls = asset.getUrlsFromFiles(files);
			asset.setUrls(urls);
		}
	}

	private AssetType _type;

	public AssetFactory(AssetType type) {
		super();
		_type = type;
	}

	public AssetType getType() {
		return _type;
	}

	public String getLabel() {
		return _type.name();
	}

	public String getHelp() {
		try {
			return AssetModel.getHelp(_type);
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}

	public abstract AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception;
}
