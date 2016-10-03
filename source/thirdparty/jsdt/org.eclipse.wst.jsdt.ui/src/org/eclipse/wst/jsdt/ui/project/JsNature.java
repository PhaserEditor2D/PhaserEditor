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
package org.eclipse.wst.jsdt.ui.project;

import java.util.Arrays;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
/**
 * 
* Provisional API: This class/interface 
 * of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class JsNature implements IProjectNature {
	//private static final String FILENAME_CLASSPATH = ".classpath"; //$NON-NLS-1$
	// private static final String NATURE_IDS[] =
	// {"org.eclipse.wst.jsdt.web.core.embeded.jsNature",JavaScriptCore.NATURE_ID};
	// //$NON-NLS-1$
	private static final String NATURE_IDS[] = { JavaScriptCore.NATURE_ID };
	private static final String SUPER_TYPE_NAME = JavaUIMessages.JsNature_Global;
	private static final String SUPER_TYPE_LIBRARY = "org.eclipse.wst.jsdt.launching.JRE_CONTAINER"; //$NON-NLS-1$
	
	public static void addJsNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (!JsNature.hasNature(project)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + JsNature.NATURE_IDS.length];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			// newNatures[prevNatures.length] = JavaScriptCore.NATURE_ID;
			for (int i = 0; i < JsNature.NATURE_IDS.length; i++) {
				newNatures[prevNatures.length + i] = JsNature.NATURE_IDS[i];
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		} else {
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	}
	
	public static boolean hasNature(IProject project) {
		try {
			for (int i = 0; i < JsNature.NATURE_IDS.length; i++) {
				if (!project.hasNature(JsNature.NATURE_IDS[i])) {
					return false;
				}
			}
		} catch (CoreException ex) {
			return false;
		}
		return true;
	}
	
	public static void removeJsNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (JsNature.hasNature(project)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length - JsNature.NATURE_IDS.length];
			int k = 0;
			head: for (int i = 0; i < prevNatures.length; i++) {
				for (int j = 0; j < JsNature.NATURE_IDS.length; j++) {
					if (prevNatures[i].equals(JsNature.NATURE_IDS[j])) {
						continue head;
					}
				}
				newNatures[k++] = prevNatures[i];
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		} else {
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	}
	private Vector classPathEntries = new Vector();
	private boolean DEBUG = false;
	private IProject fCurrProject;
	private JavaProject fJavaProject;
	private IPath fOutputLocation;
	private IProgressMonitor monitor;
	
	public JsNature() {
		monitor = new NullProgressMonitor();
	}
	
	public JsNature(IProject project, IProgressMonitor monitor) {
		fCurrProject = project;
		if (monitor != null) {
			this.monitor = monitor;
		} else {
			monitor = new NullProgressMonitor();
		}
	}
	
	public void configure() throws CoreException {
		
		initOutputPath();
		createSourceClassPath();
		initJREEntry();
		initLocalClassPath();
		if (hasProjectClassPathFile()) {
			IIncludePathEntry[] entries = getRawClassPath();
			if (entries != null && entries.length > 0) {
				classPathEntries.removeAll(Arrays.asList(entries));
				classPathEntries.addAll(Arrays.asList(entries));
			}
		}
		JsNature.addJsNature(fCurrProject, monitor);
		fJavaProject = (JavaProject) JavaScriptCore.create(fCurrProject);
		fJavaProject.setProject(fCurrProject);
		try {
			// , fOutputLocation
			if (!hasProjectClassPathFile()) {
				fJavaProject.setRawIncludepath((IIncludePathEntry[]) classPathEntries.toArray(new IIncludePathEntry[] {}), fOutputLocation, monitor);
			}
			if (hasProjectClassPathFile()) {
				fJavaProject.setRawIncludepath((IIncludePathEntry[]) classPathEntries.toArray(new IIncludePathEntry[] {}), monitor);
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		LibrarySuperType superType = new LibrarySuperType(new Path( SUPER_TYPE_LIBRARY),  getJavaProject(), SUPER_TYPE_NAME);
		getJavaProject().setCommonSuperType(superType);
		// getJavaProject().addToBuildSpec(BUILDER_ID);
		fCurrProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}
	
	private void createSourceClassPath() {
		if (hasAValidSourcePath()) {
			return;
		}
		// IPath projectPath = fCurrProject.getFullPath();
		// classPathEntries.add(JavaScriptCore.newSourceEntry(projectPath));
	}
	
	public void deconfigure() throws CoreException {
		Vector badEntries = new Vector();
		IIncludePathEntry[] defaultJRELibrary = PreferenceConstants.getDefaultJRELibrary();
		IIncludePathEntry[] localEntries = initLocalClassPath();
		badEntries.addAll(Arrays.asList(defaultJRELibrary));
		badEntries.addAll(Arrays.asList(localEntries));
		IIncludePathEntry[] entries = getRawClassPath();
		Vector goodEntries = new Vector();
		for (int i = 0; i < entries.length; i++) {
			if (!badEntries.contains(entries[i])) {
				goodEntries.add(entries[i]);
			}
		}
		// getJavaProject().removeFromBuildSpec(BUILDER_ID);
		IPath outputLocation = getJavaProject().getOutputLocation();
		getJavaProject().setRawIncludepath((IIncludePathEntry[]) goodEntries.toArray(new IIncludePathEntry[] {}), outputLocation, monitor);
		getJavaProject().deconfigure();
		JsNature.removeJsNature(fCurrProject, monitor);
		fCurrProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}
	
	
	
	public JavaProject getJavaProject() {
		if (fJavaProject == null) {
			fJavaProject = (JavaProject) JavaScriptCore.create(fCurrProject);
			fJavaProject.setProject(fCurrProject);
		}
		return fJavaProject;
	}
	
	public IProject getProject() {
		return this.fCurrProject;
	}
	
	private IIncludePathEntry[] getRawClassPath() {
		JavaProject proj = new JavaProject();
		proj.setProject(fCurrProject);
		return proj.readRawIncludepath();
	}
	
	private boolean hasAValidSourcePath() {
		if (hasProjectClassPathFile()) {
			try {
				IIncludePathEntry[] entries = getRawClassPath();
				for (int i = 0; i < entries.length; i++) {
					if (entries[i].getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
						return true;
					}
				}
			} catch (Exception e) {
				if (DEBUG) {
					System.out.println("Error checking sourcepath:" + e); //$NON-NLS-1$
				}
			}
		}
		return false;
	}
	
	private boolean hasProjectClassPathFile() {
		if (fCurrProject == null) {
			return false;
		}
		return fCurrProject.getFolder(JavaProject.DEFAULT_PREFERENCES_DIRNAME).getFile(JavaProject.CLASSPATH_FILENAME).exists();
	}
	
	private void initJREEntry() {
		IIncludePathEntry[] defaultJRELibrary = PreferenceConstants.getDefaultJRELibrary();
		try {
			IIncludePathEntry[] entries = getRawClassPath();
			for (int i = 0; i < entries.length; i++) {
				if (entries[i] == defaultJRELibrary[0]) {
					return;
				}
			}
			classPathEntries.add(defaultJRELibrary[0]);
		} catch (Exception e) {
			if (DEBUG) {
				System.out.println("Error checking sourcepath:" + e); //$NON-NLS-1$
			}
		}
	}
	
	private IIncludePathEntry[] initLocalClassPath() {
		
		
		
		
		//IPath webRoot = WebRootFinder.getWebContentFolder(fCurrProject);
		IIncludePathEntry source = JavaScriptCore.newSourceEntry(fCurrProject.getFullPath().append("/")); //$NON-NLS-1$
	//	classPathEntries.add(source);
		return new IIncludePathEntry[] {source};
	}
	
	private void initOutputPath() {
		if (fOutputLocation == null) {
			fOutputLocation = fCurrProject.getFullPath();
		}
	}
	
	public void setProject(IProject project) {
		this.fCurrProject = project;
	}
}
