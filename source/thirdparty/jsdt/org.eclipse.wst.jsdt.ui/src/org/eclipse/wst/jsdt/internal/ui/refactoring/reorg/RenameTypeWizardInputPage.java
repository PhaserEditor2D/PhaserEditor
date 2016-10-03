/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.ISimilarDeclarationUpdating;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.RowLayouter;

/**
 * Wizard page for renaming a type (with similarly named elements)
 * 
 * 
 * 
 */
class RenameTypeWizardInputPage extends RenameInputWizardPage {

	private Button fUpdateSimilarElements;
	private int fSelectedStrategy;

	private Link fUpdateSimilarElementsButton;

	public RenameTypeWizardInputPage(String description, String contextHelpId, boolean isLastUserPage, String initialValue) {
		super(description, contextHelpId, isLastUserPage, initialValue);
	}

	protected void addAdditionalOptions(Composite composite, RowLayouter layouter) {

		if (getSimilarElementUpdating() == null || !getSimilarElementUpdating().canEnableSimilarDeclarationUpdating())
			return;

		try {
			fSelectedStrategy= getRefactoringSettings().getInt(RenameRefactoringWizard.TYPE_SIMILAR_MATCH_STRATEGY);
		} catch (NumberFormatException e) {
			fSelectedStrategy= getSimilarElementUpdating().getMatchStrategy();
		}

		getSimilarElementUpdating().setMatchStrategy(fSelectedStrategy);

		Composite c= new Composite(composite, SWT.NULL);
		GridLayout layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		c.setLayout(layout);

		fUpdateSimilarElements= new Button(c, SWT.CHECK);
		fUpdateSimilarElements.setText(RefactoringMessages.RenameTypeWizardInputPage_update_similar_elements);

		final boolean updateSimilarElements= getBooleanSetting(RenameRefactoringWizard.TYPE_UPDATE_SIMILAR_ELEMENTS, getSimilarElementUpdating().getUpdateSimilarDeclarations());
		fUpdateSimilarElements.setSelection(updateSimilarElements);
		getSimilarElementUpdating().setUpdateSimilarDeclarations(updateSimilarElements);
		fUpdateSimilarElements.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fUpdateSimilarElements.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				getSimilarElementUpdating().setUpdateSimilarDeclarations(fUpdateSimilarElements.getSelection());
				fUpdateSimilarElementsButton.setEnabled(fUpdateSimilarElements.getSelection());
			}
		});

		fUpdateSimilarElementsButton= new Link(c, SWT.NONE);
		GridData d= new GridData();
		d.grabExcessHorizontalSpace= true;
		d.horizontalAlignment= SWT.RIGHT;
		fUpdateSimilarElementsButton.setText(RefactoringMessages.RenameTypeWizardInputPage_update_similar_elements_configure);
		fUpdateSimilarElementsButton.setEnabled(updateSimilarElements);
		fUpdateSimilarElementsButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				RenameTypeWizardSimilarElementsOptionsDialog dialog= new RenameTypeWizardSimilarElementsOptionsDialog(getShell(), fSelectedStrategy);
				if (dialog.open() == Window.OK) {
					fSelectedStrategy= dialog.getSelectedStrategy();
					getSimilarElementUpdating().setMatchStrategy(fSelectedStrategy);
				}
			}
		});
		fUpdateSimilarElementsButton.setLayoutData(d);

		GridData forC= new GridData();
		forC.grabExcessHorizontalSpace= true;
		forC.horizontalAlignment= SWT.FILL;
		forC.horizontalSpan= 2;
		c.setLayoutData(forC);

		final Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layouter.perform(separator);
	}

	public void dispose() {
		if (saveSettings())
			if (fUpdateSimilarElements != null && !fUpdateSimilarElements.isDisposed() && fUpdateSimilarElements.isEnabled()) {
				saveBooleanSetting(RenameRefactoringWizard.TYPE_UPDATE_SIMILAR_ELEMENTS, fUpdateSimilarElements);
				getRefactoringSettings().put(RenameRefactoringWizard.TYPE_SIMILAR_MATCH_STRATEGY, fSelectedStrategy);
			}

		super.dispose();
	}

	/*
	 * Override - we don't want to initialize the next page (may needlessly
	 * trigger change creation if similar elements page is skipped, which is not
	 * indicated by fIsLastUserInputPage in parent).
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	private ISimilarDeclarationUpdating getSimilarElementUpdating() {
		return (ISimilarDeclarationUpdating) getRefactoring().getAdapter(ISimilarDeclarationUpdating.class);
	}
	
	protected boolean performFinish() {
		boolean returner= super.performFinish();
		// check if we got deferred to the error page
		if (!returner && getContainer().getCurrentPage() != null)
			getContainer().getCurrentPage().setPreviousPage(this);
		return returner;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		RenameTypeWizard wizard= (RenameTypeWizard) getWizard();
		IWizardPage nextPage;
		
		if (wizard.isRenameType()) {
			final RenameTypeProcessor renameTypeProcessor= wizard.getRenameTypeProcessor();
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {

					public void run(IProgressMonitor pm) throws InterruptedException {
						try {
							renameTypeProcessor.initializeReferences(pm);
						} catch (OperationCanceledException e) {
							throw new InterruptedException();
						} catch (CoreException e) {
							ExceptionHandler.handle(e, RefactoringMessages.RenameTypeWizard_defaultPageTitle,
									RefactoringMessages.RenameTypeWizard_unexpected_exception);
						} finally {
							pm.done();
						}
					}
				});
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.RenameTypeWizard_defaultPageTitle,
						RefactoringMessages.RenameTypeWizard_unexpected_exception);
			} catch (InterruptedException e) {
				// user canceled
				return this;
			}

			if (renameTypeProcessor.hasSimilarElementsToRename()) {
				nextPage= super.getNextPage();
			} else {
				nextPage= computeSuccessorPage();
			}
			
		} else
			nextPage= computeSuccessorPage();
		
		nextPage.setPreviousPage(this);
		return nextPage;
	}
}
