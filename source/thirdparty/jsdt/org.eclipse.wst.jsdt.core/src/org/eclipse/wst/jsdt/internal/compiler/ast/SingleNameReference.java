/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.ISingleNameReference;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferEngine;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding;

public class SingleNameReference extends NameReference implements ISingleNameReference, OperatorIds {

	public char[] token;

	public SingleNameReference(char[] source, long pos) {
		this(source, (int) (pos >>> 32), (int) pos);
	}
	
	public SingleNameReference(char[] source, int sourceStart, int sourceEnd) {
		super();
		token = source;
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
	}
	
	public char[] getToken() {
		return this.token;
	}
	public FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean isCompound) {

		boolean isReachable = (flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0;
		// compound assignment extra work
		if (isCompound) { // check the variable part is initialized if blank final
			switch (bits & RestrictiveFlagMASK) {
				case Binding.FIELD : // reading a field
					break;
				case Binding.LOCAL : // reading a local variable
					// check if assigning a final blank field
					LocalVariableBinding localBinding = null;
					if (this.binding instanceof LocalVariableBinding &&
								!flowInfo.isDefinitelyAssigned(localBinding = (LocalVariableBinding) binding)) {
						
						if (localBinding.declaringScope instanceof MethodScope) {
							currentScope.problemReporter().uninitializedLocalVariable(localBinding, this);
						}
						// we could improve error msg here telling "cannot use compound assignment on final local variable"
					}
					
					if(localBinding != null) {
						if (isReachable) {
							localBinding.useFlag = LocalVariableBinding.USED;
						} else if (localBinding.useFlag == LocalVariableBinding.UNUSED) {
							localBinding.useFlag = LocalVariableBinding.FAKE_USED;
						}
					}
			}
		}
		if (assignment.expression != null) {
			flowInfo = assignment.expression.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
		}
		switch (bits & RestrictiveFlagMASK) {
			case Binding.FIELD : // assigning to a field
				break;
			case Binding.LOCAL : // assigning to a local variable
				if(this.binding instanceof LocalVariableBinding) {
					LocalVariableBinding localBinding = (LocalVariableBinding) binding;
					if (!flowInfo.isDefinitelyAssigned(localBinding)){// for local variable debug attributes
						bits |= FirstAssignmentToLocal;
					} else {
						bits &= ~FirstAssignmentToLocal;
					}
					if ((localBinding.tagBits & TagBits.IsArgument) != 0) {
						currentScope.problemReporter().parameterAssignment(localBinding, this);
					}
					flowInfo.markAsDefinitelyAssigned(localBinding);
				}
		}
		manageEnclosingInstanceAccessIfNecessary(currentScope, flowInfo);
		return flowInfo;
	}
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		return analyseCode(currentScope, flowContext, flowInfo, true);
	}
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, boolean valueRequired) {

		switch (bits & RestrictiveFlagMASK) {
			case Binding.FIELD : // reading a field
				break;
			case Binding.LOCAL : // reading a local variable
			case Binding.LOCAL | Binding.TYPE :
			case Binding.VARIABLE:
				if(binding instanceof LocalVariableBinding) {
					LocalVariableBinding localBinding= (LocalVariableBinding) binding;
	
					// ignore the arguments variable inside a function
					if(!(CharOperation.equals(localBinding.name, new char[]{'a','r','g','u','m','e','n','t','s'}) && (localBinding.declaringScope instanceof MethodScope))) {
						if(!flowInfo.isDefinitelyAssigned(localBinding)) {
							if (localBinding.declaringScope instanceof MethodScope) {
									currentScope.problemReporter().uninitializedLocalVariable(localBinding, this);		
							} else if(localBinding.isSameCompilationUnit(currentScope)) {
								currentScope.problemReporter().uninitializedGlobalVariable(localBinding, this);
							}
						}
					}
					
					if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0)	{
						localBinding.useFlag = LocalVariableBinding.USED;
					} else if (localBinding.useFlag == LocalVariableBinding.UNUSED) {
						localBinding.useFlag = LocalVariableBinding.FAKE_USED;
					}	
				}
				
		}
		if (valueRequired) {
			manageEnclosingInstanceAccessIfNecessary(currentScope, flowInfo);
		}
		return flowInfo;
	}

	public TypeBinding checkFieldAccess(BlockScope scope) {

		FieldBinding fieldBinding = (FieldBinding) binding;

		bits &= ~RestrictiveFlagMASK; // clear bits
		bits |= Binding.FIELD;
		MethodScope methodScope = scope.methodScope();
		boolean isStatic = fieldBinding.isStatic();
		if (!isStatic) {
			// must check for the static status....
			if (methodScope!=null && methodScope.isStatic) {
					// reference is ok if coming from compilation unit superclass
				if (fieldBinding.declaringClass==null || !fieldBinding.declaringClass.equals(scope.compilationUnitScope().superBinding))
				{
					scope.problemReporter().staticFieldAccessToNonStaticVariable(this, fieldBinding);
					this.constant = Constant.NotAConstant;
					return fieldBinding.type;
				}
			}
		}

		if (isFieldUseDeprecated(fieldBinding, scope, (this.bits & IsStrictlyAssigned) !=0))
			scope.problemReporter().deprecatedField(fieldBinding, this);

		return fieldBinding.type;

	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#genericTypeArguments()
	 */
	public TypeBinding[] genericTypeArguments() {
		return null;
	}

	/**
	 * Returns the local variable referenced by this node. Can be a direct reference (SingleNameReference)
	 * or thru a cast expression etc...
	 */
	public LocalVariableBinding localVariableBinding() {
		switch (bits & RestrictiveFlagMASK) {
			case Binding.FIELD : // reading a field
				break;
			case Binding.LOCAL : // reading a local variable
				if(this.binding instanceof LocalVariableBinding) {
					return (LocalVariableBinding) this.binding;
				}
		}
		return null;
	}

	public void manageEnclosingInstanceAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo) {
		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0)	{
			//If inlinable field, forget the access emulation, the code gen will directly target it
			if (((bits & DepthMASK) == 0) || (constant != Constant.NotAConstant)) {
				return;
			}
	
			if ((bits & RestrictiveFlagMASK) == Binding.LOCAL && this.binding instanceof LocalVariableBinding) {
				currentScope.emulateOuterAccess((LocalVariableBinding) binding);
			}
		}
	}


	public int nullStatus(FlowInfo flowInfo) {
		if (this.constant != null && this.constant != Constant.NotAConstant) {
			return FlowInfo.NON_NULL; // constant expression cannot be null
		}
		switch (bits & RestrictiveFlagMASK) {
			case Binding.FIELD : // reading a field
				return FlowInfo.UNKNOWN;
			case Binding.LOCAL : // reading a local variable
				if(this.binding instanceof LocalVariableBinding) {
					LocalVariableBinding local = (LocalVariableBinding) this.binding;
					if (local != null) {
						if (flowInfo.isDefinitelyNull(local))
							return FlowInfo.NULL;
						if (flowInfo.isDefinitelyNonNull(local))
							return FlowInfo.NON_NULL;
						return FlowInfo.UNKNOWN;
					}
				}
		}
		return FlowInfo.NON_NULL; // never get there
	}

	public StringBuffer printExpression(int indent, StringBuffer output){

		return output.append(token);
	}
	public TypeBinding reportError(BlockScope scope) {

		//=====error cases=======
		constant = Constant.NotAConstant;
		if (binding instanceof ProblemFieldBinding) {
			scope.problemReporter().invalidField(this, (FieldBinding) binding);
		} else if (binding instanceof ProblemReferenceBinding) {
			scope.problemReporter().invalidType(this, (TypeBinding) binding);
		} else {
			scope.problemReporter().unresolvableReference(this, binding);
		}
		return null;
	}

	public TypeBinding resolveType(BlockScope scope) {
		return resolveType(scope,false,null);
	}

	public TypeBinding resolveType(BlockScope scope, boolean define, TypeBinding useType) {
		// for code gen, harm the restrictiveFlag
		constant = Constant.NotAConstant;

		this.binding=findBinding(scope);
		
		//if define and the found binding is not valid or is a method then declare a local variable
		if (define && (!this.binding.isValidBinding() || this.binding.kind() == Binding.METHOD)) {
			LocalDeclaration localDeclaration = new LocalDeclaration(this.token,this.sourceStart,this.sourceEnd);
			LocalVariableBinding localBinding=new LocalVariableBinding(localDeclaration,TypeBinding.UNKNOWN,0,false);
		    scope.compilationUnitScope().addLocalVariable(localBinding);
			this.binding=localBinding;
		}
		
		/* if we could not find a binding try finding one for the anonymous global
		 * type that MAYBE associated with this single name reference */
		if(binding == null || !binding.isValidBinding()) {
			char[] typeName = InferEngine.createAnonymousGlobalTypeName(this.token);
			this.binding = scope.getBinding(typeName, (Binding.TYPE | bits) & RestrictiveFlagMASK, this, true);
		}

		if (this.binding.isValidBinding()) {
			switch (bits & RestrictiveFlagMASK) {
				case Binding.FIELD:
				case Binding.LOCAL : // =========only variable============
				case Binding.VARIABLE : // =========only variable============
				case Binding.LOCAL | Binding.TYPE : //====both variable and type============
				case Binding.VARIABLE | Binding.TYPE : //====both variable and type============
					if (binding instanceof VariableBinding) {
						VariableBinding variable = (VariableBinding) binding;
						if (binding instanceof LocalVariableBinding) {
							bits &= ~RestrictiveFlagMASK;  // clear bits
							bits |= Binding.LOCAL;
							TypeBinding fieldType = variable.type;
								
							if (useType!=null && !(useType.id==T_null ||useType.id==T_any || useType.id==T_undefined))
							{
								if (define)
								{
									fieldType=variable.type=useType;
								}
								else
								{
									if (fieldType==TypeBinding.UNKNOWN)
										fieldType=variable.type=useType;
									else if (!fieldType.isCompatibleWith(useType))
										fieldType=variable.type=TypeBinding.ANY;
								}
							}
						
							constant = Constant.NotAConstant;
							

							return this.resolvedType = fieldType;
						}
						// perform capture conversion if read access
						TypeBinding fieldType = checkFieldAccess(scope);
						if (fieldType.isAnonymousType())
							bits |= Binding.TYPE;
						
						return this.resolvedType = fieldType;
					}

					if (binding instanceof MethodBinding)
					{
						return ((MethodBinding)binding).functionTypeBinding;
					}
					else
					{
					// thus it was a type
						bits &= ~RestrictiveFlagMASK;  // clear bits
						bits |= Binding.TYPE;
					}

				case Binding.TYPE : //========only type==============
					constant = Constant.NotAConstant;
					TypeBinding type = null;
					switch (binding.kind()) {
						case Binding.VARIABLE :
							type = ((VariableBinding) binding).type;
							break;
						case Binding.METHOD :
							type = ((MethodBinding) binding).returnType;
							break;
						case Binding.TYPE :
							type = (TypeBinding) binding;
							break;
					}
					
					if(type != null) {
						if (isTypeUseDeprecated(type, scope)) {
							scope.problemReporter().deprecatedType(type, this);
						}
						return this.resolvedType = type;
					}
			}
		}

		// error scenarii
		return this.resolvedType = this.reportError(scope);
	}

	public Binding findBinding(BlockScope scope) {
		return scope.getBinding(token, (Binding.TYPE | Binding.METHOD | bits) & RestrictiveFlagMASK, this, true /*resolve*/);
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	public String unboundReferenceErrorName(){

		return new String(token);
	}
	
	public TypeBinding resolveForAllocation(BlockScope scope, ASTNode location)
	{
		char[] memberName = this.token;
		TypeBinding typeBinding=null;
		this.binding=	
				scope.getBinding(memberName, (Binding.TYPE|Binding.METHOD | bits)  & RestrictiveFlagMASK, this, true /*resolve*/);
		if (binding instanceof TypeBinding)
			typeBinding=(TypeBinding)binding;
		else if (binding instanceof MethodBinding)
			typeBinding=((MethodBinding)binding).returnType;
		else if (binding!=null && !binding.isValidBinding())
		{
			typeBinding=new ProblemReferenceBinding(memberName,null,binding.problemId());
		}
		return typeBinding;
	}
	public int getASTType() {
		return IASTNode.SINGLE_NAME_REFERENCE;
	
	}
}
