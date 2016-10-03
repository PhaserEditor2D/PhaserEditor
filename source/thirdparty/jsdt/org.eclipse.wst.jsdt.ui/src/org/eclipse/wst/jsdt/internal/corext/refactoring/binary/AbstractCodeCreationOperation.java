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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Partial implementation of a code creation operation.
 * 
 * 
 */
public abstract class AbstractCodeCreationOperation implements IWorkspaceRunnable {

	/** The URI where to output the stubs */
	protected final URI fOutputURI;

	/** The list of packages to create stubs for */
	protected final List fPackages;

	/**
	 * Creates a new abstract code creation operation.
	 * 
	 * @param uri
	 *            the URI where to output the code
	 * @param packages
	 *            the list of packages to create code for
	 */
	protected AbstractCodeCreationOperation(final URI uri, final List packages) {
		Assert.isNotNull(uri);
		Assert.isNotNull(packages);
		fOutputURI= uri;
		fPackages= packages;
	}

	/**
	 * Creates a new compilation unit with the given contents.
	 * 
	 * @param store
	 *            the file store
	 * @param name
	 *            the name of the compilation unit
	 * @param content
	 *            the content of the compilation unit
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while creating the compilation unit
	 */
	protected void createCompilationUnit(final IFileStore store, final String name, final String content, final IProgressMonitor monitor) throws CoreException {
		OutputStream stream= null;
		try {
			stream= new BufferedOutputStream(store.getChild(name).openOutputStream(EFS.NONE, new SubProgressMonitor(monitor, 1)));
			try {
				stream.write(content.getBytes());
			} catch (IOException exception) {
				throw new CoreException(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), 0, exception.getLocalizedMessage(), exception));
			}
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException exception) {
					// Do nothing
				}
			}
		}
	}

	/**
	 * Creates a package fragment with the given name.
	 * 
	 * @param store
	 *            the file store
	 * @param name
	 *            the name of the package
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while creating the package fragment
	 */
	protected void createPackageFragment(final IFileStore store, final String name, final IProgressMonitor monitor) throws CoreException {
		store.mkdir(EFS.NONE, monitor);
	}

	/**
	 * Returns the operation label.
	 * 
	 * @return the operation label
	 */
	protected abstract String getOperationLabel();

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
	protected abstract void run(IClassFile file, IFileStore parent, IProgressMonitor monitor) throws CoreException;

	/**
	 * {@inheritDoc}
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		monitor.beginTask(getOperationLabel(), 100 * fPackages.size());
		try {
			final StringBuffer buffer= new StringBuffer(128);
			for (final Iterator iterator= fPackages.iterator(); iterator.hasNext();) {
				final IPackageFragment fragment= (IPackageFragment) iterator.next();
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 100);
				final IClassFile[] files= fragment.getClassFiles();
				final int size= files.length;
				subMonitor.beginTask(getOperationLabel(), size * 50);
				final String name= fragment.getElementName();
				IFileStore store= EFS.getStore(fOutputURI);
				if (!"".equals(name)) { //$NON-NLS-1$
					final String pack= name;
					buffer.setLength(0);
					buffer.append(name);
					final int length= buffer.length();
					for (int index= 0; index < length; index++) {
						if (buffer.charAt(index) == '.')
							buffer.setCharAt(index, '/');
					}
					store= store.getChild(new Path(buffer.toString()));
					if (!pack.startsWith(".")) //$NON-NLS-1$
						createPackageFragment(store, pack, new SubProgressMonitor(subMonitor, 10));
				} else
					createPackageFragment(store, "", new SubProgressMonitor(subMonitor, 10)); //$NON-NLS-1$
				final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 30);
				try {
					subsubMonitor.beginTask(getOperationLabel(), size * 100);
					for (int index= 0; index < size; index++) {
						if (subMonitor.isCanceled())
							throw new OperationCanceledException();
						run(files[index], store, new SubProgressMonitor(subsubMonitor, 100));
					}
				} finally {
					subsubMonitor.done();
				}
			}
		} finally {
			monitor.done();
		}
	}
}
