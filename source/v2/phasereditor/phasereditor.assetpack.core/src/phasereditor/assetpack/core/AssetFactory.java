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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.atlas.core.AtlasCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.PhaserEditorUI;

public abstract class AssetFactory {

	private static AssetFactory[] _cache;

	static {
		AssetType[] types = AssetType.values();
		_cache = new AssetFactory[types.length];

		cache(new AssetFactory(AssetType.image) {
			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				AssetPackModel pack = section.getPack();
				ImageAssetModel asset = new ImageAssetModel(key, section);
				IFile file = pack.pickImageFile();
				if (file != null) {
					asset.setKey(pack.createKey(file));
					asset.setUrl(ProjectCore.getAssetUrl(file));
				}
				return asset;
			}

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

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				AssetPackModel pack = section.getPack();
				SpritesheetAssetModel asset = new SpritesheetAssetModel(key, section);
				IFile file = pack.pickImageFile();
				if (file != null) {
					asset.setKey(pack.createKey(file));
					asset.setUrl(ProjectCore.getAssetUrl(file));
				}
				return asset;
			}
		});

		cache(new AssetFactory(AssetType.audio) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new AudioAssetModel(jsonDoc, section);
			}

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				AudioAssetModel asset = new AudioAssetModel(key, section);
				AssetPackModel pack = section.getPack();
				initAudioFiles(asset, pack);
				return asset;
			}
		});

		cache(new AssetFactory(AssetType.video) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new VideoAssetModel(jsonDoc, section);
			}

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				VideoAssetModel asset = new VideoAssetModel(key, section);
				AssetPackModel pack = section.getPack();
				initVideoFiles(asset, pack);
				return asset;
			}
		});

		cache(new AssetFactory(AssetType.audiosprite) {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new AudioSpriteAssetModel(jsonDoc, section);
			}

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				AudioSpriteAssetModel asset = new AudioSpriteAssetModel(key, section);
				AssetPackModel pack = section.getPack();
				// pick an audiosprite json file
				IFile file = pack.pickAudioSpriteFile();
				if (file == null) {
					// there is not any audiosprite file, then try with an
					// audio file
					initAudioFiles(asset, pack);
				} else {
					// ok, there is an audiosprite json file, use it.
					asset.setKey(pack.createKey(file));
					asset.setJsonURLFile(file);
					asset.setUrlsFromJsonResources();
				}
				return asset;
			}
		});

		cache(new AssetFactory(AssetType.tilemap) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new TilemapAssetModel(jsonDoc, section);
			}

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				AssetPackModel pack = section.getPack();
				TilemapAssetModel asset = new TilemapAssetModel(key, section);
				IFile file = pack.pickTilemapFile();
				if (file != null) {
					asset.setKey(pack.createKey(file));
					asset.setUrl(ProjectCore.getAssetUrl(file));
					String ext = file.getFileExtension().toLowerCase();
					if (ext.equals("csv")) {
						asset.setFormat(TilemapAssetModel.TILEMAP_CSV);
					} else {
						asset.setFormat(TilemapAssetModel.TILEMAP_TILED_JSON);
					}
				}

				return asset;
			}
		});

		cache(new AssetFactory(AssetType.bitmapFont) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new BitmapFontAssetModel(jsonDoc, section);
			}

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				BitmapFontAssetModel asset = new BitmapFontAssetModel(key, section);
				AssetPackModel pack = section.getPack();
				IFile file = pack.pickBitmapFontFile();
				if (file != null) {
					asset.setKey(pack.createKey(file));
					asset.setAtlasURL(ProjectCore.getAssetUrl(file));

					String name = PhaserEditorUI.getNameFromFilename(file.getName());
					IFile imgFile = file.getParent().getFile(new Path(name + ".png"));
					if (imgFile.exists()) {
						asset.setTextureURL(asset.getUrlFromFile(imgFile));
					}
				}
				pack.pickFile(null);

				return asset;
			}
		});

		cache(new AssetFactory(AssetType.physics) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new PhysicsAssetModel(jsonDoc, section);
			}

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				// TODO: discover physics files
				return new PhysicsAssetModel(key, section);
			}
		});

		cache(new AtlasAssetFactory(AssetType.atlas));

		cache(new AtlasAssetFactory(AssetType.atlasXML));
		
		cache(new MultiAtlasAssetFactory());

		cache(new TextAssetFactory());

		cache(new TextAssetFactory(AssetType.json, "json") {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new JsonAssetModel(jsonDoc, section);
			}

			@Override
			protected TextAssetModel makeAsset(String key, AssetSectionModel section) {
				return new JsonAssetModel(key, section);
			}
		});

		cache(new TextAssetFactory(AssetType.xml, "xml") {

			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new XmlAssetModel(jsonDoc, section);
			}

			@Override
			protected TextAssetModel makeAsset(String key, AssetSectionModel section) {
				return new XmlAssetModel(key, section);
			}

		});

		cache(new ShaderAssetFactory());

		cache(new AssetFactory(AssetType.binary) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new BinaryAssetModel(jsonDoc, section);
			}

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				return new BinaryAssetModel(key, section);
			}
		});

		cache(new AssetFactory(AssetType.script) {
			@Override
			public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
				return new ScriptAssetModel(jsonDoc, section);
			}

			@Override
			public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
				return new ScriptAssetModel(key, section);
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

	/**
	 * Create a new asset.
	 * 
	 * @param model
	 *            The pack model.
	 * @param key
	 *            The key of the new asset.
	 * @return The new asset.
	 * @throws Exception
	 */
	public abstract AssetModel createAsset(String key, AssetSectionModel section) throws Exception;

	public abstract AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception;
}

