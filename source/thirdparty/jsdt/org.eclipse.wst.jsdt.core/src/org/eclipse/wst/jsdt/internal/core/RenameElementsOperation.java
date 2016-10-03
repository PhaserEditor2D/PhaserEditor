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
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * This operation renames elements.
 *
 * <p>Notes:<ul>
 * <li>Resource rename is not supported - this operation only renames
 *	   elements contained in compilation units.
 * <li>When a main type is renamed, its compilation unit and constructors are renamed.
 * <li>Constructors cannot be renamed.
 * </ul>
 */
public class RenameElementsOperation extends MoveElementsOperation {
/**
 * When executed, this operation will rename the specified elements with the given names in the
 * corresponding destinations.
 */
public RenameElementsOperation(IJavaScriptElement[] elements, IJavaScriptElement[] destinations, String[] newNames, boolean force) {
	//a rename is a move to the same parent with a new name specified
	//these elements are from different parents
	super(elements, destinations, force);
	setRenamings(newNames);
}
/**
 * @see MultiOperation
 */
protected String getMainTaskName() {
	return Messages.operation_renameElementProgress;
}
/**
 * @see CopyElementsOperation#isRename()
 */
protected boolean isRename() {
	return true;
}
/**
 * @see MultiOperation
 */
protected IJavaScriptModelStatus verify() {
	IJavaScriptModelStatus status = super.verify();
	if (! status.isOK())
		return status;
	if (this.renamingsList == null || this.renamingsList.length == 0)
		return new JavaModelStatus(IJavaScriptModelStatusConstants.NULL_NAME);
	return JavaModelStatus.VERIFIED_OK;
}
/**
 * @see MultiOperation
 */
protected void verify(IJavaScriptElement element) throws JavaScriptModelException {
	if (element == null || !element.exists())
		error(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);

	if (element.isReadOnly())
		error(IJavaScriptModelStatusConstants.READ_ONLY, element);

	if (!(element instanceof ISourceReference))
		error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element);

	int elementType = element.getElementType();
	if (elementType < IJavaScriptElement.TYPE || elementType == IJavaScriptElement.INITIALIZER)
		error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element);

	verifyRenaming(element);
}
}
