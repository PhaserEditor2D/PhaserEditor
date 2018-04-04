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
package phasereditor.assetpack.ui.refactorings;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author arian
 *
 */
public class AssetRenameWizardPage extends UserInputWizardPage {

	private Text fNameField;
	private BaseAssetRenameProcessor fRefactoringProcessor;

	public AssetRenameWizardPage(BaseAssetRenameProcessor processor) {
		super("Rename Asset");
		setTitle("Remove Asset");
		fRefactoringProcessor = processor;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setFont(parent.getFont());

		Label label = new Label(composite, SWT.NONE);
		label.setText("New name:");
		label.setLayoutData(new GridData());

		fNameField = new Text(composite, SWT.BORDER);
		String resourceName = fRefactoringProcessor.getNewName();
		fNameField.setText(resourceName);
		fNameField.setFont(composite.getFont());
		fNameField.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
		fNameField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});

		fNameField.selectAll();
		setPageComplete(false);
		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			fNameField.setFocus();
		}
		super.setVisible(visible);
	}

	protected final void validatePage() {
		String text = fNameField.getText();
		RefactoringStatus status = fRefactoringProcessor.validateNewName(text);
		setPageComplete(status);
	}
	
	@Override
	protected boolean performFinish() {
		fRefactoringProcessor.setNewName(fNameField.getText());
		return super.performFinish();
	}

}
