/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;

/**
  * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
class NodeSearcher extends ASTVisitor {
	public org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode found;
	public TypeDeclaration enclosingType;
	public int position;

	NodeSearcher(int position) {
		this.position = position;
	}

	public boolean visit(
		ConstructorDeclaration constructorDeclaration,
		ClassScope scope) {

		if (constructorDeclaration.declarationSourceStart <= position
			&& position <= constructorDeclaration.declarationSourceEnd) {
				found = constructorDeclaration;
				return false;
		}
		return true;
	}

	public boolean visit(
		FieldDeclaration fieldDeclaration,
		MethodScope scope) {
			if (fieldDeclaration.declarationSourceStart <= position
				&& position <= fieldDeclaration.declarationSourceEnd) {
					found = fieldDeclaration;
					return false;
			}
			return true;
	}

	public boolean visit(Initializer initializer, MethodScope scope) {
		if (initializer.declarationSourceStart <= position
			&& position <= initializer.declarationSourceEnd) {
				found = initializer;
				return false;
		}
		return true;
	}

	public boolean visit(
		TypeDeclaration memberTypeDeclaration,
		ClassScope scope) {
			if (memberTypeDeclaration.declarationSourceStart <= position
				&& position <= memberTypeDeclaration.declarationSourceEnd) {
					enclosingType = memberTypeDeclaration;
					return true;

			}
			return false;
	}

	public boolean visit(
		MethodDeclaration methodDeclaration,
		Scope scope) {

		if (methodDeclaration.declarationSourceStart <= position
			&& position <= methodDeclaration.declarationSourceEnd) {
				found = methodDeclaration;
				return false;
		}
		return true;
	}

	public boolean visit(
		TypeDeclaration typeDeclaration,
		CompilationUnitScope scope) {
			if (typeDeclaration.declarationSourceStart <= position
				&& position <= typeDeclaration.declarationSourceEnd) {
					enclosingType = typeDeclaration;
					return true;
			}
			return false;
	}

}
