/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IAssignment;
import org.eclipse.wst.jsdt.core.ast.ILocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FunctionTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding;

public class LocalDeclaration extends AbstractVariableDeclaration implements ILocalDeclaration {

	public LocalVariableBinding binding;
	
	private boolean isLocal = true;

	public LocalDeclaration(char[] name, int sourceStart, int sourceEnd) {

		this.name = name;
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
		this.declarationEnd = sourceEnd;
	}

	public IAssignment getAssignment() {
		if (this.initialization == null)
			return null;
		if (initialization instanceof FunctionExpression) {
			return new Assignment(new SingleNameReference(this.name, this.sourceStart, this.sourceEnd), this.initialization, this.initialization.sourceEnd);
		}
		return null;
	}

	public LocalVariableBinding getBinding() {
		return this.binding;
	}

	public void setBinding(LocalVariableBinding binding) {
		this.binding = binding;
	}

	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		// Do not analyseCode for the nextLocal local variables in recursion
		// because it may cause StackOverflowError
		AbstractVariableDeclaration currVarDecl = this;
		while (currVarDecl != null) {
			if (currVarDecl instanceof LocalDeclaration) {
				flowInfo = ((LocalDeclaration)currVarDecl).analyseCodeLocal(currentScope, flowContext, flowInfo);
			}
			currVarDecl = currVarDecl.nextLocal;
		}
		
