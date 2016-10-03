/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;

/**
 * Handle representing a source field that is resolved. The uniqueKey contains
 * the genericSignature of the resolved field. Use BindingKey to decode it.
 */
public class ResolvedSourceField extends SourceField {

	private String uniqueKey;

	/*
	 * See class comments.
	 */
	public ResolvedSourceField(JavaElement parent, String name, String uniqueKey) {
		super(parent, name);
		this.uniqueKey = uniqueKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.internal.core.SourceField#getKey()
	 */
	public String getKey() {
		return this.uniqueKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.core.IField#isResolved()
	 */
	public boolean isResolved() {
		return true;
	}

	/**
	 * @private Debugging purposes
	 */
	protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
		super.toStringInfo(tab, buffer, info, showResolvedInfo);
		if (showResolvedInfo) {
			buffer.append(" {key="); //$NON-NLS-1$
			buffer.append(this.uniqueKey);
			buffer.append("}"); //$NON-NLS-1$
		}
	}

	public JavaElement unresolved() {
		SourceRefElement handle = new SourceField(this.parent, this.name);
		handle.occurrenceCount = this.occurrenceCount;
		return handle;
	}

	// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=278904
	public ISourceRange getJSdocRange() throws JavaScriptModelException {
		ISourceRange defaultRange = super.getJSdocRange();
		if (defaultRange != null)
			return defaultRange;

		ISourceRange range = this.getSourceRange();
		if (range == null)
			return null;
		IBuffer buf = null;
		if (this.isBinary()) {
			buf = this.getClassFile().getBuffer();
		}
		else {
			IJavaScriptUnit compilationUnit = this.getJavaScriptUnit();
			if (!compilationUnit.isConsistent()) {
				return null;
			}
			buf = compilationUnit.getBuffer();
		}
		final int start = range.getOffset();
		final int length = range.getLength();
		if (length > 0 && buf.getChar(start) != '/') {
			IScanner scanner = ToolFactory.createScanner(true, false, false, false);
			scanner.setSource(buf.getContents().toCharArray());
			try {
				int docOffset = -1;
				int docEnd = -1;
				
				int previousTerminal = -1;
				int previousOffset = -1;
				int previousEnd = -1;

				int terminal = scanner.getNextToken();
				loop : while (true) {
					if (scanner.getCurrentTokenEndPosition() < start) {
						previousTerminal = terminal;
						previousOffset = scanner.getCurrentTokenStartPosition();
						previousEnd = scanner.getCurrentTokenEndPosition() + 1;
						terminal = scanner.getNextToken();
						continue loop;
					}
					
					switch (previousTerminal) {
						case ITerminalSymbols.TokenNameCOMMENT_BLOCK :
						case ITerminalSymbols.TokenNameCOMMENT_JAVADOC :
							docOffset = previousOffset;
							docEnd = previousEnd;
							terminal = scanner.getNextToken();
							break loop;
						default :
							break loop;
					}
				}
				if (docOffset != -1) {
					return new SourceRange(docOffset, docEnd - docOffset + 1);
				}
			}
			catch (InvalidInputException ex) {
				// try if there is inherited Javadoc
			}
		}
		return null;
	}
}
