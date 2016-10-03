/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Erling Ellingsen -  patch for bug 125570
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import java.util.HashMap;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.oaametadata.LibraryAPIs;



public class LibraryAPIsScope extends CompilationUnitScope {


	HashMap resolvedTypes=new HashMap();
	HashtableOfObject translations=new HashtableOfObject();
	LibraryAPIs apis;
public LibraryAPIsScope(LibraryAPIs apis, LookupEnvironment environment) {

	super(environment);
	this.apis=apis;
	this.referenceContext = null;

	
	this.currentPackageName = CharOperation.NO_CHAR_CHAR;
	
	this.resolvedTypes.put("any", TypeBinding.ANY);
	this.resolvedTypes.put("Any", TypeBinding.ANY);
	this.resolvedTypes.put("null", TypeBinding.NULL);

	translations.put("object".toCharArray(), "Object".toCharArray());
	translations.put("boolean".toCharArray(), "Boolean".toCharArray());
	translations.put("number".toCharArray(), "Number".toCharArray());
	translations.put("string".toCharArray(), "String".toCharArray());
	translations.put("array".toCharArray(), "Array".toCharArray());
	
	CompilationResult result = new CompilationResult(apis.fileName, new char[][]{},0,0,0);
	CompilationUnitDeclaration unit = new CompilationUnitDeclaration(environment.problemReporter,result,0);
	unit.scope=this;
	this.referenceContext=unit;
	
}




public PackageBinding getDefaultPackage() {
		return environment.defaultPackage;
}


public TypeBinding resolveType(String name)
{
	
	if (name==null)
		return TypeBinding.ANY;
	
  TypeBinding binding = (TypeBinding)this.resolvedTypes.get(name);
  if (binding!=null)
	  return binding;
  
       
        if (name.length()>1 && name.charAt(0)=='[' && name.charAt(name.length()-1)==']')
		{
        	name=name.substring(1, name.length()-1);
        	
			TypeBinding memberType = resolveType(name);
			binding=new ArrayBinding(memberType, 1, this.compilationUnitScope().environment) ;

		}
		else {
			if (name.indexOf('|')>0)
			{
				
				char[][] names = CharOperation.splitAndTrimOn('|', name.toCharArray());
				for (int i = 0; i < names.length; i++) {
					names[i]=translateName(names[i]);
				}
				binding=new MultipleTypeBinding(this,names);
			}
			else
			{
			   binding = this.getType(translateName(name.toCharArray()));
			}
			/* the inferred type isn't valid, so don't assign it to the variable */
			if(!binding.isValidBinding()) 
				binding=TypeBinding.UNKNOWN;
		}


//		if (node!=null && !this.resolvedType.isValidBinding()) {
//			libraryScope.problemReporter().invalidType(node, this.resolvedType);
//			return null;
//		}
//		if (node!=null && node.isTypeUseDeprecated(this.resolvedType, libraryScope))
//			libraryScope.problemReporter().deprecatedType(this.resolvedType, node);


  this.resolvedTypes.put(name, binding);
  return binding;
}

private char[] translateName(char[] name) {
	char [] newName=(char[])this.translations.get(name);
	return (newName!=null) ? newName : name;
}




public String toString() {
	return "--- LibraryAPIsScope Scope : " + new String(referenceContext.getFileName()); //$NON-NLS-1$
}

public void cleanup()
{
	super.cleanup();
}




public char[] getFileName() {
	return this.apis.fileName;
}

}
