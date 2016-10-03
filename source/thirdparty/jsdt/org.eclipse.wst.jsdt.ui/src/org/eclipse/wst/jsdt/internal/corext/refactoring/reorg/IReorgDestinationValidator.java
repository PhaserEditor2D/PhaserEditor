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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;

public interface IReorgDestinationValidator {

	public boolean canChildrenBeDestinations(IResource resource);
	public boolean canChildrenBeDestinations(IJavaScriptElement javaElement);
	public boolean canElementBeDestination(IResource resource);
	public boolean canElementBeDestination(IJavaScriptElement javaElement);
}
