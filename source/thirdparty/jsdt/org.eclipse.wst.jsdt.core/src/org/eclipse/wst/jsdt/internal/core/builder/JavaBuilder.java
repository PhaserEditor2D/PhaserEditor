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
package org.eclipse.wst.jsdt.internal.core.builder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptModelMarker;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.ValidationParticipant;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.wst.jsdt.internal.core.ClasspathEntry;
import org.eclipse.wst.jsdt.internal.core.JavaModel;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class JavaBuilder extends IncrementalProjectBuilder {

IProject currentProject;
JavaProject javaProject;
IWorkspaceRoot workspaceRoot;
ValidationParticipant[] participants;
NameEnvironment nameEnvironment;
SimpleLookupTable binaryLocationsPerProject; // maps a project to its binary resources (output folders, class folders, zip/jar files)
public State lastState;
BuildNotifier notifier;
char[][] extraResourceFileFilters;
String[] extraResourceFolderFilters;
public static final String SOURCE_ID = "JSDT"; //$NON-NLS-1$

public static boolean DEBUG = false;

/**
 * A list of project names that have been built.
 * This list is used to reset the JavaModel.existingExternalFiles cache when a build cycle begins
 * so that deleted external jars are discovered.
 */
static ArrayList builtProjects = null;

public static IMarker[] getProblemsFor(IResource resource) {
	try {
		if (resource != null && resource.exists()) {
			IMarker[] markers = resource.findMarkers(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			Set markerTypes = JavaModelManager.getJavaModelManager().validationParticipants.managedMarkerTypes();
			if (markerTypes.isEmpty()) return markers;
			ArrayList markerList = new ArrayList(5);
			for (int i = 0, length = markers.length; i < length; i++) {
				markerList.add(markers[i]);
			}
			Iterator iterator = markerTypes.iterator();
			while (iterator.hasNext()) {
				markers = resource.findMarkers((String) iterator.next(), false, IResource.DEPTH_INFINITE);
				for (int i = 0, length = markers.length; i < length; i++) {
					markerList.add(markers[i]);
				}
			}
			IMarker[] result;
			markerList.toArray(result = new IMarker[markerList.size()]);
			return result;
		}
	} catch (CoreException e) {
		// assume there are no problems
	}
	return new IMarker[0];
}

public static IMarker[] getTasksFor(IResource resource) {
	try {
		if (resource != null && resource.exists())
			return resource.findMarkers(IJavaScriptModelMarker.TASK_MARKER, false, IResource.DEPTH_INFINITE);
	} catch (CoreException e) {
		// assume there are no tasks
	}
	return new IMarker[0];
}

/**
 * Hook allowing to initialize some static state before a complete build iteration.
 * This hook is invoked during PRE_AUTO_BUILD notification
 */
public static void buildStarting() {
	// build is about to start
}

/**
 * Hook allowing to reset some static state after a complete build iteration.
 * This hook is invoked during POST_AUTO_BUILD notification
 */
public static void buildFinished() {
	BuildNotifier.resetProblemCounters();
}

public static void removeProblemsFor(IResource resource) {
	try {
		if (resource != null && resource.exists()) {
			resource.deleteMarkers(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);

			// delete managed markers
			Set markerTypes = JavaModelManager.getJavaModelManager().validationParticipants.managedMarkerTypes();
			if (markerTypes.size() == 0) return;
			Iterator iterator = markerTypes.iterator();
			while (iterator.hasNext())
				resource.deleteMarkers((String) iterator.next(), false, IResource.DEPTH_INFINITE);
		}
	} catch (CoreException e) {
		// assume there were no problems
	}
}

public static void removeTasksFor(IResource resource) {
	try {
		if (resource != null && resource.exists())
			resource.deleteMarkers(IJavaScriptModelMarker.TASK_MARKER, false, IResource.DEPTH_INFINITE);
	} catch (CoreException e) {
		// assume there were no problems
	}
}

public static void removeProblemsAndTasksFor(IResource resource) {
	try {
		if (resource != null && resource.exists()) {
			resource.deleteMarkers(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(IJavaScriptModelMarker.TASK_MARKER, false, IResource.DEPTH_INFINITE);

			// delete managed markers
			Set markerTypes = JavaModelManager.getJavaModelManager().validationParticipants.managedMarkerTypes();
			if (markerTypes.size() == 0) return;
			Iterator iterator = markerTypes.iterator();
			while (iterator.hasNext())
				resource.deleteMarkers((String) iterator.next(), false, IResource.DEPTH_INFINITE);
		}
	} catch (CoreException e) {
		// assume there were no problems
	}
}

public static State readState(IProject project, DataInputStream in) throws IOException {
	return State.read(project, in);
}

public static void writeState(Object state, DataOutputStream out) throws IOException {
	((State) state).write(out);
}

protected IProject[] build(int kind, Map ignored, IProgressMonitor monitor) throws CoreException {
	this.currentProject = getProject();
	if (currentProject == null || !currentProject.isAccessible()) return new IProject[0];

	if (DEBUG)
		System.out.println("\nStarting build of " + currentProject.getName() //$NON-NLS-1$
			+ " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
	this.notifier = new BuildNotifier(monitor, currentProject);
	notifier.begin();
	boolean ok = false;
	try {
		notifier.checkCancel();
		kind = initializeBuilder(kind, true);

		if (isWorthBuilding()) {
			if (kind == FULL_BUILD) {
				if (DEBUG)
					System.out.println("Performing full build as requested by user"); //$NON-NLS-1$
				buildAll();
			} else {
				if ((this.lastState = getLastState(currentProject)) == null) {
					if (DEBUG)
						System.out.println("Performing full build since last saved state was not found"); //$NON-NLS-1$
					buildAll();
				} else if (hasClasspathChanged()) {
					// if the output location changes, do not delete the binary files from old location
					// the user may be trying something
					if (DEBUG)
						System.out.println("Performing full build since classpath has changed"); //$NON-NLS-1$
					buildAll();
				} else if (nameEnvironment.sourceLocations.length > 0) {
					// if there is no source to compile & no classpath changes then we are done
					SimpleLookupTable deltas = findDeltas();
					if (deltas == null) {
						if (DEBUG)
							System.out.println("Performing full build since deltas are missing after incremental request"); //$NON-NLS-1$
						buildAll();
					} else if (deltas.elementSize > 0) {
						buildDeltas(deltas);
					} else if (DEBUG) {
						System.out.println("Nothing to build since deltas were empty"); //$NON-NLS-1$
					}
				} else {
					if (hasStructuralDelta()) { // double check that a jar file didn't get replaced in a binary project
						if (DEBUG)
							System.out.println("Performing full build since there are structural deltas"); //$NON-NLS-1$
						buildAll();
					} else {
						if (DEBUG)
							System.out.println("Nothing to build since there are no source folders and no deltas"); //$NON-NLS-1$
						lastState.tagAsNoopBuild();
					}
				}
			}
			ok = true;
		}
	} catch (CoreException e) {
		Util.log(e, "JavaBuilder handling CoreException while building: " + currentProject.getName()); //$NON-NLS-1$
		IMarker marker = currentProject.createMarker(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER);
		marker.setAttributes(
			new String[] {IMarker.MESSAGE, IMarker.SEVERITY, IJavaScriptModelMarker.CATEGORY_ID, IMarker.SOURCE_ID},
			new Object[] {
				Messages.bind(Messages.build_inconsistentProject, e.getLocalizedMessage()),
				Integer.valueOf(IMarker.SEVERITY_ERROR),
				Integer.valueOf(CategorizedProblem.CAT_BUILDPATH),
				JavaBuilder.SOURCE_ID
			}
		);
	} catch (ImageBuilderInternalException e) {
		Util.log(e.getThrowable(), "JavaBuilder handling ImageBuilderInternalException while building: " + currentProject.getName()); //$NON-NLS-1$
		IMarker marker = currentProject.createMarker(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER);
		marker.setAttributes(
			new String[] {IMarker.MESSAGE, IMarker.SEVERITY, IJavaScriptModelMarker.CATEGORY_ID, IMarker.SOURCE_ID},
			new Object[] {
				Messages.bind(Messages.build_inconsistentProject, e.getLocalizedMessage()),
				Integer.valueOf(IMarker.SEVERITY_ERROR),
				Integer.valueOf(CategorizedProblem.CAT_BUILDPATH),
				JavaBuilder.SOURCE_ID
			}
		);
	} catch (MissingSourceFileException e) {
		// do not log this exception since its thrown to handle aborted compiles because of missing source files
		if (DEBUG)
			System.out.println(Messages.bind(Messages.build_missingSourceFile, e.missingSourceFile));
		removeProblemsAndTasksFor(currentProject); // make this the only problem for this project
		IMarker marker = currentProject.createMarker(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER);
		marker.setAttributes(
			new String[] {IMarker.MESSAGE, IMarker.SEVERITY, IMarker.SOURCE_ID},
			new Object[] {
				Messages.bind(Messages.build_missingSourceFile, e.missingSourceFile),
				Integer.valueOf(IMarker.SEVERITY_ERROR),
				JavaBuilder.SOURCE_ID
			}
		);
	} finally {
		if (!ok)
			// If the build failed, clear the previously built state, forcing a full build next time.
			clearLastState();
		notifier.done();
		cleanup();
	}
	IProject[] requiredProjects = getRequiredProjects(true);
	if (DEBUG)
		System.out.println("Finished build of " + currentProject.getName() //$NON-NLS-1$
			+ " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
	return requiredProjects;
}

private void buildAll() {
	notifier.checkCancel();
	notifier.subTask(Messages.bind(Messages.build_preparingBuild, this.currentProject.getName()));
	if (DEBUG && lastState != null)
		System.out.println("Clearing last state : " + lastState); //$NON-NLS-1$
	clearLastState();
	BatchImageBuilder imageBuilder = new BatchImageBuilder(this, true);
	imageBuilder.build();
	recordNewState(imageBuilder.newState);
}

private void buildDeltas(SimpleLookupTable deltas) {
	notifier.checkCancel();
	notifier.subTask(Messages.bind(Messages.build_preparingBuild, this.currentProject.getName()));
	if (DEBUG && lastState != null)
		System.out.println("Clearing last state : " + lastState); //$NON-NLS-1$
	clearLastState(); // clear the previously built state so if the build fails, a full build will occur next time
	IncrementalImageBuilder imageBuilder = new IncrementalImageBuilder(this);
	if (imageBuilder.build(deltas)) {
		recordNewState(imageBuilder.newState);
	} else {
		if (DEBUG)
			System.out.println("Performing full build since incremental build failed"); //$NON-NLS-1$
		buildAll();
	}
}

protected void clean(IProgressMonitor monitor) throws CoreException {
	this.currentProject = getProject();
	if (currentProject == null || !currentProject.isAccessible()) return;

	if (DEBUG)
		System.out.println("\nCleaning " + currentProject.getName() //$NON-NLS-1$
			+ " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
	this.notifier = new BuildNotifier(monitor, currentProject);
	notifier.begin();
	try {
		notifier.checkCancel();

		initializeBuilder(CLEAN_BUILD, true);
		if (DEBUG)
			System.out.println("Clearing last state as part of clean : " + lastState); //$NON-NLS-1$
		clearLastState();
		removeProblemsAndTasksFor(currentProject);
//		new BatchImageBuilder(this, false).cleanOutputFolders(false);
	} catch (CoreException e) {
		Util.log(e, "JavaBuilder handling CoreException while cleaning: " + currentProject.getName()); //$NON-NLS-1$
		IMarker marker = currentProject.createMarker(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER);
		marker.setAttributes(
			new String[] {IMarker.MESSAGE, IMarker.SEVERITY, IMarker.SOURCE_ID},
			new Object[] {
				Messages.bind(Messages.build_inconsistentProject, e.getLocalizedMessage()),
				Integer.valueOf(IMarker.SEVERITY_ERROR),
				JavaBuilder.SOURCE_ID
			}
		);
	} finally {
		notifier.done();
		cleanup();
	}
	if (DEBUG)
		System.out.println("Finished cleaning " + currentProject.getName() //$NON-NLS-1$
			+ " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
}

private void cleanup() {
	this.participants = null;
	this.nameEnvironment = null;
	this.binaryLocationsPerProject = null;
	this.lastState = null;
	this.notifier = null;
	this.extraResourceFileFilters = null;
	this.extraResourceFolderFilters = null;
}

private void clearLastState() {
	JavaModelManager.getJavaModelManager().setLastBuiltState(currentProject, null);
}

boolean filterExtraResource(IResource resource) {
	if (extraResourceFileFilters != null) {
		char[] name = resource.getName().toCharArray();
		for (int i = 0, l = extraResourceFileFilters.length; i < l; i++)
			if (CharOperation.match(extraResourceFileFilters[i], name, true))
				return true;
	}
	if (extraResourceFolderFilters != null) {
		IPath path = resource.getProjectRelativePath();
		String pathName = path.toString();
		int count = path.segmentCount();
		if (resource.getType() == IResource.FILE) count--;
		for (int i = 0, l = extraResourceFolderFilters.length; i < l; i++)
			if (pathName.indexOf(extraResourceFolderFilters[i]) != -1)
				for (int j = 0; j < count; j++)
					if (extraResourceFolderFilters[i].equals(path.segment(j)))
						return true;
	}
	return false;
}

private SimpleLookupTable findDeltas() {
	notifier.subTask(Messages.bind(Messages.build_readingDelta, currentProject.getName()));
	IResourceDelta delta = getDelta(currentProject);
	SimpleLookupTable deltas = new SimpleLookupTable(3);
	if (delta != null) {
		if (delta.getKind() != IResourceDelta.NO_CHANGE) {
			if (DEBUG)
				System.out.println("Found source delta for: " + currentProject.getName()); //$NON-NLS-1$
			deltas.put(currentProject, delta);
		}
	} else {
		if (DEBUG)
			System.out.println("Missing delta for: " + currentProject.getName()); //$NON-NLS-1$
		notifier.subTask(""); //$NON-NLS-1$
		return null;
	}

	Object[] keyTable = binaryLocationsPerProject.keyTable;
	Object[] valueTable = binaryLocationsPerProject.valueTable;
	nextProject : for (int i = 0, l = keyTable.length; i < l; i++) {
		IProject p = (IProject) keyTable[i];
		if (p != null && p != currentProject) {
			State s = getLastState(p);
			if (!lastState.wasStructurallyChanged(p, s)) { // see if we can skip its delta
				if (s.wasNoopBuild())
					continue nextProject; // project has no source folders and can be skipped
				ClasspathLocation[] classFoldersAndJars = (ClasspathLocation[]) valueTable[i];
				boolean canSkip = true;
				for (int j = 0, m = classFoldersAndJars.length; j < m; j++) {
					if (classFoldersAndJars[j].isOutputFolder())
						classFoldersAndJars[j] = null; // can ignore output folder since project was not structurally changed
					else
						canSkip = false;
				}
				if (canSkip) continue nextProject; // project has no structural changes in its output folders
			}

			notifier.subTask(Messages.bind(Messages.build_readingDelta, p.getName()));
			delta = getDelta(p);
			if (delta != null) {
				if (delta.getKind() != IResourceDelta.NO_CHANGE) {
					if (DEBUG)
						System.out.println("Found binary delta for: " + p.getName()); //$NON-NLS-1$
					deltas.put(p, delta);
				}
			} else {
				if (DEBUG)
					System.out.println("Missing delta for: " + p.getName());	 //$NON-NLS-1$
				notifier.subTask(""); //$NON-NLS-1$
				return null;
			}
		}
	}
	notifier.subTask(""); //$NON-NLS-1$
	return deltas;
}

public State getLastState(IProject project) {
	return (State) JavaModelManager.getJavaModelManager().getLastBuiltState(project, notifier.monitor);
}

/* Return the list of projects for which it requires a resource delta. This builder's project
* is implicitly included and need not be specified. Builders must re-specify the list
* of interesting projects every time they are run as this is not carried forward
* beyond the next build. Missing projects should be specified but will be ignored until
* they are added to the workspace.
*/
private IProject[] getRequiredProjects(boolean includeBinaryPrerequisites) {
	if (javaProject == null || workspaceRoot == null) return new IProject[0];

	ArrayList projects = new ArrayList();
	try {
		IIncludePathEntry[] entries = javaProject.getExpandedClasspath();
		for (int i = 0, l = entries.length; i < l; i++) {
			IIncludePathEntry entry = entries[i];
			IPath path = entry.getPath();
			IProject p = null;
			switch (entry.getEntryKind()) {
				case IIncludePathEntry.CPE_PROJECT :
					p = workspaceRoot.getProject(path.lastSegment()); // missing projects are considered too
					if (((ClasspathEntry) entry).isOptional() && !JavaProject.hasJavaNature(p)) // except if entry is optional
						p = null;
					break;
				case IIncludePathEntry.CPE_LIBRARY :
					if (includeBinaryPrerequisites && path.segmentCount() > 1) {
						// some binary resources on the class path can come from projects that are not included in the project references
						IResource resource = workspaceRoot.findMember(path.segment(0));
						if (resource instanceof IProject)
							p = (IProject) resource;
					}
			}
			if (p != null && !projects.contains(p))
				projects.add(p);
		}
	} catch(JavaScriptModelException e) {
		return new IProject[0];
	}
	IProject[] result = new IProject[projects.size()];
	projects.toArray(result);
	return result;
}

boolean hasBuildpathErrors() throws CoreException {
//	IMarker[] markers = this.currentProject.findMarkers(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
//	for (int i = 0, l = markers.length; i < l; i++)
//		if (markers[i].getAttribute(IJavaScriptModelMarker.CATEGORY_ID, -1) == CategorizedProblem.CAT_BUILDPATH)
//			return true;
	return false;
}

private boolean hasClasspathChanged() {
	ClasspathMultiDirectory[] newSourceLocations = nameEnvironment.sourceLocations;
	ClasspathMultiDirectory[] oldSourceLocations = lastState.sourceLocations;
	int newLength = newSourceLocations.length;
	int oldLength = oldSourceLocations.length;
	int n, o;
	for (n = o = 0; n < newLength && o < oldLength; n++, o++) {
		if (newSourceLocations[n].equals(oldSourceLocations[o])) continue; // checks source & output folders
		try {
			if (newSourceLocations[n].sourceFolder.members().length == 0) { // added new empty source folder
				o--;
				continue;
			}
		} catch (CoreException ignore) { // skip it
		}
		if (DEBUG) {
			System.out.println("New location: " + newSourceLocations[n] + "\n!= old location: " + oldSourceLocations[o]); //$NON-NLS-1$ //$NON-NLS-2$
			printLocations(newSourceLocations, oldSourceLocations);
		}
		return true;
	}
	while (n < newLength) {
		try {
			if (newSourceLocations[n].sourceFolder.members().length == 0) { // added new empty source folder
				n++;
				continue;
			}
		} catch (CoreException ignore) { // skip it
		}
		if (DEBUG) {
			System.out.println("Added non-empty source folder"); //$NON-NLS-1$
			printLocations(newSourceLocations, oldSourceLocations);
		}
		return true;
	}
	if (o < oldLength) {
		if (DEBUG) {
			System.out.println("Removed source folder"); //$NON-NLS-1$
			printLocations(newSourceLocations, oldSourceLocations);
		}
		return true;
	}

	ClasspathLocation[] newBinaryLocations = nameEnvironment.binaryLocations;
	ClasspathLocation[] oldBinaryLocations = lastState.binaryLocations;
	newLength = newBinaryLocations.length;
	oldLength = oldBinaryLocations.length;
	for (n = o = 0; n < newLength && o < oldLength; n++, o++) {
		if (newBinaryLocations[n].equals(oldBinaryLocations[o])) continue;
		if (DEBUG) {
			System.out.println("New location: " + newBinaryLocations[n] + "\n!= old location: " + oldBinaryLocations[o]); //$NON-NLS-1$ //$NON-NLS-2$
			printLocations(newBinaryLocations, oldBinaryLocations);
		}
		return true;
	}
	if (n < newLength || o < oldLength) {
		if (DEBUG) {
			System.out.println("Number of binary folders/jar files has changed:"); //$NON-NLS-1$
			printLocations(newBinaryLocations, oldBinaryLocations);
		}
		return true;
	}
	return false;
}

private boolean hasJavaBuilder(IProject project) throws CoreException {
	ICommand[] buildCommands = project.getDescription().getBuildSpec();
	for (int i = 0, l = buildCommands.length; i < l; i++)
		if (buildCommands[i].getBuilderName().equals(JavaScriptCore.BUILDER_ID))
			return true;
	return false;
}

private boolean hasStructuralDelta() {
	// handle case when currentProject has only .class file folders and/or jar files... no source/output folders
	IResourceDelta delta = getDelta(currentProject);
	if (delta != null && delta.getKind() != IResourceDelta.NO_CHANGE) {
		ClasspathLocation[] classFoldersAndJars = (ClasspathLocation[]) binaryLocationsPerProject.get(currentProject);
		if (classFoldersAndJars != null) {
			for (int i = 0, l = classFoldersAndJars.length; i < l; i++) {
				ClasspathLocation classFolderOrJar = classFoldersAndJars[i]; // either a .class file folder or a zip/jar file
				if (classFolderOrJar != null) {
					IPath p = classFolderOrJar.getProjectRelativePath();
					if (p != null) {
						IResourceDelta binaryDelta = delta.findMember(p);
						if (binaryDelta != null && binaryDelta.getKind() != IResourceDelta.NO_CHANGE)
							return true;
					}
				}
			}
		}
	}
	return false;
}

private int initializeBuilder(int kind, boolean forBuild) throws CoreException {
	// some calls just need the nameEnvironment initialized so skip the rest
	this.javaProject = (JavaProject) JavaScriptCore.create(currentProject);
	this.workspaceRoot = currentProject.getWorkspace().getRoot();

	if (forBuild) {
		// cache the known participants for this project
		this.participants = JavaModelManager.getJavaModelManager().validationParticipants.getvalidationParticipants(this.javaProject);
		if (this.participants != null)
			for (int i = 0, l = this.participants.length; i < l; i++)
				if (this.participants[i].aboutToBuild(this.javaProject) == ValidationParticipant.NEEDS_FULL_BUILD)
					kind = FULL_BUILD;

		// Flush the existing external files cache if this is the beginning of a build cycle
		String projectName = currentProject.getName();
		if (builtProjects == null || builtProjects.contains(projectName)) {
			JavaModel.flushExternalFileCache();
			builtProjects = new ArrayList();
		}
		builtProjects.add(projectName);
	}

	this.binaryLocationsPerProject = new SimpleLookupTable(3);
	this.nameEnvironment = new NameEnvironment(workspaceRoot, javaProject, binaryLocationsPerProject, notifier);

	if (forBuild) {
		String filterSequence = javaProject.getOption(JavaScriptCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, true);
		char[][] filters = filterSequence != null && filterSequence.length() > 0
			? CharOperation.splitAndTrimOn(',', filterSequence.toCharArray())
			: null;
		if (filters == null) {
			this.extraResourceFileFilters = null;
			this.extraResourceFolderFilters = null;
		} else {
			int fileCount = 0, folderCount = 0;
			for (int i = 0, l = filters.length; i < l; i++) {
				char[] f = filters[i];
				if (f.length == 0) continue;
				if (f[f.length - 1] == '/') folderCount++; else fileCount++;
			}
			this.extraResourceFileFilters = new char[fileCount][];
			this.extraResourceFolderFilters = new String[folderCount];
			for (int i = 0, l = filters.length; i < l; i++) {
				char[] f = filters[i];
				if (f.length == 0) continue;
				if (f[f.length - 1] == '/')
					extraResourceFolderFilters[--folderCount] = new String(f, 0, f.length - 1);
				else
					extraResourceFileFilters[--fileCount] = f;
			}
		}
	}
	return kind;
}

private boolean isClasspathBroken(IIncludePathEntry[] classpath, IProject p) throws CoreException {
	IMarker[] markers = p.findMarkers(IJavaScriptModelMarker.BUILDPATH_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
	for (int i = 0, l = markers.length; i < l; i++)
		if (markers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
			return true;
	return false;
}

private boolean isWorthBuilding() throws CoreException {
	boolean abortBuilds =
		JavaScriptCore.ABORT.equals(javaProject.getOption(JavaScriptCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, true));
	if (!abortBuilds) return true;

	// Abort build only if there are classpath errors
	if (isClasspathBroken(javaProject.getRawIncludepath(), currentProject)) {
		if (DEBUG)
			System.out.println("Aborted build because project has classpath errors (incomplete or involved in cycle)"); //$NON-NLS-1$

		removeProblemsAndTasksFor(currentProject); // remove all compilation problems

		IMarker marker = currentProject.createMarker(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER);
		marker.setAttributes(
			new String[] {IMarker.MESSAGE, IMarker.SEVERITY, IJavaScriptModelMarker.CATEGORY_ID, IMarker.SOURCE_ID},
			new Object[] {
				Messages.build_abortDueToClasspathProblems,
				Integer.valueOf(IMarker.SEVERITY_ERROR),
				Integer.valueOf(CategorizedProblem.CAT_BUILDPATH),
				JavaBuilder.SOURCE_ID
			}
		);
		return false;
	}

	if (JavaScriptCore.WARNING.equals(javaProject.getOption(JavaScriptCore.CORE_INCOMPLETE_CLASSPATH, true)))
		return true;

	// make sure all prereq projects have valid build states... only when aborting builds since projects in cycles do not have build states
	// except for projects involved in a 'warning' cycle (see below)
	IProject[] requiredProjects = getRequiredProjects(false);
	for (int i = 0, l = requiredProjects.length; i < l; i++) {
		IProject p = requiredProjects[i];
		if (getLastState(p) == null)  {
			// The prereq project has no build state: if this prereq project has a 'warning' cycle marker then allow build (see bug id 23357)
			JavaProject prereq = (JavaProject) JavaScriptCore.create(p);
			if (prereq.hasCycleMarker() && JavaScriptCore.WARNING.equals(javaProject.getOption(JavaScriptCore.CORE_CIRCULAR_CLASSPATH, true))) {
				if (DEBUG)
					System.out.println("Continued to build even though prereq project " + p.getName() //$NON-NLS-1$
						+ " was not built since its part of a cycle"); //$NON-NLS-1$
				continue;
			}
			if (!hasJavaBuilder(p)) {
				if (DEBUG)
					System.out.println("Continued to build even though prereq project " + p.getName() //$NON-NLS-1$
						+ " is not built by JavaBuilder"); //$NON-NLS-1$
				continue;
			}
			if (DEBUG)
				System.out.println("Aborted build because prereq project " + p.getName() //$NON-NLS-1$
					+ " was not built"); //$NON-NLS-1$

			removeProblemsAndTasksFor(currentProject); // make this the only problem for this project
			IMarker marker = currentProject.createMarker(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER);
			marker.setAttributes(
				new String[] {IMarker.MESSAGE, IMarker.SEVERITY, IJavaScriptModelMarker.CATEGORY_ID, IMarker.SOURCE_ID},
				new Object[] {
					isClasspathBroken(prereq.getRawIncludepath(), p)
						? Messages.bind(Messages.build_prereqProjectHasClasspathProblems, p.getName())
						: Messages.bind(Messages.build_prereqProjectMustBeRebuilt, p.getName()),
					Integer.valueOf(IMarker.SEVERITY_ERROR),
					Integer.valueOf(CategorizedProblem.CAT_BUILDPATH),
					JavaBuilder.SOURCE_ID
				}
			);
			return false;
		}
	}
	return true;
}

/*
 * Instruct the build manager that this project is involved in a cycle and
 * needs to propagate structural changes to the other projects in the cycle.
 */
void mustPropagateStructuralChanges() {
	HashSet cycleParticipants = new HashSet(3);
	javaProject.updateCycleParticipants(new ArrayList(), cycleParticipants, workspaceRoot, new HashSet(3), null);
	IPath currentPath = javaProject.getPath();
	Iterator i= cycleParticipants.iterator();
	while (i.hasNext()) {
		IPath participantPath = (IPath) i.next();
		if (participantPath != currentPath) {
			IProject project = workspaceRoot.getProject(participantPath.segment(0));
			if (hasBeenBuilt(project)) {
				if (DEBUG)
					System.out.println("Requesting another build iteration since cycle participant " + project.getName() //$NON-NLS-1$
						+ " has not yet seen some structural changes"); //$NON-NLS-1$
				needRebuild();
				return;
			}
		}
	}
}

private void printLocations(ClasspathLocation[] newLocations, ClasspathLocation[] oldLocations) {
	System.out.println("New locations:"); //$NON-NLS-1$
	for (int i = 0, length = newLocations.length; i < length; i++)
		System.out.println("    " + newLocations[i].debugPathString()); //$NON-NLS-1$
	System.out.println("Old locations:"); //$NON-NLS-1$
	for (int i = 0, length = oldLocations.length; i < length; i++)
		System.out.println("    " + oldLocations[i].debugPathString()); //$NON-NLS-1$
}

private void recordNewState(State state) {
	Object[] keyTable = binaryLocationsPerProject.keyTable;
	for (int i = 0, l = keyTable.length; i < l; i++) {
		IProject prereqProject = (IProject) keyTable[i];
		if (prereqProject != null && prereqProject != currentProject)
			state.recordStructuralDependency(prereqProject, getLastState(prereqProject));
	}

	if (DEBUG)
		System.out.println("Recording new state : " + state); //$NON-NLS-1$
	// state.dump();
	JavaModelManager.getJavaModelManager().setLastBuiltState(currentProject, state);
}

/**
 * String representation for debugging purposes
 */
public String toString() {
	return currentProject == null
		? "JavaBuilder for unknown project" //$NON-NLS-1$
		: "JavaBuilder for " + currentProject.getName(); //$NON-NLS-1$
}
}
