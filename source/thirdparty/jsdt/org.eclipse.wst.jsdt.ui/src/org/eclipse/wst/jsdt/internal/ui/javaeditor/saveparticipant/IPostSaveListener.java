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
package org.eclipse.wst.jsdt.internal.ui.javaeditor.saveparticipant;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;

/**
 * This <code>IPostSaveListener</code> is informed when 
 * a compilation unit is saved through the {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider}.
 * <p>
 * In oder to get notified the listener must be registered with the {@link SaveParticipantRegistry}
 * and be enabled on the save participant preference page.</p>
 * <p>
 * The notification order of post save listeners is unspecified.</p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see SaveParticipantDescriptor
 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider
 * 
 */
public interface IPostSaveListener {
	
	/**
	 * A human readable name of this listener.
	 * 
	 * @return the name
	 */
	String getName();
	
	/**
	 * The unique id of this listener.
	 * 
	 * @return a non-empty id
	 */
	String getId();
	
	/**
	 * Informs this post save listener that the given <code>compilationUnit</code> 
	 * has been saved by the {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider}. The listener
	 * is allowed to modify the given compilation unit and to open a dialog.
	 * <p>
	 * <em>Every implementor of this method must strictly obey these rules:</em>
	 * <ul>
	 *   <li>not touch any file other than the given <code>compilationUnit</code>
	 *   		which is already locked by a scheduling rule </li> 
	 *   <li>changing the scheduling rule or posting a new job is not allowed</li> 
	 *   <li>it is not allowed to save the given <code>compilationUnit</code></li> 
	 *   <li>it must be able to deal with unsaved resources and with compilation units which are not on the Java build path</li>
	 *   <li>must not assume to be called in the UI thread</li>
	 *   <li>should be as fast as possible since this code is executed every time the <code>compilationUnit</code> is saved</li>
	 * </ul>
	 * The compilation unit document provider can disable a listener that violates any of the above rules.</p> 
	 *
	 * @param compilationUnit the compilation unit which was saved
	 * @param monitor the progress monitor for reporting progress
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider
	 */
	void saved(IJavaScriptUnit compilationUnit, IProgressMonitor monitor) throws CoreException;

}
