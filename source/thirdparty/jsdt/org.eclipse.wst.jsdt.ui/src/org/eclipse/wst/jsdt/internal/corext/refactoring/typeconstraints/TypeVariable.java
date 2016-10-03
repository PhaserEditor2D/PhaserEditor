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
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;

public final class TypeVariable extends ConstraintVariable {

	private final String fSource;
	private final CompilationUnitRange fTypeRange;
	
	public TypeVariable(Type type){
		super(type.resolveBinding());
		fSource= type.toString();
		IJavaScriptUnit cu= ASTCreator.getCu(type);
		Assert.isNotNull(cu);
		fTypeRange= new CompilationUnitRange(cu, ASTNodes.getElementType(type));
	}

	public TypeVariable(ITypeBinding binding, String source, CompilationUnitRange range){
		super(binding);
		fSource= source;
		fTypeRange= range;
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fSource;
	}

	public CompilationUnitRange getCompilationUnitRange() {
		return fTypeRange;
	}
}
