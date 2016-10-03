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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.CoreUtility;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/**
 * Logged implementation of new create target queries.
 * 
 * 
 */
public final class LoggedCreateTargetQueries implements ICreateTargetQueries {

	/** Default implementation of create target query */
	private final class CreateTargetQuery implements ICreateTargetQuery {

		private void createJavaProject(IProject project) throws CoreException {
			if (!project.exists()) {
				BuildPathsBlock.createProject(project, null, new NullProgressMonitor());
				BuildPathsBlock.addJavaNature(project, new NullProgressMonitor());
			}
		}

		private void createPackageFragmentRoot(IPackageFragmentRoot root) throws CoreException {
			final IJavaScriptProject project= root.getJavaScriptProject();
			if (!project.exists())
				createJavaProject(project.getProject());
			final IFolder folder= project.getProject().getFolder(root.getElementName());
			if (!folder.exists())
				CoreUtility.createFolder(folder, true, true, new NullProgressMonitor());
			final List list= Arrays.asList(project.getRawIncludepath());
			list.add(JavaScriptCore.newSourceEntry(folder.getFullPath()));
			project.setRawIncludepath((IIncludePathEntry[]) list.toArray(new IIncludePathEntry[list.size()]), new NullProgressMonitor());
		}

		/**
		 * {@inheritDoc}
		 */
		public Object getCreatedTarget(final Object selection) {
			final Object target= fLog.getCreatedElement(selection);
			if (target instanceof IPackageFragment) {
				final IPackageFragment fragment= (IPackageFragment) target;
				final IJavaScriptElement parent= fragment.getParent();
				if (parent instanceof IPackageFragmentRoot) {
					try {
						final IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
						if (!root.exists())
							createPackageFragmentRoot(root);
						if (!fragment.exists())
							root.createPackageFragment(fragment.getElementName(), true, new NullProgressMonitor());
					} catch (CoreException exception) {
						JavaScriptPlugin.log(exception);
						return null;
					}
				}
			} else if (target instanceof IFolder) {
				try {
					final IFolder folder= (IFolder) target;
					final IProject project= folder.getProject();
					if (!project.exists())
						createJavaProject(project);
					if (!folder.exists())
						CoreUtility.createFolder(folder, true, true, new NullProgressMonitor());
				} catch (CoreException exception) {
					JavaScriptPlugin.log(exception);
					return null;
				}
			}
			return target;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getNewButtonLabel() {
			return "unused"; //$NON-NLS-1$
		}
	}

	/** The create target execution log */
	private final CreateTargetExecutionLog fLog;

	/**
	 * Creates a new logged create target queries.
	 * 
	 * @param log
	 *            the create target execution log
	 */
	public LoggedCreateTargetQueries(final CreateTargetExecutionLog log) {
		Assert.isNotNull(log);
		fLog= log;
	}

	/**
	 * {@inheritDoc}
	 */
	public ICreateTargetQuery createNewPackageQuery() {
		return new CreateTargetQuery();
	}

	/**
	 * Returns the create target execution log.
	 * 
	 * @return the create target execution log
	 */
	public CreateTargetExecutionLog getCreateTargetExecutionLog() {
		return fLog;
	}
}
