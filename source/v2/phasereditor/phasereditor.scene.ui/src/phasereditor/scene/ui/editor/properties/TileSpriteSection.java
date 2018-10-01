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

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.TileSpriteComponent;
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

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(6, false));

		// tilePosition
		{
			var manager = new ToolBarManager();
			manager.add(new Action("Tile Position", EditorSharedImages.getImageDescriptor(IMG_EDIT_OBJ_PROPERTY)) {
				//
			});
			manager.createControl(comp);
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Tile Position");
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("X");
			label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

			_tilePositionXText = new Text(comp, SWT.BORDER);
			_tilePositionXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Y");

			_tilePositionYText = new Text(comp, SWT.BORDER);
			_tilePositionYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		// tileScale

		{
			var manager = new ToolBarManager();
			manager.add(new Action("Tile Scale", EditorSharedImages.getImageDescriptor(IMG_EDIT_OBJ_PROPERTY)) {
				//
			});
			manager.createControl(comp);
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Tile Scale");
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("X");
			label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

			_tileScaleXText = new Text(comp, SWT.BORDER);
			_tileScaleXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Y");

			_tileScaleYText = new Text(comp, SWT.BORDER);
			_tileScaleYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		// size

		{
			var manager = new ToolBarManager();
			manager.add(new Action("Tile Size", EditorSharedImages.getImageDescriptor(IMG_EDIT_OBJ_PROPERTY)) {
				//
			});
			manager.createControl(comp);
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Size");
		}

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Width");

			_widthText = new Text(comp, SWT.BORDER);
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 3;
			_widthText.setLayoutData(gd);
		}

		{
			new Label(comp, SWT.NONE);
			new Label(comp, SWT.NONE);

			var label = new Label(comp, SWT.NONE);
			label.setText("Height");

			_heightText = new Text(comp, SWT.BORDER);
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 3;
			_heightText.setLayoutData(gd);
		}

		update_UI_from_Model();

		return comp;
	}

	@Override
	public void update_UI_from_Model() {
		var models = List.of(getModels());

		_tilePositionXText.setText(flatValues_to_String(
				models.stream().map(model -> TileSpriteComponent.get_tilePositionX((ObjectModel) model))));
		_tilePositionYText.setText(flatValues_to_String(
				models.stream().map(model -> TileSpriteComponent.get_tilePositionY((ObjectModel) model))));

		listenFloat(_tilePositionXText, value -> {
			models.forEach(model -> TileSpriteComponent.set_tilePositionX((ObjectModel) model, value));
			getEditor().setDirty(true);
		}, models);

		listenFloat(_tilePositionYText, value -> {
			models.forEach(model -> TileSpriteComponent.set_tilePositionY((ObjectModel) model, value));
			getEditor().setDirty(true);
		}, models);

		_tileScaleXText.setText(flatValues_to_String(
				models.stream().map(model -> TileSpriteComponent.get_tileScaleX((ObjectModel) model))));
		_tileScaleYText.setText(flatValues_to_String(
				models.stream().map(model -> TileSpriteComponent.get_tileScaleY((ObjectModel) model))));

		listenFloat(_tileScaleXText, value -> {
			models.forEach(model -> TileSpriteComponent.set_tileScaleX((ObjectModel) model, value));
			getEditor().setDirty(true);
		}, models);

		listenFloat(_tileScaleYText, value -> {
			models.forEach(model -> TileSpriteComponent.set_tileScaleY((ObjectModel) model, value));
			getEditor().setDirty(true);
		}, models);

		_widthText.setText(
				flatValues_to_String(models.stream().map(model -> TileSpriteComponent.get_width((ObjectModel) model))));
		_heightText.setText(flatValues_to_String(
				models.stream().map(model -> TileSpriteComponent.get_height((ObjectModel) model))));

		listenFloat(_widthText, value -> {
			models.forEach(model -> TileSpriteComponent.set_width((ObjectModel) model, value));
			getEditor().setDirty(true);
		}, models);

		listenFloat(_heightText, value -> {
			models.forEach(model -> TileSpriteComponent.set_height((ObjectModel) model, value));
			getEditor().setDirty(true);
		}, models);

	}

}
