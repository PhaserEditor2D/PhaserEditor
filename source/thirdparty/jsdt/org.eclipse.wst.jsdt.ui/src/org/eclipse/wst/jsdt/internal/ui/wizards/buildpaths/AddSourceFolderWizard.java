/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;

public class AddSourceFolderWizard extends BuildPathWizard {
	
	private AddSourceFolderWizardPage fAddFolderPage;
	private SetFilterWizardPage fFilterPage;
	private final boolean fLinkedMode;
	private boolean fAllowConflict;
	private final boolean fAllowRemoveProjectFolder;
	private final boolean fAllowAddExclusionPatterns;
	private final boolean fCanCommitConflict;
	private final IContainer fParent;
	
	public AddSourceFolderWizard(CPListElement[] existingEntries, CPListElement newEntry, 
			boolean linkedMode, boolean canCommitConflict, 
			boolean allowConflict, boolean allowRemoveProjectFolder, boolean allowAddExclusionPatterns) {
		this(existingEntries, newEntry, linkedMode, canCommitConflict, allowConflict, allowRemoveProjectFolder, allowAddExclusionPatterns, newEntry.getJavaProject().getProject());
	}
	
	public AddSourceFolderWizard(CPListElement[] existingEntries, CPListElement newEntry, 
			boolean linkedMode, boolean canCommitConflict, 
			boolean allowConflict, boolean allowRemoveProjectFolder, boolean allowAddExclusionPatterns, IContainer parent) {
		super(existingEntries, newEntry, getTitel(newEntry, linkedMode), JavaPluginImages.DESC_WIZBAN_NEWSRCFOLDR);
		fLinkedMode= linkedMode;
		fCanCommitConflict= canCommitConflict;
		fAllowConflict= allowConflict;
		fAllowRemoveProjectFolder= allowRemoveProjectFolder;
		fAllowAddExclusionPatterns= allowAddExclusionPatterns;
		fParent= parent;
	}

	private static String getTitel(CPListElement newEntry, boolean linkedMode) {
		if (newEntry.getPath() == null) {
			if (linkedMode) {
				return NewWizardMessages.NewSourceFolderCreationWizard_link_title;
			} else {
				return NewWizardMessages.NewSourceFolderCreationWizard_title;
			}
		} else {
			return NewWizardMessages.NewSourceFolderCreationWizard_edit_title;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void addPages() {
		super.addPages();
	
		fAddFolderPage= new AddSourceFolderWizardPage(getEntryToEdit(), getExistingEntries(), 
				fLinkedMode, fCanCommitConflict,
				fAllowConflict, fAllowRemoveProjectFolder, fAllowAddExclusionPatterns, fParent);
		addPage(fAddFolderPage);
		
		fFilterPage= new SetFilterWizardPage(getEntryToEdit(), getExistingEntries());
		addPage(fFilterPage);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List getInsertedElements() {
		List result= super.getInsertedElements();
		if (getEntryToEdit().getOrginalPath() == null)
			result.add(getEntryToEdit());
		
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public List getRemovedElements() {
		return fAddFolderPage.getRemovedElements();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List getModifiedElements() {
		return fAddFolderPage.getModifiedElements();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean performFinish() {
		getEntryToEdit().setAttribute(CPListElement.INCLUSION, fFilterPage.getInclusionPattern());
		getEntryToEdit().setAttribute(CPListElement.EXCLUSION, fFilterPage.getExclusionPattern());
		
		boolean res= super.performFinish();
		if (res) {
			selectAndReveal(fAddFolderPage.getCorrespondingResource());
		}
		return res;
	}
	
	public void cancel() {
		fAddFolderPage.restore();
	}
}
