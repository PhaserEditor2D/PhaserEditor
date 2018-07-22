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

import static java.lang.System.out;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IMemento;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.bmpfont.core.JsonBitmapFontContentType;
import phasereditor.bmpfont.core.XmlBitmapFontContentType;
import phasereditor.project.core.ProjectCore;

public final class AssetPackModel {
	public static final String MEMENT_PROJECT_KEY = "projectName";
	static final String MEMENTO_KEY = "phasereditor.assetpack.core.AssetPackModel";
	public static final String PROP_DIRTY = "dirty";
	public static final String PROP_FILE = "file";
	public static final String PROP_ASSET_KEY = "assetKey";
	protected List<AssetSectionModel> _sections;
	private IFile _file;
	private boolean _dirty;

	public AssetPackModel(IFile file) throws Exception {
		this(readJSON(file), file);
	}

	public AssetPackModel(JSONObject jsonDoc, IFile file) throws Exception {
		_file = file;
		build(jsonDoc);
	}

	private void build(JSONObject jsonRoot) throws Exception {
		_sections = new ArrayList<>();

		@SuppressWarnings("rawtypes")
		Iterator keysIter = jsonRoot.keys();
		while (keysIter.hasNext()) {
			String sectionKey = (String) keysIter.next();
			if (sectionKey.equals("meta")) {
				// keep meta-data here
			} else {

				Object jsonSection = jsonRoot.get(sectionKey);
				JSONArray jsonSectionFilesArray;
				if (jsonSection instanceof JSONArray) {
					jsonSectionFilesArray = (JSONArray) jsonSection;
				} else {
					JSONObject jsonNewSection = (JSONObject) jsonSection;
					jsonSectionFilesArray = jsonNewSection.getJSONArray("files");

					String prefix = jsonNewSection.optString("prefix", null);
					String extension = jsonNewSection.optString("extension", null);
					String defaultType = jsonNewSection.optString("defaultType", null);
					String path = jsonNewSection.optString("path", "");
					
					if (path.length() > 0 && !path.endsWith("/")) {
						path += "/";
					}

					for (var jsonFile : jsonSectionFilesArray.iterJSON()) {

						String fileExtension = jsonFile.optString("extension", null);
						if (fileExtension == null) {
							fileExtension = extension;
						}

						String fileType = jsonFile.optString("type", null);

						if (fileType == null) {
							fileType = defaultType;
							jsonFile.put("type", fileType);
						}

						String origKey = jsonFile.getString("key");
						String key = origKey;

						if (prefix != null) {
							key = prefix + key;
						}

						jsonFile.put("key", key);

						String url = jsonFile.optString("url", null);
						if (url == null) {
							if (fileExtension == null) {
								fileExtension = getFileExtensionFromType(fileType);
							}
							url = path + origKey + (fileExtension.length() > 0 ? "." : "") + fileExtension;
							jsonFile.put("url", url);
						}

					}
				}

				AssetSectionModel section = new AssetSectionModel(sectionKey, jsonSectionFilesArray, this);
				addSection(section, false);
			}
		}
	}

	private static String getFileExtensionFromType(String fileType) {
		if (AssetType.isTypeSupported(fileType)) {
			AssetType type = AssetType.valueOf(fileType);
			return type.getFileExtension();
		}
		return "";
	}

	public PackDelta computeDelta(IPath deltaFilePath) {
		PackDelta delta = new PackDelta(_file.getProject());

		if (deltaFilePath == null) {
			return delta;
		}

		IPath packPath = getFile().getFullPath();

		if (packPath.equals(deltaFilePath)) {
			delta.add(this);
			return delta;
		}

		for (AssetModel asset : getAssets()) {
			IFile[] lastUsedFiles = asset.getLastUsedFiles();
			IFile[] usedFiles = asset.computeUsedFiles();
			IFile[][] allfiles = { lastUsedFiles, usedFiles };

			for (IFile[] files : allfiles) {
				for (IFile file : files) {
					if (file != null) {
						IPath path = file.getFullPath();
						if (path.equals(deltaFilePath)) {
							delta.add(asset);
						}
					}
				}
			}
		}

		return delta;
	}

	public List<IStatus> build() {
		out.println("Build asset pack " + getFile().getLocation());

		List<IStatus> problems = new ArrayList<>();
		for (AssetSectionModel section : _sections) {
			for (AssetModel model : section.getAssets()) {
				model.build(problems);
			}
		}

		return problems;
	}

	private static JSONObject readJSON(IFile file) throws Exception {
		try (InputStream contents = file.getContents()) {
			JSONObject obj = new JSONObject(new JSONTokener(contents));
			return obj;
		}
	}

