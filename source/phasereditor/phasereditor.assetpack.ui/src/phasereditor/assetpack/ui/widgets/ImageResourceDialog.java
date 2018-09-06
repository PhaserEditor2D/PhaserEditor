// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.ui.widgets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class ImageResourceDialog extends Dialog {
	private TableViewer _listViewer;
	private Object _selection;
	private Label _messageLabel;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public ImageResourceDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));

		Composite composite_1 = new Composite(container, SWT.NONE);
		GridData gd_composite_1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_composite_1.verticalIndent = 10;
		composite_1.setLayoutData(gd_composite_1);
		composite_1.setLayout(new GridLayout(1, false));

		_messageLabel = new Label(composite_1, SWT.NONE);
		_messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		_messageLabel.setText("Select the yyy image. Those in bold are not used in this pack.");

		Composite composite = new Composite(container, SWT.NONE);
		GridData gd_composite = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_composite.verticalIndent = 10;
		composite.setLayoutData(gd_composite);
		GridLayout gl_composite = new GridLayout(2, true);
		composite.setLayout(gl_composite);

		_listViewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | (_multi ? SWT.MULTI : 0));
		Table list = _listViewer.getTable();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

		_imagePreviewCanvas = new ImagePreviewComp(composite, SWT.BORDER);
		_imagePreviewCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_listViewer.setContentProvider(new ArrayContentProvider());
		_listViewer.setLabelProvider(new LabelProvider());

		return container;
	}

	private LabelProvider _labelProvider;
	private Object _input;
	private ImagePreviewComp _imagePreviewCanvas;
	private IResource _initial;
	private String _objectName;
	private Object[] _multipleSelection;
	private boolean _multi;

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		if (_labelProvider != null) {
			_listViewer.setLabelProvider(_labelProvider);
		}
		if (_input != null) {
			_listViewer.setInput(_input);
		}
		_listViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		_listViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				getPreview().setImageFile((IFile) ((IStructuredSelection) event.getSelection()).getFirstElement());
			}
		});
		if (_initial != null) {
			_listViewer.setSelection(new StructuredSelection(_initial), true);
		}
		if (_objectName != null) {
			_messageLabel.setText("Select the " + _objectName + " image. Those in bold are not used in this pack.");
		}
		return control;
	}

	public void setMulti(boolean multi) {
		_multi = multi;
	}

	public boolean isMulti() {
		return _multi;
	}

	public void setObjectName(String objectName) {
		_objectName = objectName;
	}

	public void setLabelProvider(LabelProvider labelProvider) {
		_labelProvider = labelProvider;
	}

	public void setInput(Object input) {
		_input = input;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		IStructuredSelection sel = (IStructuredSelection) _listViewer.getSelection();
		_selection = sel.getFirstElement();
		_multipleSelection = sel.toArray();

		super.okPressed();
	}

	public Object getSelection() {
		return _selection;
	}

	public Object[] getMultipleSelection() {
		return _multipleSelection;
	}

	public ImagePreviewComp getPreview() {
		return _imagePreviewCanvas;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(544, 481);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(_objectName == null ? "Image" : _objectName);
	}

	public void setInitial(IResource initial) {
		_initial = initial;
	}

}
