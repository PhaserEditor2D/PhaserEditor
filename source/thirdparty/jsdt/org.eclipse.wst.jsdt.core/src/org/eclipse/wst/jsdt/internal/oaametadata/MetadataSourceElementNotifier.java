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
package org.eclipse.wst.jsdt.internal.oaametadata;

import org.eclipse.wst.jsdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;

public class MetadataSourceElementNotifier {

	LibraryAPIs apis;
	ISourceElementRequestor requestor;
	HashtableOfObject notifiedTypes=new HashtableOfObject();

	public MetadataSourceElementNotifier(LibraryAPIs apis, ISourceElementRequestor requestor)
	{
		this.requestor=requestor;
		this.apis=apis;
	}
	
	public void notifyRequestor()
	{
		this.requestor.enterCompilationUnit();
		if (this.apis.classes!=null)
			for (int i = 0; i < apis.classes.length; i++) {
				notifySourceElementRequestor(this.apis.classes[i]);
			}
		this.requestor.exitCompilationUnit(0);
	}
	
	public void notifySourceElementRequestor( ClassData clazz ) {

		char [] typeName=clazz.name.toCharArray();
					// prevent possible recurrsion
		if (notifiedTypes.containsKey(typeName))
			return;
		notifiedTypes.put(typeName, null);
		

		ISourceElementRequestor.TypeInfo typeInfo = new ISourceElementRequestor.TypeInfo();
		typeInfo.declarationStart = 0;
		typeInfo.modifiers = 0;

		typeInfo.name = typeName;

		typeInfo.nameSourceStart = 0;//type.getNameStart();
		typeInfo.nameSourceEnd = 0;//typeInfo.nameSourceStart+typeInfo.name.length-1;
		
		String superClass=clazz.getSuperClass();
		if (superClass!=null)
			typeInfo.superclass =superClass.toCharArray();
		typeInfo.secondary = false;

		typeInfo.anonymousMember = false;

		requestor.enterType(typeInfo);

		Property[] fields = clazz.getFields();
		  for (int attributeInx=0; attributeInx<fields.length; attributeInx++) {
			  Property field = fields[attributeInx];
			ISourceElementRequestor.FieldInfo fieldInfo = new ISourceElementRequestor.FieldInfo();
			fieldInfo.declarationStart = 0;//field.sourceStart();
			fieldInfo.name = field.name.toCharArray();
			fieldInfo.modifiers = 0;

			if (field.isStatic())
				fieldInfo.modifiers |= ClassFileConstants.AccStatic;
			fieldInfo.nameSourceStart = 0;//field.nameStart;
			fieldInfo.nameSourceEnd = 0;//field.nameStart+field.name.length-1;

			fieldInfo.type = field.dataType!=null ? field.dataType.toCharArray():null;

//				fieldInfo.annotationPositions = collectAnnotationPositions(fieldDeclaration.annotations);
//				fieldInfo.categories = (char[][]) this.nodesToCategories.get(fieldDeclaration);
			requestor.enterField(fieldInfo);

//			//If this field is of an anonymous type, need to notify so that it shows as a child
//			if( field.type != null && field.type.isAnonymous && !field.type.isNamed() ){
//				notifySourceElementRequestor( field.type );
//			}

			int initializationStart=0;//field.initializationStart;
			requestor.exitField(initializationStart,0,0/*field.sourceEnd(),field.sourceEnd()*/);
		}

		if (clazz.constructors!=null)
		  for (int methodInx=0;methodInx<clazz.constructors.length;methodInx++) {
			Method method=clazz.constructors[methodInx];

			boolean isConstructor=true;

			notifyMethod(method, isConstructor);

		}


		if (clazz.methods!=null)
		  for (int methodInx=0;methodInx<clazz.methods.length;methodInx++) {
			Method method=clazz.methods[methodInx];

			boolean isConstructor=false;

			notifyMethod(method, isConstructor);

		}

		requestor.exitType(0/*clazz.sourceEnd*/);

	}

	private void notifyMethod(Method method, boolean isConstructor) {
		ISourceElementRequestor.MethodInfo methodInfo = new ISourceElementRequestor.MethodInfo();
		methodInfo.isConstructor = isConstructor;
		char[][] argumentTypes = null;
		char[][] argumentNames = null;
		Parameter [] arguments = method.parameters;
		if (arguments != null) {
			int argumentLength = arguments.length;
			argumentTypes = new char[argumentLength][];
			argumentNames = new char[argumentLength][];
			for (int i = 0; i < argumentLength; i++) {
				if (arguments[i].dataType!=null)
					argumentTypes[i] = arguments[i].dataType.toCharArray();
				argumentNames[i] = arguments[i].name.toCharArray();
			}
		}
//			int selectorSourceEnd = this.sourceEnds.get(methodDeclaration);

		methodInfo.declarationStart = 0;//methodDeclaration.declarationSourceStart;
		methodInfo.modifiers = 0;
		if (method.isStatic())
			methodInfo.modifiers |= ClassFileConstants.AccStatic;
		methodInfo.name =(isConstructor)? new char[0] : method.name.toCharArray();
		methodInfo.nameSourceStart = 0;//method.nameStart;
		methodInfo.nameSourceEnd = 0;//method.nameStart+method.name.length-1;
		methodInfo.parameterTypes = argumentTypes;
		methodInfo.parameterNames = argumentNames;
		methodInfo.categories =null;// (char[][]) this.nodesToCategories.get(methodDeclaration);
		requestor.enterMethod(methodInfo);


		requestor.exitMethod(0/*methodDeclaration.declarationSourceEnd*/, -1, -1);
	}


}
