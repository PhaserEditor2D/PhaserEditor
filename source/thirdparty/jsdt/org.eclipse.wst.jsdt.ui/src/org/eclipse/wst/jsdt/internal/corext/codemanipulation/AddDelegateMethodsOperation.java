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
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public final class AddDelegateMethodsOperation implements IWorkspaceRunnable {

	/** Should the resulting edit be applied? */
	private boolean fApply= true;

	/** The method binding keys for which a method was generated */
	private final List fCreated= new ArrayList();

	/** The resulting text edit */
	private TextEdit fEdit= null;

	/** The insertion point, or <code>null</code> */
	private final IJavaScriptElement fInsert;

	/** The method binding keys to implement */
	private final String[] fMethodKeys;

	/** Should the compilation unit content be saved? */
	private final boolean fSave;

	/** The code generation settings to use */
	private final CodeGenerationSettings fSettings;

	/** The type declaration to add the methods to */
	private final IType fType;

	/** The compilation unit ast node */
	private final JavaScriptUnit fUnit;

	/** The variable binding keys to implement */
	private final String[] fVariableKeys;

	/**
	 * Creates a new add delegate methods operation.
	 * 
	 * @param type the type to add the methods to
	 * @param insert the insertion point, or <code>null</code>
	 * @param variableKeys the variable binding keys to implement
	 * @param methodKeys the method binding keys to implement
	 * @param settings the code generation settings to use
	 * @param apply <code>true</code> if the resulting edit should be applied, <code>false</code> otherwise
	 * @param save <code>true</code> if the changed compilation unit should be saved, <code>false</code> otherwise
	 */
	public AddDelegateMethodsOperation(final IType type, final IJavaScriptElement insert, final JavaScriptUnit unit, final String[] variableKeys, final String[] methodKeys, final CodeGenerationSettings settings, final boolean apply, final boolean save) {
		Assert.isNotNull(type);
		Assert.isNotNull(unit);
		Assert.isNotNull(variableKeys);
		Assert.isNotNull(methodKeys);
		Assert.isNotNull(settings);
		Assert.isTrue(variableKeys.length == methodKeys.length);
		fType= type;
		fInsert= insert;
		fUnit= unit;
		fVariableKeys= variableKeys;
		fMethodKeys= methodKeys;
		fSettings= settings;
		fSave= save;
		fApply= apply;
	}

	/**
	 * Returns the method binding keys for which a method has been generated.
	 * 
	 * @return the method binding keys
	 */
	public final String[] getCreatedMethods() {
		final String[] keys= new String[fCreated.size()];
		fCreated.toArray(keys);
		return keys;
	}

	/**
	 * Returns the resulting text edit.
	 * 
	 * @return the resulting text edit
	 */
	public final TextEdit getResultingEdit() {
		return fEdit;
	}

	/**
	 * Returns the scheduling rule for this operation.
	 * 
	 * @return the scheduling rule
	 */
	public final ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/*
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(CodeGenerationMessages.AddDelegateMethodsOperation_monitor_message);
			fCreated.clear();
			final IJavaScriptUnit unit= fType.getJavaScriptUnit();
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(unit, fUnit);
			ITypeBinding binding= null;
			ListRewrite rewriter= null;
			if (fType.isAnonymous()) {
					final ClassInstanceCreation creation= (ClassInstanceCreation) ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), fType.getNameRange()), ClassInstanceCreation.class);
					if (creation != null) {
						binding= creation.resolveTypeBinding();
						final AnonymousClassDeclaration declaration= creation.getAnonymousClassDeclaration();
						if (declaration != null)
							rewriter= rewrite.getASTRewrite().getListRewrite(declaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
					}
			} else {
				final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), fType.getNameRange()), AbstractTypeDeclaration.class);
				if (declaration != null) {
					binding= declaration.resolveBinding();
					rewriter= rewrite.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
				}
			}
			if (binding != null && rewriter != null) {
				final IBinding[][] bindings= StubUtility2.getDelegatableMethods(rewrite.getAST(), binding);
				if (bindings != null && bindings.length > 0) {
					ITextFileBuffer buffer= null;
					IDocument document= null;
					try {
						if (!JavaModelUtil.isPrimary(unit))
							document= new Document(unit.getBuffer().getContents());
						else {
							buffer= RefactoringFileBuffers.acquire(unit);
							document= buffer.getDocument();
						}
						ASTNode insertion= null;
						if (fInsert instanceof IFunction)
							insertion= ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), ((IFunction) fInsert).getNameRange()), FunctionDeclaration.class);
						String variableKey= null;
						String methodKey= null;
						FunctionDeclaration stub= null;
						for (int index= 0; index < fMethodKeys.length; index++) {
							methodKey= fMethodKeys[index];
							variableKey= fVariableKeys[index];
							if (monitor.isCanceled())
								break;
							for (int offset= 0; offset < bindings.length; offset++) {
								if (bindings[offset][0].getKey().equals(variableKey) && bindings[offset][1].getKey().equals(methodKey)) {
									stub= StubUtility2.createDelegationStub(rewrite.getCu(), rewrite.getASTRewrite(), rewrite.getImportRewrite(), rewrite.getAST(), bindings[offset], fSettings);
									if (stub != null) {
										fCreated.add(methodKey);
										if (insertion != null)
											rewriter.insertBefore(stub, insertion, null);
										else
											rewriter.insertLast(stub, null);
									}
									break;
								}
							}
						}
						final Change result= rewrite.createChange();
						if (result instanceof CompilationUnitChange) {
							final CompilationUnitChange change= (CompilationUnitChange) result;
							final TextEdit edit= change.getEdit();
							if (edit != null) {
								try {
									fEdit= edit;
									if (fApply)
										edit.apply(document, TextEdit.UPDATE_REGIONS);
									if (fSave) {
										if (buffer != null)
											buffer.commit(new SubProgressMonitor(monitor, 1), true);
										else
											unit.getBuffer().setContents(document.get());
									}
								} catch (Exception exception) {
									throw new CoreException(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), 0, exception.getLocalizedMessage(), exception));
								}
							}
						}
					} finally {
						if (buffer != null)
							RefactoringFileBuffers.release(unit);
					}
				}
			}
		} finally {
			monitor.done();
		}
	}
}
