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
package phasereditor.atlas.ui.editors;

import static phasereditor.ui.PhaserEditorUI.isEditorSupportedImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.atlas.core.AtlasFrame;
import phasereditor.atlas.core.SettingsBean;
import phasereditor.ui.PhaserEditorUI;

public class AtlasGeneratorEditorModel {

	private List<IFile> _imageFiles;
	private IFile _file;
	private SettingsBean _settings;
	private List<EditorPage> _pages;
	private int _version;
	HashMap<AtlasFrame, String> _frameRegionNameMap;

	public static int CURRENT_VERSION = 2;

	public static class EditorPage extends ArrayList<AtlasFrame> {
		private static final long serialVersionUID = 1L;
		private int _index;
		private AtlasGeneratorEditorModel _model;
		
		public EditorPage(AtlasGeneratorEditorModel model, int index) {
			super();
			_model = model;
			_index = index;
		}
		
		public int getIndex() {
			return _index;
		}
		
		public String getName() {
			return _model.getAtlasImageName(_index);
		}
		
	}

	public AtlasGeneratorEditorModel(IFile file) throws IOException, CoreException {
		_file = file;
		_imageFiles = new ArrayList<>();
		_pages = new ArrayList<>();
		_settings = new SettingsBean();
		_frameRegionNameMap = new HashMap<>();

		if (file != null) {
			readFile(file);
		}
	}
	
	public HashMap<AtlasFrame, String> getFrameRegionNameMap() {
		return _frameRegionNameMap;
	}

	public SettingsBean getSettings() {
		return _settings;
	}

	public void setSettings(SettingsBean settings) {
		_settings = settings;
	}

	public void readFile(IFile file) throws IOException, CoreException {
		_imageFiles.clear();
		_settings = new SettingsBean();
		try (InputStream contents = file.getContents()) {
			JSONObject obj = new JSONObject(new JSONTokener(contents));

			_version = obj.optInt("version", 1);

			JSONArray jsonFiles = obj.getJSONArray("files");
			for (int i = 0; i < jsonFiles.length(); i++) {
				String pathStr = jsonFiles.getString(i);
				IPath path = new Path(pathStr);

				if (_version == 1) {
					// in version 1 the path was relative to the workspace but
					// in version 2 it is relative to the project, so we have to
					// remove the project segment.
					path = path.removeFirstSegments(1);
				}

				IFile imgfile = file.getProject().getFile(path);

				_imageFiles.add(imgfile);
			}

			JSONObject jsonSettings = obj.getJSONObject("settings");
			_settings.read(jsonSettings);
		}
	}

	public IFile getFile() {
		return _file;
	}

	public void setFile(IFile file) {
		_file = file;
	}

	public List<IFile> getImageFiles() {
		return _imageFiles;
	}

	public void addImageFiles(List<IFile> imageFiles) {
		Set<IFile> set = new HashSet<>(_imageFiles);
		for (IFile file : imageFiles) {
			if (isEditorSupportedImage(file) && !set.contains(file)) {
				_imageFiles.add(file);
				set.add(file);
			}
		}
	}

	public void setPages(List<EditorPage> pages) {
		_pages = pages;
	}

	public List<EditorPage> getPages() {
		return _pages;
	}

	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		obj.put("version", CURRENT_VERSION);

		JSONArray jsonFiles = new JSONArray();
		if (_file != null) {
			for (IFile file : _imageFiles) {
				IPath path = file.getProjectRelativePath();
				jsonFiles.put(path.toPortableString());
			}
		}
		obj.put("files", jsonFiles);

