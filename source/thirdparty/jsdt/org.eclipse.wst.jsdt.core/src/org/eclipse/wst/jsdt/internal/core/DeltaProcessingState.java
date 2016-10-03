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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Keep the global states used during Java element delta processing.
 */
public class DeltaProcessingState implements IResourceChangeListener {

	/*
	 * Collection of listeners for Java element deltas
	 */
	public IElementChangedListener[] elementChangedListeners = new IElementChangedListener[5];
	public int[] elementChangedListenerMasks = new int[5];
	public int elementChangedListenerCount = 0;

	/*
	 * Collection of pre Java resource change listeners
	 */
	public IResourceChangeListener[] preResourceChangeListeners = new IResourceChangeListener[1];
	public int[] preResourceChangeEventMasks = new int[1];
	public int preResourceChangeListenerCount = 0;

	/*
	 * The delta processor for the current thread.
	 */
	private ThreadLocal deltaProcessors = new ThreadLocal();

	/* A table from IPath (from a classpath entry) to DeltaProcessor.RootInfo */
	public HashMap roots = new HashMap();

	/* A table from IPath (from a classpath entry) to ArrayList of DeltaProcessor.RootInfo
	 * Used when an IPath corresponds to more than one root */
	public HashMap otherRoots = new HashMap();

	/* A table from IPath (from a classpath entry) to DeltaProcessor.RootInfo
	 * from the last time the delta processor was invoked. */
	public HashMap oldRoots = new HashMap();

	/* A table from IPath (from a classpath entry) to ArrayList of DeltaProcessor.RootInfo
	 * from the last time the delta processor was invoked.
	 * Used when an IPath corresponds to more than one root */
	public HashMap oldOtherRoots = new HashMap();

	/* A table from IPath (a source attachment path from a classpath entry) to IPath (a root path) */
	public HashMap sourceAttachments = new HashMap();

	/* A table from IJavaScriptProject to IJavaScriptProject[] (the list of direct dependent of the key) */
	public HashMap projectDependencies = new HashMap();

	/* Whether the roots tables should be recomputed */
	public boolean rootsAreStale = true;

	/* Threads that are currently running initializeRoots() */
	private Set initializingThreads = Collections.synchronizedSet(new HashSet());

	/* A table from file system absoulte path (String) to timestamp (Long) */
	public Hashtable externalTimeStamps;

	/* A table from JavaProject to ClasspathValidation */
	private HashMap classpathValidations = new HashMap();

	/* A table from JavaProject to ProjectReferenceChange */
	private HashMap projectReferenceChanges= new HashMap();

	/**
	 * Workaround for bug 15168 circular errors not reported
	 * This is a cache of the projects before any project addition/deletion has started.
	 */
	private HashSet javaProjectNamesCache;

	/*
	 * Need to clone defensively the listener information, in case some listener is reacting to some notification iteration by adding/changing/removing
	 * any of the other (for example, if it deregisters itself).
	 */
	public synchronized void addElementChangedListener(IElementChangedListener listener, int eventMask) {
		for (int i = 0; i < this.elementChangedListenerCount; i++){
			if (this.elementChangedListeners[i] == listener){

				// only clone the masks, since we could be in the middle of notifications and one listener decide to change
				// any event mask of another listeners (yet not notified).
				int cloneLength = this.elementChangedListenerMasks.length;
				System.arraycopy(this.elementChangedListenerMasks, 0, this.elementChangedListenerMasks = new int[cloneLength], 0, cloneLength);
				this.elementChangedListenerMasks[i] |= eventMask; // could be different
				return;
			}
		}
		// may need to grow, no need to clone, since iterators will have cached original arrays and max boundary and we only add to the end.
		int length;
		if ((length = this.elementChangedListeners.length) == this.elementChangedListenerCount){
			System.arraycopy(this.elementChangedListeners, 0, this.elementChangedListeners = new IElementChangedListener[length*2], 0, length);
			System.arraycopy(this.elementChangedListenerMasks, 0, this.elementChangedListenerMasks = new int[length*2], 0, length);
		}
		this.elementChangedListeners[this.elementChangedListenerCount] = listener;
		this.elementChangedListenerMasks[this.elementChangedListenerCount] = eventMask;
		this.elementChangedListenerCount++;
	}

