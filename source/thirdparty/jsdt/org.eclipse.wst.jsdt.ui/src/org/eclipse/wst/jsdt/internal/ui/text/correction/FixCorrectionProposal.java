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
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.fix.AbstractFix;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedFix;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;

/**
 * A correction proposal which uses an {@link IFix} to
 * fix a problem. A fix correction proposal may have an {@link ICleanUp}
 * attached which can be executed instead of the provided IFix.
 */
public class FixCorrectionProposal extends LinkedCorrectionProposal implements ICompletionProposalExtension2, IStatusLineProposal {
	
	private final IFix fFix;
	private final ICleanUp fCleanUp;
	private JavaScriptUnit fCompilationUnit;
	
	public FixCorrectionProposal(IFix fix, ICleanUp cleanUp, int relevance, Image image, IInvocationContext context) {
		super(fix.getDescription(), fix.getCompilationUnit(), null, relevance, image);
		fFix= fix;
		fCleanUp= cleanUp;
		fCompilationUnit= context.getASTRoot();
	}
	
	public ICleanUp getCleanUp() {
		return fCleanUp;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeCorrectionProposal#getImage()
	 */
	public Image getImage() {
		IStatus status= fFix.getStatus();
		if (!status.isOK()) {
			ImageImageDescriptor image= new ImageImageDescriptor(super.getImage());
			
			int flag= JavaScriptElementImageDescriptor.WARNING;
			if (status.getSeverity() == IStatus.ERROR) {
				flag= JavaScriptElementImageDescriptor.ERROR;
			}
			
			ImageDescriptor composite= new JavaScriptElementImageDescriptor(image, flag, new Point(image.getImageData().width, image.getImageData().height));
			return composite.createImage();		
		} else {
			return super.getImage();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.CUCorrectionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
	    String result= super.getAdditionalProposalInfo();
	    IStatus status= fFix.getStatus();
	    if (!status.isOK()) {
	    	StringBuffer buf= new StringBuffer();
	    	buf.append("<b>"); //$NON-NLS-1$
	    	buf.append(CorrectionMessages.FixCorrectionProposal_WarningAdditionalProposalInfo);
	    	buf.append("</b>"); //$NON-NLS-1$
	    	buf.append(status.getMessage());
	    	buf.append("<br><br>"); //$NON-NLS-1$
	    	buf.append(result);
	    	return buf.toString();
	    } else {
	    	if (fFix instanceof AbstractFix) {
				AbstractFix af = (AbstractFix) fFix;
				String info = af.getAdditionalInfo();
				if (info != null) {
					StringBuffer sb=new StringBuffer();
					sb.append(info);
					sb.append("<br>"); //$NON-NLS-1$
					sb.append(result);
					return sb.toString();
				}
			}
	    	return result;
	    }
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeCorrectionProposal#getRelevance()
	 */
	public int getRelevance() {
		IStatus status= fFix.getStatus();
	    if (status.getSeverity() == IStatus.WARNING) {
	    	return super.getRelevance() - 100;
	    } else {
	    	return super.getRelevance();
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.CUCorrectionProposal#createTextChange()
	 */
	protected TextChange createTextChange() throws CoreException {
		IFix fix= fFix;
		TextChange createChange= fix.createChange();
		if (createChange instanceof TextFileChange)
			((TextFileChange)createChange).setSaveMode(TextFileChange.LEAVE_DIRTY);
		
		if (fix instanceof LinkedFix) {
			setLinkedProposalModel(((LinkedFix) fix).getLinkedPositions());
		}
		
		if (createChange == null)
			return new CompilationUnitChange("", getCompilationUnit()); //$NON-NLS-1$
		
		return createChange;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		if (stateMask == SWT.CONTROL && fCleanUp != null){
			CleanUpRefactoring refactoring= new CleanUpRefactoring();
			refactoring.addCompilationUnit(getCompilationUnit());
			refactoring.addCleanUp(fCleanUp);
			refactoring.setLeaveFilesDirty(true);
			
			int stopSeverity= RefactoringCore.getConditionCheckingFailedSeverity();
			Shell shell= JavaScriptPlugin.getActiveWorkbenchShell();
			ProgressMonitorDialog context= new ProgressMonitorDialog(shell);
			RefactoringExecutionHelper executer= new RefactoringExecutionHelper(refactoring, stopSeverity, RefactoringSaveHelper.SAVE_NOTHING, shell, context);
			try {
				executer.perform(true, true);
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				JavaScriptPlugin.log(e);
			}
			return;
		}
		apply(viewer.getDocument());
	}

	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	public void unselected(ITextViewer viewer) {
	}

	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getStatusMessage() {
		if (fCleanUp == null)
			return null;
		
		int count= fCleanUp.maximalNumberOfFixes(fCompilationUnit);
		if (count == -1) {
			return CorrectionMessages.FixCorrectionProposal_HitCtrlEnter_description;
		} else if (count < 2) {
			return ""; //$NON-NLS-1$
		} else {
			return Messages.format(CorrectionMessages.FixCorrectionProposal_hitCtrlEnter_variable_description, Integer.valueOf(count));
		}
	}

}
