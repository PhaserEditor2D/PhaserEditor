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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.SceneObjectRenderer;
import phasereditor.scene.ui.editor.interactive.OriginTool;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class OriginSection extends ScenePropertySection {

	private Text _originXText;
	private Text _originYText;
	private List<OriginAction> _originPresetActions;
	private Action _originToolAction;

	public OriginSection(ScenePropertyPage page) {
		super("Origin", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof OriginComponent;
	}

	class OriginAction extends Action {
		private int _id;
		private boolean _x;
		private float _value;

		public OriginAction(String axis, int id) {
			super("Set origin in axis " + axis.toUpperCase(), AS_CHECK_BOX);
			_x = axis.equals("x");
			_id = id;
			_value = new float[] { 0, 0.5f, 1 }[id];

			setImageDescriptor(EditorSharedImages.getImageDescriptor("icons/origin-" + axis + "-" + _id + ".png"));
		}

		@SuppressWarnings({ "boxing", "synthetic-access" })
		public void update_UI_from_Model() {
			var flat = flatValues_to_Object(getModels().stream().map(model -> {
				if (_x) {
					return OriginComponent.get_originX(model);
				}

				return OriginComponent.get_originY(model);
			}));

			setChecked(flat != null && ((Float) flat).floatValue() == _value);

		}

		@SuppressWarnings("boxing")
		@Override
		public void run() {

			wrapOperation(() -> {

				getModels().forEach(model -> {

					SceneObjectRenderer renderer = getEditor().getScene().getSceneRenderer();

					var size = renderer.getObjectSize(model);

					var originX = OriginComponent.get_originX(model);
					var originY = OriginComponent.get_originY(model);

					var newOriginX = _x ? _value : originX;
					var newOriginY = _x ? originY : _value;

					var local1 = new float[] { originX * size[0], originY * size[1] };
					var local2 = new float[] { newOriginX * size[0], newOriginY * size[1] };

					var parent1 = renderer.localToParent(model, local1);
					var parent2 = renderer.localToParent(model, local2);

					var dx = parent2[0] - parent1[0];
					var dy = parent2[1] - parent1[1];

					OriginComponent.set_originX(model, newOriginX);
					OriginComponent.set_originY(model, newOriginY);

					TransformComponent.set_x(model, TransformComponent.get_x(model) + dx);
					TransformComponent.set_y(model, TransformComponent.get_y(model) + dy);
				});

				OriginSection.this.update_UI_from_Model();

			}, true, model -> model instanceof DynamicBitmapTextComponent);

			getEditor().setDirty(true);

		}
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {

		manager.add(_originToolAction);

		manager.add(new Separator());

		for (var action : _originPresetActions) {
			manager.add(action);
		}
	}

	private void createActions() {
		_originPresetActions = new ArrayList<>();

		for (int i = 0; i <= 2; i++) {
			var action = new OriginAction("x", i);
			_originPresetActions.add(action);
		}

		for (int i = 0; i <= 2; i++) {
			var action = new OriginAction("y", i);
			_originPresetActions.add(action);
		}

		_originToolAction = new Action("Origin tool.", IAction.AS_CHECK_BOX) {

			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_EDIT_ORIGIN));
			}

			@Override
			public void run() {

				if (isChecked()) {
					setInteractiveTools(

							new OriginTool(getEditor(), true, false),

							new OriginTool(getEditor(), false, true),

							new OriginTool(getEditor(), true, true)

					);
				} else {
					setInteractiveTools();
				}

			}
		};
	}

	@SuppressWarnings({ "boxing", "unused", "synthetic-access" })
	@Override
	public Control createContent(Composite parent) {

		createActions();

		Composite comp = new Composite(parent, SWT.NONE);

		comp.setLayout(new GridLayout(6, false));

		var manager = new ToolBarManager();

		manager.add(_originToolAction);
		manager.createControl(comp);

		label(comp, "Origin", "Phaser.GameObjects.Sprite.setOrigin");

		label(comp, "X", "Phaser.GameObjects.Sprite.originX");

		_originXText = new Text(comp, SWT.BORDER);
		_originXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new SceneTextToFloat(_originXText) {
			{
				filterDirtyModels = model -> model instanceof DynamicBitmapTextComponent;
			}

			@Override
			protected void accept2(float value) {
				getModels().forEach(model -> {
					OriginComponent.set_originX(model, value);
				});

				updateActions_UI_from_Model();
				getEditor().setDirty(true);

			}

		};

		label(comp, "Y", "Phaser.GameObjects.Sprite.originY");

		_originYText = new Text(comp, SWT.BORDER);
		_originYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new SceneTextToFloat(_originYText) {

			{
				filterDirtyModels = model -> model instanceof DynamicBitmapTextComponent;
			}

			@Override
			protected void accept2(float value) {
				getModels().forEach(model -> {
					OriginComponent.set_originY(model, value);
				});

				updateActions_UI_from_Model();
				getEditor().setDirty(true);

			}
		};

		return comp;
	}

	@Override
	@SuppressWarnings("boxing")
	public void update_UI_from_Model() {

		var models = getModels();

		// origin

		_originXText.setText(flatValues_to_String(models.stream().map(model -> OriginComponent.get_originX(model))));
		_originYText.setText(flatValues_to_String(models.stream().map(model -> OriginComponent.get_originY(model))));

		updateActions_UI_from_Model();

	}

	private void updateActions_UI_from_Model() {
		for (var action : _originPresetActions) {
			action.update_UI_from_Model();
		}

		_originToolAction.setChecked(getEditor().getScene().hasInteractiveTool(OriginTool.class));
	}

}
