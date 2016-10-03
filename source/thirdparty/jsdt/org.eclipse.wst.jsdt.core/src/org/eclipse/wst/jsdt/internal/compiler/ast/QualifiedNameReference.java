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
import org.eclipse.wst.jsdt.core.ast.IQualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;
import org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding;

public class QualifiedNameReference extends NameReference implements IQualifiedNameReference {

	public char[][] tokens;
	public long[] sourcePositions;
	public FieldBinding[] otherBindings, otherCodegenBindings;
	int[] otherDepths;
	public int indexOfFirstFieldBinding;//points (into tokens) for the first token that corresponds to first FieldBinding
	public TypeBinding genericCast;
	public TypeBinding[] otherGenericCasts;

public QualifiedNameReference(	char[][] tokens, long[] positions, int sourceStart, int sourceEnd) {
	this.tokens = tokens;
	this.sourcePositions = positions;
	this.sourceStart = sourceStart;
	this.sourceEnd = sourceEnd;
}

public FlowInfo analyseAssignment(BlockScope currentScope, 	FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean isCompound) {
	// determine the rank until which we now we do not need any actual value for the field access
	int otherBindingsCount = this.otherBindings == null ? 0 : this.otherBindings.length;
	boolean needValue = otherBindingsCount == 0 || !this.otherBindings[0].isStatic();
	boolean complyTo14 = currentScope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4;
	FieldBinding lastFieldBinding = null;
	switch (this.bits & ASTNode.RestrictiveFlagMASK) {
		case Binding.FIELD : // reading a field
			lastFieldBinding = (FieldBinding) this.binding;
			break;
		case Binding.LOCAL :
			// first binding is a local variable
			LocalVariableBinding localBinding;
			if (!flowInfo
				.isDefinitelyAssigned(localBinding = (LocalVariableBinding) this.binding)) {
				currentScope.problemReporter().uninitializedLocalVariable(localBinding, this);
			}
			if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0)	{
				localBinding.useFlag = LocalVariableBinding.USED;
			} else if (localBinding.useFlag == LocalVariableBinding.UNUSED) {
				localBinding.useFlag = LocalVariableBinding.FAKE_USED;
			}
			checkNPE(currentScope, flowContext, flowInfo, true);
	}

	if (needValue) {
		manageEnclosingInstanceAccessIfNecessary(currentScope, flowInfo);
		// only for first binding
	}
	// all intermediate field accesses are read accesses
	if (this.otherBindings != null) {
		for (int i = 0; i < otherBindingsCount-1; i++) {
			lastFieldBinding = this.otherBindings[i];
			needValue = !this.otherBindings[i+1].isStatic();
		}
		lastFieldBinding = this.otherBindings[otherBindingsCount-1];
	}

	if (isCompound) {
		TypeBinding lastReceiverType;
		switch (otherBindingsCount) {
			case 0 :
				lastReceiverType = this.actualReceiverType;
				break;
			case 1 :
				lastReceiverType = ((VariableBinding)this.binding).type;
				break;
			default:
				lastReceiverType = this.otherBindings[otherBindingsCount-2].type;
				break;
		}
	}

	if (assignment.expression != null) {
		flowInfo =
			assignment
				.expression
				.analyseCode(currentScope, flowContext, flowInfo)
				.unconditionalInits();
	}

	// equivalent to valuesRequired[maxOtherBindings]
	TypeBinding lastReceiverType;
	switch (otherBindingsCount) {
		case 0 :
			lastReceiverType = this.actualReceiverType;
			break;
		case 1 :
			lastReceiverType = ((VariableBinding)this.binding).type;
			break;
		default :
			lastReceiverType = this.otherBindings[otherBindingsCount-2].type;
			break;
	}

	return flowInfo;
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return analyseCode(currentScope, flowContext, flowInfo, true);
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, boolean valueRequired) {
	// determine the rank until which we now we do not need any actual value for the field access
	int otherBindingsCount = this.otherBindings == null ? 0 : this.otherBindings.length;

	boolean needValue = otherBindingsCount == 0 ? valueRequired : !this.otherBindings[0].isStatic();
	boolean complyTo14 = currentScope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4;
	switch (this.bits & ASTNode.RestrictiveFlagMASK) {
		case Binding.FIELD : // reading a field
			break;
		case Binding.LOCAL : // reading a local variable
			LocalVariableBinding localBinding;
			if (!flowInfo
				.isDefinitelyAssigned(localBinding = (LocalVariableBinding) this.binding)) {
				if(localBinding.declaringScope instanceof CompilationUnitScope)
					currentScope.problemReporter().uninitializedGlobalVariable(localBinding, this);
				else
					currentScope.problemReporter().uninitializedLocalVariable(localBinding, this);
			}
			if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0)	{
				localBinding.useFlag = LocalVariableBinding.USED;
			} else if (localBinding.useFlag == LocalVariableBinding.UNUSED) {
				localBinding.useFlag = LocalVariableBinding.FAKE_USED;
			}
			checkNPE(currentScope, flowContext, flowInfo, true);
	}
	if (needValue) {
		manageEnclosingInstanceAccessIfNecessary(currentScope, flowInfo);
		// only for first binding (if value needed only)
	}
	if (this.otherBindings != null) {
		for (int i = 0; i < otherBindingsCount; i++) {
			needValue = i < otherBindingsCount-1 ? !this.otherBindings[i+1].isStatic() : valueRequired;
			if (needValue || complyTo14) {
				TypeBinding lastReceiverType = getGenericCast(i);
				if (lastReceiverType == null) {
					if (i == 0) {
						 lastReceiverType = ((VariableBinding)this.binding).type;
					} else {
						lastReceiverType = this.otherBindings[i-1].type;
					}
				}
			}
		}
	}
	return flowInfo;
}

