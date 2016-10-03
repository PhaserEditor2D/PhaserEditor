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
package org.eclipse.wst.jsdt.internal.compiler.lookup;

public class ProblemReferenceBinding extends ReferenceBinding {
	private ReferenceBinding closestMatch;
	private int problemReason;

// NOTE: must only answer the subset of the name related to the problem

public ProblemReferenceBinding(char[][] compoundName, ReferenceBinding closestMatch, int problemReason) {
	this.compoundName = compoundName;
	this.closestMatch = closestMatch;
	this.problemReason = problemReason;
}
public ProblemReferenceBinding(char[] name, ReferenceBinding closestMatch, int problemReason) {
	this(new char[][] {name}, closestMatch, problemReason);
}

/**
 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#closestMatch()
 */
public ReferenceBinding closestMatch() {
	return this.closestMatch;
}

/* API
* Answer the problem id associated with the receiver.
* NoError if the receiver is a valid binding.
*/
public int problemId() {
	return this.problemReason;
}

/**
 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#shortReadableName()
 */
public char[] shortReadableName() {
	return readableName();
}

}
