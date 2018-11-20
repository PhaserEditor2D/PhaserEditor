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

import static java.util.stream.Collectors.toList;
import static phasereditor.ui.IEditorSharedImages.IMG_ADD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.GroupComponent;
import phasereditor.scene.core.GroupModel;
import phasereditor.scene.core.GroupsModel;
import phasereditor.scene.core.NameComputer;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.FrameData;
import phasereditor.ui.ImageTreeCanvasItemRenderer;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvas.TreeCanvasItemAction;
import phasereditor.ui.TreeCanvasDropAdapter;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class SceneOutlinePage extends Page implements IContentOutlinePage {

	private SceneEditor _editor;
	private FilteredTreeCanvas _filterTree;
	protected TreeCanvasViewer _viewer;

	public SceneOutlinePage(SceneEditor editor) {
		_editor = editor;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_viewer.addSelectionChangedListener(listener);
	}

	@Override
	public ISelection getSelection() {
		return _viewer.getSelection();
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_viewer.removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		_viewer.setSelection(selection, true);
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

				var sceneRenderer = getEditor().getScene().getSceneRenderer();

				if (data instanceof TextureComponent) {
					var frame = TextureComponent.get_frame((ObjectModel) data);
					var renderer = AssetsTreeCanvasViewer.createImageRenderer(item, frame);
					if (renderer != null) {
						item.setRenderer(renderer);
					}
				}

				if (data instanceof BitmapTextModel) {
					var model = (BitmapTextModel) data;

					var image = sceneRenderer.getBitmapTextImage(model);

					if (image != null) {
						item.setRenderer(new ImageTreeCanvasItemRenderer(item, image, FrameData.fromImage(image)));
					}
				}

				if (data instanceof TileSpriteModel) {
					var model = (TileSpriteModel) data;

					var image = sceneRenderer.getTileSpriteTextImage(model);

					if (image != null) {
						item.setRenderer(new ImageTreeCanvasItemRenderer(item, image, FrameData.fromImage(image)));
					}
				}

				if (data instanceof ObjectModel) {
					var model = (ObjectModel) data;
					var type = model.getType();
					item.setKeywords(type);
				}

				if (data instanceof GroupsModel) {
					item.setActions(List.of(new CreateNewGroupAction()));
				}
			}

		};
		_viewer.getCanvas().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				revealSelectedObjectInScene();
			}
		});
		_viewer.setInput(_editor.getSceneModel());

		var scene = _editor.getScene();

		_viewer.getCanvas().setEditActions(scene::copy, scene::cut, () -> {

			var sel = _viewer.getStructuredSelection().toArray();

			ObjectModel pasteParent = getEditor().getSceneModel().getDisplayList();

			if (sel.length == 1) {
				if (sel[0] instanceof ObjectModel) {
					pasteParent = (ObjectModel) sel[0];
				}
			}

			scene.paste(pasteParent, false);

		});

		init_DND();
	}

	class CreateNewGroupAction extends TreeCanvasItemAction {
		public CreateNewGroupAction() {
			super(EditorSharedImages.getImage(IMG_ADD), "Add new group");
		}

		@Override
		public void run(MouseEvent event) {
			var editor = getEditor();

			var sceneModel = editor.getSceneModel();

			var groups = sceneModel.getGroupsModel();

			var nameComputer = new NameComputer(groups);
			var initialName = nameComputer.newName("group");

			var dlg = new InputDialog(event.widget.getDisplay().getActiveShell(), "Create Group",
					"Enter the name of the new group:", initialName, new IInputValidator() {

						@Override
						public String isValid(String newText) {

							for (var group : ParentComponent.get_children(groups)) {
								if (GroupComponent.get_name(group).equals(newText)) {
									return "That name is used.";
								}
							}

							return null;
						}
					});

			if (dlg.open() == Window.OK) {
				var value = dlg.getValue();

				var group = new GroupModel(groups);

				GroupComponent.set_name(group, value);
				ParentComponent.get_children(groups).add(group);

				refresh();

				editor.setDirty(true);
			}
		}
	}

	protected void revealSelectedObjectInScene() {
		var obj = _viewer.getCanvas().getUtils().getOverObject();
		if (obj != null && obj instanceof ObjectModel) {
			_editor.getScene().reveal((ObjectModel) obj);
		}
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

				return performSelectionDrop(models);
			}

		});
	}

	boolean performSelectionDrop(List<ObjectModel> models) {
		var utils = _viewer.getCanvas().getUtils();

		int location = utils.getDropLocation();
		var targetObj = utils.getDropObject();

		if (location == TreeCanvasDropAdapter.LOCATION_ON) {

			if (targetObj instanceof ParentComponent) {
				var newParent = (ObjectModel) targetObj;

				var newDrops = new ArrayList<ObjectModel>();

				// avoid dropping on descendents

				for (var model : models) {
					if (ParentComponent.isDescendentOf(newParent, model)) {
						continue;
					}

					newDrops.add(model);
				}

				// avoid droping on parents

				{
					var list = new ArrayList<>(newDrops);
					for (var model : list) {
						if (ParentComponent.get_parent(model) == newParent) {
							newDrops.remove(model);
						}
					}
				}

				{
					// filter dropping kids

					var list = new ArrayList<>(newDrops);

					for (int i = 0; i < list.size() - 1; i++) {
						for (int j = i + 1; j < list.size(); j++) {
							var a = list.get(i);
							var b = list.get(j);
							if (ParentComponent.isDescendentOf(a, b)) {
								newDrops.remove(a);
							}

							if (ParentComponent.isDescendentOf(b, a)) {
								newDrops.remove(b);
							}
						}
					}
				}

				if (newDrops.isEmpty()) {
					return false;
				}

				var beforeSnapshot = WorldSnapshotOperation.takeSnapshot(_editor);

				var renderer = _editor.getScene().getSceneRenderer();

				for (var model : newDrops) {

					if (model instanceof TransformComponent) {

						var oldParent = ParentComponent.get_parent(model);

						float localX = TransformComponent.get_x(model);
						float localY = TransformComponent.get_y(model);

						var globalPoint = renderer.localToScene(oldParent, localX, localY);

						// move to the new parent

						ParentComponent.removeFromParent(model);
						ParentComponent.addChild(newParent, model);

						renderer.addPostPaintAction(() -> {

							var newLocalPoint = renderer.sceneToLocal(newParent, globalPoint[0], globalPoint[1]);

							TransformComponent.set_x(model, newLocalPoint[0]);
							TransformComponent.set_y(model, newLocalPoint[1]);

							getEditor().getScene().redraw();

						});
					} else {
						// move to the new parent
						ParentComponent.removeFromParent(model);
						ParentComponent.addChild(newParent, model);
					}
				}

				refresh();

				_editor.setSelection(newDrops);

				var afterSnapshot = WorldSnapshotOperation.takeSnapshot(_editor);

				_editor.executeOperation(new WorldSnapshotOperation(beforeSnapshot, afterSnapshot, "Drop into object"));

				_editor.setDirty(true);

			}
		}

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

		_editor.removeOutline();

		super.dispose();
	}

	public void refresh() {
		var elems = _viewer.getExpandedElements();

		_viewer.refresh();

		_viewer.setExpandedElements(elems);
	}

	public void refresh_basedOnId() {
		var elems = _viewer.getExpandedElements();

		var expandedIds = Arrays.stream(elems).map(e -> ((ObjectModel) e).getId()).collect(toList());

		_viewer.refresh();

		var expanded = _viewer.getCanvas().getItems().stream().map(item -> item.getData())
				.filter(data -> data instanceof ObjectModel && expandedIds.contains(((ObjectModel) data).getId()))
				.toArray();

		_viewer.setExpandedElements(expanded);
	}

	public void setSelection_from_external(StructuredSelection sel) {
		_viewer.getCanvas().reveal(sel.toArray());
		_viewer.getCanvas().getUtils().setSelection(sel, false);
	}

}