	public synchronized void addPreResourceChangedListener(IResourceChangeListener listener, int eventMask) {
		for (int i = 0; i < this.preResourceChangeListenerCount; i++){
			if (this.preResourceChangeListeners[i] == listener) {
				this.preResourceChangeEventMasks[i] |= eventMask;
				return;
			}
		}
		// may need to grow, no need to clone, since iterators will have cached original arrays and max boundary and we only add to the end.
		int length;
		if ((length = this.preResourceChangeListeners.length) == this.preResourceChangeListenerCount) {
			System.arraycopy(this.preResourceChangeListeners, 0, this.preResourceChangeListeners = new IResourceChangeListener[length*2], 0, length);
			System.arraycopy(this.preResourceChangeEventMasks, 0, this.preResourceChangeEventMasks = new int[length*2], 0, length);
		}
		this.preResourceChangeListeners[this.preResourceChangeListenerCount] = listener;
		this.preResourceChangeEventMasks[this.preResourceChangeListenerCount] = eventMask;
		this.preResourceChangeListenerCount++;
	}

	public DeltaProcessor getDeltaProcessor() {
		DeltaProcessor deltaProcessor = (DeltaProcessor)this.deltaProcessors.get();
		if (deltaProcessor != null) return deltaProcessor;
		deltaProcessor = new DeltaProcessor(this, JavaModelManager.getJavaModelManager());
		this.deltaProcessors.set(deltaProcessor);
		return deltaProcessor;
	}

	public synchronized ClasspathValidation addClasspathValidation(JavaProject project) {
		ClasspathValidation validation = (ClasspathValidation) this.classpathValidations.get(project);
		if (validation == null) {
			validation = new ClasspathValidation(project);
			this.classpathValidations.put(project, validation);
	    }
		return validation;
	}

	public synchronized void addProjectReferenceChange(JavaProject project, IIncludePathEntry[] oldResolvedClasspath) {
		ProjectReferenceChange change = (ProjectReferenceChange) this.projectReferenceChanges.get(project);
		if (change == null) {
			change = new ProjectReferenceChange(project, oldResolvedClasspath);
			this.projectReferenceChanges.put(project, change);
	    }
	}

