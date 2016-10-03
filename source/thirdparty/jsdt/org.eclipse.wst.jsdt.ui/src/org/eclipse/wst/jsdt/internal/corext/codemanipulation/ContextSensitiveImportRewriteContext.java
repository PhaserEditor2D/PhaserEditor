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
import java.util.Iterator;
import java.util.List;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class ContextSensitiveImportRewriteContext extends ImportRewriteContext {
	
	private final JavaScriptUnit fCompilationUnit;
	private final int fPosition;
	private IBinding[] fDeclarationsInScope;
	private Name[] fImportedNames;
	private final ImportRewrite fImportRewrite;
	
	public ContextSensitiveImportRewriteContext(JavaScriptUnit compilationUnit, int position, ImportRewrite importRewrite) {
		fCompilationUnit= compilationUnit;
		fPosition= position;
		fImportRewrite= importRewrite;
		fDeclarationsInScope= null;
		fImportedNames= null;
	}

	public int findInContext(String qualifier, String name, int kind) {
		int defaultResult= fImportRewrite.getDefaultImportRewriteContext().findInContext(qualifier, name, kind);
		if (defaultResult != ImportRewriteContext.RES_NAME_UNKNOWN)
			return defaultResult;
		
		if (fImportRewrite.isImportMatchesType()) {
			IBinding[] declarationsInScope = getDeclarationsInScope();
			for (int i = 0; i < declarationsInScope.length; i++) {
				if (declarationsInScope[i] instanceof ITypeBinding) {
					ITypeBinding typeBinding = (ITypeBinding) declarationsInScope[i];
					if (isSameType(typeBinding, qualifier, name)) {
						return RES_NAME_FOUND;
					} else if (isConflicting(typeBinding, name)) {
						return RES_NAME_CONFLICT;
					}
				} else if (declarationsInScope[i] != null) {
					if (isConflicting(declarationsInScope[i], name)) {
						return RES_NAME_CONFLICT;
					}
				}
			}
			Name[] names = getImportedNames();
			for (int i = 0; i < names.length; i++) {
				IBinding binding = names[i].resolveBinding();
				if (binding instanceof ITypeBinding) {
					ITypeBinding typeBinding = (ITypeBinding) binding;
					if (isConflictingType(typeBinding, qualifier, name)) {
						return RES_NAME_CONFLICT;
					}
				}
			}
			List list = fCompilationUnit.types();
			for (Iterator iter = list.iterator(); iter.hasNext();) {
				AbstractTypeDeclaration type = (AbstractTypeDeclaration) iter
						.next();
				ITypeBinding binding = type.resolveBinding();
				if (binding != null) {
					if (isSameType(binding, qualifier, name)) {
						return RES_NAME_FOUND;
					} else {
						ITypeBinding decl = containingDeclaration(binding,
								qualifier, name);
						while (decl != null && !decl.equals(binding)) {
							int modifiers = decl.getModifiers();
							if (Modifier.isPrivate(modifiers))
								return RES_NAME_CONFLICT;
							decl = decl.getDeclaringClass();
						}
					}
				}
			}
		}
		String[] addedImports= fImportRewrite.getAddedImports();
		String qualifiedName= JavaModelUtil.concatenateName(qualifier, name);
		for (int i= 0; i < addedImports.length; i++) {
			String addedImport= addedImports[i];
			if (qualifiedName.equals(addedImport)) {
				return RES_NAME_FOUND;
			} else {
				if (isConflicting(name, addedImport))
					return RES_NAME_CONFLICT;
			}
		}
		
//		if (qualifier.equals("java.lang")) { //$NON-NLS-1$
//			//No explicit import statement required
//			IJavaScriptElement parent= fCompilationUnit.getJavaElement().getParent();
//			if (parent instanceof IPackageFragment) {
//				IPackageFragment packageFragment= (IPackageFragment)parent;
//				try {
//					IJavaScriptUnit[] compilationUnits= packageFragment.getCompilationUnits();
//					for (int i= 0; i < compilationUnits.length; i++) {
//						IJavaScriptUnit cu= compilationUnits[i];
//						IType[] allTypes= cu.getAllTypes();
//						for (int j= 0; j < allTypes.length; j++) {
//							IType type= allTypes[j];
//							String packageTypeName= type.getFullyQualifiedName();
//							if (isConflicting(name, packageTypeName))
//								return RES_NAME_CONFLICT;
//						}
//					}
//				} catch (JavaScriptModelException e) {
//				}
//			}
//		}
		
		return RES_NAME_UNKNOWN;
	}

	private boolean isConflicting(String name, String importt) {
		int index= importt.lastIndexOf('.');
		String importedName;
		if (index == -1) {
			importedName= importt;
		} else {
			importedName= importt.substring(index + 1, importt.length());
		}
		if (importedName.equals(name)) {
			return true;
		}
		return false;
	}
	
	private ITypeBinding containingDeclaration(ITypeBinding binding, String qualifier, String name) {
		ITypeBinding[] declaredTypes= binding.getDeclaredTypes();
		for (int i= 0; i < declaredTypes.length; i++) {
			ITypeBinding childBinding= declaredTypes[i];
			if (isSameType(childBinding, qualifier, name)) {
				return childBinding;
			} else {
				ITypeBinding result= containingDeclaration(childBinding, qualifier, name);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	private boolean isConflicting(IBinding binding, String name) {
		return binding.getName().equals(name);
	}

	private boolean isSameType(ITypeBinding binding, String qualifier, String name) {
		String qualifiedName= JavaModelUtil.concatenateName(qualifier, name);
		return binding.getQualifiedName().equals(qualifiedName);
	}
	
	private boolean isConflictingType(ITypeBinding binding, String qualifier, String name) {
		binding= binding.getTypeDeclaration();
		return !isSameType(binding, qualifier, name) && isConflicting(binding, name);
	}
	
	private IBinding[] getDeclarationsInScope() {
		if (fDeclarationsInScope == null) {
			ScopeAnalyzer analyzer= new ScopeAnalyzer(fCompilationUnit);
			fDeclarationsInScope= analyzer.getDeclarationsInScope(fPosition, ScopeAnalyzer.METHODS | ScopeAnalyzer.TYPES | ScopeAnalyzer.VARIABLES);
		}
		return fDeclarationsInScope;
	}
	
	private Name[] getImportedNames() {
		if (fImportedNames == null) {
			IJavaScriptProject project= null;
			IJavaScriptElement javaElement= fCompilationUnit.getJavaElement();
			if (javaElement != null)
				project= javaElement.getJavaScriptProject();
			
			List imports= new ArrayList();
			ImportReferencesCollector.collect(fCompilationUnit, project, null, imports, null);
			fImportedNames= (Name[])imports.toArray(new Name[imports.size()]);
		}
		return fImportedNames;
	}
}