/**
 * Check and/or redirect the field access to the delegate receiver if any
 */
public TypeBinding checkFieldAccess(BlockScope scope) {
	FieldBinding fieldBinding = (FieldBinding) this.binding;
	MethodScope methodScope = scope.methodScope();
	// check for forward references
	if (this.indexOfFirstFieldBinding == 1
			&& methodScope.enclosingSourceType() == fieldBinding.original().declaringClass
			&& methodScope.lastVisibleFieldID >= 0
			&& fieldBinding.id >= methodScope.lastVisibleFieldID
			&& (!fieldBinding.isStatic() || methodScope.isStatic)) {
		scope.problemReporter().forwardReference(this, 0, methodScope.enclosingSourceType());
	}
	this.bits &= ~ASTNode.RestrictiveFlagMASK; // clear bits
	this.bits |= Binding.FIELD;
	return getOtherFieldBindings(scope);
}

public void checkNPE(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, boolean checkString) {
	// cannot override localVariableBinding because this would project o.m onto o when
	// analysing assignments
	if ((this.bits & ASTNode.RestrictiveFlagMASK) == Binding.LOCAL) {
		LocalVariableBinding local = (LocalVariableBinding) this.binding;
		if (local != null &&
			(local.type.tagBits & TagBits.IsBaseType) == 0 &&
			(checkString || local.type.id != TypeIds.T_JavaLangString)) {
			if ((this.bits & ASTNode.IsNonNull) == 0) {
				flowContext.recordUsingNullReference(scope, local, this,
					FlowContext.MAY_NULL, flowInfo);
			}
			flowInfo.markAsComparedEqualToNonNull(local);
				// from thereon it is set
			if (flowContext.initsOnFinally != null) {
				flowContext.initsOnFinally.markAsComparedEqualToNonNull(local);
			}
		}
	}
}

/**
 * @see org.eclipse.wst.jsdt.internal.compiler.ast.Expression#computeConversion(org.eclipse.wst.jsdt.internal.compiler.lookup.Scope, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)
 */
public void computeConversion(Scope scope, TypeBinding runtimeTimeType, TypeBinding compileTimeType) {
	if (runtimeTimeType == null || compileTimeType == null)
		return;
	// set the generic cast after the fact, once the type expectation is fully known (no need for strict cast)
	FieldBinding field = null;
	int length = this.otherBindings == null ? 0 : this.otherBindings.length;
	if (length == 0) {
		if ((this.bits & Binding.FIELD) != 0 && this.binding != null && this.binding.isValidBinding()) {
			field = (FieldBinding) this.binding;
		}
	} else {
		field  = this.otherBindings[length-1];
	}
	if (field != null) {
		FieldBinding originalBinding = field.original();
	}
}

/**
 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#genericTypeArguments()
 */
public TypeBinding[] genericTypeArguments() {
	return null;
}

// get the matching codegenBinding
protected FieldBinding getCodegenBinding(int index) {
//  if (index == 0){
//		return (FieldBinding)this.codegenBinding;
//	} else {
//		return this.otherCodegenBindings[index-1];
//	}
	return (FieldBinding)this.binding;
}

// get the matching generic cast
protected TypeBinding getGenericCast(int index) {
   if (index == 0){
		return this.genericCast;
	} else {
	    if (this.otherGenericCasts == null) return null;
		return this.otherGenericCasts[index-1];
	}
}

