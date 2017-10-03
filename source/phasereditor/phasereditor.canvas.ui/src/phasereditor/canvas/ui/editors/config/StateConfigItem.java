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

import java.util.Set;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.RGB;

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.PhysicsType;
import phasereditor.canvas.core.StateSettings;
import phasereditor.canvas.core.StateSettings.LoadPack;
import phasereditor.canvas.core.StateSettings.PreloadSpriteDirection;
import phasereditor.canvas.ui.editors.grid.PGridBooleanProperty;
import phasereditor.canvas.ui.editors.grid.PGridColorProperty;
import phasereditor.canvas.ui.editors.grid.PGridEnumProperty;
import phasereditor.canvas.ui.editors.grid.PGridLoadPackProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridSpriteProperty;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

/**
 * @author arian
 *
 */
public class StateConfigItem extends ConfigItem {

	private TreeViewer _viewer;

	public StateConfigItem(CanvasModel model, TreeViewer viewer) {
		super(model, "State");
		_viewer = viewer;
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

		{
			PGridSection section = new PGridSection("Renderer");
			section.add(new PGridBooleanProperty(null, "roundPixels",
					"If you find you're getting a slight \"jitter\" effect when following a Sprite it's probably to do with sub-pixel rendering of the Sprite position.\nThis can be disabled by setting game.renderer.renderSession.roundPixels = true to force full pixel rendering. (Comment taken from Phaser.Camera.follow())") {

				@Override
				public Boolean getValue() {
					return state.isRendererRoundPixels();
				}

				@Override
				public void setValue(Boolean value, boolean notify) {
					state.setRendererRoundPixels(value);
				}

				@Override
				public boolean isModified() {
					return state.isRendererRoundPixels();
				}
			});
			getGridModel().getSections().add(section);
		}

		{
			PGridSection section = new PGridSection("Preload");
			
			section.add(new PGridBooleanProperty(null, "autoLoad",
					"Phaser Editor: If true get all the objects of the scene and compute the sections to be loaded.\nSet to false if you loaded the assets in other secene (like in a Preloader scene).") {

				@Override
				public Boolean getValue() {
					return state.isAutoLoad();
				}

				@SuppressWarnings("synthetic-access")
				@Override
				public void setValue(Boolean value, boolean notify) {
					state.setAutoLoad(value);
					_viewer.refresh();
				}

				@Override
				public boolean isModified() {
					return !state.isAutoLoad();
				}
			});

			section.add(new PGridLoadPackProperty(null, "pack", help.getMemberHelp("Phaser.Loader.pack")) {

				@Override
				public void setValue(Set<LoadPack> value, boolean notify) {
					state.setLoadPack(value);
				}

				@Override
				public boolean isModified() {
					return !state.getLoadPack().isEmpty();
				}

				@Override
				public Set<LoadPack> getValue() {
					return state.getLoadPack();
				}
			});

			section.add(new PGridBooleanProperty(null, "isPreloader",
					"Phaser Editor: Check this for states used as preloader of the assets.\nThe objects creation code is placed in the 'preload' method instead of the 'create' one.") {

				@Override
				public Boolean getValue() {
					return state.isPreloader();
				}

				@SuppressWarnings("synthetic-access")
				@Override
				public void setValue(Boolean value, boolean notify) {
					state.setPreloader(value);
					_viewer.refresh();
				}

				@Override
				public boolean isModified() {
					return state.isPreloader();
				}
			});

			section.add(new PGridSpriteProperty(null, "preloadSprite",
					help.getMemberHelp("Phaser.Loader.setPreloadSprite")) {

				@Override
				public String getValue() {
					return state.getPreloadSpriteId();
				}

				@Override
				public void setValue(String value, boolean notify) {
					state.setPreloadSpriteId(value);
				}

				@Override
				public boolean isModified() {
					return true;
				}

				@Override
				public boolean isActive() {
					return state.isPreloader();
				}
			});

			section.add(new PGridEnumProperty<PreloadSpriteDirection>(null, "preloadSprite.direction",
					help.getMethodArgHelp("Phaser.Loader.setPreloadSprite", "direction"),
					PreloadSpriteDirection.values()) {

				@Override
				public PreloadSpriteDirection getValue() {
					return state.getPreloadSprite_direction();
				}

				@Override
				public void setValue(PreloadSpriteDirection value, boolean notify) {
					state.setPreloadSprite_direction(value);
				}

				@Override
				public boolean isModified() {
					return state.getPreloadSprite_direction() != PreloadSpriteDirection.HORIZONTAL;
				}

				@Override
				public boolean isActive() {
					return state.isPreloader();
				}

			});

			getGridModel().getSections().add(section);
		}
	}

}
