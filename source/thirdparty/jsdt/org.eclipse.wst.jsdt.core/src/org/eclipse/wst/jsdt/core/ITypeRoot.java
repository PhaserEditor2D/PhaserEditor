/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Represents an entire JavaScript type root (either an <code>IJavaScriptUnit</code>
 * or an <code>IClassFile</code>).
 *
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see IJavaScriptUnit Note that methods {@link #findPrimaryType()} and {@link #getElementAt(int)}
 * 	were already implemented in this interface respectively since version 3.0 and version 1.0.
 * @see IClassFile Note that method {@link #getWorkingCopy(WorkingCopyOwner, IProgressMonitor)}
 * 	was already implemented in this interface since version 3.0.
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface ITypeRoot extends IJavaScriptElement, IParent, IOpenable, ISourceReference, ICodeAssist, IFunctionContainer {

/**
 * Finds the primary type of this JavaScript type root (that is, the type with the same name as the
 * javascript file), or <code>null</code> if no such a type exists.
 *
 * @return the found primary type of this JavaScript type root, or <code>null</code> if no such a type exists
 */
IType findPrimaryType();

/**
 * Returns the smallest element within this JavaScript type root that
 * includes the given source position (that is, a method, field, etc.), or
 * <code>null</code> if there is no element other than the JavaScript type root
 * itself at the given position, or if the given position is not
 * within the source range of the source of this JavaScript type root.
 *
 * @param position a source position inside the JavaScript type root
 * @return the innermost JavaScript element enclosing a given source position or <code>null</code>
 *	if none (excluding the JavaScript type root).
 * @throws JavaScriptModelException if the JavaScript type root does not exist or if an
 *	exception occurs while accessing its corresponding resource
 */
IJavaScriptElement getElementAt(int position) throws JavaScriptModelException;

/**
 * Returns a shared working copy on this javaScirpt file using the given working copy owner to create
 * the buffer. If this is already a working copy of the given owner, the element itself is returned.
 * This API can only answer an already existing working copy if it is based on the same
 * original JavaScript type root AND was using the same working copy owner (that is, as defined by {@link Object#equals}).
 * <p>
 * The life time of a shared working copy is as follows:
 * <ul>
 * <li>The first call to {@link #getWorkingCopy(WorkingCopyOwner, IProgressMonitor)}
 * 	creates a new working copy for this element</li>
 * <li>Subsequent calls increment an internal counter.</li>
 * <li>A call to {@link IJavaScriptUnit#discardWorkingCopy()} decrements the internal counter.</li>
 * <li>When this counter is 0, the working copy is discarded.
 * </ul>
 * So users of this method must discard exactly once the working copy.
 * <p>
 * Note that the working copy owner will be used for the life time of the shared working copy, that is if the
 * working copy is closed then reopened, this owner will be used.
 * The buffer will be automatically initialized with the original's JavaScript type root content upon creation.
 * <p>
 * When the shared working copy instance is created, an ADDED IJavaScriptElementDelta is reported on this
 * working copy.
 * </p><p>
 * A working copy can be created on a not-yet existing compilation unit.
 * In particular, such a working copy can then be committed in order to create
 * the corresponding compilation unit.
 * </p><p>
 * Note that possible problems of this working copy are reported using this method. only
 * if the given working copy owner returns a problem requestor for this working copy
 * (see {@link WorkingCopyOwner#getProblemRequestor(IJavaScriptUnit)}).
 * </p>
 *
 * @param owner the working copy owner that creates a buffer that is used to get the content
 * 				of the working copy
 * @param monitor a progress monitor used to report progress while opening this compilation unit
 *                 or <code>null</code> if no progress should be reported
 * @throws JavaScriptModelException if the contents of this element can
 *   	not be determined.
 * @return a new working copy of this JavaScript type root using the given owner to create
 *		the buffer, or this JavaScript type root if it is already a working copy
 */
IJavaScriptUnit getWorkingCopy(WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException;


}
