/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.dom;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;
import org.eclipse.wst.jsdt.internal.core.CompilationUnit;

/**
 * This class represents the recovered binding for a type
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
class RecoveredTypeBinding implements ITypeBinding {

	private VariableDeclaration variableDeclaration;
	private Type currentType;
	private BindingResolver resolver;
	private int dimensions;
	private RecoveredTypeBinding innerTypeBinding;
	private ITypeBinding[] typeArguments;
	private org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding referenceBinding;

	RecoveredTypeBinding(BindingResolver resolver, VariableDeclaration variableDeclaration) {
		this.variableDeclaration = variableDeclaration;
		this.resolver = resolver;
		this.currentType = getType();
		this.dimensions = variableDeclaration.getExtraDimensions();
		if (this.currentType.isArrayType()) {
			this.dimensions += ((ArrayType) this.currentType).getDimensions();
		}
	}

	RecoveredTypeBinding(BindingResolver resolver, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding referenceBinding) {
		this.resolver = resolver;
		this.dimensions = referenceBinding.dimensions();
		this.referenceBinding = referenceBinding;
	}

	RecoveredTypeBinding(BindingResolver resolver, Type type) {
		this.currentType = type;
		this.resolver = resolver;
		this.dimensions = 0;
		if (type.isArrayType()) {
			this.dimensions += ((ArrayType) type).getDimensions();
		}
	}

	RecoveredTypeBinding(BindingResolver resolver, RecoveredTypeBinding typeBinding, int dimensions) {
		this.innerTypeBinding = typeBinding;
		this.dimensions = typeBinding.getDimensions() + dimensions;
		this.resolver = resolver;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#createArrayType(int)
	 */
	public ITypeBinding createArrayType(int dims) {
		return this.resolver.getTypeBinding(this, dims);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getBinaryName()
	 */
	public String getBinaryName() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getBound()
	 */
	public ITypeBinding getBound() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getComponentType()
	 */
	public ITypeBinding getComponentType() {
		if (this.dimensions == 0) return null;
		return this.resolver.getTypeBinding(this, -1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getDeclaredFields()
	 */
	public IVariableBinding[] getDeclaredFields() {
		return TypeBinding.NO_VARIABLE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getDeclaredMethods()
	 */
	public IFunctionBinding[] getDeclaredMethods() {
		return TypeBinding.NO_METHOD_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getDeclaredModifiers()
	 */
	public int getDeclaredModifiers() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getDeclaredTypes()
	 */
	public ITypeBinding[] getDeclaredTypes() {
		return TypeBinding.NO_TYPE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getDeclaringClass()
	 */
	public ITypeBinding getDeclaringClass() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getDeclaringMethod()
	 */
	public IFunctionBinding getDeclaringMethod() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getDimensions()
	 */
	public int getDimensions() {
		return this.dimensions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getElementType()
	 */
	public ITypeBinding getElementType() {
		if (this.referenceBinding != null) {
			if (this.referenceBinding.isArrayType()) {
				ArrayBinding arrayBinding = (ArrayBinding) this.referenceBinding;
				return new RecoveredTypeBinding(this.resolver, arrayBinding.leafComponentType);
			} else {
				return new RecoveredTypeBinding(this.resolver, this.referenceBinding);
			}
		}
		if (this.innerTypeBinding != null) {
			return this.innerTypeBinding.getElementType();
		}
		if (this.currentType!= null && this.currentType.isArrayType()) {
			return this.resolver.getTypeBinding(((ArrayType) this.currentType).getElementType());
		}
		if (this.variableDeclaration != null && this.variableDeclaration.getExtraDimensions() != 0) {
			return this.resolver.getTypeBinding(getType());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getErasure()
	 */
	public ITypeBinding getErasure() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getInterfaces()
	 */
	public ITypeBinding[] getInterfaces() {
		return TypeBinding.NO_TYPE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getModifiers()
	 */
	public int getModifiers() {
		return Modifier.NONE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getName()
	 */
	public String getName() {
		char[] brackets = new char[this.dimensions * 2];
		for (int i = this.dimensions * 2 - 1; i >= 0; i -= 2) {
			brackets[i] = ']';
			brackets[i - 1] = '[';
		}
		StringBuffer buffer = new StringBuffer(this.getInternalName());
		buffer.append(brackets);
		return String.valueOf(buffer);
	}

	private String getInternalName() {
		if (this.innerTypeBinding != null) {
			return this.innerTypeBinding.getInternalName();
		} else if (this.referenceBinding != null) {
			org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding typeBinding = null;
			if (this.referenceBinding.isArrayType()) {
				ArrayBinding arrayBinding = (ArrayBinding) this.referenceBinding;
				if (arrayBinding.leafComponentType instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding) {
					typeBinding = (org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding) arrayBinding.leafComponentType;
				}
			} else if (this.referenceBinding instanceof org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding) {
				typeBinding = (org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding) this.referenceBinding;
			}
			return new String(typeBinding.compoundName[typeBinding.compoundName.length - 1]);
		}
		return this.getTypeNameFrom(getType());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getPackage()
	 */
	public IPackageBinding getPackage() {
		CompilationUnitScope scope = this.resolver.scope();
		if (scope != null) {
			return this.resolver.getPackageBinding(scope.getCurrentPackage());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getQualifiedName()
	 */
	public String getQualifiedName() {
		return this.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getSuperclass()
	 */
	public ITypeBinding getSuperclass() {
		return this.resolver.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getTypeArguments()
	 */
	public ITypeBinding[] getTypeArguments() {
		if (this.referenceBinding != null) {
			return this.typeArguments = TypeBinding.NO_TYPE_BINDINGS;
		}
		if (this.typeArguments != null) {
			return typeArguments;
		}

		if (this.innerTypeBinding != null) {
			return this.innerTypeBinding.getTypeArguments();
		}

		return this.typeArguments = TypeBinding.NO_TYPE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getTypeBounds()
	 */
	public ITypeBinding[] getTypeBounds() {
		return TypeBinding.NO_TYPE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getTypeDeclaration()
	 */
	public ITypeBinding getTypeDeclaration() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getTypeParameters()
	 */
	public ITypeBinding[] getTypeParameters() {
		return TypeBinding.NO_TYPE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isAnnotation()
	 */
	public boolean isAnnotation() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isAnonymous()
	 */
	public boolean isAnonymous() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isArray()
	 */
	public boolean isArray() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isAssignmentCompatible(org.eclipse.wst.jsdt.core.dom.ITypeBinding)
	 */
	public boolean isAssignmentCompatible(ITypeBinding typeBinding) {
		if ("java.lang.Object".equals(typeBinding.getQualifiedName())) { //$NON-NLS-1$
			return true;
		}
		// since recovered binding are not unique isEqualTo is required
		return this.isEqualTo(typeBinding);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isCapture()
	 */
	public boolean isCapture() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isCastCompatible(org.eclipse.wst.jsdt.core.dom.ITypeBinding)
	 */
	public boolean isCastCompatible(ITypeBinding typeBinding) {
		if ("java.lang.Object".equals(typeBinding.getQualifiedName())) { //$NON-NLS-1$
			return true;
		}
		// since recovered binding are not unique isEqualTo is required
		return this.isEqualTo(typeBinding);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isClass()
	 */
	public boolean isClass() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isEnum()
	 */
	public boolean isEnum() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isFromSource()
	 */
	public boolean isFromSource() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isGenericType()
	 */
	public boolean isGenericType() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isInterface()
	 */
	public boolean isInterface() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isLocal()
	 */
	public boolean isLocal() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isMember()
	 */
	public boolean isMember() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isNested()
	 */
	public boolean isNested() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isNullType()
	 */
	public boolean isNullType() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isPrimitive()
	 */
	public boolean isPrimitive() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isRawType()
	 */
	public boolean isRawType() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isSubTypeCompatible(org.eclipse.wst.jsdt.core.dom.ITypeBinding)
	 */
	public boolean isSubTypeCompatible(ITypeBinding typeBinding) {
		if ("java.lang.Object".equals(typeBinding.getQualifiedName())) { //$NON-NLS-1$
			return true;
		}
		// since recovered binding are not unique isEqualTo is required
		return this.isEqualTo(typeBinding);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isTopLevel()
	 */
	public boolean isTopLevel() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isTypeVariable()
	 */
	public boolean isTypeVariable() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isUpperbound()
	 */
	public boolean isUpperbound() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.IBinding#getJavaElement()
	 */
	public IJavaScriptElement getJavaElement() {
		try {
			return new CompilationUnit(null, this.getInternalName(), this.resolver.getWorkingCopyOwner()).getWorkingCopy(this.resolver.getWorkingCopyOwner(), null);
		} catch (JavaScriptModelException e) {
			//ignore
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.IBinding#getKey()
	 */
	public String getKey() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Recovered#"); //$NON-NLS-1$
		if (this.innerTypeBinding != null) {
			buffer.append("innerTypeBinding") //$NON-NLS-1$
			      .append(this.innerTypeBinding.getKey());
		} else if (this.currentType != null) {
			buffer.append("currentType") //$NON-NLS-1$
			      .append(this.currentType.toString());
		} else if (this.referenceBinding != null) {
			buffer.append("referenceBinding") //$NON-NLS-1$
				  .append(this.referenceBinding.computeUniqueKey());
		} else if (variableDeclaration != null) {
			buffer
				.append("variableDeclaration") //$NON-NLS-1$
				.append(this.variableDeclaration.getClass())
				.append(this.variableDeclaration.getName().getIdentifier())
				.append(this.variableDeclaration.getExtraDimensions());
		}
		buffer.append(this.getDimensions());
		if (this.typeArguments != null) {
			buffer.append('<');
			for (int i = 0, max = this.typeArguments.length; i < max; i++) {
				if (i != 0) {
					buffer.append(',');
				}
				buffer.append(this.typeArguments[i].getKey());
			}
			buffer.append('>');
		}
		return String.valueOf(buffer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.IBinding#getKind()
	 */
	public int getKind() {
		return IBinding.TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.IBinding#isDeprecated()
	 */
	public boolean isDeprecated() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.IBinding#isEqualTo(org.eclipse.wst.jsdt.core.dom.IBinding)
	 */
	public boolean isEqualTo(IBinding other) {
		if (!other.isRecovered() || other.getKind() != IBinding.TYPE) return false;
		return this.getKey().equals(other.getKey());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.IBinding#isRecovered()
	 */
	public boolean isRecovered() {
		return true;
	}

	private String getTypeNameFrom(Type type) {
		if (type == null) return Util.EMPTY_STRING;
		switch(type.getNodeType0()) {
			case ASTNode.ARRAY_TYPE :
				ArrayType arrayType = (ArrayType) type;
				type = arrayType.getElementType();
				return getTypeNameFrom(type);
			case ASTNode.PRIMITIVE_TYPE :
				PrimitiveType primitiveType = (PrimitiveType) type;
				return primitiveType.getPrimitiveTypeCode().toString();
			case ASTNode.QUALIFIED_TYPE :
				QualifiedType qualifiedType = (QualifiedType) type;
				return qualifiedType.getName().getIdentifier();
			case ASTNode.SIMPLE_TYPE :
				SimpleType simpleType = (SimpleType) type;
				Name name = simpleType.getName();
				if (name.isQualifiedName()) {
					QualifiedName qualifiedName = (QualifiedName) name;
					return qualifiedName.getName().getIdentifier();
				}
				return ((SimpleName) name).getIdentifier();
		}
		return Util.EMPTY_STRING;
	}

	private Type getType() {
		if (this.currentType != null) {
			return this.currentType;
		}
		if (this.variableDeclaration == null) return null;
		switch(this.variableDeclaration.getNodeType()) {
			case ASTNode.SINGLE_VARIABLE_DECLARATION :
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) this.variableDeclaration;
				return singleVariableDeclaration.getType();
			default :
				// this is a variable declaration fragment
				ASTNode parent = this.variableDeclaration.getParent();
				switch(parent.getNodeType()) {
					case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) parent;
						return variableDeclarationExpression.getType();
					case ASTNode.VARIABLE_DECLARATION_STATEMENT :
						VariableDeclarationStatement statement = (VariableDeclarationStatement) parent;
						return statement.getType();
					case ASTNode.FIELD_DECLARATION :
						FieldDeclaration fieldDeclaration  = (FieldDeclaration) parent;
						return fieldDeclaration.getType();
				}
		}
		return null; // should not happen
	}

	public boolean isCompilationUnit() {
		return false;
	}
}
