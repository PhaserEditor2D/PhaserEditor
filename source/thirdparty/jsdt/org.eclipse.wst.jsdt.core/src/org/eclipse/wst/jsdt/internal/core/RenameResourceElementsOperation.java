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
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * This operation renames resources (Package fragments and compilation units).
 *
 * <p>Notes:<ul>
 * <li>When a compilation unit is renamed, its main type and the constructors of the
 * 		main type are renamed.
 * </ul>
 */
public class RenameResourceElementsOperation extends MoveResourceElementsOperation {
/**
 * When executed, this operation will rename the specified elements with the given names in the
 * corresponding destinations.
 */
public RenameResourceElementsOperation(IJavaScriptElement[] elements, IJavaScriptElement[] destinations, String[] newNames, boolean force) {
	//a rename is a move to the same parent with a new name specified
	//these elements are from different parents
	super(elements, destinations, force);
	setRenamings(newNames);
}
/**
 * @see MultiOperation
 */
protected String getMainTaskName() {
	return Messages.operation_renameResourceProgress;
}
/**
 * @see CopyResourceElementsOperation#isRename()
 */
protected boolean isRename() {
	return true;
}
/**
 * @see MultiOperation
 */
protected void verify(IJavaScriptElement element) throws JavaScriptModelException {
	super.verify(element);

	int elementType = element.getElementType();

	if (!(elementType == IJavaScriptElement.JAVASCRIPT_UNIT || elementType == IJavaScriptElement.PACKAGE_FRAGMENT)) {
		error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element);
	}
	if (elementType == IJavaScriptElement.JAVASCRIPT_UNIT) {
		CompilationUnit cu = (CompilationUnit)element;
		if (cu.isWorkingCopy() && !cu.isPrimary()) {
			error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element);
		}
	}
	verifyRenaming(element);
}
}
