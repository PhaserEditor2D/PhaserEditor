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

import org.eclipse.core.resources.IResource;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CopyRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgDestinationValidator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaCopyProcessor;


public class ReorgCopyWizard extends RefactoringWizard {

	public ReorgCopyWizard(CopyRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | NO_PREVIEW_PAGE); 
		setDefaultPageTitle(ReorgMessages.ReorgCopyWizard_1); 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		addPage(new CopyInputPage());
	}
	
	private static class CopyInputPage extends ReorgUserInputPage{

		private static final String PAGE_NAME= "CopyInputPage"; //$NON-NLS-1$

		public CopyInputPage() {
			super(PAGE_NAME);
		}

		private JavaCopyProcessor getCopyProcessor(){
			return (JavaCopyProcessor)((CopyRefactoring)getRefactoring()).getCopyProcessor();
		}

		protected Object getInitiallySelectedElement() {
			return getCopyProcessor().getCommonParentForInputElements();
		}

		protected IJavaScriptElement[] getJavaElements() {
			return getCopyProcessor().getJavaElements();
		}

		protected IResource[] getResources() {
			return getCopyProcessor().getResources();
		}

		protected IReorgDestinationValidator getDestinationValidator() {
			return getCopyProcessor();
		}
		
		protected RefactoringStatus verifyDestination(Object selected) throws JavaScriptModelException{
			if (selected instanceof IJavaScriptElement)
				return getCopyProcessor().setDestination((IJavaScriptElement)selected);
			if (selected instanceof IResource)
				return getCopyProcessor().setDestination((IResource)selected);
			return RefactoringStatus.createFatalErrorStatus(ReorgMessages.ReorgCopyWizard_2); 
		}		
	}
}
