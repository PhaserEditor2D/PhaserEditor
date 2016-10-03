/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.indexing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchDocument;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.wst.jsdt.internal.compiler.SourceElementParser;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;
import org.eclipse.wst.jsdt.internal.core.ClasspathEntry;
import org.eclipse.wst.jsdt.internal.core.JavaModel;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.LibraryFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.index.DiskIndex;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.search.BasicSearchEngine;
import org.eclipse.wst.jsdt.internal.core.search.PatternSearchJob;
import org.eclipse.wst.jsdt.internal.core.search.processing.IJob;
import org.eclipse.wst.jsdt.internal.core.search.processing.JobManager;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class IndexManager extends JobManager implements IIndexConstants {

	// key = containerPath, value = indexLocation path
	// indexLocation path is created by appending an index file name to the getJavaPluginWorkingLocation() path
	public SimpleLookupTable indexLocations = new SimpleLookupTable();
	// key = indexLocation path, value = an index
	private SimpleLookupTable indexes = new SimpleLookupTable();

	/* need to save ? */
	private boolean needToSave = false;
	private static final CRC32 checksumCalculator = new CRC32();
	private IPath javaPluginLocation = null;

	/* can only replace a current state if its less than the new one */
	// key = indexLocation path, value = index state integer
	private SimpleLookupTable indexStates = null;
	private File savedIndexNamesFile = new File(getSavedIndexesDirectory(), "savedIndexNames.txt"); //$NON-NLS-1$
	public static Integer SAVED_STATE = Integer.valueOf(0);
	public static Integer UPDATING_STATE = Integer.valueOf(1);
	public static Integer UNKNOWN_STATE = Integer.valueOf(2);
	public static Integer REBUILDING_STATE = Integer.valueOf(3);
	private static final String INDEX_FILE_SUFFIX = ".index";


public synchronized void aboutToUpdateIndex(IPath containerPath, Integer newIndexState) {
	// newIndexState is either UPDATING_STATE or REBUILDING_STATE
	// must tag the index as inconsistent, in case we exit before the update job is started
	IPath indexLocation = computeIndexLocation(containerPath);
	Object state = getIndexStates().get(indexLocation);
	Integer currentIndexState = state == null ? UNKNOWN_STATE : (Integer) state;
	if (currentIndexState.equals(REBUILDING_STATE)) return; // already rebuilding the index

	int compare = newIndexState.compareTo(currentIndexState);
	if (compare > 0) {
		// so UPDATING_STATE replaces SAVED_STATE and REBUILDING_STATE replaces everything
		updateIndexState(indexLocation, newIndexState);
	} else if (compare < 0 && this.indexes.get(indexLocation) == null) {
		// if already cached index then there is nothing more to do
		rebuildIndex(indexLocation, containerPath);
	}
}
/**
 * Trigger addition of a resource to an index
 * Note: the actual operation is performed in background
 */
public void addBinary(IFile resource, IPath containerPath) {
	if (JavaScriptCore.getPlugin() == null) return;
	SearchParticipant participant = SearchEngine.getDefaultSearchParticipant();
	SearchDocument document = participant.getDocument(resource.getFullPath().toString());
	IPath indexLocation = computeIndexLocation(containerPath);
	scheduleDocumentIndexing(document, containerPath, indexLocation, participant);
}
/**
 * Trigger addition of a resource to an index
 * Note: the actual operation is performed in background
 */
public void addSource(IFile resource, IPath containerPath, SourceElementParser parser) {
	if (JavaScriptCore.getPlugin() == null) return;
	SearchParticipant participant = SearchEngine.getDefaultSearchParticipant();
	SearchDocument document = participant.getDocument(resource.getFullPath().toString());
	((InternalSearchDocument) document).parser = parser;
	IPath indexLocation = computeIndexLocation(containerPath);
	scheduleDocumentIndexing(document, containerPath, indexLocation, participant);
}
/*
 * Removes unused indexes from disk.
 */
public void cleanUpIndexes() {
	SimpleSet knownPaths = new SimpleSet();
	IJavaScriptSearchScope scope = BasicSearchEngine.createWorkspaceScope();
	PatternSearchJob job = new PatternSearchJob(null, SearchEngine.getDefaultSearchParticipant(), scope, null);
	Index[] selectedIndexes = job.getIndexes(null);
	for (int i = 0, l = selectedIndexes.length; i < l; i++) {
		String path = selectedIndexes[i].getIndexFile().getAbsolutePath();
		knownPaths.add(path);
	}

	if (this.indexStates != null) {
		Object[] keys = this.indexStates.keyTable;
		IPath[] locations = new IPath[this.indexStates.elementSize];
		int count = 0;
		for (int i = 0, l = keys.length; i < l; i++) {
			IPath key = (IPath) keys[i];
			if (key != null && !knownPaths.includes(key.toOSString()))
				locations[count++] = key;
		}
		if (count > 0)
			removeIndexesState(locations);
	}
	deleteIndexFiles(knownPaths);
}
public IPath computeIndexLocation(IPath containerPath) {
	IPath indexLocation = (IPath) this.indexLocations.get(containerPath);
	if (indexLocation == null) {
		String pathString = containerPath.toOSString();
		checksumCalculator.reset();
		checksumCalculator.update(pathString.getBytes());
		String fileName = Long.toString(checksumCalculator.getValue()) + ".index"; //$NON-NLS-1$
		if (VERBOSE)
			Util.verbose("-> index name for " + pathString + " is " + fileName); //$NON-NLS-1$ //$NON-NLS-2$
		// to share the indexLocation between the indexLocations and indexStates tables, get the key from the indexStates table
		indexLocation = (IPath) getIndexStates().getKey(getJavaPluginWorkingLocation().append(fileName));
		this.indexLocations.put(containerPath, indexLocation);
	}
	return indexLocation;
}
public void deleteIndexFiles() {
	this.savedIndexNamesFile.delete(); // forget saved indexes & delete each index file
	deleteIndexFiles(null);
}
private void deleteIndexFiles(SimpleSet pathsToKeep) {
	File[] indexesFiles = getSavedIndexesDirectory().listFiles();
	if (indexesFiles == null) return;

	for (int i = 0, l = indexesFiles.length; i < l; i++) {
		String fileName = indexesFiles[i].getAbsolutePath();
		if (pathsToKeep != null && pathsToKeep.includes(fileName)) continue;
		if (fileName.regionMatches(true, fileName.length() - INDEX_FILE_SUFFIX.length(), INDEX_FILE_SUFFIX, 0, INDEX_FILE_SUFFIX.length())) {
			if (VERBOSE)
				Util.verbose("Deleting index file " + indexesFiles[i]); //$NON-NLS-1$
			indexesFiles[i].delete();
		}
	}
}
/*
 * Creates an empty index at the given location, for the given container path, if none exist.
 */
public void ensureIndexExists(IPath indexLocation, IPath containerPath) {
	SimpleLookupTable states = getIndexStates();
	Object state = states.get(indexLocation);
	if (state == null) {
		updateIndexState(indexLocation, REBUILDING_STATE);
		getIndex(containerPath, indexLocation, true, true);
	}
}
public SourceElementParser getSourceElementParser(IJavaScriptProject project, ISourceElementRequestor requestor) {
	// disable task tags to speed up parsing
	Map options = project.getOptions(true);
	options.put(JavaScriptCore.COMPILER_TASK_TAGS, ""); //$NON-NLS-1$

	SourceElementParser parser = new IndexingParser(
		requestor,
		new DefaultProblemFactory(Locale.getDefault()),
		new CompilerOptions(options),
		true, // index local declarations
		true, // optimize string literals
		false); // do not use source javadoc parser to speed up parsing
	parser.reportOnlyOneSyntaxError = true;

	// Always check javadoc while indexing
	parser.javadocParser.checkDocComment = true;
	parser.javadocParser.reportProblems = false;

	return parser;
}
/**
 * Returns the index for a given project, according to the following algorithm:
 * - if index is already in memory: answers this one back
 * - if (reuseExistingFile) then read it and return this index and record it in memory
 * - if (createIfMissing) then create a new empty index and record it in memory
 *
 * Warning: Does not check whether index is consistent (not being used)
 */
public synchronized Index getIndex(IPath containerPath, boolean reuseExistingFile, boolean createIfMissing) {
	IPath indexLocation = computeIndexLocation(containerPath);
	return getIndex(containerPath, indexLocation, reuseExistingFile, createIfMissing);
}
/**
 * Returns the index for a given project, according to the following algorithm:
 * - if index is already in memory: answers this one back
 * - if (reuseExistingFile) then read it and return this index and record it in memory
 * - if (createIfMissing) then create a new empty index and record it in memory
 *
 * Warning: Does not check whether index is consistent (not being used)
 */
public synchronized Index getIndex(IPath containerPath, IPath indexLocation, boolean reuseExistingFile, boolean createIfMissing) {
	// Path is already canonical per construction
	Index index = getIndex(indexLocation);
	if (index == null) {
		Object state = getIndexStates().get(indexLocation);
		Integer currentIndexState = state == null ? UNKNOWN_STATE : (Integer) state;
		if (currentIndexState == UNKNOWN_STATE) {
			// should only be reachable for query jobs
			// IF you put an index in the cache, then AddJarFileToIndex fails because it thinks there is nothing to do
			rebuildIndex(indexLocation, containerPath);
			return null;
		}

		// index isn't cached, consider reusing an existing index file
		String containerPathString = containerPath.getDevice() == null ? containerPath.toString() : containerPath.toOSString();
		String indexLocationString = indexLocation.toOSString();
		if (reuseExistingFile) {
			File indexFile = new File(indexLocationString);
			if (indexFile.exists()) { // check before creating index so as to avoid creating a new empty index if file is missing
				try {
					index = new Index(indexLocationString, containerPathString, true /*reuse index file*/);
					this.indexes.put(indexLocation, index);
					return index;
				} catch (IOException e) {
					// failed to read the existing file or its no longer compatible
					if (currentIndexState != REBUILDING_STATE) { // rebuild index if existing file is corrupt, unless the index is already being rebuilt
						if (VERBOSE)
							Util.verbose("-> cannot reuse existing index: "+indexLocationString+" path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
						rebuildIndex(indexLocation, containerPath);
						return null;
					}
					/*index = null;*/ // will fall thru to createIfMissing & create a empty index for the rebuild all job to populate
				}
			}
			if (currentIndexState == SAVED_STATE) { // rebuild index if existing file is missing
				rebuildIndex(indexLocation, containerPath);
				return null;
			}
		}
		// index wasn't found on disk, consider creating an empty new one
		if (createIfMissing) {
			try {
				if (VERBOSE)
					Util.verbose("-> create empty index: "+indexLocationString+" path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
				index = new Index(indexLocationString, containerPathString, false /*do not reuse index file*/);
				this.indexes.put(indexLocation, index);
				return index;
			} catch (IOException e) {
				if (VERBOSE)
					Util.verbose("-> unable to create empty index: "+indexLocationString+" path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
				// The file could not be created. Possible reason: the project has been deleted.
				return null;
			}
		}
	}
	//System.out.println(" index name: " + path.toOSString() + " <----> " + index.getIndexFile().getName());
	return index;
}
public synchronized Index getIndex(IPath indexLocation) {
	return (Index) this.indexes.get(indexLocation); // is null if unknown, call if the containerPath must be computed
}
public synchronized Index getIndexForUpdate(IPath containerPath, boolean reuseExistingFile, boolean createIfMissing) {
	IPath indexLocation = computeIndexLocation(containerPath);
	if (getIndexStates().get(indexLocation) == REBUILDING_STATE)
		return getIndex(containerPath, indexLocation, reuseExistingFile, createIfMissing);

	return null; // abort the job since the index has been removed from the REBUILDING_STATE
}
private SimpleLookupTable getIndexStates() {
	if (this.indexStates != null) return this.indexStates;

	this.indexStates = new SimpleLookupTable();
	IPath indexesDirectoryPath = getJavaPluginWorkingLocation();
	char[][] savedNames = readIndexState(indexesDirectoryPath.toOSString());
	if (savedNames != null) {
		for (int i = 1, l = savedNames.length; i < l; i++) { // first name is saved signature, see readIndexState()
			char[] savedName = savedNames[i];
			if (savedName.length > 0) {
				IPath indexLocation = indexesDirectoryPath.append(new String(savedName)); // shares indexesDirectoryPath's segments
				if (VERBOSE)
					Util.verbose("Reading saved index file " + indexLocation); //$NON-NLS-1$
				this.indexStates.put(indexLocation, SAVED_STATE);
			}
		}
	} else {
		deleteIndexFiles();
	}
	return this.indexStates;
}

	private IPath getJavaPluginWorkingLocation() {
		if (this.javaPluginLocation != null)
			return this.javaPluginLocation;

		IPath stateLocation = JavaScriptCore.getPlugin().getStateLocation().addTrailingSeparator().append("indexes"); //$NON-NLS-1$
		try {
			IFileStore store = EFS.getLocalFileSystem().getStore(stateLocation);
			if (!store.fetchInfo().exists()) {
				store.mkdir(EFS.SHALLOW, null);
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}

		return this.javaPluginLocation = stateLocation;
	}

private File getSavedIndexesDirectory() {
	return new File(getJavaPluginWorkingLocation().toOSString());
}
public void indexDocument(SearchDocument searchDocument, SearchParticipant searchParticipant, Index index, IPath indexLocation) {
	try {
		((InternalSearchDocument) searchDocument).index = index;
		searchParticipant.indexDocument(searchDocument, indexLocation);
	} finally {
		((InternalSearchDocument) searchDocument).index = null;
	}
}
/**
 * Trigger addition of the entire content of a project
 * Note: the actual operation is performed in background
 */
public void indexAll(IProject project) {
	if (JavaScriptCore.getPlugin() == null) return;

	// Also request indexing of binaries on the classpath
	// determine the new children
	try {
		JavaModel model = JavaModelManager.getJavaModelManager().getJavaModel();
		JavaProject javaProject = (JavaProject) model.getJavaProject(project);
		// only consider immediate libraries - each project will do the same
		// NOTE: force to resolve CP variables before calling indexer - 19303, so that initializers
		// will be run in the current thread.
		IIncludePathEntry[] entries = javaProject.getResolvedClasspath();
		for (int i = 0; i < entries.length; i++) {
			IIncludePathEntry entry= entries[i];
			if (entry.getEntryKind() == IIncludePathEntry.CPE_LIBRARY)
				this.indexLibrary(entry, project);
		}
	} catch(JavaScriptModelException e){ // cannot retrieve classpath info
	}

	// check if the same request is not already in the queue
	IndexRequest request = new IndexAllProject(project, this);
	if (!isJobWaiting(request))
		this.request(request);
}
/**
 * Trigger addition of a library to an index
 * Note: the actual operation is performed in background
 */
public void indexLibrary(IIncludePathEntry entry, IProject requestingProject) {
	// requestingProject is no longer used to cancel jobs but leave it here just in case
	if (JavaScriptCore.getPlugin() == null) return;
	IndexRequest request = null;
	Object target = JavaModel.getTarget(ResourcesPlugin.getWorkspace().getRoot(), entry.getPath(), true);
	char[][] inclusionPatterns = ((ClasspathEntry)entry).fullInclusionPatternChars();
	char[][] exclusionPatterns = ((ClasspathEntry)entry).fullExclusionPatternChars();
	
	if(target instanceof IFolder
			|| target instanceof IProject){
		request = new AddLibraryFolderToIndex(entry.getPath(), requestingProject, inclusionPatterns, exclusionPatterns, this);
	}
	else if (target instanceof IFile)
		request = new AddLibraryFileToIndex((IFile) target, this);
	else
		request = new AddLibraryFileToIndex(entry.getPath(), inclusionPatterns, exclusionPatterns, this);

	if (!isJobWaiting(request))
		this.request(request);

	

//	if (target instanceof IFile) {
//		request = new AddLibraryFileToIndex((IFile) target, this);
////		request = new AddJarFileToIndex((IFile) target, this);
//	} else if (target instanceof File) {
//		if (((File) target).isFile()) {
//			request = new AddJarFileToIndex(path, this);
//		} else {
//			return;
//		}
//	} else if (target instanceof IContainer) {
//		request = new IndexBinaryFolder((IContainer) target, this);
//	} else {
//		return;
//	}
//
//	// check if the same request is not already in the queue
//	if (!isJobWaiting(request))
//		this.request(request);
}

	public void indexLibrary(LibraryFragmentRoot entry, IProject requestingProject) {
		try {
			indexLibrary(entry.getRawIncludepathEntry(), requestingProject);
		} catch (JavaScriptModelException ex) {

			ex.printStackTrace();
		}

	}
/**
 * Index the content of the given source folder.
 */
public void indexSourceFolder(JavaProject javaProject, IPath sourceFolder, char[][] inclusionPatterns, char[][] exclusionPatterns) {
	IProject project = javaProject.getProject();
	if (this.jobEnd > this.jobStart) {
		// skip it if a job to index the project is already in the queue
		IndexRequest request = new IndexAllProject(project, this);
		if (isJobWaiting(request)) return;
	}

	this.request(new AddFolderToIndex(sourceFolder, project, inclusionPatterns, exclusionPatterns, this));
}
public synchronized void jobWasCancelled(IPath containerPath) {
	IPath indexLocation = computeIndexLocation(containerPath);
	Index index = getIndex(indexLocation);
	if (index != null) {
		index.monitor = null;
		this.indexes.removeKey(indexLocation);
	}
	updateIndexState(indexLocation, UNKNOWN_STATE);
}
/**
 * Advance to the next available job, once the current one has been completed.
 * Note: clients awaiting until the job count is zero are still waiting at this point.
 */
protected synchronized void moveToNextJob() {
	// remember that one job was executed, and we will need to save indexes at some point
	needToSave = true;
	super.moveToNextJob();
}
/**
 * No more job awaiting.
 */
protected void notifyIdle(long idlingTime){
	if (idlingTime > 1000 && needToSave) saveIndexes();
}
/**
 * Name of the background process
 */
public String processName(){
	return Messages.process_name;
}
private void rebuildIndex(IPath indexLocation, IPath containerPath) {
	IWorkspace workspace = ResourcesPlugin.getWorkspace();
	if (workspace == null) return;
	Object target = JavaModel.getTarget(workspace.getRoot(), containerPath, true);
	if (target == null) return;

	if (VERBOSE)
		Util.verbose("-> request to rebuild index: "+indexLocation+" path: "+containerPath); //$NON-NLS-1$ //$NON-NLS-2$

	updateIndexState(indexLocation, REBUILDING_STATE);
	IndexRequest request = null;
	if (target instanceof IProject) {
		IProject p = (IProject) target;
		if (JavaProject.hasJavaNature(p))
			request = new IndexAllProject(p, this);
	} else if (target instanceof IFolder) {
		request = new IndexBinaryFolder((IFolder) target, this);
	} else if (target instanceof IFile && org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(((IFile)target).getName())) {
		request = new AddJarFileToIndex((IFile) target, this);
	} else if (target instanceof File) {
		if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(((File) target).getName()))
			request = new AddJarFileToIndex(containerPath, this);
		else
			request = new AddLibraryFileToIndex(containerPath, this);
	}
	if (request != null)
		request(request);
}
/**
 * Recreates the index for a given path, keeping the same read-write monitor.
 * Returns the new empty index or null if it didn't exist before.
 * Warning: Does not check whether index is consistent (not being used)
 */
public synchronized Index recreateIndex(IPath containerPath) {
	// only called to over write an existing cached index...
	String containerPathString = containerPath.getDevice() == null ? containerPath.toString() : containerPath.toOSString();
	try {
		// Path is already canonical
		IPath indexLocation = computeIndexLocation(containerPath);
		Index index = getIndex(indexLocation);
		ReadWriteMonitor monitor = index == null ? null : index.monitor;

		if (VERBOSE)
			Util.verbose("-> recreating index: "+indexLocation+" for path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
		index = new Index(indexLocation.toOSString(), containerPathString, false /*reuse index file*/);
		this.indexes.put(indexLocation, index);
		index.monitor = monitor;
		return index;
	} catch (IOException e) {
		// The file could not be created. Possible reason: the project has been deleted.
		if (VERBOSE) {
			Util.verbose("-> failed to recreate index for path: "+containerPathString); //$NON-NLS-1$
			e.printStackTrace();
		}
		return null;
	}
}
/**
 * Trigger removal of a resource to an index
 * Note: the actual operation is performed in background
 */
public void remove(String containerRelativePath, IPath indexedContainer){
	request(new RemoveFromIndex(containerRelativePath, indexedContainer, this));
}
/**
 * Removes the index for a given path.
 * This is a no-op if the index did not exist.
 */
public synchronized void removeIndex(IPath containerPath) {
	if (VERBOSE)
		Util.verbose("removing index " + containerPath); //$NON-NLS-1$
	IPath indexLocation = computeIndexLocation(containerPath);
	Index index = getIndex(indexLocation);
	File indexFile = null;
	if (index != null) {
		index.monitor = null;
		indexFile = index.getIndexFile();
	}
	if (indexFile == null)
		indexFile = new File(indexLocation.toOSString()); // index is not cached yet, but still want to delete the file
	if (indexFile.exists())
		indexFile.delete();
	this.indexes.removeKey(indexLocation);
	updateIndexState(indexLocation, null);
}
/**
 * Removes all indexes whose paths start with (or are equal to) the given path.
 */
public synchronized void removeIndexPath(IPath path) {
	Object[] keyTable = this.indexes.keyTable;
	Object[] valueTable = this.indexes.valueTable;
	IPath[] locations = null;
	int max = this.indexes.elementSize;
	int count = 0;
	for (int i = 0, l = keyTable.length; i < l; i++) {
		IPath indexLocation = (IPath) keyTable[i];
		if (indexLocation == null)
			continue;
		if (path.isPrefixOf(indexLocation)) {
			Index index = (Index) valueTable[i];
			index.monitor = null;
			if (locations == null)
				locations = new IPath[max];
			locations[count++] = indexLocation;
			File indexFile = index.getIndexFile();
			if (indexFile.exists())
				indexFile.delete();
		} else {
			max--;
		}
	}
	if (locations != null) {
		for (int i = 0; i < count; i++)
			this.indexes.removeKey(locations[i]);
		removeIndexesState(locations);
	}
}
/**
 * Removes all indexes whose paths start with (or are equal to) the given path.
 */
public synchronized void removeIndexFamily(IPath path) {
	// only finds cached index files... shutdown removes all non-cached index files
	ArrayList toRemove = null;
	Object[] containerPaths = this.indexLocations.keyTable;
	for (int i = 0, length = containerPaths.length; i < length; i++) {
		IPath containerPath = (IPath) containerPaths[i];
		if (containerPath == null) continue;
		if (path.isPrefixOf(containerPath)) {
			if (toRemove == null)
				toRemove = new ArrayList();
			toRemove.add(containerPath);
		}
	}
	if (toRemove != null)
		for (int i = 0, length = toRemove.size(); i < length; i++)
			this.removeIndex((IPath) toRemove.get(i));
}
/**
 * Remove the content of the given source folder from the index.
 */
public void removeSourceFolderFromIndex(JavaProject javaProject, IPath sourceFolder, char[][] inclusionPatterns, char[][] exclusionPatterns) {
	IProject project = javaProject.getProject();
	if (this.jobEnd > this.jobStart) {
		// skip it if a job to index the project is already in the queue
		IndexRequest request = new IndexAllProject(project, this);
		if (isJobWaiting(request)) return;
	}

	this.request(new RemoveFolderFromIndex(sourceFolder, inclusionPatterns, exclusionPatterns, project, this));
}
/**
 * Flush current state
 */
public synchronized void reset() {
	super.reset();
	if (this.indexes != null) {
		this.indexes = new SimpleLookupTable();
		this.indexStates = null;
	}
	this.indexLocations = new SimpleLookupTable();
	this.javaPluginLocation = null;
}
/**
 * Resets the index for a given path.
 * Returns true if the index was reset, false otherwise.
 */
public synchronized boolean resetIndex(IPath containerPath) {
	// only called to over write an existing cached index...
	String containerPathString = containerPath.getDevice() == null ? containerPath.toString() : containerPath.toOSString();
	try {
		// Path is already canonical
		IPath indexLocation = computeIndexLocation(containerPath);
		Index index = getIndex(indexLocation);
		if (VERBOSE) {
			Util.verbose("-> reseting index: "+indexLocation+" for path: "+containerPathString); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (index == null) {
			// the index does not exist, try to recreate it
			return recreateIndex(containerPath) != null;
		}
		index.reset();
		return true;
	} catch (IOException e) {
		// The file could not be created. Possible reason: the project has been deleted.
		if (VERBOSE) {
			Util.verbose("-> failed to reset index for path: "+containerPathString); //$NON-NLS-1$
			e.printStackTrace();
		}
		return false;
	}
}
public void saveIndex(Index index) throws IOException {
	// must have permission to write from the write monitor
	if (index.hasChanged()) {
		if (VERBOSE)
			Util.verbose("-> saving index " + index.getIndexFile()); //$NON-NLS-1$
		index.save();
	}
	synchronized (this) {
		IPath containerPath = new Path(index.containerPath);
		if (this.jobEnd > this.jobStart) {
			for (int i = this.jobEnd; i > this.jobStart; i--) { // skip the current job
				IJob job = this.awaitingJobs[i];
				if (job instanceof IndexRequest)
					if (((IndexRequest) job).containerPath.equals(containerPath)) return;
			}
		}
		IPath indexLocation = computeIndexLocation(containerPath);
		updateIndexState(indexLocation, SAVED_STATE);
	}
}
/**
 * Commit all index memory changes to disk
 */
public void saveIndexes() {
	// only save cached indexes... the rest were not modified
	ArrayList toSave = new ArrayList();
	synchronized(this) {
		Object[] valueTable = this.indexes.valueTable;
		for (int i = 0, l = valueTable.length; i < l; i++) {
			Index index = (Index) valueTable[i];
			if (index != null)
				toSave.add(index);
		}
	}

	boolean allSaved = true;
	for (int i = 0, length = toSave.size(); i < length; i++) {
		Index index = (Index) toSave.get(i);
		ReadWriteMonitor monitor = index.monitor;
		if (monitor == null) continue; // index got deleted since acquired
		try {
			// take read lock before checking if index has changed
			// don't take write lock yet since it can cause a deadlock (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=50571)
			monitor.enterRead();
			if (index.hasChanged()) {
				if (monitor.exitReadEnterWrite()) {
					try {
						saveIndex(index);
					} catch(IOException e) {
						if (VERBOSE) {
							Util.verbose("-> got the following exception while saving:", System.err); //$NON-NLS-1$
							e.printStackTrace();
						}
						allSaved = false;
					} finally {
						monitor.exitWriteEnterRead();
					}
				} else {
					allSaved = false;
				}
			}
		} finally {
			monitor.exitRead();
		}
	}
	this.needToSave = !allSaved;
}
public void scheduleDocumentIndexing(final SearchDocument searchDocument, IPath container, final IPath indexLocation, final SearchParticipant searchParticipant) {
	request(new IndexRequest(container, this) {
		public boolean execute(IProgressMonitor progressMonitor) {
			if (this.isCancelled || progressMonitor != null && progressMonitor.isCanceled()) return true;

			/* ensure no concurrent write access to index */
			Index index = getIndex(this.containerPath, indexLocation, true, /*reuse index file*/ true /*create if none*/);
			if (index == null) return true;
			ReadWriteMonitor monitor = index.monitor;
			if (monitor == null) return true; // index got deleted since acquired

			try {
				monitor.enterWrite(); // ask permission to write
				indexDocument(searchDocument, searchParticipant, index, indexLocation);
			} finally {
				monitor.exitWrite(); // free write lock
			}
			return true;
		}
		public String toString() {
			return "indexing " + searchDocument.getPath(); //$NON-NLS-1$
		}
	});
}

public String toString() {
	StringBuffer buffer = new StringBuffer(10);
	buffer.append(super.toString());
	buffer.append("In-memory indexes:\n"); //$NON-NLS-1$
	int count = 0;
	Object[] valueTable = this.indexes.valueTable;
	for (int i = 0, l = valueTable.length; i < l; i++) {
		Index index = (Index) valueTable[i];
		if (index != null)
			buffer.append(++count).append(" - ").append(index.toString()).append('\n'); //$NON-NLS-1$
	}
	return buffer.toString();
}

private char[][] readIndexState(String dirOSString) {
	try {
		char[] savedIndexNames = org.eclipse.wst.jsdt.internal.compiler.util.Util.getFileCharContent(savedIndexNamesFile, null);
		if (savedIndexNames.length > 0) {
			char[][] names = CharOperation.splitOn('\n', savedIndexNames);
			if (names.length > 1) {
				// First line is DiskIndex signature + saved plugin working location (see writeSavedIndexNamesFile())
				String savedSignature = DiskIndex.SIGNATURE + "+" + dirOSString; //$NON-NLS-1$
				if (savedSignature.equals(new String(names[0])))
					return names;
			}
		}
	}
	catch (IOException ignored) {
		if (VERBOSE)
			Util.verbose("Failed to read saved index file names"); //$NON-NLS-1$
	}
	return null;
}
private synchronized void removeIndexesState(IPath[] locations) {
	getIndexStates(); // ensure the states are initialized
	int length = locations.length;
	boolean changed = false;
	for (int i=0; i<length; i++) {
		if (locations[i] == null) continue;
		if ((indexStates.removeKey(locations[i]) != null)) {
			changed = true;
			if (VERBOSE) {
				Util.verbose("-> index state updated to: ? for: "+locations[i]); //$NON-NLS-1$
			}
		}
	}
	if (!changed) return;

	writeSavedIndexNamesFile();
}
private synchronized void updateIndexState(IPath indexLocation, Integer indexState) {
	if (indexLocation.isEmpty())
		throw new IllegalArgumentException();

	getIndexStates(); // ensure the states are initialized
	if (indexState != null) {
		if (indexState.equals(indexStates.get(indexLocation))) return; // not changed
		indexStates.put(indexLocation, indexState);
	} else {
		if (!indexStates.containsKey(indexLocation)) return; // did not exist anyway
		indexStates.removeKey(indexLocation);
	}

	writeSavedIndexNamesFile();

	if (VERBOSE) {
		String state = "?"; //$NON-NLS-1$
		if (indexState == SAVED_STATE) state = "SAVED"; //$NON-NLS-1$
		else if (indexState == UPDATING_STATE) state = "UPDATING"; //$NON-NLS-1$
		else if (indexState == UNKNOWN_STATE) state = "UNKNOWN"; //$NON-NLS-1$
		else if (indexState == REBUILDING_STATE) state = "REBUILDING"; //$NON-NLS-1$
		Util.verbose("-> index state updated to: " + state + " for: "+indexLocation); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
private void writeSavedIndexNamesFile() {
	BufferedWriter writer = null;
	try {
		writer = new BufferedWriter(new FileWriter(savedIndexNamesFile));
		writer.write(DiskIndex.SIGNATURE);
		writer.write('+');
		writer.write(getJavaPluginWorkingLocation().toOSString());
		writer.write('\n');
		Object[] keys = indexStates.keyTable;
		Object[] states = indexStates.valueTable;
		for (int i = 0, l = states.length; i < l; i++) {
			IPath key = (IPath) keys[i];
			if (key != null && !key.isEmpty() && states[i] == SAVED_STATE) {
				writer.write(key.lastSegment());
				writer.write('\n');
			}
		}
	} catch (IOException ignored) {
		if (VERBOSE)
			Util.verbose("Failed to write saved index file names", System.err); //$NON-NLS-1$
	} finally {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
}