		return flowInfo;
	}
	
	public FlowInfo analyseCodeLocal(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		// record variable initialization if any
		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0) {
			// only set if actually reached
			bits |= ASTNode.IsLocalDeclarationReachable; 
		}
		if (this.initialization != null) {
			int nullStatus = this.initialization.nullStatus(flowInfo);
			flowInfo = this.initialization.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
			// for local variable debug attributes
			if (!flowInfo.isDefinitelyAssigned(this.binding)) {
				this.bits |= FirstAssignmentToLocal;
			}
			else {
				this.bits &= ~FirstAssignmentToLocal; // int i = (i = 0);
			}
			flowInfo.markAsDefinitelyAssigned(binding);

			switch (nullStatus) {
				case FlowInfo.NULL :
					flowInfo.markAsDefinitelyNull(this.binding);
					break;
				case FlowInfo.NON_NULL :
					flowInfo.markAsDefinitelyNonNull(this.binding);
					break;
				default :
					flowInfo.markAsDefinitelyUnknown(this.binding);
			}
			// no need to inform enclosing try block since its locals won't get
			// known by the finally block
		}

		return flowInfo;
	}

	public void checkModifiers() {

		// only potential valid modifier is <<final>>
		if (((modifiers & ExtraCompilerModifiers.AccJustFlag) & ~ClassFileConstants.AccFinal) != 0) {
			// AccModifierProblem -> other (non-visibility problem)
			// AccAlternateModifierProblem -> duplicate modifier
			// AccModifierProblem | AccAlternateModifierProblem -> visibility problem"

			modifiers = (modifiers & ~ExtraCompilerModifiers.AccAlternateModifierProblem) | ExtraCompilerModifiers.AccModifierProblem;
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration#getKind()
	 */
	public int getKind() {
		return LOCAL_VARIABLE;
	}

	public TypeBinding resolveVarType(BlockScope scope) {
		TypeBinding variableType = null;

		if (type != null)
			variableType = type.resolveType(scope, true /* check bounds */);
		else {
			if (inferredType != null)
				variableType = inferredType.resolveType(scope, this);
			else
				variableType = TypeBinding.UNKNOWN;
		}

		checkModifiers();
		return variableType;
	}

	public void resolve(BlockScope scope) {
		// Do not resolve the nextLocal local variables in recursion
		// because it may cause StackOverflowError
		AbstractVariableDeclaration currVarDecl = this;
		while (currVarDecl != null) {
			if (currVarDecl instanceof LocalDeclaration) {
				((LocalDeclaration)currVarDecl).resolveLocal(scope);
			}
			currVarDecl = currVarDecl.nextLocal;
		}
	}

	public void resolveLocal(BlockScope scope) {
		// create a binding and add it to the scope
		TypeBinding variableType = resolveVarType(scope);

		if (type != null) {
			variableType = type.resolveType(scope, true /* check bounds */);
		} else {
			if (inferredType != null) {
				variableType = inferredType.resolveType(scope, this);
			}
			else {
				variableType = TypeBinding.UNKNOWN;
			}
		}

		checkModifiers();

		Binding varBinding = null;
		if (scope.enclosingMethodScope() == null) {
			// do not resolve hidden field
			varBinding = scope.getBinding(name, Binding.VARIABLE, this, false);
		} else {
			varBinding = scope.getLocalBinding(name, Binding.VARIABLE, this, false);
		}
		
		boolean alreadyDefined = false;
		if (varBinding != null && varBinding.isValidBinding()) {
			VariableBinding existingVariable = (VariableBinding) varBinding;
			if (existingVariable.isFor(this)) {
				if (variableType != null)
					existingVariable.type = variableType;
				alreadyDefined = true;
			}
			else {
				if (existingVariable instanceof LocalVariableBinding && this.hiddenVariableDepth == 0) {
					LocalVariableBinding localVariableBinding = (LocalVariableBinding) existingVariable;
					if (localVariableBinding.declaringScope instanceof CompilationUnitScope && scope.enclosingMethodScope() != null) {
						scope.problemReporter().localVariableHiding(this, existingVariable, false);
					} else {
						scope.problemReporter().redefineLocal(this);
					}
				}
				else {
					scope.problemReporter().localVariableHiding(this, existingVariable, false);
				}
			}
		}

		if ((modifiers & ClassFileConstants.AccFinal) != 0 && this.initialization == null) {
			modifiers |= ExtraCompilerModifiers.AccBlankFinal;
		}
		if (!(this.binding != null && alreadyDefined)) {
			this.binding = new LocalVariableBinding(this, variableType, modifiers, false);
			MethodScope methodScope = scope.enclosingMethodScope();
			if (methodScope != null) {
				methodScope.addLocalVariable(binding);
			} else {
				scope.compilationUnitScope().addLocalVariable(binding);
			}
		}
		// allow to recursivelly target the binding....
		// the correct constant is harmed if correctly computed at the end of this method

		if (variableType == null) {
			if (initialization != null) {
				// want to report all possible errors
				initialization.resolveType(scope); 
			}

			return;
		}

		// store the constant for final locals
		if (initialization != null) {
			if (initialization instanceof ArrayInitializer) {
				TypeBinding initializationType = initialization.resolveTypeExpecting(scope, variableType);
				if (initializationType != null) {
					((ArrayInitializer) initialization).binding = (ArrayBinding) initializationType;
				}
			}
			else {
				this.initialization.setExpectedType(variableType);
				TypeBinding initializationType = this.initialization.resolveType(scope);
				if (initializationType != null) {
					if (initializationType.isFunctionType()) {
						MethodBinding existingMethod = scope.findMethod(this.name, null, false);
						if (existingMethod != null) {
							MethodBinding functionBinding = ((FunctionTypeBinding) initializationType).functionBinding;
							existingMethod.updateFrom(functionBinding);
						}
					}
					if (variableType == TypeBinding.UNKNOWN && initializationType != TypeBinding.NULL)
						this.binding.type = initializationType;
				}
			}
			
			// check for assignment with no effect
			if (this.binding == Assignment.getDirectBinding(this.initialization)) {
				scope.problemReporter().assignmentHasNoEffect(this, this.name);
			}
		}
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {
		if (this.javadoc != null) {
			this.javadoc.print(indent, output);
		}
		return super.printStatement(indent, output);
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		// Do not traverse the nextLocal local variables in recursion
		// because it may cause StackOverflowError
		AbstractVariableDeclaration currVarDecl = this;
		while (currVarDecl != null) {
			if (currVarDecl instanceof LocalDeclaration) {
				((LocalDeclaration)currVarDecl).traverseLocal(visitor, scope);
			}
			currVarDecl = currVarDecl.nextLocal;
		}
	}
	
	private void traverseLocal(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (type != null) {
				type.traverse(visitor, scope);
			}
			
			IAssignment assignment = getAssignment();
			if (assignment != null) {
				((Assignment) assignment).traverse(visitor, scope);
			}
			else if (initialization != null) {
				initialization.traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}

	public String getTypeName() {
		if (type != null) {
			return type.toString();
		}
		
		if (inferredType != null) {
			return new String(inferredType.getName());
		}
		
		return null;
	}

	public int getASTType() {
		return IASTNode.LOCAL_DECLARATION;
	}
	
	/**
	 * <p>
	 * Set if declaration is a type on the declaration and the assignment.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration#setIsType(boolean)
	 */
	public void setIsType(boolean isType) {
		super.setIsType(isType);
		
		if(this.getAssignment() != null) {
			this.getAssignment().setIsType(isType);
		}
	}
	
	/**
	 * <p>
	 * Is type if declaration set as type or assignment is set as type.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration#isType()
	 */
	public boolean isType() {
		return super.isType() || (this.getAssignment() != null && this.getAssignment().isType());
	}
	
	public void setIsLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}
	
	public boolean isLocal() {
		return this.isLocal;
	}
}