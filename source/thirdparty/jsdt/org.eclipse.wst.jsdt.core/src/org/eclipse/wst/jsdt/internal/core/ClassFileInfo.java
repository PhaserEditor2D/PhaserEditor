/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import java.util.HashMap;

import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryField;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryNestedType;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryType;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;

/**
 * Element info for <code>ClassFile</code> handles.
 */

/* package */ class ClassFileInfo extends OpenableElementInfo implements SuffixConstants {
	/**
	 * The children of the <code>BinaryType</code> corresponding to our
	 * <code>ClassFile</code>. These are kept here because we don't have
	 * access to the <code>BinaryType</code> info (<code>ClassFileReader</code>).
	 */
	protected JavaElement[] binaryChildren = null;

/**
 * Creates the handles and infos for the fields of the given binary type.
 * Adds new handles to the given vector.
 */
private void generateFieldInfos(IType type, IBinaryType typeInfo, HashMap newElements, ArrayList childrenHandles) {
	// Make the fields
	IBinaryField[] fields = typeInfo.getFields();
	if (fields == null) {
		return;
	}
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	for (int i = 0, fieldCount = fields.length; i < fieldCount; i++) {
		IBinaryField fieldInfo = fields[i];
		IField field = new BinaryField((JavaElement)type, manager.intern(new String(fieldInfo.getName())));
		newElements.put(field, fieldInfo);
		childrenHandles.add(field);
	}
}
/**
 * Creates the handles for the inner types of the given binary type.
 * Adds new handles to the given vector.
 */
private void generateInnerClassHandles(IType type, IBinaryType typeInfo, ArrayList childrenHandles) {
	// Add inner types
	// If the current type is an inner type, innerClasses returns
	// an extra entry for the current type.  This entry must be removed.
	// Can also return an entry for the enclosing type of an inner type.
	IBinaryNestedType[] innerTypes = typeInfo.getMemberTypes();
	if (innerTypes != null) {
		IPackageFragment pkg = (IPackageFragment) type.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT);
		for (int i = 0, typeCount = innerTypes.length; i < typeCount; i++) {
			IBinaryNestedType binaryType = innerTypes[i];
			IClassFile parentClassFile= pkg.getClassFile(new String(ClassFile.unqualifiedName(binaryType.getName())) + SUFFIX_STRING_java);
			IType innerType = new BinaryType((JavaElement) parentClassFile, ClassFile.simpleName(binaryType.getName()));
			childrenHandles.add(innerType);
		}
	}
}
/**
 * Creates the handles and infos for the methods of the given binary type.
 * Adds new handles to the given vector.
 */
private void generateMethodInfos(IType type, IBinaryType typeInfo, HashMap newElements, ArrayList childrenHandles, ArrayList typeParameterHandles) {
	IBinaryMethod[] methods = typeInfo.getMethods();
	if (methods == null) {
		return;
	}
	for (int i = 0, methodCount = methods.length; i < methodCount; i++) {
		IBinaryMethod methodInfo = methods[i];
		// TODO (jerome) filter out synthetic members
		//                        indexer should not index them as well
		// if ((methodInfo.getModifiers() & IConstants.AccSynthetic) != 0) continue; // skip synthetic
		char[] signature = methodInfo.getGenericSignature();
		if (signature == null) signature = methodInfo.getMethodDescriptor();
		String[] pNames = null;
		try {
			pNames = Signature.getParameterTypes(new String(signature));
		} catch (IllegalArgumentException e) {
			// protect against malformed .class file (e.g. com/sun/crypto/provider/SunJCE_b.class has a 'a' generic signature)
			signature = methodInfo.getMethodDescriptor();
			pNames = Signature.getParameterTypes(new String(signature));
		}
		char[][] paramNames= new char[pNames.length][];
		for (int j= 0; j < pNames.length; j++) {
			paramNames[j]= pNames[j].toCharArray();
		}
		char[][] parameterTypes = ClassFile.translatedNames(paramNames);
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		String selector = new String(methodInfo.getSelector());
		if (methodInfo.isConstructor()) {
			selector =type.getElementName();
		}
		selector =  manager.intern(selector);
		for (int j= 0; j < pNames.length; j++) {
			pNames[j]= manager.intern(new String(parameterTypes[j]));
		}
		BinaryMethod method = new BinaryMethod((JavaElement)type, selector, pNames);
		childrenHandles.add(method);

		// ensure that 2 binary methods with the same signature but with different return types have different occurence counts.
		// (case of bridge methods in 1.5)
		while (newElements.containsKey(method))
			method.occurrenceCount++;

		newElements.put(method, methodInfo);
	}
}
/**
 * Returns true iff the <code>readBinaryChildren</code> has already
 * been called.
 */
boolean hasReadBinaryChildren() {
	return this.binaryChildren != null;
}
/**
 * Creates the handles for <code>BinaryMember</code>s defined in this
 * <code>ClassFile</code> and adds them to the
 * <code>JavaModelManager</code>'s cache.
 */
protected void readBinaryChildren(ClassFile classFile, HashMap newElements, IBinaryType typeInfo) {
	ArrayList childrenHandles = new ArrayList();
	BinaryType type = (BinaryType) classFile.getType();
	ArrayList typeParameterHandles = new ArrayList();
	if (typeInfo != null) { //may not be a valid class file
		generateFieldInfos(type, typeInfo, newElements, childrenHandles);
		generateMethodInfos(type, typeInfo, newElements, childrenHandles, typeParameterHandles);
		generateInnerClassHandles(type, typeInfo, childrenHandles); // Note inner class are separate openables that are not opened here: no need to pass in newElements
	}

	this.binaryChildren = new JavaElement[childrenHandles.size()];
	childrenHandles.toArray(this.binaryChildren);
}
/**
 * Removes the binary children handles and remove their infos from
 * the <code>JavaModelManager</code>'s cache.
 */
void removeBinaryChildren() throws JavaScriptModelException {
	if (this.binaryChildren != null) {
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		for (int i = 0; i <this.binaryChildren.length; i++) {
			JavaElement child = this.binaryChildren[i];
			if (child instanceof BinaryType) {
				manager.removeInfoAndChildren((JavaElement)child.getParent());
			} else {
				manager.removeInfoAndChildren(child);
			}
		}
		this.binaryChildren = JavaElement.NO_ELEMENTS;
	}
}
}
