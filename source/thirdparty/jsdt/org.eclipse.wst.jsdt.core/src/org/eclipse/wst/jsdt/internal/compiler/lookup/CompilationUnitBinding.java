/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;


public class CompilationUnitBinding  extends SourceTypeBinding {
//	public char[] sourceName;
//
//	private FieldBinding[] fields;
//
//	private FunctionBinding[] methods;
//	public long tagBits = 0; // See values in the interface TagBits below
	CompilationUnitScope compilationUnitScope;
	private char[]shortName;

	char [] path;

	public CompilationUnitBinding(CompilationUnitScope scope,PackageBinding fPackage,char [] path) {
		this(scope,fPackage,path,null);
	}

	public CompilationUnitBinding(CompilationUnitScope scope,PackageBinding fPackage,char [] path,ReferenceBinding superType ) {
		super(new char [][]{scope.referenceContext.getFileName()}, fPackage, scope);
		this.compilationUnitScope=scope;
		this.memberTypes=Binding.NO_MEMBER_TYPES;
		this.sourceName=this.fileName;
		setShortName(this.fileName);
		this.path=path;
		/* bc - allows super type of 'Window' (and other types) for a compilation unit */
		this.setSuperBinding(superType);

	}

	private void setShortName(char[] fileName) {
		for (int i=fileName.length-1;i>=0;i--)
		{
			if (fileName[i]==File.separatorChar || fileName[i]=='/')
			{
				shortName=new char[fileName.length-1-i];
				System.arraycopy(fileName, i+1, shortName, 0, shortName.length);
				return;
			}
		}
		shortName=fileName;
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
		if (compilationUnitScope == null)
			return null;
		
		  ProgramElement[] statements = compilationUnitScope.referenceContext.statements;
		  for (int i = 0; i < statements.length; i++) {
			if (statements[i] instanceof AbstractMethodDeclaration && ((AbstractMethodDeclaration)statements[i]).getBinding()==binding)
				return (AbstractMethodDeclaration)statements[i];
			else if (statements[i] instanceof Assignment && (((Assignment)statements[i]).expression instanceof FunctionExpression)) {
				FunctionExpression functionExpression = (FunctionExpression) ((Assignment)statements[i]).expression;
				if (functionExpression.methodDeclaration !=null && functionExpression.methodDeclaration.getBinding()==binding)
					return functionExpression.methodDeclaration;
			}
		  }

		  class  MethodFinder extends ASTVisitor
		  {
			  MethodBinding binding;
			  MethodDeclaration method;
			  MethodFinder(MethodBinding binding)
			  {this.binding=binding;}
			  
				public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
					if (methodDeclaration.getBinding()==this.binding)
					{
						method=methodDeclaration;
						return false;
					}
					return true;
				}

				public boolean visit(InferredType inferredType, BlockScope scope) {	// not possible to contain method
					return false;
				}

				public boolean visit(Javadoc javadoc, BlockScope scope) {	// not possible to contain method
					return false;
				}

				public boolean visit(Javadoc javadoc, ClassScope scope) { // not possible to contain method
					return false;
				}

				public boolean visit(PostfixExpression postfixExpression,  // not possible to contain method
						BlockScope scope) {
					return false;
				}

				public boolean visit(PrefixExpression prefixExpression,	// not possible to contain method
						BlockScope scope) {
					return false;
				}

				public boolean visit(ThisReference thisReference,	// not possible to contain method
						BlockScope scope) {
					return false;
				}

				public boolean visit(ThisReference thisReference,	// not possible to contain method
						ClassScope scope) {
					return false;
				}
				
				
				
		  }
		  MethodFinder visitor=new MethodFinder(binding);
		  compilationUnitScope.referenceContext.traverse(visitor, compilationUnitScope,true);
		  return visitor.method;
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
		if (this.methods!=null)
			for (int i = 0; i < this.methods.length; i++) {
				this.methods[i].cleanup();
			}
		this.compilationUnitScope=null;
	}
}
