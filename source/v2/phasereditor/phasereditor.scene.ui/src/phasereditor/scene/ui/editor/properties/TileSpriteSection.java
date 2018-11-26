// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.scene.ui.editor.properties;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.interactive.TilePositionTool;
import phasereditor.scene.ui.editor.interactive.TileScaleTool;
import phasereditor.scene.ui.editor.interactive.TileSizeTool;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class TileSpriteSection extends ScenePropertySection {

	private Text _tilePositionXText;
	private Text _tilePositionYText;
	private Text _tileScaleXText;
	private Text _tileScaleYText;
	private Text _widthText;
	private Text _heightText;
	private Action _positionToolAction;
	private Action _sizeToolAction;
	private Action _scaleToolAction;
	private Action _resetTextureWidthAction;
	private Action _resetTextureHeightAction;

	public TileSpriteSection(FormPropertyPage page) {
		super("Tile Sprite", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TileSpriteComponent;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		createActions();

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(6, false));

		// size

		{
			var manager = new ToolBarManager();
			manager.add(_sizeToolAction);
			manager.createControl(comp);
		}

		{
			label(comp, "Tile Size", "Phaser.GameObjects.TileSprite.setSize");
		}

		{
			label(comp, "Width", "Phaser.GameObjects.TileSprite.width");

			var comp2 = new Composite(comp, SWT.NONE);
			var ly = new GridLayout(2, false);
			ly.marginWidth = 0;
			ly.marginHeight = 0;
			comp2.setLayout(ly);
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 3;
			comp2.setLayoutData(gd);

			_widthText = new Text(comp2, SWT.BORDER);
			_widthText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			var manager = new ToolBarManager();

			manager.add(_resetTextureWidthAction);
			manager.createControl(comp2);
		}

		{
			new Label(comp, SWT.NONE);
			new Label(comp, SWT.NONE);

			label(comp, "Height", "Phaser.GameObjects.TileSprite.height");

			var comp2 = new Composite(comp, SWT.NONE);
			var ly = new GridLayout(2, false);
			ly.marginWidth = 0;
			ly.marginHeight = 0;
			comp2.setLayout(ly);
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 3;
			comp2.setLayoutData(gd);

			_heightText = new Text(comp2, SWT.BORDER);
			_heightText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			var manager = new ToolBarManager();
			manager.add(_resetTextureHeightAction);
			manager.createControl(comp2);
		}

		// tilePosition
		{
			var manager = new ToolBarManager();
			manager.add(_positionToolAction);
			manager.createControl(comp);
		}

		{
			label(comp, "Tile Position", "Phaser.GameObjects.TileSprite.setTilePosition");
		}

		{
			label(comp, "X", "Phaser.GameObjects.TileSprite.tilePositionX",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));

			_tilePositionXText = new Text(comp, SWT.BORDER);
			_tilePositionXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		{
			label(comp, "Y", "Phaser.GameObjects.TileSprite.tilePositionY",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));

			_tilePositionYText = new Text(comp, SWT.BORDER);
			_tilePositionYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		// tileScale

		{
			var manager = new ToolBarManager();
			manager.add(_scaleToolAction);
			manager.createControl(comp);
		}

		{
			label(comp, "Tile Scale", "Phaser.GameObjects.TileSprite.setTileScale");
		}

		{
			label(comp, "X", "Phaser.GameObjects.TileSprite.tileScaleX",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));

			_tileScaleXText = new Text(comp, SWT.BORDER);
			_tileScaleXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		{
			label(comp, "Y", "Phaser.GameObjects.TileSprite.tileScaleY",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));

			_tileScaleYText = new Text(comp, SWT.BORDER);
			_tileScaleYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		return comp;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(_sizeToolAction);
		manager.add(_positionToolAction);
		manager.add(_scaleToolAction);
	}

	private void createActions() {
		_resetTextureWidthAction = new Action("Reset to texture width.",
				EditorSharedImages.getImageDescriptor(IMG_ARROW_REFRESH)) {
			@Override
			public void run() {
				resetSizeToTexture(true, false);
			}
		};

		_resetTextureHeightAction = new Action("Reset to texture height.",
				EditorSharedImages.getImageDescriptor(IMG_ARROW_REFRESH)) {
			@Override
			public void run() {
				resetSizeToTexture(false, true);
			}
		};

		_positionToolAction = new Action("Tile position tool.", IAction.AS_CHECK_BOX) {

			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_EDIT_TILE_POSITION));
			}

			@Override
			public void run() {
				if (isChecked()) {
					setInteractiveTools(

							new TilePositionTool(getEditor(), true, false),
							new TilePositionTool(getEditor(), false, true),
							new TilePositionTool(getEditor(), true, true)

					);
				} else {
					setInteractiveTools();
				}

			}
		};

		_scaleToolAction = new Action("Tile scale tool.", IAction.AS_CHECK_BOX) {

			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_EDIT_TILE_SCALE));
			}

			@Override
			public void run() {
				if (isChecked()) {
					setInteractiveTools(

							new TileScaleTool(getEditor(), true, false),

							new TileScaleTool(getEditor(), false, true),

							new TileScaleTool(getEditor(), true, true)

					);
				} else {
					setInteractiveTools();
				}
			}
		};

		_sizeToolAction = new Action("Tile size tool.", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_EDIT_SCALE));
			}

			@Override
			public void run() {
				if (isChecked()) {
					setInteractiveTools(

							new TileSizeTool(getEditor(), true, false),

							new TileSizeTool(getEditor(), false, true),

							new TileSizeTool(getEditor(), true, true)

					);
				} else {
					setInteractiveTools();
				}
			}
		};

	}

	protected void resetSizeToTexture(boolean width, boolean height) {

		var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

		for (var obj : getModels()) {
			var model = (TileSpriteModel) obj;
			var frame = TextureComponent.utils_getTexture(model, getEditor().getScene().getAssetFinder());
			if (frame != null) {
				var fd = frame.getFrameData();
				if (fd != null) {
					var size = fd.src;

					if (width) {
						TileSpriteComponent.set_width(model, size.width);
					}

					if (height) {
						TileSpriteComponent.set_height(model, size.height);
					}

					GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);
				}
			}
		}

		var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

		var editor = getEditor();

		editor.executeOperation(
				new SingleObjectSnapshotOperation(before, after, "Reset tile sprite size to texture size.", true));

		editor.updatePropertyPagesContentWithSelection();

		editor.getScene().redraw();

		if (editor.getOutline() != null) {
			editor.refreshOutline_basedOnId();
		}
	}

	@Override
	public void update_UI_from_Model() {
		var models = getModels();

		// tilePosition

		_tilePositionXText.setText(
				flatValues_to_String(models.stream().map(model -> TileSpriteComponent.get_tilePositionX(model))));
		_tilePositionYText.setText(
				flatValues_to_String(models.stream().map(model -> TileSpriteComponent.get_tilePositionY(model))));

		listenFloat(_tilePositionXText, value -> {

			models.forEach(model -> TileSpriteComponent.set_tilePositionX(model, value));

			getEditor().setDirty(true);

		}, models, true);

		listenFloat(_tilePositionYText, value -> {

			models.forEach(model -> TileSpriteComponent.set_tilePositionY(model, value));

			getEditor().setDirty(true);

		}, models, true);

		// tileScale

		_tileScaleXText
				.setText(flatValues_to_String(models.stream().map(model -> TileSpriteComponent.get_tileScaleX(model))));
		_tileScaleYText
				.setText(flatValues_to_String(models.stream().map(model -> TileSpriteComponent.get_tileScaleY(model))));

		listenFloat(_tileScaleXText, value -> {

			models.forEach(model -> TileSpriteComponent.set_tileScaleX(model, value));

			getEditor().setDirty(true);

		}, models, true);

		listenFloat(_tileScaleYText, value -> {

			models.forEach(model -> TileSpriteComponent.set_tileScaleY(model, value));

			getEditor().setDirty(true);

		}, models, true);

		// size

		_widthText.setText(flatValues_to_String(models.stream().map(model -> TileSpriteComponent.get_width(model))));
		_heightText.setText(flatValues_to_String(models.stream().map(model -> TileSpriteComponent.get_height(model))));

		listenFloat(_widthText, value -> {

			models.forEach(model -> TileSpriteComponent.set_width(model, value));

			getEditor().setDirty(true);

		}, models, true);

		listenFloat(_heightText, value -> {

			models.forEach(model -> TileSpriteComponent.set_height(model, value));

			getEditor().setDirty(true);

		}, models, true);

		updateActions();

	}

	private void updateActions() {
		var scene = getEditor().getScene();

		_positionToolAction.setChecked(scene.hasInteractiveTool(TilePositionTool.class));
		_scaleToolAction.setChecked(scene.hasInteractiveTool(TileScaleTool.class));
		_sizeToolAction.setChecked(scene.hasInteractiveTool(TileSizeTool.class));
	}

}
