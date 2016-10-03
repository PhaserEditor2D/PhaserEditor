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
package org.eclipse.wst.jsdt.core;

/**
 * Common protocol for JavaScript elements that support source code assist and code
 * resolve.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface ICodeAssist {

	/**
	 * Performs code completion at the given offset position in this javaScript unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * javaScript unit.
	 * <p>
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 * @exception JavaScriptModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this javaScript unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
 	 */
	void codeComplete(int offset, CompletionRequestor requestor)
		throws JavaScriptModelException;

	/**
	 * Performs code completion at the given offset position in this javaScript unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * javaScript unit.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original javaScript units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original javaScript
	 * unit had been deleted.
	 * </p>
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 * @param owner the owner of working copies that take precedence over their original javaScript units
	 * @exception JavaScriptModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this javaScript unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 */
	void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner)
		throws JavaScriptModelException;

	/**
	 * Returns the JavaScript elements corresponding to the given selected text in this javaScript unit.
	 * The <code>offset</code> is the 0-based index of the first selected character.
	 * The <code>length</code> is the number of selected characters.
	 * <p>
	 * Note that if the <code>length</code> is 0 and the <code>offset</code> is inside an identifier
	 * or the index just after an identifier then this identifier is considered as the selection.
	 * </p>
	 *
	 * @param offset the given offset position
	 * @param length the number of selected characters
	 * @return the JavaScript elements corresponding to the given selected text
	 *
	 * @exception JavaScriptModelException if code resolve could not be performed. Reasons include:
	 * <ul>
	 *  <li>This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The range specified is not within this element's
	 *      source range (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 */
	IJavaScriptElement[] codeSelect(int offset, int length) throws JavaScriptModelException;
	/**
	 * Returns the JavaScript elements corresponding to the given selected text in this javaScript unit.
	 * The <code>offset</code> is the 0-based index of the first selected character.
	 * The <code>length</code> is the number of selected characters.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original javaScript units
	 * in the workspace.
	 * <p>
	 * Note that if the <code>length</code> is 0 and the <code>offset</code> is inside an identifier
	 * or the index just after an identifier then this identifier is considered as the selection.
	 * </p>
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original javaScript
	 * unit had been deleted.
	 * </p>
	 *
	 * @param offset the given offset position
	 * @param length the number of selected characters
	 * @param owner the owner of working copies that take precedence over their original javaScript units
	 * @return the JavaScript elements corresponding to the given selected text
	 *
	 * @exception JavaScriptModelException if code resolve could not be performed. Reasons include:
	 * <ul>
	 *  <li>This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The range specified is not within this element's
	 *      source range (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 */
	IJavaScriptElement[] codeSelect(int offset, int length, WorkingCopyOwner owner) throws JavaScriptModelException;
}
