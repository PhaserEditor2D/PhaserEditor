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
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public final class AddUnimplementedConstructorsOperation implements IWorkspaceRunnable {

	/** Should the resulting edit be applied? */
	private final boolean fApply;

	/** The qualified names of the generated imports */
	private String[] fCreatedImports;

	/** The method binding keys for which a constructor was generated */
	private final List fCreatedMethods= new ArrayList();

	/** Should the import edits be applied? */
	private final boolean fImports;

	/** The insertion point, or <code>-1</code> */
	private final int fInsertPos;

	/** The method bindings to implement */
	private final IFunctionBinding[] fConstructorsToImplement;

	/** Should the call to the super constructor be omitted? */
	private boolean fOmitSuper;

	/** Should the compilation unit content be saved? */
	private final boolean fSave;

	/** Specified if comments should be created */
	private boolean fCreateComments;

	/** The type declaration to add the constructors to */
	private final ITypeBinding fType;

	/** The compilation unit AST node */
	private final JavaScriptUnit fASTRoot;

	/** The visibility flags of the new constructor */
	private int fVisibility;

	/**
	 * Creates a new add unimplemented constructors operation.
	 * 
	 * @param astRoot the compilation unit AST node
	 * @param type the type to add the methods to
	 * @param constructorsToImplement the method binding keys to implement
	 * @param insertPos the insertion point, or <code>-1</code>
	 * @param imports <code>true</code> if the import edits should be applied, <code>false</code> otherwise
	 * @param apply <code>true</code> if the resulting edit should be applied, <code>false</code> otherwise
	 * @param save <code>true</code> if the changed compilation unit should be saved, <code>false</code> otherwise
	 */
	public AddUnimplementedConstructorsOperation(JavaScriptUnit astRoot, ITypeBinding type, IFunctionBinding[] constructorsToImplement, int insertPos, final boolean imports, final boolean apply, final boolean save) {
		if (astRoot == null || !(astRoot.getJavaElement() instanceof IJavaScriptUnit)) {
			throw new IllegalArgumentException("AST must not be null and has to be created from a IJavaScriptUnit"); //$NON-NLS-1$
		}
		if (type == null) {
			throw new IllegalArgumentException("The type must not be null"); //$NON-NLS-1$
		}
		ASTNode node= astRoot.findDeclaringNode(type);
		if (!(node instanceof AnonymousClassDeclaration || node instanceof AbstractTypeDeclaration)) {
			throw new IllegalArgumentException("type has to map to a type declaration in the AST"); //$NON-NLS-1$
		}
		
		fType= type;
		fInsertPos= insertPos;
		fASTRoot= astRoot;
		fConstructorsToImplement= constructorsToImplement;
		fSave= save;
		fApply= apply;
		fImports= imports;
		
		fCreateComments= StubUtility.doAddComments(astRoot.getJavaElement().getJavaScriptProject());
		fVisibility= Modifier.PUBLIC;
		fOmitSuper= false;
	}

	/**
	 * Returns the method binding keys for which a constructor has been generated.
	 * 
	 * @return the method binding keys
	 */
	public String[] getCreatedConstructors() {
		final String[] keys= new String[fCreatedMethods.size()];
		fCreatedMethods.toArray(keys);
		return keys;
	}

	/**
	 * Returns the qualified names of the generated imports.
	 * 
	 * @return the generated imports
	 */
	public String[] getCreatedImports() {
		return fCreatedImports;
	}

	/**
	 * Returns the scheduling rule for this operation.
	 * 
	 * @return the scheduling rule
	 */
	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * Returns the visibility of the constructors.
	 * 
	 * @return the visibility
	 */
	public int getVisibility() {
		return fVisibility;
	}

	/**
	 * Returns whether the super call should be omitted.
	 * 
	 * @return <code>true</code> to omit the super call, <code>false</code> otherwise
	 */
	public boolean isOmitSuper() {
		return fOmitSuper;
	}

	/**
	 * Determines whether to create comments.
	 * @param comments <code>true</code> to create comments, <code>false</code> otherwise
	 */
	public void setCreateComments(final boolean comments) {
		fCreateComments= comments;
	}

	/*
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(CodeGenerationMessages.AddUnimplementedMethodsOperation_description);
			fCreatedMethods.clear();
			IJavaScriptUnit cu= (IJavaScriptUnit) fASTRoot.getJavaElement();
			
			AST ast= fASTRoot.getAST();
			
			ASTRewrite astRewrite= ASTRewrite.create(ast);
			ImportRewrite importRewrite= StubUtility.createImportRewrite(fASTRoot, true);
			
			ITypeBinding currTypeBinding= fType;
			ListRewrite memberRewriter= null;
			
			ASTNode node= fASTRoot.findDeclaringNode(currTypeBinding);
			if (node instanceof AnonymousClassDeclaration) {
				memberRewriter= astRewrite.getListRewrite(node, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
			} else if (node instanceof AbstractTypeDeclaration) {
				ChildListPropertyDescriptor property= ((AbstractTypeDeclaration) node).getBodyDeclarationsProperty();
				memberRewriter= astRewrite.getListRewrite(node, property);
			} else {
				throw new IllegalArgumentException();
				// not possible, we checked this in the constructor
			}
			
			final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu.getJavaScriptProject());
			settings.createComments= fCreateComments;

			ASTNode insertion= getNodeToInsertBefore(memberRewriter);
			
			IFunctionBinding[] toImplement= fConstructorsToImplement;
			if (toImplement == null) {
				toImplement= StubUtility2.getVisibleConstructors(currTypeBinding, true, true);
			}
			
			int deprecationCount= 0;
			for (int i= 0; i < toImplement.length; i++) {
				if (toImplement[i].isDeprecated())
					deprecationCount++;
			}
			boolean createDeprecated= deprecationCount == toImplement.length;
			for (int i= 0; i < toImplement.length; i++) {
				IFunctionBinding curr= toImplement[i];
				if (!curr.isDeprecated() || createDeprecated) {
					FunctionDeclaration stub= StubUtility2.createConstructorStub(cu, astRewrite, importRewrite, curr, currTypeBinding.getName(), fVisibility, fOmitSuper, true, settings);
					if (stub != null) {
						fCreatedMethods.add(curr.getKey());
						if (insertion != null)
							memberRewriter.insertBefore(stub, insertion, null);
						else
							memberRewriter.insertLast(stub, null);
					}
				}
			}
			MultiTextEdit edit= new MultiTextEdit();
			
			TextEdit importEdits= importRewrite.rewriteImports(new SubProgressMonitor(monitor, 1));
			fCreatedImports= importRewrite.getCreatedImports();
			if (fImports) {
				edit.addChild(importEdits);
			}
			edit.addChild(astRewrite.rewriteAST());
			
			if (fApply) {
				JavaModelUtil.applyEdit(cu, edit, fSave, new SubProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Determines whether the super call should be omitted.
	 * 
	 * @param omit <code>true</code> to omit the super call, <code>false</code> otherwise
	 */
	public void setOmitSuper(final boolean omit) {
		fOmitSuper= omit;
	}

	/**
	 * Determines the visibility of the constructors.
	 * 
	 * @param visibility the visibility
	 */
	public void setVisibility(final int visibility) {
		fVisibility= visibility;
	}
	
	private ASTNode getNodeToInsertBefore(ListRewrite rewriter) {
		if (fInsertPos != -1) {
			List members= rewriter.getOriginalList();
			for (int i= 0; i < members.size(); i++) {
				ASTNode curr= (ASTNode) members.get(i);
				if (curr.getStartPosition() >= fInsertPos) {
					return curr;
				}
			}
		}
		return null;
	}
}
