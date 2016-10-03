/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls;

public class KeyValuePair {

	public String fKey;
	public String fValue;

	public KeyValuePair(String key, String value) {
		fKey= key;
		fValue= value;
	}

	public String getKey() {
		return fKey;
	}

	public void setKey(String key) {
		fKey= key;
	}

	public String getValue() {
		return fValue;
	}

	public void setValue(String value) {
		fValue= value;
	}
}
