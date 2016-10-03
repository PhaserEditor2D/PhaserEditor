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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;

/**
 * Handler to be used to run a quick fix or assist by keyboard shortcut
 */
public class CorrectionCommandHandler extends AbstractHandler {
		
	private final JavaEditor fEditor;
	private final String fId;
	private final boolean fIsAssist;

	public CorrectionCommandHandler(JavaEditor editor, String id, boolean isAssist) {
		fEditor= editor;
		fId= id;
		fIsAssist= isAssist;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection= fEditor.getSelectionProvider().getSelection();
		IJavaScriptUnit cu= JavaScriptUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
		IAnnotationModel model= JavaScriptUI.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
		if (selection instanceof ITextSelection && cu != null && model != null) {
			if (! ActionUtil.isEditable(fEditor)) {
				return null;
			}
			ICompletionProposal proposal= findCorrection(fId, fIsAssist, (ITextSelection) selection, cu, model);
			if (proposal != null) {
				invokeProposal(proposal, ((ITextSelection) selection).getOffset());
			}
		}
		return null;
	}
	
	private ICompletionProposal findCorrection(String id, boolean isAssist, ITextSelection selection, IJavaScriptUnit cu, IAnnotationModel model) {
		AssistContext context= new AssistContext(cu, selection.getOffset(), selection.getLength());
		Collection proposals= new ArrayList(10);
		if (isAssist) {
			if (id.equals(LinkedNamesAssistProposal.ASSIST_ID)) {
				return getLocalRenameProposal(context); // shortcut for local rename
			}
			JavaCorrectionProcessor.collectAssists(context, new ProblemLocation[0], proposals);
		} else {
			try {
				boolean goToClosest= selection.getLength() == 0; 
				Annotation[] annotations= getAnnotations(selection.getOffset(), goToClosest);
				JavaCorrectionProcessor.collectProposals(context, model, annotations, true, false, proposals);
			} catch (BadLocationException e) {
				return null;
			}
		}
		for (Iterator iter= proposals.iterator(); iter.hasNext();) {
			Object curr= iter.next();
			if (curr instanceof ICommandAccess) {
				if (id.equals(((ICommandAccess) curr).getCommandId())) {
					return (ICompletionProposal) curr;
				}
			}
		}
		return null;
	}

	private Annotation[] getAnnotations(int offset, boolean goToClosest) throws BadLocationException {
		ArrayList resultingAnnotations= new ArrayList();
		JavaCorrectionAssistant.collectQuickFixableAnnotations(fEditor, offset, goToClosest, resultingAnnotations);
		return (Annotation[]) resultingAnnotations.toArray(new Annotation[resultingAnnotations.size()]);
	}
	
	private ICompletionProposal getLocalRenameProposal(IInvocationContext context) {
		ASTNode node= context.getCoveringNode();
		if (node instanceof SimpleName) {
			return new LinkedNamesAssistProposal(context.getCompilationUnit(), (SimpleName) node);
		}
		return null;
	}

	private IDocument getDocument() {
		return JavaScriptUI.getDocumentProvider().getDocument(fEditor.getEditorInput());
	}
	
	
	private void invokeProposal(ICompletionProposal proposal, int offset) {
		if (proposal instanceof ICompletionProposalExtension2) {
			ITextViewer viewer= fEditor.getViewer();
			if (viewer != null) {
				((ICompletionProposalExtension2) proposal).apply(viewer, (char) 0, 0, offset);
				return;
			}
		} else if (proposal instanceof ICompletionProposalExtension) {
			IDocument document= getDocument();
			if (document != null) {
				((ICompletionProposalExtension) proposal).apply(document, (char) 0, offset);
				return;
			}
		}
		IDocument document= getDocument();
		if (document != null) {
			proposal.apply(document);
		}
	}
	
	public static String getShortCutString(String proposalId) {
		if (proposalId != null) {
			IBindingService bindingService= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
			if (bindingService != null) {
				TriggerSequence[] activeBindingsFor= bindingService.getActiveBindingsFor(proposalId);
				if (activeBindingsFor.length > 0) {
					return activeBindingsFor[0].format();
				}
			}
		}
		return null;
	}
	
}
