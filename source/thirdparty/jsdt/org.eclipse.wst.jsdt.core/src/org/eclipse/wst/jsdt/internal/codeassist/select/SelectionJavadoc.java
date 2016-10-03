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
package org.eclipse.wst.jsdt.internal.codeassist.select;

import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;

/**
 * Node representing a Javadoc comment including code selection.
 */
public class SelectionJavadoc extends Javadoc {

	Expression selectedNode;

	public SelectionJavadoc(int sourceStart, int sourceEnd) {
		super(sourceStart, sourceEnd);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc#print(int, java.lang.StringBuffer)
	 */
	public StringBuffer print(int indent, StringBuffer output) {
		super.print(indent, output);
		if (this.selectedNode != null) {
			String selectedString = null;
			if (this.selectedNode instanceof JavadocFieldReference) {
				JavadocFieldReference fieldRef = (JavadocFieldReference) this.selectedNode;
				if (fieldRef.methodBinding != null) {
					selectedString = "<SelectOnMethod:"; //$NON-NLS-1$
				} else {
					selectedString = "<SelectOnField:"; //$NON-NLS-1$
				}
			} else if (this.selectedNode instanceof JavadocMessageSend) {
				selectedString = "<SelectOnMethod:"; //$NON-NLS-1$
			} else if (this.selectedNode instanceof JavadocAllocationExpression) {
				selectedString = "<SelectOnConstructor:"; //$NON-NLS-1$
			} else if (this.selectedNode instanceof JavadocSingleNameReference) {
				selectedString = "<SelectOnLocalVariable:"; //$NON-NLS-1$
			} else if (this.selectedNode instanceof JavadocSingleTypeReference) {
				JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) this.selectedNode;
				if (typeRef.packageBinding == null) {
					selectedString = "<SelectOnType:"; //$NON-NLS-1$
				}
			} else if (this.selectedNode instanceof JavadocQualifiedTypeReference) {
				JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) this.selectedNode;
				if (typeRef.packageBinding == null) {
					selectedString = "<SelectOnType:"; //$NON-NLS-1$
				}
			} else {
				selectedString = "<SelectOnType:"; //$NON-NLS-1$
			}
			int pos = output.length()-3;
			output.replace(pos-2,pos, selectedString+selectedNode+'>');
		}
		return output;
	}

	/**
	 * Resolve selected node if not null and throw exception to let clients know
	 * that it has been found.
	 *
	 * @throws SelectionNodeFound
	 */
	private void internalResolve(Scope scope) {
		if (this.selectedNode != null) {
			switch (scope.kind) {
				case Scope.CLASS_SCOPE:
					this.selectedNode.resolveType((ClassScope)scope);
					break;
				case Scope.METHOD_SCOPE:
					this.selectedNode.resolveType((MethodScope)scope);
					break;
			}
			Binding binding = null;
			if (this.selectedNode instanceof JavadocFieldReference) {
				JavadocFieldReference fieldRef = (JavadocFieldReference) this.selectedNode;
				binding = fieldRef.binding;
				if (binding == null && fieldRef.methodBinding != null) {
					binding = fieldRef.methodBinding;
				}
			} else if (this.selectedNode instanceof JavadocMessageSend) {
				binding = ((JavadocMessageSend) this.selectedNode).binding;
			} else if (this.selectedNode instanceof JavadocAllocationExpression) {
				binding = ((JavadocAllocationExpression) this.selectedNode).binding;
			} else if (this.selectedNode instanceof JavadocSingleNameReference) {
				binding = ((JavadocSingleNameReference) this.selectedNode).binding;
			} else if (this.selectedNode instanceof JavadocSingleTypeReference) {
				JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) this.selectedNode;
				if (typeRef.packageBinding == null) {
					binding = typeRef.resolvedType;
				}
			} else if (this.selectedNode instanceof JavadocQualifiedTypeReference) {
				JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) this.selectedNode;
				if (typeRef.packageBinding == null) {
					binding = typeRef.resolvedType;
				}
			} else {
				binding = this.selectedNode.resolvedType;
			}
			throw new SelectionNodeFound(binding);
		}
	}

	/**
	 * Resolve selected node if not null and throw exception to let clients know
	 * that it has been found.
	 *
	 * @throws SelectionNodeFound
	 */
	public void resolve(ClassScope scope) {
		internalResolve(scope);
	}

	/**
	 * Resolve selected node if not null and throw exception to let clients know
	 * that it has been found.
	 *
	 * @throws SelectionNodeFound
	 */
	public void resolve(MethodScope scope) {
		internalResolve(scope);
	}

}
