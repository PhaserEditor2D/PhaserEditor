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
package org.eclipse.wst.jsdt.core.infer;

import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;


/**
 * 
 * This represents an inferred method
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class InferredMethod extends InferredMember{

	private MethodDeclaration methodDeclaration;

	public boolean isConstructor;
	public MethodBinding methodBinding;
	public InferredMethod(char [] name, IFunctionDeclaration functionDeclaration, InferredType inType )
	{
		this.methodDeclaration=(MethodDeclaration)functionDeclaration;
		this.name=name;
		this.inType = inType;
		this.sourceStart=methodDeclaration.sourceStart;
		this.sourceEnd=methodDeclaration.sourceEnd;
	}

	public IFunctionDeclaration getFunctionDeclaration()
	{
		return methodDeclaration;
	}
	
	public StringBuffer print(int indent, StringBuffer output)
	{
		String modifier=(isStatic)? "static ":""; //$NON-NLS-1$ //$NON-NLS-2$
		printIndent(indent, output).append(modifier);
		if (!isConstructor)
		{
		 if (methodDeclaration.inferredType!=null)
			 methodDeclaration.inferredType.dumpReference(output);
		else
			output.append("??"); //$NON-NLS-1$
		output.append(" "); //$NON-NLS-1$
		}

		output.append(name).append("("); //$NON-NLS-1$
		   if (methodDeclaration.arguments!=null)
			   for (int i = 0; i < methodDeclaration.arguments.length; i++) {
				   if (i>0)
					   output.append(", "); //$NON-NLS-1$
				  InferredType argumentType = methodDeclaration.arguments[i].inferredType;
				  if (argumentType!=null )
				  {
					  output.append(argumentType.name).append(" "); //$NON-NLS-1$
				  }
				   output.append(methodDeclaration.arguments[i].name);
			   }
		   output.append(")"); //$NON-NLS-1$

		   return output;
	}
}
