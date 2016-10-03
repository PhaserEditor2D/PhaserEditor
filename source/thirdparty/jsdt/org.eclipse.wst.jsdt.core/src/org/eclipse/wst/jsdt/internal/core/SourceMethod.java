/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;

/**
 * @see IFunction
 */

public class SourceMethod extends NamedMember implements IFunction {

	/**
	 * The parameter type signatures of the method - stored locally
	 * to perform equality test. <code>null</code> indicates no
	 * parameters.
	 */
	protected String[] parameterTypes;

protected SourceMethod(JavaElement parent, String name, String[] parameterTypes) {
	super(parent, name);
	// Assertion disabled since bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=179011
	// Assert.isTrue(name.indexOf('.') == -1);
	if (parameterTypes == null) {
		this.parameterTypes= CharOperation.NO_STRINGS;
	} else {
		this.parameterTypes= parameterTypes;
	}
}
public boolean equals(Object o) {
	if (!(o instanceof SourceMethod)) return false;
	return super.equals(o);// && Util.equalArraysOrNull(this.parameterTypes, ((SourceMethod)o).parameterTypes);
}
/**
 * @see IJavaScriptElement
 */
public int getElementType() {
	return METHOD;
}
/**
 * @see JavaElement#getHandleMemento(StringBuffer)
 */
protected void getHandleMemento(StringBuffer buff) {
	((JavaElement) getParent()).getHandleMemento(buff);
	char delimiter = getHandleMementoDelimiter();
	buff.append(delimiter);
	escapeMementoName(buff, getElementName());
//	for (int i = 0; i < this.parameterTypes.length; i++) {
//		buff.append(delimiter);
//		escapeMementoName(buff, this.parameterTypes[i]);
//	}
	if (this.occurrenceCount > 1) {
		buff.append(JEM_COUNT);
		buff.append(this.occurrenceCount);
	}
}
/**
 * @see JavaElement#getHandleMemento()
 */
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_METHOD;
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.core.IFunction#getKey()
 */
public String getKey() {
	try {
		return getKey(this, false/*don't open*/);
	} catch (JavaScriptModelException e) {
		// happen only if force open is true
		return null;
	}
}
/**
 * @see IFunction
 */
public int getNumberOfParameters() {
	return this.parameterTypes == null ? 0 : this.parameterTypes.length;
}
/**
 * @see IFunction
 */
public String[] getParameterNames() throws JavaScriptModelException {
	SourceMethodElementInfo info = (SourceMethodElementInfo) getElementInfo();
	char[][] names= info.getArgumentNames();
	return CharOperation.toStrings(names);
}
/**
 * @see IFunction
 */
public String[] getParameterTypes() {
	return this.parameterTypes;
}

/*
 * @see JavaElement#getPrimaryElement(boolean)
 */
public IJavaScriptElement getPrimaryElement(boolean checkOwner) {
	if (checkOwner) {
		CompilationUnit cu = (CompilationUnit)getAncestor(JAVASCRIPT_UNIT);
		if (cu.isPrimary()) return this;
	}
	IJavaScriptElement primaryParent = this.parent.getPrimaryElement(false);
	if (primaryParent instanceof IType)
		return ((IType)primaryParent).getFunction(this.name, this.parameterTypes);
	return ((IJavaScriptUnit)primaryParent).getFunction(this.name, this.parameterTypes);
}
public String[] getRawParameterNames() throws JavaScriptModelException {
	return getParameterNames();
}
/**
 * @see IFunction
 */
public String getReturnType() throws JavaScriptModelException {
	SourceMethodElementInfo info = (SourceMethodElementInfo) getElementInfo();
	return  Signature.createTypeSignature(info.getReturnTypeName(), false) ;
}
/**
 * @see IFunction
 */
public String getSignature() throws JavaScriptModelException {
	SourceMethodElementInfo info = (SourceMethodElementInfo) getElementInfo();
	return Signature.createMethodSignature(this.parameterTypes, Signature.createTypeSignature(info.getReturnTypeName(), false));
}
/**
 * @see org.eclipse.wst.jsdt.internal.core.JavaElement#hashCode()
 */
public int hashCode() {
   int hash = super.hashCode();
//	for (int i = 0, length = this.parameterTypes.length; i < length; i++) {
//	    int hashCode = (this.parameterTypes[i]!=null) ? this.parameterTypes[i].hashCode() : "".hashCode();
//		hash = Util.combineHashCodes(hash, hashCode);
//	}
	return hash;
}
/**
 * @see IFunction
 */
public boolean isConstructor() throws JavaScriptModelException {
	SourceMethodElementInfo info = (SourceMethodElementInfo) getElementInfo();
	return info.isConstructor();
}
/**
 * @see IFunction#isMainMethod()
 */
public boolean isMainMethod() throws JavaScriptModelException {
	return this.isMainMethod(this);
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.core.IFunction#isResolved()
 */
public boolean isResolved() {
	return false;
}
/**
 * @see IFunction#isSimilar(IFunction)
 */
public boolean isSimilar(IFunction method) {
	return
		areSimilarMethods(
			this.getElementName(), this.getParameterTypes(),
			method.getElementName(), method.getParameterTypes(),
			null);
}

/**
 */
public String readableName() {

	StringBuffer buffer = new StringBuffer(super.readableName());
	buffer.append('(');
	int length;
	if (this.parameterTypes != null && (length = this.parameterTypes.length) > 0) {
		for (int i = 0; i < length; i++) {
			buffer.append(Signature.toString(this.parameterTypes[i]));
			if (i < length - 1) {
				buffer.append(", "); //$NON-NLS-1$
			}
		}
	}
	buffer.append(')');
	return buffer.toString();
}
public JavaElement resolved(Binding binding) {
	SourceRefElement resolvedHandle = new ResolvedSourceMethod(this.parent, this.name, this.parameterTypes, new String(binding.computeUniqueKey()));
	resolvedHandle.occurrenceCount = this.occurrenceCount;
	return resolvedHandle;
}
/**
 * @private Debugging purposes
 */
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(tabString(tab));
	if (info == null) {
		toStringName(buffer);
		buffer.append(" (not open)"); //$NON-NLS-1$
	} else if (info == NO_INFO) {
		toStringName(buffer);
	} else {
		SourceMethodElementInfo methodInfo = (SourceMethodElementInfo) info;
		int flags = methodInfo.getModifiers();
		if (Flags.isStatic(flags)) {
			buffer.append("static "); //$NON-NLS-1$
		}
		if (!methodInfo.isConstructor()) {
//			buffer.append(methodInfo.getReturnTypeName());
			buffer.append("function "); //$NON-NLS-1$
		}
		toStringName(buffer, flags);
	}
}
protected void toStringName(StringBuffer buffer) {
	toStringName(buffer, 0);
}
protected void toStringName(StringBuffer buffer, int flags) {
	buffer.append(getElementName());
	buffer.append('(');
	String[] parameters = getParameterTypes();
	int length;
	if (parameters != null && (length = parameters.length) > 0) {
		boolean isVarargs = Flags.isVarargs(flags);
		for (int i = 0; i < length; i++) {
			try {
				if (i < length - 1) {
//					buffer.append(Signature.toString(parameters[i]));
					buffer.append("p"+i); //$NON-NLS-1$
					buffer.append(", "); //$NON-NLS-1$
				} else if (isVarargs) {
					// remove array from signature
					String parameter = parameters[i].substring(1);
					buffer.append(Signature.toString(parameter));
					buffer.append(" ..."); //$NON-NLS-1$
				} else {
//					buffer.append(Signature.toString(parameters[i]));
					buffer.append("p"+i); //$NON-NLS-1$
				}
			} catch (IllegalArgumentException e) {
				// parameter signature is malformed
				buffer.append("*** invalid signature: "); //$NON-NLS-1$
				buffer.append(parameters[i]);
			}
		}
	}
	buffer.append(')');
	if (this.occurrenceCount > 1) {
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(this.occurrenceCount);
	}
}

public IFunction getFunction(String selector, String[] parameterTypeSignatures)
{
	return new SourceMethod(this, selector, parameterTypeSignatures);

}

public String getDisplayName() {
	String displayName = super.getDisplayName();
	if (displayName.equals("___anonymous")) //$NON-NLS-1$
		displayName = ""; //$NON-NLS-1$
	return displayName;
}

}
