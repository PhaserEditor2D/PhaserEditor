/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IImportContainer;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;

public class GenericRefactoringHandleTransplanter {

	public final IJavaScriptElement transplantHandle(IJavaScriptElement element) {
		IJavaScriptElement parent= element.getParent();
		if (parent != null)
			parent= transplantHandle(parent); // recursive
		
		switch (element.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_MODEL:
				return transplantHandle((IJavaScriptModel) element);
				
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				return transplantHandle((IJavaScriptProject) element);
				
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				return transplantHandle((IJavaScriptProject) parent, (IPackageFragmentRoot) element);
				
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				return transplantHandle((IPackageFragmentRoot) parent, (IPackageFragment) element);
				
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return transplantHandle((IPackageFragment) parent, (IJavaScriptUnit) element);
				
			case IJavaScriptElement.CLASS_FILE:
				return transplantHandle((IPackageFragment) parent, (IClassFile) element);
				
			case IJavaScriptElement.TYPE:
				return transplantHandle(parent, (IType) element);
				
			case IJavaScriptElement.FIELD:
				return transplantHandle((IType) parent, (IField) element);
				
			case IJavaScriptElement.METHOD:
				return transplantHandle((IType) parent, (IFunction) element);
				
			case IJavaScriptElement.INITIALIZER:
				return transplantHandle((IType) parent, (IInitializer) element);
				
			case IJavaScriptElement.IMPORT_CONTAINER:
				return transplantHandle((IJavaScriptUnit) parent, (IImportContainer) element);
				
			case IJavaScriptElement.IMPORT_DECLARATION:
				return transplantHandle((IImportContainer) parent, (IImportDeclaration) element);
				
			case IJavaScriptElement.LOCAL_VARIABLE:
				return transplantHandle((ILocalVariable) element);
				
			default:
				throw new IllegalArgumentException(element.toString());
		}
		
	}

	protected IJavaScriptModel transplantHandle(IJavaScriptModel element) {
		return element;
	}
	
	protected IJavaScriptProject transplantHandle(IJavaScriptProject element) {
		return element;
	}
	
	protected IPackageFragmentRoot transplantHandle(IJavaScriptProject parent, IPackageFragmentRoot element) {
		return element;
	}
	
	protected IPackageFragment transplantHandle(IPackageFragmentRoot parent, IPackageFragment element) {
		return parent.getPackageFragment(element.getElementName());
	}
	
	protected IJavaScriptUnit transplantHandle(IPackageFragment parent, IJavaScriptUnit element) {
		return parent.getJavaScriptUnit(element.getElementName());
	}
	
	protected IClassFile transplantHandle(IPackageFragment parent, IClassFile element) {
		return parent.getClassFile(element.getElementName());
	}
	
	protected IType transplantHandle(IJavaScriptElement parent, IType element) {
		switch (parent.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return ((IJavaScriptUnit) parent).getType(element.getElementName());
			case IJavaScriptElement.CLASS_FILE:
				return ((IClassFile) parent).getType();
			case IJavaScriptElement.METHOD:
				return ((IFunction) parent).getType(element.getElementName(), element.getOccurrenceCount());
			case IJavaScriptElement.FIELD:
				return ((IField) parent).getType(element.getElementName(), element.getOccurrenceCount());
			case IJavaScriptElement.INITIALIZER:
				return ((IInitializer) parent).getType(element.getElementName(), element.getOccurrenceCount());
			case IJavaScriptElement.TYPE:
				return ((IType) parent).getType(element.getElementName(), element.getOccurrenceCount());
			default:
				throw new IllegalStateException(element.toString());
		}
	}
	
	protected IField transplantHandle(IType parent, IField element) {
		return parent.getField(element.getElementName());
	}
	
	protected IFunction transplantHandle(IType parent, IFunction element) {
		return parent.getFunction(element.getElementName(), element.getParameterTypes());
	}
	
	protected IInitializer transplantHandle(IType parent, IInitializer element) {
		return parent.getInitializer(element.getOccurrenceCount());
	}
	
	protected IImportContainer transplantHandle(IJavaScriptUnit parent, IImportContainer element) {
		return parent.getImportContainer();
	}
	
	protected IImportDeclaration transplantHandle(IImportContainer parent, IImportDeclaration element) {
		return parent.getImport(element.getElementName());
	}
	
	protected ILocalVariable transplantHandle(ILocalVariable element) {
		return element; // can't get from parent!
	}
}
