/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.core.ClasspathEntry;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;

public class ConvertUtility {
	private static final String NATURE_IDS[] = {JavaScriptCore.NATURE_ID};

	private static final String SYSTEM_LIBRARY = org.eclipse.wst.jsdt.launching.JavaRuntime.JRE_CONTAINER; //$NON-NLS-1$
	private static final String SYSTEM_SUPER_TYPE_NAME = new String(IIndexConstants.GLOBAL);

	private static final String BROWSER_LIBRARY = org.eclipse.wst.jsdt.launching.JavaRuntime.BASE_BROWSER_LIB; //$NON-NLS-1$
	public static final IPath BROWSER_LIBRARY_PATH = new Path(BROWSER_LIBRARY);
	private static final String BROWSER_SUPER_TYPE_NAME = new String(IIndexConstants.WINDOW);

	public static final String VIRTUAL_CONTAINER = "org.eclipse.wst.jsdt.launching.WebProject"; //$NON-NLS-1$
	public static final IIncludePathEntry VIRTUAL_SCOPE_ENTRY = JavaScriptCore.newContainerEntry(new Path(VIRTUAL_CONTAINER), new IAccessRule[0], new IIncludePathAttribute[]{IIncludePathAttribute.HIDE}, false);


	static void addJsNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (!hasNature(project)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + NATURE_IDS.length];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			// newNatures[prevNatures.length] = JavaScriptCore.NATURE_ID;
			for (int i = 0; i < NATURE_IDS.length; i++) {
				newNatures[prevNatures.length + i] = NATURE_IDS[i];
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		}
		else {
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	}

	public static boolean hasNature(IProject project) {
		try {
			for (int i = 0; i < NATURE_IDS.length; i++) {
				if (!project.hasNature(NATURE_IDS[i])) {
					return false;
				}
			}
		}
		catch (CoreException ex) {
			return false;
		}
		return true;
	}

	static void removeJsNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (hasNature(project)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length - NATURE_IDS.length];
			int k = 0;
			head : for (int i = 0; i < prevNatures.length; i++) {
				for (int j = 0; j < NATURE_IDS.length; j++) {
					if (prevNatures[i].equals(NATURE_IDS[j])) {
						continue head;
					}
				}
				newNatures[k++] = prevNatures[i];
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		}
		else {
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	}

	private boolean DEBUG = false;
	private IProject fCurrProject;
	private JavaProject fJavaProject;
	private IPath fOutputLocation;

	public ConvertUtility(IProject project) {
		fCurrProject = project;
		fOutputLocation = fCurrProject.getFullPath();
	}

	private IProgressMonitor monitorFor(IProgressMonitor monitor) {
		if (monitor != null)
			return monitor;
		return new NullProgressMonitor();
	}

