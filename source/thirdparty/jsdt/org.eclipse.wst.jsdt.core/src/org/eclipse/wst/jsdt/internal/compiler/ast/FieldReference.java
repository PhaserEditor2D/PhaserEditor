/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFieldReference;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FunctionTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalFunctionBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

public class FieldReference extends Reference implements InvocationSite, IFieldReference {

	public Expression receiver;
	public char[] token;
	
	/**
	 * <p>
	 * exact binding resulting from lookup
	 * </p>
	 */
	public FieldBinding binding;
	
	/**
	 * <p>
	 * exact binding resulting from lookup
	 * </p>
	 */
	public TypeBinding typeBinding;

	/**
	 * (start<<32)+end
	 */
	public long nameSourcePosition;
	public TypeBinding receiverType;

	public FieldReference(char[] source, long pos) {
		token = source;
		nameSourcePosition = pos;
		// by default the position are the one of the field (not true for super access)
		sourceStart = (int) (pos >>> 32);
		sourceEnd = (int) (pos & 0x00000000FFFFFFFFL);
		bits |= Binding.FIELD;
	}

	public FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean isCompound) {
		if (receiver instanceof SingleNameReference && ((SingleNameReference) receiver).binding instanceof LocalVariableBinding) {
			flowInfo.markAsDefinitelyNonNull((LocalVariableBinding) ((SingleNameReference) receiver).binding);
			flowInfo.markAsDefinitelyAssigned((LocalVariableBinding) ((SingleNameReference) receiver).binding);
		}
		flowInfo = receiver.analyseCode(currentScope, flowContext, flowInfo, binding == null || !binding.isStatic()).unconditionalInits();
		if (assignment.expression != null) {
			flowInfo = assignment.expression.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
		}

		return flowInfo;
	}

	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		return analyseCode(currentScope, flowContext, flowInfo, true);
	}

	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, boolean valueRequired) {
		boolean nonStatic = binding == null || !binding.isStatic();
		receiver.analyseCode(currentScope, flowContext, flowInfo, nonStatic);
		if (nonStatic) {
			receiver.checkNPE(currentScope, flowContext, flowInfo);
		}

		return flowInfo;
	}

	public FieldBinding fieldBinding() {
		return binding;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#genericTypeArguments()
	 */
	public TypeBinding[] genericTypeArguments() {
		return null;
	}

	public boolean isSuperAccess() {
		return receiver.isSuper();
	}

	public boolean isTypeAccess() {
		return receiver != null && receiver.isTypeReference();
	}

	public int nullStatus(FlowInfo flowInfo) {
		return FlowInfo.UNKNOWN;
	}

	public Constant optimizedBooleanConstant() {
		return Constant.NotAConstant;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {
		return receiver.printExpression(0, output).append('.').append(token);
	}


	public TypeBinding resolveType(BlockScope scope) {
		return resolveType(scope, false, null);
	}

	public TypeBinding resolveType(BlockScope scope, boolean define, TypeBinding useType) {
		/*
		 * Handle if this is a reference to the prototype of a type
		 * 
		 * By default, the prototype is of type Object, but if there is an
		 * InferredType for the receiver, it should yeild the receiver type.
		 */
		if (this.isPrototype()) {
			// check if receiver type is defined
			if ((this.receiverType = receiver.resolveType(scope)) == null) {
				constant = Constant.NotAConstant;
				return null;
			}

			// construct the name of the type based on the receiver
			char[] possibleTypeName = Util.getTypeName(receiver);
			TypeBinding typeBinding = scope.getJavaLangObject();
			if (possibleTypeName != null) {
				Binding possibleTypeBinding = scope.getBinding(possibleTypeName, Binding.TYPE & RestrictiveFlagMASK, this, true /* resolve */);

				if (possibleTypeBinding.isValidBinding()) {
					typeBinding = (TypeBinding) possibleTypeBinding;
				}
				char[] fieldname = new char[]{'p', 'r', 'o', 't', 'o', 't', 'y', 'p', 'e'};
				this.binding = scope.getJavaLangObject().getField(fieldname, true);
				constant = Constant.NotAConstant;
				return this.resolvedType = typeBinding;
			}

		}

		char[] possibleTypeName = Util.getTypeName(this);
		Binding possibleTypeBinding = null;
		if (possibleTypeName != null) {
			possibleTypeBinding = scope.getBinding(possibleTypeName, Binding.TYPE & RestrictiveFlagMASK, this, true /* resolve */);
		}

		if (possibleTypeBinding != null && possibleTypeBinding.isValidBinding() && (TypeBinding) possibleTypeBinding != scope.getJavaLangObject()) {
			this.typeBinding = (TypeBinding) possibleTypeBinding;
			constant = Constant.NotAConstant;
			this.bits |= Binding.TYPE;
			return this.typeBinding;
		}

		/* if this could be a qualified type name, first check if receiver is
 		 * defined, and if not look up as type name */
		this.receiverType = this.receiver.resolveType(scope);

		if (this.receiverType == null || this.receiverType == scope.getJavaLangObject()) {
			if (possibleTypeBinding != null && possibleTypeBinding.isValidBinding()) {
				this.typeBinding = (TypeBinding) possibleTypeBinding;
				this.bits |= Binding.TYPE;
				return this.typeBinding;
			}
			else {
				this.binding = new ProblemFieldBinding(null, this.token, ProblemReasons.NotFound);
				constant = Constant.NotAConstant;
				this.resolvedType = TypeBinding.ANY;
			}
			return null;
		}

		/* Need to look in the fields and method for a match... In JS there is
		 * no distinction between member functions or field. We are trying to
		 * mimic that property below (Java does have a distinction) */
		if (this.receiverType.id == TypeIds.T_any) {
			constant = Constant.NotAConstant;
			this.binding = new ProblemFieldBinding(null, token, ProblemReasons.NotFound);
			return this.resolvedType = TypeBinding.ANY;
		}

		Binding memberBinding = scope.getFieldOrMethod(this.receiverType, token, this);
		boolean receiverIsType = (receiver instanceof NameReference || receiver instanceof FieldReference || receiver instanceof ThisReference) && (receiver.bits & Binding.TYPE) != 0;
		if (!memberBinding.isValidBinding() && (this.receiverType != null && this.receiverType.isFunctionType())) {
			Binding alternateBinding = receiver.alternateBinding();
			if (alternateBinding instanceof TypeBinding) {
				this.receiverType = (TypeBinding) alternateBinding;
				memberBinding = scope.getFieldOrMethod(this.receiverType, token, this);
				receiverIsType = true;
			}
		}

		constant = Constant.NotAConstant;
		if (memberBinding instanceof FieldBinding) {
			FieldBinding fieldBinding = /* this.codegenBinding = */this.binding = (FieldBinding) memberBinding;
			if (!fieldBinding.isValidBinding()) {
				this.binding = fieldBinding;
				this.resolvedType = TypeBinding.ANY;
				if (!define) {
					constant = Constant.NotAConstant;
					scope.problemReporter().invalidField(this, this.receiverType);
					return null;
				}
				else {
					// should add binding here
				}
			}
			if (JavaScriptCore.IS_ECMASCRIPT4) {
				TypeBinding receiverErasure = this.receiverType;
				if (receiverErasure instanceof ReferenceBinding) {
					if (receiverErasure.findSuperTypeWithSameErasure(fieldBinding.declaringClass) == null) {
						// handle indirect inheritance thru variable secondary bound
						this.receiverType = fieldBinding.declaringClass; 
					}
				}
			}
			if (isFieldUseDeprecated(fieldBinding, scope, (this.bits & IsStrictlyAssigned) != 0)) {
				scope.problemReporter().deprecatedField(fieldBinding, this);
			}
			boolean isImplicitThisRcv = receiver.isImplicitThis();
			constant = Constant.NotAConstant;
			if (fieldBinding.isStatic()) {
				// static field accessed through receiver? legal but unoptimal (optional warning)
				if (!(isImplicitThisRcv || receiverIsType)) {
					scope.problemReporter().nonStaticAccessToStaticField(this, fieldBinding);
				}
				if (!isImplicitThisRcv && fieldBinding.declaringClass != receiverType && fieldBinding.declaringClass.canBeSeenBy(scope)) {
					scope.problemReporter().indirectAccessToStaticField(this, fieldBinding);
				}
			}
			else {
				if (receiverIsType) {
					scope.problemReporter().staticFieldAccessToNonStaticVariable(this, fieldBinding);
				}
			}
			
			//if there is a given useType and the field is not valid, create a valid binding
			if(useType != null && !fieldBinding.isValidBinding()) {
				fieldBinding = new FieldBinding(fieldBinding, fieldBinding.declaringClass);
				if(fieldBinding.declaringClass instanceof SourceTypeBinding) {
					((SourceTypeBinding)fieldBinding.declaringClass).addField(fieldBinding);
				}
				this.binding = fieldBinding;
			}
			
			//set use type
			if(useType != null) {
				fieldBinding.type = useType;
				
				//add as a function binding as well if there is not already a function binding by the same name
				if(useType.isFunctionType() && fieldBinding.declaringClass instanceof SourceTypeBinding) {
					SourceTypeBinding declaringBinding = (SourceTypeBinding)fieldBinding.declaringClass;
					InferredMethod dupMeth = declaringBinding.getInferredType().findMethod(this.getToken(), null);
					if(dupMeth == null) {
						MethodBinding[] funcBindings = declaringBinding.getMethods(this.getToken());
						if(funcBindings == null || funcBindings.length == 0) {
							MethodBinding methBinding = new MethodBinding(
										((FunctionTypeBinding)useType).functionBinding, fieldBinding.declaringClass);
							methBinding.selector = fieldBinding.name;
							
							if(methBinding.declaringClass instanceof SourceTypeBinding) {
								((SourceTypeBinding)methBinding.declaringClass).addMethod(methBinding);
							}
						}
					}
				}
			}
			
			// perform capture conversion if read access
			return this.resolvedType = fieldBinding.type;
		}
		else if (memberBinding instanceof MethodBinding) {
			MethodBinding methodBinding = (MethodBinding) memberBinding;

			if (!methodBinding.isStatic() || memberBinding instanceof LocalFunctionBinding) {
				if (receiverIsType && methodBinding.isValidBinding() && !methodBinding.isConstructor()) {
					if (this.receiverType == null || !this.receiverType.isAnonymousType())
						scope.problemReporter().mustUseAStaticMethod(this, methodBinding);
				}
			}
			else {
				if (!receiverIsType && methodBinding.isValidBinding())
					scope.problemReporter().nonStaticAccessToStaticMethod(this, methodBinding);

			}

			this.resolvedType = methodBinding.functionTypeBinding;
			this.binding = new FieldBinding(((MethodBinding) memberBinding).selector, this.receiverType, ((MethodBinding) memberBinding).modifiers, methodBinding.declaringClass);
			if (memberBinding.isValidBinding()) {
				return this.resolvedType;
			}
			return null;
		}

		return null;
	}

	public void setActualReceiverType(ReferenceBinding receiverType) {
		// ignored
	}

	public void setDepth(int depth) {
		bits &= ~DepthMASK; // flush previous depth if any
		if (depth > 0) {
			bits |= (depth & 0xFF) << DepthSHIFT; // encoded on 8 bits
		}
	}

	public void setFieldIndex(int index) {
		// ignored
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			receiver.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}

	public boolean isPrototype() {
		return (CharOperation.equals(TypeConstants.PROTOTYPE, this.token));
	}


	public TypeBinding resolveForAllocation(BlockScope scope, ASTNode location) {
		char[][] qualifiedName = asQualifiedName();
		TypeBinding typeBinding = null;
		if (qualifiedName != null) {
			typeBinding = scope.getType(CharOperation.concatWith(qualifiedName, '.'));
		}
		if (typeBinding == null || !typeBinding.isValidBinding()) {
			this.receiverType = receiver.resolveType(scope);
			if (this.receiverType == null) {
				this.binding = new ProblemFieldBinding(null, this.token, ProblemReasons.NotFound);
				constant = Constant.NotAConstant;
				this.resolvedType = TypeBinding.ANY;
				return null;
			}
			Binding memberBinding = scope.getFieldOrMethod(this.receiverType, token, this);
			if (memberBinding instanceof MethodBinding && memberBinding.isValidBinding()) {
				this.resolvedType = ((MethodBinding) memberBinding).allocationType;
				this.binding = new ProblemFieldBinding(null, this.token, ProblemReasons.NotFound);
				if (memberBinding.isValidBinding())
					return this.resolvedType;
			}

		}
		if (typeBinding == null) {
			if (qualifiedName == null)
				qualifiedName = new char[][]{token};
			typeBinding = new ProblemReferenceBinding(qualifiedName, null, ProblemReasons.NotFound);
		}
		return typeBinding;
	}

	public int getASTType() {
		return IASTNode.FIELD_REFERENCE;

	}

	public char[][] asQualifiedName() {
		ArrayList list = new ArrayList();
		list.add(token);
		FieldReference fieldReference = this;
		while (fieldReference != null) {
			if (fieldReference.receiver instanceof SingleNameReference) {
				list.add(0, ((SingleNameReference) fieldReference.receiver).token);
				fieldReference = null;
			}
			else if (fieldReference.receiver instanceof FieldReference) {
				fieldReference = (FieldReference) fieldReference.receiver;
				list.add(0, fieldReference.token);
			}
		else if (fieldReference.receiver instanceof ThisReference) {
			//use the inferred type name of "this" as the next segment
			InferredType type = ((ThisReference)fieldReference.receiver).getInferredType();
			if(type != null) {
				list.add(0, type.getName());
			} else {
				//if do not have a type for "this" then can't build the fully qualified name
				return null;
			}
			
			fieldReference = null;
		}
		else {
			return null;
		}
	}
		return (char[][]) list.toArray(new char[list.size()][]);
	}

	public IExpression getReceiver() {
		return receiver;
	}

	public char[] getToken() {
		return token;
	}

	public boolean isTypeReference() {
		return (this.bits & Binding.TYPE) == Binding.TYPE;
	}
}