		JSONObject jsonSettings = new JSONObject();
		_settings.write(jsonSettings);
		obj.put("settings", jsonSettings);
		return obj;
	}

	public JSONObject toPhaser3MultiatlasJSON() {
		JSONObject jsonData = new JSONObject();

		JSONArray jsonTextures = new JSONArray();
		jsonData.put("textures", jsonTextures);

		int pageIndex = 0;
		for (EditorPage page : _pages) {
			JSONObject jsonTexture = new JSONObject();
			jsonTextures.put(jsonTexture);
			jsonTexture.put("image", getAtlasImageName(pageIndex));

			JSONArray jsonFrames = new JSONArray();
			jsonTexture.put("frames", jsonFrames);

			for (AtlasFrame frame : page) {
				JSONObject jsonFrame = new JSONObject();
				jsonFrames.put(jsonFrame);

				jsonFrame.put("filename", frame.getName());
				writeFrameJsonData(frame, jsonFrame);
			}
			pageIndex++;
		}

		JSONObject meta = new JSONObject();
		jsonData.put("meta", meta);
		writeJsonMeta(meta);

		return jsonData;
	}

	public JSONObject[] toPhaserHashJSON() {
		JSONObject[] list = new JSONObject[_pages.size()];

		int i = 0;
		for (EditorPage page : _pages) {
			JSONObject obj = new JSONObject();
			list[i] = obj;

			JSONObject jsonFrames = new JSONObject();
			obj.put("frames", jsonFrames);

			for (AtlasFrame frame : page) {

				JSONObject jsonEntry = new JSONObject();
				jsonFrames.put(frame.getName(), jsonEntry);

				writeFrameJsonData(frame, jsonEntry);

			}
			JSONObject jsonMeta = new JSONObject();
			obj.put("meta", jsonMeta);

			jsonMeta.put("image", getAtlasImageName(i));

			writeJsonMeta(jsonMeta);

			i++;
		}

		return list;
	}

	private static void writeJsonMeta(JSONObject jsonMeta) {
		jsonMeta.put("app", "Phaser Editor - Atlas Generator");
		jsonMeta.put("version", "2");
	}

	private void writeFrameJsonData(AtlasFrame frame, JSONObject jsonEntry) {
		JSONObject jsonFrame = new JSONObject();
		jsonEntry.put("trimmed", _settings.stripWhitespaceX || _settings.stripWhitespaceY);
		jsonEntry.put("rotated", _settings.rotation);
		jsonEntry.put("frame", jsonFrame);

		jsonFrame.put("x", frame.getFrameX());
		jsonFrame.put("y", frame.getFrameY());
		jsonFrame.put("w", frame.getFrameW());
		jsonFrame.put("h", frame.getFrameH());

		JSONObject jsonSpriteSourceSize = new JSONObject();
		jsonEntry.put("spriteSourceSize", jsonSpriteSourceSize);

		jsonSpriteSourceSize.put("x", frame.getSpriteX());
		jsonSpriteSourceSize.put("y", frame.getSpriteY());
		jsonSpriteSourceSize.put("w", frame.getSpriteW());
		jsonSpriteSourceSize.put("h", frame.getSpriteH());

		JSONObject jsonSourceSize = new JSONObject();
		jsonEntry.put("sourceSize", jsonSourceSize);

		jsonSourceSize.put("w", frame.getSourceW());
		jsonSourceSize.put("h", frame.getSourceH());
	}

	public String getAtlasImageName(int i) {
		return getName(i) + ".png";
	}

	public String getAtlasJSONName(int i) {
		return getName(i) + ".json";
	}

	public String getAtlasName() {
		return PhaserEditorUI.getNameFromFilename(_file.getName());
	}

	private String getName(int i) {
		String name = getAtlasName();
		if (_pages.size() > 1) {
			name += i + 1;
		}
		return name;
	}

	public List<IFile> guessOutputFiles() {
		IContainer parent = _file.getParent();
		List<IFile> list = new ArrayList<>();
		for (int i = 0; i < _pages.size(); i++) {
			{
				IFile file = parent.getFile(new Path(getAtlasImageName(i)));
				list.add(file);
			}
			{
				IFile file = parent.getFile(new Path(getAtlasJSONName(i)));
				list.add(file);
			}
		}
		return list;
	}

}
