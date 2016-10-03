/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import java.util.ArrayList;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IConstructorDeclaration;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.flow.ExceptionHandlingFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.InitializationFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortMethod;

public class ConstructorDeclaration extends AbstractMethodDeclaration implements IConstructorDeclaration {

	public ExplicitConstructorCall constructorCall;

	public boolean isDefaultConstructor = false;

public ConstructorDeclaration(CompilationResult compilationResult){
	super(compilationResult);
}

public FlowInfo analyseCode(Scope classScope, FlowContext initializationContext, FlowInfo flowInfo) {
	analyseCode((ClassScope)classScope, (InitializationFlowContext)initializationContext, flowInfo, FlowInfo.REACHABLE);
	return flowInfo;
}
/**
 * The flowInfo corresponds to non-static field initialization infos. It may be unreachable (155423), but still the explicit constructor call must be
 * analysed as reachable, since it will be generated in the end.
 */
public void analyseCode(ClassScope classScope, InitializationFlowContext initializerFlowContext, FlowInfo flowInfo, int initialReachMode) {
	if (this.ignoreFurtherInvestigation)
		return;

	int nonStaticFieldInfoReachMode = flowInfo.reachMode();
	flowInfo.setReachMode(initialReachMode);

	checkUnused: {
		MethodBinding constructorBinding;
		if ((constructorBinding = this.getBinding()) == null) break checkUnused;
		if (this.isDefaultConstructor) break checkUnused;
		if (constructorBinding.isUsed()) break checkUnused;
		if (constructorBinding.isPrivate()) {
			if ((this.getBinding().declaringClass.tagBits & TagBits.HasNonPrivateConstructor) == 0)
				break checkUnused; // tolerate as known pattern to block instantiation
		} else if ((this.getBinding().declaringClass.tagBits & (TagBits.IsAnonymousType|TagBits.IsLocalType)) != TagBits.IsLocalType) {
			break checkUnused;
		}
	}

	try {
		ExceptionHandlingFlowContext constructorContext =
			new ExceptionHandlingFlowContext(
				initializerFlowContext.parent,
				this,
				null,
				this.getScope(),
				FlowInfo.DEAD_END);
		initializerFlowContext.checkInitializerExceptions(
			this.getScope(),
			constructorContext,
			flowInfo);

		// anonymous constructor can gain extra thrown exceptions from unhandled ones
		if (this.getBinding().declaringClass.isAnonymousType()) {
			ArrayList computedExceptions = constructorContext.extendedExceptions;
			if (computedExceptions != null){
				int size;
				if ((size = computedExceptions.size()) > 0){
					ReferenceBinding[] actuallyThrownExceptions;
					computedExceptions.toArray(actuallyThrownExceptions = new ReferenceBinding[size]);
				}
			}
		}

		// tag parameters as being set
		if (this.arguments != null) {
			for (int i = 0, count = this.arguments.length; i < count; i++) {
				flowInfo.markAsDefinitelyAssigned(this.arguments[i].binding);
			}
		}

		// propagate to constructor call
		if (this.constructorCall != null) {
			// if calling 'this(...)', then flag all non-static fields as definitely
			// set since they are supposed to be set inside other local constructor
			if (this.constructorCall.accessMode == ExplicitConstructorCall.This) {
				FieldBinding[] fields = this.getBinding().declaringClass.fields();
				for (int i = 0, count = fields.length; i < count; i++) {
					FieldBinding field;
					if (!(field = fields[i]).isStatic()) {
						flowInfo.markAsDefinitelyAssigned(field);
					}
				}
			}
			flowInfo = this.constructorCall.analyseCode(this.getScope(), constructorContext, flowInfo);
		}

		// reuse the reachMode from non static field info
		flowInfo.setReachMode(nonStaticFieldInfoReachMode);

		// propagate to statements
		if (this.statements != null) {
			boolean didAlreadyComplain = false;
			for (int i = 0, count = this.statements.length; i < count; i++) {
				Statement stat = this.statements[i];
				if (!stat.complainIfUnreachable(flowInfo, this.getScope(), didAlreadyComplain)) {
					flowInfo = stat.analyseCode(this.getScope(), constructorContext, flowInfo);
				} else {
					didAlreadyComplain = true;
				}
			}
		}
		// check for missing returning path
		this.needFreeReturn = (flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0;

		// reuse the initial reach mode for diagnosing missing blank finals
		flowInfo.setReachMode(initialReachMode);

		// check missing blank final field initializations
		if ((this.constructorCall != null)
			&& (this.constructorCall.accessMode != ExplicitConstructorCall.This)) {
			flowInfo = flowInfo.mergedWith(constructorContext.initsOnReturn);
		}
		// check unreachable catch blocks
		constructorContext.complainIfUnusedExceptionHandlers(this);
	} catch (AbortMethod e) {
		this.ignoreFurtherInvestigation = true;
	}
}

public boolean isConstructor() {
	return true;
}

public boolean isDefaultConstructor() {
	return this.isDefaultConstructor;
}

public boolean isInitializationMethod() {
	return true;
}

/*
 * Returns true if the constructor is directly involved in a cycle.
 * Given most constructors aren't, we only allocate the visited list
 * lazily.
 */
public boolean isRecursive(ArrayList visited) {
	if (this.getBinding() == null
			|| this.constructorCall == null
			|| this.constructorCall.binding == null
			|| this.constructorCall.isSuperAccess()
			|| !this.constructorCall.binding.isValidBinding()) {
		return false;
	}

	ConstructorDeclaration targetConstructor =
		((ConstructorDeclaration)this.getScope().referenceType().declarationOf(this.constructorCall.binding.original()));
	if (this == targetConstructor) return true; // direct case

	if (visited == null) { // lazy allocation
		visited = new ArrayList(1);
	} else {
		int index = visited.indexOf(this);
		if (index >= 0) return index == 0; // only blame if directly part of the cycle
	}
	visited.add(this);

	return targetConstructor.isRecursive(visited);
}

public void parseStatements(Parser parser, CompilationUnitDeclaration unit) {
	//fill up the constructor body with its statements
	if (this.ignoreFurtherInvestigation)
		return;
	if (this.isDefaultConstructor && this.constructorCall == null){
		this.constructorCall = SuperReference.implicitSuperConstructorCall();
		this.constructorCall.sourceStart = this.sourceStart;
		this.constructorCall.sourceEnd = this.sourceEnd;
		return;
	}
	parser.parse(this, unit);

}

public StringBuffer printBody(int indent, StringBuffer output) {
	output.append(" {"); //$NON-NLS-1$
	if (this.constructorCall != null) {
		output.append('\n');
		this.constructorCall.printStatement(indent, output);
	}
	if (this.statements != null) {
		for (int i = 0; i < this.statements.length; i++) {
			output.append('\n');
			this.statements[i].printStatement(indent, output);
		}
	}
	output.append('\n');
	printIndent(indent == 0 ? 0 : indent - 1, output).append('}');
	return output;
}

public void resolveJavadoc() {
	if (this.getBinding() == null || this.javadoc != null) {
		super.resolveJavadoc();
	} else if (!this.isDefaultConstructor) {
		this.getScope().problemReporter().javadocMissing(this.sourceStart, this.sourceEnd, this.getBinding().modifiers);
	}
}

/*
 * Type checking for constructor, just another method, except for special check
 * for recursive constructor invocations.
 */
public void resolveStatements() {
	SourceTypeBinding sourceType = this.getScope().enclosingSourceType();
	if (!CharOperation.equals(sourceType.sourceName, this.selector)){
		this.getScope().problemReporter().missingReturnType(this);
	}
	if (this.getBinding() != null && !this.getBinding().isPrivate()) {
		sourceType.tagBits |= TagBits.HasNonPrivateConstructor;
	}
	// if null ==> an error has occurs at parsing time ....
	if (this.constructorCall != null) {
		if (sourceType.id == TypeIds.T_JavaLangObject
				&& this.constructorCall.accessMode != ExplicitConstructorCall.This) {
			this.constructorCall = null;
		} else {
			this.constructorCall.resolve(this.getScope());
		}
	}
	super.resolveStatements();
}

public void traverse(ASTVisitor visitor,	ClassScope classScope) {
	if (visitor.visit(this, classScope)) {
		if (this.javadoc != null) {
			this.javadoc.traverse(visitor, this.getScope());
		}
		if (this.arguments != null) {
			int argumentLength = this.arguments.length;
			for (int i = 0; i < argumentLength; i++)
				this.arguments[i].traverse(visitor, this.getScope());
		}
		if (this.constructorCall != null)
			this.constructorCall.traverse(visitor, this.getScope());
		if (this.statements != null) {
			int statementsLength = this.statements.length;
			for (int i = 0; i < statementsLength; i++)
				this.statements[i].traverse(visitor, this.getScope());
		}
	}
	visitor.endVisit(this, classScope);
}
public int getASTType() {
	return IASTNode.CONSTRUCTOR_DECLARATION;

}
}
