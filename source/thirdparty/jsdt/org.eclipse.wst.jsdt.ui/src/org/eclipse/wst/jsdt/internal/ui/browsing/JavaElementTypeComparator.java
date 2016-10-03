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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import java.util.Comparator;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;

public class JavaElementTypeComparator implements Comparator {


	/**
	 * Compares two Java element types. A type is considered to be
	 * greater if it may contain the other.
	 *
	 * @return		an int less than 0 if object1 is less than object2,
	 *				0 if they are equal, and > 0 if object1 is greater
	 *
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2) {
		if (!(o1 instanceof IJavaScriptElement) || !(o2 instanceof IJavaScriptElement))
			throw new ClassCastException();
		return getIdForJavaElement((IJavaScriptElement)o1) - getIdForJavaElement((IJavaScriptElement)o2);
	}

	/**
	 * Compares two Java element types. A type is considered to be
	 * greater if it may contain the other.
	 *
	 * @return		an int < 0 if object1 is less than object2,
	 *				0 if they are equal, and > 0 if object1 is greater
	 *
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, int elementType) {
		if (!(o1 instanceof IJavaScriptElement))
			throw new ClassCastException();
		return getIdForJavaElement((IJavaScriptElement)o1) - getIdForJavaElementType(elementType);
	}

	int getIdForJavaElement(IJavaScriptElement element) {
		return getIdForJavaElementType(element.getElementType());
	}

	int getIdForJavaElementType(int elementType) {
		switch (elementType) {
			case IJavaScriptElement.JAVASCRIPT_MODEL:
				return 130;
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				return 120;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				return 110;
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				return 100;
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return 90;
			case IJavaScriptElement.CLASS_FILE:
				return 80;
			case IJavaScriptElement.TYPE:
				return 70;
			case IJavaScriptElement.FIELD:
				return 60;
			case IJavaScriptElement.METHOD:
				return 50;
			case IJavaScriptElement.INITIALIZER:
				return 40;
			case IJavaScriptElement.IMPORT_CONTAINER:
				return 20;
			case IJavaScriptElement.IMPORT_DECLARATION:
				return 10;
			default :
				return 1;
		}
	}
}
