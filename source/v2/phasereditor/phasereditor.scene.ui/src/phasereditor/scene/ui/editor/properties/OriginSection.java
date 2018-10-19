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
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.OriginComponent;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class OriginSection extends ScenePropertySection {

	private Text _originXText;
	private Text _originYText;
	private List<OriginAction> _actions;

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
			super("Set Origin.", AS_CHECK_BOX);
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
					if (_x) {
						OriginComponent.set_originX(model, _value);
					} else {
						OriginComponent.set_originY(model, _value);
					}
				});

				OriginSection.this.update_UI_from_Model();

			}, getModels(), true, model -> model instanceof DynamicBitmapTextComponent);

		}
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {

		_actions = new ArrayList<>();

		for (int i = 0; i <= 2; i++) {
			var action = new OriginAction("x", i);
			manager.add(action);
			_actions.add(action);
		}

		for (int i = 0; i <= 2; i++) {
			var action = new OriginAction("y", i);
			manager.add(action);
			_actions.add(action);
		}
	}

	@Override
	public Control createContent(Composite parent) {

		Composite comp = new Composite(parent, SWT.NONE);

		comp.setLayout(new GridLayout(6, false));

		var manager = new ToolBarManager();
		manager.add(new Action("Origin", EditorSharedImages.getImageDescriptor(IMG_EDIT_OBJ_PROPERTY)) {
			//
		});
		manager.createControl(comp);

		label(comp, "Origin", "Phaser.GameObjects.Sprite.setOrigin");

		label(comp, "X", "Phaser.GameObjects.Sprite.originX");

		_originXText = new Text(comp, SWT.BORDER);
		_originXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		label(comp, "Y", "Phaser.GameObjects.Sprite.originY");

		_originYText = new Text(comp, SWT.BORDER);
		_originYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

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

		listenFloat(_originXText, value -> {
			models.forEach(model -> {
				OriginComponent.set_originX(model, value);
			});

			updateActions_UI_from_Model();
			getEditor().setDirty(true);
		}, models, true, model -> model instanceof DynamicBitmapTextComponent);

		listenFloat(_originYText, value -> {
			models.forEach(model -> {
				OriginComponent.set_originY(model, value);
			});

			updateActions_UI_from_Model();
			getEditor().setDirty(true);
		}, models, true, model -> model instanceof DynamicBitmapTextComponent);

	}

	private void updateActions_UI_from_Model() {
		for (var action : _actions) {
			action.update_UI_from_Model();
		}
	}

}
