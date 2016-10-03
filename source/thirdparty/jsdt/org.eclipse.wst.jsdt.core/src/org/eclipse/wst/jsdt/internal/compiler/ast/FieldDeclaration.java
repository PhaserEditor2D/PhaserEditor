/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class FieldDeclaration extends AbstractVariableDeclaration implements IFieldDeclaration {

	public FieldBinding binding;
	// is in super public Javadoc javadoc;

	// allows to retrieve both the "type" part of the declaration (part1)
	// and also the part that decribe the name and the init and optionally
	// some other dimension ! ....
	// public int[] a, b[] = X, c ;
	// for b that would give for
	// - part1 : public int[]
	// - part2 : b[] = X,

	public int endPart1Position;
	public int endPart2Position;

	public FieldDeclaration() {
		// for subtypes or conversion
	}

	public FieldDeclaration(char[] name, int sourceStart, int sourceEnd) {
		this.name = name;
		// due to some declaration like
		// int x, y = 3, z , x ;
		// the sourceStart and the sourceEnd is ONLY on the name
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
	}

	public FlowInfo analyseCode(MethodScope initializationScope, FlowContext flowContext, FlowInfo flowInfo) {
		if (this.binding != null && !this.binding.isUsed()) {
			if (this.binding.isPrivate() || (this.binding.declaringClass != null && this.binding.declaringClass.isLocalType())) {
				if (!initializationScope.referenceCompilationUnit().compilationResult.hasSyntaxError) {
					initializationScope.problemReporter().unusedPrivateField(this);
				}
			}
		}

		if (this.initialization != null) {
			flowInfo = this.initialization.analyseCode(initializationScope, flowContext, flowInfo).unconditionalInits();
			flowInfo.markAsDefinitelyAssigned(this.binding);
		}
		return flowInfo;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration#getKind()
	 */
	public int getKind() {
		return FIELD;
	}

	public boolean isStatic() {
		if (this.binding != null) {
			return this.binding.isStatic();
		}
		return (this.modifiers & ClassFileConstants.AccStatic) != 0;
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {
		if (this.javadoc != null) {
			this.javadoc.print(indent, output);
		}
		return super.printStatement(indent, output);
	}

	public void resolve(MethodScope initializationScope) {
		// the two <constant = Constant.NotAConstant> could be regrouped into
		// a single line but it is clearer to have two lines while the reason
		// of their existence is not at all the same. See comment for the second one.

		// --------------------------------------------------------
		if ((this.bits & ASTNode.HasBeenResolved) != 0) {
			return;
		}
		
		if (this.binding == null || !this.binding.isValidBinding()) {
			return;
		}

		this.bits |= ASTNode.HasBeenResolved;

		// check if field is hiding some variable - issue is that field
		// binding already got inserted in scope
		// thus must lookup separately in super type and outer context
		ClassScope classScope = initializationScope.enclosingClassScope();

		if (classScope != null) {
			checkHiding : {
				SourceTypeBinding declaringType = classScope.enclosingSourceType();
				checkHidingSuperField : {
					if (declaringType.getSuperBinding0() == null) {
						break checkHidingSuperField;
					}
					Binding existingVariable = classScope.findField(declaringType.getSuperBinding0(), this.name, this, false);
					if (existingVariable == null) {
						// keep checking outer scenario
						break checkHidingSuperField; 
					}

					if (!existingVariable.isValidBinding()) {
						 // keep checking outer scenario
						break checkHidingSuperField;
					}

					if (existingVariable instanceof FieldBinding) {
						FieldBinding existingField = (FieldBinding) existingVariable;
						if (existingField.original() == this.binding) {
							// keep checking outer scenario
							break checkHidingSuperField; 
						}
					}
					// collision with supertype field
					initializationScope.problemReporter().fieldHiding(this, existingVariable);
					break checkHiding; // already found a matching field
				}
				// only corner case is: lookup of outer field through static
				// declaringType, which isn't detected by #getBinding as lookup starts
				// from outer scope. Subsequent static contexts are detected for free.
				Scope outerScope = classScope.parent;
				if (outerScope.kind == Scope.COMPILATION_UNIT_SCOPE) {
					break checkHiding;
				}
				Binding existingVariable = outerScope.getBinding(this.name, Binding.VARIABLE, this, false);
				if (existingVariable == null) {
					break checkHiding;
				}
				
				if (!existingVariable.isValidBinding()) {
					break checkHiding;
				}
				
				if (existingVariable == this.binding) {
					break checkHiding;
				}
				
				if (existingVariable instanceof FieldBinding) {
					FieldBinding existingField = (FieldBinding) existingVariable;
					if (existingField.original() == this.binding) {
						break checkHiding;
					}
					
					if (!existingField.isStatic() && declaringType.isStatic()) {
						break checkHiding;
					}
				}
				// collision with outer field or local variable
				initializationScope.problemReporter().fieldHiding(this, existingVariable);
			}
		}

		// enum constants have no declared type
		if (this.type != null) {
			// update binding for type reference
			this.type.resolvedType = this.binding.type;
		}

		FieldBinding previousField = initializationScope.initializedField;
		int previousFieldID = initializationScope.lastVisibleFieldID;
		try {
			initializationScope.initializedField = this.binding;
			initializationScope.lastVisibleFieldID = this.binding.id;

			// the resolution of the initialization hasn't been done
			if (this.initialization != null) {

				TypeBinding fieldType = this.binding.type;
				TypeBinding initializationType;
				// needed in case of generic method invocation
				this.initialization.setExpectedType(fieldType); 
				if (this.initialization instanceof ArrayInitializer) {
					if ((initializationType = this.initialization.resolveTypeExpecting(initializationScope, fieldType)) != null) {
						((ArrayInitializer) this.initialization).binding = (ArrayBinding) initializationType;
					}
				}
				else if ((initializationType = this.initialization.resolveType(initializationScope)) != null) {
					// must call before computeConversion() and typeMismatchError()
					if (fieldType != initializationType) {
						initializationScope.compilationUnitScope().recordTypeConversion(fieldType, initializationType);
					}
					if (this.initialization.isConstantValueOfTypeAssignableToType(initializationType, fieldType) ||
								(fieldType.isBaseType() && BaseTypeBinding.isWidening(fieldType.id, initializationType.id)) ||
								initializationType.isCompatibleWith(fieldType)) {
						
					}
					else if (initializationScope.isBoxingCompatibleWith(initializationType, fieldType) || (initializationType.isBaseType() 
								&& initializationScope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5 // autoboxing
								&& !fieldType.isBaseType()
								&& initialization.isConstantValueOfTypeAssignableToType(initializationType, initializationScope.environment().computeBoxingType(fieldType)))) {
					}
					else {
						initializationScope.problemReporter().typeMismatchError(initializationType, fieldType, this);
					}
				}
				// check for assignment with no effect
				if (this.binding == Assignment.getDirectBinding(this.initialization)) {
					initializationScope.problemReporter().assignmentHasNoEffect(this, this.name);
				}
			}
			// Resolve Javadoc comment if one is present
			if (this.javadoc != null) {
				this.javadoc.resolve(initializationScope);
			}
			else if (this.binding.declaringClass != null && !this.binding.declaringClass.isLocalType()) {
				initializationScope.problemReporter().javadocMissing(this.sourceStart, this.sourceEnd, this.binding.modifiers);
			}
		}
		finally {
			initializationScope.initializedField = previousField;
			initializationScope.lastVisibleFieldID = previousFieldID;
		}
	}

	public void traverse(ASTVisitor visitor, MethodScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.javadoc != null) {
				this.javadoc.traverse(visitor, scope);
			}
			if (this.type != null) {
				this.type.traverse(visitor, scope);
			}
			if (this.initialization != null) {
				this.initialization.traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}

	public int getASTType() {
		return IASTNode.FIELD_DECLARATION;

	}

	public IExpression getInitialization() {
		return this.initialization;
	}
}