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
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.GameObjectComponent;
import phasereditor.scene.core.VisibleComponent;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class GameObjectSection extends ScenePropertySection {

	public GameObjectSection(ScenePropertyPage page) {
		super("Game Object", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof VisibleComponent && obj instanceof GameObjectComponent;
	}

	@SuppressWarnings("boxing")
	@Override
	public void fillToolbar(ToolBarManager manager) {
		super.fillToolbar(manager);

		{
			var action = new Action("", IAction.AS_CHECK_BOX) {

				{
					setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_LIGHTNING));
					setToolTipText(getHelp("Phaser.GameObjects.GameObject.active"));
				}

				@Override
				public void run() {
					wrapOperation(() -> {
						getModels().forEach(model -> GameObjectComponent.set_active(model, isChecked()));
					});

					getEditor().setDirty(true);

					update_UI_from_Model();
				}
			};

			manager.add(action);

			addUpdate(() -> {
				action.setChecked(flatValues_to_Boolean(
						getModels().stream().map(model -> GameObjectComponent.get_active(model))));
			});
		}

		{
			var action = new Action("", IAction.AS_CHECK_BOX) {
				{
					setToolTipText(getHelp("Phaser.GameObjects.Components.Visible.visible"));
				}

				@Override
				public void run() {
					wrapOperation(() -> {
						getModels().forEach(model -> VisibleComponent.set_visible(model, isChecked()));
					});

					getEditor().setDirty(true);

					update_UI_from_Model();
				}
			};

			manager.add(action);

			addUpdate(() -> {
				action.setChecked(
						flatValues_to_Boolean(getModels().stream().map(model -> VisibleComponent.get_visible(model))));

				action.setImageDescriptor(

						EditorSharedImages.getImageDescriptor(action.isChecked() ?

								IMG_EYE_OPEN

								: IMG_EYE_CLOSE));
			});
		}

	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(1, false));

		label(comp, "Data", "");

		new DataRowComp(comp, 0);
		new DataRowComp(comp, 0);
		new DataRowComp(comp, 0);

		return comp;
	}

	private class DataRowComp extends Composite {

		public DataRowComp(Composite parent, int style) {
			super(parent, style);

			setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			setLayout(new GridLayout(3, false));
			
			{
				var text = new Text(this, SWT.BORDER);
				text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			}

			{
				var text = new Text(this, SWT.BORDER);
				text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			}

			{
				var toolbar = new ToolBarManager();
				toolbar.add(new Action("Delete", EditorSharedImages.getImageDescriptor(IMG_DELETE)) {
					@Override
					public void run() {
						// nothing for now
					}
				});
				toolbar.createControl(this);
			}

		}

	}
}
