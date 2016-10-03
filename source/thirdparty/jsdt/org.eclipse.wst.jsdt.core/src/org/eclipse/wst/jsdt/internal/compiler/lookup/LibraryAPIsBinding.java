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

import java.io.File;

import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;


public class LibraryAPIsBinding  extends SourceTypeBinding {
	CompilationUnitScope compilationUnitScope;
	private char[]shortName;

	char [] path;

 

	public LibraryAPIsBinding(CompilationUnitScope scope,PackageBinding fPackage, char [] fileName ) {
		this.compilationUnitScope=scope;
		this.memberTypes=Binding.NO_MEMBER_TYPES;
		this.fileName=fileName;
		this.sourceName=this.fileName;
		setShortName(this.fileName);
		this.fPackage = fPackage;
		this.compoundName=new char [][]{fileName};
		this.scope = scope;

		// expect the fields & methods to be initialized correctly later
		this.fields = Binding.NO_FIELDS;
		this.methods = Binding.NO_METHODS;

		computeId();

	}

	private void setShortName(char[] fileName) {
		for (int i=fileName.length-1;i>=0;i--)
		{
			if (fileName[i]==File.separatorChar || fileName[i]=='/')
			{
				shortName=new char[fileName.length-1-i];
				this.path=new char[i];
				System.arraycopy(fileName, i+1, shortName, 0, shortName.length);
				System.arraycopy(fileName, 0, this.path, 0, this.path.length);
				return;
			}
		}
		shortName=fileName;
		this.path=CharOperation.NO_CHAR;
	}

	public int kind() {
		return COMPILATION_UNIT;
	}

	public char[] signature() /* Ljava/lang/Object; */ {
		if (this.signature != null)
			return this.signature;

		return this.signature = CharOperation.concat(Signature.C_COMPILATION_UNIT, constantPoolName(), ';');
	}


	public AbstractMethodDeclaration sourceMethod(MethodBinding binding) {
		  return null;
	}

	public char[] qualifiedSourceName() {
		return CharOperation.concatWith(compoundName, '.');
	}

	public char[] qualifiedPackageName() {
		return this.path;
	}

	public void cleanup()
	{
		super.cleanup();
		this.compilationUnitScope=null;
	}
}
