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
package phasereditor.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

/**
 * @author arian
 *
 */
public abstract class FrameCanvasUtils extends SelectionProviderImpl
		implements MouseMoveListener, MouseWheelListener, MouseListener, KeyListener, DragSourceListener {

	private List<Object> _selectedObjects;
	private Object _lastSelectedObject;
	private Object _overObject;
	private Canvas _canvas;
	private Object _dropObject;

	/**
	 * The same as {@link DropTargetEvent#feedback}.
	 */
	private int _dropLocation;
	private int _dropIndex;
	private boolean _filterInputWhenSetSelection;

	public FrameCanvasUtils(Canvas canvas, boolean initDND) {
		super(true);

		_canvas = canvas;

		_overObject = null;
		_dropObject = null;
		_dropIndex = -1;

		_filterInputWhenSetSelection = true;

		_selectedObjects = new ArrayList<>();

		canvas.addMouseMoveListener(this);
		canvas.addMouseWheelListener(this);
		canvas.addMouseListener(this);

		canvas.addKeyListener(this);

		if (initDND) {
			init_DND();
		}
	}

	public abstract int getFramesCount();

	public abstract Rectangle getSelectionFrameArea(int index);

	public abstract Point viewToModel(int x, int y);

	public abstract Point modelToView(int x, int y);

	public abstract Object getFrameObject(int index);

	@SuppressWarnings("all")
	public ImageProxy get_DND_Image(int index) {
		return null;
	}
	
	@SuppressWarnings({ "static-method", "unused" })
	public Image get_DND_Disposable_Image(int index) {
		return null;
	}

	@SuppressWarnings({ "static-method", "unused" })
	public boolean isInformationControlValidPosition(int index, int x, int y) {
		return true;
	}

	public int getOverIndex() {
		return indexOf(_overObject);
	}

	public Object getOverObject() {
		return _overObject;
	}

	public boolean isSelected(Object obj) {
		return _selectedObjects.contains(obj);
	}

	public boolean isSelectedIndex(int i) {
		var obj = getFrameObject(i);
		return obj != null && _selectedObjects.contains(obj);
	}

	public boolean isOver(Object obj) {
		return _overObject != null && _overObject == obj;
	}

	private int indexOf(Object obj) {
		if (obj == null) {
			return -1;
		}

		for (int i = 0; i < getFramesCount(); i++) {
			var obj2 = getFrameObject(i);
			if (obj == obj2) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		updateOverIndex(e);
		if (_dropIndex != -1) {
			dropDone();
		}
	}

	public void updateOverIndex(MouseEvent e) {
		if (getFramesCount() == 0) {
			return;
		}

		var old = _overObject;
		Object newObj = null;
		for (int i = 0; i < getFramesCount(); i++) {
			Rectangle rect = getSelectionFrameArea(i);
			if (rect.contains(viewToModel(e.x, e.y))) {
				newObj = getFrameObject(i);
				break;
			}
		}
		if (old != newObj) {
			_overObject = newObj;
			_canvas.redraw();
		}
	}

	public void updateDropIndex(DropTargetEvent e) {

		_dropIndex = -1;

		if (getFramesCount() == 0) {
			return;
		}

		var old = _dropObject;
		Object newObj = null;

		var viewPoint = _canvas.toControl(new Point(e.x, e.y));
		var modelPoint = viewToModel(viewPoint.x, viewPoint.y);

		for (int i = 0; i < getFramesCount(); i++) {
			Rectangle rect = getSelectionFrameArea(i);
			if (rect.contains(modelPoint)) {
				newObj = getFrameObject(i);
				_dropIndex = i;

				_dropLocation = TreeCanvasDropAdapter.LOCATION_ON;

				if (modelPoint.y < rect.y + rect.height * 0.25) {
					_dropLocation = TreeCanvasDropAdapter.LOCATION_BEFORE;
				} else if (modelPoint.y >= rect.y + rect.height * 0.75) {
					_dropLocation = TreeCanvasDropAdapter.LOCATION_AFTER;
				}

				break;
			}
		}

		if (old != newObj) {
			_dropObject = newObj;
		}

		if (_dropObject == null) {
			_dropIndex = -1;
		}

	}

	public Object getDropObject() {
		return _dropObject;
	}

	public int getDropIndex() {
		return _dropIndex;
	}

	/**
	 * {@linkplain TreeCanvasDropAdapter#LOCATION_ON}
	 */
	public int getDropLocation() {
		return _dropLocation;
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		// nothing
	}

	public List<Object> getSelectedObjects() {
		return _selectedObjects;
	}

	private void updateSelectionProvider() {
		super.setSelection(new StructuredSelection(getSelectedObjects()));
	}

	public boolean isFilterInputWhenSetSelection() {
		return _filterInputWhenSetSelection;
	}

	public void setFilterInputWhenSetSelection(boolean filterInputWhenSetSelection) {
		_filterInputWhenSetSelection = filterInputWhenSetSelection;
	}

	public void setSelection(ISelection sel, boolean fireChanged) {
		var b = isAutoFireSelectionChanged();
		setAutoFireSelectionChanged(fireChanged);
		setSelection(sel);
		setAutoFireSelectionChanged(b);
	}

	@Override
	public void setSelection(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			var selArray = ((IStructuredSelection) sel).toArray();
			var list = new ArrayList<>();

			if (_filterInputWhenSetSelection) {
				for (var obj : selArray) {
					for (int i = 0; i < getFramesCount(); i++) {
						var frameObj = getFrameObject(i);
						if (obj == frameObj) {
							list.add(obj);
						}
					}
				}
			} else {
				list.addAll(List.of(selArray));
			}

			_selectedObjects = list;

			super.setSelection(new StructuredSelection(list));
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		updateSelectionWithMouseEvent(e);
	}

	@Override
	public void mouseDown(MouseEvent e) {
		//
	}

	private void updateSelectionWithMouseEvent(MouseEvent e) {
		if (e.button != 1) {
			return;
		}

		var updateLastSelectionFrame = true;

		if (_overObject == null) {
			// it clicked away, so remove all selected objects, including those not shown by
			// the filter.
			_selectedObjects = new ArrayList<>();
		} else {
			if ((e.stateMask & SWT.CTRL) != 0) {

				// control pressed

				if (_selectedObjects.contains(_overObject)) {
					_selectedObjects.remove(_overObject);
				} else {
					_selectedObjects.add(_overObject);
				}

			} else if ((e.stateMask & SWT.SHIFT) != 0 && !_selectedObjects.isEmpty()) {

				// select the whole range

				int a = _lastSelectedObject == null ? 0 : indexOf(_lastSelectedObject);
				int b = indexOf(_overObject);

				int from = Math.min(a, b);
				int to = Math.max(a, b);

				// clear all the selected objects, including those hidden by the filtering.
				_selectedObjects = new ArrayList<>();

				for (int i = from; i <= to; i++) {
					_selectedObjects.add(getFrameObject(i));
				}

				updateLastSelectionFrame = false;

			} else {

				// Just select that frame. In this case we are not interested on keep selected
				// filtered objects.
				_selectedObjects = new ArrayList<>(List.of(_overObject));

			}

		}

		if (updateLastSelectionFrame) {
			_lastSelectedObject = _overObject;
		}

		updateSelectionProvider();

		_canvas.redraw();
	}

	private void clearSelection() {
		// We do not clear all the selected objects because it is possible that some of
		// them are filtered off, so we need to keep them.
		// To clear the selection then we remove just those visible objects.
		for (int i = 0; i < getFramesCount(); i++) {
			var obj = getFrameObject(i);
			_selectedObjects.remove(obj);
		}
	}

	public void selectAll() {
		// clear all selected objects, including those are not visible
		_selectedObjects = new ArrayList<>();

		for (int i = 0; i < getFramesCount(); i++) {
			_selectedObjects.add(getFrameObject(i));
		}
		_canvas.redraw();
		updateSelectionProvider();
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		//
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.character) {
		case SWT.ESC:
			_lastSelectedObject = null;
			clearSelection();
			_canvas.redraw();
			updateSelectionProvider();
			break;
		default:
			break;
		}

		switch (e.keyCode) {
		case SWT.ARROW_UP:
		case SWT.ARROW_LEFT:
			shiftSelection(-1);
			break;
		case SWT.ARROW_DOWN:
		case SWT.ARROW_RIGHT:
			shiftSelection(1);
			break;
		case SWT.HOME:
			// TODO: scroll to the start
			break;
		case SWT.END:
			// TODO: scroll to the end
			break;
		default:
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		//
	}

	protected void shiftSelection(int dir) {

		if (getFramesCount() == 0) {
			return;
		}

		if (_lastSelectedObject == null) {
			_lastSelectedObject = getFrameObject(0);
		}

		int i = indexOf(_lastSelectedObject);
		int j = i + dir;
		if (j >= 0 && j < getFramesCount()) {

			clearSelection();

			Object obj = getFrameObject(j);
			_selectedObjects.add(obj);
			_lastSelectedObject = obj;

			_canvas.redraw();
		}

		updateSelectionProvider();

	}

	public void init_DND() {
		{
			DragSource dragSource = new DragSource(_canvas, DND.DROP_MOVE | DND.DROP_DEFAULT);
			dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer() });
			dragSource.addDragListener(this);
		}
	}

	@Override
	public void dragStart(DragSourceEvent event) {

		if (_overObject == null) {
			event.doit = false;
			return;
		}

		int index = indexOf(_overObject);

		ISelection sel = null;

		if (_selectedObjects.contains(_overObject)) {
			sel = new StructuredSelection(_selectedObjects);
		} else {
			sel = new StructuredSelection(_overObject);
			clearSelection();
			_canvas.redraw();
		}

		Image img = null;
		var proxy = get_DND_Image(index);
		
		if (proxy == null) {
			img = get_DND_Disposable_Image(index);
			PhaserEditorUI.set_DND_Image(event, img);
			img.dispose();
		} else {
			img = proxy.getImage();
			PhaserEditorUI.set_DND_Image(event, img);
		}

		LocalSelectionTransfer.getTransfer().setSelection(sel);
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		event.data = "" + _overObject;
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.image != null) {
			// always dispose the image because it is a scaled copy of the original!
			event.image.dispose();
		}
	}

	public void dropDone() {
		_dropObject = null;
		_dropIndex = -1;
		_canvas.redraw();
	}

	public IStructuredSelection getStructuredSelection() {
		return (IStructuredSelection) getSelection();
	}
}
