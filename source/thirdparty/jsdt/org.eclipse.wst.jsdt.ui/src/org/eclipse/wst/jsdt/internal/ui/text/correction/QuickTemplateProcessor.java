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
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitContextType;
import org.eclipse.wst.jsdt.internal.corext.template.java.JavaContextType;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.internal.ui.text.template.contentassist.SurroundWithTemplateProposal;
import org.eclipse.wst.jsdt.internal.ui.text.template.contentassist.TemplateProposal;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;
import org.eclipse.wst.jsdt.ui.text.java.IQuickAssistProcessor;

import com.ibm.icu.text.Collator;


/**
 * Quick template processor.
 */
public class QuickTemplateProcessor implements IQuickAssistProcessor {

	private static final String $_LINE_SELECTION= "${" + GlobalTemplateVariables.LineSelection.NAME + "}"; //$NON-NLS-1$ //$NON-NLS-2$

	public QuickTemplateProcessor() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistProcessor#hasAssists(org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistContext)
	 */
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		IJavaScriptUnit cu= context.getCompilationUnit();
		IDocument document= getDocument(cu);

		int offset= context.getSelectionOffset();
		int length= context.getSelectionLength();
		if (length == 0) {
			return false;
		}

		try {
			int startLine= document.getLineOfOffset(offset);
			int endLine= document.getLineOfOffset(offset + length);
			IRegion region= document.getLineInformation(endLine);
			return startLine  < endLine || length > 0 && offset == region.getOffset() && length == region.getLength();
		} catch (BadLocationException e) {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistProcessor#getAssists(org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistContext, org.eclipse.wst.jsdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		if (locations != null && locations.length > 0) {
			return new IJavaCompletionProposal[0];
		}

		try {
			int offset= context.getSelectionOffset();
			int length= context.getSelectionLength();
			if (length == 0) {
				return null;
			}

			IJavaScriptUnit cu= context.getCompilationUnit();
			IDocument document= getDocument(cu);

			// test if selection is either a full line or spans over multiple lines
			int startLine= document.getLineOfOffset(offset);
			int endLine= document.getLineOfOffset(offset + length);
			IRegion endLineRegion= document.getLineInformation(endLine);
			//if end position is at start of line, set it back to the previous line's end
			if (endLine > startLine && endLineRegion.getOffset() == offset + length) {
				endLine--;
				endLineRegion= document.getLineInformation(endLine);
				length= endLineRegion.getOffset() + endLineRegion.getLength() - offset;
			}
			if (startLine  == endLine) {
				if (length == 0 || offset != endLineRegion.getOffset() || length != endLineRegion.getLength()) {
					AssistContext invocationContext= new AssistContext(cu, offset, length);
					if (!SurroundWith.isApplicable(invocationContext))
						return null;
				}
			} else {
				// expand selection
				offset= document.getLineOffset(startLine);
				length= endLineRegion.getOffset() + endLineRegion.getLength() - offset;
			}

			ArrayList resultingCollections= new ArrayList();
			collectSurroundTemplates(document, cu, offset, length, resultingCollections);
			sort(resultingCollections);
			return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	private void sort(ArrayList proposals) {
		Collections.sort(proposals, new Comparator() {
			public int compare(Object o1, Object o2) {
				IJavaCompletionProposal p1= (IJavaCompletionProposal)o1;
				IJavaCompletionProposal p2= (IJavaCompletionProposal)o2;
				return Collator.getInstance().compare(p1.getDisplayString(), p2.getDisplayString());
			}
		});
	}

	private IDocument getDocument(IJavaScriptUnit cu) throws JavaScriptModelException {
		IFile file= (IFile) cu.getResource();
		IDocument document= JavaScriptUI.getDocumentProvider().getDocument(new FileEditorInput(file));
		if (document == null) {
			return new Document(cu.getSource()); // only used by test cases
		}
		return document;
	}

	private void collectSurroundTemplates(IDocument document, IJavaScriptUnit cu, int offset, int length, Collection result) throws BadLocationException, CoreException {
		CompilationUnitContextType contextType= (CompilationUnitContextType) JavaScriptPlugin.getDefault().getTemplateContextRegistry().getContextType(JavaContextType.NAME);
		CompilationUnitContext context= contextType.createContext(document, offset, length, cu);
		context.setVariable("selection", document.get(offset, length)); //$NON-NLS-1$
		context.setForceEvaluation(true);

		int start= context.getStart();
		int end= context.getEnd();
		IRegion region= new Region(start, end - start);

		AssistContext invocationContext= new AssistContext(cu, start, end - start);
		Statement[] selectedStatements= SurroundWith.getSelectedStatements(invocationContext);
		
		Template[] templates= JavaScriptPlugin.getDefault().getTemplateStore().getTemplates();
		for (int i= 0; i != templates.length; i++) {
			Template currentTemplate= templates[i];
			if (context.canEvaluate(currentTemplate) && currentTemplate.getContextTypeId().equals(JavaContextType.NAME) && currentTemplate.getPattern().indexOf($_LINE_SELECTION) != -1) {
				// TODO using jdt proposals for the moment, as jdt expects IJavaCompletionProposals
				
				if (selectedStatements != null) {
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					TemplateProposal proposal= new SurroundWithTemplateProposal(cu, currentTemplate, context, region, image, selectedStatements);
					String[] arg= new String[] { currentTemplate.getName(), currentTemplate.getDescription() };
					proposal.setDisplayString(Messages.format(CorrectionMessages.QuickTemplateProcessor_surround_label, arg));
					result.add(proposal);
				} else {
					TemplateProposal proposal= new TemplateProposal(currentTemplate, context, region, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)) {
						/**
						 * {@inheritDoc}
						 */
						public boolean validate(IDocument doc, int off, DocumentEvent event) {
							return false;
						}
					};
					String[] arg= new String[] { currentTemplate.getName(), currentTemplate.getDescription() };
					proposal.setDisplayString(Messages.format(CorrectionMessages.QuickTemplateProcessor_surround_label, arg));
					result.add(proposal);
				}
			}
		}
	}

}
