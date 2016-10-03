/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredType;

public class MultipleTypeBinding extends ReferenceBinding {

	public ReferenceBinding[] types;
	int problemID = ProblemReasons.NoError;
	
	public MultipleTypeBinding(Scope scope, char[][] names) {

		  char [][] name={};
		  ArrayList resolveTypes=new ArrayList(names.length);
		  for (int i = 0; i < names.length; i++) {
			  TypeBinding typeBinding= scope.getType(names[i]);
			  if (typeBinding instanceof ReferenceBinding)
			  {
				  if (!typeBinding.isValidBinding())
					  problemID=typeBinding.problemId();
				  else
				  {
					  this.tagBits|=typeBinding.tagBits;
					  this.modifiers|=((ReferenceBinding)typeBinding).modifiers;
				    resolveTypes.add(typeBinding);
				    this.compoundName=((ReferenceBinding)typeBinding).compoundName;
				  }
			  }
		}
	  types = (ReferenceBinding[]) resolveTypes.toArray(new ReferenceBinding[resolveTypes.size()]);

	}

	public int problemId() {
		return problemID;
	}

	public FieldBinding[] availableFields() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.types.length ; i++) {
			FieldBinding[] bindings =  this.types[i].availableFields();
			list.addAll(Arrays.asList(bindings));
		}
		return (FieldBinding[]) list.toArray(new FieldBinding[list.size()]);
	}

	public MethodBinding[] availableMethods() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.types.length ; i++) {
			MethodBinding[] bindings =  this.types[i].availableMethods();
			list.addAll(Arrays.asList(bindings));
		}
		return (MethodBinding[]) list.toArray(new MethodBinding[list.size()]);
	}

	public FieldBinding[] fields() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.types.length ; i++) {
			FieldBinding[] bindings =  this.types[i].fields();
			list.addAll(Arrays.asList(bindings));
		}
		return (FieldBinding[]) list.toArray(new FieldBinding[list.size()]);
	}

	public MethodBinding getExactMethod(char[] selector,
			TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
		MethodBinding methodBinding=null;
		for (int i = 0; i < this.types.length && methodBinding==null; i++) {
			methodBinding= this.types[i].getExactMethod(selector, argumentTypes, refScope);
		}
		return methodBinding;
	}

	public FieldBinding getField(char[] fieldName, boolean needResolve) {
		FieldBinding fieldBinding=null;
		for (int i = 0; i < this.types.length && fieldBinding==null; i++) {
			fieldBinding= this.types[i].getField(fieldName, needResolve);
		}
		return fieldBinding;
	}

	public InferredType getInferredType() {
		throw new UnimplementedException("should not get here"); //$NON-NLS-1$
	}

	public MethodBinding[] getMethods(char[] selector) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.types.length ; i++) {
			MethodBinding[] bindings = this.types[i].getMethods(selector);
			list.addAll(Arrays.asList(bindings));
		}
		return (MethodBinding[]) list.toArray(new MethodBinding[list.size()]);
	}

	public boolean hasMemberTypes() {
		throw new UnimplementedException("should not get here"); //$NON-NLS-1$
	}

	public boolean isCompatibleWith(TypeBinding otherType) {
		for (int i = 0; i < this.types.length ; i++) 
			if (types[i].isCompatibleWith(otherType))
				return true;
		return false;
		
	}

	public boolean isSuperclassOf(ReferenceBinding otherType) {
		for (int i = 0; i < this.types.length ; i++) 
			if (types[i].isSuperclassOf(otherType))
				return true;
		return false;
	}

	public MethodBinding[] methods() {
		ArrayList list = new ArrayList();
		for (int i = 0; i < this.types.length ; i++) {
			MethodBinding[] bindings =  this.types[i].methods();
			list.addAll(Arrays.asList(bindings));
		}
		return (MethodBinding[]) list.toArray(new MethodBinding[list.size()]);
	}

	public char[] signature() {
		char [] sig={};
		for (int i = 0; i < this.types.length ; i++) {
			if (i>0)
				  sig=CharOperation.append(sig, '|');
			sig=CharOperation.concat(sig, this.types[i].signature());
		}
		return sig;
	}
	
	public boolean isViewedAsDeprecated() {
		for (int i = 0; i < this.types.length ; i++) 
			if (types[i].isViewedAsDeprecated())
				return true;
		return false;
	}

	public char[] readableName() {
		char [] name={};
		for (int i = 0; i < this.types.length ; i++) {
			if (i>0)
				  name=CharOperation.append(name, '|');
			name=CharOperation.concat(name, this.types[i].readableName());
		}
		return name;
	}

	public char[] shortReadableName() {
		char [] name={};
		for (int i = 0; i < this.types.length ; i++) {
			if (i>0)
				  name=CharOperation.append(name, '|');
			name=CharOperation.concat(name, this.types[i].shortReadableName());
		}
		return name;
	}
	

}
