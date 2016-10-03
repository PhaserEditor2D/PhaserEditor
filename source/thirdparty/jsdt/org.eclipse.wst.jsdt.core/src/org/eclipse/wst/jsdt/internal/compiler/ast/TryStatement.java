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

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.ITryStatement;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.ExceptionHandlingFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FinallyFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.InsideSubRoutineFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.NullInfoRegistry;
import org.eclipse.wst.jsdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;

public class TryStatement extends SubRoutineStatement implements ITryStatement {

	public Block tryBlock;
	public Block[] catchBlocks;

	public Argument[] catchArguments;

	// should rename into subRoutineComplete to be set to false by default

	public Block finallyBlock;
	BlockScope scope;

	public UnconditionalFlowInfo subRoutineInits;
	ReferenceBinding[] caughtExceptionTypes;
	boolean[] catchExits;

	boolean isSubRoutineStartLabel;
	public LocalVariableBinding anyExceptionVariable,
		returnAddressVariable,
		secretReturnValue;



	// for local variables table attributes
	int mergedInitStateIndex = -1;
	int preTryInitStateIndex = -1;
	int naturalExitMergeInitStateIndex = -1;
	int[] catchExitInitStateIndexes;

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {

	// Consider the try block and catch block so as to compute the intersection of initializations and
	// the minimum exit relative depth amongst all of them. Then consider the subroutine, and append its
	// initialization to the try/catch ones, if the subroutine completes normally. If the subroutine does not
	// complete, then only keep this result for the rest of the analysis

	// process the finally block (subroutine) - create a context for the subroutine

	if (this.anyExceptionVariable != null) {
		this.anyExceptionVariable.useFlag = LocalVariableBinding.USED;
	}
	if (this.returnAddressVariable != null) { // TODO (philippe) if subroutine is escaping, unused
		this.returnAddressVariable.useFlag = LocalVariableBinding.USED;
	}
	if (!isSubRoutineStartLabel) {
		// no finally block -- this is a simplified copy of the else part
		// process the try block in a context handling the local exceptions.
		ExceptionHandlingFlowContext handlingContext =
			new ExceptionHandlingFlowContext(
				flowContext,
				this,
				this.caughtExceptionTypes,
				this.scope,
				flowInfo.unconditionalInits());
		handlingContext.initsOnFinally =
			new NullInfoRegistry(flowInfo.unconditionalInits());
		// only try blocks initialize that member - may consider creating a
		// separate class if needed

		FlowInfo tryInfo;
		if (this.tryBlock.isEmptyBlock()) {
			tryInfo = flowInfo;
		} else {
			tryInfo = this.tryBlock.analyseCode(currentScope, handlingContext, flowInfo.copy());
			if ((tryInfo.tagBits & FlowInfo.UNREACHABLE) != 0)
				this.bits |= ASTNode.IsTryBlockExiting;
		}

		// process the catch blocks - computing the minimal exit depth amongst try/catch
		if (this.catchArguments != null) {
			int catchCount;
			this.catchExits = new boolean[catchCount = this.catchBlocks.length];
			this.catchExitInitStateIndexes = new int[catchCount];
			for (int i = 0; i < catchCount; i++) {
				// keep track of the inits that could potentially have led to this exception handler (for final assignments diagnosis)
				FlowInfo catchInfo;
				if (this.caughtExceptionTypes[i].isUncheckedException(true)) {
					catchInfo =
						handlingContext.initsOnFinally.mitigateNullInfoOf(
							flowInfo.unconditionalCopy().
								addPotentialInitializationsFrom(
									handlingContext.initsOnException(
										this.caughtExceptionTypes[i])).
								addPotentialInitializationsFrom(tryInfo).
								addPotentialInitializationsFrom(
									handlingContext.initsOnReturn));
				} else {
					catchInfo =
						flowInfo.unconditionalCopy().
							addPotentialInitializationsFrom(
								handlingContext.initsOnException(
									this.caughtExceptionTypes[i]))
							.addPotentialInitializationsFrom(
								tryInfo.nullInfoLessUnconditionalCopy())
								// remove null info to protect point of
								// exception null info
							.addPotentialInitializationsFrom(
								handlingContext.initsOnReturn.
									nullInfoLessUnconditionalCopy());
				}

				// catch var is always set
				LocalVariableBinding catchArg = this.catchArguments[i].binding;
				catchInfo.markAsDefinitelyAssigned(catchArg);
				catchInfo.markAsDefinitelyNonNull(catchArg);
				/*
				"If we are about to consider an unchecked exception handler, potential inits may have occured inside
				the try block that need to be detected , e.g.
				try { x = 1; throwSomething();} catch(Exception e){ x = 2} "
				"(uncheckedExceptionTypes notNil and: [uncheckedExceptionTypes at: index])
				ifTrue: [catchInits addPotentialInitializationsFrom: tryInits]."
				*/
				if (this.tryBlock.statements == null) {
					catchInfo.setReachMode(FlowInfo.UNREACHABLE);
				}
				catchInfo =
					this.catchBlocks[i].analyseCode(
						currentScope,
						flowContext,
						catchInfo);
//				this.catchExitInitStateIndexes[i] = currentScope.methodScope().recordInitializationStates(catchInfo);
				this.catchExits[i] =
					(catchInfo.tagBits & FlowInfo.UNREACHABLE) != 0;
				tryInfo = tryInfo.mergedWith(catchInfo.unconditionalInits());
			}
		}
//		this.mergedInitStateIndex =
//			currentScope.methodScope().recordInitializationStates(tryInfo);

		// chain up null info registry
		if (flowContext.initsOnFinally != null) {
			flowContext.initsOnFinally.add(handlingContext.initsOnFinally);
		}

		return tryInfo;
	} else {
		InsideSubRoutineFlowContext insideSubContext;
		FinallyFlowContext finallyContext;
		UnconditionalFlowInfo subInfo;
		// analyse finally block first
		insideSubContext = new InsideSubRoutineFlowContext(flowContext, this);

		subInfo =
			this.finallyBlock
				.analyseCode(
					currentScope,
					finallyContext = new FinallyFlowContext(flowContext, this.finallyBlock),
					flowInfo.nullInfoLessUnconditionalCopy())
				.unconditionalInits();
		if (subInfo == FlowInfo.DEAD_END) {
			this.bits |= ASTNode.IsSubRoutineEscaping;
			this.scope.problemReporter().finallyMustCompleteNormally(this.finallyBlock);
		}
		this.subRoutineInits = subInfo;
		// process the try block in a context handling the local exceptions.
		ExceptionHandlingFlowContext handlingContext =
			new ExceptionHandlingFlowContext(
				insideSubContext,
				this,
				this.caughtExceptionTypes,
				this.scope,
				flowInfo.unconditionalInits());
		handlingContext.initsOnFinally =
			new NullInfoRegistry(flowInfo.unconditionalInits());
		// only try blocks initialize that member - may consider creating a
		// separate class if needed

		FlowInfo tryInfo;
		if (this.tryBlock.isEmptyBlock()) {
			tryInfo = flowInfo;
		} else {
			tryInfo = this.tryBlock.analyseCode(currentScope, handlingContext, flowInfo.copy());
			if ((tryInfo.tagBits & FlowInfo.UNREACHABLE) != 0)
				this.bits |= ASTNode.IsTryBlockExiting;
		}

		// process the catch blocks - computing the minimal exit depth amongst try/catch
		if (this.catchArguments != null) {
			int catchCount;
			this.catchExits = new boolean[catchCount = this.catchBlocks.length];
			this.catchExitInitStateIndexes = new int[catchCount];
			for (int i = 0; i < catchCount; i++) {
				// keep track of the inits that could potentially have led to this exception handler (for final assignments diagnosis)
				FlowInfo catchInfo;
				if (this.caughtExceptionTypes[i].isUncheckedException(true)) {
					catchInfo =
						handlingContext.initsOnFinally.mitigateNullInfoOf(
							flowInfo.unconditionalCopy().
								addPotentialInitializationsFrom(
									handlingContext.initsOnException(
										this.caughtExceptionTypes[i])).
								addPotentialInitializationsFrom(tryInfo).
								addPotentialInitializationsFrom(
									handlingContext.initsOnReturn));
				}else {
					catchInfo =
						flowInfo.unconditionalCopy()
							.addPotentialInitializationsFrom(
								handlingContext.initsOnException(
									this.caughtExceptionTypes[i]))
									.addPotentialInitializationsFrom(
								tryInfo.nullInfoLessUnconditionalCopy())
								// remove null info to protect point of
								// exception null info
							.addPotentialInitializationsFrom(
									handlingContext.initsOnReturn.
									nullInfoLessUnconditionalCopy());
				}

				// catch var is always set
				LocalVariableBinding catchArg = this.catchArguments[i].binding;
				catchInfo.markAsDefinitelyAssigned(catchArg);
				catchInfo.markAsDefinitelyNonNull(catchArg);
				/*
				"If we are about to consider an unchecked exception handler, potential inits may have occured inside
				the try block that need to be detected , e.g.
				try { x = 1; throwSomething();} catch(Exception e){ x = 2} "
				"(uncheckedExceptionTypes notNil and: [uncheckedExceptionTypes at: index])
				ifTrue: [catchInits addPotentialInitializationsFrom: tryInits]."
				*/
				if (this.tryBlock.statements == null) {
					catchInfo.setReachMode(FlowInfo.UNREACHABLE);
				}
				catchInfo =
					this.catchBlocks[i].analyseCode(
						currentScope,
						insideSubContext,
						catchInfo);
//				this.catchExitInitStateIndexes[i] = currentScope.methodScope().recordInitializationStates(catchInfo);
				this.catchExits[i] =
					(catchInfo.tagBits & FlowInfo.UNREACHABLE) != 0;
				tryInfo = tryInfo.mergedWith(catchInfo.unconditionalInits());
			}
		}
		// we also need to check potential multiple assignments of final variables inside the finally block
		// need to include potential inits from returns inside the try/catch parts - 1GK2AOF
		finallyContext.complainOnDeferredChecks(
			handlingContext.initsOnFinally.mitigateNullInfoOf(
				(tryInfo.tagBits & FlowInfo.UNREACHABLE) == 0 ?
					flowInfo.unconditionalCopy().
					addPotentialInitializationsFrom(tryInfo).
						// lighten the influence of the try block, which may have
						// exited at any point
					addPotentialInitializationsFrom(insideSubContext.initsOnReturn) :
					insideSubContext.initsOnReturn),
			currentScope);

		// chain up null info registry
		if (flowContext.initsOnFinally != null) {
			flowContext.initsOnFinally.add(handlingContext.initsOnFinally);
		}

//		this.naturalExitMergeInitStateIndex =
//			currentScope.methodScope().recordInitializationStates(tryInfo);
		if (subInfo == FlowInfo.DEAD_END) {
//			this.mergedInitStateIndex =
//				currentScope.methodScope().recordInitializationStates(subInfo);
			return subInfo;
		} else {
			FlowInfo mergedInfo = tryInfo.addInitializationsFrom(subInfo);
//			this.mergedInitStateIndex =
//				currentScope.methodScope().recordInitializationStates(mergedInfo);
			return mergedInfo;
		}
	}
}


 

 
public boolean isSubRoutineEscaping() {
	return (this.bits & ASTNode.IsSubRoutineEscaping) != 0;
}

public StringBuffer printStatement(int indent, StringBuffer output) {
	printIndent(indent, output).append("try \n"); //$NON-NLS-1$
	this.tryBlock.printStatement(indent + 1, output);

	//catches
	if (this.catchBlocks != null)
		for (int i = 0; i < this.catchBlocks.length; i++) {
				output.append('\n');
				printIndent(indent, output).append("catch ("); //$NON-NLS-1$
				this.catchArguments[i].print(0, output).append(") "); //$NON-NLS-1$
				this.catchBlocks[i].printStatement(indent + 1, output);
		}
	//finally
	if (this.finallyBlock != null) {
		output.append('\n');
		printIndent(indent, output).append("finally\n"); //$NON-NLS-1$
		this.finallyBlock.printStatement(indent + 1, output);
	}
	return output;
}

public void resolve(BlockScope upperScope) {
	// special scope for secret locals optimization.
	this.scope = new BlockScope(upperScope);

	BlockScope tryScope = new BlockScope(this.scope);
	BlockScope finallyScope = null;

	if (this.finallyBlock != null) {
		if (this.finallyBlock.isEmptyBlock()) {
			if ((this.finallyBlock.bits & ASTNode.UndocumentedEmptyBlock) != 0) {
				this.scope.problemReporter().undocumentedEmptyBlock(this.finallyBlock.sourceStart, this.finallyBlock.sourceEnd);
			}
		} else {
			finallyScope = JavaScriptCore.IS_ECMASCRIPT4 ? new BlockScope(this.scope, false) : this.scope; // don't add it yet to parent scope

			// provision for returning and forcing the finally block to run
			MethodScope methodScope = this.scope.methodScope();

			// the type does not matter as long as it is not a base type
//			if (!upperScope.compilerOptions().inlineJsrBytecode) {
//				this.returnAddressVariable =
//					new LocalVariableBinding(TryStatement.SECRET_RETURN_ADDRESS_NAME, upperScope.getJavaLangObject(), ClassFileConstants.AccDefault, false);
//				finallyScope.addLocalVariable(this.returnAddressVariable);
//				this.returnAddressVariable.setConstant(Constant.NotAConstant); // not inlinable
//			}
			this.isSubRoutineStartLabel = true;

//			this.anyExceptionVariable =
//				new LocalVariableBinding(TryStatement.SECRET_ANY_HANDLER_NAME, this.scope.getJavaLangThrowable(), ClassFileConstants.AccDefault, false);
//			finallyScope.addLocalVariable(this.anyExceptionVariable);
//			this.anyExceptionVariable.setConstant(Constant.NotAConstant); // not inlinable

			if (methodScope != null && !methodScope.isInsideInitializer()) {
				MethodBinding methodBinding =
					((AbstractMethodDeclaration) methodScope.referenceContext).binding;
				if (methodBinding != null) {
					TypeBinding methodReturnType = methodBinding.returnType;
					if (methodReturnType.id != TypeIds.T_void) {
//						this.secretReturnValue =
//							new LocalVariableBinding(
//								TryStatement.SECRET_RETURN_VALUE_NAME,
//								methodReturnType,
//								ClassFileConstants.AccDefault,
//								false);
//						finallyScope.addLocalVariable(this.secretReturnValue);
//						this.secretReturnValue.setConstant(Constant.NotAConstant); // not inlinable
					}
				}
			}
			this.finallyBlock.resolveUsing(finallyScope);
			if (JavaScriptCore.IS_ECMASCRIPT4) {
				// force the finally scope to have variable positions shifted after its try scope and catch ones
				finallyScope.shiftScopes = new BlockScope[this.catchArguments == null ? 1
						: this.catchArguments.length + 1];
				finallyScope.shiftScopes[0] = tryScope;
			}
		}
	}
	this.tryBlock.resolveUsing(tryScope);

	// arguments type are checked against JavaLangThrowable in resolveForCatch(..)
	if (this.catchBlocks != null) {
		int length = this.catchArguments.length;
		TypeBinding[] argumentTypes = new TypeBinding[length];
		boolean catchHasError = false;
		for (int i = 0; i < length; i++) {
			BlockScope catchScope = new BlockScope(this.scope);
			if (JavaScriptCore.IS_ECMASCRIPT4 && finallyScope != null){
				finallyScope.shiftScopes[i+1] = catchScope;
			}
			// side effect on catchScope in resolveForCatch(..)
			if ((argumentTypes[i] = this.catchArguments[i].resolveForCatch(catchScope)) == null) {
				catchHasError = true;
			}
			this.catchBlocks[i].resolveUsing(catchScope);
		}
		if (catchHasError) {
			return;
		}
		// Verify that the catch clause are ordered in the right way:
		// more specialized first.
		this.caughtExceptionTypes = new ReferenceBinding[length];
		for (int i = 0; i < length; i++) {
			this.caughtExceptionTypes[i] = (ReferenceBinding) argumentTypes[i];
//			for (int j = 0; j < i; j++) {
//				if (this.caughtExceptionTypes[i].isCompatibleWith(argumentTypes[j])) {
//					this.scope.problemReporter().wrongSequenceOfExceptionTypesError(this, this.caughtExceptionTypes[i], i, argumentTypes[j]);
//				}
//			}
		}
	} else {
		this.caughtExceptionTypes = new ReferenceBinding[0];
	}

	if (JavaScriptCore.IS_ECMASCRIPT4 && finallyScope != null){
		// add finallyScope as last subscope, so it can be shifted behind try/catch subscopes.
		// the shifting is necessary to achieve no overlay in between the finally scope and its
		// sibling in term of local variable positions.
		this.scope.addSubscope(finallyScope);
	}
}

public void traverse(ASTVisitor visitor, BlockScope blockScope) {
	if (visitor.visit(this, blockScope)) {
		if(this.scope==null) this.scope=blockScope;
		this.tryBlock.traverse(visitor, this.scope);
		if (this.catchArguments != null) {
			for (int i = 0, max = this.catchBlocks.length; i < max; i++) {
				this.catchArguments[i].traverse(visitor, this.scope);
				this.catchBlocks[i].traverse(visitor, this.scope);
			}
		}
		if (this.finallyBlock != null)
			this.finallyBlock.traverse(visitor, this.scope);
	}
	visitor.endVisit(this, blockScope);
}
public int getASTType() {
	return IASTNode.TRY_STATEMENT;

}
}
