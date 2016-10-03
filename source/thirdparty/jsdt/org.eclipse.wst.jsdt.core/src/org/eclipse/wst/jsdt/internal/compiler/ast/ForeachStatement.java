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

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IForeachStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.LoopingFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ForeachStatement extends Statement implements IForeachStatement {

	public LocalDeclaration elementVariable;
	public int elementVariableImplicitWidening = -1;
	public Expression collection;
	public Statement action;

	// set the kind of foreach
	private int kind;
	// possible kinds of iterating behavior
	private static final int ARRAY = 0;
	private static final int RAW_ITERABLE = 1;
	private static final int GENERIC_ITERABLE = 2;

	private TypeBinding iteratorReceiverType;
	private TypeBinding collectionElementType;


	public BlockScope scope;

	// secret variables for codegen
	public LocalVariableBinding indexVariable;
	public LocalVariableBinding collectionVariable;	// to store the collection expression value
	public LocalVariableBinding maxVariable;
	// secret variable names
	private static final char[] SecretIndexVariableName = " index".toCharArray(); //$NON-NLS-1$
	private static final char[] SecretCollectionVariableName = " collection".toCharArray(); //$NON-NLS-1$
	private static final char[] SecretMaxVariableName = " max".toCharArray(); //$NON-NLS-1$

	int postCollectionInitStateIndex = -1;
	int mergedInitStateIndex = -1;

	public ForeachStatement(
		LocalDeclaration elementVariable,
		int start) {

		this.elementVariable = elementVariable;
		this.sourceStart = start;
		this.kind = -1;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {
		// initialize break and continue labels
		boolean isContinue=true;
		
		// process the element variable and collection
		this.collection.checkNPE(currentScope, flowContext, flowInfo);
		flowInfo = this.elementVariable.analyseCode(scope, flowContext, flowInfo);
		FlowInfo condInfo = this.collection.analyseCode(scope, flowContext, flowInfo.copy());

		// element variable will be assigned when iterating
		condInfo.markAsDefinitelyAssigned(this.elementVariable.binding);

//		this.postCollectionInitStateIndex = currentScope.methodScope().recordInitializationStates(condInfo);

		// process the action
		LoopingFlowContext loopingContext =
			new LoopingFlowContext(flowContext, flowInfo, this, scope);
		UnconditionalFlowInfo actionInfo =
			condInfo.nullInfoLessUnconditionalCopy();
		actionInfo.markAsDefinitelyUnknown(this.elementVariable.binding);
		FlowInfo exitBranch;
		if (!(action == null || (action.isEmptyBlock()
		        	&& currentScope.compilerOptions().complianceLevel <= ClassFileConstants.JDK1_3))) {

			if (!this.action.complainIfUnreachable(actionInfo, scope, false)) {
				actionInfo = action.
					analyseCode(scope, loopingContext, actionInfo).
					unconditionalCopy();
			}

			// code generation can be optimized when no need to continue in the loop
			exitBranch = flowInfo.unconditionalCopy().
				addInitializationsFrom(condInfo.initsWhenFalse());
			// TODO (maxime) no need to test when false: can optimize (same for action being unreachable above)
			if ((actionInfo.tagBits & loopingContext.initsOnContinue.tagBits &
					FlowInfo.UNREACHABLE) != 0) {
				isContinue = false;
			} else {
				actionInfo = actionInfo.mergedWith(loopingContext.initsOnContinue);
				exitBranch.addPotentialInitializationsFrom(actionInfo);
			}
		} else {
			exitBranch = condInfo.initsWhenFalse();
		}

		// we need the variable to iterate the collection even if the
		// element variable is not used
		final boolean hasEmptyAction = this.action == null
				|| this.action.isEmptyBlock()
				|| ((this.action.bits & IsUsefulEmptyStatement) != 0);

		switch(this.kind) {
			case ARRAY :
				if (!hasEmptyAction
						|| this.elementVariable.binding.resolvedPosition != -1) {
					this.collectionVariable.useFlag = LocalVariableBinding.USED;
					if (isContinue) {
					this.indexVariable.useFlag = LocalVariableBinding.USED;
					this.maxVariable.useFlag = LocalVariableBinding.USED;
					}
				}
				break;
			case RAW_ITERABLE :
			case GENERIC_ITERABLE :
				this.indexVariable.useFlag = LocalVariableBinding.USED;
				break;
		}
		//end of loop
		loopingContext.complainOnDeferredNullChecks(currentScope, actionInfo);

		FlowInfo mergedInfo = FlowInfo.mergedOptimizedBranches(
				(loopingContext.initsOnBreak.tagBits &
					FlowInfo.UNREACHABLE) != 0 ?
					loopingContext.initsOnBreak :
					flowInfo.addInitializationsFrom(loopingContext.initsOnBreak), // recover upstream null info
				false,
				exitBranch,
				false,
				true /*for(;;){}while(true); unreachable(); */);
//		mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
		return mergedInfo;
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {

		printIndent(indent, output).append("for ("); //$NON-NLS-1$
		this.elementVariable.printAsExpression(0, output);
		output.append(" : ");//$NON-NLS-1$
		this.collection.print(0, output).append(") "); //$NON-NLS-1$
		//block
		if (this.action == null) {
			output.append(';');
		} else {
			output.append('\n');
			this.action.printStatement(indent + 1, output);
		}
		return output;
	}

	public void resolve(BlockScope upperScope) {
		// use the scope that will hold the init declarations
		scope = new BlockScope(upperScope);
		this.elementVariable.resolve(scope); // collection expression can see itemVariable
		TypeBinding elementType = this.elementVariable.type.resolvedType;
		TypeBinding collectionType = this.collection == null ? null : this.collection.resolveType(scope);

		if (elementType != null && collectionType != null) {
			if (collectionType.isArrayType()) { // for(E e : E[])
				this.kind = ARRAY;
				this.collectionElementType = ((ArrayBinding) collectionType).elementsType();
				// in case we need to do a conversion
				int compileTimeTypeID = collectionElementType.id;
				if (elementType.isBaseType()) {
					if (!collectionElementType.isBaseType()) {
						compileTimeTypeID = scope.environment().computeBoxingType(collectionElementType).id;
						this.elementVariableImplicitWidening = UNBOXING;
						if (elementType.isBaseType()) {
							this.elementVariableImplicitWidening |= (elementType.id << 4) + compileTimeTypeID;
						}
					} else {
						this.elementVariableImplicitWidening = (elementType.id << 4) + compileTimeTypeID;
					}
				} else {
					if (collectionElementType.isBaseType()) {
						int boxedID = scope.environment().computeBoxingType(collectionElementType).id;
						this.elementVariableImplicitWidening = BOXING | (compileTimeTypeID << 4) | compileTimeTypeID; // use primitive type in implicit conversion
						compileTimeTypeID = boxedID;
					}
				}
			} else if (collectionType instanceof ReferenceBinding) {
			    ReferenceBinding iterableType = ((ReferenceBinding)collectionType).findSuperTypeErasingTo(T_JavaLangIterable, false /*Iterable is not a class*/);
			    checkIterable: {
			    	if (iterableType == null) break checkIterable;

					this.iteratorReceiverType = collectionType;
					if (((ReferenceBinding)iteratorReceiverType).findSuperTypeErasingTo(T_JavaLangIterable, false) == null) {
						this.iteratorReceiverType = iterableType; // handle indirect inheritance thru variable secondary bound
					}

			    	
			    	break checkIterable;
			    }
			}
			switch(this.kind) {
				case ARRAY :
					// allocate #index secret variable (of type int)
					this.indexVariable = new LocalVariableBinding(SecretIndexVariableName, TypeBinding.INT, ClassFileConstants.AccDefault, false);
					scope.addLocalVariable(this.indexVariable);

					// allocate #max secret variable
					this.maxVariable = new LocalVariableBinding(SecretMaxVariableName, TypeBinding.INT, ClassFileConstants.AccDefault, false);
					scope.addLocalVariable(this.maxVariable);
					// add #array secret variable (of collection type)
					this.collectionVariable = new LocalVariableBinding(SecretCollectionVariableName, collectionType, ClassFileConstants.AccDefault, false);
					scope.addLocalVariable(this.collectionVariable);
					break;
				case RAW_ITERABLE :
				case GENERIC_ITERABLE :
//					// allocate #index secret variable (of type Iterator)
//					this.indexVariable = new LocalVariableBinding(SecretIndexVariableName, scope.getJavaUtilIterator(), ClassFileConstants.AccDefault, false);
//					scope.addLocalVariable(this.indexVariable);
//					this.indexVariable.setConstant(Constant.NotAConstant); // not inlinable
					break;
			}
		}
		if (action != null) {
			action.resolve(scope);
		}
	}

	public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			this.elementVariable.traverse(visitor, scope);
			this.collection.traverse(visitor, scope);
			if (action != null) {
				action.traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.FOR_EACH_STATEMENT;
	
	}
}
