// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors;

import static java.lang.System.out;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import phasereditor.canvas.ui.shapes.BaseObjectNode;
import phasereditor.canvas.ui.shapes.GroupNode;

/**
 * @author arian
 *
 */
public class SelectionBehavior implements ISelectionProvider {
	private ShapeCanvas _canvas;
	private ListenerList _listenerList;
	private IStructuredSelection _selection;

	public SelectionBehavior(ShapeCanvas canvas) {
		super();
		_canvas = canvas;
		_selection = StructuredSelection.EMPTY;
		_listenerList = new ListenerList(ListenerList.IDENTITY);

		_canvas.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
			Node userPicked = event.getPickResult().getIntersectedNode();

			BaseObjectNode picked = findBestToPick(userPicked);

			// to know the bounds in the scene.
			// if (picked != null) {
			// out.println(picked.localToScreen(picked.getBoundsInLocal()));
			// }

			if (picked == null) {
				setSelection(StructuredSelection.EMPTY);
				return;
			}

			if (_selection != null && !_selection.isEmpty() && event.isControlDown()) {
				HashSet<Object> selection = new HashSet<>(Arrays.asList(_selection.toArray()));
				selection.add(picked);
				setSelection(new StructuredSelection(selection.toArray()));
			} else {
				setSelection(new StructuredSelection(picked));
			}
		});
		_canvas.getOutline().addSelectionChangedListener(new ISelectionChangedListener() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty()) {
					setSelection_private(event.getSelection());
				}
			}
		});
	}

	/**
	 * Find the real object to pick when an object is picked by the user. The
	 * rule is to pick a "BaseObjectNode" but also if there is a parent that is
	 * already selected, the return that parent.
	 * 
	 * @param picked
	 *            The real object to pick, or the group this object belongs if
	 *            that group is selected.
	 * @return
	 */
	public static BaseObjectNode findBestToPick(Node picked) {
		if (picked == null) {
			return null;
		}

		GroupNode selected = findSelectedParent(picked);

		if (selected != null) {
			return selected;
		}

		return findBestToPick2(picked);
	}

	private static BaseObjectNode findBestToPick2(Node picked) {
		if (picked == null) {
			return null;
		}

		if (picked instanceof BaseObjectNode) {
			return (BaseObjectNode) picked;
		}

		return findBestToPick2(picked.getParent());
	}

	private static GroupNode findSelectedParent(Node picked) {
		if (picked == null) {
			return null;
		}

		if (picked instanceof GroupNode) {
			if (((GroupNode) picked).isSelected()) {
				return (GroupNode) picked;
			}
		}

		return findSelectedParent(picked.getParent());
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_listenerList.add(listener);
	}

	@Override
	public IStructuredSelection getSelection() {
		return _selection;
	}

	public boolean containsInSelection(Node node) {
		return _selection != null && _selection.toList().contains(node);
	}

	@Override
	public void setSelection(ISelection selection) {
		setSelection_private(selection);
		if (!selection.isEmpty()) {
			TreeViewer outline = _canvas.getOutline();
			outline.setSelection(selection, true);
		}
	}

	private void setSelection_private(ISelection selection) {
		updateSelectedNodes(false);

		_selection = (IStructuredSelection) selection;

		Object[] list = _listenerList.getListeners();
		for (Object l : list) {
			((ISelectionChangedListener) l).selectionChanged(new SelectionChangedEvent(this, selection));
		}

		updateSelectedNodes(true);
	}

	private void updateSelectedNodes(boolean sel) {
		Pane selpane = _canvas.getSelectionPane();
		if (sel) {
			for (Object obj : _selection.toArray()) {
				if (obj instanceof BaseObjectNode) {
					BaseObjectNode node = (BaseObjectNode) obj;
					node.setSelected(true);

					Bounds nodeLocal = node.getBoundsInLocal();
					Bounds nodeScene = node.localToScene(nodeLocal);

					out.println("nodeLocal " + nodeLocal);
					out.println("nodeScene " + nodeScene);

					Pane selnode = new Pane();
					selnode.setLayoutX(nodeScene.getMinX());
					selnode.setLayoutY(nodeScene.getMinY());
					selnode.setMinWidth(nodeScene.getWidth());
					selnode.setMinHeight(nodeScene.getHeight());
					selnode.setStyle("-fx-background-color:rgba(200, 0, 0, 150)-fx-border-color: red;-fx-border-width:2px;-fx-border-style:dashed;");
					selpane.getChildren().add(selnode);
				}
			}
		} else {
			for (Object obj : _selection.toArray()) {
				if (obj instanceof BaseObjectNode) {
					BaseObjectNode node = (BaseObjectNode) obj;
					node.setSelected(false);
				}
			}
			selpane.getChildren().clear();
		}

		for (Object obj : _selection.toArray()) {
			if (obj instanceof BaseObjectNode) {
				BaseObjectNode node = (BaseObjectNode) obj;
				node.setSelected(sel);
			}
		}
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_listenerList.remove(listener);
	}

}
