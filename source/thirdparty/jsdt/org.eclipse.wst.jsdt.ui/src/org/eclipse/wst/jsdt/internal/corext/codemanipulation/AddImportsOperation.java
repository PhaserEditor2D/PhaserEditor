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

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.TypeNameMatchCollector;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.wst.jsdt.internal.ui.text.correction.SimilarElementsRequestor;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class AddImportsOperation implements IWorkspaceRunnable {
	
	public static interface IChooseImportQuery {
		/**
		 * Selects an import from a list of choices.
		 * @param openChoices Array of found types
		 * @param containerName Name type to be imported
		 * @return Returns <code>null</code> to cancel the operation, or the
		 *         selected imports.
		 */
		TypeNameMatch chooseImport(TypeNameMatch[] openChoices, String containerName);
	}
	
	private IJavaScriptUnit fCompilationUnit;
	private final int fSelectionOffset;
	private final int fSelectionLength;
	private final IChooseImportQuery fQuery;
	private IStatus fStatus;
	private boolean fDoSave;
	
	
	/**
	 * Generate import statements for the passed java elements
	 * Elements must be of type IType (-> single import) or IPackageFragment
	 * (on-demand-import). Other JavaElements are ignored
	 * @param cu The compilation unit
	 * @param selectionOffset Start of the current text selection
	 * 	@param selectionLength End of the current text selection
	 * @param query Query element to be used for UI interaction or <code>null</code> to not select anything
	 * when multiple possibilities are available
	 * @param save If set, the result will be saved
	 */
	public AddImportsOperation(IJavaScriptUnit cu, int selectionOffset, int selectionLength, IChooseImportQuery query, boolean save) {
		super();
		Assert.isNotNull(cu);
		
		fCompilationUnit= cu;
		fSelectionOffset= selectionOffset;
		fSelectionLength= selectionLength;
		fQuery= query;
		fStatus= Status.OK_STATUS;
		fDoSave= save;
	}
	
	/**
	 * @return Returns the status.
	 */
	public IStatus getStatus() {
		return fStatus;
	}

	/**
	 * Runs the operation.
	 * @param monitor The progress monitor
	 * @throws CoreException  
	 * @throws OperationCanceledException Runtime error thrown when operation is canceled.
	 */
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(CodeGenerationMessages.AddImportsOperation_description, 4); 
			
			JavaScriptUnit astRoot= JavaScriptPlugin.getDefault().getASTProvider().getAST(fCompilationUnit, ASTProvider.WAIT_YES, new SubProgressMonitor(monitor, 1));

			ImportRewrite importRewrite= StubUtility.createImportRewrite(astRoot, true);
			
			MultiTextEdit res= new MultiTextEdit();
			
			TextEdit edit= evaluateEdits(astRoot, importRewrite, fSelectionOffset, fSelectionLength, new SubProgressMonitor(monitor, 1));
			if (edit == null) {
				return;
			}
			res.addChild(edit);
				
			TextEdit importsEdit= importRewrite.rewriteImports(new SubProgressMonitor(monitor, 1));
			res.addChild(importsEdit);
			
			JavaModelUtil.applyEdit(fCompilationUnit, res, fDoSave, new SubProgressMonitor(monitor, 1));
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		} finally {
			monitor.done();
		}
	}
	
	private TextEdit evaluateEdits(JavaScriptUnit root, ImportRewrite importRewrite, int offset, int length, IProgressMonitor monitor) throws BadLocationException, JavaScriptModelException {
		SimpleName nameNode= null;
		if (root != null) { // got an AST
			ASTNode node= NodeFinder.perform(root, offset, length);

			if (node instanceof QualifiedName) {
				nameNode= ((QualifiedName) node).getName();
			} else if (node instanceof SimpleName) {
				nameNode= (SimpleName) node;
			}
		}
		
		String name, simpleName, containerName;
		int qualifierStart;
		int simpleNameStart;
		if (nameNode != null) {
			simpleName= nameNode.getIdentifier();
			simpleNameStart= nameNode.getStartPosition();
			if (nameNode.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
				Name qualifier= ((QualifiedName) nameNode.getParent()).getQualifier();
				containerName= qualifier.getFullyQualifiedName();
				name= JavaModelUtil.concatenateName(containerName, simpleName);
				qualifierStart= qualifier.getStartPosition();
			} else if (nameNode.getParent().getLocationInParent() == QualifiedType.NAME_PROPERTY) {
				Type qualifier= ((QualifiedType) nameNode.getParent().getParent()).getQualifier();
				containerName= ASTNodes.asString(qualifier);
				name= JavaModelUtil.concatenateName(containerName, simpleName);
				qualifierStart= qualifier.getStartPosition();
			} else if (nameNode.getLocationInParent() == FunctionInvocation.NAME_PROPERTY) {
				ASTNode qualifier= ((FunctionInvocation) nameNode.getParent()).getExpression();
				if (qualifier instanceof Name) {
					containerName= ASTNodes.asString(qualifier);
					name= JavaModelUtil.concatenateName(containerName, simpleName);
					qualifierStart= qualifier.getStartPosition();
				} else {
					return null;
				}
			} else {
				containerName= ""; //$NON-NLS-1$
				name= simpleName;
				qualifierStart= simpleNameStart;
			}
			
			IBinding binding= nameNode.resolveBinding();
			if (binding != null && !binding.isRecovered()) {
				if (binding instanceof ITypeBinding) {
					ITypeBinding typeBinding= (ITypeBinding) binding;
					String qualifiedBindingName= typeBinding.getQualifiedName();
					if (containerName.length() > 0 && !qualifiedBindingName.equals(name)) {
						return null;
					}
					
					String res= importRewrite.addImport(typeBinding);
					if (containerName.length() > 0 && !res.equals(simpleName)) {
						// adding import failed
						return null;
					}
					return new ReplaceEdit(qualifierStart, simpleNameStart - qualifierStart, new String());
				} else if (binding instanceof IVariableBinding || binding instanceof IFunctionBinding) {
					boolean isField= binding instanceof IVariableBinding;
					ITypeBinding declaringClass= isField ? ((IVariableBinding) binding).getDeclaringClass() : ((IFunctionBinding) binding).getDeclaringClass();
					if (declaringClass == null) {
						return null; // variableBinding.getDeclaringClass() is null for array.length
					}
					if (Modifier.isStatic(binding.getModifiers())) {
						if (Modifier.isPrivate(declaringClass.getModifiers())) {
							fStatus= JavaUIStatus.createError(IStatus.ERROR, Messages.format(CodeGenerationMessages.AddImportsOperation_error_not_visible_class, declaringClass.getName()), null);
							return null;
						}
						
						if (containerName.length() > 0) { 
							if (containerName.equals(declaringClass.getName()) || containerName.equals(declaringClass.getQualifiedName()) ) {
								String res= importRewrite.addStaticImport(declaringClass.getQualifiedName(), binding.getName(), isField);
								if (!res.equals(simpleName)) {
									// adding import failed
									return null;
								}
								return new ReplaceEdit(qualifierStart, simpleNameStart - qualifierStart, ""); //$NON-NLS-1$
							}
						}
					}
					return null; // no static imports for packages
				} else {
					return null;
				}
			}
			if (binding != null && binding.getKind() != IBinding.TYPE) {
				// recovered binding
				return null; 
			}
			
		} else {
			IBuffer buffer= fCompilationUnit.getBuffer();
			
			qualifierStart= getNameStart(buffer, offset);
			int nameEnd= getNameEnd(buffer, offset + length);
			int len= nameEnd - qualifierStart;
			name= buffer.getText(qualifierStart, len).trim();
			if (name.length() == 0) {
				return null;
			}
			
			simpleName= Signature.getSimpleName(name);
			containerName= Signature.getQualifier(name);
			
			simpleNameStart= getSimpleNameStart(buffer, qualifierStart, containerName);
			
			int res= importRewrite.getDefaultImportRewriteContext().findInContext(containerName, simpleName, ImportRewriteContext.KIND_TYPE);
			if (res == ImportRewriteContext.RES_NAME_CONFLICT) {
				fStatus= JavaUIStatus.createError(IStatus.ERROR, CodeGenerationMessages.AddImportsOperation_error_importclash, null); 
				return null;
			} else if (res == ImportRewriteContext.RES_NAME_FOUND) {
				return new ReplaceEdit(qualifierStart, simpleNameStart - qualifierStart, ""); //$NON-NLS-1$
			}
		}
		IJavaScriptSearchScope searchScope= SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { fCompilationUnit.getJavaScriptProject() });
		
		TypeNameMatch[] types= findAllTypes(simpleName, searchScope, nameNode, new SubProgressMonitor(monitor, 1));
		if (types.length == 0) {
			fStatus= JavaUIStatus.createError(IStatus.ERROR, Messages.format(CodeGenerationMessages.AddImportsOperation_error_notresolved_message, simpleName), null); 
			return null;
		}
		
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		TypeNameMatch chosen;
		if (types.length > 1 && fQuery != null) {
			chosen= fQuery.chooseImport(types, containerName);
			if (chosen == null) {
				throw new OperationCanceledException();
			}
		} else {
			chosen= types[0];
		}
		importRewrite.addImport(chosen.getFullyQualifiedName());
		return new ReplaceEdit(qualifierStart, simpleNameStart - qualifierStart, ""); //$NON-NLS-1$
	}

	
	private int getNameStart(IBuffer buffer, int pos) throws BadLocationException {
		while (pos > 0) {
			char ch= buffer.getChar(pos - 1);
			if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
				return pos;
			}
			pos--;
		}
		return pos;
	}
	
	private int getNameEnd(IBuffer doc, int pos) throws BadLocationException {
		if (pos > 0) {
			if (Character.isWhitespace(doc.getChar(pos - 1))) {
				return pos;
			}
		}
		int len= doc.getLength();
		while (pos < len) {
			char ch= doc.getChar(pos);
			if (!Character.isJavaIdentifierPart(ch)) {
				return pos;
			}
			pos++;
		}
		return pos;
	}
	
	private int getSimpleNameStart(IBuffer buffer, int nameStart, String containerName) throws BadLocationException {
		int containerLen= containerName.length();
		int docLen= buffer.getLength();
		if ((containerLen > 0) && (nameStart + containerLen + 1 < docLen)) {
			for (int k= 0; k < containerLen; k++) {
				if (buffer.getChar(nameStart + k) != containerName.charAt(k)) {
					return nameStart;
				}
			}
			if (buffer.getChar(nameStart + containerLen) == '.') {
				return nameStart + containerLen + 1;
			}
		}
		return nameStart;
	}
	
	private int getSearchForConstant(int typeKinds) {
		final int CLASSES= SimilarElementsRequestor.CLASSES;

		switch (typeKinds & (CLASSES)) {
			case CLASSES: return IJavaScriptSearchConstants.CLASS;
			default: return IJavaScriptSearchConstants.TYPE;
		}
	}
	
	
	/*
	 * Finds a type by the simple name.
	 */
	private TypeNameMatch[] findAllTypes(String simpleTypeName, IJavaScriptSearchScope searchScope, SimpleName nameNode, IProgressMonitor monitor) throws JavaScriptModelException {
		boolean is50OrHigher= JavaModelUtil.is50OrHigher(fCompilationUnit.getJavaScriptProject());
		
		int typeKinds= SimilarElementsRequestor.ALL_TYPES;
		if (nameNode != null) {
			typeKinds= ASTResolving.getPossibleTypeKinds(nameNode, is50OrHigher);
		}
		
		ArrayList typeInfos= new ArrayList();
		TypeNameMatchCollector requestor= new TypeNameMatchCollector(typeInfos);
		int matchMode= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
		new SearchEngine().searchAllTypeNames(null, matchMode, simpleTypeName.toCharArray(), matchMode, getSearchForConstant(typeKinds), searchScope, requestor, IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);

		ArrayList typeRefsFound= new ArrayList(typeInfos.size());
		for (int i= 0, len= typeInfos.size(); i < len; i++) {
			TypeNameMatch curr= (TypeNameMatch) typeInfos.get(i);
			if (curr.getPackageName().length() > 0) { // do not suggest imports from the default package
				if (isOfKind(curr, typeKinds, is50OrHigher) && isVisible(curr)) {
					typeRefsFound.add(curr);
				}
			}
		}
		return (TypeNameMatch[]) typeRefsFound.toArray(new TypeNameMatch[typeRefsFound.size()]);
	}
	
	private boolean isOfKind(TypeNameMatch curr, int typeKinds, boolean is50OrHigher) {
		return (typeKinds & SimilarElementsRequestor.CLASSES) != 0;
	}

	
	private boolean isVisible(TypeNameMatch curr) {
		int flags= curr.getModifiers();
		if (Flags.isPrivate(flags)) {
			return false;
		}
		if (Flags.isPublic(flags)) {
			return true;
		}
		return curr.getPackageName().equals(fCompilationUnit.getParent().getElementName());
	}
	
	
	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return fCompilationUnit.getJavaScriptProject().getResource();
	}
		
}
