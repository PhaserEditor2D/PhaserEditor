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

import java.util.Set;

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
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class TileSpriteSection extends ScenePropertySection {

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

			var text = new Text(comp2, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(text) {
				{
					dirtyModels = true;
				}

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TileSpriteComponent.set_width(model, value));

					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				text.setText(
						flatValues_to_String(getModels().stream().map(model -> TileSpriteComponent.get_width(model))));
			});

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

			var text = new Text(comp2, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			new SceneTextToFloat(text) {
				{
					dirtyModels = true;
				}

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TileSpriteComponent.set_height(model, value));

					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				text.setText(
						flatValues_to_String(getModels().stream().map(model -> TileSpriteComponent.get_height(model))));
			});

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

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(text) {
				{
					dirtyModels = true;
				}

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TileSpriteComponent.set_tilePositionX(model, value));

					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(
						getModels().stream().map(model -> TileSpriteComponent.get_tilePositionX(model))));
			});
		}

		{
			label(comp, "Y", "Phaser.GameObjects.TileSprite.tilePositionY",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			new SceneTextToFloat(text) {

				{
					dirtyModels = true;
				}

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TileSpriteComponent.set_tilePositionY(model, value));

					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(
						getModels().stream().map(model -> TileSpriteComponent.get_tilePositionY(model))));
			});

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

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(text) {
				{
					dirtyModels = true;
				}

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TileSpriteComponent.set_tileScaleX(model, value));

					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(
						getModels().stream().map(model -> TileSpriteComponent.get_tileScaleX(model))));
			});
		}

		{
			label(comp, "Y", "Phaser.GameObjects.TileSprite.tileScaleY",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(text) {
				{
					dirtyModels = true;
				}

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TileSpriteComponent.set_tileScaleY(model, value));

					getEditor().setDirty(true);

				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(
						getModels().stream().map(model -> TileSpriteComponent.get_tileScaleY(model))));
			});
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
					getEditor().setInteractiveTools("TilePosition");
				} else {
					getEditor().setInteractiveTools();
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
					getEditor().setInteractiveTools("TileScale");
				} else {
					getEditor().setInteractiveTools();
				}
			}
		};

		_sizeToolAction = new CommandSectionAction(this, SceneUIEditor.COMMAND_ID_RESIZE_TILE_SPRITE_TOOL);
		_sizeToolAction.setChecked(false);

		addUpdate(this::updateActions);

	}

	protected void resetSizeToTexture(boolean width, boolean height) {

		wrapOperation(() -> {
			for (var obj : getModels()) {
				var model = (TileSpriteModel) obj;
				var frame = TextureComponent.utils_getTexture(model, getEditor().getAssetFinder());
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
		});

		var editor = getEditor();

		editor.updatePropertyPagesContentWithSelection();

		editor.getScene().redraw();

		if (editor.getOutline() != null) {
			editor.refreshOutline_basedOnId();
		}

	}

	private void updateActions() {
		_positionToolAction.setChecked(getEditor().hasInteractiveTools(Set.of("TilePosition")));
		_sizeToolAction.setChecked(getEditor().hasInteractiveTools(Set.of("TileSize")));
		_scaleToolAction.setChecked(getEditor().hasInteractiveTools(Set.of("TileScale")));
	}

}
