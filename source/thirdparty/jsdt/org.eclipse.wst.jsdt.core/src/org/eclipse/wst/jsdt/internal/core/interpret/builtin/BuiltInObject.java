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
package org.eclipse.wst.jsdt.internal.core.interpret.builtin;

import org.eclipse.wst.jsdt.internal.core.interpret.BooleanValue;
import org.eclipse.wst.jsdt.internal.core.interpret.InterpreterContext;
import org.eclipse.wst.jsdt.internal.core.interpret.InterpreterEngine;
import org.eclipse.wst.jsdt.internal.core.interpret.NativeFunction;
import org.eclipse.wst.jsdt.internal.core.interpret.ObjectValue;
import org.eclipse.wst.jsdt.internal.core.interpret.StringValue;
import org.eclipse.wst.jsdt.internal.core.interpret.Value;

public class BuiltInObject {

	public static ObjectValue prototype;
	
	public static void initializeContext(InterpreterContext context) {

		prototype = new ObjectValue();
		
		prototype.setValue("toString".toCharArray(), //$NON-NLS-1$
				new NativeFunction()	{

			public Value execute(InterpreterEngine interpreter,ObjectValue receiver,  Value[] arguments) {
				if (receiver.getType()==Value.STRING)
					return receiver;
				return new StringValue(receiver.stringValue());
			}
			
		});

		prototype.setValue("hasProperty".toCharArray(), //$NON-NLS-1$
				new NativeFunction()	{

			public Value execute(InterpreterEngine interpreter,ObjectValue receiver,  Value[] arguments) {
				if (arguments.length>0)
					return new BooleanValue(receiver.properties.containsKey(arguments[0].stringValue().toCharArray()));
				return new BooleanValue(false);
			}
			
		});

		
		NativeFunction constructor = 	new NativeFunction()	{

			public Value execute(InterpreterEngine interpreter,ObjectValue receiver,  Value[] arguments) {
			
				return Value.UndefinedObjectValue;
			}
		};
		constructor.prototype=prototype;
		
		context.setValue(new char[]{'O','b','j','e','c','t'},constructor);


		
	}
}
