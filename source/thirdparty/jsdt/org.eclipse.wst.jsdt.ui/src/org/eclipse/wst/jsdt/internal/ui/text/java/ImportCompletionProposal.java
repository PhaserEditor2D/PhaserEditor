/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;


/**
 * Completion proposal for required imports.
 * 
 * 
  */
public class ImportCompletionProposal extends AbstractJavaCompletionProposal {

	private final IJavaScriptUnit fCompilationUnit;
	private final int fParentProposalKind;
	private ImportRewrite fImportRewrite;
	private ContextSensitiveImportRewriteContext fImportContext;
	private final CompletionProposal fProposal;
	private boolean fReplacementStringComputed;	
	

	public ImportCompletionProposal(CompletionProposal proposal, JavaContentAssistInvocationContext context, int parentProposalKind) {
		super(context);
		fProposal= proposal;
		fParentProposalKind= parentProposalKind;
		fCompilationUnit= context.getCompilationUnit();
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.AbstractJavaCompletionProposal#getReplacementString()
	 */
	public final String getReplacementString() {
		if (!fReplacementStringComputed)
			setReplacementString(computeReplacementString());
		return super.getReplacementString();
	}
	
	/**
	 * Computes the replacement string.
	 * 
	 * @return the replacement string
	 */
	private String computeReplacementString() {
		int proposalKind= fProposal.getKind();
		String qualifiedTypeName= null;
		char[] qualifiedType= null;
 		if (proposalKind == CompletionProposal.TYPE_IMPORT) {
 			qualifiedType= fProposal.getSignature();
 	 		qualifiedTypeName= String.valueOf(Signature.toCharArray(qualifiedType));
 		} else if (proposalKind == CompletionProposal.METHOD_IMPORT || proposalKind == CompletionProposal.FIELD_IMPORT) {
 	 		qualifiedType= fProposal.getDeclarationSignature();
 	 		qualifiedTypeName= String.valueOf(Signature.toCharArray(qualifiedType));
		} else {
			/*
			 * In 3.3 we only support the above import proposals, see
			 * CompletionProposal#getRequiredProposals()
			 */
			 Assert.isTrue(false);
		}
		
 		/* Add imports if the preference is on. */
 		fImportRewrite= createImportRewrite();
 		if (fImportRewrite != null) {
	 		if (proposalKind == CompletionProposal.TYPE_IMPORT) {
	 			String simpleType= fImportRewrite.addImport(qualifiedTypeName, qualifiedTypeName,fImportContext);
		 		if (fParentProposalKind == CompletionProposal.METHOD_REF)
		 			return simpleType + "."; //$NON-NLS-1$
 			} else
	 			fImportRewrite.addStaticImport(qualifiedTypeName, String.valueOf(fProposal.getName()), proposalKind == CompletionProposal.FIELD_IMPORT, fImportContext);
	 		return ""; //$NON-NLS-1$
	 	}		
		
		// Case where we don't have an import rewrite (see allowAddingImports)
		
		if (fCompilationUnit != null && JavaModelUtil.isImplicitImport(Signature.getQualifier(qualifiedTypeName), fCompilationUnit)) {
			/* No imports for implicit imports. */
			
			if (fProposal.getKind() == CompletionProposal.TYPE_IMPORT && fParentProposalKind == CompletionProposal.FIELD_REF)
				return ""; //$NON-NLS-1$
			qualifiedTypeName= String.valueOf(Signature.getSignatureSimpleName(qualifiedType));
		}
		
		return qualifiedTypeName + "."; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.AbstractJavaCompletionProposal#apply(org.eclipse.jface.text.IDocument, char, int)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		try {
			super.apply(document, trigger, offset);

			if (fImportRewrite != null && fImportRewrite.hasRecordedChanges()) {
				int oldLen= document.getLength();
				fImportRewrite.rewriteImports(new NullProgressMonitor()).apply(document, TextEdit.UPDATE_REGIONS);
				setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
			}
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
	}
	
	/**
	 * Creates and returns the import rewrite
	 * if imports should be added at all.
	 * 
	 * @return the import rewrite or <code>null</code> if no imports can or should be added
	 */
	private ImportRewrite createImportRewrite() {
		if (fCompilationUnit != null && shouldAddImports()) {
			try {
				JavaScriptUnit cu= getASTRoot(fCompilationUnit);
				if (cu == null) {
					ImportRewrite rewrite= StubUtility.createImportRewrite(fCompilationUnit, true);
					fImportContext= null;
					return rewrite;
				} else {
					ImportRewrite rewrite= StubUtility.createImportRewrite(cu, true);
					fImportContext= new ContextSensitiveImportRewriteContext(cu, fInvocationContext.getInvocationOffset(), rewrite);
					return rewrite;
				}
			} catch (CoreException x) {
				JavaScriptPlugin.log(x);
			}
		}
		return null;
	}

	private JavaScriptUnit getASTRoot(IJavaScriptUnit compilationUnit) {
		return JavaScriptPlugin.getDefault().getASTProvider().getAST(compilationUnit, ASTProvider.WAIT_NO, new NullProgressMonitor());
	}

	/**
	 * Returns <code>true</code> if imports should be added. The return value depends on the context
	 * and preferences only and does not take into account the contents of the compilation unit or
	 * the kind of proposal. Even if <code>true</code> is returned, there may be cases where no
	 * imports are added for the proposal. For example:
	 * <ul>
	 * <li>when completing within the import section</li>
	 * <li>when completing informal javadoc references (e.g. within <code>&lt;code&gt;</code>
	 * tags)</li>
	 * <li>when completing a type that conflicts with an existing import</li>
	 * <li>when completing an implicitly imported type (same package, <code>java.lang</code>
	 * types)</li>
	 * </ul>
	 * <p>
	 * The decision whether a qualified type or the simple type name should be inserted must take
	 * into account these different scenarios.
	 * </p>
	 * 
	 * @return <code>true</code> if imports may be added, <code>false</code> if not
	 */
	private boolean shouldAddImports() {
		if (isInJavadoc() && !isJavadocProcessingEnabled())
			return false;
		
		IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_ADDIMPORT);
	}

	/**
	 * Returns whether Javadoc processing is enabled.
	 * 
	 * @return <code>true</code> if Javadoc processing is enabled, <code>false</code> otherwise
	 */
	private boolean isJavadocProcessingEnabled() {
		IJavaScriptProject project= fCompilationUnit.getJavaScriptProject();
		boolean processJavadoc;
		if (project == null)
			processJavadoc= JavaScriptCore.ENABLED.equals(JavaScriptCore.getOption(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT));
		else
			processJavadoc= JavaScriptCore.ENABLED.equals(project.getOption(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT, true));
		return processJavadoc;
	}
}
