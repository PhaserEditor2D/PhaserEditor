// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.ui.editors.grid.editors;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import phasereditor.canvas.core.GroupModel.SetAllData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.CellEditor;

/**
 * @author arian
 *
 */
@SuppressWarnings("synthetic-access")
public class SetAllDialog extends Dialog {
	private Table _table;
	SetAllData _list = new SetAllData();
	TableViewer _tableViewer;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public SetAllDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE | SWT.TITLE);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@SuppressWarnings("unused")
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, false));

		Label lblAddThePairs = new Label(container, SWT.NONE);
		lblAddThePairs.setText(
				"Add the pairs of property/value to be set to the Group.\r\nThe value will be verbatim copied into the generated code.");
		new Label(container, SWT.NONE);

		_tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
		_table = _tableViewer.getTable();
		_table.setLinesVisible(true);
		_table.setHeaderVisible(true);
		_table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		TableViewerColumn tableViewerColumn = new TableViewerColumn(_tableViewer, SWT.NONE);
		tableViewerColumn.setEditingSupport(new EditingSupport(_tableViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(_table);
			}

			@Override
			protected Object getValue(Object element) {
				return ((String[]) element)[0];
			}

			@Override
			protected void setValue(Object element, Object value) {
				((String[]) element)[0] = (String) value;
				_tableViewer.refresh(element);
			}
		});
		tableViewerColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				return ((String[]) element)[0];
			}
		});
		TableColumn tblclmnProperty = tableViewerColumn.getColumn();
		tblclmnProperty.setWidth(269);
		tblclmnProperty.setText("Property");

		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(_tableViewer, SWT.NONE);
		tableViewerColumn_1.setEditingSupport(new EditingSupport(_tableViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(_table);
			}

			@Override
			protected Object getValue(Object element) {
				return ((String[]) element)[1];
			}

			@Override
			protected void setValue(Object element, Object value) {
				((String[]) element)[1] = (String) value;
				_tableViewer.refresh(element);
			}
		});
		tableViewerColumn_1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((String[]) element)[1];
			}
		});
		TableColumn tblclmnValue_1 = tableViewerColumn_1.getColumn();
		tblclmnValue_1.setWidth(133);
		tblclmnValue_1.setText("Value");
		_tableViewer.setContentProvider(new ArrayContentProvider());

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.marginWidth = 0;
		gl_composite.marginHeight = 0;
		gl_composite.horizontalSpacing = 0;
		composite.setLayout(gl_composite);

		Button btnAdd = new Button(composite, SWT.NONE);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SetAll_Entry_Dialog dlg = new SetAll_Entry_Dialog(e.display.getActiveShell());
				if (dlg.open() == Window.OK) {
					_list.add(new String[] { dlg.getProperty(), dlg.getValue() });
					_tableViewer.refresh();
				}
			}
		});
		GridData gd_btnAdd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_btnAdd.widthHint = 100;
		btnAdd.setLayoutData(gd_btnAdd);
		btnAdd.setBounds(0, 0, 90, 30);
		btnAdd.setText("Add");

		Button btnRemove = new Button(composite, SWT.NONE);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<?> elems = _tableViewer.getStructuredSelection().toList();
				_list.removeAll(elems);
				_tableViewer.refresh();
			}
		});
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnRemove.setText("Remove");

		afterCreateWidgets();

		return container;
	}

	private void afterCreateWidgets() {
		_tableViewer.setInput(_list);
	}

	public void setSetAllData(SetAllData data) {
		_list.addAll(data.copy());
	}

	public SetAllData getResult() {
		return _list.copy();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(640, 425);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Set All");
	}
}
