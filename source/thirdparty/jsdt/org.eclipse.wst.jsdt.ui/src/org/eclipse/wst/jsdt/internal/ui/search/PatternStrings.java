/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.search;

import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class PatternStrings {

	public static String getSignature(IJavaScriptElement element) {
		if (element == null)
			return null;
		else
			switch (element.getElementType()) {
				case IJavaScriptElement.METHOD:
					return getMethodSignature((IFunction)element);
				case IJavaScriptElement.TYPE:
					return getTypeSignature((IType) element);
				case IJavaScriptElement.FIELD:
					return getFieldSignature((IField) element);
				default:
					return element.getElementName();
			}
	}
	
	public static String getMethodSignature(IFunction method) {
		StringBuffer buffer= new StringBuffer();
		if (method.getDeclaringType()!=null)
		{
			buffer.append(JavaScriptElementLabels.getElementLabel(
			method.getDeclaringType(), 
			JavaScriptElementLabels.T_FULLY_QUALIFIED | JavaScriptElementLabels.USE_RESOLVED));
			boolean isConstructor= method.getElementName().equals(method.getDeclaringType().getElementName());
			if (!isConstructor) {
				buffer.append('.');
			}
			
			buffer.append(getUnqualifiedMethodSignature(method, !isConstructor));
			
		}
		
		
		return buffer.toString();
	}
	
	private static String getUnqualifiedMethodSignature(IFunction method, boolean includeName) {
		StringBuffer buffer= new StringBuffer();
		if (includeName) {
			buffer.append(method.getElementName());
		}
		buffer.append('(');
		
		String[] types= method.getParameterTypes();
		for (int i= 0; i < types.length; i++) {
			if (i > 0)
				buffer.append(", "); //$NON-NLS-1$
			String typeSig= Signature.toString(types[i]);
			buffer.append(typeSig);
		}
		buffer.append(')');
		
		return buffer.toString();
	}

	public static String getUnqualifiedMethodSignature(IFunction method) {
		return getUnqualifiedMethodSignature(method, true);
	}

	public static String getTypeSignature(IType field) {
		return JavaScriptElementLabels.getElementLabel(field, 
			JavaScriptElementLabels.T_FULLY_QUALIFIED | JavaScriptElementLabels.T_TYPE_PARAMETERS | JavaScriptElementLabels.USE_RESOLVED);
	}	
	
	public static String getFieldSignature(IField field) {
		return JavaScriptElementLabels.getElementLabel(field, JavaScriptElementLabels.F_FULLY_QUALIFIED);
	}	
}
