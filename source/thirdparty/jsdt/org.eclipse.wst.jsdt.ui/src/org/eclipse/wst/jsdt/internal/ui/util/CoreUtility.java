/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.osgi.framework.Bundle;


public class CoreUtility {
	
	public static void createDerivedFolder(IFolder folder, boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
		if (!folder.exists()) {
			IContainer parent= folder.getParent();
			if (parent instanceof IFolder) {
				createDerivedFolder((IFolder)parent, force, local, null);
			}
			folder.create(force ? (IResource.FORCE | IResource.DERIVED) : IResource.DERIVED, local, monitor);
		}
	}

	/**
	 * Creates a folder and all parent folders if not existing.
	 * Project must exist.
	 * <code> org.eclipse.ui.dialogs.ContainerGenerator</code> is too heavy
	 * (creates a runnable)
	 */
	public static void createFolder(IFolder folder, boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
		if (!folder.exists()) {
			IContainer parent= folder.getParent();
			if (parent instanceof IFolder) {
				createFolder((IFolder)parent, force, local, null);
			}
			folder.create(force, local, monitor);
		}
	}
	
	/**
	 * Creates an extension.  If the extension plugin has not
	 * been loaded a busy cursor will be activated during the duration of
	 * the load.
	 *
	 * @param element the config element defining the extension
	 * @param classAttribute the name of the attribute carrying the class
	 * @return the extension object
	 */
	public static Object createExtension(final IConfigurationElement element, final String classAttribute) throws CoreException {
		// If plugin has been loaded create extension.
		// Otherwise, show busy cursor then create extension.
		String pluginId = element.getContributor().getName();
		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle != null && bundle.getState() == Bundle.ACTIVE ) {
			return element.createExecutableExtension(classAttribute);
		} else {
			final Object[] ret = new Object[1];
			final CoreException[] exc = new CoreException[1];
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					try {
						ret[0] = element.createExecutableExtension(classAttribute);
					} catch (CoreException e) {
						exc[0] = e;
					}
				}
			});
			if (exc[0] != null)
				throw exc[0];
			else
				return ret[0];
		}
	}	

	
	/**
	 * Starts a build in the background.
	 * @param project The project to build or <code>null</code> to build the workspace.
	 */
	public static void startBuildInBackground(final IProject project) {
		getBuildJob(project).schedule();
	}

	
	private static final class BuildJob extends Job {
		private final IProject fProject;
		private BuildJob(String name, IProject project) {
			super(name);
			fProject= project;
		}
		
		public boolean isCoveredBy(BuildJob other) {
			if (other.fProject == null) {
				return true;
			}
			return fProject != null && fProject.equals(other.fProject);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			synchronized (getClass()) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
		        Job[] buildJobs = Job.getJobManager().find(ResourcesPlugin.FAMILY_MANUAL_BUILD);
		        for (int i= 0; i < buildJobs.length; i++) {
		        	Job curr= buildJobs[i];
		        	if (curr != this && curr instanceof BuildJob) {
		        		BuildJob job= (BuildJob) curr;
		        		if (job.isCoveredBy(this)) {
		        			curr.cancel(); // cancel all other build jobs of our kind
		        		}
		        	}
				}
			}
			try {
				if (fProject != null) {
					monitor.beginTask(Messages.format(JavaUIMessages.CoreUtility_buildproject_taskname, fProject.getName()), 2); 
					fProject.build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor,1));
					//This call should not be a problem, except in case of having multiple projects with 
					//incomplete or dirty build states
					JavaScriptPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(monitor,1));
				} else {
					IJavaScriptProject[] projects = JavaScriptCore.create(JavaScriptPlugin.getWorkspace().getRoot()).getJavaScriptProjects();
					if(projects.length > 0) { 
						monitor.beginTask(JavaUIMessages.CoreUtility_buildall_taskname, projects.length);
						for (int i = 0; i < projects.length; i++) {
							IProject project = projects[i].getProject();
							if(project.isAccessible())
								projects[i].getProject().build(IncrementalProjectBuilder.FULL_BUILD, JavaScriptCore.BUILDER_ID, null, new SubProgressMonitor(monitor, 1));
						}
					}
				}
			} catch (CoreException e) {
				return e.getStatus();
			} catch (OperationCanceledException e) {
				return Status.CANCEL_STATUS;
			}
			finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}
		public boolean belongsTo(Object family) {
			return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
		}
	}
	
	/**
	 * Returns a build job
	 * @param project The project to build or <code>null</code> to build the workspace.
	 */
	public static Job getBuildJob(final IProject project) {
		Job buildJob= new BuildJob(JavaUIMessages.CoreUtility_job_title, project);
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true);
		return buildJob;
	}
	
	/**
     * Set the autobuild to the value of the parameter and
     * return the old one.
     * 
     * @param state the value to be set for autobuilding.
     * @return the old value of the autobuild state
     */
    public static boolean enableAutoBuild(boolean state) throws CoreException {
        IWorkspace workspace= ResourcesPlugin.getWorkspace();
        IWorkspaceDescription desc= workspace.getDescription();
        boolean isAutoBuilding= desc.isAutoBuilding();
        if (isAutoBuilding != state) {
            desc.setAutoBuilding(state);
            workspace.setDescription(desc);
        }
        return isAutoBuilding;
    }
	
}
