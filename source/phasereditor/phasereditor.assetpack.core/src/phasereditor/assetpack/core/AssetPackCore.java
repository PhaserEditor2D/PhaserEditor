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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IMemento;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.atlas.core.AtlasCore;
import phasereditor.audiosprite.core.AudioSpriteCore;

/**
 * Utilities related to the assets and resources.
 * 
 * @author arian
 *
 */
public class AssetPackCore {

	public static final String ASSET_EDITOR_ID = "phasereditor.assetpack.editor";
	public static final String ASSET_EDITOR_GOTO_MARKER_ATTR = "gotoMarker";

	private static final Set<String> _imageExtensions;
	private static final Set<String> _shaderExtensions;
	private static final Set<String> _audioExtensions;
	private static final Set<String> _videoExtensions;

	static {
		_imageExtensions = new HashSet<>();
		_imageExtensions.addAll(Arrays.asList("png", "jpg", "gif", "bmp", "svg"));

		_audioExtensions = new HashSet<>();
		// TODO: missing audio extensions
		_audioExtensions.addAll(Arrays.asList("wav", "ogg", "mp3", "flac", "wma", "au", "webm"));

		_videoExtensions = new HashSet<>();
		// TODO: missing video extensions
		_videoExtensions.addAll(Arrays.asList("mp4", "ogv", "webm", "flv", "wmv", "avi", "mpg"));

		_shaderExtensions = new HashSet<>();
		_shaderExtensions.addAll(Arrays.asList("vert", "frag", "tesc", "tese", "geom", "comp"));

		initWorkspaceListener();
	}

	/**
	 * If the given resource is an image.
	 * 
	 * @param resource
	 *            The resource to test.
	 * @return If it is an image.
	 */
	public static boolean isImage(IResource resource) {
		return resource instanceof IFile && _imageExtensions.contains(resource.getFullPath().getFileExtension());
	}

	public static boolean isShader(IResource resource) {
		return resource instanceof IFile && _shaderExtensions.contains(resource.getFullPath().getFileExtension());
	}

