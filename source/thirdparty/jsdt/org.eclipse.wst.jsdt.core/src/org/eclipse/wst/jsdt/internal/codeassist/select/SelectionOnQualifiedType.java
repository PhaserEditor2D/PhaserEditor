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
package org.eclipse.wst.jsdt.internal.codeassist.select;

public class SelectionOnQualifiedType extends SelectionOnQualifiedNameReference {

	public SelectionOnQualifiedType(char[][] previousIdentifiers, char[] selectionIdentifier, long[] positions) {
		super(previousIdentifiers, selectionIdentifier, positions);
	}
	public StringBuffer printExpression(int indent, StringBuffer output) {

		output.append("<SelectOnType:"); //$NON-NLS-1$
		for (int i = 0, length = tokens.length; i < length; i++) {
			if (i > 0) output.append('.');
			output.append(tokens[i]);
		}
		return output.append('>');
	}
}
