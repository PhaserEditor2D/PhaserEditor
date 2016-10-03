/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.text.java;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.jsdt.core.CompletionContext;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.text.java.ContentAssistHistory.RHSHistory;

/**
 * Describes the context of a content assist invocation in a JavaScript editor.
 * <p>
 * Clients may use but not subclass this class.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public class JavaContentAssistInvocationContext extends ContentAssistInvocationContext {
	private final IEditorPart fEditor;
	
	private IJavaScriptUnit fCU= null;
	private boolean fCUComputed= false;
	
	private CompletionProposalLabelProvider fLabelProvider;
	private CompletionProposalCollector fCollector;
	private RHSHistory fRHSHistory;
	private IType fType;

	private IJavaCompletionProposal[] fKeywordProposals= null;
	private CompletionContext fCoreContext= null;

	/**
	 * Creates a new context.
	 * 
	 * @param viewer the viewer used by the editor
	 * @param offset the invocation offset
	 * @param editor the editor that content assist is invoked in
	 */
	public JavaContentAssistInvocationContext(ITextViewer viewer, int offset, IEditorPart editor) {
		super(viewer, offset);
		
		fEditor= editor;
	}
	
	/**
	 * Creates a new context.
	 * 
	 * @param unit the compilation unit in <code>document</code>
	 */
	public JavaContentAssistInvocationContext(IJavaScriptUnit unit) {
		super();
		fCU= unit;
		fCUComputed= true;
		fEditor= null;
	}
	
	/**
	 * Returns the compilation unit that content assist is invoked in, <code>null</code> if there
	 * is none.
	 * 
	 * @return the compilation unit that content assist is invoked in, possibly <code>null</code>
	 */
	public IJavaScriptUnit getCompilationUnit() {
		if (!fCUComputed) {
			fCUComputed= true;
			if (fCollector != null)
				fCU= fCollector.getCompilationUnit();
			else {
				IJavaScriptElement je= EditorUtility.getEditorInputJavaElement(fEditor, false);
				if (je instanceof IJavaScriptUnit)
					fCU= (IJavaScriptUnit)je;
			}
		}
		return fCU;
	}
	
	/**
	 * Returns the project of the compilation unit that content assist is invoked in,
	 * <code>null</code> if none.
	 * 
	 * @return the current JavaScript project, possibly <code>null</code>
	 */
	public IJavaScriptProject getProject() {
		IJavaScriptUnit unit= getCompilationUnit();
		return unit == null ? null : unit.getJavaScriptProject();
	}
	
	/**
	 * Returns the keyword proposals that are available in this context, possibly none.
	 * <p>
	 * <strong>Note:</strong> This method may run
	 * {@linkplain org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor) codeComplete}
	 * on the compilation unit.
	 * </p>
	 * 
	 * @return the available keyword proposals
	 */
	public IJavaCompletionProposal[] getKeywordProposals() {
		if (fKeywordProposals == null) {
			if (fCollector != null && !fCollector.isIgnored(CompletionProposal.KEYWORD) && fCollector.getContext() != null) {
				// use the existing collector if it exists, collects keywords, and has already been invoked
				fKeywordProposals= fCollector.getKeywordCompletionProposals();
			} else {
				// otherwise, retrieve keywords ourselves
				computeKeywordsAndContext();
			}
		}
		
		return fKeywordProposals;
	}

	/**
	 * Returns the {@link CompletionContext core completion context} if available, <code>null</code>
	 * otherwise.
	 * <p>
	 * <strong>Note:</strong> This method may run
	 * {@linkplain org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor) codeComplete}
	 * on the compilation unit.
	 * </p>
	 * 
	 * @return the core completion context if available, <code>null</code> otherwise
	 */
	public CompletionContext getCoreContext() {
		if (fCoreContext == null) {
			// use the context from the existing collector if it exists, retrieve one ourselves otherwise
			if (fCollector != null)
				fCoreContext= fCollector.getContext();
			if (fCoreContext == null)
				computeKeywordsAndContext();
		}
		return fCoreContext;
	}

	/**
	 * Returns an float in [0.0,&nbsp;1.0] based on whether the type has been recently used as a
	 * right hand side for the type expected in the current context. 0 signals that the
	 * <code>qualifiedTypeName</code> does not match the expected type, while 1.0 signals that
	 * <code>qualifiedTypeName</code> has most recently been used in a similar context.
	 * <p>
	 * <strong>Note:</strong> This method may run
	 * {@linkplain org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor) codeComplete}
	 * on the compilation unit.
	 * </p>
	 * 
	 * @param qualifiedTypeName the type name of the type of interest
	 * @return a relevance in [0.0,&nbsp;1.0] based on previous content assist invocations
	 */
	public float getHistoryRelevance(String qualifiedTypeName) {
		return getRHSHistory().getRank(qualifiedTypeName);
	}
	
	/**
	 * Returns the content assist type history for the expected type.
	 * 
	 * @return the content assist type history for the expected type
	 */
	private RHSHistory getRHSHistory() {
		if (fRHSHistory == null) {
			CompletionContext context= getCoreContext();
			if (context != null) {
				char[][] expectedTypes= context.getExpectedTypesSignatures();
				if (expectedTypes != null && expectedTypes.length > 0) {
					String expected= SignatureUtil.stripSignatureToFQN(String.valueOf(expectedTypes[0]));
					fRHSHistory= JavaScriptPlugin.getDefault().getContentAssistHistory().getHistory(expected);
				}
			}
			if (fRHSHistory == null)
				fRHSHistory= JavaScriptPlugin.getDefault().getContentAssistHistory().getHistory(null);
		}
		return fRHSHistory;
	}
	
	/**
	 * Returns the expected type if any, <code>null</code> otherwise.
	 * <p>
	 * <strong>Note:</strong> This method may run
	 * {@linkplain org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor) codeComplete}
	 * on the compilation unit.
	 * </p>
	 * 
	 * @return the expected type if any, <code>null</code> otherwise
	 */
	public IType getExpectedType() {
		if (fType == null && getCompilationUnit() != null) {
			CompletionContext context= getCoreContext();
			if (context != null) {
				char[][] expectedTypes= context.getExpectedTypesSignatures();
				if (expectedTypes != null && expectedTypes.length > 0) {
					IJavaScriptProject project= getCompilationUnit().getJavaScriptProject();
					if (project != null) {
						try {
							fType= project.findType(SignatureUtil.stripSignatureToFQN(String.valueOf(expectedTypes[0])));
						} catch (JavaScriptModelException x) {
							JavaScriptPlugin.log(x);
						}
					}
				}
			}
		}
		return fType;
	}
	
	/**
	 * Returns a label provider that can be used to compute proposal labels.
	 * 
	 * @return a label provider that can be used to compute proposal labels
	 */
	public CompletionProposalLabelProvider getLabelProvider() {
		if (fLabelProvider == null) {
			if (fCollector != null)
				fLabelProvider= fCollector.getLabelProvider();
			else
				fLabelProvider= new CompletionProposalLabelProvider();
		}

		return fLabelProvider;
	}
	
	/**
	 * Sets the collector, which is used to access the compilation unit, the core context and the
	 * label provider. This is a performance optimization: {@link IJavaCompletionProposalComputer}s
	 * may instantiate a {@link CompletionProposalCollector} and set this invocation context via
	 * {@link CompletionProposalCollector#setInvocationContext(JavaContentAssistInvocationContext)},
	 * which in turn calls this method. This allows the invocation context to retrieve the core
	 * context and keyword proposals from the existing collector, instead of computing theses values
	 * itself via {@link #computeKeywordsAndContext()}.
	 * 
	 * @param collector the collector
	 */
	protected void setCollector(CompletionProposalCollector collector) {
		fCollector= collector;
	}
	
	/**
	 * Fallback to retrieve a core context and keyword proposals when no collector is available.
	 * Runs code completion on the cu and collects keyword proposals. {@link #fKeywordProposals} is
	 * non-<code>null</code> after this call.
	 * 
	 * 
	 */
	private void computeKeywordsAndContext() {
		IJavaScriptUnit cu= getCompilationUnit();
		if (cu == null) {
			if (fKeywordProposals == null)
				fKeywordProposals= new IJavaCompletionProposal[0];
			return;
		}
		
		CompletionProposalCollector collector= new CompletionProposalCollector(cu);
		collector.setIgnored(CompletionProposal.KEYWORD, false);
		collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
		collector.setIgnored(CompletionProposal.FIELD_REF, true);
		collector.setIgnored(CompletionProposal.LABEL_REF, true);
		collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);
		collector.setIgnored(CompletionProposal.METHOD_DECLARATION, true);
		collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
		collector.setIgnored(CompletionProposal.METHOD_REF, true);
		collector.setIgnored(CompletionProposal.PACKAGE_REF, true);
		collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
		collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
		collector.setIgnored(CompletionProposal.JSDOC_BLOCK_TAG, true);
		collector.setIgnored(CompletionProposal.JSDOC_FIELD_REF, true);
		collector.setIgnored(CompletionProposal.JSDOC_INLINE_TAG, true);
		collector.setIgnored(CompletionProposal.JSDOC_METHOD_REF, true);
		collector.setIgnored(CompletionProposal.JSDOC_PARAM_REF, true);
		collector.setIgnored(CompletionProposal.JSDOC_TYPE_REF, true);
		collector.setIgnored(CompletionProposal.TYPE_REF, true);
		
		try {
			cu.codeComplete(getInvocationOffset(), collector);
			if (fCoreContext == null)
				fCoreContext= collector.getContext();
			if (fKeywordProposals == null)
				fKeywordProposals= collector.getKeywordCompletionProposals();
			if (fLabelProvider == null)
				fLabelProvider= collector.getLabelProvider();
		} catch (JavaScriptModelException x) {
			JavaScriptPlugin.log(x);
			if (fKeywordProposals == null)
				fKeywordProposals= new IJavaCompletionProposal[0];
		}
	}
	
	/*
	 * Implementation note: There is no need to override hashCode and equals, as we only add cached
	 * values shared across one assist invocation.
	 */
}
