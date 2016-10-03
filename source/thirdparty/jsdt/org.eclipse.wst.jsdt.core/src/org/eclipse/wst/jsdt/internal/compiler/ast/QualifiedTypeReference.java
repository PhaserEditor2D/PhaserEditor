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
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;

public class QualifiedTypeReference extends TypeReference implements IQualifiedTypeReference {

	public char[][] tokens;
	public long[] sourcePositions;

	public QualifiedTypeReference(char[][] sources , long[] poss) {

		tokens = sources ;
		sourcePositions = poss ;
		sourceStart = (int) (sourcePositions[0]>>>32) ;
		sourceEnd = (int)(sourcePositions[sourcePositions.length-1] & 0x00000000FFFFFFFFL ) ;
	}

	public TypeReference copyDims(int dim){
		//return a type reference copy of me with some dimensions
		//warning : the new type ref has a null binding
		return new ArrayQualifiedTypeReference(tokens, dim, sourcePositions);
	}

	protected TypeBinding findNextTypeBinding(int tokenIndex, Scope scope, PackageBinding packageBinding) {
		LookupEnvironment env = scope.environment();
		try {
			env.missingClassFileLocation = this;
			if (this.resolvedType == null) {
				this.resolvedType = scope.getType(this.tokens[tokenIndex], packageBinding);
			} else {
				this.resolvedType = scope.getMemberType(this.tokens[tokenIndex], (ReferenceBinding) this.resolvedType);
				if (this.resolvedType instanceof ProblemReferenceBinding) {
					ProblemReferenceBinding problemBinding = (ProblemReferenceBinding) this.resolvedType;
					this.resolvedType = new ProblemReferenceBinding(
						org.eclipse.wst.jsdt.core.compiler.CharOperation.subarray(this.tokens, 0, tokenIndex + 1),
						problemBinding.closestMatch(),
						this.resolvedType.problemId());
				}
			}
			return this.resolvedType;
		} catch (AbortCompilation e) {
			e.updateContext(this, scope.referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	public char[] getLastToken() {
		return this.tokens[this.tokens.length-1];
	}
	protected TypeBinding getTypeBinding(Scope scope) {

		if (this.resolvedType != null)
			return this.resolvedType;

		Binding binding = scope.getPackage(this.tokens);
		if (binding != null && !binding.isValidBinding())
			return (ReferenceBinding) binding; // not found

	    PackageBinding packageBinding = binding == null ? null : (PackageBinding) binding;
	    boolean isClassScope = scope.kind == Scope.CLASS_SCOPE;
	    ReferenceBinding qualifiedType = null;
		for (int i = packageBinding == null ? 0 : packageBinding.compoundName.length, max = this.tokens.length, last = max-1; i < max; i++) {
			findNextTypeBinding(i, scope, packageBinding);
			if (!this.resolvedType.isValidBinding())
				return this.resolvedType;
			if (i < last && isTypeUseDeprecated(this.resolvedType, scope)) {
				reportDeprecatedType(this.resolvedType, scope);
			}
			if (isClassScope)
				if (((ClassScope) scope).detectHierarchyCycle(this.resolvedType, this)) // must connect hierarchy to find inherited member types
					return null;
			ReferenceBinding currentType = (ReferenceBinding) this.resolvedType;
			if (qualifiedType != null) {
				qualifiedType = currentType;
			} else {
				qualifiedType = currentType;
			}
		}
		this.resolvedType = qualifiedType;
		return this.resolvedType;
	}

	public char[][] getTypeName(){

		return tokens;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {

		for (int i = 0; i < tokens.length; i++) {
			if (i > 0) output.append('.');
			output.append(tokens[i]);
		}
		return output;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {

		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {

		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.QUALIFIED_TYPE_REFERENCE;
	
	}
}
