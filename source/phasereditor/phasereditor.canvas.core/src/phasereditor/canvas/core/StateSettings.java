// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.RGB;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class StateSettings {
	public static final String SCALE_MODE_NO_SCALE = "NO_SCALE";

	public static final RGB DEFAULT_STAGE_BG_COLOR = new RGB(0, 0, 0);

	public static String[] SCALE_MODES = { SCALE_MODE_NO_SCALE, "SHOW_ALL", "RESIZE", "USER_SCALE" };

	private String _scaleMode = SCALE_MODE_NO_SCALE;
	private boolean _pageAlignHorizontally;
	private boolean _pageAlignVertically;
	private RGB _stageBackgroundColor = DEFAULT_STAGE_BG_COLOR;
	private PhysicsType _physicsSystem = PhysicsType.NONE;
	private boolean _rendererRoundPixels;
	private boolean _isPreloader = false;
	private String _preloadSpriteId = null;
	private PreloadSpriteDirection _preloadSprite_direction = PreloadSpriteDirection.HORIZONTAL;
	private Set<LoadPack> _loadPack = new LinkedHashSet<>();

	public static class LoadPack {
		private String _file;
		private String _section;

		public LoadPack(String file, String section) {
			super();
			_file = file;
			_section = section;
		}

		public String getFile() {
			return _file;
		}

		public void setFile(String file) {
			_file = file;
		}

		public String getSection() {
			return _section;
		}

		public void setSection(String section) {
			_section = section;
		}

		public static String toString(Set<LoadPack> value) {
			if (value == null) {
				return "[]";
			}

			String join = value.stream().map(e -> e.getSection()).collect(Collectors.joining(","));
			return "[" + join + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((_file == null) ? 0 : _file.hashCode());
			result = prime * result + ((_section == null) ? 0 : _section.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LoadPack other = (LoadPack) obj;
			if (_file == null) {
				if (other._file != null)
					return false;
			} else if (!_file.equals(other._file))
				return false;
			if (_section == null) {
				if (other._section != null)
					return false;
			} else if (!_section.equals(other._section))
				return false;
			return true;
		}

	}

	public enum PreloadSpriteDirection {
		HORIZONTAL, VERTICAL
	}

	public void write(JSONObject data) {
		data.put("scaleMode", _scaleMode, SCALE_MODE_NO_SCALE);
		data.put("pageAlignHorizontally", _pageAlignHorizontally, false);
		data.put("pageAlignVertically", _pageAlignVertically, false);
		EditorSettings.writeColor(data, "stageBackgroundColor", _stageBackgroundColor);
		data.put("physicsSystem", _physicsSystem.name(), PhysicsType.NONE.name());
		data.put("rendererRoundPixels", _rendererRoundPixels, false);
		data.put("isPreloader", _isPreloader, false);
		data.put("preloadSpriteId", _preloadSpriteId);
		data.put("preloadSprite_direction", _preloadSprite_direction.ordinal());
		{
			JSONArray list = new JSONArray();
			for (LoadPack section : _loadPack) {
				JSONObject obj = new JSONObject();
				obj.put("file", section.getFile());
				obj.put("section", section.getSection());
				list.put(obj);
			}
			data.put("load.pack", list);
		}
	}

	public void read(JSONObject data) {
		_scaleMode = data.optString("scaleMode", SCALE_MODE_NO_SCALE);
		_pageAlignHorizontally = data.optBoolean("pageAlignHorizontally", false);
		_pageAlignVertically = data.optBoolean("pageAlignVertically", false);
		_stageBackgroundColor = EditorSettings.readColor(data, "stageBackgroundColor", DEFAULT_STAGE_BG_COLOR);
		{
			String name = data.optString("physicsSystem", PhysicsType.NONE.name());
			_physicsSystem = PhysicsType.valueOf(name);
		}
		_rendererRoundPixels = data.optBoolean("rendererRoundPixels", false);
		_isPreloader = data.optBoolean("isPreloader");
		_preloadSpriteId = data.optString("preloadSpriteId");
		{
			_preloadSprite_direction = PreloadSpriteDirection.values()[data.optInt("preloadSprite_direction", 0)];
		}
		{
			JSONArray list = data.optJSONArray("load.pack");
			LinkedHashSet<LoadPack> loadpack = new LinkedHashSet<>();
			if (list != null) {
				for (int i = 0; i < list.length(); i++) {
					JSONObject obj = list.getJSONObject(i);
					loadpack.add(new LoadPack(obj.getString("file"), obj.getString("section")));
				}
			}
			_loadPack = loadpack;
		}
	}

	public Set<LoadPack> getLoadPack() {
		return _loadPack;
	}

	public void setLoadPack(Set<LoadPack> loadPack) {
		_loadPack = loadPack;
	}

	public boolean isPreloader() {
		return _isPreloader;
	}

	public void setPreloader(boolean isPreloader) {
		_isPreloader = isPreloader;
	}

	public String getPreloadSpriteId() {
		return _preloadSpriteId;
	}

	public void setPreloadSpriteId(String preloadSpriteId) {
		_preloadSpriteId = preloadSpriteId;
	}

	public PreloadSpriteDirection getPreloadSprite_direction() {
		return _preloadSprite_direction;
	}

	public void setPreloadSprite_direction(PreloadSpriteDirection preloadSprite_direction) {
		_preloadSprite_direction = preloadSprite_direction;
	}

	public boolean isRendererRoundPixels() {
		return _rendererRoundPixels;
	}

	public void setRendererRoundPixels(boolean rendererRoundPixels) {
		_rendererRoundPixels = rendererRoundPixels;
		firePropertyChange("rendererRoundPixels");
	}

	public String getScaleMode() {
		return _scaleMode;
	}

	public void setScaleMode(String scaleMode) {
		_scaleMode = scaleMode;
		firePropertyChange("scaleMode");
	}

	public boolean isPageAlignHorizontally() {
		return _pageAlignHorizontally;
	}

	public void setPageAlignHorizontally(boolean pageAlignHorizontally) {
		_pageAlignHorizontally = pageAlignHorizontally;
		firePropertyChange("pageAlignHorizontally");
	}

	public boolean isPageAlignVertically() {
		return _pageAlignVertically;
	}

	public void setPageAlignVertically(boolean pageAlignVertically) {
		_pageAlignVertically = pageAlignVertically;
		firePropertyChange("pageAlignVertically");
	}

	public RGB getStageBackgroundColor() {
		return _stageBackgroundColor;
	}

	public void setStageBackgroundColor(RGB stageBackgroundColor) {
		_stageBackgroundColor = stageBackgroundColor;
		firePropertyChange("stageBackgroundColor");
	}

	public PhysicsType getPhysicsSystem() {
		return _physicsSystem;
	}

	public void setPhysicsSystem(PhysicsType physicsSystem) {
		_physicsSystem = physicsSystem;
		firePropertyChange("physicsSystem");
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
}
