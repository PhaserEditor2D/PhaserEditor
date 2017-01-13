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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

import org.eclipse.swt.graphics.RGB;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author arian
 */
public class EditorSettings {
	public static final RGB DEFAULT_BACKGROUND_COLOR = new RGB(180, 180, 180);
	public static final RGB DEFAULT_GRID_COLOR = new RGB(200, 200, 200);

	private double _sceneWidth;
	private double _sceneHeight;
	private RGB _backgroundColor;
	private boolean _showGrid;
	private boolean _generateOnSave;
	private boolean _enableStepping;
	private int _stepWidth;
	private int _stepHeight;
	private SourceLang _lang;
	private String _baseClass;
	private RGB _gridColor;

	public EditorSettings() {
		_sceneWidth = 640;
		_sceneHeight = 360;
		_backgroundColor = DEFAULT_BACKGROUND_COLOR;
		_generateOnSave = true;
		_enableStepping = false;
		_stepWidth = 32;
		_stepHeight = 32;
		_lang = SourceLang.JAVA_SCRIPT;
		// for backward compatibility
		_baseClass = "Phaser.Group";
		_backgroundColor = DEFAULT_BACKGROUND_COLOR;
		_gridColor = DEFAULT_GRID_COLOR;
		_showGrid = true;
	}

	public EditorSettings(JSONObject settingsData) {
		read(settingsData);
	}

	public boolean isShowGrid() {
		return _showGrid;
	}

	public void setShowGrid(boolean showGrid) {
		_showGrid = showGrid;
		firePropertyChange("showGrid");
	}

	public RGB getGridColor() {
		return _gridColor;
	}

	public void setGridColor(RGB gridColor) {
		_gridColor = gridColor;
		firePropertyChange("gridColor");
	}

	public String getBaseClass() {
		return _baseClass;
	}

	public void setBaseClass(String baseClass) {
		_baseClass = baseClass;
		firePropertyChange("baseClass");
	}

	public SourceLang getLang() {
		return _lang;
	}

	public void setLang(SourceLang lang) {
		_lang = lang;
		firePropertyChange("lang");
	}

	public double getSceneWidth() {
		return _sceneWidth;
	}

	public void setSceneWidth(double sceneWidth) {
		_sceneWidth = sceneWidth;
		firePropertyChange("sceneWidth");
	}

	public double getSceneHeight() {
		return _sceneHeight;
	}

	public void setSceneHeight(double sceneHeight) {
		_sceneHeight = sceneHeight;
		firePropertyChange("sceneHeight");
	}

	public RGB getBackgroundColor() {
		return _backgroundColor;
	}

	public void setBackgroundColor(RGB sceneColor) {
		_backgroundColor = sceneColor;
		firePropertyChange("sceneColor");
	}

	public boolean isGenerateOnSave() {
		return _generateOnSave;
	}

	public void setGenerateOnSave(boolean generateOnScave) {
		_generateOnSave = generateOnScave;
		firePropertyChange("generateOnScave");
	}

	public boolean isEnableStepping() {
		return _enableStepping;
	}

	public void setEnableStepping(boolean enableStepping) {
		_enableStepping = enableStepping;
		firePropertyChange("enableStepping");
	}

	public int getStepWidth() {
		return _stepWidth;
	}

	public void setStepWidth(int stepWidth) {
		_stepWidth = stepWidth;
		firePropertyChange("stepWidth");
	}

	public int getStepHeight() {
		return _stepHeight;
	}

	public void setStepHeight(int stepHeight) {
		_stepHeight = stepHeight;
		firePropertyChange("stepHeight");
	}

	public void write(JSONObject obj) {
		obj.put("sceneWidth", _sceneWidth);
		obj.put("sceneHeight", _sceneHeight);
		obj.put("generateOnSave", _generateOnSave);
		obj.put("enableStepping", _enableStepping);
		obj.put("stepWidth", _stepWidth, 32);
		obj.put("stepHeight", _stepHeight, 32);
		obj.put("lang", _lang);
		obj.put("baseClass", _baseClass);
		writeColor(obj, "backgroundColor", _backgroundColor);
		writeColor(obj, "gridColor", _gridColor);
		obj.put("showGrid", _showGrid);
	}

	private static RGB readColor(JSONObject obj, String key, RGB def) {
		JSONArray array = obj.optJSONArray(key);
		return array == null ? def : new RGB(array.getInt(0), array.getInt(1), array.getInt(2));
	}

	@SuppressWarnings("boxing")
	private static void writeColor(JSONObject obj, String key, RGB color) {
		if (color != null) {
			obj.put(key, Arrays.asList(color.red, color.green, color.blue));
		}
	}

	public void read(JSONObject obj) {
		_sceneWidth = obj.getDouble("sceneWidth");
		_sceneHeight = obj.getDouble("sceneHeight");
		_generateOnSave = obj.getBoolean("generateOnSave");
		_enableStepping = obj.optBoolean("enableStepping", false);
		_stepWidth = obj.optInt("stepWidth", 32);
		_stepHeight = obj.optInt("stepHeight", 32);
		_lang = SourceLang.valueOf(obj.optString("lang", SourceLang.JAVA_SCRIPT.name()));
		// use Phaser.Group for backward compatibility
		_baseClass = obj.optString("baseClass", "Phaser.Group");

		_backgroundColor = readColor(obj, "backgroundColor", DEFAULT_BACKGROUND_COLOR);
		_gridColor = readColor(obj, "gridColor", DEFAULT_GRID_COLOR);
		_showGrid = obj.optBoolean("showGrid", true);
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