	public void initializeRoots() {

		// recompute root infos only if necessary
		HashMap newRoots = null;
		HashMap newOtherRoots = null;
		HashMap newSourceAttachments = null;
		HashMap newProjectDependencies = null;
		if (this.rootsAreStale) {
			Thread currentThread = Thread.currentThread();
			boolean addedCurrentThread = false;
			try {
				// if reentering initialization (through a container initializer for example) no need to compute roots again
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=47213
				if (!this.initializingThreads.add(currentThread)) return;
				addedCurrentThread = true;

				// all classpaths in the workspace are going to be resolved
				// ensure that containers are initialized in one batch
				JavaModelManager.getJavaModelManager().batchContainerInitializations = true;

				newRoots = new HashMap();
				newOtherRoots = new HashMap();
				newSourceAttachments = new HashMap();
				newProjectDependencies = new HashMap();

				IJavaScriptModel model = JavaModelManager.getJavaModelManager().getJavaModel();
				IJavaScriptProject[] projects;
				try {
					projects = model.getJavaScriptProjects();
				} catch (JavaScriptModelException e) {
					// nothing can be done
					return;
				}
				for (int i = 0, length = projects.length; i < length; i++) {
					JavaProject project = (JavaProject) projects[i];
					IIncludePathEntry[] classpath;
					try {
						classpath = project.getResolvedClasspath();
					} catch (JavaScriptModelException e) {
						// continue with next project
						continue;
					}
					for (int j= 0, classpathLength = classpath.length; j < classpathLength; j++) {
						IIncludePathEntry entry = classpath[j];
						if (entry.getEntryKind() == IIncludePathEntry.CPE_PROJECT) {
							IJavaScriptProject key = model.getJavaScriptProject(entry.getPath().segment(0)); // TODO (jerome) reuse handle
							IJavaScriptProject[] dependents = (IJavaScriptProject[]) newProjectDependencies.get(key);
							if (dependents == null) {
								dependents = new IJavaScriptProject[] {project};
							} else {
								int dependentsLength = dependents.length;
								System.arraycopy(dependents, 0, dependents = new IJavaScriptProject[dependentsLength+1], 0, dependentsLength);
								dependents[dependentsLength] = project;
							}
							newProjectDependencies.put(key, dependents);
							continue;
						}

						// root path
						IPath path = entry.getPath();
						if (newRoots.get(path) == null) {
							newRoots.put(path, new DeltaProcessor.RootInfo(project, path, ((ClasspathEntry)entry).fullInclusionPatternChars(), ((ClasspathEntry)entry).fullExclusionPatternChars(), entry.getEntryKind()));
						} else {
							ArrayList rootList = (ArrayList)newOtherRoots.get(path);
							if (rootList == null) {
								rootList = new ArrayList();
								newOtherRoots.put(path, rootList);
							}
							rootList.add(new DeltaProcessor.RootInfo(project, path, ((ClasspathEntry)entry).fullInclusionPatternChars(), ((ClasspathEntry)entry).fullExclusionPatternChars(), entry.getEntryKind()));
						}

						// source attachment path
						if (entry.getEntryKind() != IIncludePathEntry.CPE_LIBRARY) continue;
						String propertyString = null;
						try {
							propertyString = Util.getSourceAttachmentProperty(path);
						} catch (JavaScriptModelException e) {
							e.printStackTrace();
						}
						IPath sourceAttachmentPath;
						if (propertyString != null) {
							int index= propertyString.lastIndexOf(PackageFragmentRoot.ATTACHMENT_PROPERTY_DELIMITER);
							sourceAttachmentPath = (index < 0) ?  new Path(propertyString) : new Path(propertyString.substring(0, index));
						} else {
							sourceAttachmentPath = entry.getSourceAttachmentPath();
						}
						if (sourceAttachmentPath != null) {
							newSourceAttachments.put(sourceAttachmentPath, path);
						}
					}
				}
			} finally {
				if (addedCurrentThread) {
					this.initializingThreads.remove(currentThread);
				}
			}
		}
		synchronized(this) {
			this.oldRoots = this.roots;
			this.oldOtherRoots = this.otherRoots;
			if (this.rootsAreStale && newRoots != null) { // double check again
				this.roots = newRoots;
				this.otherRoots = newOtherRoots;
				this.sourceAttachments = newSourceAttachments;
				this.projectDependencies = newProjectDependencies;
				this.rootsAreStale = false;
			}
		}
	}

	public synchronized ClasspathValidation[] removeClasspathValidations() {
	    int length = this.classpathValidations.size();
	    if (length == 0) return null;
	    ClasspathValidation[]  validations = new ClasspathValidation[length];
	    this.classpathValidations.values().toArray(validations);
	    this.classpathValidations.clear();
	    return validations;
	}

	public synchronized ProjectReferenceChange[] removeProjectReferenceChanges() {
	    int length = this.projectReferenceChanges.size();
	    if (length == 0) return null;
	    ProjectReferenceChange[]  updates = new ProjectReferenceChange[length];
	    this.projectReferenceChanges.values().toArray(updates);
	    this.projectReferenceChanges.clear();
	    return updates;
	}

