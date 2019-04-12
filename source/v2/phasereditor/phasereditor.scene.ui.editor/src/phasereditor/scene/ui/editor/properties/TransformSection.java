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
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.scene.ui.editor.interactive.AngleTool;
import phasereditor.scene.ui.editor.interactive.PositionTool;
import phasereditor.scene.ui.editor.interactive.ScaleTool;
import phasereditor.scene.ui.editor.messages.RunPositionActionMessage;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class TransformSection extends ScenePropertySection {

	private Action _localTransformAction;
	private Action _positionToolAction;
	private Action _scaleToolAction;
	private Action _angleToolAction;

	public TransformSection(ScenePropertyPage page) {
		super("Transform", page);
		setStartCollapsed(true);
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

		manager.add(_localTransformAction);

		manager.add(new Separator());

		manager.add(new AlignAction(AlignValue.LEFT));
		manager.add(new AlignAction(AlignValue.HORIZONTAL_CENTER));
		manager.add(new AlignAction(AlignValue.RIGHT));

		manager.add(new AlignAction(AlignValue.TOP));
		manager.add(new AlignAction(AlignValue.VERTICAL_CENTER));
		manager.add(new AlignAction(AlignValue.BOTTOM));
	}

	public enum AlignValue {
		LEFT, RIGHT, TOP, BOTTOM, HORIZONTAL_CENTER, VERTICAL_CENTER
	}

	class AlignAction extends Action {
		private AlignValue _value;

		public AlignAction(AlignValue value) {
			super("Align to '" + value.name().toLowerCase().replace("_", " ") + "'.");
			_value = value;

			String icon = null;

			switch (_value) {
			case LEFT:
				icon = IMG_ALIGN_LEFT;
				break;
			case RIGHT:
				icon = IMG_ALIGN_RIGHT;
				break;
			case TOP:
				icon = IMG_ALIGN_TOP;
				break;
			case BOTTOM:
				icon = IMG_ALIGN_BOTTOM;
				break;
			case HORIZONTAL_CENTER:
				icon = IMG_ALIGN_CENTER;
				break;
			case VERTICAL_CENTER:
				icon = IMG_ALIGN_MIDDLE;
				break;
			default:
				break;
			}

			setImageDescriptor(EditorSharedImages.getImageDescriptor(icon));

		}

		@Override
		public void run() {
			var data = new JSONObject();
			data.put("align", _value);

			getEditor().getBroker().sendAll(new RunPositionActionMessage("Align", data, getEditor()));
		}

	}

	@SuppressWarnings({ "unused", "boxing" })
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

			{
				label(comp, "X", "Phaser.GameObjects.Sprite.x");

				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

				new SceneTextToFloat(text) {

					@Override
					protected void accept2(float value) {
						getModels().forEach(model -> TransformComponent.set_x(model, value));
						getEditor().setDirty(true);
					}
				};

				addUpdate(() -> {
					text.setText(
							flatValues_to_String(getModels().stream().map(model -> TransformComponent.get_x(model))));
				});
			}

			{
				label(comp, "Y", "Phaser.GameObjects.Sprite.y");

				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

				new SceneTextToFloat(text) {

					@Override
					protected void accept2(float value) {
						getModels().forEach(model -> TransformComponent.set_y(model, value));
						getEditor().setDirty(true);

					}
				};

				addUpdate(() -> {
					text.setText(
							flatValues_to_String(getModels().stream().map(model -> TransformComponent.get_y(model))));
				});
			}

		}

		{
			// scale

			var manager = new ToolBarManager();
			manager.add(_scaleToolAction);
			manager.createControl(comp);

			label(comp, "Scale", "Phaser.GameObjects.Sprite.setScale");

			{
				label(comp, "X", "Phaser.GameObjects.Sprite.scaleX");

				Text text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				new SceneTextToFloat(text) {

					@Override
					protected void accept2(float value) {
						getModels().forEach(model -> TransformComponent.set_scaleX(model, value));
						getEditor().setDirty(true);
					}

				};

				addUpdate(() -> {
					text.setText(flatValues_to_String(
							getModels().stream().map(model -> TransformComponent.get_scaleX(model))));
				});
			}

			{
				label(comp, "Y", "Phaser.GameObjects.Sprite.scaleY");

				Text _scaleYText = new Text(comp, SWT.BORDER);
				_scaleYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				new SceneTextToFloat(_scaleYText) {

					@Override
					protected void accept2(float value) {
						getModels().forEach(model -> TransformComponent.set_scaleY(model, value));
						getEditor().setDirty(true);
					}

				};

				addUpdate(() -> {
					_scaleYText.setText(flatValues_to_String(
							getModels().stream().map(model -> TransformComponent.get_scaleY(model))));
				});
			}
		}

		{
			// angle

			var manager = new ToolBarManager();
			manager.add(_angleToolAction);
			manager.createControl(comp);

			{
				label(comp, "Angle", "Phaser.GameObjects.Sprite.angle");

				new Label(comp, SWT.NONE);

				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				new SceneTextToFloat(text) {

					@Override
					protected void accept2(float value) {
						getModels().forEach(model -> TransformComponent.set_angle(model, value));
						getEditor().setDirty(true);
					}
				};

				addUpdate(() -> {
					text.setText(flatValues_to_String(
							getModels().stream().map(model -> TransformComponent.get_angle(model))));
				});

				new Label(comp, SWT.NONE);
				new Label(comp, SWT.NONE);
			}

		}

		return comp;
	}

	private void createActions() {
		// we use the setChecked() to convert the action to a toogle button.
		_positionToolAction = new CommandSectionAction(this, SceneUIEditor.COMMAND_ID_POSITION_TOOL);
		_positionToolAction.setChecked(false);

		_scaleToolAction = new CommandSectionAction(this, SceneUIEditor.COMMAND_ID_SCALE_TOOL);
		_scaleToolAction.setChecked(false);

		_angleToolAction = new CommandSectionAction(this, SceneUIEditor.COMMAND_ID_ANGLE_TOOL);
		_angleToolAction.setChecked(false);

		_localTransformAction = new Action("Transform in local/global coords.") {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_HOUSE));
			}

			@Override
			public void run() {

				var scene = getScene();

				scene.setTransformLocalCoords(!scene.isTransformLocalCoords());

				updateActions();

				scene.redraw();
			}
		};

		addUpdate(this::updateActions);

	}

	void updateActions() {
		_positionToolAction.setChecked(getEditor().getScene().hasInteractiveTool(PositionTool.class));
		_scaleToolAction.setChecked(getEditor().getScene().hasInteractiveTool(ScaleTool.class));
		_angleToolAction.setChecked(getEditor().getScene().hasInteractiveTool(AngleTool.class));

		var local = getScene().isTransformLocalCoords();

		_localTransformAction.setImageDescriptor(EditorSharedImages.getImageDescriptor(local ? IMG_HOUSE : IMG_WORLD));
		_localTransformAction.setText(local ? "Transform in local coords." : "Transform in global coords.");
	}

}
