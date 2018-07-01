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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.atlas.core.AtlasFrame;
import phasereditor.atlas.core.SettingsBean;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.properties.PGridBooleanProperty;
import phasereditor.ui.properties.PGridModel;
import phasereditor.ui.properties.PGridNumberProperty;
import phasereditor.ui.properties.PGridSection;

public class AtlasGeneratorEditorModel implements IAdaptable {

	private List<IFile> _imageFiles;
	private IFile _file;
	private SettingsBean _settings;
	private List<EditorPage> _pages;
	private int _version;
	private PGridModel _gridModel;
	private AtlasGeneratorEditor _editor;
	private BuildResult _buildResult;

	public static int CURRENT_VERSION = 3;

	public AtlasGeneratorEditorModel(AtlasGeneratorEditor editor, IFile file) throws IOException, CoreException {
		_editor = editor;
		_file = file;
		_imageFiles = new ArrayList<>();
		_pages = new ArrayList<>();
		_settings = new SettingsBean();

		if (file != null) {
			readFile(file);
		}
	}

	public BuildResult getBuildResult() {
		return _buildResult;
	}

	public void setBuildResult(BuildResult buildResult) {
		_buildResult = buildResult;
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
				IFile imgfile = file.getProject().getFile(path);
				_imageFiles.add(imgfile);
			}

