/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.interpret;

public class NumberValue extends Value{

	int intValue;
	
	public NumberValue(int value)
	{
		super(NUMBER);
		this.intValue=value;
	}


	public  int numberValue()
	{
		return intValue;
	}
	public  String stringValue() 
	{
		return String.valueOf(intValue);
	}
	
	public  boolean booleanValue() { return (intValue==0) ?  false:true;}
	

}
