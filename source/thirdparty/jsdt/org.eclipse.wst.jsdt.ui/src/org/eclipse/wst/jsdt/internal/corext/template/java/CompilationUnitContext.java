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
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.text.template.contentassist.MultiVariableGuess;


/**
 * A compilation unit context.
 */
public abstract class CompilationUnitContext extends DocumentTemplateContext {

	/** The compilation unit, may be <code>null</code>. */
	private final IJavaScriptUnit fCompilationUnit;
	/** A flag to force evaluation in head-less mode. */
	protected boolean fForceEvaluation;
	/** A global state for proposals that change if a master proposal changes. */
	protected MultiVariableGuess fMultiVariableGuess;
	/** <code>true</code> if the context has a managed (i.e. added to the document) position, <code>false</code> otherwise. */
	protected final boolean fIsManaged;

	/**
	 * Creates a compilation unit context.
	 * 
	 * @param type   the context type
	 * @param document the document
	 * @param completionOffset the completion position within the document
	 * @param completionLength the completion length within the document
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 */
	protected CompilationUnitContext(TemplateContextType type, IDocument document, int completionOffset, int completionLength, IJavaScriptUnit compilationUnit) {
		super(type, document, completionOffset, completionLength);
		fCompilationUnit= compilationUnit;
		fIsManaged= false;
	}
	
	/**
	 * Creates a compilation unit context.
	 * 
	 * @param type   the context type
	 * @param document the document
	 * @param completionPosition the position defining the completion offset and length 
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 * 
	 */
	protected CompilationUnitContext(TemplateContextType type, IDocument document, Position completionPosition, IJavaScriptUnit compilationUnit) {
		super(type, document, completionPosition);
		fCompilationUnit= compilationUnit;
		fIsManaged= true;
	}
	
	/**
	 * Returns the compilation unit if one is associated with this context,
	 * <code>null</code> otherwise.
	 * 
	 * @return the compilation unit of this context or <code>null</code>
	 */
	public final IJavaScriptUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Returns the enclosing element of a particular element type,
	 * <code>null</code> if no enclosing element of that type exists.
	 * 
	 * @param elementType the element type
	 * @return the enclosing element of the given type or <code>null</code>
	 */
	public IJavaScriptElement findEnclosingElement(int elementType) {
		if (fCompilationUnit == null)
			return null;

		try {
			IJavaScriptElement element= fCompilationUnit.getElementAt(getStart());
			if (element == null) {
				element= fCompilationUnit;
			}
			
			return element.getAncestor(elementType);

		} catch (JavaScriptModelException e) {
			return null;
		}	
	}

	/**
	 * Sets whether evaluation is forced or not.
	 * 
	 * @param evaluate <code>true</code> in order to force evaluation,
	 *            <code>false</code> otherwise
	 */
	public void setForceEvaluation(boolean evaluate) {
		fForceEvaluation= evaluate;	
	}
	
	/**
	 * Returns the multi-variable guess.
	 * 
	 * @return the multi-variable guess
	 */
	public MultiVariableGuess getMultiVariableGuess() {
		return fMultiVariableGuess;
	}

	/**
	 * @param multiVariableGuess The multiVariableGuess to set.
	 */
	void setMultiVariableGuess(MultiVariableGuess multiVariableGuess) {
		fMultiVariableGuess= multiVariableGuess;
	}
	
	protected IJavaScriptProject getJavaProject() {
		IJavaScriptUnit compilationUnit= getCompilationUnit();
		IJavaScriptProject project= compilationUnit == null ? null : compilationUnit.getJavaScriptProject();
		return project;
	}	
}
