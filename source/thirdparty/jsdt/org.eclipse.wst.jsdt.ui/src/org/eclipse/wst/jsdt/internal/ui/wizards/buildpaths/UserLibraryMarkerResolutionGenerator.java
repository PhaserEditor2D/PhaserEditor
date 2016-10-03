/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.jsdt.core.CorrectionEngine;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptModelMarker;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.preferences.BuildPathsPropertyPage;
import org.eclipse.wst.jsdt.internal.ui.preferences.UserLibraryPreferencePage;
import org.eclipse.wst.jsdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.ui.wizards.BuildPathDialogAccess;

public class UserLibraryMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {
	
	private final static IMarkerResolution[] NO_RESOLUTION = new IMarkerResolution[0];

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		int id= marker.getAttribute(IJavaScriptModelMarker.ID, -1);
		if (id == IJavaScriptModelStatusConstants.CP_CONTAINER_PATH_UNBOUND
				|| id == IJavaScriptModelStatusConstants.CP_VARIABLE_PATH_UNBOUND
				|| id == IJavaScriptModelStatusConstants.INVALID_CP_CONTAINER_ENTRY
				|| id == IJavaScriptModelStatusConstants.DEPRECATED_VARIABLE
				|| id == IJavaScriptModelStatusConstants.INVALID_INCLUDEPATH) {
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		final Shell shell= JavaScriptPlugin.getActiveWorkbenchShell();
		if (!hasResolutions(marker) || shell == null) {
			return NO_RESOLUTION;
		}
		
		ArrayList resolutions= new ArrayList();
		
		final IJavaScriptProject project= getJavaProject(marker);
		
		int id= marker.getAttribute(IJavaScriptModelMarker.ID, -1);
		if (id == IJavaScriptModelStatusConstants.CP_CONTAINER_PATH_UNBOUND) {
			String[] arguments= CorrectionEngine.getProblemArguments(marker);
			final IPath path= new Path(arguments[0]);
			
			if (path.segment(0).equals(JavaScriptCore.USER_LIBRARY_CONTAINER_ID)) {
				String label= NewWizardMessages.UserLibraryMarkerResolutionGenerator_changetouserlib_label; 
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME);
				resolutions.add(new UserLibraryMarkerResolution(label, image) {
					public void run(IMarker m) {
						changeToExistingLibrary(shell, path, false, project);
					}
				});
				if (path.segmentCount() == 2) {
					String label2= Messages.format(NewWizardMessages.UserLibraryMarkerResolutionGenerator_createuserlib_label, path.segment(1)); 
					Image image2= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
					resolutions.add(new UserLibraryMarkerResolution(label2, image2) {
						public void run(IMarker m) {
							createUserLibrary(shell, path, project);
						}
					});
				}
			}
			String label= NewWizardMessages.UserLibraryMarkerResolutionGenerator_changetoother; 
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME);
			resolutions.add(new UserLibraryMarkerResolution(label, image) {
				public void run(IMarker m) {
					changeToExistingLibrary(shell, path, true, project);
				}
			});
		}
		
		if (project != null) {
			resolutions.add(new OpenBuildPathMarkerResolution(project));
		}
		
		return (IMarkerResolution[]) resolutions.toArray(new IMarkerResolution[resolutions.size()]);
	}

	protected void changeToExistingLibrary(Shell shell, IPath path, boolean isNew, final IJavaScriptProject project) {
		try {
			IIncludePathEntry[] entries= project.getRawIncludepath();
			int idx= indexOfClasspath(entries, path);
			if (idx == -1) {
				return;
			}
			IIncludePathEntry[] res;
			if (isNew) {
				res= BuildPathDialogAccess.chooseContainerEntries(shell, project, entries);
				if (res == null) {
					return;
				}
			} else {
				IIncludePathEntry resEntry= BuildPathDialogAccess.configureContainerEntry(shell, entries[idx], project, entries);
				if (resEntry == null) {
					return;
				}
				res= new IIncludePathEntry[] { resEntry };
			}
			final IIncludePathEntry[] newEntries= new IIncludePathEntry[entries.length - 1 + res.length];
			System.arraycopy(entries, 0, newEntries, 0, idx);
			System.arraycopy(res, 0, newEntries, idx, res.length);
			System.arraycopy(entries, idx + 1, newEntries, idx + res.length, entries.length - idx - 1);
			
			IRunnableContext context= JavaScriptPlugin.getActiveWorkbenchWindow();
			if (context == null) {
				context= PlatformUI.getWorkbench().getProgressService();
			}
			context.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						project.setRawIncludepath(newEntries, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (JavaScriptModelException e) {
			String title= NewWizardMessages.UserLibraryMarkerResolutionGenerator_error_title; 
			String message= NewWizardMessages.UserLibraryMarkerResolutionGenerator_error_creationfailed_message; 
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.UserLibraryMarkerResolutionGenerator_error_title; 
			String message= NewWizardMessages.UserLibraryMarkerResolutionGenerator_error_applyingfailed_message; 
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InterruptedException e) {
			// user cancelled
		}
	}
	
	private int indexOfClasspath(IIncludePathEntry[] entries, IPath path) {
		for (int i= 0; i < entries.length; i++) {
			IIncludePathEntry curr= entries[i];
			if (curr.getEntryKind() == IIncludePathEntry.CPE_CONTAINER && curr.getPath().equals(path)) {
				return i;
			}
		}
		return -1;
	}
	
	protected void createUserLibrary(final Shell shell, IPath unboundPath, IJavaScriptProject project) {
		String name= unboundPath.segment(1);
		String id= UserLibraryPreferencePage.ID;
		HashMap data= new HashMap(3);
		data.put(UserLibraryPreferencePage.DATA_LIBRARY_TO_SELECT, name);
		data.put(UserLibraryPreferencePage.DATA_DO_CREATE, Boolean.TRUE);
		PreferencesUtil.createPreferenceDialogOn(shell, id, new String[] { id }, data).open();
	}

	private IJavaScriptProject getJavaProject(IMarker marker) {
		return JavaScriptCore.create(marker.getResource().getProject());
	}

	/**
	 * Library quick fix base class
	 */
	private static abstract class UserLibraryMarkerResolution implements IMarkerResolution, IMarkerResolution2 {
		
		private String fLabel;
		private Image fImage;
		
		public UserLibraryMarkerResolution(String label, Image image) {
			fLabel= label;
			fImage= image;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution#getLabel()
		 */
		public String getLabel() {
			return fLabel;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
		 */
		public String getDescription() {
			return null;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution2#getImage()
		 */
		public Image getImage() {
			return fImage;
		}
	}

	private static class OpenBuildPathMarkerResolution implements IMarkerResolution2 {
		private IJavaScriptProject fProject;

		public OpenBuildPathMarkerResolution(IJavaScriptProject project) {
			fProject= project;
		}

		public String getDescription() {
			return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_description, fProject.getElementName());
		}

		public Image getImage() {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ACCESSRULES_ATTRIB);
		}

		public String getLabel() {
			return CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_label;
		}

		public void run(IMarker marker) {
			PreferencesUtil.createPropertyDialogOn(JavaScriptPlugin.getActiveWorkbenchShell(), fProject, BuildPathsPropertyPage.PROP_ID, null, null).open();
		}
	}

}
