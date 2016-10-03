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
package org.eclipse.wst.jsdt.internal.ui.util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.typehierarchy.TypeHierarchyViewPart;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class OpenTypeHierarchyUtil {
	
	private OpenTypeHierarchyUtil() {
	}

	public static TypeHierarchyViewPart open(IJavaScriptElement element, IWorkbenchWindow window) {
		IJavaScriptElement[] candidates= getCandidates(element);
		if (candidates != null) {
			return open(candidates, window);
		}
		return null;
	}	
	
	public static TypeHierarchyViewPart open(IJavaScriptElement[] candidates, IWorkbenchWindow window) {
		Assert.isTrue(candidates != null && candidates.length != 0);
			
		IJavaScriptElement input= null;
		if (candidates.length > 1) {
			String title= JavaUIMessages.OpenTypeHierarchyUtil_selectionDialog_title;  
			String message= JavaUIMessages.OpenTypeHierarchyUtil_selectionDialog_message; 
			input= SelectionConverter.selectJavaElement(candidates, window.getShell(), title, message);			
		} else {
			input= candidates[0];
		}
		if (input == null)
			return null;

			
		return openInViewPart(window, input);
	}

	private static TypeHierarchyViewPart openInViewPart(IWorkbenchWindow window, IJavaScriptElement input) {
		IWorkbenchPage page= window.getActivePage();
		try {
			TypeHierarchyViewPart result= (TypeHierarchyViewPart) page.findView(JavaScriptUI.ID_TYPE_HIERARCHY);
			if (result != null) {
				result.clearNeededRefresh(); // avoid refresh of old hierarchy on 'becomes visible'
			}
			result= (TypeHierarchyViewPart) page.showView(JavaScriptUI.ID_TYPE_HIERARCHY);
			result.setInputElement(input);
			return result;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, window.getShell(), 
				JavaUIMessages.OpenTypeHierarchyUtil_error_open_view, e.getMessage()); 
		}
		return null;		
	}

	/**
	 * Converts the input to a possible input candidates
	 */	
	public static IJavaScriptElement[] getCandidates(Object input) {
		if (!(input instanceof IJavaScriptElement)) {
			return null;
		}
		try {
			IJavaScriptElement elem= (IJavaScriptElement) input;
			switch (elem.getElementType()) {
				case IJavaScriptElement.INITIALIZER:
				case IJavaScriptElement.METHOD:
				case IJavaScriptElement.FIELD:
				case IJavaScriptElement.TYPE:
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
					return new IJavaScriptElement[] { elem };
				case IJavaScriptElement.PACKAGE_FRAGMENT:
					if (((IPackageFragment)elem).containsJavaResources())
						return new IJavaScriptElement[] {elem};
					break;
				case IJavaScriptElement.IMPORT_DECLARATION:	
					IImportDeclaration decl= (IImportDeclaration) elem;
					if (decl.isOnDemand()) {
						elem= JavaModelUtil.findTypeContainer(elem.getJavaScriptProject(), Signature.getQualifier(elem.getElementName()));
					} else {
						elem= elem.getJavaScriptProject().findType(elem.getElementName());
					}
					if (elem == null)
						return null;
					return new IJavaScriptElement[] {elem};
					
				case IJavaScriptElement.CLASS_FILE:
					return new IJavaScriptElement[] { ((IClassFile)input).getType() };				
				case IJavaScriptElement.JAVASCRIPT_UNIT: {
					IJavaScriptUnit cu= (IJavaScriptUnit) elem.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
					if (cu != null) {
						IType[] types= cu.getTypes();
						if (types.length > 0) {
							return types;
						}
					}
					break;
				}					
				default:
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
		return null;	
	}
}
