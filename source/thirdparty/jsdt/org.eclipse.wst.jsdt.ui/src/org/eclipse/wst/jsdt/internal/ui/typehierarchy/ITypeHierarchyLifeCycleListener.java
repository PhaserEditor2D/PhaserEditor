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
package org.eclipse.wst.jsdt.internal.ui.typehierarchy;

import org.eclipse.wst.jsdt.core.IType;


/**
 * Used by the TypeHierarchyLifeCycle to inform listeners about a change in the
 * type hierarchy
 */
public interface ITypeHierarchyLifeCycleListener {
	
	/**
	 * A Java element changed. 
	 */
	void typeHierarchyChanged(TypeHierarchyLifeCycle typeHierarchyProvider, IType[] changedTypes);

}
