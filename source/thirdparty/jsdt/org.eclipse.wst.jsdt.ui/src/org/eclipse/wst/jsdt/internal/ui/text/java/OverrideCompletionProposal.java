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
package org.eclipse.wst.jsdt.internal.ui.text.java;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.formatter.IndentManipulation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;

public class OverrideCompletionProposal extends JavaTypeCompletionProposal implements ICompletionProposalExtension4 {

	private IJavaScriptProject fJavaProject;
	private String fMethodName;
	private String[] fParamTypes;

	public OverrideCompletionProposal(IJavaScriptProject jproject, IJavaScriptUnit cu, String methodName, String[] paramTypes, int start, int length, String displayName, String completionProposal) {
		super(completionProposal, cu, start, length, null, displayName, 0);
		Assert.isNotNull(jproject);
		Assert.isNotNull(methodName);
		Assert.isNotNull(paramTypes);
		Assert.isNotNull(cu);

		fParamTypes= paramTypes;
		fMethodName= methodName;

		fJavaProject= jproject;
		
		StringBuffer buffer= new StringBuffer();
		buffer.append(completionProposal);
		buffer.append(" {};"); //$NON-NLS-1$
		
		setReplacementString(buffer.toString());
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getPrefixCompletionText(org.eclipse.jface.text.IDocument,int)
	 */
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return fMethodName;
	}

	/*
	 * @see JavaTypeCompletionProposal#updateReplacementString(IDocument,char,int,ImportRewrite)
	 */
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportRewrite importRewrite) throws CoreException, BadLocationException {
		final IDocument buffer= new Document(document.get());
		int index= offset - 1;
		while (index >= 0 && Character.isJavaIdentifierPart(buffer.getChar(index)))
			index--;
		final int length= offset - index - 1;
		buffer.replace(index + 1, length, " "); //$NON-NLS-1$
		final ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(true);
		parser.setSource(buffer.get().toCharArray());
		parser.setUnitName(fCompilationUnit.getResource().getFullPath().toString());
		parser.setProject(fCompilationUnit.getJavaScriptProject());
		final JavaScriptUnit unit= (JavaScriptUnit) parser.createAST(new NullProgressMonitor());
		ITypeBinding binding= null;
		ChildListPropertyDescriptor descriptor= null;
		ASTNode node= NodeFinder.perform(unit, index + 1, 0);
		if (node instanceof AnonymousClassDeclaration) {
			switch (node.getParent().getNodeType()) {
				case ASTNode.CLASS_INSTANCE_CREATION:
					binding= ((ClassInstanceCreation) node.getParent()).resolveTypeBinding();
					break;
			}
			descriptor= AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
		} else if (node instanceof AbstractTypeDeclaration) {
			final AbstractTypeDeclaration declaration= ((AbstractTypeDeclaration) node);
			descriptor= declaration.getBodyDeclarationsProperty();
			binding= declaration.resolveBinding();
		}
		if (binding != null) {
			ASTRewrite rewrite= ASTRewrite.create(unit.getAST());
			IFunctionBinding[] bindings= StubUtility2.getOverridableMethods(rewrite.getAST(), binding, true);
			if (bindings != null && bindings.length > 0) {
				List candidates= new ArrayList(bindings.length);
				IFunctionBinding method= null;
				for (index= 0; index < bindings.length; index++) {
					if (bindings[index].getName().equals(fMethodName) && bindings[index].getParameterTypes().length == fParamTypes.length)
						candidates.add(bindings[index]);
				}
				if (candidates.size() > 1) {
					method= Bindings.findMethodInHierarchy(binding, fMethodName, fParamTypes);
					if (method == null) {
						ITypeBinding objectType= rewrite.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
						method= Bindings.findMethodInType(objectType, fMethodName, fParamTypes);
					}
				} else if (candidates.size() == 1)
					method= (IFunctionBinding) candidates.get(0);
				if (method != null) {
					CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fJavaProject);
					ListRewrite rewriter= rewrite.getListRewrite(node, descriptor);
					String key= method.getKey();
					FunctionDeclaration stub= null;
					for (index= 0; index < bindings.length; index++) {
						if (key.equals(bindings[index].getKey())) {
							stub= StubUtility2.createImplementationStub(fCompilationUnit, rewrite, importRewrite, bindings[index], binding.getName(), false, settings);
							if (stub != null)
								rewriter.insertFirst(stub, null);
							break;
						}
					}
					if (stub != null) {
						IDocument contents= new Document(fCompilationUnit.getBuffer().getContents());
						IRegion region= contents.getLineInformationOfOffset(getReplacementOffset());
						ITrackedNodePosition position= rewrite.track(stub);
						String indent= IndentManipulation.extractIndentString(contents.get(region.getOffset(), region.getLength()), settings.tabWidth, settings.indentWidth);
						try {
							rewrite.rewriteAST(contents, fJavaProject.getOptions(true)).apply(contents, TextEdit.UPDATE_REGIONS);
						} catch (MalformedTreeException exception) {
							JavaScriptPlugin.log(exception);
						} catch (BadLocationException exception) {
							JavaScriptPlugin.log(exception);
						}
						setReplacementString(IndentManipulation.changeIndent(Strings.trimIndentation(contents.get(position.getStartPosition(), position.getLength()), settings.tabWidth, settings.indentWidth, false), 0, settings.tabWidth, settings.indentWidth, indent, TextUtilities.getDefaultLineDelimiter(contents)));
					}
				}
			}
		}
		return true;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	public boolean isAutoInsertable() {
		return false;
	}
}
