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
package phasereditor.canvas.ui.editors.config;

import org.eclipse.swt.graphics.RGB;

import phasereditor.canvas.core.CanvasMainSettings;
import phasereditor.canvas.ui.editors.grid.PGridBooleanProperty;
import phasereditor.canvas.ui.editors.grid.PGridColorProperty;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;

/**
 * @author arian
 *
 */
public class MainEditorConfig extends ConfigItem {

	public MainEditorConfig(CanvasMainSettings canvasModel) {
		super(canvasModel, "Editor");
	}

	@SuppressWarnings("boxing")
	@Override
	public void buildProperties() {
		CanvasMainSettings settings = getSettings();

		PGridSection section = new PGridSection("Scene");

		section.add(new PGridNumberProperty(getId(), "width", "The width of the canvas.") {

			@Override
			public Double getValue() {
				return settings.getSceneWidth();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				settings.setSceneWidth(value.doubleValue());
			}

			@Override
			public boolean isModified() {
				return true;
			}
		});

		section.add(new PGridNumberProperty(getId(), "height", "The height of the canvas.") {

			@Override
			public Double getValue() {
				return settings.getSceneHeight();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				settings.setSceneHeight(value.doubleValue());
			}

			@Override
			public boolean isModified() {
				return true;
			}
		});

		section.add(new PGridColorProperty(getId(), "backgroundColor", "The canvas background color.") {

			@Override
			public void setValue(RGB value, boolean notify) {
				settings.setBackgroundColor(value);
			}

			@Override
			public RGB getValue() {
				return settings.getBackgroundColor();
			}

			@Override
			public boolean isModified() {
				return !getValue().equals(CanvasMainSettings.DEFAULT_BACKGROUND_COLOR);
			}
		});

		section.add(new PGridColorProperty(getId(), "gridColor", "The canvas grid color.") {

			@Override
			public void setValue(RGB value, boolean notify) {
				settings.setGridColor(value);
			}

			@Override
			public RGB getValue() {
				return settings.getGridColor();
			}

			@Override
			public boolean isModified() {
				return !getValue().equals(CanvasMainSettings.DEFAULT_GRID_COLOR);
			}
		});

		section.add(new PGridBooleanProperty(getId(), "showGrid", "Show the grid.") {

			@Override
			public Boolean getValue() {
				return settings.isShowGrid();
			}

			@Override
			public void setValue(Boolean value, boolean notify) {
				settings.setShowGrid(value);
			}

			@Override
			public boolean isModified() {
				return !settings.isShowGrid();
			}
		});

		getGridModel().getSections().add(section);
	}

}