class MultiAtlasAssetFactory extends AssetFactory {

	public MultiAtlasAssetFactory() {
		super(AssetType.multiatlas);
	}

	@Override
	public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
		AssetPackModel pack = section.getPack();
		var asset = new MultiAtlasAssetModel(key, section);

		IFile file = pack.pickFile(pack.discoverAtlasFiles(getType()));

		if (file != null) {
			asset.setKey(pack.createKey(file));
			asset.setAtlasURL(ProjectCore.getAssetUrl(file));
			asset.setPath(ProjectCore.getAssetUrl(file.getProject(), file.getParent().getFullPath()));
			asset.build(new ArrayList<>());
		}

		return asset;
	}

	@Override
	public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
		return new MultiAtlasAssetModel(jsonData, section);
	}
}

class AtlasAssetFactory extends AssetFactory {
	AtlasAssetFactory(AssetType type) {
		super(type);
	}

	@Override
	public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
		AtlasAssetModel asset = new AtlasAssetModel(jsonDoc, section);
		return asset;
	}

	@Override
	public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
		AssetPackModel pack = section.getPack();
		AtlasAssetModel asset = new AtlasAssetModel(getType(), key, section);

		IFile file = pack.pickFile(pack.discoverAtlasFiles(getType()));

		if (file == null) {
			file = pack.pickImageFile();
			if (file != null) {
				asset.setKey(pack.createKey(file));
				asset.setTextureURL(ProjectCore.getAssetUrl(file));
			}
		} else {
			asset.setKey(pack.createKey(file));
			String format = AtlasCore.getAtlasFormat(file);
			if (format != null) {
				asset.setFormat(format);
			}
			asset.setAtlasURL(ProjectCore.getAssetUrl(file));
			String name = PhaserEditorUI.getNameFromFilename(file.getName());
			IFile imgFile = file.getParent().getFile(new Path(name + ".png"));
			if (imgFile.exists()) {
				asset.setTextureURL(asset.getUrlFromFile(imgFile));
			}
		}

		return asset;
	}
}

class TextAssetFactory extends AssetFactory {

	private String[] _exts;

	protected TextAssetFactory(AssetType type, String... exts) {
		super(type);
		_exts = exts;
	}

	public TextAssetFactory() {
		this(AssetType.text, "txt", "text");
	}

	@Override
	public AssetModel createAsset(JSONObject jsonDoc, AssetSectionModel section) throws Exception {
		return new TextAssetModel(jsonDoc, section);
	}

	@Override
	public AssetModel createAsset(String key, AssetSectionModel section) throws Exception {
		AssetPackModel pack = section.getPack();
		TextAssetModel asset = makeAsset(key, section);
		List<IFile> files = pack.discoverTextFiles(_exts);
		IFile file = pack.pickFile(files);
		if (file != null) {
			asset.setKey(pack.createKey(file));
			asset.setUrl(ProjectCore.getAssetUrl(file));
		}
		return asset;
	}

	@SuppressWarnings("static-method")
	protected TextAssetModel makeAsset(String key, AssetSectionModel section) {
		return new TextAssetModel(key, section);
	}
}

class ShaderAssetFactory extends TextAssetFactory {
	public ShaderAssetFactory() {
		super(AssetType.shader, "vert", "frag", "tesc", "tese", "geom", "comp");
	}

	@Override
	public AssetModel createAsset(JSONObject jsonDef, AssetSectionModel section) throws Exception {
		return new ShaderAssetModel(jsonDef, section);
	}

	@Override
	protected ShaderAssetModel makeAsset(String key, AssetSectionModel section) {
		return new ShaderAssetModel(key, section);
	}
}
