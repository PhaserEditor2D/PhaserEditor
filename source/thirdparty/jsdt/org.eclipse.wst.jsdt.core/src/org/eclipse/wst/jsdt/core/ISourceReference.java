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
 * Common protocol for JavaScript elements that have associated source code.
 * This set consists of <code>IClassFile</code>, <code>IJavaScriptUnit</code>,
 * <code>IPackageDeclaration</code>, <code>IImportDeclaration</code>,
 * <code>IImportContainer</code>, <code>IType</code>, <code>IField</code>,
 * <code>IFunction</code>, and <code>IInitializer</code>.
 * <p>
 * Source reference elements may be working copies if they were created from
 * a compilation unit that is a working copy.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see IPackageFragmentRoot#attachSource(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface ISourceReference {
/**
 * Returns whether this element exists in the model.
 *
 * @return <code>true</code> if this element exists in the JavaScript model
 */
boolean exists();

/**
 * Returns the source code associated with this element.
 * This extracts the substring from the source buffer containing this source
 * element. This corresponds to the source range that would be returned by
 * <code>getSourceRange</code>.
 *
 * @return the source code, or <code>null</code> if this element has no
 *   associated source code
 * @exception JavaScriptModelException if an exception occurs while accessing its corresponding resource
 */
String getSource() throws JavaScriptModelException;
/**
 * Returns the source range associated with this element.
 *
 * @return the source range, or <code>null</code> if this element has no
 *   associated source code
 * @exception JavaScriptModelException if an exception occurs while accessing its corresponding resource
 */
ISourceRange getSourceRange() throws JavaScriptModelException;
}
