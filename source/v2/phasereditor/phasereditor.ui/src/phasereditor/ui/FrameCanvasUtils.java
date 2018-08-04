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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public abstract class FrameCanvasUtils
		implements MouseMoveListener, MouseWheelListener, MouseListener, KeyListener, DragSourceListener {

	private int _overIndex;
	private Canvas _canvas;

	public FrameCanvasUtils(Canvas canvas) {

		_canvas = canvas;

		_overIndex = -1;

		emptySelection();

		canvas.addMouseMoveListener(this);
		canvas.addMouseWheelListener(this);
		canvas.addMouseListener(this);
		canvas.addKeyListener(this);

		init_DND();
	}

	public abstract int getFramesCount();

	public abstract Rectangle getPaintFrame(int index);

	public abstract Point getRealPosition(int x, int y);

	public abstract Object getFrameObject(int index);

	public abstract IFile getImageFile(int index);

	public abstract Rectangle getImageFrame(int index);

	public int getOverIndex() {
		return _overIndex;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (getFramesCount() == 0) {
			return;
		}

		int old = _overIndex;
		int index = -1;
		for (int i = 0; i < getFramesCount(); i++) {
			Rectangle place = getPaintFrame(i);
			if (place.contains(getRealPosition(e.x, e.y))) {
				index = i;
				break;
			}
		}
		if (old != index) {
			_overIndex = index;
			_canvas.redraw();
		}
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		// nothing
	}

	private List<Integer> _selectedIndexes;
	private int _lastSelectedIndex;

	public List<Integer> getSelectedIndexes() {
		return _selectedIndexes;
	}

	public void setSelectedIndexes(List<Integer> indexes) {
		_selectedIndexes = new ArrayList<>(indexes);
	}

	public List<Object> getSelectedObjects() {
		var list = new ArrayList<>();
		for (int i : _selectedIndexes) {
			list.add(getFrameObject(i));
		}
		return list;
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (e.button != 1) {
			return;
		}

		var updateLastSelectionFrame = true;

		int index = _overIndex;

		if (index == -1) {
			emptySelection();
		} else {
			if ((e.stateMask & SWT.CTRL) != 0) {

				// control pressed

				if (_selectedIndexes.contains(index)) {
					_selectedIndexes.remove(index);
				} else {
					_selectedIndexes.add(index);
				}

			} else if ((e.stateMask & SWT.SHIFT) != 0 && !_selectedIndexes.isEmpty()) {

				// select the whole range

				int a = _lastSelectedIndex;
				int b = index;

				int from = Math.min(a, b);
				int to = Math.max(a, b);

				emptySelection();

				for (int i = from; i <= to; i++) {
					_selectedIndexes.add(i);
				}

				updateLastSelectionFrame = false;

			} else {

				// just select that frame

				emptySelection();
				_selectedIndexes.add(index);

			}

			// updateSelectionProvider();
		}

		if (updateLastSelectionFrame) {
			_lastSelectedIndex = index;
		}

		_canvas.redraw();
	}

	public void selectAll() {
		emptySelection();
		for (int i = 0; i < getFramesCount(); i++) {
			_selectedIndexes.add(i);
		}
		_canvas.redraw();
	}

	@Override
	public void mouseDown(MouseEvent e) {
		//
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		//
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.character) {
		case SWT.ESC:
			_lastSelectedIndex = -1;
			emptySelection();
			_canvas.redraw();
			break;
		default:
			break;
		}

		switch (e.keyCode) {
		case SWT.ARROW_LEFT:
			shiftSelection(-1);
			break;
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

	private void shiftSelection(int dir) {

		if (_lastSelectedIndex == -1) {
			return;
		}

		if (getFramesCount() == 0) {
			return;
		}

		int i = _lastSelectedIndex;
		int j = i + dir;
		if (j >= 0 && j < getFramesCount()) {
			emptySelection();
			_selectedIndexes.add(j);
			_lastSelectedIndex = j;

			_canvas.redraw();
		}

	}

	public void emptySelection() {
		_selectedIndexes = new ArrayList<>();
	}

	private void init_DND() {
		{
			DragSource dragSource = new DragSource(_canvas, DND.DROP_MOVE | DND.DROP_DEFAULT);
			dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer() });
			dragSource.addDragListener(this);
		}
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		int index = getOverIndex();

		if (index == -1) {
			event.doit = false;
			return;
		}

		var file = getImageFile(index);
		var src = getImageFrame(index);

		ISelection sel = null;

		if (_selectedIndexes.contains(index)) {
			sel = new StructuredSelection(getSelectedObjects());
		} else {
			sel = new StructuredSelection(getFrameObject(index));
			emptySelection();
			_canvas.redraw();
		}

		event.image = PhaserEditorUI.scaleImage_DND(file, src);

		LocalSelectionTransfer.getTransfer().setSelection(sel);
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		int index = getOverIndex();
		var object = getFrameObject(index);
		event.data = "" + object;
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.image != null) {
			event.image.dispose();
		}
	}

}
