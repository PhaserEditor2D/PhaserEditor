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
package phasereditor.ui.properties;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.properties.IPropertySheetPage;

/**
 * @author arian
 *
 */
public class PGridPage extends Page implements IPropertySheetPage {

	private PGrid _grid;
	private boolean _alwaysExandAll;
	public static final String PART = "part";
	public static final String PGRID = "pgrid";

	public PGridPage(boolean alwaysExpandAll) {
		_alwaysExandAll = alwaysExpandAll;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection.isEmpty()) {
			_grid.setModel(null);
		}

		if (selection instanceof IStructuredSelection) {
			Object elem = ((IStructuredSelection) selection).getFirstElement();
			PGridModel model = Adapters.adapt(elem, PGridModel.class);
			if (model != null) {
				model.getExtraData().put(PART, part);
				model.getExtraData().put(PGRID, _grid);
			}
			_grid.setModel(model);
		}
	}

	@Override
	public void createControl(Composite parent) {
		_grid = new PGrid(parent, SWT.NONE, false, _alwaysExandAll);
	}

	@Override
	public Control getControl() {
		return _grid;
	}

	@Override
	public void setFocus() {
		_grid.setFocus();
	}

	public PGrid getGrid() {
		return _grid;
	}
}
