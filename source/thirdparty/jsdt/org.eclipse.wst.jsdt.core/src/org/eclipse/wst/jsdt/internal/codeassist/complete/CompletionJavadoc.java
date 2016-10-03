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
package org.eclipse.wst.jsdt.internal.codeassist.complete;

import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;

/**
 * Node representing a Javadoc comment including code selection.
 */
public class CompletionJavadoc extends Javadoc {

	Expression completionNode;

	public CompletionJavadoc(int sourceStart, int sourceEnd) {
		super(sourceStart, sourceEnd);
	}

	/**
	 * @return Returns the completionNode.
	 */
	public Expression getCompletionNode() {
		return this.completionNode;
	}

	/**
	 * Resolve selected node if not null and throw exception to let clients know
	 * that it has been found.
	 *
	 * @throws CompletionNodeFound
	 */
	private void internalResolve(Scope scope) {
		if (this.completionNode != null) {
			if (this.completionNode instanceof CompletionOnJavadocTag) {
				((CompletionOnJavadocTag)this.completionNode).filterPossibleTags(scope);
			} else {
				boolean resolve = true;
				if (this.completionNode instanceof CompletionOnJavadocParamNameReference) {
					resolve = ((CompletionOnJavadocParamNameReference)this.completionNode).token != null;
				}
				if (resolve) {
					switch (scope.kind) {
						case Scope.CLASS_SCOPE:
							this.completionNode.resolveType((ClassScope)scope);
							break;
						case Scope.METHOD_SCOPE:
							this.completionNode.resolveType((MethodScope) scope);
							break;
					}
				}
				if (this.completionNode instanceof CompletionOnJavadocParamNameReference) {
					CompletionOnJavadocParamNameReference paramNameReference = (CompletionOnJavadocParamNameReference) this.completionNode;
					if (scope.kind == Scope.METHOD_SCOPE) {
						paramNameReference.missingParams = missingParamTags(paramNameReference.binding, (MethodScope)scope);
					}
				}
			}
			Binding qualifiedBinding = null;
			if (this.completionNode instanceof CompletionOnJavadocQualifiedTypeReference) {
				CompletionOnJavadocQualifiedTypeReference typeRef = (CompletionOnJavadocQualifiedTypeReference) this.completionNode;
				if (typeRef.packageBinding == null) {
					qualifiedBinding = typeRef.resolvedType;
				} else {
					qualifiedBinding = typeRef.packageBinding;
				}
			} else if (this.completionNode instanceof CompletionOnJavadocMessageSend) {
				CompletionOnJavadocMessageSend msg = (CompletionOnJavadocMessageSend) this.completionNode;
				if (msg.receiver!=null && !msg.receiver.isThis()) qualifiedBinding = msg.receiver.resolvedType;
			} else if (this.completionNode instanceof CompletionOnJavadocAllocationExpression) {
				CompletionOnJavadocAllocationExpression alloc = (CompletionOnJavadocAllocationExpression) this.completionNode;
				qualifiedBinding = alloc.type.resolvedType;
			}
			throw new CompletionNodeFound(this.completionNode, qualifiedBinding, scope);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode#print(int, java.lang.StringBuffer)
	 */
	public StringBuffer print(int indent, StringBuffer output) {
		printIndent(indent, output).append("/**\n"); //$NON-NLS-1$
		boolean nodePrinted = false;
		if (this.paramReferences != null) {
			for (int i = 0, length = this.paramReferences.length; i < length; i++) {
				printIndent(indent, output).append(" * @param "); //$NON-NLS-1$
				this.paramReferences[i].print(indent, output).append('\n');
				if (!nodePrinted && this.completionNode != null) {
					nodePrinted =  this.completionNode == this.paramReferences[i];
				}
			}
		}
		if (this.returnStatement != null) {
			printIndent(indent, output).append(" * @"); //$NON-NLS-1$
			this.returnStatement.print(indent, output).append('\n');
		}
		if (this.exceptionReferences != null) {
			for (int i = 0, length = this.exceptionReferences.length; i < length; i++) {
				printIndent(indent, output).append(" * @throws "); //$NON-NLS-1$
				this.exceptionReferences[i].print(indent, output).append('\n');
				if (!nodePrinted && this.completionNode != null) {
					nodePrinted =  this.completionNode == this.exceptionReferences[i];
				}
			}
		}
		if (this.seeReferences != null) {
			for (int i = 0, length = this.seeReferences.length; i < length; i++) {
				printIndent(indent, output).append(" * @see "); //$NON-NLS-1$
				this.seeReferences[i].print(indent, output).append('\n');
				if (!nodePrinted && this.completionNode != null) {
					nodePrinted =  this.completionNode == this.seeReferences[i];
				}
			}
		}
		if (!nodePrinted && this.completionNode != null) {
			printIndent(indent, output).append(" * "); //$NON-NLS-1$
			this.completionNode.print(indent, output).append('\n');
		}
		printIndent(indent, output).append(" */\n"); //$NON-NLS-1$
		return output;
	}

	/**
	 * Resolve completion node if not null and throw exception to let clients know
	 * that it has been found.
	 *
	 * @throws CompletionNodeFound
	 */
	public void resolve(ClassScope scope) {
		super.resolve(scope);
		internalResolve(scope);
	}

	/**
	 * Resolve completion node if not null and throw exception to let clients know
	 * that it has been found.
	 *
	 * @throws CompletionNodeFound
	 */
	public void resolve(CompilationUnitScope scope) {
		internalResolve(scope);
	}

	/**
	 * Resolve completion node if not null and throw exception to let clients know
	 * that it has been found.
	 *
	 * @throws CompletionNodeFound
	 */
	public void resolve(MethodScope scope) {
		super.resolve(scope);
		internalResolve(scope);
	}

	/*
	 * Look for missing method @param tags
	 */
	private char[][] missingParamTags(Binding paramNameRefBinding, MethodScope methScope) {

		// Verify if there's some possible param tag
		AbstractMethodDeclaration md = methScope.referenceMethod();
		int paramTagsSize = this.paramReferences == null ? 0 : this.paramReferences.length;
		if (md == null) return null;
		int argumentsSize = md.arguments == null ? 0 : md.arguments.length;
		if (argumentsSize == 0) return null;

		// Store all method arguments if there's no @param in javadoc
		if (paramTagsSize == 0) {
			char[][] missingParams = new char[argumentsSize][];
			for (int i = 0; i < argumentsSize; i++) {
				missingParams[i] = md.arguments[i].name;
			}
			return missingParams;
		}

		// Look for missing arguments
		char[][] missingParams = new char[argumentsSize][];
		int size = 0;
		for (int i = 0; i < argumentsSize; i++) {
			Argument arg = md.arguments[i];
			boolean found = false;
			int paramNameRefCount = 0;
			for (int j = 0; j < paramTagsSize && !found; j++) {
				JavadocSingleNameReference param = this.paramReferences[j];
				if (arg.binding == param.binding) {
					if (param.binding == paramNameRefBinding) { // do not count first occurence of param name reference
						paramNameRefCount++;
						found = paramNameRefCount > 1;
					} else {
						found = true;
					}
				}
			}
			if (!found) {
				missingParams[size++] = arg.name;
			}
		}
		if (size > 0) {
			if (size != argumentsSize) {
				System.arraycopy(missingParams, 0, missingParams = new char[size][], 0, size);
			}
			return missingParams;
		}
		return null;
	}
}
