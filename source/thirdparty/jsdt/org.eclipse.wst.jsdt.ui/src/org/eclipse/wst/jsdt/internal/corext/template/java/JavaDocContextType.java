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
package org.eclipse.wst.jsdt.internal.corext.template.java;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;


/**
 * A context type for javadoc.
 */
public class JavaDocContextType extends CompilationUnitContextType {

	public static final String NAME= "jsdoc"; //$NON-NLS-1$

	/**
	 * Creates a java context type.
	 */
	public JavaDocContextType() {
		super(NAME);
		
		// global
		addResolver(new GlobalTemplateVariables.Cursor());
		addResolver(new GlobalTemplateVariables.LineSelection());
		addResolver(new GlobalTemplateVariables.WordSelection());
		addResolver(new GlobalTemplateVariables.Dollar());
		addResolver(new GlobalTemplateVariables.Date());
		addResolver(new GlobalTemplateVariables.Year());
		addResolver(new GlobalTemplateVariables.Time());
		addResolver(new GlobalTemplateVariables.User());
		
		// compilation unit
		addResolver(new File());
		addResolver(new PrimaryTypeName());
		addResolver(new Method());
		addResolver(new ReturnType());
		addResolver(new Arguments());
		addResolver(new Type());
		addResolver(new Package());
		addResolver(new Project());
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, int, int, org.eclipse.wst.jsdt.core.IJavaScriptUnit)
	 */
	public CompilationUnitContext createContext(IDocument document, int offset, int length, IJavaScriptUnit compilationUnit) {
		return new JavaDocContext(this, document, offset, length, compilationUnit);
	}	
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.Position, org.eclipse.wst.jsdt.core.IJavaScriptUnit)
	 */
	public CompilationUnitContext createContext(IDocument document, Position completionPosition, IJavaScriptUnit compilationUnit) {
		return new JavaDocContext(this, document, completionPosition, compilationUnit);
	}
}