	public void addBrowserSupport(boolean changeSuperType, IProgressMonitor monitor) throws CoreException {
		IProgressMonitor progressMonitor = monitorFor(monitor);
		progressMonitor.beginTask(Messages.converter_ConfiguringForBrowser, 2);
		fJavaProject = (JavaProject) JavaScriptCore.create(fCurrProject);
		if (!fJavaProject.exists())
			configure(new SubProgressMonitor(progressMonitor, 1));

		fJavaProject.setProject(fCurrProject);

		IIncludePathEntry[] includePath = getRawClassPath();
		includePath = addEntry(includePath, VIRTUAL_SCOPE_ENTRY, false);
		includePath = addEntry(includePath, JavaScriptCore.newContainerEntry(BROWSER_LIBRARY_PATH), false);

		try {
			if (!hasProjectClassPathFile()) {
				fJavaProject.setRawIncludepath(includePath, fOutputLocation, new SubProgressMonitor(progressMonitor, 1));
			}
			else {
				fJavaProject.setRawIncludepath(includePath, new SubProgressMonitor(progressMonitor, 1));
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}

		if (changeSuperType) {
			LibrarySuperType superType = new LibrarySuperType(BROWSER_LIBRARY_PATH, getJavaScriptProject(), BROWSER_SUPER_TYPE_NAME);
			getJavaScriptProject().setCommonSuperType(superType);
		}

		// getJavaProject().addToBuildSpec(BUILDER_ID);
		// fCurrProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		progressMonitor.done();
	}

	private IIncludePathEntry[] addEntry(IIncludePathEntry[] entries, IIncludePathEntry newEntry, boolean first) {
		for (int i = 0; i < entries.length; i++) {
			// avoid duplicate IIncludePathEntry-s
			if (newEntry.getPath().equals(entries[i].getPath())) {
				return entries;
			}
		}

		List entriesList = new ArrayList(Arrays.asList(entries));
		if (first && !entriesList.isEmpty())
			entriesList.add(0, newEntry);
		else
			entriesList.add(newEntry);
		return (IIncludePathEntry[]) entriesList.toArray(new IIncludePathEntry[entriesList.size()]);
	}

	// private void createSourceClassPath() {
	// if (hasAValidSourcePath()) {
	// return;
	// }
	// // IPath projectPath = fCurrProject.getFullPath();
	// // classPathEntries.add(JavaScriptCore.newSourceEntry(projectPath));
	// }

	// public void deconfigure() throws CoreException {
	// Vector badEntries = new Vector();
	// IIncludePathEntry defaultJRELibrary = createRuntimeEntry();
	// IIncludePathEntry[] localEntries = initLocalClassPath();
	// badEntries.add(defaultJRELibrary);
	// badEntries.addAll(Arrays.asList(localEntries));
	// IIncludePathEntry[] entries = getRawClassPath();
	// List goodEntries = new ArrayList();
	// for (int i = 0; i < entries.length; i++) {
	// if (!badEntries.contains(entries[i])) {
	// goodEntries.add(entries[i]);
	// }
	// }
	// IPath outputLocation = getJavaScriptProject().getOutputLocation();
	// getJavaScriptProject().setRawIncludepath((IIncludePathEntry[])
	// goodEntries.toArray(new IIncludePathEntry[] {}), outputLocation,
	// monitor);
	//
	// // getJavaProject().removeFromBuildSpec(BUILDER_ID);
	// getJavaScriptProject().deconfigure();
	//
	// removeJsNature(fCurrProject, monitor);
	// fCurrProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	// }

	private IIncludePathEntry[] addSystemEntry(IIncludePathEntry[] entries) {
		IIncludePathEntry defaultJRELibrary = createRuntimeEntry();
		try {
			for (int i = 0; i < entries.length; i++) {
				if (defaultJRELibrary.equals(entries[i])) {
					return entries;
				}
			}
		}
		catch (Exception e) {
			if (DEBUG) {
				System.out.println("Error checking system library in include path:" + e); //$NON-NLS-1$
			}
		}
		return addEntry(entries, defaultJRELibrary, false);
	}

	public void configure(IProgressMonitor monitor) throws CoreException {
		IProgressMonitor progressMonitor = monitorFor(monitor);
		progressMonitor.beginTask("", 2);//$NON-NLS-1$
		addJsNature(fCurrProject, new SubProgressMonitor(progressMonitor, 1));

		fJavaProject = (JavaProject) JavaScriptCore.create(fCurrProject);
		fJavaProject.setProject(fCurrProject);

		IIncludePathEntry[] includePath = getRawClassPath();
		includePath = addSystemEntry(includePath);

		try {
			if (!hasProjectClassPathFile()) {
				fJavaProject.setRawIncludepath(includePath, fOutputLocation, new SubProgressMonitor(progressMonitor, 1));
			}
			else {
				fJavaProject.setRawIncludepath(includePath, new SubProgressMonitor(progressMonitor, 1));
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}

		LibrarySuperType superType = new LibrarySuperType(new Path(SYSTEM_LIBRARY), getJavaScriptProject(), SYSTEM_SUPER_TYPE_NAME);
		getJavaScriptProject().setCommonSuperType(superType);
		progressMonitor.done();

		// getJavaProject().addToBuildSpec(BUILDER_ID);
		// fCurrProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}

	private IIncludePathEntry createRuntimeEntry() {
		return JavaScriptCore.newContainerEntry(new Path(SYSTEM_LIBRARY));
	}

	private JavaProject getJavaScriptProject() {
		if (fJavaProject == null) {
			fJavaProject = (JavaProject) JavaScriptCore.create(fCurrProject);
			fJavaProject.setProject(fCurrProject);
		}
		return fJavaProject;
	}

	public IProject getProject() {
		return this.fCurrProject;
	}

	public IIncludePathEntry[] getDefaultSourcePaths(IProject p) {
		IPath[] defaultExclusionPatterns = JavaScriptCore.getJavaScriptCore().getDefaultClasspathExclusionPatterns();
		IIncludePathEntry[] defaults = new IIncludePathEntry[] { JavaScriptCore.newSourceEntry(p.getFullPath(), defaultExclusionPatterns) };
		try {
			IConfigurationElement[] configurationElements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.wst.jsdt.core.sourcePathProvider");
			Map paths = new HashMap();
			for (int i = 0; i < configurationElements.length; i++) {
				DefaultSourcePathProvider provider = (DefaultSourcePathProvider) configurationElements[i].createExecutableExtension("class");
				if (provider != null) {
					IIncludePathEntry[] defaultSourcePaths = provider.getDefaultSourcePaths(p);
					for (int j = 0; j < defaultSourcePaths.length; j++) {
						if (defaultSourcePaths[i] instanceof ClasspathEntry) {
							ClasspathEntry cpe = (ClasspathEntry)defaultSourcePaths[i];
							
							Set<IPath> exclusions = new HashSet<IPath>();
							exclusions.addAll(Arrays.asList(cpe.getExclusionPatterns()));
							exclusions.addAll(Arrays.asList(defaultExclusionPatterns));
							IPath[] exclusionPatterns= exclusions.toArray(new IPath[exclusions.size()]);

							defaultSourcePaths[i] = JavaScriptCore.newSourceEntry(cpe.getPath(), 
										cpe.getInclusionPatterns(), exclusionPatterns, 
										cpe.getOutputLocation(), cpe.getExtraAttributes());

						}
						
						paths.put(defaultSourcePaths[j].getPath(), defaultSourcePaths[j]);
					}
				}
			}
			if (!paths.isEmpty()) {
				IPath[] pathsArray = (IPath[]) paths.keySet().toArray(new IPath[paths.size()]);
				// keep only the most specific paths
				for (int i = 0; i < pathsArray.length; i++) {
					for (int j = 0; j < pathsArray.length; j++) {
						if (i != j && pathsArray[i] != null && pathsArray[j] != null && pathsArray[j].isPrefixOf(pathsArray[i])) {
							// only remove if same kind of entry
							if ((((IIncludePathEntry) paths.get(pathsArray[i]))).getEntryKind() == (((IIncludePathEntry) paths.get(pathsArray[j]))).getEntryKind()) {
								paths.remove(pathsArray[j]);
								pathsArray[j] = null;
							}
						}
					}
				}
				defaults = (IIncludePathEntry[]) paths.values().toArray(new IIncludePathEntry[paths.size()]);
				Arrays.sort(defaults, new Comparator() {
					public int compare(Object o1, Object o2) {
						IIncludePathEntry entry1 = (IIncludePathEntry) o1;
						IIncludePathEntry entry2 = (IIncludePathEntry) o2;
						if (entry1.getEntryKind() == IIncludePathEntry.CPE_SOURCE && entry2.getEntryKind() != IIncludePathEntry.CPE_SOURCE)
							return -1;
						if (entry1.getEntryKind() != IIncludePathEntry.CPE_SOURCE && entry2.getEntryKind() == IIncludePathEntry.CPE_SOURCE)
							return 1;
						return entry1.getPath().toString().compareTo(entry2.getPath().toString());
					}
				});
			}
		}
		catch (Exception e) {
			if (Platform.inDebugMode()) {
				Platform.getLog(JavaScriptCore.getPlugin().getBundle()).log(new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, "Problem getting source paths", e));
			}
		}
		return defaults;
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
			}
			catch (Exception e) {
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
}
