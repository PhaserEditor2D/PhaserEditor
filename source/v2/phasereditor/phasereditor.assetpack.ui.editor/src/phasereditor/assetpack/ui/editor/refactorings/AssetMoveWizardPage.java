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
package phasereditor.assetpack.ui.editor.refactorings;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.AssetLabelProvider;

/**
 * @author arian
 *
 */
public class AssetMoveWizardPage extends UserInputWizardPage {

	private AssetMoveProcessor fRefactoringProcessor;
	private ComboViewer _viewer;

	public AssetMoveWizardPage(AssetMoveProcessor processor) {
		super("Move Asset");
		setTitle("Move Asset");
		fRefactoringProcessor = processor;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.None);
		label.setText("Move to section:");
		GridData gd_label = new GridData();
		label.setLayoutData(gd_label);

		_viewer = new ComboViewer(composite);
		Combo combo = _viewer.getCombo();
		GridData gd_combo = new GridData();
		gd_combo.horizontalAlignment = SWT.FILL;
		gd_combo.grabExcessHorizontalSpace = true;
		combo.setLayoutData(gd_combo);

		AssetSectionModel section = fRefactoringProcessor.getDstSection();

		_viewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
		_viewer.setContentProvider(new ArrayContentProvider());
		_viewer.setInput(section.getPack().getSections());
		_viewer.setSelection(new StructuredSelection(section));

		_viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validatePage();
			}
		});
		_viewer.setSelection(new StructuredSelection(fRefactoringProcessor.getDstSection()));
		
		validatePage();

		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			_viewer.getCombo().setFocus();
		}
		super.setVisible(visible);
	}

	protected final void validatePage() {
		fRefactoringProcessor.setDstSection(getSelectedSection());
		RefactoringStatus status = fRefactoringProcessor.validateDestiny(getSelectedSection());
		setPageComplete(status);
	}

	private AssetSectionModel getSelectedSection() {
		return (AssetSectionModel) _viewer.getStructuredSelection().getFirstElement();
	}

	@Override
	protected boolean performFinish() {
		fRefactoringProcessor.setDstSection(getSelectedSection());
		return super.performFinish();
	}

}
