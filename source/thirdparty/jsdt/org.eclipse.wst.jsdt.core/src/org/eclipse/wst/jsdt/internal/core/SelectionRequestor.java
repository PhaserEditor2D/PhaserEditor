/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import java.util.ArrayList;

import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.codeassist.ISelectionRequestor;
import org.eclipse.wst.jsdt.internal.codeassist.SelectionEngine;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.core.util.HandleFactory;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Implementation of <code>ISelectionRequestor</code> to assist with
 * code resolve in a compilation unit. Translates names to elements.
 */
public class SelectionRequestor implements ISelectionRequestor {
	/*
	 * The name lookup facility used to resolve packages
	 */
	protected NameLookup nameLookup;

	/*
	 * The compilation unit or class file we are resolving in
	 */
	protected Openable openable;

	/*
	 * The collection of resolved elements.
	 */
	protected IJavaScriptElement[] elements = JavaElement.NO_ELEMENTS;
	protected int elementIndex = -1;

	protected HandleFactory handleFactory = new HandleFactory();

/**
 * Creates a selection requestor that uses that given
 * name lookup facility to resolve names.
 *
 * Fix for 1FVXGDK
 */
public SelectionRequestor(NameLookup nameLookup, Openable openable) {
	super();
	this.nameLookup = nameLookup;
	this.openable = openable;
}
private void acceptBinaryMethod(
		IType type,
		IFunction method,
		char[] uniqueKey,
		boolean isConstructor) {
	try {
		if(!isConstructor || ((JavaElement)method).getSourceMapper() == null) {
			if (uniqueKey != null) {
				ResolvedBinaryMethod resolvedMethod = new ResolvedBinaryMethod(
						(JavaElement)method.getParent(),
						method.getElementName(),
						method.getParameterTypes(),
						new String(uniqueKey));
				resolvedMethod.occurrenceCount = method.getOccurrenceCount();
				method = resolvedMethod;
			}

			addElement(method);
			if(SelectionEngine.DEBUG){
				System.out.print("SELECTION - accept method("); //$NON-NLS-1$
				System.out.print(method.toString());
				System.out.println(")"); //$NON-NLS-1$
			}
		} else {
			ISourceRange range = method.getSourceRange();
			if (range.getOffset() != -1 && range.getLength() != 0 ) {
				if (uniqueKey != null) {
					ResolvedBinaryMethod resolvedMethod = new ResolvedBinaryMethod(
							(JavaElement)method.getParent(),
							method.getElementName(),
							method.getParameterTypes(),
							new String(uniqueKey));
					resolvedMethod.occurrenceCount = method.getOccurrenceCount();
					method = resolvedMethod;
				}
				addElement(method);
				if(SelectionEngine.DEBUG){
					System.out.print("SELECTION - accept method("); //$NON-NLS-1$
					System.out.print(method.toString());
					System.out.println(")"); //$NON-NLS-1$
				}
			} else {
				// no range was actually found, but a method was originally given -> default constructor
				addElement(type);
				if(SelectionEngine.DEBUG){
					System.out.print("SELECTION - accept type("); //$NON-NLS-1$
					System.out.print(type.toString());
					System.out.println(")"); //$NON-NLS-1$
				}
			}
		}
	} catch (JavaScriptModelException e) {
		// an exception occurs, return nothing
	}
}
/**
 * Resolve the binary method
 *
 * fix for 1FWFT6Q
 */
protected void acceptBinaryMethod(
		IType type,
		char[] selector,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		String[] parameterSignatures,
		char[][] typeParameterNames,
		char[][][] typeParameterBoundNames,
		char[] uniqueKey,
		boolean isConstructor) {
	IFunction method= type.getFunction(new String(selector), parameterSignatures);

	if (method.exists()) {
		if (typeParameterNames != null && typeParameterNames.length != 0) {
			IFunction[] methods = type.findMethods(method);
			if (methods.length > 1) {
				for (int i = 0; i < methods.length; i++) {
					acceptBinaryMethod(type, method, uniqueKey, isConstructor);
				}
				return;
			}
		}
		acceptBinaryMethod(type, method, uniqueKey, isConstructor);
	}
}
/**
 * Resolve the type.
 */
public void acceptType(char[] packageName, 		char[] fileName,char[] typeName, int modifiers, boolean isDeclaration, char[] uniqueKey, int start, int end) {
	int acceptFlags = 0;
	int kind = 0;
	switch (kind) {
		default:
			acceptFlags = NameLookup.ACCEPT_CLASSES;
			break;
	}
	IType type = null;
	if(isDeclaration) {
		type = resolveTypeByLocation(packageName, typeName, acceptFlags, start, end);
	} else {
		type = resolveType(packageName,fileName, typeName, acceptFlags);
		if(type != null ) {
			String key = uniqueKey == null ? type.getKey() : new String(uniqueKey);
//			if(type.isBinary()) {
//				ResolvedBinaryType resolvedType = new ResolvedBinaryType((JavaElement)type.getParent(), type.getElementName(), key);
//				resolvedType.occurrenceCount = type.getOccurrenceCount();
//				type = resolvedType;
//			} else {
				ResolvedSourceType resolvedType = new ResolvedSourceType((JavaElement)type.getParent(), type.getElementName(), key);
				resolvedType.occurrenceCount = type.getOccurrenceCount();
				type = resolvedType;
//			}
		}
	}

	if (type != null) {
		addElement(type);
		if(SelectionEngine.DEBUG){
			System.out.print("SELECTION - accept type("); //$NON-NLS-1$
			System.out.print(type.toString());
			System.out.println(")"); //$NON-NLS-1$
		}
	}
}
/**
 * @see ISelectionRequestor#acceptError
 */
public void acceptError(CategorizedProblem error) {
	// do nothing
}
/**
 * Resolve the field.
 */
public void acceptField(char[] declaringTypePackageName, char[] fileName, char[] declaringTypeName, char[] name, boolean isDeclaration, char[] uniqueKey, int start, int end) {
	if(isDeclaration) {
		IType type= resolveTypeByLocation(declaringTypePackageName, declaringTypeName,
				NameLookup.ACCEPT_ALL,
				start, end);
		if(type != null) {
			try {
				IField[] fields = type.getFields();
				for (int i = 0; i < fields.length; i++) {
					IField field = fields[i];
					ISourceRange range = field.getNameRange();
					if(range.getOffset() <= start
							&& range.getOffset() + range.getLength() >= end
							&& field.getElementName().equals(new String(name))) {
						addElement(fields[i]);
						if(SelectionEngine.DEBUG){
							System.out.print("SELECTION - accept field("); //$NON-NLS-1$
							System.out.print(field.toString());
							System.out.println(")"); //$NON-NLS-1$
						}
						return; // only one method is possible
					}
				}
			} catch (JavaScriptModelException e) {
				return;
			}
		}
	} else {
		IType type= resolveType(declaringTypePackageName, fileName, declaringTypeName, NameLookup.ACCEPT_ALL);
		if (type != null) {
			IField field= type.getField(new String(name));
			if (field.exists()) {
				if (uniqueKey != null) {
//					if(field.isBinary()) {
//						ResolvedBinaryField resolvedField = new ResolvedBinaryField(
//								(JavaElement)field.getParent(),
//								field.getElementName(),
//								new String(uniqueKey));
//						resolvedField.occurrenceCount = field.getOccurrenceCount();
//						field = resolvedField;
//					} else {
						ResolvedSourceField resolvedField = new ResolvedSourceField(
								(JavaElement)field.getParent(),
								field.getElementName(),
								new String(uniqueKey));
						resolvedField.occurrenceCount = field.getOccurrenceCount();
						field = resolvedField;
//					}
				}
				addElement(field);
				if(SelectionEngine.DEBUG){
					System.out.print("SELECTION - accept field("); //$NON-NLS-1$
					System.out.print(field.toString());
					System.out.println(")"); //$NON-NLS-1$
				}
			} else {
				IFunction method = type.getFunction(new String(name), null);
				if(method.exists()) {
					if (uniqueKey != null) {
						ResolvedSourceField resolvedField = new ResolvedSourceField(
								(JavaElement)method.getParent(),
								method.getElementName(),
								new String(uniqueKey));
						resolvedField.occurrenceCount = method.getOccurrenceCount();
						field = resolvedField;
					}
					addElement(field);
					if(SelectionEngine.DEBUG){
						System.out.print("SELECTION - accept field("); //$NON-NLS-1$
						System.out.print(field.toString());
						System.out.println(")"); //$NON-NLS-1$
					}
				}
			}
		}
	}
}
public void acceptLocalField(FieldBinding fieldBinding) {
	IJavaScriptElement res;
	
	SourceTypeBinding typeBinding = (SourceTypeBinding)fieldBinding.declaringClass;
	res = findLocalElement(typeBinding.sourceStart());
	
	if (res != null && res.getElementType() == IJavaScriptElement.TYPE) {
		IType type = (IType) res;
		IField field= type.getField(new String(fieldBinding.name));
		if (field.exists()) {
			char[] uniqueKey = fieldBinding.computeUniqueKey();
			if(field.isBinary()) {
				ResolvedBinaryField resolvedField = new ResolvedBinaryField(
						(JavaElement)field.getParent(),
						field.getElementName(),
						new String(uniqueKey));
				resolvedField.occurrenceCount = field.getOccurrenceCount();
				field = resolvedField;
			} else {
				ResolvedSourceField resolvedField = new ResolvedSourceField(
						(JavaElement)field.getParent(),
						field.getElementName(),
						new String(uniqueKey));
				resolvedField.occurrenceCount = field.getOccurrenceCount();
				field = resolvedField;
			}
			addElement(field);
			if(SelectionEngine.DEBUG){
				System.out.print("SELECTION - accept field("); //$NON-NLS-1$
				System.out.print(field.toString());
				System.out.println(")"); //$NON-NLS-1$
			}
		}
	}
}
public void acceptLocalMethod(MethodBinding methodBinding) {
	IJavaScriptElement res = findLocalElement(methodBinding.sourceStart());
	if(res != null) {
		if(res.getElementType() == IJavaScriptElement.METHOD) {
			IFunction method = (IFunction) res;

			char[] uniqueKey = methodBinding.computeUniqueKey();
			if(method.isBinary()) {
				ResolvedBinaryMethod resolvedRes = new ResolvedBinaryMethod(
						(JavaElement)res.getParent(),
						method.getElementName(),
						method.getParameterTypes(),
						new String(uniqueKey));
				resolvedRes.occurrenceCount = method.getOccurrenceCount();
				res = resolvedRes;
			} else {
				ResolvedSourceMethod resolvedRes = new ResolvedSourceMethod(
						(JavaElement)res.getParent(),
						method.getElementName(),
						method.getParameterTypes(),
						new String(uniqueKey));
				resolvedRes.occurrenceCount = method.getOccurrenceCount();
				res = resolvedRes;
			}
			addElement(res);
			if(SelectionEngine.DEBUG){
				System.out.print("SELECTION - accept method("); //$NON-NLS-1$
				System.out.print(res.toString());
				System.out.println(")"); //$NON-NLS-1$
			}
		} else if(methodBinding.selector == TypeConstants.INIT && res.getElementType() == IJavaScriptElement.TYPE) {
			// it's a default constructor
			res = ((JavaElement)res).resolved(methodBinding.declaringClass);
			addElement(res);
			if(SelectionEngine.DEBUG){
				System.out.print("SELECTION - accept type("); //$NON-NLS-1$
				System.out.print(res.toString());
				System.out.println(")"); //$NON-NLS-1$
			}
		}
	}
}
public void acceptLocalType(TypeBinding typeBinding) {
	IJavaScriptElement res =  null;
	if(typeBinding instanceof SourceTypeBinding) {
		res = findLocalElement(((SourceTypeBinding)typeBinding).sourceStart());
	}
	if(res != null && res.getElementType() == IJavaScriptElement.TYPE) {
		res = ((JavaElement)res).resolved(typeBinding);
		addElement(res);
		if(SelectionEngine.DEBUG){
			System.out.print("SELECTION - accept type("); //$NON-NLS-1$
			System.out.print(res.toString());
			System.out.println(")"); //$NON-NLS-1$
		}
	}
}
public void acceptLocalVariable(LocalVariableBinding binding) {
	LocalDeclaration local = binding.declaration;
	if (local==null)
		return;
	IJavaScriptElement parent =null;
	if (binding.declaringScope instanceof CompilationUnitScope) {
		CompilationUnitScope compilationUnitScope = (CompilationUnitScope) binding.declaringScope;
		char [] packageName=CharOperation.concatWith(compilationUnitScope.currentPackageName, '.');
		char[] fileName = compilationUnitScope.referenceContext.compilationUnitBinding.qualifiedSourceName();

		parent=resolveCompilationUnit(packageName, fileName);
		if(parent==null) {
			parent=resolveCompilationUnit(CharOperation.NO_CHAR, fileName);
		}
	} else
	 parent = findLocalElement(local.sourceStart); // findLocalElement() cannot find local variable
	IJavaScriptElement localVar = null;
	if(parent != null) {
		String localName=new String(local.name);
			// this may have been put in model as Source Field
		if (parent instanceof SourceField) {
			SourceField sourceField = (SourceField) parent;
			if (sourceField.name.equals(localName))
				parent=sourceField.getParent();
		}
		localVar = new LocalVariable(
				(JavaElement)parent,
				localName,
				local.declarationSourceStart,
				local.declarationSourceEnd,
				local.sourceStart,
				local.sourceEnd,
				Util.typeSignature(local.type));
	}
	if (localVar != null) {
		addElement(localVar);
		if(SelectionEngine.DEBUG){
			System.out.print("SELECTION - accept local variable("); //$NON-NLS-1$
			System.out.print(localVar.toString());
			System.out.println(")"); //$NON-NLS-1$
		}
	}

}
/**
 * Resolve the method
 */
public void acceptMethod(
		char[] declaringTypePackageName,
		char[] fileName,
		char[] declaringTypeName,
		String enclosingDeclaringTypeSignature,
		char[] selector,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		String[] parameterSignatures,
		char[][] typeParameterNames,
		char[][][] typeParameterBoundNames,
		boolean isConstructor,
		boolean isDeclaration,
		char[] uniqueKey,
		int start,
		int end) {
	IJavaScriptElement[] previousElement = this.elements;
	int previousElementIndex = this.elementIndex;
	this.elements = JavaElement.NO_ELEMENTS;
	this.elementIndex = -1;
	boolean isFileName= isFileName(declaringTypeName);


	if(isDeclaration) {
		IType type = resolveTypeByLocation(declaringTypePackageName, declaringTypeName,
				NameLookup.ACCEPT_ALL,
				start, end);

//		if(type != null) {
			this.acceptMethodDeclaration(type, selector, start, end);
//		}
	} else {
		IJavaScriptElement parent = (!isFileName) ?
				resolveType(declaringTypePackageName, fileName,declaringTypeName,NameLookup.ACCEPT_ALL)
			:
				resolveCompilationUnit(declaringTypePackageName, declaringTypeName);		// fix for 1FWFT6Q
//		if (type != null) {
//			if (type.isBinary()) {
//
//				// need to add a paramater for constructor in binary type
//				IType declaringDeclaringType = type.getDeclaringType();
//
//				boolean isStatic = false;
//				try {
//					isStatic = Flags.isStatic(type.getFlags());
//				} catch (JavaScriptModelException e) {
//					// isStatic == false
//				}
//
//				if(declaringDeclaringType != null && isConstructor	&& !isStatic) {
//					int length = parameterPackageNames.length;
//					System.arraycopy(parameterPackageNames, 0, parameterPackageNames = new char[length+1][], 1, length);
//					System.arraycopy(parameterTypeNames, 0, parameterTypeNames = new char[length+1][], 1, length);
//					System.arraycopy(parameterSignatures, 0, parameterSignatures = new String[length+1], 1, length);
//
//					parameterPackageNames[0] = declaringDeclaringType.getPackageFragment().getElementName().toCharArray();
//					parameterTypeNames[0] = declaringDeclaringType.getTypeQualifiedName().toCharArray();
//					parameterSignatures[0] = Signature.getTypeErasure(enclosingDeclaringTypeSignature);
//				}
//
//				acceptBinaryMethod(type, selector, parameterPackageNames, parameterTypeNames, parameterSignatures, typeParameterNames, typeParameterBoundNames, uniqueKey, isConstructor);
//			} else {
				acceptSourceMethod(parent, selector, parameterPackageNames, parameterTypeNames, parameterSignatures, typeParameterNames, typeParameterBoundNames, uniqueKey);
//			}
//		}
	}

	if(previousElementIndex > -1) {
		int elementsLength = this.elementIndex + previousElementIndex + 2;
		if(elementsLength > this.elements.length) {
			System.arraycopy(this.elements, 0, this.elements = new IJavaScriptElement[elementsLength * 2 + 1], 0, this.elementIndex + 1);
		}
		System.arraycopy(previousElement, 0, this.elements, this.elementIndex + 1, previousElementIndex + 1);
		this.elementIndex += previousElementIndex + 1;
	}
}

private static final char [] js={'.','j','s'};
private static boolean isFileName(char [] name)
{
	return  (CharOperation.endsWith(name, js) && CharOperation.contains('/', name));
}
/**
 * Resolve the package
 */
public void acceptPackage(char[] packageName) {
	IPackageFragment[] pkgs = this.nameLookup.findPackageFragments(new String(packageName), false);
	if (pkgs != null) {
		for (int i = 0, length = pkgs.length; i < length; i++) {
			addElement(pkgs[i]);
			if(SelectionEngine.DEBUG){
				System.out.print("SELECTION - accept package("); //$NON-NLS-1$
				System.out.print(pkgs[i].toString());
				System.out.println(")"); //$NON-NLS-1$
			}
		}
	}
}
/**
 * Resolve the source method
 *
 * fix for 1FWFT6Q
 */
protected void acceptSourceMethod(
		IJavaScriptElement parent,
		char[] selector,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		String[] parameterSignatures,
		char[][] typeParameterNames,
		char[][][] typeParameterBoundNames,
		char[] uniqueKey) {

	String name = new String(selector);
	IFunction[] methods = null;
	try {
		if (parent instanceof IType)
			methods = ((IType)parent).getFunctions();
		else if (parent instanceof IJavaScriptUnit)
			methods=((IJavaScriptUnit)parent).getFunctions();
		else if (parent instanceof IClassFile)
		{
			methods=((IClassFile)parent).getFunctions();
		}
		if (methods!=null)
			for (int i = 0; i < methods.length; i++) {
			if (methods[i].getElementName().equals(name)
//					&& methods[i].getParameterTypes().length == parameterTypeNames.length
					) {
				IFunction method = methods[i];
				if (uniqueKey != null) {
					ResolvedSourceMethod resolvedMethod = new ResolvedSourceMethod(
						(JavaElement)method.getParent(),
						method.getElementName(),
						method.getParameterTypes(),
						new String(uniqueKey));
					resolvedMethod.occurrenceCount = method.getOccurrenceCount();
					method = resolvedMethod;
				}
				addElement(method);
			}
		}
	} catch (JavaScriptModelException e) {
		return;
	}

	// if no matches, nothing to report
	if (this.elementIndex == -1) {
		// no match was actually found, but a method was originally given -> default constructor
		addElement(parent);
		if(SelectionEngine.DEBUG){
			System.out.print("SELECTION - accept type("); //$NON-NLS-1$
			System.out.print(parent.toString());
			System.out.println(")"); //$NON-NLS-1$
		}
		return;
	}

	// if there is only one match, we've got it
	if (this.elementIndex == 0) {
		if(SelectionEngine.DEBUG){
			System.out.print("SELECTION - accept method("); //$NON-NLS-1$
			System.out.print(this.elements[0].toString());
			System.out.println(")"); //$NON-NLS-1$
		}
		return;
	}

	// more than one match - must match simple parameter types
	IJavaScriptElement[] matches = this.elements;
	int matchesIndex = this.elementIndex;
	this.elements = JavaElement.NO_ELEMENTS;
	this.elementIndex = -1;
	for (int i = 0; i <= matchesIndex; i++) {
		IFunction method= (IFunction)matches[i];
		String[] signatures = method.getParameterTypes();
		boolean match= true;
		for (int p = 0; p < signatures.length; p++) {
			String simpleName= Signature.getSimpleName(Signature.toString(signatures[p]));
			char[] simpleParameterName = CharOperation.lastSegment(parameterTypeNames[p], '.');
			if (!simpleName.equals(new String(simpleParameterName))) {
				match = false;
				break;
			}
		}

		if (match) {
			addElement(method);
			if(SelectionEngine.DEBUG){
				System.out.print("SELECTION - accept method("); //$NON-NLS-1$
				System.out.print(method.toString());
				System.out.println(")"); //$NON-NLS-1$
			}
		}
	}

}
protected void acceptMethodDeclaration(IType type, char[] selector, int start, int end) {
	String name = new String(selector);
	IFunction[] methods = null;
	try {
		if (type!=null)
			methods = type.getFunctions();
			else
				if (this.openable instanceof CompilationUnit)
					methods=((CompilationUnit)this.openable).getFunctions();
				else if (this.openable instanceof ClassFile)
				methods=((ClassFile)this.openable).getFunctions();
		for (int i = 0; i < methods.length; i++) {
			ISourceRange range = methods[i].getNameRange();
			if(range.getOffset() <= start
					&& range.getOffset() + range.getLength() >= end
					&& methods[i].getElementName().equals(name)) {
				addElement(methods[i]);
				if(SelectionEngine.DEBUG){
					System.out.print("SELECTION - accept method("); //$NON-NLS-1$
					System.out.print(this.elements[0].toString());
					System.out.println(")"); //$NON-NLS-1$
				}
				return; // only one method is possible
			}
		}
	} catch (JavaScriptModelException e) {
		return;
	}

	// no match was actually found
	addElement(type);
	if(SelectionEngine.DEBUG){
		System.out.print("SELECTION - accept type("); //$NON-NLS-1$
		System.out.print(type.toString());
		System.out.println(")"); //$NON-NLS-1$
	}
	return;
}
public void acceptTypeParameter(char[] declaringTypePackageName, char[] fileName, char[] declaringTypeName, char[] typeParameterName, boolean isDeclaration, int start, int end) {
	IType type;
	if(isDeclaration) {
		type = resolveTypeByLocation(declaringTypePackageName, declaringTypeName,
				NameLookup.ACCEPT_ALL,
				start, end);
	} else {
		type = resolveType(declaringTypePackageName, fileName, declaringTypeName,
				NameLookup.ACCEPT_ALL);
	}

	if(type != null) {
		addElement(type);
		if(SelectionEngine.DEBUG){
			System.out.print("SELECTION - accept type("); //$NON-NLS-1$
			System.out.print(type.toString());
			System.out.println(")"); //$NON-NLS-1$
		}
	}
}
public void acceptMethodTypeParameter(char[] declaringTypePackageName, char[] fileName, char[] declaringTypeName, char[] selector,int selectorStart, int selectorEnd, char[] typeParameterName, boolean isDeclaration, int start, int end) {
	IType type = resolveTypeByLocation(declaringTypePackageName, declaringTypeName,
			NameLookup.ACCEPT_ALL,
			selectorStart, selectorEnd);

	if(type != null) {
		IFunction method = null;

		String name = new String(selector);
		IFunction[] methods = null;

		try {
			methods = type.getFunctions();
			done : for (int i = 0; i < methods.length; i++) {
				ISourceRange range = methods[i].getNameRange();
				if(range.getOffset() >= selectorStart
						&& range.getOffset() + range.getLength() <= selectorEnd
						&& methods[i].getElementName().equals(name)) {
					method = methods[i];
					break done;
				}
			}
		} catch (JavaScriptModelException e) {
			//nothing to do
		}

		if(method == null) {
			addElement(type);
			if(SelectionEngine.DEBUG){
				System.out.print("SELECTION - accept type("); //$NON-NLS-1$
				System.out.print(type.toString());
				System.out.println(")"); //$NON-NLS-1$
			}
		} else {
			addElement(method);
			if(SelectionEngine.DEBUG){
				System.out.print("SELECTION - accept method("); //$NON-NLS-1$
				System.out.print(method.toString());
				System.out.println(")"); //$NON-NLS-1$
			}
		}
	}
}
/*
 * Adds the given element to the list of resolved elements.
 */
protected void addElement(IJavaScriptElement element) {
	int elementLength = this.elementIndex + 1;
	if (elementLength == this.elements.length) {
		System.arraycopy(this.elements, 0, this.elements = new IJavaScriptElement[(elementLength*2) + 1], 0, elementLength);
	}
	this.elements[++this.elementIndex] = element;
}
/*
 * findLocalElement() cannot find local variable
 */
protected IJavaScriptElement findLocalElement(int pos) {
	IJavaScriptElement res = null;
	if(this.openable instanceof ITypeRoot) {
		ITypeRoot cu = (ITypeRoot) this.openable;
		try {
			res = cu.getElementAt(pos);
		} catch (JavaScriptModelException e) {
			// do nothing
		}
	} else if (this.openable instanceof ClassFile) {
		ClassFile cf = (ClassFile) this.openable;
		try {
			 res = cf.getElementAtConsideringSibling(pos);
		} catch (JavaScriptModelException e) {
			// do nothing
		}
	}
	return res;
}
/**
 * Returns the resolved elements.
 */
public IJavaScriptElement[] getElements() {
	int elementLength = this.elementIndex + 1;
	if (this.elements.length != elementLength) {
		System.arraycopy(this.elements, 0, this.elements = new IJavaScriptElement[elementLength], 0, elementLength);
	}
	return this.elements;
}
/**
 * Resolve the type
 */
protected IType resolveType(char[] packageName, char[] fileName, char[] typeName, int acceptFlags) {

	IType type= null;

	if (fileName!=null)
	{
		ITypeRoot compilationUnit = (ITypeRoot)resolveCompilationUnit(packageName, fileName);
		if (compilationUnit!=null && compilationUnit.exists())
		{
			 String fulltypeName=null;
			 if (packageName==null || packageName.length==0)
			    fulltypeName=new String(typeName);
			 else {
				fulltypeName=new String(CharOperation.concat(packageName, typeName, '.'));
			}
			 type=compilationUnit.getType(fulltypeName);
			 if (type!=null && type.exists())
				 return type;
		}
		
	}
	
	if (this.openable instanceof CompilationUnit && ((CompilationUnit)this.openable).isWorkingCopy()) {
		CompilationUnit wc = (CompilationUnit) this.openable;
		if(((packageName == null || packageName.length == 0))) {

			char[][] compoundName = CharOperation.splitOn('.', typeName);
			if(compoundName.length > 0) {
				type = wc.getType(new String(compoundName[0]));
				for (int i = 1, length = compoundName.length; i < length; i++) {
					type = type.getType(new String(compoundName[i]));
				}
			}

			if(type != null && !type.exists()) {
				type = null;
			}
		}
	}

	if(type == null) {
		IPackageFragment[] pkgs = this.nameLookup.findPackageFragments(
			(packageName == null || packageName.length == 0) ? IPackageFragment.DEFAULT_PACKAGE_NAME : new String(packageName),
			false);
		// iterate type lookup in each package fragment
		for (int i = 0, length = pkgs == null ? 0 : pkgs.length; i < length; i++) {
			type= this.nameLookup.findType(new String(typeName), pkgs[i], false, acceptFlags, true/*consider secondary types*/);
			if (type != null) break;
		}
		if (type == null) {
			String pName= IPackageFragment.DEFAULT_PACKAGE_NAME;
			if (packageName != null) {
				pName = new String(packageName);
			}
			if (this.openable != null && this.openable.getParent().getElementName().equals(pName)) {
				// look inside the type in which we are resolving in
				String tName= new String(typeName);
//				tName = tName.replace('.','$');
				IType[] allTypes= null;
				try {
					ArrayList list = this.openable.getChildrenOfType(IJavaScriptElement.TYPE);
					allTypes = new IType[list.size()];
					list.toArray(allTypes);
				} catch (JavaScriptModelException e) {
					return null;
				}
				for (int i= 0; i < allTypes.length; i++) {
					if (allTypes[i].getTypeQualifiedName().equals(tName)) {
						return allTypes[i];
					}
				}
			}
		}
	}
	return type;
}


protected IJavaScriptElement resolveCompilationUnit(char[] packageName, char[] compilationUnitName) {

	IJavaScriptUnit compilationUnit= null;

	String fullCUName=new String(compilationUnitName);
	Path cuPath = new Path(fullCUName);
	String cuName=cuPath.lastSegment();

	Openable cu = new HandleFactory().createOpenable(fullCUName,null);
	if ((cu instanceof CompilationUnit || cu instanceof ClassFile)&& cu.exists()) 
		return cu;
	
	if (this.openable instanceof CompilationUnit || this.openable instanceof ClassFile) {
		if ( (cuName.equals(this.openable.getElementName()) &&
				new String(packageName).equals(this.openable.getParent().getElementName())
				) || fullCUName.startsWith("http:")&&fullCUName.equals(this.openable.getElementName())) //$NON-NLS-1$
		{
			return this.openable;
		}
	}

		IPackageFragment[] pkgs = this.nameLookup.findPackageFragments(
			(packageName == null || packageName.length == 0) ? IPackageFragment.DEFAULT_PACKAGE_NAME : new String(packageName),
			false);
		// iterate type lookup in each package fragment
		for (int i = 0, length = pkgs == null ? 0 : pkgs.length; i < length; i++) {
			if (!Util.isMetadataFileName(cuName))
			{
			IJavaScriptUnit compUnit=pkgs[i].getJavaScriptUnit(cuName);
			if (compUnit.exists())
				return compUnit;
			}
			IClassFile classFile=pkgs[i].getClassFile(cuName);
			if (classFile.exists())
				return classFile;
			//compUnit = pkgs[i].getCompilationUnit(fullCUName);
			//if(compUnit.exists()) return compUnit;
			//classFile=pkgs[i].getClassFile(fullCUName);
			//if (classFile.exists())
			//	return classFile;

		}
//		if (type == null) {
//			String pName= IPackageFragment.DEFAULT_PACKAGE_NAME;
//			if (packageName != null) {
//				pName = new String(packageName);
//			}
//			if (this.openable != null && this.openable.getParent().getElementName().equals(pName)) {
//				// look inside the type in which we are resolving in
//				String tName= new String(typeName);
//				tName = tName.replace('.','$');
//				IType[] allTypes= null;
//				try {
//					ArrayList list = this.openable.getChildrenOfType(IJavaScriptElement.TYPE);
//					allTypes = new IType[list.size()];
//					list.toArray(allTypes);
//				} catch (JavaScriptModelException e) {
//					return null;
//				}
//				for (int i= 0; i < allTypes.length; i++) {
//					if (allTypes[i].getTypeQualifiedName().equals(tName)) {
//						return allTypes[i];
//					}
//				}
//			}
//		}
	return compilationUnit;
}


protected IType resolveTypeByLocation(char[] packageName, char[] typeName, int acceptFlags, int start, int end) {

	IType type= null;

	// TODO (david) post 3.0 should remove isOpen check, and investigate reusing IJavaScriptUnit#getElementAt. may need to optimize #getElementAt to remove recursions
	if (this.openable instanceof CompilationUnit && ((CompilationUnit)this.openable).isOpen()) {
		CompilationUnit wc = (CompilationUnit) this.openable;
		try {
			if(((packageName == null || packageName.length == 0))) {

				char[][] compoundName = CharOperation.splitOn('.', typeName);
				if(compoundName.length > 0) {

					IType[] tTypes = wc.getTypes();
					int i = 0;
					int depth = 0;
					done : while(i < tTypes.length) {
						ISourceRange range = tTypes[i].getSourceRange();
						if(range.getOffset() <= start
								&& range.getOffset() + range.getLength() >= end
								&& tTypes[i].getElementName().equals(new String(compoundName[depth]))) {
							if(depth == compoundName.length - 1) {
								type = tTypes[i];
								break done;
							}
							tTypes = tTypes[i].getTypes();
							i = 0;
							depth++;
							continue done;
						}
						i++;
					}
				}

				IType[] tTypes = wc.getTypes();
				String typeNameString = new String(typeName);
				for (int i = 0; i < tTypes.length && type == null; i++) {
					ISourceRange range = tTypes[i].getSourceRange();
					if (range.getOffset() <= start && range.getOffset() + range.getLength() >= end && tTypes[i].getElementName().equals(typeNameString)) {
						type = tTypes[i];
					}
				}

				if(type != null && !type.exists()) {
					type = null;
				}
			}
		}catch (JavaScriptModelException e) {
			// type is null
		}
	}

	if(type == null&& typeName!=null) {
//		IPackageFragment[] pkgs = this.nameLookup.findPackageFragments(
//			(packageName == null || packageName.length == 0) ? IPackageFragment.DEFAULT_PACKAGE_NAME : new String(packageName),
//			false);
//		// iterate type lookup in each package fragment
//		for (int i = 0, length = pkgs == null ? 0 : pkgs.length; i < length; i++) {
//			type= this.nameLookup.findType(new String(typeName), pkgs[i], false, acceptFlags, true/*consider secondary types*/);
//			if (type != null) break;
//		}
		type=this.nameLookup.findType(new String(typeName),false,acceptFlags);
		if (type == null) {
			String pName= IPackageFragment.DEFAULT_PACKAGE_NAME;
			if (packageName != null) {
				pName = new String(packageName);
			}
			if (this.openable != null && this.openable.getParent().getElementName().equals(pName)) {
				// look inside the type in which we are resolving in
				String tName= new String(typeName);
//				tName = tName.replace('.','$');
				IType[] allTypes= null;
				try {
					ArrayList list = this.openable.getChildrenOfType(IJavaScriptElement.TYPE);
					allTypes = new IType[list.size()];
					list.toArray(allTypes);
				} catch (JavaScriptModelException e) {
					return null;
				}
				for (int i= 0; i < allTypes.length; i++) {
					if (allTypes[i].getTypeQualifiedName().equals(tName)) {
						return allTypes[i];
					}
				}
			}
		}
	}
	return type;
}
}
