/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.wst.jsdt.core.UnimplementedException;
import org.eclipse.wst.jsdt.core.infer.InferredType;

public class CombinedSourceTypeBinding extends SourceTypeBinding {

	SourceTypeBinding [] sourceTypes=new SourceTypeBinding[2];
	public CombinedSourceTypeBinding( Scope scope, SourceTypeBinding initialSourceType, SourceTypeBinding initialSourceType2) {
		super(initialSourceType.compoundName, initialSourceType.fPackage, scope);
		sourceTypes[0]=initialSourceType;
		sourceTypes[1]=initialSourceType2;
		setSuperclass(initialSourceType);
		setSuperclass(initialSourceType2);
		this.tagBits|=initialSourceType.tagBits;
		this.tagBits|=initialSourceType2.tagBits;
	}


	public void addSourceType(SourceTypeBinding binding)
	{
		int length = this.sourceTypes.length;
		System.arraycopy(this.sourceTypes, 0, this.sourceTypes=new SourceTypeBinding[length+1], 0, length);
		this.sourceTypes[length]=binding;
		setSuperclass(binding);
		this.tagBits|=binding.tagBits;
	}


	public FieldBinding[] fields() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.sourceTypes.length ; i++) {
			FieldBinding[] bindings =  this.sourceTypes[i].fields();
			list.addAll(Arrays.asList(bindings));
		}
		return (FieldBinding[]) list.toArray(new FieldBinding[list.size()]);
	}


	public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
		MethodBinding methodBinding=null;
		for (int i = 0; i < this.sourceTypes.length && methodBinding==null; i++) {
			methodBinding= this.sourceTypes[i].getExactMethod(selector, argumentTypes, refScope);
		}
		return methodBinding;
	}


	public FieldBinding getField(char[] fieldName, boolean needResolve) {
		FieldBinding fieldBinding=null;
		for (int i = 0; i < this.sourceTypes.length && fieldBinding==null; i++) {
			fieldBinding= this.sourceTypes[i].getField(fieldName, needResolve);
		}
		return fieldBinding;
	}


	public InferredType getInferredType() {
		throw new UnimplementedException("should not get here"); //$NON-NLS-1$
	}


	public MethodBinding[] getMethods(char[] selector) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.sourceTypes.length ; i++) {
			MethodBinding[] bindings = this.sourceTypes[i].getMethods(selector);
			list.addAll(Arrays.asList(bindings));
		}
		return (MethodBinding[]) list.toArray(new MethodBinding[list.size()]);
	}


	public boolean hasMemberTypes() {
		for (int i = 0; i < this.sourceTypes.length ; i++) {
			if (this.sourceTypes[i].hasMemberTypes())
			return true;
		}
		return false;
	}


	public boolean isEquivalentTo(TypeBinding otherType) {
		if (this == otherType) return true;

		return false;
	}


	public ReferenceBinding[] memberTypes() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.sourceTypes.length ; i++) {
			ReferenceBinding[] bindings =  this.sourceTypes[i].memberTypes();
			list.addAll(Arrays.asList(bindings));
		}
		return (ReferenceBinding[]) list.toArray(new ReferenceBinding[list.size()]);
	}


	public MethodBinding[] methods() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.sourceTypes.length ; i++) {
			MethodBinding[] bindings =  this.sourceTypes[i].methods();
			list.addAll(Arrays.asList(bindings));
		}
		return (MethodBinding[]) list.toArray(new MethodBinding[list.size()]);
	}


	public void setFields(FieldBinding[] fields) {
		throw new UnimplementedException("should not get here"); //$NON-NLS-1$
	}


	public void setMethods(MethodBinding[] methods) {
	throw new UnimplementedException("should not get here"); //$NON-NLS-1$
	}


	public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {
		for (int i = 0; i < this.sourceTypes.length ; i++) {
			MethodBinding exactConstructor = this.sourceTypes[i].getExactConstructor(argumentTypes);
			if (exactConstructor!=null && exactConstructor.isValidBinding())
				return exactConstructor;
		}
		return null;
	}


	public boolean isLinkedType(ReferenceBinding binding)
	{
		for (int i = 0; i < this.sourceTypes.length ; i++)
			if (this.sourceTypes[i]==binding)
				return true;

		return false;
	}

	public void cleanup()
	{
		super.cleanup();
		for (int i = 0; i < this.sourceTypes.length ; i++)
			this.sourceTypes[i].cleanup();
	}


	private void setSuperclass(SourceTypeBinding from)
	{
		if (this.getSuperBinding0()==null || (from.getSuperBinding0()!=null && from.getSuperBinding0().id!=TypeIds.T_JavaLangObject))
			this.setSuperBinding(from.getSuperBinding0());
	}


	public ReferenceBinding getSuperBinding() {
		ReferenceBinding supercls = null;
		for (int i = 0; i < this.sourceTypes.length ; i++)
		{
			supercls = this.sourceTypes[i].getSuperBinding0();
			if (supercls!=null && supercls.id!=TypeIds.T_JavaLangObject)
				return supercls;
		}
		if (supercls!=null && this.getSuperBinding0()==null)
			return supercls;
		return this.getSuperBinding0();
	}
	
	

}

