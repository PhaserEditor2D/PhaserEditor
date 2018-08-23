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

import java.util.Arrays;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertySheet;

/**
 * @author arian
 *
 */
public abstract class QuickEditDialog extends Dialog {

	private PGrid _grid;
	private PGridModel _model;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public QuickEditDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());

		_grid = createPGrid(container);

		afterCreateWidgets();

		return container;
	}

	protected abstract PGrid createPGrid(Composite container);

	@Override
	protected void configureShell(Shell newShell) {
		newShell.setText("Quick Edit");

		super.configureShell(newShell);
	}

	public void setModel(PGridModel model) {
		_model = model;
	}

	public PGridModel getModel() {
		return _model;
	}

	private void afterCreateWidgets() {
		_grid.setModel(_model);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	public static void regreshAllPGridPropertyViews() {

		Arrays.stream(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences())
				.filter(ref -> ref.getId().equals("org.eclipse.ui.views.PropertySheet")).forEach(ref -> {
					var view = ref.getView(false);
					if (view != null) {
						var propView = (PropertySheet) view;
						var page = (PGridPage) propView.getCurrentPage();
						page.getGrid().refresh();
					}
				});

	}

}
