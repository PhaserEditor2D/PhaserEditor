/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.INameReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;

import org.eclipse.wst.jsdt.internal.compiler.lookup.FunctionTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public abstract class NameReference extends Reference implements INameReference, InvocationSite {

	public Binding binding;//, codegenBinding; //may be aTypeBinding-aFieldBinding-aLocalVariableBinding

	public TypeBinding actualReceiverType;	// modified receiver type - actual one according to namelookup

	//the error printing
	//some name reference are build as name reference but
	//only used as type reference. When it happens, instead of
	//creating a new objet (aTypeReference) we just flag a boolean
	//This concesion is valuable while their are cases when the NameReference
	//will be a TypeReference (static message sends.....) and there is
	//no changeClass in java.
public NameReference() {
	super();
	bits |= Binding.TYPE | Binding.VARIABLE; // restrictiveFlag

}
public FieldBinding fieldBinding() {
	//this method should be sent ONLY after a check against isFieldReference()
	//check its use doing senders.........

	return (FieldBinding) binding ;
}
public boolean isSuperAccess() {
	return false;
}
public boolean isTypeAccess() {
	// null is acceptable when we are resolving the first part of a reference
	return binding == null || binding instanceof ReferenceBinding;
}
public boolean isTypeReference() {
	return binding instanceof ReferenceBinding;
}
public void setActualReceiverType(ReferenceBinding receiverType) {
	if (receiverType == null) return; // error scenario only
	this.actualReceiverType = receiverType;
}
public void setDepth(int depth) {
	bits &= ~DepthMASK; // flush previous depth if any
	if (depth > 0) {
		bits |= (depth & 0xFF) << DepthSHIFT; // encoded on 8 bits
	}
}
public void setFieldIndex(int index){
	// ignored
}

public abstract String unboundReferenceErrorName();

public Binding alternateBinding()
{
	   Binding alternateBinding = binding;
	   if (alternateBinding instanceof MethodBinding && ((MethodBinding)alternateBinding).isConstructor())
	   {
		   MethodBinding constructorBinding=(MethodBinding)alternateBinding;
		   alternateBinding=constructorBinding.returnType;
	   } else if(alternateBinding instanceof LocalVariableBinding) {
		   if(((LocalVariableBinding)alternateBinding).type instanceof FunctionTypeBinding) {
			   FunctionTypeBinding functionBinding = (FunctionTypeBinding)((LocalVariableBinding)alternateBinding).type;
			   if(functionBinding.functionBinding.isConstructor()) {
				   alternateBinding = functionBinding.functionBinding.returnType;
			   }
		   }
	   }
	
	return alternateBinding;


}
public int getASTType() {
	return IASTNode.NAME_REFERENCE;

}
}