	public synchronized void removeElementChangedListener(IElementChangedListener listener) {

		for (int i = 0; i < this.elementChangedListenerCount; i++){

			if (this.elementChangedListeners[i] == listener){

				// need to clone defensively since we might be in the middle of listener notifications (#fire)
				int length = this.elementChangedListeners.length;
				IElementChangedListener[] newListeners = new IElementChangedListener[length];
				System.arraycopy(this.elementChangedListeners, 0, newListeners, 0, i);
				int[] newMasks = new int[length];
				System.arraycopy(this.elementChangedListenerMasks, 0, newMasks, 0, i);

				// copy trailing listeners
				int trailingLength = this.elementChangedListenerCount - i - 1;
				if (trailingLength > 0){
					System.arraycopy(this.elementChangedListeners, i+1, newListeners, i, trailingLength);
					System.arraycopy(this.elementChangedListenerMasks, i+1, newMasks, i, trailingLength);
				}

				// update manager listener state (#fire need to iterate over original listeners through a local variable to hold onto
				// the original ones)
				this.elementChangedListeners = newListeners;
				this.elementChangedListenerMasks = newMasks;
				this.elementChangedListenerCount--;
				return;
			}
		}
	}

	public synchronized void removePreResourceChangedListener(IResourceChangeListener listener) {

		for (int i = 0; i < this.preResourceChangeListenerCount; i++){

			if (this.preResourceChangeListeners[i] == listener){

				// need to clone defensively since we might be in the middle of listener notifications (#fire)
				int length = this.preResourceChangeListeners.length;
				IResourceChangeListener[] newListeners = new IResourceChangeListener[length];
				int[] newEventMasks = new int[length];
				System.arraycopy(this.preResourceChangeListeners, 0, newListeners, 0, i);
				System.arraycopy(this.preResourceChangeEventMasks, 0, newEventMasks, 0, i);

				// copy trailing listeners
				int trailingLength = this.preResourceChangeListenerCount - i - 1;
				if (trailingLength > 0) {
					System.arraycopy(this.preResourceChangeListeners, i+1, newListeners, i, trailingLength);
					System.arraycopy(this.preResourceChangeEventMasks, i+1, newEventMasks, i, trailingLength);
				}

				// update manager listener state (#fire need to iterate over original listeners through a local variable to hold onto
				// the original ones)
				this.preResourceChangeListeners = newListeners;
				this.preResourceChangeEventMasks = newEventMasks;
				this.preResourceChangeListenerCount--;
				return;
			}
		}
	}

