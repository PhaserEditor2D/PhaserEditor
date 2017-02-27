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

import org.eclipse.swt.graphics.RGB;
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

	public void write(JSONObject data) {
		data.put("scaleMode", _scaleMode, SCALE_MODE_NO_SCALE);
		data.put("pageAlignHorizontally", _pageAlignHorizontally, false);
		data.put("pageAlignVertically", _pageAlignVertically, false);
		EditorSettings.writeColor(data, "stageBackgroundColor", DEFAULT_STAGE_BG_COLOR);
		data.put("physicsSystem", _physicsSystem.name(), PhysicsType.NONE.name());
		data.put("rendererRoundPixels", _rendererRoundPixels, false);
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
