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
package phasereditor.assetpack.ui.editors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import phasereditor.assetpack.core.AssetFactory;
import phasereditor.assetpack.core.AssetType;

public class AddAssetToPackDialog extends TitleAreaDialog {
	private static class SupportLabelProvider extends LabelProvider {
		public SupportLabelProvider() {
		}

		@Override
		public String getText(Object element) {
			return ((AssetFactory) element).getLabel();
		}
	}

	private Text _descriptionText;
	private ListViewer _listViewer;
	private Button _okButton;
	private AssetFactory _selection;
	private AssetType _initialType;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public AddAssetToPackDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage("Add new asset.");
		setTitle("Asset Type");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		_listViewer = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL);
		_listViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				AddAssetToPackDialog.this.doubleClick();
			}
		});
		List list = _listViewer.getList();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_listViewer.setLabelProvider(new SupportLabelProvider());
		_listViewer.setContentProvider(new ArrayContentProvider());

		_descriptionText = new Text(container, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		GridData gd_descriptionLabel = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_descriptionLabel.widthHint = 200;
		gd_descriptionLabel.heightHint = 100;
		_descriptionText.setLayoutData(gd_descriptionLabel);
		_descriptionText.setText("Description");

		return area;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		// $hide>>$
		AssetFactory[] all = AssetFactory.getFactories();
		_listViewer.setInput(all);
		_listViewer.addSelectionChangedListener(this::selectionChanged);
		AssetFactory factory = _initialType == null ? all[0] : AssetFactory.getFactory(_initialType);
		_listViewer.setSelection(new StructuredSelection(factory), true);
		// $hide<<$
		return contents;
	}

	private void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection sel = (IStructuredSelection) event.getSelection();
		_okButton.setEnabled(!sel.isEmpty());
		_selection = (AssetFactory) sel.getFirstElement();
		_descriptionText.setText(_selection.getHelp());
	}

	void doubleClick() {
		okPressed();
	}

	public AssetFactory getSelection() {
		return _selection;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		_okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Add Asset");
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(549, 561);
	}

	public void setInitialType(AssetType initialType) {
		_initialType = initialType;
	}
}
