/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.SourceElementParser;
import org.eclipse.wst.jsdt.internal.core.builder.JavaBuilder;
import org.eclipse.wst.jsdt.internal.core.hierarchy.TypeHierarchy;
import org.eclipse.wst.jsdt.internal.core.search.AbstractSearchScope;
import org.eclipse.wst.jsdt.internal.core.search.JavaWorkspaceScope;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IndexManager;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * This class is used by <code>JavaModelManager</code> to convert
 * <code>IResourceDelta</code>s into <code>IJavaScriptElementDelta</code>s.
 * It also does some processing on the <code>JavaElement</code>s involved
 * (e.g. closing them or updating classpaths).
 * <p>
 * High level summary of what the delta processor does:
 * <ul>
 * <li>reacts to resource deltas</li>
 * <li>fires corresponding Java element deltas</li>
 * <li>deltas also contain non-Java resources changes</li>
 * <li>updates the model to reflect the Java element changes</li>
 * <li>notifies type hierarchies of the changes</li>
 * <li>triggers indexing of the changed elements</li>
 * <li>refresh external archives (delta, model update, indexing)</li>
 * <li>is thread safe (one delta processor instance per thread, see DeltaProcessingState#resourceChanged(...))</li>
 * <li>handles .classpath changes (updates package fragment roots, update project references, validate classpath (.classpath format,
 * 		resolved classpath, cycles))</li>
 * </ul>
 */
public class DeltaProcessor {

	static class OutputsInfo {
		int outputCount;
		IPath[] paths;
		int[] traverseModes;
		OutputsInfo(IPath[] paths, int[] traverseModes, int outputCount) {
			this.paths = paths;
			this.traverseModes = traverseModes;
			this.outputCount = outputCount;
		}
		public String toString() {
			if (this.paths == null) return "<none>"; //$NON-NLS-1$
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < this.outputCount; i++) {
				buffer.append("path="); //$NON-NLS-1$
				buffer.append(this.paths[i].toString());
				buffer.append("\n->traverse="); //$NON-NLS-1$
				switch (this.traverseModes[i]) {
					case BINARY:
						buffer.append("BINARY"); //$NON-NLS-1$
						break;
					case IGNORE:
						buffer.append("IGNORE"); //$NON-NLS-1$
						break;
					case SOURCE:
						buffer.append("SOURCE"); //$NON-NLS-1$
						break;
					default:
						buffer.append("<unknown>"); //$NON-NLS-1$
				}
				if (i+1 < this.outputCount) {
					buffer.append('\n');
				}
			}
			return buffer.toString();
		}
	}

	static class RootInfo {
		char[][] inclusionPatterns;
		char[][] exclusionPatterns;
		JavaProject project;
		IPath rootPath;
		int entryKind;
		IPackageFragmentRoot root;
		RootInfo(JavaProject project, IPath rootPath, char[][] inclusionPatterns, char[][] exclusionPatterns, int entryKind) {
			this.project = project;
			this.rootPath = rootPath;
			this.inclusionPatterns = inclusionPatterns;
			this.exclusionPatterns = exclusionPatterns;
			this.entryKind = entryKind;
		}
		IPackageFragmentRoot getPackageFragmentRoot(IResource resource) {
			if (this.root == null) {
				if (resource != null) {
					this.root = this.project.getPackageFragmentRoot(resource);
				} else {
					Object target = JavaModel.getTarget(ResourcesPlugin.getWorkspace().getRoot(), this.rootPath, false/*don't check existence*/);
					if (target instanceof IResource) {
						this.root = this.project.getPackageFragmentRoot((IResource)target);
					} else {
						this.root = this.project.getPackageFragmentRoot(this.rootPath.toOSString());
					}
				}
			}
			return this.root;
		}
		boolean isRootOfProject(IPath path) {
			return this.rootPath.equals(path) && this.project.getProject().getFullPath().isPrefixOf(path);
		}
		public String toString() {
			StringBuffer buffer = new StringBuffer("project="); //$NON-NLS-1$
			if (this.project == null) {
				buffer.append("null"); //$NON-NLS-1$
			} else {
				buffer.append(this.project.getElementName());
			}
			buffer.append("\npath="); //$NON-NLS-1$
			if (this.rootPath == null) {
				buffer.append("null"); //$NON-NLS-1$
			} else {
				buffer.append(this.rootPath.toString());
			}
			buffer.append("\nincluding="); //$NON-NLS-1$
			if (this.inclusionPatterns == null) {
				buffer.append("null"); //$NON-NLS-1$
			} else {
				for (int i = 0, length = this.inclusionPatterns.length; i < length; i++) {
					buffer.append(new String(this.inclusionPatterns[i]));
					if (i < length-1) {
						buffer.append("|"); //$NON-NLS-1$
					}
				}
			}
			buffer.append("\nexcluding="); //$NON-NLS-1$
			if (this.exclusionPatterns == null) {
				buffer.append("null"); //$NON-NLS-1$
			} else {
				for (int i = 0, length = this.exclusionPatterns.length; i < length; i++) {
					buffer.append(new String(this.exclusionPatterns[i]));
					if (i < length-1) {
						buffer.append("|"); //$NON-NLS-1$
					}
				}
			}
			return buffer.toString();
		}
	}

	private final static int IGNORE = 0;
	private final static int SOURCE = 1;
	private final static int BINARY = 2;

	private final static String EXTERNAL_JAR_ADDED = "external jar added"; //$NON-NLS-1$
	private final static String EXTERNAL_JAR_CHANGED = "external jar changed"; //$NON-NLS-1$
	private final static String EXTERNAL_JAR_REMOVED = "external jar removed"; //$NON-NLS-1$
	private final static String EXTERNAL_JAR_UNCHANGED = "external jar unchanged"; //$NON-NLS-1$
	private final static String INTERNAL_JAR_IGNORE = "internal jar ignore"; //$NON-NLS-1$

	private final static int NON_JAVA_RESOURCE = -1;
	public static boolean DEBUG = false;
	public static boolean VERBOSE = false;
	public static boolean PERF = false;

	public static final int DEFAULT_CHANGE_EVENT = 0; // must not collide with ElementChangedEvent event masks

	/*
	 * Answer a combination of the lastModified stamp and the size.
	 * Used for detecting external JAR changes
	 */
	public static long getTimeStamp(File file) {
		return file.lastModified() + file.length();
	}

	/*
	 * The global state of delta processing.
	 */
	private DeltaProcessingState state;

	/*
	 * The Java model manager
	 */
	JavaModelManager manager;

	/*
	 * The <code>JavaElementDelta</code> corresponding to the <code>IResourceDelta</code> being translated.
	 */
	private JavaElementDelta currentDelta;

	/* The java element that was last created (see createElement(IResource)).
	 * This is used as a stack of java elements (using getParent() to pop it, and
	 * using the various get*(...) to push it. */
	private Openable currentElement;

	/*
	 * Queue of deltas created explicily by the Java Model that
	 * have yet to be fired.
	 */
	public ArrayList javaModelDeltas= new ArrayList();

	/*
	 * Queue of reconcile deltas on working copies that have yet to be fired.
	 * This is a table form IWorkingCopy to IJavaScriptElementDelta
	 */
	public HashMap reconcileDeltas = new HashMap();

	/*
	 * Turns delta firing on/off. By default it is on.
	 */
	private boolean isFiring= true;

	/*
	 * Used to update the JavaModel for <code>IJavaScriptElementDelta</code>s.
	 */
	private final ModelUpdater modelUpdater = new ModelUpdater();

	/* A set of IJavaScriptProject whose caches need to be reset */
	public HashSet projectCachesToReset = new HashSet();

	/*
	 * A list of IJavaScriptElement used as a scope for external archives refresh during POST_CHANGE.
	 * This is null if no refresh is needed.
	 */
	private HashSet refreshedElements;

	/* A table from IJavaScriptProject to an array of IPackageFragmentRoot.
	 * This table contains the pkg fragment roots of the project that are being deleted.
	 */
	public Map oldRoots;

	/* A set of IJavaScriptProject whose package fragment roots need to be refreshed */
	private HashSet rootsToRefresh = new HashSet();

	/*
	 * Type of event that should be processed no matter what the real event type is.
	 */
	public int overridenEventType = -1;

	/*
	 * Cache SourceElementParser for the project being visited
	 */
	private SourceElementParser sourceElementParserCache;

	/*
	 * Map from IProject to ClasspathChange
	 */
	public HashMap classpathChanges = new HashMap();

	public DeltaProcessor(DeltaProcessingState state, JavaModelManager manager) {
		this.state = state;
		this.manager = manager;
	}

	public ClasspathChange addClasspathChange(IProject project, IIncludePathEntry[] oldRawClasspath, IPath oldOutputLocation, IIncludePathEntry[] oldResolvedClasspath) {
		ClasspathChange change = (ClasspathChange) this.classpathChanges.get(project);
		if (change == null) {
			change = new ClasspathChange((JavaProject) this.manager.getJavaModel().getJavaProject(project), oldRawClasspath, oldOutputLocation, oldResolvedClasspath);
			this.classpathChanges.put(project, change);
		} else {
			if (change.oldRawClasspath == null)
				change.oldRawClasspath = oldRawClasspath;
			if (change.oldOutputLocation == null)
				change.oldOutputLocation = oldOutputLocation;
			if (change.oldResolvedClasspath == null)
				change.oldResolvedClasspath = oldResolvedClasspath;
		}
		return change;
	}

	/*
	 * Adds the dependents of the given project to the list of the projects
	 * to update.
	 */
	private void addDependentProjects(IJavaScriptProject project, HashMap projectDependencies, HashSet result) {
		IJavaScriptProject[] dependents = (IJavaScriptProject[]) projectDependencies.get(project);
		if (dependents == null) return;
		for (int i = 0, length = dependents.length; i < length; i++) {
			IJavaScriptProject dependent = dependents[i];
			if (result.contains(dependent))
				continue; // no need to go further as the project is already known
			result.add(dependent);
			addDependentProjects(dependent, projectDependencies, result);
		}
	}
	/*
	 * Adds the given element to the list of elements used as a scope for external jars refresh.
	 */
	public void addForRefresh(IJavaScriptElement element) {
		if (this.refreshedElements == null) {
			this.refreshedElements = new HashSet();
		}
		this.refreshedElements.add(element);
	}
	/*
	 * Adds the given child handle to its parent's cache of children.
	 */
	private void addToParentInfo(Openable child) {
		Openable parent = (Openable) child.getParent();
		if (parent != null && parent.isOpen()) {
			try {
				JavaElementInfo info = (JavaElementInfo)parent.getElementInfo();
				info.addChild(child);
			} catch (JavaScriptModelException e) {
				// do nothing - we already checked if open
			}
		}
	}
	/*
	 * Adds the given project and its dependents to the list of the roots to refresh.
	 */
	private void addToRootsToRefreshWithDependents(IJavaScriptProject javaProject) {
		this.rootsToRefresh.add(javaProject);
		this.addDependentProjects(javaProject, this.state.projectDependencies, this.rootsToRefresh);
	}
	/*
	 * Check all external archive (referenced by given roots, projects or model) status and issue a corresponding root delta.
	 * Also triggers index updates
	 */
	public void checkExternalArchiveChanges(IJavaScriptElement[] elementsToRefresh, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor != null && monitor.isCanceled())
			throw new OperationCanceledException();
		try {
			if (monitor != null) monitor.beginTask("", 1); //$NON-NLS-1$

			for (int i = 0, length = elementsToRefresh.length; i < length; i++) {
				this.addForRefresh(elementsToRefresh[i]);
			}
			boolean hasDelta = this.createExternalArchiveDelta(monitor);
			if (monitor != null && monitor.isCanceled()) return;
			if (hasDelta){
				// force classpath marker refresh of affected projects
				JavaModel.flushExternalFileCache();

				// flush jar type cache
				JavaModelManager.getJavaModelManager().resetJarTypeCache();

				IJavaScriptElementDelta[] projectDeltas = this.currentDelta.getAffectedChildren();
				final int length = projectDeltas.length;
				final IProject[] projectsToTouch = new IProject[length];
				for (int i = 0; i < length; i++) {
					IJavaScriptElementDelta delta = projectDeltas[i];
					JavaProject javaProject = (JavaProject)delta.getElement();
					projectsToTouch[i] = javaProject.getProject();
				}

				// touch the projects to force them to be recompiled while taking the workspace lock
				// so that there is no concurrency with the Java builder
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=96575
				IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
					public void run(IProgressMonitor progressMonitor) throws CoreException {
						for (int i = 0; i < length; i++) {
							IProject project = projectsToTouch[i];

							// touch to force a build of this project
							if (JavaBuilder.DEBUG)
								System.out.println("Touching project " + project.getName() + " due to external jar file change"); //$NON-NLS-1$ //$NON-NLS-2$
							project.touch(progressMonitor);
						}
					}
				};
				try {
					ResourcesPlugin.getWorkspace().run(runnable, monitor);
				} catch (CoreException e) {
					throw new JavaScriptModelException(e);
				}

				if (this.currentDelta != null) { // if delta has not been fired while creating markers
					this.fire(this.currentDelta, DEFAULT_CHANGE_EVENT);
				}
			}
		} finally {
			this.currentDelta = null;
			if (monitor != null) monitor.done();
		}
	}
	/*
	 * Process the given delta and look for projects being added, opened, closed or
	 * with a java nature being added or removed.
	 * Note that projects being deleted are checked in deleting(IProject).
	 * In all cases, add the project's dependents to the list of projects to update
	 * so that the classpath related markers can be updated.
	 */
	private void checkProjectsBeingAddedOrRemoved(IResourceDelta delta) {
		IResource resource = delta.getResource();
		IResourceDelta[] children = null;

		switch (resource.getType()) {
			case IResource.ROOT :
				// workaround for bug 15168 circular errors not reported
				this.state.getOldJavaProjecNames(); // force list to be computed
				children = delta.getAffectedChildren();
				break;
			case IResource.PROJECT :
				// NB: No need to check project's nature as if the project is not a java project:
				//     - if the project is added or changed this is a noop for projectsBeingDeleted
				//     - if the project is closed, it has already lost its java nature
				IProject project = (IProject)resource;
				JavaProject javaProject = (JavaProject)JavaScriptCore.create(project);
				switch (delta.getKind()) {
					case IResourceDelta.ADDED :
						this.manager.batchContainerInitializations = true;

						// remember project and its dependents
						addToRootsToRefreshWithDependents(javaProject);

						// workaround for bug 15168 circular errors not reported
						if (JavaProject.hasJavaNature(project)) {
							addToParentInfo(javaProject);
							readRawClasspath(javaProject);
							// ensure project references are updated (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=121569)
							checkProjectReferenceChange(project, javaProject);
						}

						this.state.rootsAreStale = true;
						break;

					case IResourceDelta.CHANGED :
							if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
								this.manager.batchContainerInitializations = true;

								// project opened or closed: remember  project and its dependents
								addToRootsToRefreshWithDependents(javaProject);

								// workaround for bug 15168 circular errors not reported
								if (project.isOpen()) {
									if (JavaProject.hasJavaNature(project)) {
										addToParentInfo(javaProject);
										readRawClasspath(javaProject);
										// ensure project references are updated
										checkProjectReferenceChange(project, javaProject);
									}
								} else {
									try {
										javaProject.close();
									} catch (JavaScriptModelException e) {
										// java project doesn't exist: ignore
									}
									this.removeFromParentInfo(javaProject);
									this.manager.removePerProjectInfo(javaProject);
									this.manager.containerRemove(javaProject);
								}
								this.state.rootsAreStale = true;
							} else if ((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
								boolean wasJavaProject = this.state.findJavaProject(project.getName()) != null;
								boolean isJavaProject = JavaProject.hasJavaNature(project);
								if (wasJavaProject != isJavaProject) {
									this.manager.batchContainerInitializations = true;

									// java nature added or removed: remember  project and its dependents
									this.addToRootsToRefreshWithDependents(javaProject);

									// workaround for bug 15168 circular errors not reported
									if (isJavaProject) {
										this.addToParentInfo(javaProject);
										readRawClasspath(javaProject);
										// ensure project references are updated (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=172666)
										checkProjectReferenceChange(project, javaProject);
									} else {
										// remove classpath cache so that initializeRoots() will not consider the project has a classpath
										this.manager.removePerProjectInfo(javaProject);
										// remove container cache for this project
										this.manager.containerRemove(javaProject);
										// close project
										try {
											javaProject.close();
										} catch (JavaScriptModelException e) {
											// java project doesn't exist: ignore
										}
										this.removeFromParentInfo(javaProject);
									}
									this.state.rootsAreStale = true;
								} else {
									// in case the project was removed then added then changed (see bug 19799)
									if (isJavaProject) { // need nature check - 18698
										this.addToParentInfo(javaProject);
										children = delta.getAffectedChildren();
									}
								}
							} else {
								// workaround for bug 15168 circular errors not reported
								// in case the project was removed then added then changed
								if (JavaProject.hasJavaNature(project)) { // need nature check - 18698
									this.addToParentInfo(javaProject);
									children = delta.getAffectedChildren();
								}
							}
							break;

					case IResourceDelta.REMOVED :
						this.manager.batchContainerInitializations = true;

						// remove classpath cache so that initializeRoots() will not consider the project has a classpath
						this.manager.removePerProjectInfo(javaProject);
						// remove container cache for this project
						this.manager.containerRemove(javaProject);

						this.state.rootsAreStale = true;
						break;
				}

				// in all cases, refresh the external jars for this project
				addForRefresh(javaProject);

				break;
			case IResource.FOLDER :
				IFolder folder = (IFolder) resource;
				if(folder.getName().equals(JavaProject.SHARED_PROPERTIES_DIRECTORY)) {
					switch (delta.getKind()) {
					case IResourceDelta.CHANGED :
					// fall through
					case IResourceDelta.ADDED :
						children = delta.getAffectedChildren();
						break;
					}
				}
				break;
			case IResource.FILE :
				IFile file = (IFile) resource;
				/* classpath file change */
				if (file.getName().equals(JavaProject.CLASSPATH_FILENAME)) {
					this.manager.batchContainerInitializations = true;
					switch (delta.getKind()) {
						case IResourceDelta.CHANGED :
							int flags = delta.getFlags();
							if ((flags & IResourceDelta.CONTENT) == 0  // only consider content change
								&& (flags & IResourceDelta.ENCODING) == 0 // and encoding change
								&& (flags & IResourceDelta.MOVED_FROM) == 0) {// and also move and overide scenario (see http://dev.eclipse.org/bugs/show_bug.cgi?id=21420)
								break;
							}
						// fall through
						case IResourceDelta.ADDED :
							javaProject = (JavaProject)JavaScriptCore.create(file.getProject());

							// force to (re)read the .classpath file
							try {
								javaProject.getPerProjectInfo().readAndCacheClasspath(javaProject);
							} catch (JavaScriptModelException e) {
								// project doesn't exist
								return;
							}
							break;
					}
					this.state.rootsAreStale = true;
				}
				break;

		}
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				checkProjectsBeingAddedOrRemoved(children[i]);
			}
		}
	}

	private void checkProjectReferenceChange(IProject project, JavaProject javaProject) {
		ClasspathChange change = (ClasspathChange) this.classpathChanges.get(project);
		this.state.addProjectReferenceChange(javaProject, change == null ? null : change.oldResolvedClasspath);
	}

	private void readRawClasspath(JavaProject javaProject) {
		try {
			// force to (re)read the .classpath file
			javaProject.getPerProjectInfo().readAndCacheClasspath(javaProject);
		} catch (JavaScriptModelException e) {
			if (VERBOSE) {
				e.printStackTrace();
			}
		}
	}
	private void checkSourceAttachmentChange(IResourceDelta delta, IResource res) {
		IPath rootPath = (IPath)this.state.sourceAttachments.get(res.getFullPath());
		if (rootPath != null) {
			RootInfo rootInfo = this.rootInfo(rootPath, delta.getKind());
			if (rootInfo != null) {
				IJavaScriptProject projectOfRoot = rootInfo.project;
				IPackageFragmentRoot root = null;
				try {
					// close the root so that source attachement cache is flushed
					root = projectOfRoot.findPackageFragmentRoot(rootPath);
					if (root != null) {
						root.close();
					}
				} catch (JavaScriptModelException e) {
					// root doesn't exist: ignore
				}
				if (root == null) return;
				switch (delta.getKind()) {
					case IResourceDelta.ADDED:
						currentDelta().sourceAttached(root);
						break;
					case IResourceDelta.CHANGED:
						currentDelta().sourceDetached(root);
						currentDelta().sourceAttached(root);
						break;
					case IResourceDelta.REMOVED:
						currentDelta().sourceDetached(root);
						break;
				}
			}
		}
	}
	/*
	 * Closes the given element, which removes it from the cache of open elements.
	 */
	private void close(Openable element) {
		try {
			element.close();
		} catch (JavaScriptModelException e) {
			// do nothing
		}
	}
	/*
	 * Generic processing for elements with changed contents:<ul>
	 * <li>The element is closed such that any subsequent accesses will re-open
	 * the element reflecting its new structure.
	 * <li>An entry is made in the delta reporting a content change (K_CHANGE with F_CONTENT flag set).
	 * </ul>
	 * Delta argument could be null if processing an external JAR change
	 */
	private void contentChanged(Openable element) {

		boolean isPrimary = false;
		boolean isPrimaryWorkingCopy = false;
		if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT) {
			CompilationUnit cu = (CompilationUnit)element;
			isPrimary = cu.isPrimary();
			isPrimaryWorkingCopy = isPrimary && cu.isWorkingCopy();
		}
		if (isPrimaryWorkingCopy) {
			// filter out changes to primary compilation unit in working copy mode
			// just report a change to the resource (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59500)
			currentDelta().changed(element, IJavaScriptElementDelta.F_PRIMARY_RESOURCE);
		} else {
			close(element);
			int flags = IJavaScriptElementDelta.F_CONTENT;
//			if (element instanceof JarPackageFragmentRoot){
//				flags |= IJavaScriptElementDelta.F_ARCHIVE_CONTENT_CHANGED;
//				// need also to reset project cache otherwise it will be out-of-date
//				// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=162621
//				this.projectCachesToReset.add(element.getJavaScriptProject());
//			}
			if (isPrimary) {
				flags |= IJavaScriptElementDelta.F_PRIMARY_RESOURCE;
			}
			currentDelta().changed(element, flags);
		}
	}
	/*
	 * Creates the openables corresponding to this resource.
	 * Returns null if none was found.
	 */
	private Openable createElement(IResource resource, int elementType, RootInfo rootInfo) {
		if (resource == null) return null;

		IPath path = resource.getFullPath();
		IJavaScriptElement element = null;
		switch (elementType) {

			case IJavaScriptElement.JAVASCRIPT_PROJECT:

				// note that non-java resources rooted at the project level will also enter this code with
				// an elementType JAVASCRIPT_PROJECT (see #elementType(...)).
				if (resource instanceof IProject){

					this.popUntilPrefixOf(path);

					if (this.currentElement != null
						&& this.currentElement.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT
						&& ((IJavaScriptProject)this.currentElement).getProject().equals(resource)) {
						return this.currentElement;
					}
					if  (rootInfo != null && rootInfo.project.getProject().equals(resource)){
						element = rootInfo.project;
						break;
					}
					IProject proj = (IProject)resource;
					if (JavaProject.hasJavaNature(proj)) {
						element = JavaScriptCore.create(proj);
					} else {
						// java project may have been been closed or removed (look for
						// element amongst old java project s list).
						element =  this.state.findJavaProject(proj.getName());
					}
				}
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				element = rootInfo == null ? JavaScriptCore.create(resource) : rootInfo.getPackageFragmentRoot(resource);
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				if (rootInfo != null) {
					if (rootInfo.project.contains(resource)) {
						PackageFragmentRoot root = (PackageFragmentRoot) rootInfo.getPackageFragmentRoot(null);
						// create package handle
						IPath pkgPath = path.removeFirstSegments(rootInfo.rootPath.segmentCount());
						String[] pkgName = pkgPath.segments();
						element = root.getPackageFragment(pkgName);
					}
				} else {
					// find the element that encloses the resource
					this.popUntilPrefixOf(path);

					if (this.currentElement == null) {
						element = JavaScriptCore.create(resource);
					} else {
						// find the root
						PackageFragmentRoot root = this.currentElement.getPackageFragmentRoot();
						if (root == null) {
							element =  JavaScriptCore.create(resource);
						} else if (((JavaProject)root.getJavaScriptProject()).contains(resource)) {
							// create package handle
							IPath pkgPath = path.removeFirstSegments(root.getPath().segmentCount());
							String[] pkgName = pkgPath.segments();
							element = root.getPackageFragment(pkgName);
						}
					}
				}
				break;
			case IJavaScriptElement.JAVASCRIPT_UNIT:
			case IJavaScriptElement.CLASS_FILE:
				// find the element that encloses the resource
				this.popUntilPrefixOf(path);

				if (this.currentElement == null) {
					element =  rootInfo == null ? JavaScriptCore.create(resource) : JavaModelManager.create(resource, rootInfo.project);
				} else {
					// find the package
					IPackageFragment pkgFragment = null;
					switch (this.currentElement.getElementType()) {
						case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
							PackageFragmentRoot root = (PackageFragmentRoot)this.currentElement;
							IPath rootPath = root.getPath();
							IPath pkgPath = path.removeLastSegments(1);
							String[] pkgName = pkgPath.removeFirstSegments(rootPath.segmentCount()).segments();
							pkgFragment = root.getPackageFragment(pkgName);
							break;
						case IJavaScriptElement.PACKAGE_FRAGMENT:
							Openable pkg = this.currentElement;
							if (pkg.getPath().equals(path.removeLastSegments(1))) {
								pkgFragment = (IPackageFragment)pkg;
							} // else case of package x which is a prefix of x.y
							break;
						case IJavaScriptElement.JAVASCRIPT_UNIT:
						case IJavaScriptElement.CLASS_FILE:
							pkgFragment = (IPackageFragment)this.currentElement.getParent();
							break;
					}
					if (pkgFragment == null) {
						boolean isSource = rootInfo == null || rootInfo.entryKind == IIncludePathEntry.CPE_SOURCE;
						if(!isSource)
							element = JavaScriptCore.createClassFileFrom((IFile)resource);
						else 
							element =  rootInfo == null ? JavaScriptCore.create(resource) : JavaModelManager.create(resource, rootInfo.project);
					} else {
						if (elementType == IJavaScriptElement.JAVASCRIPT_UNIT) {
							// create compilation unit handle
							// fileName validation has been done in elementType(IResourceDelta, int, boolean)
							String fileName = path.lastSegment();
							element = pkgFragment.getJavaScriptUnit(fileName);
						} else {
							// create class file handle
							// fileName validation has been done in elementType(IResourceDelta, int, boolean)
							String fileName = path.lastSegment();
							element = pkgFragment.getClassFile(fileName);
						}
					}
				}
				break;
		}
		if (element == null) return null;
		this.currentElement = (Openable)element;
		return this.currentElement;
	}
	/*
	 * Check if external archives have changed and create the corresponding deltas.
	 * Returns whether at least on delta was created.
	 */
	private boolean createExternalArchiveDelta(IProgressMonitor monitor) {

		if (this.refreshedElements == null) return false;

		HashMap externalArchivesStatus = new HashMap();
		boolean hasDelta = false;

		// find JARs to refresh
		HashSet archivePathsToRefresh = new HashSet();
		Iterator iterator = this.refreshedElements.iterator();
		this.refreshedElements = null; // null out early to avoid concurrent modification exception (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63534)
		while (iterator.hasNext()) {
			IJavaScriptElement element = (IJavaScriptElement)iterator.next();
			switch(element.getElementType()){
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
					archivePathsToRefresh.add(element.getPath());
					break;
				case IJavaScriptElement.JAVASCRIPT_PROJECT :
					JavaProject javaProject = (JavaProject) element;
					if (!JavaProject.hasJavaNature(javaProject.getProject())) {
						// project is not accessible or has lost its Java nature
						break;
					}
					IIncludePathEntry[] classpath;
					try {
						classpath = javaProject.getResolvedClasspath();
						for (int j = 0, cpLength = classpath.length; j < cpLength; j++){
							if (classpath[j].getEntryKind() == IIncludePathEntry.CPE_LIBRARY){
								archivePathsToRefresh.add(classpath[j].getPath());
							}
						}
					} catch (JavaScriptModelException e) {
						// project doesn't exist -> ignore
					}
					break;
				case IJavaScriptElement.JAVASCRIPT_MODEL :
					Iterator projectNames = this.state.getOldJavaProjecNames().iterator();
					while (projectNames.hasNext()) {
						String projectName = (String) projectNames.next();
						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
						if (!JavaProject.hasJavaNature(project)) {
							// project is not accessible or has lost its Java nature
							continue;
						}
						javaProject = (JavaProject) JavaScriptCore.create(project);
						try {
							classpath = javaProject.getResolvedClasspath();
						} catch (JavaScriptModelException e2) {
							// project doesn't exist -> ignore
							continue;
						}
						for (int k = 0, cpLength = classpath.length; k < cpLength; k++){
							if (classpath[k].getEntryKind() == IIncludePathEntry.CPE_LIBRARY){
								archivePathsToRefresh.add(classpath[k].getPath());
							}
						}
					}
					break;
			}
		}

		// perform refresh
		Iterator projectNames = this.state.getOldJavaProjecNames().iterator();
		IWorkspaceRoot wksRoot = ResourcesPlugin.getWorkspace().getRoot();
		while (projectNames.hasNext()) {

			if (monitor != null && monitor.isCanceled()) break;

			String projectName = (String) projectNames.next();
			IProject project = wksRoot.getProject(projectName);
			if (!JavaProject.hasJavaNature(project)) {
				// project is not accessible or has lost its Java nature
				continue;
			}
			JavaProject javaProject = (JavaProject) JavaScriptCore.create(project);
			IIncludePathEntry[] entries;
			try {
				entries = javaProject.getResolvedClasspath();
			} catch (JavaScriptModelException e1) {
				// project does not exist -> ignore
				continue;
			}
			for (int j = 0; j < entries.length; j++){
				if (entries[j].getEntryKind() == IIncludePathEntry.CPE_LIBRARY) {

					IPath entryPath = entries[j].getPath();

					if (!archivePathsToRefresh.contains(entryPath)) continue; // not supposed to be refreshed

					String status = (String)externalArchivesStatus.get(entryPath);
					if (status == null){

						// compute shared status
						Object targetLibrary = JavaModel.getTarget(wksRoot, entryPath, true);

						if (targetLibrary == null){ // missing JAR
							if (this.state.getExternalLibTimeStamps().remove(entryPath) != null){
								externalArchivesStatus.put(entryPath, EXTERNAL_JAR_REMOVED);
								// the jar was physically removed: remove the index
								this.manager.indexManager.removeIndex(entryPath);
							}

						} else if (targetLibrary instanceof File){ // external JAR

							File externalFile = (File)targetLibrary;

							// check timestamp to figure if JAR has changed in some way
							Long oldTimestamp =(Long) this.state.getExternalLibTimeStamps().get(entryPath);
							long newTimeStamp = getTimeStamp(externalFile);
							if (oldTimestamp != null){

								if (newTimeStamp == 0){ // file doesn't exist
									externalArchivesStatus.put(entryPath, EXTERNAL_JAR_REMOVED);
									this.state.getExternalLibTimeStamps().remove(entryPath);
									// remove the index
									this.manager.indexManager.removeIndex(entryPath);

								} else if (oldTimestamp.longValue() != newTimeStamp){
									externalArchivesStatus.put(entryPath, EXTERNAL_JAR_CHANGED);
									this.state.getExternalLibTimeStamps().put(entryPath, Long.valueOf(newTimeStamp));
									// first remove the index so that it is forced to be re-indexed
									this.manager.indexManager.removeIndex(entryPath);
									// then index the jar
									this.manager.indexManager.indexLibrary(entries[j], project.getProject());
								} else {
									externalArchivesStatus.put(entryPath, EXTERNAL_JAR_UNCHANGED);
								}
							} else {
								if (newTimeStamp == 0){ // jar still doesn't exist
									externalArchivesStatus.put(entryPath, EXTERNAL_JAR_UNCHANGED);
								} else {
									externalArchivesStatus.put(entryPath, EXTERNAL_JAR_ADDED);
									this.state.getExternalLibTimeStamps().put(entryPath, Long.valueOf(newTimeStamp));
									// index the new jar
									this.manager.indexManager.indexLibrary(entries[j], project.getProject());
								}
							}
						} else { // internal JAR
							externalArchivesStatus.put(entryPath, INTERNAL_JAR_IGNORE);
						}
					}
					// according to computed status, generate a delta
					status = (String)externalArchivesStatus.get(entryPath);
					if (status != null){
						if (status == EXTERNAL_JAR_ADDED){
							PackageFragmentRoot root = (PackageFragmentRoot) javaProject.getPackageFragmentRoot(entryPath.toString());
							if (VERBOSE){
								System.out.println("- External JAR ADDED, affecting root: "+root.getElementName()); //$NON-NLS-1$
							}
							elementAdded(root, null, null);
							this.state.addClasspathValidation(javaProject); // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=185733
							hasDelta = true;
						} else if (status == EXTERNAL_JAR_CHANGED) {
							PackageFragmentRoot root = (PackageFragmentRoot) javaProject.getPackageFragmentRoot(entryPath.toString());
							if (VERBOSE){
								System.out.println("- External JAR CHANGED, affecting root: "+root.getElementName()); //$NON-NLS-1$
							}
							contentChanged(root);
							hasDelta = true;
						} else if (status == EXTERNAL_JAR_REMOVED) {
							PackageFragmentRoot root = (PackageFragmentRoot) javaProject.getPackageFragmentRoot(entryPath.toString());
							if (VERBOSE){
								System.out.println("- External JAR REMOVED, affecting root: "+root.getElementName()); //$NON-NLS-1$
							}
							elementRemoved(root, null, null);
							this.state.addClasspathValidation(javaProject); // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=185733
							hasDelta = true;
						}
					}
				}
			}
		}
		return hasDelta;
	}
	private JavaElementDelta currentDelta() {
		if (this.currentDelta == null) {
			this.currentDelta = new JavaElementDelta(this.manager.getJavaModel());
		}
		return this.currentDelta;
	}
	/*
	 * Note that the project is about to be deleted.
	 */
	private void deleting(IProject project) {

		try {
			// discard indexing jobs that belong to this project so that the project can be
			// deleted without interferences from the index manager
			this.manager.indexManager.discardJobs(project.getName());

			JavaProject javaProject = (JavaProject)JavaScriptCore.create(project);

			// remember roots of this project
			if (this.oldRoots == null) {
				this.oldRoots = new HashMap();
			}
			if (javaProject.isOpen()) {
				this.oldRoots.put(javaProject, javaProject.getPackageFragmentRoots());
			} else {
				// compute roots without opening project
				this.oldRoots.put(
					javaProject,
					javaProject.computePackageFragmentRoots(
						javaProject.getResolvedClasspath(),
						false,
						null /*no reverse map*/));
			}

			javaProject.close();

			// workaround for bug 15168 circular errors not reported
			this.state.getOldJavaProjecNames(); // foce list to be computed

			this.removeFromParentInfo(javaProject);

			// remove preferences from per project info
			this.manager.resetProjectPreferences(javaProject);
		} catch (JavaScriptModelException e) {
			// java project doesn't exist: ignore
		}
	}
	/*
	 * Processing for an element that has been added:<ul>
	 * <li>If the element is a project, do nothing, and do not process
	 * children, as when a project is created it does not yet have any
	 * natures - specifically a java nature.
	 * <li>If the elemet is not a project, process it as added (see
	 * <code>basicElementAdded</code>.
	 * </ul>
	 * Delta argument could be null if processing an external JAR change
	 */
	private void elementAdded(Openable element, IResourceDelta delta, RootInfo rootInfo) {
		int elementType = element.getElementType();

		if (elementType == IJavaScriptElement.JAVASCRIPT_PROJECT) {
			// project add is handled by JavaProject.configure() because
			// when a project is created, it does not yet have a java nature
			if (delta != null && JavaProject.hasJavaNature((IProject)delta.getResource())) {
				addToParentInfo(element);
				if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
					Openable movedFromElement = (Openable)element.getJavaScriptModel().getJavaScriptProject(delta.getMovedFromPath().lastSegment());
					currentDelta().movedTo(element, movedFromElement);
				} else {
					// Force the project to be closed as it might have been opened
					// before the resource modification came in and it might have a new child
					// For example, in an IWorkspaceRunnable:
					// 1. create a Java project P (where P=src)
					// 2. open project P
					// 3. add folder f in P's pkg fragment root
					// When the resource delta comes in, only the addition of P is notified,
					// but the pkg fragment root of project P is already opened, thus its children are not recomputed
					// and it appears to contain only the default package.
					close(element);

					currentDelta().added(element);
				}
				this.state.updateRoots(element.getPath(), delta, this);

				// refresh pkg fragment roots and caches of the project (and its dependents)
				this.rootsToRefresh.add(element);
				this.projectCachesToReset.add(element);
			}
		} else {
			if (delta == null || (delta.getFlags() & IResourceDelta.MOVED_FROM) == 0) {
				// regular element addition
				if (isPrimaryWorkingCopy(element, elementType) ) {
					// filter out changes to primary compilation unit in working copy mode
					// just report a change to the resource (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59500)
					currentDelta().changed(element, IJavaScriptElementDelta.F_PRIMARY_RESOURCE);
				} else {
					addToParentInfo(element);

					// Force the element to be closed as it might have been opened
					// before the resource modification came in and it might have a new child
					// For example, in an IWorkspaceRunnable:
					// 1. create a package fragment p using a java model operation
					// 2. open package p
					// 3. add file X.js in folder p
					// When the resource delta comes in, only the addition of p is notified,
					// but the package p is already opened, thus its children are not recomputed
					// and it appears empty.
					close(element);

					currentDelta().added(element);
				}
			} else {
				// element is moved
				addToParentInfo(element);
				close(element);

				IPath movedFromPath = delta.getMovedFromPath();
				IResource res = delta.getResource();
				IResource movedFromRes;
				if (res instanceof IFile) {
					movedFromRes = res.getWorkspace().getRoot().getFile(movedFromPath);
				} else {
					movedFromRes = res.getWorkspace().getRoot().getFolder(movedFromPath);
				}

				// find the element type of the moved from element
				RootInfo movedFromInfo = this.enclosingRootInfo(movedFromPath, IResourceDelta.REMOVED);
				int movedFromType =
					this.elementType(
						movedFromRes,
						IResourceDelta.REMOVED,
						element.getParent().getElementType(),
						movedFromInfo);

				// reset current element as it might be inside a nested root (popUntilPrefixOf() may use the outer root)
				this.currentElement = null;

				// create the moved from element
				Openable movedFromElement =
					elementType != IJavaScriptElement.JAVASCRIPT_PROJECT && movedFromType == IJavaScriptElement.JAVASCRIPT_PROJECT ?
						null : // outside classpath
						this.createElement(movedFromRes, movedFromType, movedFromInfo);
				if (movedFromElement == null) {
					// moved from outside classpath
					currentDelta().added(element);
				} else {
					currentDelta().movedTo(element, movedFromElement);
				}
			}

			switch (elementType) {
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
					// when a root is added, and is on the classpath, the project must be updated
					JavaProject project = (JavaProject) element.getJavaScriptProject();

					// refresh pkg fragment roots and caches of the project (and its dependents)
					this.rootsToRefresh.add(project);
					this.projectCachesToReset.add(project);

					break;
				case IJavaScriptElement.PACKAGE_FRAGMENT :
					// reset project's package fragment cache
					project = (JavaProject) element.getJavaScriptProject();
					this.projectCachesToReset.add(project);

					break;
			}
		}
	}
	/*
	 * Generic processing for a removed element:<ul>
	 * <li>Close the element, removing its structure from the cache
	 * <li>Remove the element from its parent's cache of children
	 * <li>Add a REMOVED entry in the delta
	 * </ul>
	 * Delta argument could be null if processing an external JAR change
	 */
	private void elementRemoved(Openable element, IResourceDelta delta, RootInfo rootInfo) {

		int elementType = element.getElementType();
		if (delta == null || (delta.getFlags() & IResourceDelta.MOVED_TO) == 0) {
			// regular element removal
			if (isPrimaryWorkingCopy(element, elementType) ) {
				// filter out changes to primary compilation unit in working copy mode
				// just report a change to the resource (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59500)
				currentDelta().changed(element, IJavaScriptElementDelta.F_PRIMARY_RESOURCE);
			} else {
				close(element);
				removeFromParentInfo(element);
				currentDelta().removed(element);
			}
		} else {
			// element is moved
			close(element);
			removeFromParentInfo(element);
			IPath movedToPath = delta.getMovedToPath();
			IResource res = delta.getResource();
			IResource movedToRes;
			switch (res.getType()) {
				case IResource.PROJECT:
					movedToRes = res.getWorkspace().getRoot().getProject(movedToPath.lastSegment());
					break;
				case IResource.FOLDER:
					movedToRes = res.getWorkspace().getRoot().getFolder(movedToPath);
					break;
				case IResource.FILE:
					movedToRes = res.getWorkspace().getRoot().getFile(movedToPath);
					break;
				default:
					return;
			}

			// find the element type of the moved from element
			RootInfo movedToInfo = this.enclosingRootInfo(movedToPath, IResourceDelta.ADDED);
			int movedToType =
				this.elementType(
					movedToRes,
					IResourceDelta.ADDED,
					element.getParent().getElementType(),
					movedToInfo);

			// reset current element as it might be inside a nested root (popUntilPrefixOf() may use the outer root)
			this.currentElement = null;

			// create the moved To element
			Openable movedToElement =
				elementType != IJavaScriptElement.JAVASCRIPT_PROJECT && movedToType == IJavaScriptElement.JAVASCRIPT_PROJECT ?
					null : // outside classpath
					this.createElement(movedToRes, movedToType, movedToInfo);
			if (movedToElement == null) {
				// moved outside classpath
				currentDelta().removed(element);
			} else {
				currentDelta().movedFrom(element, movedToElement);
			}
		}

		switch (elementType) {
			case IJavaScriptElement.JAVASCRIPT_MODEL :
				this.manager.indexManager.reset();
				break;
			case IJavaScriptElement.JAVASCRIPT_PROJECT :
				this.state.updateRoots(element.getPath(), delta, this);

				// refresh pkg fragment roots and caches of the project (and its dependents)
				this.rootsToRefresh.add(element);
				this.projectCachesToReset.add(element);

				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
				JavaProject project = (JavaProject) element.getJavaScriptProject();

				// refresh pkg fragment roots and caches of the project (and its dependents)
				this.rootsToRefresh.add(project);
				this.projectCachesToReset.add(project);

				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT :
				// reset package fragment cache
				project = (JavaProject) element.getJavaScriptProject();
				this.projectCachesToReset.add(project);

				break;
		}
	}
	/*
	 * Returns the type of the java element the given delta matches to.
	 * Returns NON_JAVA_RESOURCE if unknown (e.g. a non-java resource or excluded .js file)
	 */
	private int elementType(IResource res, int kind, int parentType, RootInfo rootInfo) {
		switch (parentType) {
			case IJavaScriptElement.JAVASCRIPT_MODEL:
				// case of a movedTo or movedFrom project (other cases are handled in processResourceDelta(...)
				return IJavaScriptElement.JAVASCRIPT_PROJECT;

			case NON_JAVA_RESOURCE:
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				if (rootInfo == null) {
					rootInfo = this.enclosingRootInfo(res.getFullPath(), kind);
				}
				if (rootInfo != null && rootInfo.isRootOfProject(res.getFullPath())) {
					return IJavaScriptElement.PACKAGE_FRAGMENT_ROOT;
				}
				// not yet in a package fragment root or root of another project
				// or package fragment to be included (see below)
				// -> let it go through

			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				if (rootInfo == null) {
					rootInfo = this.enclosingRootInfo(res.getFullPath(), kind);
				}
				if (rootInfo == null) {
					return NON_JAVA_RESOURCE;
				}
				if (Util.isExcluded(res, rootInfo.inclusionPatterns, rootInfo.exclusionPatterns)) {
					return NON_JAVA_RESOURCE;
				}
				if (res.getType() == IResource.FOLDER) {
					if (parentType == NON_JAVA_RESOURCE && !Util.isExcluded(res.getParent(), rootInfo.inclusionPatterns, rootInfo.exclusionPatterns)) {
						// parent is a non-Java resource because it doesn't have a valid package name (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=130982)
						return NON_JAVA_RESOURCE;
					}
					String sourceLevel = rootInfo.project == null ? null : rootInfo.project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
					String complianceLevel = rootInfo.project == null ? null : rootInfo.project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
					if (Util.isValidFolderNameForPackage(res.getName(), sourceLevel, complianceLevel)) {
						return IJavaScriptElement.PACKAGE_FRAGMENT;
					}
					return NON_JAVA_RESOURCE;
				}
				String fileName = res.getName();
				String sourceLevel = rootInfo.project == null ? null : rootInfo.project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
				String complianceLevel = rootInfo.project == null ? null : rootInfo.project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
				boolean isSource = rootInfo.entryKind == IIncludePathEntry.CPE_SOURCE;
				if ((Util.isValidClassFileName(fileName, sourceLevel, complianceLevel) || Util.isMetadataFileName(fileName)) && !isSource) {
					return IJavaScriptElement.CLASS_FILE;
				} else if (Util.isValidCompilationUnitName(fileName, sourceLevel, complianceLevel)) {
					return IJavaScriptElement.JAVASCRIPT_UNIT;
				} else if ((rootInfo = this.rootInfo(res.getFullPath(), kind)) != null
						&& rootInfo.project.getProject().getFullPath().isPrefixOf(res.getFullPath()) /*ensure root is a root of its project (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=185310) */) {
					// case of proj=src=bin and resource is a jar file on the classpath
					return IJavaScriptElement.PACKAGE_FRAGMENT_ROOT;
				} else {
					return NON_JAVA_RESOURCE;
				}

			default:
				return NON_JAVA_RESOURCE;
		}
	}
	/*
	 * Flushes all deltas without firing them.
	 */
	public void flush() {
		this.javaModelDeltas = new ArrayList();
	}

	private SourceElementParser getSourceElementParser(Openable element) {
		if (this.sourceElementParserCache == null)
			this.sourceElementParserCache = this.manager.indexManager.getSourceElementParser(element.getJavaScriptProject(), null/*requestor will be set by indexer*/);
		return this.sourceElementParserCache;
	}
	/*
	 * Finds the root info this path is included in.
	 * Returns null if not found.
	 */
	private RootInfo enclosingRootInfo(IPath path, int kind) {
		while (path != null && path.segmentCount() > 0) {
			RootInfo rootInfo =  this.rootInfo(path, kind);
			if (rootInfo != null) return rootInfo;
			path = path.removeLastSegments(1);
		}
		return null;
	}
	/*
	 * Fire Java Model delta, flushing them after the fact after post_change notification.
	 * If the firing mode has been turned off, this has no effect.
	 */
	public void fire(IJavaScriptElementDelta customDelta, int eventType) {
		if (!this.isFiring) return;

		if (DEBUG) {
			System.out.println("-----------------------------------------------------------------------------------------------------------------------");//$NON-NLS-1$
		}

		IJavaScriptElementDelta deltaToNotify;
		if (customDelta == null){
			deltaToNotify = this.mergeDeltas(this.javaModelDeltas);
		} else {
			deltaToNotify = customDelta;
		}

		// Refresh internal scopes
		if (deltaToNotify != null) {
			Iterator scopes = this.manager.searchScopes.keySet().iterator();
			while (scopes.hasNext()) {
				AbstractSearchScope scope = (AbstractSearchScope)scopes.next();
				scope.processDelta(deltaToNotify);
			}
			JavaWorkspaceScope workspaceScope = this.manager.workspaceScope;
			if (workspaceScope != null)
				workspaceScope.processDelta(deltaToNotify);
		}

		// Notification

		// Important: if any listener reacts to notification by updating the listeners list or mask, these lists will
		// be duplicated, so it is necessary to remember original lists in a variable (since field values may change under us)
		IElementChangedListener[] listeners;
		int[] listenerMask;
		int listenerCount;
		synchronized (this.state) {
			listeners = this.state.elementChangedListeners;
			listenerMask = this.state.elementChangedListenerMasks;
			listenerCount = this.state.elementChangedListenerCount;
		}

		switch (eventType) {
			case DEFAULT_CHANGE_EVENT:
				firePostChangeDelta(deltaToNotify, listeners, listenerMask, listenerCount);
				fireReconcileDelta(listeners, listenerMask, listenerCount);
				break;
			case ElementChangedEvent.POST_CHANGE:
				firePostChangeDelta(deltaToNotify, listeners, listenerMask, listenerCount);
				fireReconcileDelta(listeners, listenerMask, listenerCount);
				break;
		}
	}

	private void firePostChangeDelta(
		IJavaScriptElementDelta deltaToNotify,
		IElementChangedListener[] listeners,
		int[] listenerMask,
		int listenerCount) {

		// post change deltas
		if (DEBUG){
			System.out.println("FIRING POST_CHANGE Delta ["+Thread.currentThread()+"]:"); //$NON-NLS-1$//$NON-NLS-2$
			System.out.println(deltaToNotify == null ? "<NONE>" : deltaToNotify.toString()); //$NON-NLS-1$
		}
		if (deltaToNotify != null) {
			// flush now so as to keep listener reactions to post their own deltas for subsequent iteration
			this.flush();

			// mark the operation stack has not modifying resources since resource deltas are being fired
			JavaModelOperation.setAttribute(JavaModelOperation.HAS_MODIFIED_RESOURCE_ATTR, null);

			notifyListeners(deltaToNotify, ElementChangedEvent.POST_CHANGE, listeners, listenerMask, listenerCount);
		}
	}
	private void fireReconcileDelta(
		IElementChangedListener[] listeners,
		int[] listenerMask,
		int listenerCount) {


		IJavaScriptElementDelta deltaToNotify = mergeDeltas(this.reconcileDeltas.values());
		if (DEBUG){
			System.out.println("FIRING POST_RECONCILE Delta ["+Thread.currentThread()+"]:"); //$NON-NLS-1$//$NON-NLS-2$
			System.out.println(deltaToNotify == null ? "<NONE>" : deltaToNotify.toString()); //$NON-NLS-1$
		}
		if (deltaToNotify != null) {
			// flush now so as to keep listener reactions to post their own deltas for subsequent iteration
			this.reconcileDeltas = new HashMap();

			notifyListeners(deltaToNotify, ElementChangedEvent.POST_RECONCILE, listeners, listenerMask, listenerCount);
		}
	}
	/*
	 * Returns whether a given delta contains some information relevant to the JavaModel,
	 * in particular it will not consider SYNC or MARKER only deltas.
	 */
	private boolean isAffectedBy(IResourceDelta rootDelta){
		//if (rootDelta == null) System.out.println("NULL DELTA");
		//long start = System.currentTimeMillis();
		if (rootDelta != null) {
			// use local exception to quickly escape from delta traversal
			class FoundRelevantDeltaException extends RuntimeException {
				private static final long serialVersionUID = 7137113252936111022L; // backward compatible
				// only the class name is used (to differenciate from other RuntimeExceptions)
			}
			try {
				rootDelta.accept(new IResourceDeltaVisitor() {
					public boolean visit(IResourceDelta delta) /* throws CoreException */ {
						switch (delta.getKind()){
							case IResourceDelta.ADDED :
							case IResourceDelta.REMOVED :
								throw new FoundRelevantDeltaException();
							case IResourceDelta.CHANGED :
								// if any flag is set but SYNC or MARKER, this delta should be considered
								if (delta.getAffectedChildren().length == 0 // only check leaf delta nodes
										&& (delta.getFlags() & ~(IResourceDelta.SYNC | IResourceDelta.MARKERS)) != 0) {
									throw new FoundRelevantDeltaException();
								}
						}
						return true;
					}
				});
			} catch(FoundRelevantDeltaException e) {
				//System.out.println("RELEVANT DELTA detected in: "+ (System.currentTimeMillis() - start));
				return true;
			} catch(CoreException e) { // ignore delta if not able to traverse
			}
		}
		//System.out.println("IGNORE SYNC DELTA took: "+ (System.currentTimeMillis() - start));
		return false;
	}
	/*
	 * Returns whether the given element is a primary compilation unit in working copy mode.
	 */
	private boolean isPrimaryWorkingCopy(IJavaScriptElement element, int elementType) {
		if (elementType == IJavaScriptElement.JAVASCRIPT_UNIT) {
			CompilationUnit cu = (CompilationUnit)element;
			return cu.isPrimary() && cu.isWorkingCopy();
		}
		return false;
	}
	/*
	 * Returns whether the given resource is in one of the given output folders and if
	 * it is filtered out from this output folder.
	 */
	private boolean isResFilteredFromOutput(RootInfo rootInfo, OutputsInfo info, IResource res, int elementType) {
		if (info != null) {
			JavaProject javaProject = null;
			String sourceLevel = null;
			String complianceLevel = null;
			IPath resPath = res.getFullPath();
			for (int i = 0;  i < info.outputCount; i++) {
				if (info.paths[i].isPrefixOf(resPath)) {
					if (info.traverseModes[i] != IGNORE) {
						// case of bin=src
						if (info.traverseModes[i] == SOURCE && elementType == IJavaScriptElement.CLASS_FILE) {
							return true;
						}
						// case of .class file under project and no source folder
						// proj=bin
						if (elementType == IJavaScriptElement.JAVASCRIPT_PROJECT && res instanceof IFile) {
							if (sourceLevel == null) {
								// Get java project to use its source and compliance levels
								javaProject = rootInfo == null ?
									(JavaProject)this.createElement(res.getProject(), IJavaScriptElement.JAVASCRIPT_PROJECT, null) :
									rootInfo.project;
								if (javaProject != null) {
									sourceLevel = javaProject.getOption(JavaScriptCore.COMPILER_SOURCE, true);
									complianceLevel = javaProject.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
								}
							}
							if (Util.isValidClassFileName(res.getName(), sourceLevel, complianceLevel)) {
								return true;
							}
						}
					} else {
						return true;
					}
				}
			}
		}
		return false;
	}
	/*
	 * Merges all awaiting deltas.
	 */
	private IJavaScriptElementDelta mergeDeltas(Collection deltas) {
		if (deltas.size() == 0) return null;
		if (deltas.size() == 1) return (IJavaScriptElementDelta)deltas.iterator().next();

		if (VERBOSE) {
			System.out.println("MERGING " + deltas.size() + " DELTAS ["+Thread.currentThread()+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		Iterator iterator = deltas.iterator();
		JavaElementDelta rootDelta = new JavaElementDelta(this.manager.javaModel);
		boolean insertedTree = false;
		while (iterator.hasNext()) {
			JavaElementDelta delta = (JavaElementDelta)iterator.next();
			if (VERBOSE) {
				System.out.println(delta.toString());
			}
			IJavaScriptElement element = delta.getElement();
			if (this.manager.javaModel.equals(element)) {
				IJavaScriptElementDelta[] children = delta.getAffectedChildren();
				for (int j = 0; j < children.length; j++) {
					JavaElementDelta projectDelta = (JavaElementDelta) children[j];
					rootDelta.insertDeltaTree(projectDelta.getElement(), projectDelta);
					insertedTree = true;
				}
				IResourceDelta[] resourceDeltas = delta.getResourceDeltas();
				if (resourceDeltas != null) {
					for (int i = 0, length = resourceDeltas.length; i < length; i++) {
						rootDelta.addResourceDelta(resourceDeltas[i]);
						insertedTree = true;
					}
				}
			} else {
				rootDelta.insertDeltaTree(element, delta);
				insertedTree = true;
			}
		}
		if (insertedTree) return rootDelta;
		return null;
	}
	private void notifyListeners(IJavaScriptElementDelta deltaToNotify, int eventType, IElementChangedListener[] listeners, int[] listenerMask, int listenerCount) {
		final ElementChangedEvent extraEvent = new ElementChangedEvent(deltaToNotify, eventType);
		for (int i= 0; i < listenerCount; i++) {
			if ((listenerMask[i] & eventType) != 0){
				final IElementChangedListener listener = listeners[i];
				long start = -1;
				if (VERBOSE) {
					System.out.print("Listener #" + (i+1) + "=" + listener.toString());//$NON-NLS-1$//$NON-NLS-2$
					start = System.currentTimeMillis();
				}
				// wrap callbacks with Safe runnable for subsequent listeners to be called when some are causing grief
				SafeRunner.run(new ISafeRunnable() {
					public void handleException(Throwable exception) {
						Util.log(exception, "Exception occurred in listener of Java element change notification"); //$NON-NLS-1$
					}
					public void run() throws Exception {
						PerformanceStats stats = null;
						if(PERF) {
							stats = PerformanceStats.getStats(JavaModelManager.DELTA_LISTENER_PERF, listener);
							stats.startRun();
						}
						listener.elementChanged(extraEvent);
						if(PERF) {
							stats.endRun();
						}
					}
				});
				if (VERBOSE) {
					System.out.println(" -> " + (System.currentTimeMillis()-start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}
	private void notifyTypeHierarchies(IElementChangedListener[] listeners, int listenerCount) {
		for (int i= 0; i < listenerCount; i++) {
			final IElementChangedListener listener = listeners[i];
			if (!(listener instanceof TypeHierarchy)) continue;

			// wrap callbacks with Safe runnable for subsequent listeners to be called when some are causing grief
			SafeRunner.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					Util.log(exception, "Exception occurred in listener of Java element change notification"); //$NON-NLS-1$
				}
				public void run() throws Exception {
					TypeHierarchy typeHierarchy = (TypeHierarchy)listener;
					if (typeHierarchy.hasFineGrainChanges()) {
						// case of changes in primary working copies
						typeHierarchy.needsRefresh = true;
						typeHierarchy.fireChange();
					}
				}
			});
		}
	}
	/*
	 * Generic processing for elements with changed contents:<ul>
	 * <li>The element is closed such that any subsequent accesses will re-open
	 * the element reflecting its new structure.
	 * <li>An entry is made in the delta reporting a content change (K_CHANGE with F_CONTENT flag set).
	 * </ul>
	 */
	private void nonJavaResourcesChanged(Openable element, IResourceDelta delta)
		throws JavaScriptModelException {

		// reset non-java resources if element was open
		if (element.isOpen()) {
			JavaElementInfo info = (JavaElementInfo)element.getElementInfo();
			switch (element.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_MODEL :
					((JavaModelInfo) info).nonJavaResources = null;
					currentDelta().addResourceDelta(delta);
					return;
				case IJavaScriptElement.JAVASCRIPT_PROJECT :
					((JavaProjectElementInfo) info).setNonJavaResources(null);

					// if a package fragment root is the project, clear it too
					JavaProject project = (JavaProject) element;
					PackageFragmentRoot projectRoot =
						(PackageFragmentRoot) project.getPackageFragmentRoot(project.getProject());
					if (projectRoot.isOpen()) {
						((PackageFragmentRootInfo) projectRoot.getElementInfo()).setNonJavaResources(
							null);
					}
					break;
				case IJavaScriptElement.PACKAGE_FRAGMENT :
					 ((PackageFragmentInfo) info).setNonJavaResources(null);
					break;
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
					 ((PackageFragmentRootInfo) info).setNonJavaResources(null);
			}
		}

		JavaElementDelta current = currentDelta();
		JavaElementDelta elementDelta = current.find(element);
		if (elementDelta == null) {
			// don't use find after creating the delta as it can be null (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63434)
			elementDelta = current.changed(element, IJavaScriptElementDelta.F_CONTENT);
		}
		elementDelta.addResourceDelta(delta);
	}
	/*
	 * Returns the other root infos for the given path. Look in the old other roots table if kind is REMOVED.
	 */
	private ArrayList otherRootsInfo(IPath path, int kind) {
		if (kind == IResourceDelta.REMOVED) {
			return (ArrayList)this.state.oldOtherRoots.get(path);
		}
		return (ArrayList)this.state.otherRoots.get(path);
	}

	private OutputsInfo outputsInfo(RootInfo rootInfo, IResource res) {
		try {
			JavaProject proj =
				rootInfo == null ?
					(JavaProject)this.createElement(res.getProject(), IJavaScriptElement.JAVASCRIPT_PROJECT, null) :
					rootInfo.project;
			if (proj != null) {
				IPath projectOutput = proj.getOutputLocation();
				int traverseMode = IGNORE;
				if (proj.getProject().getFullPath().equals(projectOutput)){ // case of proj==bin==src
					return new OutputsInfo(new IPath[] {projectOutput}, new int[] {SOURCE}, 1);
				}
				IIncludePathEntry[] classpath = proj.getResolvedClasspath();
				IPath[] outputs = new IPath[classpath.length+1];
				int[] traverseModes = new int[classpath.length+1];
				int outputCount = 1;
				outputs[0] = projectOutput;
				traverseModes[0] = traverseMode;
				for (int i = 0, length = classpath.length; i < length; i++) {
					IIncludePathEntry entry = classpath[i];
					IPath entryPath = entry.getPath();

					// check case of src==bin
					if (entryPath.equals(projectOutput)) {
						traverseModes[0] = (entry.getEntryKind() == IIncludePathEntry.CPE_SOURCE) ? SOURCE : BINARY;
					}
				}
				return new OutputsInfo(outputs, traverseModes, outputCount);
			}
		} catch (JavaScriptModelException e) {
			// java project doesn't exist: ignore
		}
		return null;
	}
	private void popUntilPrefixOf(IPath path) {
		while (this.currentElement != null) {
			IPath currentElementPath = null;
			if (this.currentElement instanceof IPackageFragmentRoot) {
				currentElementPath = ((IPackageFragmentRoot)this.currentElement).getPath();
			} else {
				IResource currentElementResource = this.currentElement.getResource();
				if (currentElementResource != null) {
					currentElementPath = currentElementResource.getFullPath();
				}
			}
			if (currentElementPath != null) {
				if (this.currentElement instanceof IPackageFragment
					&& ((IPackageFragment) this.currentElement).isDefaultPackage()
					&& currentElementPath.segmentCount() != path.segmentCount()-1) {
						// default package and path is not a direct child
						this.currentElement = (Openable)this.currentElement.getParent();
				}
				if (currentElementPath.isPrefixOf(path)) {
					return;
				}
			}
			this.currentElement = (Openable)this.currentElement.getParent();
		}
	}
	/*
	 * Converts a <code>IResourceDelta</code> rooted in a <code>Workspace</code> into
	 * the corresponding set of <code>IJavaScriptElementDelta</code>, rooted in the
	 * relevant <code>JavaModel</code>s.
	 */
	private IJavaScriptElementDelta processResourceDelta(IResourceDelta changes) {

		try {
			IJavaScriptModel model = this.manager.getJavaModel();
			if (!model.isOpen()) {
				// force opening of java model so that java element delta are reported
				try {
					model.open(null);
				} catch (JavaScriptModelException e) {
					if (VERBOSE) {
						e.printStackTrace();
					}
					return null;
				}
			}
			this.state.initializeRoots();
			this.currentElement = null;

			// get the workspace delta, and start processing there.
			IResourceDelta[] deltas = changes.getAffectedChildren();
			for (int i = 0; i < deltas.length; i++) {
				IResourceDelta delta = deltas[i];
				IResource res = delta.getResource();

				// find out the element type
				RootInfo rootInfo = null;
				int elementType;
				IProject proj = (IProject)res;
				boolean wasJavaProject = this.state.findJavaProject(proj.getName()) != null;
				boolean isJavaProject = JavaProject.hasJavaNature(proj);
				if (!wasJavaProject && !isJavaProject) {
					elementType = NON_JAVA_RESOURCE;
				} else {
					rootInfo = this.enclosingRootInfo(res.getFullPath(), delta.getKind());
					if (rootInfo != null && rootInfo.isRootOfProject(res.getFullPath())) {
						elementType = IJavaScriptElement.PACKAGE_FRAGMENT_ROOT;
					} else {
						elementType = IJavaScriptElement.JAVASCRIPT_PROJECT;
					}
				}

				// traverse delta
				this.traverseDelta(delta, elementType, rootInfo, null);

				if (elementType == NON_JAVA_RESOURCE
						|| (wasJavaProject != isJavaProject && (delta.getKind()) == IResourceDelta.CHANGED)) { // project has changed nature (description or open/closed)
					try {
						// add child as non java resource
						nonJavaResourcesChanged((JavaModel)model, delta);
					} catch (JavaScriptModelException e) {
						// java model could not be opened
					}
				}

			}
			refreshPackageFragmentRoots();
			resetProjectCaches();

			return this.currentDelta;
		} finally {
			this.currentDelta = null;
			this.rootsToRefresh.clear();
			this.projectCachesToReset.clear();
		}
	}
	/*
	 * Traverse the set of projects which have changed namespace, and reset their
	 * caches and their dependents
	 */
	public void resetProjectCaches() {
		if (this.projectCachesToReset.size() == 0)
			return;

		JavaModelManager.getJavaModelManager().resetJarTypeCache();

		Iterator iterator = this.projectCachesToReset.iterator();
		HashMap projectDepencies = this.state.projectDependencies;
		HashSet affectedDependents = new HashSet();
		while (iterator.hasNext()) {
			JavaProject project = (JavaProject)iterator.next();
			project.resetCaches();
			addDependentProjects(project, projectDepencies, affectedDependents);
		}
		// reset caches of dependent projects
		iterator = affectedDependents.iterator();
		while (iterator.hasNext()) {
			JavaProject project = (JavaProject) iterator.next();
			project.resetCaches();
		}
	}
	/*
	 * Refresh package fragment roots of projects that were affected
	 */
	private void refreshPackageFragmentRoots() {
		Iterator iterator = this.rootsToRefresh.iterator();
		while (iterator.hasNext()) {
			JavaProject project = (JavaProject)iterator.next();
			project.updatePackageFragmentRoots();
		}
	}
	/*
	 * Registers the given delta with this delta processor.
	 */
	public void registerJavaModelDelta(IJavaScriptElementDelta delta) {
		this.javaModelDeltas.add(delta);
	}
	/*
	 * Removes the given element from its parents cache of children. If the
	 * element does not have a parent, or the parent is not currently open,
	 * this has no effect.
	 */
	private void removeFromParentInfo(Openable child) {

		Openable parent = (Openable) child.getParent();
		if (parent != null && parent.isOpen()) {
			try {
				JavaElementInfo info = (JavaElementInfo)parent.getElementInfo();
				info.removeChild(child);
			} catch (JavaScriptModelException e) {
				// do nothing - we already checked if open
			}
		}
	}
	/*
	 * Notification that some resource changes have happened
	 * on the platform, and that the Java Model should update any required
	 * internal structures such that its elements remain consistent.
	 * Translates <code>IResourceDeltas</code> into <code>IJavaElementDeltas</code>.
	 *
	 * @see IResourceDelta
	 * @see IResource
	 */
	public void resourceChanged(IResourceChangeEvent event) {

		int eventType = this.overridenEventType == -1 ? event.getType() : this.overridenEventType;
		IResource resource = event.getResource();
		IResourceDelta delta = event.getDelta();

		switch(eventType){
			case IResourceChangeEvent.PRE_DELETE :
				try {
					if(resource.getType() == IResource.PROJECT
						&& ((IProject) resource).hasNature(JavaScriptCore.NATURE_ID)) {

						deleting((IProject)resource);
					}
				} catch(CoreException e){
					// project doesn't exist or is not open: ignore
				}
				return;

			case IResourceChangeEvent.POST_CHANGE :
				if (isAffectedBy(delta)) { // avoid populating for SYNC or MARKER deltas
					try {
						try {
							stopDeltas();
							checkProjectsBeingAddedOrRemoved(delta);

							// generate classpath change deltas
							if (this.classpathChanges.size() > 0) {
								boolean hasDelta = this.currentDelta != null;
								JavaElementDelta javaDelta = currentDelta();
								Iterator changes = this.classpathChanges.values().iterator();
								while (changes.hasNext()) {
									ClasspathChange change = (ClasspathChange) changes.next();
									int result = change.generateDelta(javaDelta);
									if ((result & ClasspathChange.HAS_DELTA) != 0) {
										hasDelta = true;
										change.requestIndexing();
										this.state.addClasspathValidation(change.project);
									}
									if ((result & ClasspathChange.HAS_PROJECT_CHANGE) != 0) {
										this.state.addProjectReferenceChange(change.project, change.oldResolvedClasspath);
									}
								}
								this.classpathChanges.clear();
								if (!hasDelta)
									this.currentDelta = null;
							}

							// generate external archive change deltas
							if (this.refreshedElements != null) {
								createExternalArchiveDelta(null);
							}

							// generate Java deltas from resource changes
							IJavaScriptElementDelta translatedDelta = processResourceDelta(delta);
							if (translatedDelta != null) {
								registerJavaModelDelta(translatedDelta);
							}
						} finally {
							this.sourceElementParserCache = null; // don't hold onto parser longer than necessary
							startDeltas();
						}
						IElementChangedListener[] listeners;
						int listenerCount;
						synchronized (this.state) {
							listeners = this.state.elementChangedListeners;
							listenerCount = this.state.elementChangedListenerCount;
						}
						notifyTypeHierarchies(listeners, listenerCount);
						fire(null, ElementChangedEvent.POST_CHANGE);
					} finally {
						// workaround for bug 15168 circular errors not reported
						this.state.resetOldJavaProjectNames();
						this.oldRoots = null;
					}
				}
				return;

			case IResourceChangeEvent.PRE_BUILD :
				if(!isAffectedBy(delta))
					return; // avoid populating for SYNC or MARKER deltas

				// create classpath markers if necessary
				boolean needCycleValidation = validateClasspaths(delta);
				ClasspathValidation[] validations = this.state.removeClasspathValidations();
				if (validations != null) {
					for (int i = 0, length = validations.length; i < length; i++) {
						ClasspathValidation validation = validations[i];
						validation.validate();
					}
				}

				// update project references if necessary
			    ProjectReferenceChange[] projectRefChanges = this.state.removeProjectReferenceChanges();
				if (projectRefChanges != null) {
				    for (int i = 0, length = projectRefChanges.length; i < length; i++) {
				        try {
					        projectRefChanges[i].updateProjectReferencesIfNecessary();
				        } catch(JavaScriptModelException e) {
				            // project doesn't exist any longer, continue with next one
				        }
				    }
				}

				if (needCycleValidation || projectRefChanges != null) {
					// update all cycle markers since the project references changes may have affected cycles
					try {
						JavaProject.validateCycles(null);
					} catch (JavaScriptModelException e) {
						// a project no longer exists
					}
				}

				JavaModel.flushExternalFileCache();
				JavaBuilder.buildStarting();

				// does not fire any deltas
				return;

			case IResourceChangeEvent.POST_BUILD :
				JavaBuilder.buildFinished();
				return;
		}
	}

	/*
	 * Returns the root info for the given path. Look in the old roots table if kind is REMOVED.
	 */
	private RootInfo rootInfo(IPath path, int kind) {
		if (kind == IResourceDelta.REMOVED) {
			return (RootInfo)this.state.oldRoots.get(path);
		}
		return (RootInfo)this.state.roots.get(path);
	}
	/*
	 * Turns the firing mode to on. That is, deltas that are/have been
	 * registered will be fired.
	 */
	private void startDeltas() {
		this.isFiring= true;
	}
	/*
	 * Turns the firing mode to off. That is, deltas that are/have been
	 * registered will not be fired until deltas are started again.
	 */
	private void stopDeltas() {
		this.isFiring= false;
	}
	/*
	 * Converts an <code>IResourceDelta</code> and its children into
	 * the corresponding <code>IJavaScriptElementDelta</code>s.
	 */
	private void traverseDelta(
		IResourceDelta delta,
		int elementType,
		RootInfo rootInfo,
		OutputsInfo outputsInfo) {

		IResource res = delta.getResource();

		// set stack of elements
		if (this.currentElement == null && rootInfo != null) {
			this.currentElement = rootInfo.project;
		}

		// process current delta
		boolean processChildren = true;
		if (res instanceof IProject) {
			// reset source element parser cache
			this.sourceElementParserCache = null;

			processChildren =
				this.updateCurrentDeltaAndIndex(
					delta,
					elementType == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT ?
						IJavaScriptElement.JAVASCRIPT_PROJECT : // case of prj=src
						elementType,
					rootInfo);
		} else if (rootInfo != null) {
			processChildren = this.updateCurrentDeltaAndIndex(delta, elementType, rootInfo);
		} else {
			// not yet inside a package fragment root
			processChildren = true;
		}

		// get the project's output locations and traverse mode
		if (outputsInfo == null) outputsInfo = this.outputsInfo(rootInfo, res);

		// process children if needed
		if (processChildren) {
			IResourceDelta[] children = delta.getAffectedChildren();
			boolean oneChildOnClasspath = false;
			int length = children.length;
			IResourceDelta[] orphanChildren = null;
			Openable parent = null;
			boolean isValidParent = true;
			for (int i = 0; i < length; i++) {
				IResourceDelta child = children[i];
				IResource childRes = child.getResource();

				// check source attachment change
				this.checkSourceAttachmentChange(child, childRes);

				// find out whether the child is a package fragment root of the current project
				IPath childPath = childRes.getFullPath();
				int childKind = child.getKind();
				RootInfo childRootInfo = this.rootInfo(childPath, childKind);
				if (childRootInfo != null && !childRootInfo.isRootOfProject(childPath)) {
					// package fragment root of another project (dealt with later)
					childRootInfo = null;
				}

				// compute child type
				int childType =
					this.elementType(
						childRes,
						childKind,
						elementType,
						rootInfo == null ? childRootInfo : rootInfo
					);

				// is childRes in the output folder and is it filtered out ?
				boolean isResFilteredFromOutput = false; //this.isResFilteredFromOutput(rootInfo, outputsInfo, childRes, childType);

				boolean isNestedRoot = rootInfo != null && childRootInfo != null;
				if (!isResFilteredFromOutput
						&& !isNestedRoot) { // do not treat as non-java rsc if nested root

					this.traverseDelta(child, childType, rootInfo == null ? childRootInfo : rootInfo, outputsInfo); // traverse delta for child in the same project

					if (childType == NON_JAVA_RESOURCE) {
						if (rootInfo != null) { // if inside a package fragment root
							if (!isValidParent) continue;
							if (parent == null) {
								// find the parent of the non-java resource to attach to
								if (this.currentElement == null
										|| !rootInfo.project.equals(this.currentElement.getJavaScriptProject())) { // note if currentElement is the IJavaScriptModel, getJavaProject() is null
									// force the currentProject to be used
									this.currentElement = rootInfo.project;
								}
								if (elementType == IJavaScriptElement.JAVASCRIPT_PROJECT
									|| (elementType == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT
										&& res instanceof IProject)) {
									// NB: attach non-java resource to project (not to its package fragment root)
									parent = rootInfo.project;
								} else {
									parent = this.createElement(res, elementType, rootInfo);
								}
								if (parent == null) {
									isValidParent = false;
									continue;
								}
							}
							// add child as non java resource
							try {
								nonJavaResourcesChanged(parent, child);
							} catch (JavaScriptModelException e) {
								// ignore
							}
						} else {
							// the non-java resource (or its parent folder) will be attached to the java project
							if (orphanChildren == null) orphanChildren = new IResourceDelta[length];
							orphanChildren[i] = child;
						}
					} else {
						oneChildOnClasspath = true;
					}
				} else {
					oneChildOnClasspath = true; // to avoid reporting child delta as non-java resource delta
				}

				// if child is a nested root
				// or if it is not a package fragment root of the current project
				// but it is a package fragment root of another project, traverse delta too
				if (isNestedRoot
						|| (childRootInfo == null && (childRootInfo = this.rootInfo(childPath, childKind)) != null)) {
					this.traverseDelta(child, IJavaScriptElement.PACKAGE_FRAGMENT_ROOT, childRootInfo, null); // binary output of childRootInfo.project cannot be this root
				}

				// if the child is a package fragment root of one or several other projects
				ArrayList rootList;
				if ((rootList = this.otherRootsInfo(childPath, childKind)) != null) {
					Iterator iterator = rootList.iterator();
					while (iterator.hasNext()) {
						childRootInfo = (RootInfo) iterator.next();
						this.traverseDelta(child, IJavaScriptElement.PACKAGE_FRAGMENT_ROOT, childRootInfo, null); // binary output of childRootInfo.project cannot be this root
					}
				}
			}
			if (orphanChildren != null
					&& (oneChildOnClasspath // orphan children are siblings of a package fragment root
						|| res instanceof IProject)) { // non-java resource directly under a project

				// attach orphan children
				IProject rscProject = res.getProject();
				JavaProject adoptiveProject = (JavaProject)JavaScriptCore.create(rscProject);
				if (adoptiveProject != null
						&& JavaProject.hasJavaNature(rscProject)) { // delta iff Java project (18698)
					for (int i = 0; i < length; i++) {
						if (orphanChildren[i] != null) {
							try {
								nonJavaResourcesChanged(adoptiveProject, orphanChildren[i]);
							} catch (JavaScriptModelException e) {
								// ignore
							}
						}
					}
				}
			} // else resource delta will be added by parent
		} // else resource delta will be added by parent
	}

	private void validateClasspaths(IResourceDelta delta, HashSet affectedProjects) {
		IResource resource = delta.getResource();
		boolean processChildren = false;
		switch (resource.getType()) {
			case IResource.ROOT :
				if (delta.getKind() == IResourceDelta.CHANGED) {
					processChildren = true;
				}
				break;
			case IResource.PROJECT :
				IProject project = (IProject)resource;
				int kind = delta.getKind();
				boolean isJavaProject = JavaProject.hasJavaNature(project);
				switch (kind) {
					case IResourceDelta.ADDED:
						processChildren = isJavaProject;
						affectedProjects.add(project.getFullPath());
						break;
					case IResourceDelta.CHANGED:
						processChildren = isJavaProject;
						if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
							// project opened or closed
							if (isJavaProject) {
								JavaProject javaProject = (JavaProject)JavaScriptCore.create(project);
								this.state.addClasspathValidation(javaProject); // in case .classpath got modified while closed
							}
							affectedProjects.add(project.getFullPath());
						} else if ((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
							boolean wasJavaProject = this.state.findJavaProject(project.getName()) != null;
							if (wasJavaProject  != isJavaProject) {
								// project gained or lost Java nature
								JavaProject javaProject = (JavaProject)JavaScriptCore.create(project);
								this.state.addClasspathValidation(javaProject); // add/remove classpath markers
								affectedProjects.add(project.getFullPath());
							}
						}
						break;
					case IResourceDelta.REMOVED:
						affectedProjects.add(project.getFullPath());
						break;
				}
				break;
			case IResource.FILE :
				/* check classpath or prefs files change */
				IFile file = (IFile) resource;
				String fileName = file.getName();
				if (fileName.equals(JavaProject.CLASSPATH_FILENAME)) {
					JavaProject javaProject = (JavaProject)JavaScriptCore.create(file.getProject());
					this.state.addClasspathValidation(javaProject);
					affectedProjects.add(file.getProject().getFullPath());
				}
				break;
		}
		if (processChildren) {
			IResourceDelta[] children = delta.getAffectedChildren();
			for (int i = 0; i < children.length; i++) {
				validateClasspaths(children[i], affectedProjects);
			}
		}
	}

	/*
	 * Validate the classpaths of the projects affected by the given delta.
	 * Create markers if necessary.
	 * Returns whether cycle markers should be recomputed.
	 */
	private boolean validateClasspaths(IResourceDelta delta) {
		HashSet affectedProjects = new HashSet(5);
		validateClasspaths(delta, affectedProjects);
		boolean needCycleValidation = false;

		// validate classpaths of affected projects (dependent projects
		// or projects that reference a library in one of the projects that have changed)
		if (!affectedProjects.isEmpty()) {
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IProject[] projects = workspaceRoot.getProjects();
			int length = projects.length;
			for (int i = 0; i < length; i++){
				IProject project = projects[i];
				/* do not update classpath for closed projects
				 * 
				 * if class path is updated for a closed project then the correct classpath entries will
				 * be overwritten by incorrect ones because the containers will not be able to inspect
				 * the resources of closed projects */
				if(project.isOpen()) {
					JavaProject javaProject = (JavaProject)JavaScriptCore.create(project);
					try {
						IPath projectPath = project.getFullPath();
						IIncludePathEntry[] classpath = javaProject.getResolvedClasspath(); // allowed to reuse model cache
						for (int j = 0, cpLength = classpath.length; j < cpLength; j++) {
							IIncludePathEntry entry = classpath[j];
							switch (entry.getEntryKind()) {
								case IIncludePathEntry.CPE_PROJECT:
									if (affectedProjects.contains(entry.getPath())) {
										this.state.addClasspathValidation(javaProject);
										needCycleValidation = true;
									}
									break;
								case IIncludePathEntry.CPE_LIBRARY:
									IPath entryPath = entry.getPath();
									IPath libProjectPath = entryPath.removeLastSegments(entryPath.segmentCount()-1);
									if (!libProjectPath.equals(projectPath) // if library contained in another project
											&& affectedProjects.contains(libProjectPath)) {
										this.state.addClasspathValidation(javaProject);
									}
									break;
							}
						}
					} catch(JavaScriptModelException e) {
							// project no longer exists
					}
				}
			}
		}
		return needCycleValidation;
	}

	/*
	 * Update the current delta (ie. add/remove/change the given element) and update the correponding index.
	 * Returns whether the children of the given delta must be processed.
	 * @throws a JavaScriptModelException if the delta doesn't correspond to a java element of the given type.
	 */
	public boolean updateCurrentDeltaAndIndex(IResourceDelta delta, int elementType, RootInfo rootInfo) {
		Openable element;
		switch (delta.getKind()) {
			case IResourceDelta.ADDED :
				IResource deltaRes = delta.getResource();
				element = createElement(deltaRes, elementType, rootInfo);
				if (element == null) {
					// resource might be containing shared roots (see bug 19058)
					this.state.updateRoots(deltaRes.getFullPath(), delta, this);
					return rootInfo != null && rootInfo.inclusionPatterns != null;
				}
				updateIndex(element, delta);
				elementAdded(element, delta, rootInfo);
				if (elementType == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
					this.state.addClasspathValidation(rootInfo.project);
				return elementType == IJavaScriptElement.PACKAGE_FRAGMENT;
			case IResourceDelta.REMOVED :
				deltaRes = delta.getResource();
				element = createElement(deltaRes, elementType, rootInfo);
				if (element == null) {
					// resource might be containing shared roots (see bug 19058)
					this.state.updateRoots(deltaRes.getFullPath(), delta, this);
					return rootInfo != null && rootInfo.inclusionPatterns != null;
				}
				updateIndex(element, delta);
				elementRemoved(element, delta, rootInfo);
				if (elementType == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
					this.state.addClasspathValidation(rootInfo.project);

				if (deltaRes.getType() == IResource.PROJECT){
					// reset the corresponding project built state, since cannot reuse if added back
					if (JavaBuilder.DEBUG)
						System.out.println("Clearing last state for removed project : " + deltaRes); //$NON-NLS-1$
					this.manager.setLastBuiltState((IProject)deltaRes, null /*no state*/);

					// clean up previous session containers (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=89850)
					this.manager.previousSessionContainers.remove(element);
				}
				return elementType == IJavaScriptElement.PACKAGE_FRAGMENT;
			case IResourceDelta.CHANGED :
				int flags = delta.getFlags();
				if ((flags & IResourceDelta.CONTENT) != 0 || (flags & IResourceDelta.ENCODING) != 0) {
					// content or encoding has changed
					element = createElement(delta.getResource(), elementType, rootInfo);
					if (element == null) return false;
					updateIndex(element, delta);
					contentChanged(element);
				} else if (elementType == IJavaScriptElement.JAVASCRIPT_PROJECT) {
					if ((flags & IResourceDelta.OPEN) != 0) {
						// project has been opened or closed
						IProject res = (IProject)delta.getResource();
						element = createElement(res, elementType, rootInfo);
						if (element == null) {
							// resource might be containing shared roots (see bug 19058)
							this.state.updateRoots(res.getFullPath(), delta, this);
							return false;
						}
						if (res.isOpen()) {
							if (JavaProject.hasJavaNature(res)) {
								addToParentInfo(element);
								currentDelta().opened(element);
								this.state.updateRoots(element.getPath(), delta, this);

								// refresh pkg fragment roots and caches of the project (and its dependents)
								this.rootsToRefresh.add(element);
								this.projectCachesToReset.add(element);

								this.manager.indexManager.indexAll(res);
							}
						} else {
							boolean wasJavaProject = this.state.findJavaProject(res.getName()) != null;
							if (wasJavaProject) {
								close(element);
								removeFromParentInfo(element);
								currentDelta().closed(element);
								this.manager.indexManager.discardJobs(element.getElementName());
								this.manager.indexManager.removeIndexFamily(res.getFullPath());
							}
						}
						return false; // when a project is open/closed don't process children
					}
					if ((flags & IResourceDelta.DESCRIPTION) != 0) {
						IProject res = (IProject)delta.getResource();
						boolean wasJavaProject = this.state.findJavaProject(res.getName()) != null;
						boolean isJavaProject = JavaProject.hasJavaNature(res);
						if (wasJavaProject != isJavaProject) {
							// project's nature has been added or removed
							element = this.createElement(res, elementType, rootInfo);
							if (element == null) return false; // note its resources are still visible as roots to other projects
							if (isJavaProject) {
								elementAdded(element, delta, rootInfo);
								this.manager.indexManager.indexAll(res);
							} else {
								elementRemoved(element, delta, rootInfo);
								this.manager.indexManager.discardJobs(element.getElementName());
								this.manager.indexManager.removeIndexFamily(res.getFullPath());
								// reset the corresponding project built state, since cannot reuse if added back
								if (JavaBuilder.DEBUG)
									System.out.println("Clearing last state for project loosing Java nature: " + res); //$NON-NLS-1$
								this.manager.setLastBuiltState(res, null /*no state*/);
							}
							return false; // when a project's nature is added/removed don't process children
						}
					}
				}
				return true;
		}
		return true;
	}
	private void updateIndex(Openable element, IResourceDelta delta) {

		IndexManager indexManager = this.manager.indexManager;
		if (indexManager == null)
			return;

		switch (element.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_PROJECT :
				switch (delta.getKind()) {
					case IResourceDelta.ADDED :
						indexManager.indexAll(element.getJavaScriptProject().getProject());
						break;
					case IResourceDelta.REMOVED :
						indexManager.removeIndexFamily(element.getJavaScriptProject().getProject().getFullPath());
						// NB: Discarding index jobs belonging to this project was done during PRE_DELETE
						break;
					// NB: Update of index if project is opened, closed, or its java nature is added or removed
					//     is done in updateCurrentDeltaAndIndex
				}
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
				if (element instanceof LibraryFragmentRoot) {
					LibraryFragmentRoot root = (LibraryFragmentRoot)element;
					// index jar file only once (if the root is in its declaring project)
					IPath jarPath = root.getPath();
					switch (delta.getKind()) {
						case IResourceDelta.ADDED:
							// index the new jar
							indexManager.indexLibrary((LibraryFragmentRoot)element, root.getJavaScriptProject().getProject());
							break;
						case IResourceDelta.CHANGED:
							// first remove the index so that it is forced to be re-indexed
							indexManager.removeIndex(jarPath);
							// then index the jar
							indexManager.indexLibrary((LibraryFragmentRoot)element, root.getJavaScriptProject().getProject());
							break;
						case IResourceDelta.REMOVED:
							// the jar was physically removed: remove the index
							indexManager.discardJobs(jarPath.toString());
							indexManager.removeIndex(jarPath);
							break;
					}
					break;
				}
				int kind = delta.getKind();
				if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
					PackageFragmentRoot root = (PackageFragmentRoot)element;
					this.updateRootIndex(root, CharOperation.NO_STRINGS, delta);
					break;
				}
				// don't break as packages of the package fragment root can be indexed below
			case IJavaScriptElement.PACKAGE_FRAGMENT :
				switch (delta.getKind()) {
					case IResourceDelta.ADDED:
					case IResourceDelta.REMOVED:
						IPackageFragment pkg = null;
						if (element instanceof IPackageFragmentRoot) {
							PackageFragmentRoot root = (PackageFragmentRoot)element;
							pkg = root.getPackageFragment(CharOperation.NO_STRINGS);
						} else {
							pkg = (IPackageFragment)element;
						}
						RootInfo rootInfo = rootInfo(pkg.getParent().getPath(), delta.getKind());
						boolean isSource =
							rootInfo == null // if null, defaults to source
							|| rootInfo.entryKind == IIncludePathEntry.CPE_SOURCE;
						IResourceDelta[] children = delta.getAffectedChildren();
						for (int i = 0, length = children.length; i < length; i++) {
							IResourceDelta child = children[i];
							IResource resource = child.getResource();
							// TODO (philippe) Why do this? Every child is added anyway as the delta is walked
							if (resource instanceof IFile) {
								String name = resource.getName();
								if (isSource) {
									if (org.eclipse.wst.jsdt.internal.core.util.Util.isJavaLikeFileName(name)) {
										Openable cu = (Openable)pkg.getJavaScriptUnit(name);
										this.updateIndex(cu, child);
									}
								} else if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(name)) {
									Openable classFile = (Openable)pkg.getClassFile(name);
									this.updateIndex(classFile, child);
								}
							}
						}
						break;
				}
				break;
			case IJavaScriptElement.CLASS_FILE :
				IFile file = (IFile) delta.getResource();
				IJavaScriptProject project = element.getJavaScriptProject();
				IPath binaryFolderPath = element.getPackageFragmentRoot().getPath();
			
				switch (delta.getKind()) {
					case IResourceDelta.CHANGED :
						// no need to index if the content has not changed
						int flags = delta.getFlags();
						if ((flags & IResourceDelta.CONTENT) == 0 && (flags & IResourceDelta.ENCODING) == 0)
							break;
					case IResourceDelta.ADDED :
						indexManager.addBinary(file, binaryFolderPath);
						break;
					case IResourceDelta.REMOVED :
						String containerRelativePath = Util.relativePath(file.getFullPath(), binaryFolderPath.segmentCount());
						indexManager.remove(containerRelativePath, binaryFolderPath);
						break;
				}
				break;
			case IJavaScriptElement.JAVASCRIPT_UNIT :
				file = (IFile) delta.getResource();
				switch (delta.getKind()) {
					case IResourceDelta.CHANGED :
						// no need to index if the content has not changed
						int flags = delta.getFlags();
						if ((flags & IResourceDelta.CONTENT) == 0 && (flags & IResourceDelta.ENCODING) == 0)
							break;
					case IResourceDelta.ADDED :
						indexManager.addSource(file, file.getProject().getFullPath(), getSourceElementParser(element));
						// Clean file from secondary types cache but do not update indexing secondary type cache as it will be updated through indexing itself
						this.manager.secondaryTypesRemoving(file, false);
						break;
					case IResourceDelta.REMOVED :
						indexManager.remove(Util.relativePath(file.getFullPath(), 1/*remove project segment*/), file.getProject().getFullPath());
						// Clean file from secondary types cache and update indexing secondary type cache as indexing cannot remove secondary types from cache
						this.manager.secondaryTypesRemoving(file, true);
						break;
				}
		}
	}
	/*
	 * Update Java Model given some delta
	 */
	public void updateJavaModel(IJavaScriptElementDelta customDelta) {

		if (customDelta == null){
			for (int i = 0, length = this.javaModelDeltas.size(); i < length; i++){
				IJavaScriptElementDelta delta = (IJavaScriptElementDelta)this.javaModelDeltas.get(i);
				this.modelUpdater.processJavaDelta(delta);
			}
		} else {
			this.modelUpdater.processJavaDelta(customDelta);
		}
	}
	/*
	 * Updates the index of the given root (assuming it's an addition or a removal).
	 * This is done recusively, pkg being the current package.
	 */
	private void updateRootIndex(PackageFragmentRoot root, String[] pkgName, IResourceDelta delta) {
		Openable pkg = root.getPackageFragment(pkgName);
		this.updateIndex(pkg, delta);
		IResourceDelta[] children = delta.getAffectedChildren();
		for (int i = 0, length = children.length; i < length; i++) {
			IResourceDelta child = children[i];
			IResource resource = child.getResource();
			if (resource instanceof IFolder) {
				String[] subpkgName = Util.arrayConcat(pkgName, resource.getName());
				this.updateRootIndex(root, subpkgName, child);
			}
		}
	}
}
