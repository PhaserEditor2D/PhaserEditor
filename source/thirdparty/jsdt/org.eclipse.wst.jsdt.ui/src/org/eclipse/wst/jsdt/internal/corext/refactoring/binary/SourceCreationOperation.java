/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.binary;

import java.net.URI;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Operation, which run, creates source code for a list of binary package
 * fragments with attached source.
 * 
 * 
 */
public class SourceCreationOperation extends AbstractCodeCreationOperation {

	/**
	 * Creates a new source creation operation.
	 * 
	 * @param uri
	 *            the URI where to output the source
	 * @param packages
	 *            the list of packages to create source for
	 */
	public SourceCreationOperation(final URI uri, final List packages) {
		super(uri, packages);
	}

	/**
	 * Returns the operation label.
	 * 
	 * @return the operation label
	 */
	protected String getOperationLabel() {
		return RefactoringCoreMessages.SourceCreationOperation_creating_source_folder;
	}

	/**
	 * Runs the stub generation on the specified class file.
	 * 
	 * @param file
	 *            the class file
	 * @param parent
	 *            the parent store
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected void run(final IClassFile file, final IFileStore parent, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(getOperationLabel(), 2);
			final IType type= file.getType();
			if (type.isAnonymous() || type.isLocal() || type.isMember())
				return;
			final String source= file.getSource();
			createCompilationUnit(parent, type.getElementName() + ".java", source != null ? source : "", monitor); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			monitor.done();
		}
	}
}