			JSONObject jsonSettings = obj.getJSONObject("settings");
			_settings.read(jsonSettings);
		}
	}

	public int getVersion() {
		return _version;
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

		{
			obj.put("version", CURRENT_VERSION);
		}

		{
			JSONArray jsonFiles = new JSONArray();
			if (_file != null) {
				for (IFile file : _imageFiles) {
					IPath path = file.getProjectRelativePath();
					jsonFiles.put(path.toPortableString());
				}
			}
			obj.put("files", jsonFiles);
		}

		{
			JSONObject jsonSettings = new JSONObject();
			_settings.write(jsonSettings);
			obj.put("settings", jsonSettings);
		}
		
		{
			JSONArray jsonPages = new JSONArray();
			obj.put("pages", jsonPages);
			for(EditorPage page : _pages) {
				JSONArray jsonPage = new JSONArray();
				jsonPages.put(jsonPage);
				for(AtlasEditorFrame frame : page) {
					JSONObject jsonFrame = new JSONObject();
					jsonPage.put(jsonFrame);
					writeFrameJsonData(frame, jsonFrame);
				}
			}
		}

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

			for (AtlasEditorFrame frame : page) {
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

	public PGridModel getGridModel() {
		if (_gridModel == null) {
			_gridModel = createGridModel();
		}
		return _gridModel;
	}

	@SuppressWarnings("boxing")
	private PGridModel createGridModel() {
		PGridModel model = new PGridModel();

		PGridSection section = new PGridSection("Layout");
		model.getSections().add(section);

		section.add(new PGridNumberProperty("Min Width", "The minimum width of output pages.", true) {

			@Override
			public Double getValue() {
				return (double) getSettings().minWidth;
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getSettings().minWidth = value.intValue();
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().minWidth != 16;
			}
		});

		section.add(new PGridNumberProperty("Min Height", "The minimum height of output pages.", true) {

			@Override
			public Double getValue() {
				return (double) getSettings().minHeight;
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getSettings().minHeight = value.intValue();
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().minHeight != 16;
			}
		});

		section.add(new PGridNumberProperty("Max Width",
				"The maximum width of output pages.\r\n1024 is safe for all devices.\r\nExtremely old devices may have degraded performance over 512.",
				true) {

			@Override
			public Double getValue() {
				return (double) getSettings().maxWidth;
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getSettings().maxWidth = value.intValue();
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().maxWidth != 16;
			}
		});

		section.add(new PGridNumberProperty("Max Height",
				"The maximum height of output pages.\r\n1024 is safe for all devices.\r\nExtremely old devices may have degraded performance over 512.",
				true) {

			@Override
			public Double getValue() {
				return (double) getSettings().maxHeight;
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getSettings().maxHeight = value.intValue();
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().maxHeight != 16;
			}
		});

		section.add(new PGridBooleanProperty("Size Power of Two") {

			@Override
			public Boolean getValue() {
				return getSettings().pot;
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getSettings().pot = value;
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().pot;
			}
		});

		section = new PGridSection("Sprites");
		model.getSections().add(section);

		section.add(new PGridNumberProperty("Padding X", "The number of pixels between packed images on the x-axis.",
				true) {

			@Override
			public void setValue(Double value, boolean notify) {
				getSettings().paddingX = value.intValue();
				afterPropertyUpdate();
			}

			@Override
			public Double getValue() {
				return (double) getSettings().paddingX;
			}

			@Override
			public boolean isModified() {
				return getSettings().paddingX != 2;
			}
		});

		section.add(new PGridNumberProperty("Padding Y", "The number of pixels between packed images on the y-axis.",
				true) {

			@Override
			public void setValue(Double value, boolean notify) {
				getSettings().paddingY = value.intValue();
				afterPropertyUpdate();
			}

			@Override
			public Double getValue() {
				return (double) getSettings().paddingY;
			}

			@Override
			public boolean isModified() {
				return getSettings().paddingY != 2;
			}
		});

		section.add(new PGridBooleanProperty("Strip Whitespace X",
				"If true, blank pixels on the left and right edges of input images\r\nwill be removed. Applications must take special care to draw\r\nthese regions properly.") {

			@Override
			public Boolean getValue() {
				return getSettings().stripWhitespaceX;
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getSettings().stripWhitespaceX = value;
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return !getSettings().stripWhitespaceX;
			}
		});

		section.add(new PGridBooleanProperty("Strip Whitespace Y",
				"If true, blank pixels on the left and right edges of input images\r\nwill be removed. Applications must take special care to draw\r\nthese regions properly.") {

			@Override
			public Boolean getValue() {
				return getSettings().stripWhitespaceY;
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getSettings().stripWhitespaceY = value;
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return !getSettings().stripWhitespaceY;
			}
		});

		section = new PGridSection("Flags");
		model.getSections().add(section);

		section.add(new PGridBooleanProperty("Alias",
				"If true, two images that are pixel for pixel the same will only be packed once.") {

			@Override
			public Boolean getValue() {
				return getSettings().alias;
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getSettings().alias = value;
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return !getSettings().alias;
			}
		});

		section.add(new PGridBooleanProperty("Use Indexes",
				"If true, images are sorted by parsing the sufix of the file names\r\n(eg. animation_01.png, animation_02.png, ...)") {

			@Override
			public Boolean getValue() {
				return getSettings().useIndexes;
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getSettings().useIndexes = value;
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().useIndexes;
			}
		});

		section.add(new PGridBooleanProperty("Grid", "If true, images are packed in a uniform grid, in order.") {

			@Override
			public Boolean getValue() {
				return getSettings().grid;
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getSettings().grid = value;
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().grid;
			}
		});

		section.add(new PGridBooleanProperty("Multi-atlas",
				"If true, use the multiple atlas Phaser 3 JSON format\n(a single atlas JSON file for multiple textures).") {

			@Override
			public Boolean getValue() {
				return getSettings().multiatlas;
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getSettings().multiatlas = value;
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().multiatlas;
			}
		});

		section.add(new PGridBooleanProperty("Debug",
				"If true, lines are drawn on the output pages\nto show the packed image bounds.") {

			@Override
			public Boolean getValue() {
				return getSettings().debug;
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				getSettings().debug = value;
				afterPropertyUpdate();
			}

			@Override
			public boolean isModified() {
				return getSettings().debug;
			}

		});

		return model;
	}

	protected void afterPropertyUpdate() {
		_editor.dirtify();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == PGridModel.class) {
			return getGridModel();
		}
		return null;
	}

}
