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

import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;

public class ObjectValue extends Value implements ValueReference {

	public HashtableOfObject properties=new HashtableOfObject();
	public ObjectValue() {
		super(OBJECT);
	}
	public ObjectValue(ObjectValue prototype) {
		super(OBJECT);
		if (prototype!=null)
			try {
				properties=(HashtableOfObject)prototype.properties.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	public Value getValue(char[] name) {
		return (Value)properties.get(name);
	}
	public void setValue(char [] name, Value value) {
		properties.put(name, value);
	}

	public  String stringValue() 
	{
		Value value = (Value)properties.get(VALUE_ARR);
		if (value!=null)
			return value.stringValue();
		return ""; //$NON-NLS-1$
	}
	
	public ObjectValue getObjectValue()
	{
		return this;
	}

}
