/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.LocalVariable;

/**
 * Internal implementation of variable bindings.
 */
class VariableBinding implements IVariableBinding {

	private static final int VALID_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
		Modifier.STATIC | Modifier.FINAL | Modifier.TRANSIENT | Modifier.VOLATILE;

	private org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding binding;
	private ITypeBinding declaringClass;
	private String key;
	private String name;
	private BindingResolver resolver;
	private ITypeBinding type;

	VariableBinding(BindingResolver resolver, org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding binding) {
		this.resolver = resolver;
		this.binding = binding;
	}

	/* (non-Javadoc)
	 * @see IVariableBinding#getConstantValue()
	 *  
	 */
	public Object getConstantValue() {
		return null;
	}

	/*
	 * @see IVariableBinding#getDeclaringClass()
	 */
	public ITypeBinding getDeclaringClass() {
		if (isField()) {
			if (this.declaringClass == null) {
				FieldBinding fieldBinding = (FieldBinding) this.binding;
				this.declaringClass = this.resolver.getTypeBinding(fieldBinding.declaringClass);
			}
			return this.declaringClass;
		} else {
			return null;
		}
	}

	/*
	 * @see IVariableBinding#getDeclaringMethod()
	 */
	public IFunctionBinding getDeclaringMethod() {
		if (!isField()) {
			ASTNode node = this.resolver.findDeclaringNode(this);
			while (true) {
				if (node == null) break;
				switch(node.getNodeType()) {
					case ASTNode.INITIALIZER :
						return null;
					case ASTNode.FUNCTION_DECLARATION :
						FunctionDeclaration methodDeclaration = (FunctionDeclaration) node;
						return methodDeclaration.resolveBinding();
					default:
						node = node.getParent();
				}
			}
		}
		return null;
	}

	/*
	 * @see IBinding#getJavaElement()
	 */
	public IJavaScriptElement getJavaElement() {
		JavaElement element = getUnresolvedJavaElement();
		if (element == null)
			return null;
		return element.resolved(this.binding);
	}

	/*
	 * @see IBinding#getKey()
	 */
	public String getKey() {
		if (this.key == null) {
			this.key = new String(this.binding.computeUniqueKey());
		}
		return this.key;
	}

	/*
	 * @see IBinding#getKind()
	 */
	public int getKind() {
		return IBinding.VARIABLE;
	}

	/*
	 * @see IBinding#getModifiers()
	 */
	public int getModifiers() {
		if (isField()) {
			return ((FieldBinding) this.binding).getAccessFlags() & VALID_MODIFIERS;
		}
		return Modifier.NONE;
	}

	/*
	 * @see IBinding#getName()
	 */
	public String getName() {
		if (this.name == null) {
			this.name = new String(this.binding.name);
		}
		return this.name;
	}

	/*
	 * @see IVariableBinding#getType()
	 */
	public ITypeBinding getType() {
		if (this.type == null) {
			this.type = this.resolver.getTypeBinding(this.binding.type);
		}
		return this.type;
	}

	private JavaElement getUnresolvedJavaElement() {
		if (isField()) {
			// field
			FieldBinding fieldBinding = (FieldBinding) this.binding;
			if (fieldBinding.declaringClass == null) return null; // arraylength
			IType declaringType = (IType) getDeclaringClass().getJavaElement();
			if (declaringType == null) return null;
			return (JavaElement) declaringType.getField(getName());
		}
		// local variable
		IFunctionBinding declaringMethod = getDeclaringMethod();
		if (declaringMethod == null) return null;
		JavaElement method = (JavaElement) declaringMethod.getJavaElement();
		if (!(this.resolver instanceof DefaultBindingResolver)) return null;
		VariableDeclaration localVar = (VariableDeclaration) ((DefaultBindingResolver) this.resolver).bindingsToAstNodes.get(this);
		if (localVar == null) return null;
		int nameStart;
		int nameLength;
		int sourceStart;
		int sourceLength;
		if (localVar instanceof SingleVariableDeclaration) {
			sourceStart = localVar.getStartPosition();
			sourceLength = localVar.getLength();
			SimpleName simpleName = ((SingleVariableDeclaration) localVar).getName();
			nameStart = simpleName.getStartPosition();
			nameLength = simpleName.getLength();
		} else {
			nameStart =  localVar.getStartPosition();
			nameLength = localVar.getLength();
			ASTNode node = localVar.getParent();
			sourceStart = node.getStartPosition();
			sourceLength = node.getLength();
		}
		char[] typeSig = this.binding.type.signature();
		return new LocalVariable(method, localVar.getName().getIdentifier(), sourceStart, sourceStart+sourceLength-1, nameStart, nameStart+nameLength-1, new String(typeSig));
	}

	/*
	 * @see IVariableBinding#getVariableDeclaration()
	 *  
	 */
	public IVariableBinding getVariableDeclaration() {
		if (this.isField()) {
			FieldBinding fieldBinding = (FieldBinding) this.binding;
			return this.resolver.getVariableBinding(fieldBinding.original());
		}
		return this;
	}

	/*
	 * @see IVariableBinding#getVariableId()
	 */
	public int getVariableId() {
		return this.binding.id;
	}

	/*
	 * @see IVariableBinding#isParameter()
	 */
	public boolean isParameter() {
		return (this.binding.tagBits & TagBits.IsArgument) != 0;
	}
	/*
	 * @see IBinding#isDeprecated()
	 */
	public boolean isDeprecated() {
		if (isField()) {
			return ((FieldBinding) this.binding).isDeprecated();
		}
		return false;
	}

	/*
	 * @see IBinding#isEqualTo(Binding)
	 *  
	 */
	public boolean isEqualTo(IBinding other) {
		if (other == this) {
			// identical binding - equal (key or no key)
			return true;
		}
		if (other == null) {
			// other binding missing
			return false;
		}
		if (!(other instanceof VariableBinding)) {
			return false;
		}
		org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding otherBinding = ((VariableBinding) other).binding;
		if (this.binding instanceof FieldBinding) {
			if (otherBinding instanceof FieldBinding) {
				return BindingComparator.isEqual((FieldBinding) this.binding, (FieldBinding) otherBinding);
			} else {
				return false;
			}
		} else {
			if (BindingComparator.isEqual(this.binding, otherBinding)) {
				IFunctionBinding declaringMethod = this.getDeclaringMethod();
				IFunctionBinding otherDeclaringMethod = ((VariableBinding) other).getDeclaringMethod();
				if (declaringMethod == null) {
					if (otherDeclaringMethod != null) {
						return false;
					}
					return true;
				}
				return declaringMethod.isEqualTo(otherDeclaringMethod);
			}
			return false;
		}
	}

	/*
	 * @see IVariableBinding#isField()
	 */
	public boolean isField() {
		return this.binding instanceof FieldBinding;
	}

	public boolean isGlobal()
	{
		return this.binding instanceof LocalVariableBinding && ((LocalVariableBinding)this.binding).declaringScope instanceof CompilationUnitScope;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.IBinding#isRecovered()
	 */
	public boolean isRecovered() {
		return false;
	}

	/*
	 * For debugging purpose only.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.binding.toString();
	}
}