	public void resourceChanged(final IResourceChangeEvent event) {
		for (int i = 0; i < this.preResourceChangeListenerCount; i++) {
			// wrap callbacks with Safe runnable for subsequent listeners to be called when some are causing grief
			final IResourceChangeListener listener = this.preResourceChangeListeners[i];
			if ((this.preResourceChangeEventMasks[i] & event.getType()) != 0)
				SafeRunner.run(new ISafeRunnable() {
					public void handleException(Throwable exception) {
						Util.log(exception, "Exception occurred in listener of pre Java resource change notification"); //$NON-NLS-1$
					}
					public void run() throws Exception {
						listener.resourceChanged(event);
					}
				});
		}
		try {
			getDeltaProcessor().resourceChanged(event);
		} finally {
			// TODO (jerome) see 47631, may want to get rid of following so as to reuse delta processor ?
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				this.deltaProcessors.set(null);
			}
		}

	}

	public Hashtable getExternalLibTimeStamps() {
		if (this.externalTimeStamps == null) {
			Hashtable timeStamps = new Hashtable();
			File timestampsFile = getTimeStampsFile();
			DataInputStream in = null;
			try {
				in = new DataInputStream(new BufferedInputStream(new FileInputStream(timestampsFile)));
				int size = in.readInt();
				while (size-- > 0) {
					String key = in.readUTF();
					long timestamp = in.readLong();
					timeStamps.put(Path.fromPortableString(key), Long.valueOf(timestamp));
				}
			} catch (IOException e) {
				if (timestampsFile.exists())
					Util.log(e, "Unable to read external time stamps"); //$NON-NLS-1$
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						// nothing we can do: ignore
					}
				}
			}
			this.externalTimeStamps = timeStamps;
		}
		return this.externalTimeStamps;
	}

	public IJavaScriptProject findJavaProject(String name) {
		if (getOldJavaProjecNames().contains(name))
			return JavaModelManager.getJavaModelManager().getJavaModel().getJavaScriptProject(name);
		return null;
	}

	/*
	 * Workaround for bug 15168 circular errors not reported
	 * Returns the list of java projects before resource delta processing
	 * has started.
	 */
	public synchronized HashSet getOldJavaProjecNames() {
		if (this.javaProjectNamesCache == null) {
			HashSet result = new HashSet();
			IJavaScriptProject[] projects;
			try {
				projects = JavaModelManager.getJavaModelManager().getJavaModel().getJavaScriptProjects();
			} catch (JavaScriptModelException e) {
				return this.javaProjectNamesCache;
			}
			for (int i = 0, length = projects.length; i < length; i++) {
				IJavaScriptProject project = projects[i];
				result.add(project.getElementName());
			}
			return this.javaProjectNamesCache = result;
		}
		return this.javaProjectNamesCache;
	}

	public synchronized void resetOldJavaProjectNames() {
		this.javaProjectNamesCache = null;
	}

	private File getTimeStampsFile() {
		return JavaScriptCore.getPlugin().getStateLocation().append("externalLibsTimeStamps").toFile(); //$NON-NLS-1$
	}

	public void saveExternalLibTimeStamps() throws CoreException {
		if (this.externalTimeStamps == null) return;
		File timestamps = getTimeStampsFile();
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(timestamps)));
			out.writeInt(this.externalTimeStamps.size());
			Iterator entries = this.externalTimeStamps.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry entry = (Map.Entry) entries.next();
				IPath key = (IPath) entry.getKey();
				out.writeUTF(key.toPortableString());
				Long timestamp = (Long) entry.getValue();
				out.writeLong(timestamp.longValue());
			}
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, IStatus.ERROR, "Problems while saving timestamps", e); //$NON-NLS-1$
			throw new CoreException(status);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// nothing we can do: ignore
				}
			}
		}
	}

	/*
	 * Update the roots that are affected by the addition or the removal of the given container resource.
	 */
	public synchronized void updateRoots(IPath containerPath, IResourceDelta containerDelta, DeltaProcessor deltaProcessor) {
		Map updatedRoots;
		Map otherUpdatedRoots;
		if (containerDelta.getKind() == IResourceDelta.REMOVED) {
			updatedRoots = this.oldRoots;
			otherUpdatedRoots = this.oldOtherRoots;
		} else {
			updatedRoots = this.roots;
			otherUpdatedRoots = this.otherRoots;
		}
		int containerSegmentCount = containerPath.segmentCount();
		boolean containerIsProject = containerSegmentCount == 1;
		Iterator iterator = updatedRoots.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = (Map.Entry) iterator.next();
			IPath path = (IPath) entry.getKey();
			if (containerPath.isPrefixOf(path) && !containerPath.equals(path)) {
				IResourceDelta rootDelta = containerDelta.findMember(path.removeFirstSegments(containerSegmentCount));
				if (rootDelta == null) continue;
				DeltaProcessor.RootInfo rootInfo = (DeltaProcessor.RootInfo) entry.getValue();

				if (!containerIsProject
						|| !rootInfo.project.getPath().isPrefixOf(path)) { // only consider folder roots that are not included in the container
					deltaProcessor.updateCurrentDeltaAndIndex(rootDelta, IJavaScriptElement.PACKAGE_FRAGMENT_ROOT, rootInfo);
				}

				ArrayList rootList = (ArrayList)otherUpdatedRoots.get(path);
				if (rootList != null) {
					Iterator otherProjects = rootList.iterator();
					while (otherProjects.hasNext()) {
						rootInfo = (DeltaProcessor.RootInfo)otherProjects.next();
						if (!containerIsProject
								|| !rootInfo.project.getPath().isPrefixOf(path)) { // only consider folder roots that are not included in the container
							deltaProcessor.updateCurrentDeltaAndIndex(rootDelta, IJavaScriptElement.PACKAGE_FRAGMENT_ROOT, rootInfo);
						}
					}
				}
			}
		}
	}

}
