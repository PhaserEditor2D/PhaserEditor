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
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * This operation moves resources (package fragments and compilation units) from their current
 * container to a specified destination container, optionally renaming the
 * elements.
 * A move resource operation is equivalent to a copy resource operation, where
 * the source resources are deleted after the copy.
 * <p>This operation can be used for reorganizing resources within the same container.
 *
 * @see CopyResourceElementsOperation
 */
public class MoveResourceElementsOperation extends CopyResourceElementsOperation {
/**
 * When executed, this operation will move the given elements to the given containers.
 */
public MoveResourceElementsOperation(IJavaScriptElement[] elementsToMove, IJavaScriptElement[] destContainers, boolean force) {
	super(elementsToMove, destContainers, force);
}
/**
 * @see MultiOperation
 */
protected String getMainTaskName() {
	return Messages.operation_moveResourceProgress;
}
/**
 * @see CopyResourceElementsOperation#isMove()
 */
protected boolean isMove() {
	return true;
}
}