	private static void initWorkspaceListener() {
		// listen for project deletions
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					IResourceDelta mainDelta = event.getDelta();
					if (mainDelta != null) {

						PackDelta packDelta = new PackDelta();

						IResourceDeltaVisitor removeVisitor = new IResourceDeltaVisitor() {

							@Override
							public boolean visit(IResourceDelta delta) throws CoreException {
								if (delta.getKind() == IResourceDelta.REMOVED
										&& delta.getResource() instanceof IProject) {
									IProject project = (IProject) delta.getResource();
									List<AssetPackModel> packs = getAssetPackModels(project);
									for (AssetPackModel pack : packs) {
										removeAssetPackModel(pack);
									}
									packDelta.getPacks().addAll(packs);
								}
								return true;
							}
						};

						mainDelta.accept(removeVisitor);

						firePacksChanged(packDelta);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

			}
		});
	}

	/**
	 * If the given resource is an audio.
	 * 
	 * @param resource
	 *            The resource to test.
	 * @return If it is an audio.
	 */
	public static boolean isAudio(IResource resource) {
		return resource instanceof IFile && _audioExtensions.contains(resource.getFullPath().getFileExtension());
	}

	/**
	 * If the given resource is a video.
	 * 
	 * @param resource
	 *            The resource to test.
	 * @return If it is an audio.
	 */
	public static boolean isVideo(IResource resource) {
		if (resource instanceof IFolder) {
			return false;
		}

		String ext = resource.getFullPath().getFileExtension();

		boolean b = _videoExtensions.contains(ext);

		return b;
	}

	/**
	 * Build a list with the files with the same name but different extension.
	 * 
	 * 
	 * @param mainFile
	 *            The file with the name we want to collect.
	 * @param files
	 *            All the files.
	 * @return All the files with the mainFile's name.
	 */
	public static List<IFile> getSameNameFiles(IFile mainFile, List<IFile> files, Function<IFile, Boolean> accept) {
		Set<IFile> set = new HashSet<>();

		String mainName = mainFile.getName();
		String mainExt = mainFile.getFileExtension();
		if (mainExt.length() > 0) {
			mainName = mainName.substring(0, mainName.length() - mainExt.length() - 1);
		}

		for (IFile file : files) {
			String ext = file.getFileExtension();
			if (ext.length() > 0) {
				Boolean b = accept.apply(file);
				if (b.booleanValue()) {
					String name = file.getName();
					name = name.substring(0, name.length() - ext.length() - 1);
					if (name.equals(mainName)) {
						set.add(file);
					}
				}
			}
		}

		return new ArrayList<>(set);
	}

	/**
	 * Discover all image files under the given folder.
	 * 
	 * @param folder
	 *            The folder where to search.
	 * @return The list of the discovered images.
	 * @throws CoreException
	 *             If error.
	 */
	public static List<IFile> discoverImageFiles(IContainer folder) throws CoreException {
		return discoverFiles(folder, AssetPackCore::isImage);
	}

	/**
	 * Discover all audio files under the given folder.
	 * 
	 * @param folder
	 *            The folder where to search.
	 * @return The list of the discovered audio.
	 * @throws CoreException
	 *             If error.
	 */
	public static List<IFile> discoverAudioFiles(IContainer folder) throws CoreException {
		return discoverFiles(folder, AssetPackCore::isAudio);
	}

	/**
	 * Discover all video files under the given folder.
	 * 
	 * @param folder
	 *            The folder where to search.
	 * @return The list of the discovered audio.
	 * @throws CoreException
	 *             If error.
	 */
	public static List<IFile> discoverVideoFiles(IContainer folder) throws CoreException {
		return discoverFiles(folder, AssetPackCore::isVideo);
	}

	/**
	 * Discover all audio sprite files under the given folder.
	 * 
	 * @param folder
	 *            The folder where to search.
	 * @return The list of the discovered audio sprites.
	 * @throws CoreException
	 *             If error.
	 */
	public static List<IFile> discoverAudioSpriteFiles(IContainer folder) throws CoreException {
		return discoverFiles(folder, AudioSpriteCore::isAudioSpriteFile);
	}

	public static List<IFile> discoverTilemapFiles(IContainer folder) throws CoreException {
		return discoverFiles(folder, new Function<IFile, Boolean>() {

			@Override
			public Boolean apply(IFile file) {
				String ext = file.getFileExtension();
				if (ext != null && ext.toLowerCase().equals("csv"))
					return Boolean.TRUE;
				boolean tilemap = isTilemapJSONFile(file);
				return Boolean.valueOf(tilemap);
			}
		});
	}

	/**
	 * Discover the atlas (JSON/XML) files.
	 * 
	 * @param folder
	 *            The root folder.
	 * @return The files with content atlas type.
	 * @throws CoreException
	 *             If error.
	 */
	public static List<IFile> discoverAtlasFiles(IContainer folder) throws CoreException {
		return discoverFiles(folder, new Function<IFile, Boolean>() {

			@Override
			public Boolean apply(IFile t) {
				String format;
				try {
					format = AtlasCore.getAtlasFormat(t);
					return format == null ? Boolean.FALSE : Boolean.TRUE;
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	/**
	 * Discover all files under the given folder that are accepted by the given
	 * criterion.
	 * 
	 * @param folder
	 *            The folder where to search.
	 * @param accept
	 *            The accept criteria.
	 * @return The list of the discovered files.
	 * @throws CoreException
	 *             If error.
	 */

	public static List<IFile> discoverFiles(IContainer folder, Function<IFile, Boolean> accept) throws CoreException {
		List<IFile> list = new ArrayList<>();
		folder.accept(new IResourceVisitor() {

			@Override
			public boolean visit(IResource resource) throws CoreException {
				if (resource.isDerived()) {
					return true;
				}

				if (resource instanceof IFolder) {
					return true;
				}

				IFile file = (IFile) resource;

				Boolean b = accept.apply(file);
				if (b.booleanValue()) {
					list.add(file);
				}

				return true;
			}
		});
		return list;
	}

	public static List<IFile> discoverTextFiles(IContainer folder) throws CoreException {
		return discoverFiles(folder, new Function<IFile, Boolean>() {

			@Override
			public Boolean apply(IFile t) {
				return isImage(t) || isAudio(t) || isVideo(t) || isShader(t) ? Boolean.FALSE : Boolean.TRUE;
			}
		});
	}

	public static List<IFile> discoverShaderFiles(IContainer folder) throws CoreException {
		return discoverFiles(folder, new Function<IFile, Boolean>() {

			@Override
			public Boolean apply(IFile t) {
				return isShader(t) ? Boolean.TRUE : Boolean.FALSE;
			}
		});
	}

	public static Function<IFile, Boolean> createFileExtFilter(String... exts) {
		return new Function<IFile, Boolean>() {

			@Override
			public Boolean apply(IFile f) {
				String ext2 = f.getFileExtension();
				if (ext2 != null) {
					for (String ext : exts) {
						if (ext2.toLowerCase().equals(ext.toLowerCase())) {
							return Boolean.TRUE;
						}
					}
				}
				return Boolean.FALSE;
			}
		};
	}

	public static void sortFilesByExtension(List<IFile> files, String... exts) {
		Set<String> set = new HashSet<>(Arrays.asList(exts));
		files.sort(new Comparator<IFile>() {

			@Override
			public int compare(IFile o1, IFile o2) {
				String e1 = o1.getFileExtension();
				String e2 = o2.getFileExtension();
				int a = e1 == null ? 0 : (set.contains(e1) ? -1 : 1);
				int b = e2 == null ? 0 : (set.contains(e2) ? -1 : 1);
				return Integer.compare(a, b);
			}
		});
	}

	/**
	 * Test if the content type of the file is the asset pack type.
	 * 
	 * @param file
	 *            The file to test.
	 * @return If the file is an asset pack.
	 * @throws CoreException
	 *             If error.
	 */
	public static boolean isAssetPackFile(IFile file) throws CoreException {
		if (!file.exists() || !file.isSynchronized(IResource.DEPTH_ONE)) {
			return false;
		}
		IContentDescription desc = file.getContentDescription();
		if (desc == null) {
			return false;
		}

		IContentType contentType = desc.getContentType();
		String id = contentType.getId();
		return id.equals(AssetPackContentDescriber.CONTENT_TYPE_ID);
	}

	/**
	 * Test if the content type of the file is the tilemap JSON.
	 * 
	 * @param file
	 *            The file to test.
	 * @return If the file is a tilemap JSON.
	 * @throws CoreException
	 *             If error.
	 */
	public static boolean isTilemapJSONFile(IFile file) {
		if (!file.exists()) {
			return false;
		}

		try {
			IContentDescription desc = file.getContentDescription();
			if (desc == null) {
				return false;
			}
			IContentType contentType = desc.getContentType();
			String id = contentType.getId();
			return id.equals(TilemapJSONDescriber.CONTENT_TYPE_ID);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Check if the given content has an Asset Pack format.
	 * 
	 * @param contents
	 *            The content to test.
	 * @return Return <code>null</code> if the content has a valid format, else
	 *         it return an error message.
	 */
	public static String isAssetPackContent(InputStream contents) {
		try {
			JSONTokener tokener = new JSONTokener(new InputStreamReader(contents));
			JSONObject obj = new JSONObject(tokener);
			JSONObject meta = obj.getJSONObject("meta");
			meta.get("generated");
			meta.get("version");
			meta.get("app");
		} catch (JSONException e) {
			return e.getMessage();
		}
		return null;
	}

	/**
	 * Check if the given content has a Tiled JSON format
	 * (https://github.com/bjorn/tiled/wiki/JSON-Map-Format).
	 * 
	 * @param contents
	 *            The content to test.
	 * @return <code>null</code> if has a valid format, else return an error
	 *         message.
	 */
	public static String isTilemapJSONContent(InputStream contents) {
		try {
			JSONTokener tokener = new JSONTokener(new InputStreamReader(contents));
			JSONObject obj = new JSONObject(tokener);
			obj.getJSONArray("layers");
			obj.getJSONArray("tilesets");
			obj.getDouble("tileheight");
			obj.getDouble("tilewidth");
			return null;
		} catch (JSONException e) {
			return e.getMessage();
		}
	}

	protected static Map<IFile, AssetPackModel> _filePackMap = new HashMap<>();

	public static List<AssetPackModel> getAssetPackModels(IProject project) {
		synchronized (_filePackMap) {
			List<AssetPackModel> list = new ArrayList<>();
			for (AssetPackModel model : _filePackMap.values()) {
				IFile file = model.getFile();
				IProject project2 = file.getProject();
				if (project2.equals(project)) {
					list.add(model);
				}
			}
			return list;
		}
	}

	/**
	 * Find the assets or asset elements with the same key of the given one.
	 * 
	 * @param project
	 *            Find in the packs of the given project.
	 * @param key
	 *            The key to match.
	 * @return The list of the matching assets or asset elements.
	 */
	public static List<Object> findAssetObjects(IProject project, String key) {
		List<Object> list = new ArrayList<>();
		List<AssetPackModel> packs = getAssetPackModels(project);
		for (AssetPackModel pack : packs) {
			List<AssetModel> assets = pack.getAssets();
			for (AssetModel asset : assets) {
				if (asset.getKey().equals(key)) {
					list.add(asset);
				}
				List<? extends IAssetElementModel> elems = asset.getSubElements();
				for (IAssetElementModel elem : elems) {
					if (elem.getName().equals(key)) {
						list.add(elem);
					}
				}
			}
		}
		return list;
	}

	/**
	 * Get the asset packs of the whole workspace.
	 * 
	 * @return A list with the asset pack models.
	 */
	public static List<AssetPackModel> getAssetPackModels() {
		if (_filePackMap.isEmpty()) {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				if (!project.isAccessible()) {
					continue;
				}

				try {
					project.accept(new IResourceVisitor() {

						@Override
						public boolean visit(IResource resource) throws CoreException {
							if (resource instanceof IFile) {
								IFile file = (IFile) resource;
								if (isAssetPackFile(file)) {
									try {
										getAssetPackModel(file);
									} catch (Exception e) {
										throw new RuntimeException(e);
									}
								}
							}
							return true;
						}
					});
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		}

		synchronized (_filePackMap) {
			return new ArrayList<>(_filePackMap.values());
		}
	}

	public static AssetPackModel getAssetPackModel(IFile file) throws Exception {
		synchronized (_filePackMap) {

			if (_filePackMap.containsKey(file)) {
				return _filePackMap.get(file);
			}

			AssetPackModel model = new AssetPackModel(file);
			_filePackMap.put(file, model);

			return model;
		}
	}

	public static void removeAssetPackModel(AssetPackModel pack) {
		// when the newFile=null, then the pack is removed from the record.
		moveAssetPackModel(null, pack);
	}

	public static void moveAssetPackModel(IFile newFile, AssetPackModel model) {
		synchronized (_filePackMap) {

			IFile oldFile = model.getFile();

			_filePackMap.remove(oldFile);

			if (newFile != null) {
				_filePackMap.put(newFile, model);
				model.setFile(newFile);
			}
		}
	}

	public static String getAssetStringReference(Object elem) {
		JSONObject ref = getAssetJSONReference(elem);
		if (ref == null) {
			return null;
		}
		return ref.toString(2);
	}

	public static JSONObject getAssetJSONReference(Object elem) {
		AssetPackModel pack = null;
		if (elem instanceof AssetModel) {
			pack = ((AssetModel) elem).getPack();
		} else if (elem instanceof IAssetElementModel) {
			pack = ((IAssetElementModel) elem).getAsset().getPack();
		}
		if (pack != null) {
			return pack.getAssetJSONRefrence(elem);
		}
		return null;
	}

	public static Object findAssetElement(String ref) {
		JSONObject obj = new JSONObject(ref);
		return findAssetElement(obj);
	}

	public static Object findAssetElement(JSONObject ref) {
		String filename = ref.getString("file");
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile file = root.getFile(root.getProjectRelativePath().append(filename));
		if (file.exists()) {
			try {
				AssetPackModel pack = AssetPackCore.getAssetPackModel(file);
				return pack.getElementFromJSONReference(ref);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Object findAssetElement(IMemento memento) {
		String ref = memento.getString(AssetPackModel.MEMENTO_KEY);
		if (ref != null) {
			return findAssetElement(ref);
		}
		return null;
	}

	public static class PackDelta {
		private Set<AssetPackModel> _packs;
		private Set<AssetModel> _assets;

		public PackDelta() {
			super();
			_packs = new HashSet<>();
			_assets = new HashSet<>();
		}

		public Set<AssetModel> getAssets() {
			return _assets;
		}

		public Set<AssetPackModel> getPacks() {
			return _packs;
		}

		public boolean isEmpty() {
			return _packs.isEmpty() && _assets.isEmpty();
		}

		public boolean contains(Object... list) {
			for (Object obj : list) {
				if (_assets.contains(obj) || _packs.contains(obj)) {
					return true;
				}
			}
			return false;
		}

		public void add(AssetPackModel pack) {
			_packs.add(pack);
		}

		public void add(AssetModel asset) {
			_assets.add(asset);
		}

		public void add(PackDelta delta) {
			_packs.addAll(delta.getPacks());
			_assets.addAll(delta.getAssets());
		}
	}

	public interface IPacksChangeListener {
		public void packsChanged(PackDelta delta);
	}

	private static List<IPacksChangeListener> _packsChanged = new CopyOnWriteArrayList<>();

	public static void firePacksChanged(PackDelta delta) {
		if (!delta.isEmpty()) {
			for (IPacksChangeListener listener : _packsChanged) {
				listener.packsChanged(delta);
			}
		}
	}

	public static void addPacksChangedListener(IPacksChangeListener listener) {
		_packsChanged.add(listener);
	}

	public static void removePacksChangedListener(IPacksChangeListener listener) {
		_packsChanged.remove(listener);
	}

}