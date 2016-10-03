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
package org.eclipse.wst.jsdt.internal.codeassist.select;

/*
 * Selection node build by the parser in any case it was intending to
 * reduce a single name reference containing the assist identifier.
 * e.g.
 *
 *	class X {
 *    void foo() {
 *      [start]ba[end]
 *    }
 *  }
 *
 *	---> class X {
 *         void foo() {
 *           <SelectOnName:ba>
 *         }
 *       }
 *
 */

import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnSingleNameReference extends SingleNameReference {
public SelectionOnSingleNameReference(char[] source, long pos) {
	super(source, pos);
}
public TypeBinding resolveType(BlockScope scope) {
	if (this.actualReceiverType != null) {
		this.binding = scope.getField(this.actualReceiverType, token, this);
		if (this.binding != null && this.binding.isValidBinding()) {
			throw new SelectionNodeFound(binding);
		}
	}
	// it can be a package, type, member type, local variable or field
	binding = scope.getBinding(token, Binding.VARIABLE | Binding.METHOD, this, true /*resolve*/);
	if (!binding.isValidBinding()) {
		if (binding instanceof ProblemFieldBinding) {
			// tolerate some error cases
			if (binding.problemId() == ProblemReasons.NotVisible
					|| binding.problemId() == ProblemReasons.InheritedNameHidesEnclosingName
					|| binding.problemId() == ProblemReasons.NonStaticReferenceInConstructorInvocation
					|| binding.problemId() == ProblemReasons.NonStaticReferenceInStaticContext){
				throw new SelectionNodeFound(binding);
			}
			scope.problemReporter().invalidField(this, (FieldBinding) binding);
		} else if (binding instanceof ProblemReferenceBinding) {
			// tolerate some error cases
			if (binding.problemId() == ProblemReasons.NotVisible){
				throw new SelectionNodeFound(binding);
			}
			scope.problemReporter().invalidType(this, (TypeBinding) binding);
		} else {
			scope.problemReporter().unresolvableReference(this, binding);
		}
		throw new SelectionNodeFound();
	}

	throw new SelectionNodeFound(binding);
}

/**
 * <p>Overrides parent so that {@link #resolveType(BlockScope)} will be called and the {@link SelectionNodeFound} exception
 * will be thrown.  But calls parent implementation first so that if part of an assignment any possible defining will take
 * place before trying to get the binding.</p>
 * 
 * @see org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference#resolveType(org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope, boolean, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)
 */
public TypeBinding resolveType(BlockScope scope, boolean define, TypeBinding useType) {
	/* resolve the single name reference
	 * this is needed for the case where the selection is the LHS of an assignment
	 */
	super.resolveType(scope, define, useType);
	
	//run the selection resolve type so that the SelectionNodeFound exception will be thrown
	return this.resolveType(scope);
}

public TypeBinding resolveForAllocation(BlockScope scope, ASTNode location) {
	TypeBinding typeBinding=null;
	this.binding=	
			scope.getBinding(this.token, (Binding.TYPE|Binding.METHOD | bits)  & RestrictiveFlagMASK, this, true /*resolve*/);
	if (binding instanceof TypeBinding)
		typeBinding=(TypeBinding)binding;
	else if (binding instanceof MethodBinding)
		typeBinding=((MethodBinding)binding).returnType;
	else
		if (typeBinding==null || binding==null)
			throw new SelectionNodeFound();
		else if (!binding.isValidBinding()){
			switch (binding.problemId() ) {
			case ProblemReasons.NotVisible:
			case ProblemReasons.InheritedNameHidesEnclosingName:
			case ProblemReasons.NonStaticReferenceInConstructorInvocation:
			case ProblemReasons.NonStaticReferenceInStaticContext:
				throw new SelectionNodeFound(typeBinding);

			default:
				throw new SelectionNodeFound();
			}
//			!binding.isValidBinding())
		}

	throw new SelectionNodeFound(typeBinding);
}

public StringBuffer printExpression(int indent, StringBuffer output) {
	output.append("<SelectOnName:"); //$NON-NLS-1$
	return super.printExpression(0, output).append('>');
}
}
