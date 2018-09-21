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
package phasereditor.scene.ui.editor.outline;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvasDropAdapter;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class SceneOutlinePage extends Page implements IContentOutlinePage {

	private SceneEditor _editor;
	private FilteredTreeCanvas _filterTree;
	private TreeCanvasViewer _viewer;

	public SceneOutlinePage(SceneEditor editor) {
		_editor = editor;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		// TODO Auto-generated method stub
	}

	@Override
	public ISelection getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setSelection(ISelection selection) {
		// TODO Auto-generated method stub

	}

	public SceneEditor getEditor() {
		return _editor;
	}

	@Override
	public void createControl(Composite parent) {
		_filterTree = new FilteredTreeCanvas(parent, SWT.NONE);
		_viewer = new TreeCanvasViewer(_filterTree.getTree(), new SceneOutlineContentProvider(),
				new OutlineLabelProvider2(_editor)) {

			@Override
			protected void setItemIconProperties(TreeCanvasItem item) {
				super.setItemIconProperties(item);

				var data = item.getData();

				if (data instanceof TextureComponent) {
					var frame = TextureComponent.get_frame((ObjectModel) data);
					var renderer = AssetsTreeCanvasViewer.createImageRenderer(item, frame);
					if (renderer != null) {
						item.setRenderer(renderer);
					}
				}

			}

		};
		_viewer.setInput(_editor.getSceneModel());

		init_DND();
	}

	private void init_DND() {
		Transfer[] types = { LocalSelectionTransfer.getTransfer() };

		_viewer.addDropSupport(DND.DROP_MOVE, types, new TreeCanvasDropAdapter(_viewer) {

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				return true;
			}

			@Override
			public boolean performDrop(Object data) {
				Object[] array = ((IStructuredSelection) data).toArray();

				var models = Arrays.stream(array).filter(obj -> obj instanceof ObjectModel)
						.map(obj -> (ObjectModel) obj).collect(toList());

				return performSectionDrop(models);
			}

		});
	}

	boolean performSectionDrop(List<ObjectModel> models) {
		var utils = _viewer.getCanvas().getUtils();

		int location = utils.getDropLocation();
		var targetObj = utils.getDropObject();

		var sceneObjects = _editor.getSceneModel().getObjects();

		out.println("---");
		out.println("location: " + location);
		out.println("target: " + targetObj);

		if (location == TreeCanvasDropAdapter.LOCATION_ON) {

			if (targetObj instanceof ParentComponent) {
				var parent = (ObjectModel) targetObj;

				// remove target from the objects to drop
				models.remove(parent);

				for (var model : models) {

					// remove from the scene objects, if it is the case
					sceneObjects.remove(model);

					// remove from a parent object, if it is the case
					ParentComponent.removeFromParent(model);

					// add to the new parent
					ParentComponent.addChild(parent, model);
				}

			}
		} else {

			if (targetObj == null) {

				sceneObjects.removeAll(models);

				for (var model : models) {
					ParentComponent.removeFromParent(model);
				}

				if (location == TreeCanvasDropAdapter.LOCATION_BEFORE) {
					sceneObjects.addAll(0, models);
				} else {
					sceneObjects.addAll(models);
				}

			} else if (targetObj instanceof ObjectModel) {

				sceneObjects.removeAll(models);

				var targetModel = (ObjectModel) targetObj;

				List<ObjectModel> list;

				{
					var parentOfTarget = ParentComponent.get_parent(targetModel);

					if (parentOfTarget == null) {
						list = sceneObjects;
					} else {
						list = ParentComponent.get_children(parentOfTarget);
					}
				}

				for (var model : models) {
					ParentComponent.removeFromParent(model);
				}

				var index = list.indexOf(targetModel);

				if (location == TreeCanvasDropAdapter.LOCATION_AFTER) {
					index++;
				}

				if (index > list.size()) {
					index = list.size();
				}

				if (index < 0) {
					index = 0;
				}

				list.addAll(index, models);

				for (var model : models) {
					ParentComponent.set_parent(model, targetModel);
				}
			}

		}

		_viewer.refresh();

		_editor.getCanvas().redraw();

		return true;
	}

	@Override
	public Control getControl() {
		return _filterTree;
	}

	@Override
	public void setFocus() {
		_filterTree.getTree().setFocus();
	}

	@Override
	public void dispose() {

		_editor.setOutline(null);

		super.dispose();
	}

	public void refresh() {
		var elems = _viewer.getExpandedElements();

		_viewer.refresh();

		_viewer.setExpandedElements(elems);
	}

}
