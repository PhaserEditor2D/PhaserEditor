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

import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.scene.ui.editor.interactive.AngleTool;
import phasereditor.scene.ui.editor.interactive.PositionTool;
import phasereditor.scene.ui.editor.interactive.ScaleTool;
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
			super();
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

			var scene = getScene();
			var rend = scene.getSceneRenderer();

			var minX = Float.MAX_VALUE;
			var maxX = Float.MIN_VALUE;
			var minY = Float.MAX_VALUE;
			var maxY = Float.MIN_VALUE;

			var models = getModels();

			if (models.size() == 1) {

				var sm = getEditor().getSceneModel();

				var p1 = scene.modelToView(sm.getBorderX(), sm.getBorderY());
				var p2 = scene.modelToView(sm.getBorderX() + sm.getBorderWidth(),
						sm.getBorderY() + sm.getBorderHeight());

				minX = p1[0];
				maxX = p2[0];
				minY = p1[1];
				maxY = p2[1];

			} else {
				for (var model : models) {
					var parent = ParentComponent.get_parent(model);

					var point = rend.localToScene(parent, TransformComponent.get_x(model),
							TransformComponent.get_y(model));

					minX = Math.min(minX, point[0]);
					maxX = Math.max(maxX, point[0]);
					minY = Math.min(minY, point[1]);
					maxY = Math.max(maxY, point[1]);
				}
			}

			var fminX = minX;
			var fmaxX = maxX;
			var fminY = minY;
			var fmaxY = maxY;

			wrapOperation(() -> {

				for (var model : models) {

					var parent = ParentComponent.get_parent(model);

					switch (_value) {
					case LEFT:
						TransformComponent.set_x(model, rend.sceneToLocal(parent, fminX, 0)[0]);
						break;
					case RIGHT:
						TransformComponent.set_x(model, rend.sceneToLocal(parent, fmaxX, 0)[0]);
						break;
					case HORIZONTAL_CENTER:
						TransformComponent.set_x(model, rend.sceneToLocal(parent, (fmaxX + fminX) / 2, 0)[0]);
						break;
					case TOP:
						TransformComponent.set_y(model, rend.sceneToLocal(parent, 0, fminY)[1]);
						break;
					case BOTTOM:
						TransformComponent.set_y(model, rend.sceneToLocal(parent, 0, fmaxY)[1]);
						break;
					case VERTICAL_CENTER:
						TransformComponent.set_y(model, rend.sceneToLocal(parent, 0, (fmaxY + fminY) / 2)[1]);
						break;
					default:
						break;
					}
				}

			});

			var editor = getEditor();

			editor.setDirty(true);

			scene.redraw();

			editor.updatePropertyPagesContentWithSelection();

		}

	}

	@SuppressWarnings({ "unused" })
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

			new SceneTextToFloat(_xText) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TransformComponent.set_x(model, value));
					getEditor().setDirty(true);
				}
			};

			label(comp, "Y", "Phaser.GameObjects.Sprite.y");

			_yText = new Text(comp, SWT.BORDER);
			_yText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			new SceneTextToFloat(_yText) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TransformComponent.set_y(model, value));
					getEditor().setDirty(true);

				}
			};

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
			new SceneTextToFloat(_scaleXText) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TransformComponent.set_scaleX(model, value));
					getEditor().setDirty(true);
				}

			};

			label(comp, "Y", "Phaser.GameObjects.Sprite.scaleY");

			_scaleYText = new Text(comp, SWT.BORDER);
			_scaleYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(_scaleYText) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TransformComponent.set_scaleY(model, value));
					getEditor().setDirty(true);
				}

			};
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
			new SceneTextToFloat(_angleText) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TransformComponent.set_angle(model, value));
					getEditor().setDirty(true);
				}
			};

			new Label(comp, SWT.NONE);
			new Label(comp, SWT.NONE);

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

	}

	@Override
	@SuppressWarnings("boxing")
	public void user_update_UI_from_Model() {

		var models = getModels();

		// x y

		_xText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_x(model))));
		_yText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_y(model))));

		// scale

		_scaleXText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_scaleX(model))));
		_scaleYText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_scaleY(model))));

		// angle

		_angleText.setText(flatValues_to_String(models.stream().map(model -> TransformComponent.get_angle(model))));

		updateActions();
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