public TypeBinding getOtherFieldBindings(BlockScope scope) {
	// At this point restrictiveFlag may ONLY have two potential value : FIELD LOCAL (i.e cast <<(VariableBinding) binding>> is valid)
	int length = this.tokens.length;
	FieldBinding field;
	if ((this.bits & Binding.FIELD) != 0) {
		field = (FieldBinding) this.binding;
		if (!field.isStatic()) {
			//must check for the static status....
			if (this.indexOfFirstFieldBinding > 1  //accessing to a field using a type as "receiver" is allowed only with static field
					 || scope.methodScope().isStatic) { 	// the field is the first token of the qualified reference....
				scope.problemReporter().staticFieldAccessToNonStaticVariable(this, field);
				return null;
			 }
		} else {
			// indirect static reference ?
			if (this.indexOfFirstFieldBinding > 1
					&& field.declaringClass != this.actualReceiverType
					&& field.declaringClass.canBeSeenBy(scope)) {
				scope.problemReporter().indirectAccessToStaticField(this, field);
			}
		}
		// only last field is actually a write access if any
		if (isFieldUseDeprecated(field, scope, (this.bits & ASTNode.IsStrictlyAssigned) != 0 && this.indexOfFirstFieldBinding == length))
			scope.problemReporter().deprecatedField(field, this);
	} else {
		field = null;
	}
	TypeBinding type = ((VariableBinding) this.binding).type;
	int index = this.indexOfFirstFieldBinding;
	if (index == length) { //	restrictiveFlag == FIELD
		// perform capture conversion if read access
		return type;
	}
	// allocation of the fieldBindings array	and its respective constants
	int otherBindingsLength = length - index;
	this.otherCodegenBindings = this.otherBindings = new FieldBinding[otherBindingsLength];
	this.otherDepths = new int[otherBindingsLength];
	
	// save first depth, since will be updated by visibility checks of other bindings
	int firstDepth = (this.bits & ASTNode.DepthMASK) >> ASTNode.DepthSHIFT;
	// iteration on each field
	while (index < length) {
		char[] token = this.tokens[index];
		if (type == null)
			return null; // could not resolve type prior to this point

		this.bits &= ~ASTNode.DepthMASK; // flush previous depth if any
		FieldBinding previousField = field;
		field = scope.getField(type, token, this);
		int place = index - this.indexOfFirstFieldBinding;
		this.otherBindings[place] = field;
		this.otherDepths[place] = (this.bits & ASTNode.DepthMASK) >> ASTNode.DepthSHIFT;
		if (field.isValidBinding()) {
			// set generic cast of for previous field (if any)
			if (previousField != null) {
				TypeBinding fieldReceiverType = type;
				TypeBinding receiverErasure = type;
				if (receiverErasure instanceof ReferenceBinding) {
					if (receiverErasure.findSuperTypeWithSameErasure(field.declaringClass) == null) {
						fieldReceiverType = field.declaringClass; // handle indirect inheritance thru variable secondary bound
					}
				}
				FieldBinding originalBinding = previousField.original();
		    }
			// only last field is actually a write access if any
			if (isFieldUseDeprecated(field, scope, (this.bits & ASTNode.IsStrictlyAssigned) !=0 && index+1 == length)) {
				scope.problemReporter().deprecatedField(field, this);
			}

			if (field.isStatic()) {
				// static field accessed through receiver? legal but unoptimal (optional warning)
				scope.problemReporter().nonStaticAccessToStaticField(this, field);
				// indirect static reference ?
				if (field.declaringClass != type) {
					scope.problemReporter().indirectAccessToStaticField(this, field);
				}
			}
			type = field.type;
			index++;
		} else {
			this.constant = Constant.NotAConstant; //don't fill other constants slots...
			scope.problemReporter().invalidField(this, field, index, type);
			setDepth(firstDepth);
			return null;
		}
	}
	setDepth(firstDepth);
	type = (this.otherBindings[otherBindingsLength - 1]).type;
	// perform capture conversion if read access
	return type;
}

public void manageEnclosingInstanceAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo) {
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0)	{
	//If inlinable field, forget the access emulation, the code gen will directly target it
	if (((this.bits & ASTNode.DepthMASK) == 0) || (this.constant != Constant.NotAConstant)) {
		return;
	}
	if ((this.bits & ASTNode.RestrictiveFlagMASK) == Binding.LOCAL) {
		currentScope.emulateOuterAccess((LocalVariableBinding) this.binding);
	}
	}
}

public int nullStatus(FlowInfo flowInfo) {
	return FlowInfo.UNKNOWN;
}

public Constant optimizedBooleanConstant() {
	switch (this.resolvedType.id) {
		case T_boolean :
		case T_JavaLangBoolean :
			if (this.constant != Constant.NotAConstant) return this.constant;
			switch (this.bits & ASTNode.RestrictiveFlagMASK) {
				case Binding.FIELD : // reading a field
				if (this.otherBindings == null)
					return Constant.NotAConstant;
				// fall thru
			case Binding.LOCAL : // reading a local variable
				return Constant.NotAConstant;
		}
	}
	return Constant.NotAConstant;
}

