/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.util.ConvertUtility;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.Logger;

/**
 * Not API
 */
public class ConvertAction implements IObjectActionDelegate, IActionDelegate {
	IWorkbenchPart fPart;
	Object[] fTarget;
	private static final String FACET_NATURE = "org.eclipse.wst.common.project.facet.core.nature"; //$NON-NLS-1$
	private static final String FACET_PROPERTY_PAGE = "org.eclipse.wst.common.project.facet.ui.FacetsPropertyPage"; //$NON-NLS-1$

	private void doInstall(IProject project) {
		boolean configured = false;

		ConvertUtility convertor = new ConvertUtility(project);
		try {
			boolean hadBasicNature = ConvertUtility.hasNature(project);

			convertor.configure(new NullProgressMonitor());
			convertor.addBrowserSupport(!hadBasicNature, new NullProgressMonitor());

			if (!hadBasicNature) {
				/*
				 * No nature before, so no existing include path. Define the
				 * project itself as an source folder.
				 */
				JavaProject jp = (JavaProject) JavaScriptCore.create(project);
				IIncludePathEntry[] oldEntries = null;
				try {
					oldEntries = jp.getRawIncludepath();
					List entries = new ArrayList();
					for (int i = 0; i < oldEntries.length; i++) {
						if (oldEntries[i].getContentKind() != IPackageFragmentRoot.K_SOURCE || oldEntries[i].getEntryKind() != IIncludePathEntry.CPE_SOURCE) {
							entries.add(oldEntries[i]);
						}
					}
					oldEntries = (IIncludePathEntry[]) entries.toArray(new IIncludePathEntry[entries.size()]);
				}
				catch (JavaScriptModelException ex1) {
					Logger.log(Logger.ERROR_DEBUG, null, ex1);
					oldEntries = new IIncludePathEntry[0];
				}
				IIncludePathEntry[] sourcePaths = convertor.getDefaultSourcePaths(project);
				IIncludePathEntry[] newEntries = new IIncludePathEntry[oldEntries.length + sourcePaths.length];
				System.arraycopy(sourcePaths, 0, newEntries, 0, sourcePaths.length);
				System.arraycopy(oldEntries, 0, newEntries, sourcePaths.length, oldEntries.length);
				
				
				try {
					jp.setRawIncludepath(newEntries, project.getFullPath(), new NullProgressMonitor());
				}
				catch (JavaScriptModelException ex1) {
					Logger.log(Logger.ERROR_DEBUG, null, ex1);
				}
			}
			configured = true;
		}
		catch (CoreException ex) {
			Logger.logException(ex);
		}
	}

	private void doUninstall(IProject project, IProgressMonitor monitor) {
//		ConvertUtility nature = new ConvertUtility(project);
//		try {
//			nature.deconfigure();
//		} catch (CoreException ex) {
//			Logger.logException(ex);
//		}
	}

	void enableForFacets(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			
			boolean hasNature = project.hasNature(FACET_NATURE);
			if (!hasNature) {
				List natures = new ArrayList(Arrays.asList(description.getNatureIds()));
				natures.add(FACET_NATURE);
				description.setNatureIds((String[]) natures.toArray(new String[natures.size()]));
			}
			
			if (!hasNature)
				project.setDescription(description, new NullProgressMonitor());
		}
		catch (CoreException e) {
			Logger.logException(e);
		}
	}

	private void install(final IProject project) {
		doInstall(project);
	}

	public void run(IAction action) {
		if (fTarget == null)
			return;

		new Job(Messages.converter_ConfiguringForJavaScript) {
			protected IStatus run(IProgressMonitor arg0) {
				for (int i = 0; i < fTarget.length; i++) {
					if (fTarget[i] instanceof IResource) {
						final IProject project = ((IResource) fTarget[i]).getProject();

						/* Temporary until https://bugs.eclipse.org/bugs/show_bug.cgi?id=298483 is resolved */
						// enableForFacets(project);

						if (!ConvertUtility.hasNature(project)) {
							/* Doesn't have nature, do a full install. */
							install(project);
						}
						else {
							/*
							 * Has nature, check for browser library on
							 * include path and setup if not found.
							 */
							IJavaScriptProject jp = JavaScriptCore.create(project);
							IIncludePathEntry[] rawClasspath = null;
							try {
								rawClasspath = jp.getRawIncludepath();
							}
							catch (JavaScriptModelException ex1) {
								Logger.log(Logger.ERROR_DEBUG, null, ex1);
							}

							boolean browserFound = false;
							for (int k = 0; rawClasspath != null && !browserFound && k < rawClasspath.length; k++) {
								if (rawClasspath[k].getPath().equals(ConvertUtility.BROWSER_LIBRARY_PATH)) {
									browserFound = true;
								}
							}
							if (!browserFound) {
								install(project);
							}
						}
					}
				}
				return Status.OK_STATUS;
			}
		}.schedule();

	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			fTarget = ((IStructuredSelection) selection).toArray();
		}
		else {
			fTarget = null;
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fPart = targetPart;
	}

	private void showPropertiesOn(final IProject project, final IProgressMonitor monitor) {
		IExtension[] extensions = Platform.getExtensionRegistry().getExtensionPoint("org.eclipse.ui.propertyPages").getExtensions(); //$NON-NLS-1$
		final List pageIds = new ArrayList(8);
		pageIds.add(FACET_PROPERTY_PAGE);
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].getNamespaceIdentifier().startsWith("org.eclipse.wst.jsdt.")) { //$NON-NLS-1$
				IConfigurationElement[] configurationElements = extensions[i].getConfigurationElements();
				for (int j = 0; j < configurationElements.length; j++) {
					if ("page".equals(configurationElements[j].getName())) { //$NON-NLS-1$
						pageIds.add(configurationElements[j].getAttribute("id")); //$NON-NLS-1$
					}
				}
			}
		}
		Shell shell = (Shell) fPart.getAdapter(Shell.class);
		if (shell == null) {
			IWorkbenchWindow activeWorkbenchWindow = JavaScriptPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
			if (activeWorkbenchWindow != null)
				shell = activeWorkbenchWindow.getShell();
		}
		final Shell finalShell = shell;
		if (finalShell != null) {
			finalShell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(finalShell, project, "org.eclipse.wst.jsdt.ui.propertyPages.BuildPathsPropertyPage", (String[]) pageIds.toArray(new String[pageIds.size()]), null); //$NON-NLS-1$
					if (dialog.open() == Window.CANCEL) {
						doUninstall(project, monitor);
					}
				}
			});
		}
	}
}