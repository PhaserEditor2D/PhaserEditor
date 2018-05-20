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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.EditorSettings_UserCode;

/**
 * @author arian
 *
 */
public class UserCodeDialog extends Dialog {

	private EditorSettings_UserCode _code;
	private UserCodeBeforeAfterCodeComp _codeComp_init;
	private UserCodeBeforeAfterCodeComp _codeComp_ctor;
	private UserCodeBeforeAfterCodeComp _codeComp_preload;
	private UserCodeBeforeAfterCodeComp _codeComp_create;
	private TabFolder _tabFolder;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public UserCodeDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE | SWT.TITLE);
	}

	@Override
	protected void configureShell(Shell newShell) {
		newShell.setText("User Code");
		super.configureShell(newShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout(SWT.HORIZONTAL));

		_tabFolder = new TabFolder(container, SWT.NONE);

		if (_code.getCanvasModel().getType() == CanvasType.STATE) {
			{
				// constructor

				TabItem tabItem = new TabItem(_tabFolder, SWT.NONE);
				tabItem.setText("Constructor");

				_codeComp_ctor = new UserCodeBeforeAfterCodeComp(_tabFolder, SWT.NONE, "constructor");
				tabItem.setControl(_codeComp_ctor);
				tabItem.setData(_codeComp_ctor);
			}

			{
				// init
				TabItem tabItem = new TabItem(_tabFolder, SWT.NONE);
				tabItem.setText("Init");

				Composite comp = new Composite(_tabFolder, SWT.NONE);

				GridLayout gl = new GridLayout(2, false);
				comp.setLayout(gl);

				_codeComp_init = new UserCodeBeforeAfterCodeComp(comp, SWT.NONE, "init");
				_codeComp_init.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

				tabItem.setControl(comp);
				tabItem.setData(_codeComp_init);
			}

			{
				// preload
				TabItem tabItem = new TabItem(_tabFolder, SWT.NONE);
				tabItem.setText("Preload");

				_codeComp_preload = new UserCodeBeforeAfterCodeComp(_tabFolder, SWT.NONE, "preload");
				tabItem.setControl(_codeComp_preload);
				tabItem.setData(_codeComp_preload);
			}
		}

		{
			// create
			TabItem tabItem = new TabItem(_tabFolder, SWT.NONE);
			tabItem.setText("Create");

			_codeComp_create = new UserCodeBeforeAfterCodeComp(_tabFolder, SWT.NONE, "create");
			tabItem.setControl(_codeComp_create);
			tabItem.setData(_codeComp_create);
		}

		afterCreateWidgets();

		return container;
	}

	private void afterCreateWidgets() {
		if (_code.getCanvasModel().getType() == CanvasType.STATE) {
			_codeComp_ctor.setBeforeText(_code.getState_constructor_before());
			_codeComp_ctor.setAfterText(_code.getState_constructor_after());

			_codeComp_init.setInitArgs(_code.getState_init_args());
			_codeComp_init.setBeforeText(_code.getState_init_before());
			_codeComp_init.setAfterText(_code.getState_init_after());

			_codeComp_preload.setBeforeText(_code.getState_preload_before());
			_codeComp_preload.setAfterText(_code.getState_preload_after());
		}

		_codeComp_create.setBeforeText(_code.getCreate_before());
		_codeComp_create.setAfterText(_code.getCreate_after());

		for (TabItem item : _tabFolder.getItems()) {
			UserCodeBeforeAfterCodeComp comp = (UserCodeBeforeAfterCodeComp) item.getData();

			if (comp.getBeforeText().trim().length() > 0 || comp.getAfterText().trim().length() > 0) {
				item.setText(item.getText() + "*");
			}
		}
	}

	public void setUserCode(EditorSettings_UserCode code) {
		_code = code;
	}

	@Override
	protected void okPressed() {
		if (_code.getCanvasModel().getType() == CanvasType.STATE) {
			_code.setState_constructor_before(_codeComp_ctor.getBeforeText());
			_code.setState_constructor_after(_codeComp_ctor.getAfterText());

			_code.setState_init_args(_codeComp_init.getInitArgs());
			_code.setState_init_before(_codeComp_init.getBeforeText());
			_code.setState_init_after(_codeComp_init.getAfterText());

			_code.setState_preload_before(_codeComp_preload.getBeforeText());
			_code.setState_preload_after(_codeComp_preload.getAfterText());
		}

		_code.setCreate_before(_codeComp_create.getBeforeText());
		_code.setCreate_after(_codeComp_create.getAfterText());

		super.okPressed();
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

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(560, 472);
	}

}
