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
import java.util.Set;

import org.eclipse.jface.action.Action;
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
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.scene.ui.editor.messages.SetObjectOriginKeepPositionMessage;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class OriginSection extends ScenePropertySection {

	private List<OriginAction> _originPresetActions;
	private Action _originToolAction;

	public OriginSection(ScenePropertyPage page) {
		super("Origin", page);

		setStartCollapsed(true);
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

		@Override
		public void run() {
			getEditor().getBroker()
					.sendAll(new SetObjectOriginKeepPositionMessage(getModels(), _value, _x ? "x" : "y"));
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

		_originToolAction = new CommandSectionAction(this, SceneUIEditor.COMMAND_ID_ORIGIN_TOOL);
		_originToolAction.setChecked(false);
	}

	@SuppressWarnings({ "boxing", "unused" })
	@Override
	public Control createContent(Composite parent) {

		createActions();

		Composite comp = new Composite(parent, SWT.NONE);

		comp.setLayout(new GridLayout(6, false));

		var manager = new ToolBarManager();

		manager.add(_originToolAction);
		manager.createControl(comp);

		label(comp, "Origin", "Phaser.GameObjects.Sprite.setOrigin");

		{
			label(comp, "X", "Phaser.GameObjects.Sprite.originX");

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			new SceneTextToFloat(text) {
				{
					filterDirtyModels = model -> model instanceof DynamicBitmapTextComponent;
				}

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> {
						OriginComponent.set_originX(model, value);
					});

					update_UI_from_Model();

					getEditor().setDirty(true);

				}

			};

			addUpdate(() -> {
				text.setText(
						flatValues_to_String(getModels().stream().map(model -> OriginComponent.get_originX(model))));
			});
		}

		{
			label(comp, "Y", "Phaser.GameObjects.Sprite.originY");

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(text) {

				{
					filterDirtyModels = model -> model instanceof DynamicBitmapTextComponent;
				}

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> {
						OriginComponent.set_originY(model, value);
					});

					update_UI_from_Model();

					getEditor().setDirty(true);

				}
			};
			addUpdate(() -> {
				text.setText(
						flatValues_to_String(getModels().stream().map(model -> OriginComponent.get_originY(model))));
			});
		}

		addUpdate(this::updateActions_UI_from_Model);

		return comp;
	}

	private void updateActions_UI_from_Model() {
		for (var action : _originPresetActions) {
			action.update_UI_from_Model();
		}

		_originToolAction.setChecked(getEditor().hasInteractiveTools(Set.of("Origin")));
	}

}