public StringBuffer printExpression(int indent, StringBuffer output) {
	for (int i = 0; i < this.tokens.length; i++) {
		if (i > 0) output.append('.');
		output.append(this.tokens[i]);
	}
	return output;
}

/**
 * Normal field binding did not work, try to bind to a field of the delegate receiver.
 */
public TypeBinding reportError(BlockScope scope) {
	if (this.binding instanceof ProblemFieldBinding) {
		scope.problemReporter().invalidField(this, (FieldBinding) this.binding);
	} else if (this.binding instanceof ProblemReferenceBinding) {
		scope.problemReporter().invalidType(this, (TypeBinding) this.binding);
	} else {
		scope.problemReporter().unresolvableReference(this, this.binding);
	}
	return null;
}

public TypeBinding resolveType(BlockScope scope) {
	// field and/or local are done before type lookups
	// the only available value for the restrictiveFlag BEFORE
	// the TC is Flag_Type Flag_LocalField and Flag_TypeLocalField
	this.actualReceiverType = scope.enclosingReceiverType();
	this.constant = Constant.NotAConstant;
	if ((/*this.codegenBinding =*/ this.binding = scope.getBinding(this.tokens, this.bits & ASTNode.RestrictiveFlagMASK, this, true /*resolve*/)).isValidBinding()) {
		switch (this.bits & ASTNode.RestrictiveFlagMASK) {
			case Binding.VARIABLE : //============only variable===========
			case Binding.TYPE | Binding.VARIABLE :
				if (this.binding instanceof LocalVariableBinding) {
					this.bits &= ~ASTNode.RestrictiveFlagMASK; // clear bits
					this.bits |= Binding.LOCAL;
					return this.resolvedType = getOtherFieldBindings(scope);
				}
				if (this.binding instanceof FieldBinding) {
					FieldBinding fieldBinding = (FieldBinding) this.binding;
					MethodScope methodScope = scope.methodScope();
					// check for forward references
					if (this.indexOfFirstFieldBinding == 1
							&& methodScope.enclosingSourceType() == fieldBinding.original().declaringClass
							&& methodScope.lastVisibleFieldID >= 0
							&& fieldBinding.id >= methodScope.lastVisibleFieldID
							&& (!fieldBinding.isStatic() || methodScope.isStatic)) {
						scope.problemReporter().forwardReference(this, 0, methodScope.enclosingSourceType());
					}
					this.bits &= ~ASTNode.RestrictiveFlagMASK; // clear bits
					this.bits |= Binding.FIELD;

//						// check for deprecated receiver type
//						// deprecation check for receiver type if not first token
//						if (indexOfFirstFieldBinding > 1) {
//							if (isTypeUseDeprecated(this.actualReceiverType, scope))
//								scope.problemReporter().deprecatedType(this.actualReceiverType, this);
//						}

					return this.resolvedType = getOtherFieldBindings(scope);
				}
				// thus it was a type
				this.bits &= ~ASTNode.RestrictiveFlagMASK; // clear bits
				this.bits |= Binding.TYPE;
			case Binding.TYPE : //=============only type ==============
			    TypeBinding type = (TypeBinding) this.binding;
//					if (isTypeUseDeprecated(type, scope))
//						scope.problemReporter().deprecatedType(type, this);
				return this.resolvedType = type;
		}
	}
	//========error cases===============
	return this.resolvedType = this.reportError(scope);
}

// set the matching codegenBinding and generic cast
protected void setCodegenBinding(int index, FieldBinding someCodegenBinding) {
//	if (index == 0){
//		this.codegenBinding = someCodegenBinding;
//	} else {
//	    int length = this.otherBindings.length;
//		if (this.otherCodegenBindings == this.otherBindings){
//			System.arraycopy(this.otherBindings, 0, this.otherCodegenBindings = new FieldBinding[length], 0, length);
//		}
//		this.otherCodegenBindings[index-1] = someCodegenBinding;
//	}
}

public void setFieldIndex(int index) {
	this.indexOfFirstFieldBinding = index;
}

// set the matching codegenBinding and generic cast
protected void setGenericCast(int index, TypeBinding someGenericCast) {
	if (index == 0){
		this.genericCast = someGenericCast;
	} else {
	    if (this.otherGenericCasts == null) {
	        this.otherGenericCasts = new TypeBinding[this.otherBindings.length];
	    }
	    this.otherGenericCasts[index-1] = someGenericCast;
	}
}

public void traverse(ASTVisitor visitor, BlockScope scope) {
	visitor.visit(this, scope);
	visitor.endVisit(this, scope);
}

public void traverse(ASTVisitor visitor, ClassScope scope) {
	visitor.visit(this, scope);
	visitor.endVisit(this, scope);
}

public String unboundReferenceErrorName() {
	return new String(this.tokens[0]);
}
public int getASTType() {
	return IASTNode.QUALIFIED_NAME_REFERENCE;

}
}
