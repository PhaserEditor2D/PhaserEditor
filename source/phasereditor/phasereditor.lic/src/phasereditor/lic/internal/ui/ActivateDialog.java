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
package phasereditor.lic.internal.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import phasereditor.lic.LicCore;
import phasereditor.lic.LicenseInfo;

/**
 * @author arian
 *
 */
public class ActivateDialog extends TitleAreaDialog {
	private Text _text;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public ActivateDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage("Activate this product by entering the license key");
		setTitle("Activation");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label lblTheLicenseKey = new Label(container, SWT.WRAP);
		GridData gd_lblTheLicenseKey = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_lblTheLicenseKey.heightHint = 40;
		lblTheLicenseKey.setLayoutData(gd_lblTheLicenseKey);
		lblTheLicenseKey.setText("The license key is a code you get when purchase Phaser Editor.");

		Label lblLicenseKey = new Label(container, SWT.NONE);
		lblLicenseKey.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblLicenseKey.setText("License Key");

		_text = new Text(container, SWT.BORDER);
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		return area;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		String key = _text.getText().trim();
		try {
			LicenseInfo info = LicCore.activateProduct(key);
			if (info.success) {
				getShell().getDisplay().asyncExec(new Runnable() {

					@SuppressWarnings("synthetic-access")
					@Override
					public void run() {
						String name = info.fullName.equals(LicCore.NO_NAME) ? info.email : info.fullName;
						MessageDialog.openInformation(getParentShell(), "Activation",
								"Congratulations " + name + ", Phaser Editor was activated.");
					}
				});
				super.okPressed();
			} else {
				MessageDialog.openError(getParentShell(), "Activation", info.message);
			}
		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(getParentShell(), "Activation",
					e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		button.setText("Activate");
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

}