	public JSONObject toJSON() {
		JSONObject data = new JSONObject();
		for (AssetSectionModel section : _sections) {
			section.writeSection(data);
		}
		writeMeta(data);
		return data;
	}

	private static void writeMeta(JSONObject pack) {
		JSONObject meta = new JSONObject();
		pack.put("meta", meta);
		meta.put("generated", Long.toString(System.currentTimeMillis()));
		meta.put("app", "Phaser Editor");
		meta.put("url", "https://phasereditor2d.com");
		meta.put("version", "1.0");
		meta.put("copyright", "Arian Fornaris (c) 2015,2018");
	}

	public void save(IProgressMonitor monitor) {
		JSONObject json = toJSON();
		try (ByteArrayInputStream source = new ByteArrayInputStream(json.toString(2).getBytes())) {
			_file.setContents(source, false, false, monitor);
		} catch (IOException | CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		setDirty(false);
	}

	public boolean isDirty() {
		return _dirty;
	}

	public void setDirty(boolean dirty) {
		if (_dirty != dirty) {
			_dirty = dirty;
			firePropertyChange(PROP_DIRTY);
		}
	}

	public boolean isSharedVersion() {
		return AssetPackCore.getAssetPackModel(_file, false) == this;
	}

	/**
	 * Get the file associated with this model.
	 * 
	 * @return The file or <code>null</code> if the file was deleted.
	 */
	public IFile getFile() {
		return _file;
	}

	public void setFile(IFile file) {
		_file = file;
		firePropertyChange(PROP_FILE);
	}

	public String getName() {
		if (_file == null) {
			return "<unkown>";
		}
		return _file.getName();
	}

	public String getRelativePath() {
		return _file.getProjectRelativePath().toPortableString();
	}

	public IContainer getWebContentFolder() {
		if (_file == null) {
			return null;
		}

		return ProjectCore.getWebContentFolder(_file.getProject());
	}

	public List<IFile> discoverImageFiles() throws CoreException {
		return AssetPackCore.discoverImageFiles(getDiscoverFolder());
	}
	
	public List<IFile> discoverSvgFiles() throws CoreException {
		return AssetPackCore.discoverSvgFiles(getDiscoverFolder());
	}

	public List<IFile> discoverTilemapFiles(AssetType tilemapType) throws CoreException {
		return AssetPackCore.discoverTilemapFiles(getDiscoverFolder(), tilemapType);
	}

	public List<IFile> discoverAudioFiles() throws CoreException {
		return AssetPackCore.discoverAudioFiles(getDiscoverFolder());
	}

	public List<IFile> discoverVideoFiles() throws CoreException {
		return AssetPackCore.discoverVideoFiles(getDiscoverFolder());
	}

	public List<IFile> discoverAudioSpriteFiles() throws CoreException {
		return AssetPackCore.discoverAudioSpriteFiles(getDiscoverFolder());
	}

	public List<IFile> discoverAtlasFiles(String... formats) throws CoreException {
		return AssetPackCore.discoverAtlasFiles(getDiscoverFolder(), formats);
	}

	public List<IFile> discoverAtlasFiles(AssetType type) throws CoreException {
		return discoverAtlasFiles(AssetPackCore.getAtlasFormatsForType(type));
	}

	public IContainer getDiscoverFolder() {
		return getFile().getParent();
	}

	public List<IFile> discoverTextFiles(String[] exts) throws CoreException {
		return AssetPackCore.discoverFiles(getDiscoverFolder(), AssetPackCore.createFileExtFilter(exts));
	}

	public List<IFile> discoverFiles(Function<IFile, Boolean> filter) throws CoreException {
		return AssetPackCore.discoverFiles(getDiscoverFolder(), filter);
	}

	@SuppressWarnings("boxing")
	public List<IFile> discoverFilesWithContentType(String... contentTypes) throws CoreException {
		return AssetPackCore.discoverFiles(getDiscoverFolder(), f -> {
			IContentDescription desc;
			try {
				desc = f.getContentDescription();

				if (desc == null) {
					return false;
				}

				IContentType type = desc.getContentType();

				if (type == null) {
					return false;
				}

				for (String id : contentTypes) {
					if (type.getId().equals(id)) {
						return true;
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}

			return false;
		});
	}

	public void visitAssets(Consumer<AssetModel> visitor) {
		for (AssetSectionModel section : _sections) {
			for (AssetModel asset : section.getAssets()) {
				visitor.accept(asset);
			}
		}
	}

	public static void sortFilesByNotUsed(List<IFile> files, Set<IFile> usedFiles) {
		files.sort(new Comparator<IFile>() {

			@Override
			public int compare(IFile o1, IFile o2) {
				int a = usedFiles.contains(o1) ? 1 : 0;
				int b = usedFiles.contains(o2) ? 1 : 0;
				if (a == b) {
					return o1.getFullPath().toPortableString().compareTo(o2.getFullPath().toPortableString());
				}
				return Integer.compare(a, b);
			}
		});
	}

	public Set<IFile> sortFilesByNotUsed(List<IFile> files) {
		Set<IFile> usedFiles = findUsedFiles();
		sortFilesByNotUsed(files, usedFiles);
		return usedFiles;
	}

	public Set<IFile> findUsedFiles() {
		Set<IFile> usedFiles = new HashSet<>();
		visitAssets(new Consumer<AssetModel>() {

			@Override
			public void accept(AssetModel t) {
				IFile[] list = t.computeUsedFiles();
				for (IFile f : list) {
					if (f != null) {
						usedFiles.add(f);
					}
				}
			}
		});
		return usedFiles;
	}

	public IFile pickFile(List<IFile> files) {
		if (files != null && !files.isEmpty()) {
			Set<IFile> used = findUsedFiles();
			for (IFile file : files) {
				if (!used.contains(file)) {
					return file;
				}
			}
		}
		return null;
	}

	/**
	 * Pick one of the not used image files.
	 * 
	 * @return The unused image resource, or null if there is not any image
	 *         available.
	 * @throws CoreException
	 */
	public IFile pickImageFile() throws CoreException {
		return pickFile(discoverImageFiles());
	}
	
	public IFile pickSvgFile() throws CoreException {
		return pickFile(discoverSvgFiles());
	}

	/**
	 * Pick one of the not used audiosprite files.
	 * 
	 * @return The unused audiosprite resource, or null if there is not any
	 *         audiosprite available.
	 * @throws CoreException
	 */
	public IFile pickAudioSpriteFile() throws CoreException {
		return pickFile(discoverAudioSpriteFiles());
	}

	/**
	 * Pick a list of not used audio files.
	 * 
	 * @return The list or not used audio files, or null if there is not anyone
	 *         available.
	 * @throws CoreException
	 */
	public List<IFile> pickAudioFiles() throws CoreException {
		Set<IFile> used = findUsedFiles();
		List<IFile> audios = discoverAudioFiles();
		Set<IFile> result = new HashSet<>();
		if (audios.size() > 0) {
			for (IFile audio : audios) {
				if (!used.contains(audio)) {
					List<IFile> closure = AssetPackCore.getSameNameFiles(audio, audios, AssetPackCore::isAudio);
					result.addAll(closure);
					break;
				}
			}
		}
		return new ArrayList<>(result);
	}

	/**
	 * Pick a list of not used audio files.
	 * 
	 * @return The list or not used audio files, or null if there is not anyone
	 *         available.
	 * @throws CoreException
	 */
	public List<IFile> pickVideoFiles() throws CoreException {
		Set<IFile> used = findUsedFiles();
		List<IFile> videos = discoverVideoFiles();
		Set<IFile> result = new HashSet<>();
		if (videos.size() > 0) {
			for (IFile video : videos) {
				if (!used.contains(video)) {
					List<IFile> closure = AssetPackCore.getSameNameFiles(video, videos, AssetPackCore::isVideo);
					result.addAll(closure);
					break;
				}
			}
		}
		return new ArrayList<>(result);
	}

	/**
	 * Pick a tilemap file that is not used.
	 * @param tilemapType 
	 * 
	 * @return The non used tilemap file or null if there is not anyone available.
	 * @throws CoreException
	 */
	public IFile pickTilemapFile(AssetType tilemapType) throws CoreException {
		return pickFile(discoverTilemapFiles(tilemapType));
	}

	public IFile pickBitmapFontFile() throws CoreException {
		List<IFile> files = discoverFilesWithContentType(XmlBitmapFontContentType.CONTENT_TYPE_ID,
				JsonBitmapFontContentType.CONTENT_TYPE_ID);
		IFile file = pickFile(files);
		return file;
	}

	public void addSection(AssetSectionModel section, boolean notify) {
		addSection(_sections.size(), section, notify);
	}

	public void addSection(int index, AssetSectionModel section, boolean notify) {
		section.setPack(this);
		_sections.add(index, section);
		if (notify) {
			setDirty(true);
		}
	}

	public void removeSection(AssetSectionModel section, boolean notify) {
		_sections.remove(section);
		if (notify) {
			setDirty(true);
		}
	}
	
	public void removeAllSections(List<AssetSectionModel> sections, boolean notify) {
		_sections.removeAll(sections);
		if (notify) {
			setDirty(true);
		}
	}
	
	public void addAllSections(int index, List<AssetSectionModel> sections, boolean notify) {
		_sections.addAll(index, sections);
		if (notify) {
			setDirty(true);
		}
	}
	

	public List<AssetSectionModel> getSections() {
		return Collections.unmodifiableList(_sections);
	}

	public List<AssetModel> getAssets() {
		List<AssetModel> list = new ArrayList<>();
		for (AssetSectionModel section : _sections) {
			for (AssetModel asset : section.getAssets()) {
				list.add(asset);
			}
		}
		return list;
	}

	public AssetSectionModel findSection(String key) {
		if (key == null) {
			return null;
		}
		for (AssetSectionModel section : _sections) {
			String key2 = section.getKey();
			if (key2 != null && key2.equals(key)) {
				return section;
			}
		}
		return null;
	}

	public AssetModel findAsset(String sectionKey, String assetKey) {
		AssetSectionModel section = findSection(sectionKey);

		if (section == null) {
			return null;
		}

		return section.findAsset(assetKey);
	}

	public String createKey(String prefix) {
		for (int i = 0; i < 200; i++) {
			String key = prefix + (i == 0 ? "" : Integer.valueOf(i));
			if (!hasKey(key)) {
				return key;
			}
		}
		// in the practice we always find a key, but at the end we have to
		// return something
		return prefix;
	}

	public String createKey(IFile file) {
		String name = file.getName();
		String ext = file.getFileExtension();
		if (ext.length() > 0) {
			name = name.substring(0, name.length() - ext.length() - 1);
		}
		return createKey(name);
	}

	public boolean hasKey(String key) {
		for (AssetSectionModel section : _sections) {
			if (section.getKey().equals(key)) {
				return true;
			}
			for (AssetModel asset : section.getAssets()) {
				if (asset.getKey().equals(key)) {
					return true;
				}
			}
		}
		return false;
	}

	public void saveState(IMemento memento, Object element) {
		memento.putString(MEMENT_PROJECT_KEY, _file.getProject().getName());
		memento.putString(MEMENTO_KEY, getStringReference(element));
	}

	public String getStringReference(Object element) {
		JSONObject obj = getAssetJSONRefrence(element);
		return obj.toString();
	}

	public JSONObject getAssetJSONRefrence(Object element) {
		JSONObject obj = new JSONObject();
		obj.put("file", _file.getProjectRelativePath().toString());
		AssetSectionModel section = null;
		AssetGroupModel group = null;
		AssetModel asset = null;
		IAssetElementModel sprite = null;
		if (element instanceof AssetSectionModel) {
			section = (AssetSectionModel) element;
		} else if (element instanceof AssetGroupModel) {
			group = (AssetGroupModel) element;
			section = group.getSection();
		} else if (element instanceof AssetModel) {
			asset = (AssetModel) element;
			section = asset.getSection();
		} else if (element instanceof IAssetElementModel) {
			sprite = (IAssetElementModel) element;
			asset = sprite.getAsset();
			section = asset.getSection();
		}
		if (section != null) {
			obj.put("section", section.getKey());
		}
		if (group != null) {
			obj.put("group", group.getType().name());
		}
		if (asset != null) {
			obj.put("asset", asset.getKey());
		}
		if (sprite != null) {
			obj.put("sprite", sprite.getName());
		}
		return obj;
	}

	public Object getElementFromStringReference(String ref) {
		try {
			JSONObject obj = new JSONObject(ref);
			return getElementFromJSONReference(obj);
		} catch (JSONException e) {
			return null;
		}
	}

	public Object getElementFromJSONReference(JSONObject obj) {
		AssetSectionModel section = null;
		AssetGroupModel group = null;
		AssetModel asset = null;
		if (obj.has("section")) {
			section = findSection(obj.getString("section"));
		}
		if (obj.has("group")) {
			if (section == null) {
				return null;
			}
			group = section.getGroup(AssetType.valueOf(obj.getString("group")));
			return group;
		}
		if (obj.has("asset")) {
			if (section == null) {
				return null;
			}
			asset = section.findAsset(obj.getString("asset"));

			if (asset == null) {
				return null;
			}

			if (obj.has("sprite")) {
				String spriteName = obj.getString("sprite");
				for (IAssetElementModel elem : asset.getSubElements()) {
					if (elem.getName().equals(spriteName)) {
						return elem;
					}
				}
				return null;
			}

			return asset;
		}
		return section;
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		support.firePropertyChange(property, true, false);
	}

	public AssetPackModel getSharedVersion() {
		try {
			return AssetPackCore.getAssetPackModel(getFile(), false);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public boolean touched(IResourceDelta delta) {
		for (AssetModel asset : getAssets()) {
			if (asset.touched(delta)) {
				return true;
			}
		}
		return false;
	}
}
