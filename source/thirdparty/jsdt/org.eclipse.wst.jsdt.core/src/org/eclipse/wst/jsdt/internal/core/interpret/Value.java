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

import org.eclipse.wst.jsdt.core.UnimplementedException;

public class Value implements Contants {
	public static final int BOOLEAN =1;
	public static final int NUMBER = 2;
	public static final int STRING = 3;
	public static final int OBJECT = 4;
	public static final int UNDEFINED = 5;
	public static final int NULL = 6;
	public static final int FUNCTION = 7;
	
	int type;
	
	public static Value UndefinedObjectValue=new Value(UNDEFINED); 
	public static Value NullObjectValue=new Value(NULL); 
	
		
	protected Value(int type)
	{
		this.type=type;
	}
		
	public  int numberValue()
	{
		return 0;
	}
	
	public final int getType()
	{return type;}
	
	public  String stringValue() 
	{
		switch (type)
		{
		case NULL:
			return "null"; //$NON-NLS-1$
		case UNDEFINED: 
			return "undefined"; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
	public  boolean booleanValue() {return false;}
	
	public ObjectValue getObjectValue()
	{
		
		
		switch (type)
		{
		case NULL:
		case UNDEFINED:
			throw new InterpretException("null reference"); //$NON-NLS-1$
		case BOOLEAN:
		case NUMBER:
		case STRING:
			ObjectValue obj= new ObjectValue();
					obj.setValue(VALUE_ARR,this);
					return obj;
		}
			
		throw new UnimplementedException();
	
	
	}

	public Object valueObject() {
		return null;
	}
	
	Value getValue()
	{
		return this;
	}

}
