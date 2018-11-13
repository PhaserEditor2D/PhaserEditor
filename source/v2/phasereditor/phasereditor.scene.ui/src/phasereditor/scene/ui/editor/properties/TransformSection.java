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
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.interactive.PositionTool;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class TransformSection extends ScenePropertySection {

	private Text _xText;
	private Text _yText;
	private Text _scaleXText;
	private Text _scaleYText;
	private Text _angleText;
	private Action _localTransformAction;
	private Action _positionToolAction;
	private Action _scaleToolAction;
	private Action _angleToolAction;

	public TransformSection(ScenePropertyPage page) {
		super("Transform", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TransformComponent;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		
		manager.add(_positionToolAction);
		manager.add(_scaleToolAction);
		manager.add(_angleToolAction);
		
		manager.add(new Separator());

		manager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_ALIGN_LEFT)) {
			//
		});
		manager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_ALIGN_CENTER)) {
			//
		});
		manager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_ALIGN_RIGHT)) {
			//
		});

		manager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_ALIGN_TOP)) {
			//
		});
		manager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_ALIGN_MIDDLE)) {
			//
		});
		manager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_ALIGN_BOTTOM)) {
			//
		});
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		createActions();

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(6, false));

		{
			// position

			{
				var manager = new ToolBarManager();
				manager.add(_positionToolAction);
				manager.createControl(comp);
			}

			label(comp, "Position", "Phaser.GameObjects.Sprite.setPosition");

			label(comp, "X", "Phaser.GameObjects.Sprite.x");

			_xText = new Text(comp, SWT.BORDER);
			_xText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			label(comp, "Y", "Phaser.GameObjects.Sprite.y");

			_yText = new Text(comp, SWT.BORDER);
			_yText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		}

		{
			// scale

			var manager = new ToolBarManager();
			manager.add(_scaleToolAction);
			manager.createControl(comp);

			label(comp, "Scale", "Phaser.GameObjects.Sprite.setScale");

			label(comp, "X", "Phaser.GameObjects.Sprite.scaleX");

			_scaleXText = new Text(comp, SWT.BORDER);
			_scaleXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			label(comp, "Y", "Phaser.GameObjects.Sprite.scaleY");

			_scaleYText = new Text(comp, SWT.BORDER);
			_scaleYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		}

		{
			// angle

			var manager = new ToolBarManager();
			manager.add(_angleToolAction);
			manager.createControl(comp);

			label(comp, "Angle", "Phaser.GameObjects.Sprite.angle");

			new Label(comp, SWT.NONE);

			_angleText = new Text(comp, SWT.BORDER);
			_angleText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

			new Label(comp, SWT.NONE);
			new Label(comp, SWT.NONE);

		}

		return comp;
	}

	private void createActions() {
		_positionToolAction = new Action("Position tool.", IAction.AS_CHECK_BOX) {

			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_EDIT_POSITION));
			}

			@Override
			public void run() {
				if (isChecked()) {
					setInteractiveTools(

							new PositionTool(getEditor(), true, false), new PositionTool(getEditor(), false, true),
							new PositionTool(getEditor(), true, true)

					);
				} else {
					setInteractiveTools();
				}

			}
		};

		_scaleToolAction = new Action("Scale tool", EditorSharedImages.getImageDescriptor(IMG_EDIT_SCALE)) {
			//
		};
		 _angleToolAction = new Action("Angle tool", EditorSharedImages.getImageDescriptor(IMG_EDIT_ANGLE)) {
			//
		};
		
		_localTransformAction = new Action("Transform in local coords.") {

		};
		
		
	}

	@Override
	@SuppressWarnings("boxing")
	public void update_UI_from_Model() {

		var models = getModels();

		// x y

		_xText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_x(model))));
		_yText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_y(model))));

		listenFloat(_xText, value -> {
			models.forEach(model -> TransformComponent.set_x(model, value));
			getEditor().setDirty(true);
		}, models);

		listenFloat(_yText, value -> {
			models.forEach(model -> TransformComponent.set_y(model, value));
			getEditor().setDirty(true);
		}, models);

		// scale

		_scaleXText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_scaleX(model))));
		_scaleYText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_scaleY(model))));

		listenFloat(_scaleXText, value -> {
			models.forEach(model -> TransformComponent.set_scaleX(model, value));
			getEditor().setDirty(true);
		}, models);

		listenFloat(_scaleYText, value -> {
			models.forEach(model -> TransformComponent.set_scaleY(model, value));
			getEditor().setDirty(true);
		}, models);

		// angle

		_angleText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_angle(model))));

		listenFloat(_angleText, value -> {
			models.forEach(model -> TransformComponent.set_angle(model, value));
			getEditor().setDirty(true);
		}, models);

		updateActions();
	}

	private void updateActions() {
		_positionToolAction.setChecked(getEditor().getScene().hasInteractiveTool(PositionTool.class));
	}

}
