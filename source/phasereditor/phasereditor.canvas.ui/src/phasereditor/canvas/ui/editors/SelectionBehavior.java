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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.SpriteNode;

/**
 * @author arian
 *
 */
public class SelectionBehavior implements ISelectionProvider {
	private ShapeCanvas _canvas;
	private ListenerList _listenerList;
	private IStructuredSelection _selection;
	private List<IObjectNode> _selectedNodes;

	public SelectionBehavior(ShapeCanvas canvas) {
		super();
		_canvas = canvas;
		_selection = StructuredSelection.EMPTY;
		_selectedNodes = new ArrayList<>();
		_listenerList = new ListenerList(ListenerList.IDENTITY);

		_canvas.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
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

	private void handleMousePressed(MouseEvent event) {
		Node userPicked = event.getPickResult().getIntersectedNode();
		Node picked = findBestToPick(userPicked);

		// out.println(userPicked + " --> " + picked);

		if (picked == null) {
			setSelection(StructuredSelection.EMPTY);
			return;
		}

		if (isSelected(picked)) {
			if (event.isControlDown()) {
				removeNodeFromSelection(picked);
			}
			return;
		}

		if (_selection != null && !_selection.isEmpty() && event.isControlDown()) {
			HashSet<Object> selection = new HashSet<>(Arrays.asList(_selection.toArray()));
			selection.add(picked);
			setSelection(new StructuredSelection(selection.toArray()));
		} else {
			setSelection(new StructuredSelection(picked));
		}
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
	public Node findBestToPick(Node picked) {
		if (picked == null) {
			return null;
		}

		if (picked == _canvas.getWorldNode()) {
			return null;
		}

		GroupNode selected = findSelectedParent(picked);

		if (selected != null) {
			return selected;
		}

		return findBestToPick2(picked);
	}

	private static Node findBestToPick2(Node picked) {
		if (picked == null) {
			return null;
		}

		if (picked instanceof SpriteNode) {
			return picked;
		}

		return findBestToPick2(picked.getParent());
	}

	private GroupNode findSelectedParent(Node picked) {
		if (picked == null) {
			return null;
		}

		if (picked == _canvas.getWorldNode()) {
			return null;
		}

		if (picked instanceof GroupNode) {
			if (isSelected(picked)) {
				return (GroupNode) picked;
			}
		}

		return findSelectedParent(picked.getParent());
	}

	public boolean isSelected(Object node) {
		if (_selection == null) {
			return false;
		}

		for (Object e : _selection.toArray()) {
			if (e == node) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_listenerList.add(listener);
	}

	@Override
	public IStructuredSelection getSelection() {
		return _selection;
	}

	public List<IObjectNode> getSelectedNodes() {
		return _selectedNodes;
	}

	public boolean containsInSelection(Node node) {
		return _selection != null && _selection.toList().contains(node);
	}

	@Override
	public void setSelection(ISelection selection) {
		setSelection_private(selection);
		TreeViewer outline = _canvas.getOutline();
		outline.setSelection(selection, true);
	}

	private void setSelection_private(ISelection selection) {
		{
			_selection = (IStructuredSelection) selection;
			List<IObjectNode> list = new ArrayList<>();
			for (Object obj : _selection.toArray()) {
				list.add((IObjectNode) obj);
			}
			_selectedNodes = list;
		}

		{
			Object[] list = _listenerList.getListeners();
			for (Object l : list) {
				((ISelectionChangedListener) l).selectionChanged(new SelectionChangedEvent(this, selection));
			}
		}

		updateSelectedNodes();
	}

	public void updateSelectedNodes() {
		Pane selpane = _canvas.getSelectionPane();

		selpane.getChildren().clear();

		for (Object obj : _selection.toArray()) {
			if (obj instanceof IObjectNode) {
				IObjectNode inode = (IObjectNode) obj;
				Node node = inode.getNode();

				Bounds rect = buildSelectionBounds(node);

				if (rect == null) {
					continue;
				}

				selpane.getChildren().add(new SelectionNode(inode, rect));
			}
		}
	}

	private Bounds buildSelectionBounds(Node node) {
		List<Bounds> list = new ArrayList<>();

		buildSelectionBounds(node, list);

		if (list.isEmpty()) {
			return null;
		}

		Bounds first = list.get(0);

		double x0 = first.getMinX();
		double y0 = first.getMinY();
		double x1 = x0 + first.getWidth();
		double y1 = y0 + first.getHeight();

		for (Bounds r : list) {
			double r_x0 = r.getMinX();
			double r_x1 = r_x0 + r.getWidth();
			double r_y0 = r.getMinY();
			double r_y1 = r_y0 + r.getHeight();

			if (r_x0 < x0) {
				x0 = r_x0;
			}

			if (r_x1 > x1) {
				x1 = r_x1;
			}

			if (r_y0 < y0) {
				y0 = r_y0;
			}

			if (r_y1 > y1) {
				y1 = r_y1;
			}
		}

		return new BoundingBox(x0, y0, x1 - x0, y1 - y0);
	}

	private void buildSelectionBounds(Node node, List<Bounds> list) {
		GroupNode world = _canvas.getWorldNode();
		if (node instanceof GroupNode) {
			// add the children bounds
			for (Node child : ((GroupNode) node).getChildren()) {
				buildSelectionBounds(child, list);
			}
		} else {
			Bounds b = localToAncestor(node.getBoundsInLocal(), node, world);
			list.add(b);
		}

	}

	public static Point2D localToAncestor(Point2D point, Node local, Node ancestor) {
		if (local == ancestor) {
			return point;
		}

		Point2D p = local.localToParent(point);
		return localToAncestor(p, local.getParent(), ancestor);
	}

	public static Bounds localToAncestor(Bounds bounds, Node local, Node ancestor) {
		if (local == ancestor) {
			return bounds;
		}

		Bounds b = local.localToParent(bounds);
		return localToAncestor(b, local.getParent(), ancestor);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_listenerList.remove(listener);
	}

	/**
	 * If the given node is selected, remove it from the selection.
	 * 
	 * @param node
	 */
	public void removeNodeFromSelection(Node node) {
		if (isSelected(node)) {
			@SuppressWarnings("unchecked")
			List<Object> list = new ArrayList<>(_selection.toList());
			list.remove(node);
			setSelection(new StructuredSelection(list));
		}
	}

}
