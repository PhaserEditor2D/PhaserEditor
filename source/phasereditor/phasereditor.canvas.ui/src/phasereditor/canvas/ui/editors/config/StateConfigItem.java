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
import phasereditor.canvas.core.PhysicsType;
import phasereditor.canvas.core.StateSettings;
import phasereditor.canvas.ui.editors.grid.PGridBooleanProperty;
import phasereditor.canvas.ui.editors.grid.PGridColorProperty;
import phasereditor.canvas.ui.editors.grid.PGridEnumProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

/**
 * @author arian
 *
 */
public class StateConfigItem extends ConfigItem {

	public StateConfigItem(CanvasModel model) {
		super(model, "State");
	}

	@SuppressWarnings("boxing")
	@Override
	public void buildProperties() {
		StateSettings state = getModel().getStateSettings();

		PhaserJSDoc help = InspectCore.getPhaserHelp();
		{
			PGridSection section = new PGridSection("Scale");
			section.add(new PGridEnumProperty<String>(null, "scaleMode",
					help.getMemberHelp("Phaser.ScaleManager.scaleMode"), StateSettings.SCALE_MODES) {

				@Override
				public String getValue() {
					return state.getScaleMode();
				}

				@Override
				public void setValue(String value, boolean notify) {
					state.setScaleMode(value);
				}

				@Override
				public boolean isModified() {
					return !state.getScaleMode().equals(StateSettings.SCALE_MODE_NO_SCALE);
				}
			});

			section.add(new PGridBooleanProperty(null, "pageAlignHorizontally",
					help.getMemberHelp("Phaser.ScaleManager.pageAlignHorizontally")) {

				@Override
				public void setValue(Boolean value, boolean notify) {
					state.setPageAlignHorizontally(value);
				}

				@Override
				public Boolean getValue() {
					return state.isPageAlignHorizontally();
				}

				@Override
				public boolean isModified() {
					return state.isPageAlignHorizontally();
				}
			});
			section.add(new PGridBooleanProperty(null, "pageAlignVertically",
					help.getMemberHelp("Phaser.ScaleManager.pageAlignVertically")) {

				@Override
				public void setValue(Boolean value, boolean notify) {
					state.setPageAlignVertically(value);
				}

				@Override
				public Boolean getValue() {
					return state.isPageAlignVertically();
				}

				@Override
				public boolean isModified() {
					return state.isPageAlignVertically();
				}
			});

			getGridModel().getSections().add(section);
		}

		{
			PGridSection section = new PGridSection("Physics");
			section.add(new PGridEnumProperty<PhysicsType>(null, "startSystem",
					help.getMemberHelp("Phaser.Physics.startSystem"), PhysicsType.values()) {

				@Override
				public PhysicsType getValue() {
					return state.getPhysicsSystem();
				}

				@Override
				public void setValue(PhysicsType value, boolean notify) {
					state.setPhysicsSystem(value);
				}

				@Override
				public boolean isModified() {
					return state.getPhysicsSystem() != PhysicsType.NONE;
				}
			});
			getGridModel().getSections().add(section);
		}

		{
			PGridSection section = new PGridSection("Stage");
			section.add(new PGridColorProperty(null, "backgroundColor",
					help.getMemberHelp("Phaser.Stage.backgroundColor")) {

				@Override
				public void setValue(RGB value, boolean notify) {
					state.setStageBackgroundColor(value);
				}

				@Override
				public RGB getValue() {
					return state.getStageBackgroundColor();
				}

				@Override
				public boolean isModified() {
					return !state.getStageBackgroundColor().equals(StateSettings.DEFAULT_STAGE_BG_COLOR);
				}
			});
			getGridModel().getSections().add(section);
		}
	}

}
