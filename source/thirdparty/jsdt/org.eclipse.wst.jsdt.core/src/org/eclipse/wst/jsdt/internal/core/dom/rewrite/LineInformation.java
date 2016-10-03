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

package org.eclipse.wst.jsdt.internal.core.dom.rewrite;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

/**
 *
 */
public abstract class LineInformation {

	public static LineInformation create(final IDocument doc) {
		return new LineInformation() {
			public int getLineOfOffset(int offset) {
				try {
					return doc.getLineOfOffset(offset);
				} catch (BadLocationException e) {
					return -1;
				}
			}

			public int getLineOffset(int line) {
				try {
					return doc.getLineOffset(line);
				} catch (BadLocationException e) {
					return -1;
				}
			}
		};
	}

	public static LineInformation create(final JavaScriptUnit astRoot) {
		return new LineInformation() {
			public int getLineOfOffset(int offset) {
				return astRoot.getLineNumber(offset) - 1;
			}
			public int getLineOffset(int line) {
				return astRoot.getPosition(line + 1, 0);
			}
		};
	}



	public abstract int getLineOfOffset(int offset);
	public abstract int getLineOffset(int line);

}
