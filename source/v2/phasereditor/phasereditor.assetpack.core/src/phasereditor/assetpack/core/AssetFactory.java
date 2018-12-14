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
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.atlas.core.AtlasCore;
import phasereditor.project.core.ProjectCore;

public abstract class AssetFactory {

	private static AssetFactory[] _cache;

	static {
		AssetType[] types = AssetType.values();
		_cache = new AssetFactory[types.length];

		cache(new AssetFactory(AssetType.image) {
			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();
				ImageAssetModel asset = new ImageAssetModel(pack.createKey(file), section);
				asset.setUrl(ProjectCore.getAssetUrl(file));
				return asset;
			}

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new ImageAssetModel(jsonData, section);
			}
		});

		cache(new AssetFactory(AssetType.svg) {
			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();

				var asset = new SvgAssetModel(pack.createKey(file), section);

				asset.setUrl(ProjectCore.getAssetUrl(file));

				return asset;
			}

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new SvgAssetModel(jsonData, section);
			}
		});

		cache(new AssetFactory(AssetType.animation) {
			@Override
			public AnimationsAssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();
				var asset = new AnimationsAssetModel(pack.createKey(file), section);

				asset.setUrl(ProjectCore.getAssetUrl(file));

				return asset;
			}

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new AnimationsAssetModel(jsonData, section);
			}
		});

		cache(new AssetFactory(AssetType.spritesheet) {
			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new SpritesheetAssetModel(jsonData, section);
			}

			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();

				SpritesheetAssetModel asset = new SpritesheetAssetModel(pack.createKey(file), section);

				asset.setUrl(ProjectCore.getAssetUrl(file));

				return asset;
			}
		});

		cache(new AssetFactory(AssetType.audio) {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new AudioAssetModel(jsonData, section);
			}

			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {

				AssetPackModel pack = section.getPack();

				AudioAssetModel asset = new AudioAssetModel(pack.createKey(file), section);

				initAudioFiles(asset, pack);

				return asset;
			}
		});

		cache(new AssetFactory(AssetType.video) {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new VideoAssetModel(jsonData, section);
			}

			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();

				VideoAssetModel asset = new VideoAssetModel(pack.createKey(file), section);

				initVideoFiles(asset, pack);

				return asset;
			}
		});

		cache(new AssetFactory(AssetType.audioSprite) {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new AudioSpriteAssetModel(jsonData, section);
			}

			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();
				AudioSpriteAssetModel asset = new AudioSpriteAssetModel(pack.createKey(file), section);

				asset.setJsonURLFile(file);
				asset.setUrlsFromJsonResources();

				return asset;
			}
		});

		cache(new TilemapAssetFactory(AssetType.tilemapCSV));

		cache(new TilemapAssetFactory(AssetType.tilemapTiledJSON));

		cache(new TilemapAssetFactory(AssetType.tilemapImpact));

		cache(new AssetFactory(AssetType.bitmapFont) {
			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new BitmapFontAssetModel(jsonData, section);
			}

			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();
				BitmapFontAssetModel asset = new BitmapFontAssetModel(pack.createKey(file), section);

				asset.setFontDataURL(ProjectCore.getAssetUrl(file));

				IFile imgFile = discoverSiblingWithExtensions(file, AssetPackCore.IMAGE_EXTS);

				if (imgFile.exists()) {
					asset.setTextureURL(asset.getUrlFromFile(imgFile));
				}

				pack.pickFile(null);

				return asset;
			}
		});

		cache(new AssetFactory(AssetType.physics) {
			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new PhysicsAssetModel(jsonData, section);
			}

			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				var pack = section.getPack();
				return new PhysicsAssetModel(pack.createKey(file), section);
			}
		});

		cache(new AtlasAssetFactory(AssetType.atlas));

		cache(new AtlasAssetFactory(AssetType.atlasXML));

		cache(new AtlasAssetFactory(AssetType.unityAtlas));

		cache(new MultiAtlasAssetFactory());

		cache(new AbstractFileAssetFactory(AssetType.text, "txt", "text") {
			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new TextAssetModel(jsonData, section);
			}

			@Override
			protected AbstractFileAssetModel makeAsset(String key, AssetSectionModel section) {
				return new TextAssetModel(key, section);
			}
		});

		cache(new AbstractFileAssetFactory(AssetType.json, "json") {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new JsonAssetModel(jsonData, section);
			}

			@Override
			protected AbstractFileAssetModel makeAsset(String key, AssetSectionModel section) {
				return new JsonAssetModel(key, section);
			}
		});

		cache(new AbstractFileAssetFactory(AssetType.xml, "xml") {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new XmlAssetModel(jsonData, section);
			}

			@Override
			protected AbstractFileAssetModel makeAsset(String key, AssetSectionModel section) {
				return new XmlAssetModel(key, section);
			}

		});

		cache(new AbstractFileAssetFactory(AssetType.html, "html") {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new HtmlAssetModel(jsonData, section);
			}

			@Override
			protected AbstractFileAssetModel makeAsset(String key, AssetSectionModel section) {
				return new HtmlAssetModel(key, section);
			}

		});

		cache(new ShaderAssetFactory());

		cache(new AbstractFileAssetFactory(AssetType.binary, "dat") {
			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new BinaryAssetModel(jsonData, section);
			}

			@Override
			protected AbstractFileAssetModel makeAsset(String key, AssetSectionModel section) {
				return new BinaryAssetModel(key, section);
			}
		});

		cache(new AbstractFileAssetFactory(AssetType.script, "js") {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new ScriptAssetModel(jsonData, section);
			}

			@Override
			protected AbstractFileAssetModel makeAsset(String key, AssetSectionModel section) {
				return new ScriptAssetModel(key, section);
			}
		});

		cache(new AssetFactory(AssetType.plugin) {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new PluginAssetModel(jsonData, section);
			}

			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();
				var asset = new PluginAssetModel(pack.createKey(file), section);
				asset.setUrl(ProjectCore.getAssetUrl(file));
				return asset;
			}
		});

		cache(new AssetFactory(AssetType.scenePlugin) {

			@Override
			public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
				return new ScenePluginAssetModel(jsonData, section);
			}

			@Override
			public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
				AssetPackModel pack = section.getPack();

				var key = pack.createKey(file);

				var asset = new ScenePluginAssetModel(key, section);

				asset.setUrl(ProjectCore.getAssetUrl(file));
				asset.setSystemKey("plugin" + key.substring(0, 1).toUpperCase() + key.substring(1));
				asset.setSceneKey(key);

				return asset;
			}
		});

		cache(new HtmlTextureAssetFactory());
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
	public abstract AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception;

	public abstract AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception;

	public static class MultiAtlasAssetFactory extends AssetFactory {

		public MultiAtlasAssetFactory() {
			super(AssetType.multiatlas);
		}

		@Override
		public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
			var pack = section.getPack();

			var asset = new MultiAtlasAssetModel(pack.createKey(file), section);

			asset.setUrl(ProjectCore.getAssetUrl(file));
			asset.setPath(ProjectCore.getAssetUrl(file.getProject(), file.getParent().getFullPath()));
			asset.build(new ArrayList<>());

			return asset;
		}

		@Override
		public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
			return new MultiAtlasAssetModel(jsonData, section);
		}
	}

	public static class TilemapAssetFactory extends AssetFactory {

		public TilemapAssetFactory(AssetType type) {
			super(type);
		}

		@Override
		public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
			return new TilemapAssetModel(jsonData, section);
		}

		@Override
		public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
			AssetPackModel pack = section.getPack();
			TilemapAssetModel asset = new TilemapAssetModel(pack.createKey(file), getType(), section);

			asset.setUrl(ProjectCore.getAssetUrl(file));

			return asset;
		}
	}

	public static class AtlasAssetFactory extends AssetFactory {
		AtlasAssetFactory(AssetType type) {
			super(type);
		}

		@Override
		public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
			AtlasAssetModel asset = new AtlasAssetModel(jsonData, section);
			return asset;
		}

		@Override
		public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
			var pack = section.getPack();
			var asset = new AtlasAssetModel(getType(), pack.createKey(file), section);

			var format = AtlasCore.getAtlasFormat(file);

			if (format != null) {
				asset.setFormat(format);
			}

			asset.setAtlasURL(ProjectCore.getAssetUrl(file));

			var imgFile = discoverSiblingWithExtensions(file, AssetPackCore.IMAGE_EXTS);

			if (imgFile.exists()) {
				asset.setTextureURL(asset.getUrlFromFile(imgFile));
			}

			return asset;
		}
	}

	private static IFile discoverSiblingWithExtensions(IFile file, String... exts) {
		var project = file.getProject();
		var filepath = file.getProjectRelativePath();

		var sibling = file;

		// try by replacing the extension

		for (var ext : exts) {
			sibling = project.getFile(filepath.removeFileExtension().addFileExtension(ext));

			if (sibling.exists()) {
				return sibling;
			}
		}

		// try by removing the extension

		sibling = project.getFile(filepath.removeFileExtension());

		if (sibling.exists()) {
			return sibling;
		}

		// try by adding the extension

		for (var ext : exts) {
			sibling = project.getFile(filepath.addFileExtension(ext));

			if (sibling.exists()) {
				return sibling;
			}
		}

		return sibling;
	}

	public static class HtmlTextureAssetFactory extends AssetFactory {

		protected HtmlTextureAssetFactory() {
			super(AssetType.htmlTexture);
		}

		@Override
		public AssetModel createAsset(JSONObject jsonData, AssetSectionModel section) throws Exception {
			return new HtmlTextureAssetModel(jsonData, section);
		}

		@Override
		public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
			AssetPackModel pack = section.getPack();
			HtmlTextureAssetModel asset = new HtmlTextureAssetModel(pack.createKey(file), section);

			asset.setUrl(ProjectCore.getAssetUrl(file));

			return asset;
		}

	}

	public static abstract class AbstractFileAssetFactory extends AssetFactory {

		private String[] _exts;

		protected AbstractFileAssetFactory(AssetType type, String... exts) {
			super(type);
			_exts = exts;
		}

		@Override
		public AssetModel createAsset(AssetSectionModel section, IFile file) throws Exception {
			AssetPackModel pack = section.getPack();

			AbstractFileAssetModel asset = makeAsset(pack.createKey(file), section);

			asset.setKey(pack.createKey(file));
			asset.setUrl(ProjectCore.getAssetUrl(file));

			return asset;
		}

		public String[] getExtensions() {
			return _exts;
		}

		protected abstract AbstractFileAssetModel makeAsset(String key, AssetSectionModel section);
	}

	public static class ShaderAssetFactory extends AbstractFileAssetFactory {
		public ShaderAssetFactory() {
			super(AssetType.glsl, "vert", "frag", "tesc", "tese", "geom", "comp");
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

}
