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
package phasereditor.canvas.ui.editors.palette;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

/**
 * @author arian
 *
 */
public class PaletteDropAdapter extends ViewerDropAdapter {
	private PaletteComp _palette;
	private int _location;
	private Object _target;

	protected PaletteDropAdapter(PaletteComp palette) {
		super(palette._viewer);
		_palette = palette;
	}

	@Override
	public void dragOver(DropTargetEvent event) {
		_location = determineLocation(event);
		_target = determineTarget(event);
		super.dragOver(event);
	}

	@Override
	public boolean performDrop(Object data) {
		List<Object> toDrop = new ArrayList<>();
		ArrayList<Object> list = _palette._list;
		for (Object obj : ((IStructuredSelection) data).toArray()) {
			if (obj != _target) {
				toDrop.add(obj);
			}
		}

		if (_location != LOCATION_NONE) {
			list.removeAll(toDrop);
		}

		int i = list.indexOf(_target);
		if (i < 0) {
			i = list.size();
		}

		switch (_location) {
		case LOCATION_NONE:
			_palette.drop(list.size(), toDrop.toArray());
			break;
		case LOCATION_BEFORE:
		case LOCATION_ON:
			_palette.drop(i, toDrop.toArray());
			break;
		case LOCATION_AFTER:
			_palette.drop(i + 1, toDrop.toArray());
			break;
		default:
			break;
		}

		return true;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		return true;
	}

}
