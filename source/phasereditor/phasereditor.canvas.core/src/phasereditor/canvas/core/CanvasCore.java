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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.IAssetReference;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.canvas.core.codegen.CanvasCodeGeneratorProvider;
import phasereditor.project.core.codegen.ICodeGenerator;
import phasereditor.project.core.codegen.SourceLang;

/**
 * @author arian
 *
 */
public class CanvasCore {
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	public static final String CANVAS_PROBLEM_MARKER_ID = "phasereditor.canvas.core.problem";
	public static final String SPRITE_CONTENT_TYPE_ID = "phasereditor.canvas.core.spriteContentType";
	public static final String GROUP_CONTENT_TYPE_ID = "phasereditor.canvas.core.groupContentType";
	public static final String STATE_CONTENT_TYPE_ID = "phasereditor.canvas.core.stateContentType";
	private static final CanvasFileDataCache _fileDataCache = new CanvasFileDataCache();
	public static final String GOTO_MARKER_OBJECT_ID_ATTR = "phasereditor.canvas.core.marker.objectId";
	public static final String CANVAS_OBJECT_REF_MARKER_ID = "phasereditor.canvas.core.objectref";

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, e.getMessage(), e));
	}

	public static String getValidJavaScriptName(String name) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (char c : name.toCharArray()) {
			if (i == 0 && !Character.isJavaIdentifierStart(c)) {
				sb.append("_");
			} else if (Character.isJavaIdentifierPart(c)) {
				sb.append(c);
			} else {
				sb.append("_");
			}
			i++;
		}
		return sb.toString();
	}

	public static boolean isCanvasFile(IFile file) {
		CanvasType type = getCanvasType(file);
		return type != null;
	}

	public static boolean isPrefabFile(IFile file) {
		CanvasType type = getCanvasType(file);
		return type == CanvasType.GROUP || type == CanvasType.SPRITE;
	}

	public static boolean isCanvasFileExtension(IFile file) {
		return file.getFileExtension().equals("canvas");
	}

	/**
	 * This method is used to quickly know the type of a canvas file. If it is
	 * not a canvas file then it returns <code>null</code>.
	 * 
	 * @param file
	 *            The file to test.
	 * @return The canvas type or <code>null</code> if it is not a canvas file.
	 */
	public static CanvasType getCanvasType(IFile file) {
		IContentDescription desc;
		try {
			desc = file.getContentDescription();

			if (desc == null) {
				return null;
			}

			IContentType contentType = desc.getContentType();

			if (contentType == null) {
				return null;
			}

			String id = contentType.getId();

			switch (id) {
			case SPRITE_CONTENT_TYPE_ID:
				return CanvasType.SPRITE;
			case GROUP_CONTENT_TYPE_ID:
				return CanvasType.GROUP;
			case STATE_CONTENT_TYPE_ID:
				return CanvasType.STATE;
			default:
				return null;
			}
		} catch (CoreException e) {
			// e.printStackTrace();
		}
		return null;
	}

	public static CanvasType getCanvasType(InputStream contents) {
		JSONObject data = new JSONObject(new JSONTokener(contents));
		if (data.has("settings") && data.has("world")) {
			// use the GROUP type as default, for backward compatibility with
			// v1.3.0 and bellow
			String name = data.optString("type", CanvasType.GROUP.name());
			CanvasType type = CanvasType.valueOf(name);
			return type;
		}
		return null;
	}

	public static class PrefabReference {
		private IFile _file;
		private String _objectName;
		private String _objectId;

		public PrefabReference(IFile file, String objectName, String objectId) {
			super();
			_file = file;
			_objectName = objectName;
			_objectId = objectId;
		}

		public String getObjectId() {
			return _objectId;
		}

		public String getObjectName() {
			return _objectName;
		}

		public IFile getFile() {
			return _file;
		}
	}

	public static List<PrefabReference> findPrefabReferencesInFileContent(Prefab prefab, IFile file) {
		CanvasModel canvasModel = new CanvasModel(file);
		try (InputStream contents = file.getContents()) {
			canvasModel.read(new JSONObject(new JSONTokener(contents)));
			return findPrefabReferenceInModelContent(prefab, canvasModel.getWorld());
		} catch (Exception e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	public static List<PrefabReference> findPrefabReferenceInModelContent(Prefab prefab, WorldModel world) {
		List<PrefabReference> list = new ArrayList<>();

		world.getWorld().walk(model -> {
			Prefab modelPrefab = model.getPrefab();
			if (prefab.equals(modelPrefab)) {
				list.add(new PrefabReference(world.getFile(), model.getEditorName(), model.getId()));
			}
		});

		return list;
	}

	public static class AssetInCanvasReference implements IAssetReference {

		private String _objectId;
		private String _objectName;
		private IFile _file;
		private IAssetKey _assetKey;
		private AssetSpriteModel<IAssetKey> _model;

		public AssetInCanvasReference(AssetSpriteModel<IAssetKey> model, IAssetKey assetKey) {
			_objectId = model.getId();
			_objectName = model.getEditorName();
			_file = model.getWorld().getFile();
			_assetKey = assetKey;
			_model = model;
		}

		public AssetSpriteModel<IAssetKey> getModel() {
			return _model;
		}

		@Override
		public IAssetKey getAssetKey() {
			return _assetKey;
		}

		@Override
		public String getLabel() {
			return _objectName;
		}

		@Override
		public IFile getFile() {
			return _file;
		}

		@Override
		public void reveal(IWorkbenchPage workbenchPage) {
			IMarker marker = CanvasCore.createNodeMarker(_file, _objectId);
			try {
				IDE.openEditor(workbenchPage, marker);
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			}
		}

		public String getObjectId() {
			return _objectId;
		}

		public String getObjectName() {
			return _objectName;
		}

		@Override
		public String getId() {
			return _file.getFullPath().toPortableString() + "@" + _objectId;
		}
	}

	public static List<IAssetReference> findAssetKeyReferencesInFileContent(IAssetKey assetKey, IFile file) {
		CanvasModel canvasModel = new CanvasModel(file);
		try (InputStream contents = file.getContents()) {
			canvasModel.read(new JSONObject(new JSONTokener(contents)));
			return findAssetKeyReferenceInModelContent(assetKey, canvasModel.getWorld());
		} catch (Exception e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void forEachAssetKeyInModelContent(WorldModel world,
			BiConsumer<IAssetKey, AssetSpriteModel<IAssetKey>> visitor) {
		world.getWorld().walk_skipGroupIfFalse(model -> {

			if (model instanceof AssetSpriteModel) {

				if (model.isPrefabInstance() && !model.isOverriding(BaseSpriteModel.PROPSET_TEXTURE)) {
					// skip prefab instance that cannot change its texture
					return false;
				}

				AssetSpriteModel spriteModel = (AssetSpriteModel) model;
				IAssetKey key = spriteModel.getAssetKey();

				if (key instanceof ImageAssetModel.Frame) {
					key = key.getAsset();
				}

				visitor.accept(key, spriteModel);
			} else if (model instanceof GroupModel) {

				// just avoid to loop into group prefabs
				if (model.isPrefabInstance()) {
					return false;
				}

			}
			return true;
		});
	}

	public static List<IAssetReference> findAssetKeyReferenceInModelContent(IAssetKey assetKey, WorldModel world) {
		IAssetKey assetKey2 = assetKey instanceof ImageAssetModel.Frame ? assetKey.getAsset() : assetKey;

		List<IAssetReference> list = new ArrayList<>();

		forEachAssetKeyInModelContent(world, (key, spriteModel) -> {
			if (AssetPackCore.equals(key, assetKey2)) {
				list.add(new AssetInCanvasReference(spriteModel, key));
			}
		});

		return list;
	}

	public static List<IAssetReference> findAssetReferencesInFileContent(AssetModel asset, IFile file) {
		CanvasModel canvasModel = new CanvasModel(file);
		try (InputStream contents = file.getContents()) {
			canvasModel.read(new JSONObject(new JSONTokener(contents)));
			return findAssetReferenceInModelContent(asset, canvasModel.getWorld());
		} catch (Exception e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<IAssetReference> findAssetReferenceInModelContent(AssetModel asset, WorldModel world) {
		List<IAssetReference> list = new ArrayList<>();

		world.getWorld().walk_skipGroupIfFalse(model -> {

			if (model instanceof AssetSpriteModel) {

				if (model.isPrefabInstance() && !model.isOverriding(BaseSpriteModel.PROPSET_TEXTURE)) {
					// skip prefab instance that cannot change its texture
					return false;
				}

				AssetSpriteModel spriteModel = (AssetSpriteModel) model;
				AssetModel spriteAsset = spriteModel.getAssetKey().getAsset();
				if (AssetPackCore.equals(spriteAsset, asset)) {
					list.add(new AssetInCanvasReference(spriteModel, spriteAsset));
				}
			} else if (model instanceof GroupModel) {

				// just avoid to loop into group prefabs
				if (model.isPrefabInstance()) {
					return false;
				}

			}
			return true;
		});

		return list;
	}

	public static List<IFile> getCanvasDereivedFiles(IFile canvasFile) {
		List<IFile> result = new ArrayList<>();

		CanvasModel model = new CanvasModel(canvasFile);
		SourceLang lang;
		try (InputStream contents = canvasFile.getContents()) {
			model.read(new JSONObject(new JSONTokener(contents)));
			lang = model.getSettings().getLang();
		} catch (IOException | CoreException e) {
			logError(e);
			throw new RuntimeException(e);
		}

		// add the code file
		IFile srcFile = canvasFile.getWorkspace().getRoot()
				.getFile(canvasFile.getFullPath().removeFileExtension().addFileExtension(lang.getExtension()));
		if (srcFile.exists()) {
			result.add(srcFile);
		}

		return result;
	}

	public static CanvasFileDataCache getCanvasFileCache() {
		return _fileDataCache;
	}

	public static String getDefaultClassName(IFile file) {
		if (file == null) {
			return "CanvasClass";
		}

		return file.getFullPath().removeFileExtension().lastSegment();
	}

	public static IMarker createNodeMarker(IFile file, String objectId) {
		try {
			IMarker marker = file.createMarker(CanvasCore.CANVAS_OBJECT_REF_MARKER_ID);
			marker.setAttribute("EDITOR_ID_ATTR", "phasereditor.canvas.ui.editors.canvas");
			marker.setAttribute(GOTO_MARKER_OBJECT_ID_ATTR, objectId);
			return marker;
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public static void forEachJSONReference(JSONObject canvasFileData, Consumer<JSONObject> visitor) {
		JSONObject tableData = canvasFileData.optJSONObject("asset-table");

		if (tableData != null) {
			for (String k : tableData.keySet()) {
				JSONObject assetRef = tableData.getJSONObject(k);
				visitor.accept(assetRef);
			}
		}

		JSONObject worldData = canvasFileData.getJSONObject("world");
		forEachJSONReference_object(worldData, visitor);
	}

	private static void forEachJSONReference_object(JSONObject objData, Consumer<JSONObject> visitor) {
		String type = objData.getString("type");
		switch (type) {
		case GroupModel.TYPE_NAME:
			JSONObject info = objData.getJSONObject("info");
			JSONArray children = info.getJSONArray("children");
			for (int i = 0; i < children.length(); i++) {
				JSONObject childData = children.getJSONObject(i);
				forEachJSONReference_object(childData, visitor);
			}
			break;
		default:
			JSONObject assetRef = objData.optJSONObject("asset-ref");
			if (assetRef != null) {
				visitor.accept(assetRef);
			}
			break;
		}
	}

	public static void compile(CanvasFile canvasFile, IProgressMonitor monitor) {

		try {

			CanvasModel canvasModel = canvasFile.newModel();

			if (canvasModel.getWorld().hasErrors()) {
				return;
			}

			compile(canvasModel, monitor);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void compile(CanvasModel canvasModel, IProgressMonitor monitor) {
		try {
			IFile inputFile = canvasModel.getFile();
			String fname = inputFile.getFullPath().removeFileExtension()
					.addFileExtension(canvasModel.getSettings().getLang().getExtension()).lastSegment();

			 
			IFile file = inputFile.getParent().getFile(new Path(fname));

			Charset charset;
			
			if (file.exists()) {
				charset = Charset.forName(file.getCharset());
			} else {
				charset = Charset.forName("UTF-8");
			}
			
			String replace = null;

			if (file.exists()) {
				byte[] bytes = Files.readAllBytes(file.getLocation().makeAbsolute().toFile().toPath());
				replace = new String(bytes, charset);
			}

			ICodeGenerator generator = new CanvasCodeGeneratorProvider().getCodeGenerator(canvasModel);

			String content = generator.generate(replace);

			ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes(charset));
			if (file.exists()) {
				file.setContents(stream, IResource.NONE, monitor);
			} else {
				file.setCharset(charset.name(), monitor);
				file.create(stream, false, monitor);
			}
			file.refreshLocal(1, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
