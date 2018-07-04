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

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.EditorSettings_UserCode;
import phasereditor.canvas.core.codegen.CanvasCodeGeneratorProvider;
import phasereditor.canvas.ui.editors.grid.PGridUserCodeProperty;
import phasereditor.project.core.codegen.SourceLang;
import phasereditor.ui.properties.PGridBooleanProperty;
import phasereditor.ui.properties.PGridColorProperty;
import phasereditor.ui.properties.PGridEnumProperty;
import phasereditor.ui.properties.PGridNumberProperty;
import phasereditor.ui.properties.PGridSection;
import phasereditor.ui.properties.PGridStringProperty;

/**
 * @author arian
 *
 */
public class EditorConfigItem extends ConfigItem {

	public EditorConfigItem(CanvasModel canvasModel) {
		super(canvasModel, "Editor");
	}

	@SuppressWarnings("boxing")
	@Override
	public void buildProperties() {
		EditorSettings settings = getSettings();

		{
			PGridSection section = new PGridSection("Scene");

			section.add(new PGridNumberProperty(null, "width", "The width of the canvas.") {

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

			section.add(new PGridNumberProperty(null, "height", "The height of the canvas.") {

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

			section.add(new PGridColorProperty(null, "backgroundColor", "The canvas background color.") {

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
					return !getValue().equals(EditorSettings.DEFAULT_BACKGROUND_COLOR);
				}
				
				@Override
				public RGB getDefaultValue() {
					return EditorSettings.DEFAULT_BACKGROUND_COLOR;
				}
			});

			section.add(new PGridColorProperty(null, "gridColor", "The canvas grid color.") {

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
					return !getValue().equals(EditorSettings.DEFAULT_GRID_COLOR);
				}
				
				@Override
				public RGB getDefaultValue() {
					return EditorSettings.DEFAULT_GRID_COLOR;
				}
			});

			section.add(new PGridBooleanProperty(null, "showGrid", "Show the grid.") {

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

		{
			PGridSection section = new PGridSection("Snapping");

			section.add(new PGridBooleanProperty(null, "enable", "Enable snapping.") {

				@Override
				public Boolean getValue() {
					return settings.isEnableStepping();
				}

				@Override
				public void setValue(Boolean value, boolean notify) {
					settings.setEnableStepping(value);
				}

				@Override
				public boolean isModified() {
					return settings.isEnableStepping();
				}
			});

			section.add(new PGridNumberProperty(null, "stepWidth", "Snapping step width.") {

				@Override
				public Double getValue() {
					return (double) settings.getStepWidth();
				}

				@Override
				public void setValue(Double value, boolean notify) {
					settings.setStepWidth(value.intValue());
				}

				@Override
				public boolean isModified() {
					return settings.getStepWidth() != 32;
				}
			});

			section.add(new PGridNumberProperty(null, "stepHeight", "Snapping step height.") {

				@Override
				public Double getValue() {
					return (double) settings.getStepHeight();
				}

				@Override
				public void setValue(Double value, boolean notify) {
					settings.setStepHeight(value.intValue());
				}

				@Override
				public boolean isModified() {
					return settings.getStepHeight() != 32;
				}
			});
			getGridModel().getSections().add(section);
		}

		{
			PGridSection section = new PGridSection("Source");

			section.add(new PGridStringProperty(null, "className", "The generated class name.") {

				@Override
				public void setValue(String value, boolean notify) {
					settings.setClassName(value);
				}

				@Override
				public String getValue() {
					return settings.getClassName();
				}

				@Override
				public boolean isModified() {
					return true;
				}
			});

			section.add(new PGridStringProperty(null, "baseClass", "The base class for the generated class.") {

				@Override
				public void setValue(String value, boolean notify) {
					settings.setBaseClass(value);
				}

				@Override
				public String getValue() {
					return settings.getBaseClass();
				}

				@Override
				public boolean isModified() {
					return !settings.getBaseClass()
							.equals(CanvasCodeGeneratorProvider.getDefaultBaseClassFor(getModel().getType()));
				}
			});

			section.add(new PGridUserCodeProperty(null, "userCode", "Code inserted into the generated source.") {

				@Override
				public void setValue(EditorSettings_UserCode value, boolean notify) {
					settings.setUserCode(value);
				}

				@Override
				public EditorSettings_UserCode getValue() {
					return settings.getUserCode();
				}

				@Override
				public boolean isModified() {
					return true;
				}
			});

			section.add(new PGridEnumProperty<>(null, "lang", "The language of the generated code.",
					SourceLang.values()) {

				@Override
				public SourceLang getValue() {
					return settings.getLang();
				}

				@Override
				public void setValue(SourceLang value, boolean notify) {
					settings.setLang(value);
				}

				@Override
				public boolean isModified() {
					return settings.getLang() != SourceLang.JAVA_SCRIPT;
				}
			});

			section.add(new PGridBooleanProperty(null, "generateOnSave",
					"Generate the source code when the editor is saved.") {

				@Override
				public Boolean getValue() {
					return settings.isGenerateOnSave();
				}

				@Override
				public void setValue(Boolean value, boolean notify) {
					settings.setGenerateOnSave(value);
				}

				@Override
				public boolean isModified() {
					return !settings.isGenerateOnSave();
				}
			});

			getGridModel().getSections().add(section);
		}
	}

}
