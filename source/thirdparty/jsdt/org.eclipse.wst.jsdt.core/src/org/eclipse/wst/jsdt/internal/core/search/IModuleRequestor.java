/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search;

import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;


public interface IModuleRequestor {

	public void acceptFunction(char[] signature,
				char[] declaringQualification,
				char[] declaringSimpleName,
				String path);


	public void acceptField(char[] signature,
				char[] declaringQualification,
				char[] declaringSimpleName,
				String path);
	
	
	public void acceptType(char[] qualification, 
				char[] simpleName, 
				String documentPath);
	
	
	public void acceptFunction(IFunction function);

	
	public void acceptField(IField field);
	
	
	public void acceptType(IType type);


}
