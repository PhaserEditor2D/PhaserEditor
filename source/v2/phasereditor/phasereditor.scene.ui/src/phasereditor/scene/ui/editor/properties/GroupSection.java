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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.scene.core.GroupComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.ui.editor.outline.SceneObjectsViewer;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.TreeArrayContentProvider;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class GroupSection extends ScenePropertySection {

	private GroupChildrenViewer _childrenViewer;
	private Button _selectInSceneButton;

	public GroupSection(FormPropertyPage page) {
		super("Group", page);
		setFillSpace(true);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof GroupComponent;
	}

	@Override
	public Control createContent(Composite parent) {

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));

		{
			label(comp, "Children:", "Phaser.GameObjects.Group.children");

			var filteredTree = new FilteredTreeCanvas(comp, 0);
			var gd = new GridData(GridData.FILL_BOTH);
			filteredTree.setLayoutData(gd);

			_childrenViewer = new GroupChildrenViewer(filteredTree.getTree());
		}

		{
			_selectInSceneButton = new Button(comp, 0);
			_selectInSceneButton.setText("Select In Scene");
			_selectInSceneButton.setImage(EditorSharedImages.getImage(IMG_BULLET_GO));
			_selectInSceneButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_selectInSceneButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				selectInScene();
			}));

			_childrenViewer.addSelectionChangedListener(e -> {
				updateSelectButton();
			});
		}

		return comp;
	}

	@SuppressWarnings("unchecked")
	private void selectInScene() {
		getEditor().setSelection(_childrenViewer.getStructuredSelection().toList());
		getEditor().updatePropertyPagesContentWithSelection();
	}

	private void updateSelectButton() {
		_selectInSceneButton.setEnabled(!_childrenViewer.getStructuredSelection().isEmpty());
	}

	class GroupChildrenViewer extends SceneObjectsViewer {

		public GroupChildrenViewer(TreeCanvas canvas) {
			super(canvas, getEditor(), new TreeArrayContentProvider());
		}

		public void updateWithModels() {
			var list = new ArrayList<ObjectModel>();

			for (var group : getModels()) {
				list.addAll(ParentComponent.get_children(group));
			}

			setInput(list);
		}

	}

	@Override
	public void update_UI_from_Model() {
		_childrenViewer.updateWithModels();
		updateSelectButton();
	}

}
