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

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import phasereditor.lic.LicCore;

public class EvaluationDialog extends Dialog {

	protected Object _result;
	protected Shell _shell;
	private Button _btnContinue;
	private Button _button;
	private Composite _composite;

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public EvaluationDialog(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public Object open() {
		createContents();
		_shell.open();
		_shell.layout();
		Display display = getParent().getDisplay();
		while (!_shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return _result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		_shell = new Shell(getParent(), SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
		_shell.addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.keyCode == SWT.TRAVERSE_ESCAPE || e.character == SWT.ESC) {
					e.doit = false;
				}
			}
		});
		_shell.setSize(450, 179);
		_shell.setText("Evaluation Product");
		_shell.setLayout(new GridLayout(1, false));

		Label lblThisIsAn = new Label(_shell, SWT.CENTER);
		lblThisIsAn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		lblThisIsAn.setText("This is an evaluation copy of " + LicCore.PRODUCT_NAME);

		_composite = new Composite(_shell, SWT.NONE);
		_composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		_composite.setLayout(new GridLayout(2, false));

		_button = new Button(_composite, SWT.NONE);
		_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				LicCore.openActivationDialog();
				if (!LicCore.isEvaluationProduct()) {
					_shell.dispose();
				}
			}
		});
		GridData gd_button = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_button.widthHint = 120;
		_button.setLayoutData(gd_button);
		_button.setText("Activate");

		_btnContinue = new Button(_composite, SWT.NONE);
		GridData gd_btnContinue = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnContinue.widthHint = 120;
		_btnContinue.setLayoutData(gd_btnContinue);
		_btnContinue.setText("Wait");
		_btnContinue.setEnabled(false);

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		Rectangle b1 = getParent().getDisplay().getBounds();
		Rectangle b2 = _shell.getBounds();
		_shell.setLocation(b1.width / 2 - b2.width / 2, b1.height / 2 - b2.height / 2);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(3_000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Display.getDefault().asyncExec(new Runnable() {

					@SuppressWarnings("synthetic-access")
					@Override
					public void run() {
						try {
							_btnContinue.setText("Continue");
							_btnContinue.setEnabled(true);
							_btnContinue.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									_shell.close();
								}
							});
						} catch (SWTException e) {
							// nothing
						}
					}
				});
			}
		}).start();
	}
}
