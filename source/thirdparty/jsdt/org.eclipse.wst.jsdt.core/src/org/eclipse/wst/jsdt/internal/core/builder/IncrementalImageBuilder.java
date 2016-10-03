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
package org.eclipse.wst.jsdt.internal.core.builder;

import java.util.ArrayList;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IJavaScriptModelMarker;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * The incremental image builder
 */
public class IncrementalImageBuilder extends AbstractImageBuilder {

protected ArrayList sourceFiles;
protected ArrayList previousSourceFiles;
protected StringSet qualifiedStrings;
protected StringSet simpleStrings;
protected SimpleLookupTable secondaryTypesToRemove;
protected boolean hasStructuralChanges;
protected int compileLoop;
protected boolean makeOutputFolderConsistent;

public static int MaxCompileLoop = 5; // perform a full build if it takes more than ? incremental compile loops

protected IncrementalImageBuilder(JavaBuilder javaBuilder, State buildState) {
	super(javaBuilder, true, buildState);
	this.nameEnvironment.isIncrementalBuild = true;
	this.makeOutputFolderConsistent = JavaScriptCore.ENABLED.equals(
		javaBuilder.javaProject.getOption(JavaScriptCore.CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER, true));
}

protected IncrementalImageBuilder(JavaBuilder javaBuilder) {
	this(javaBuilder, null);
	this.newState.copyFrom(javaBuilder.lastState);
}

protected IncrementalImageBuilder(BatchImageBuilder batchBuilder) {
	this(batchBuilder.javaBuilder, batchBuilder.newState);
	resetCollections();
}

public boolean build(SimpleLookupTable deltas) {
	// initialize builder
	// walk this project's deltas, find changed source files
	// walk prereq projects' deltas, find changed class files & add affected source files
	//   use the build state # to skip the deltas for certain prereq projects
	//   ignore changed zip/jar files since they caused a full build
	// compile the source files & acceptResult()
	// compare the produced class files against the existing ones on disk
	// recompile all dependent source files of any type with structural changes or new/removed secondary type
	// keep a loop counter to abort & perform a full build

	if (JavaBuilder.DEBUG)
		System.out.println("INCREMENTAL build"); //$NON-NLS-1$

	try {
		resetCollections();

		notifier.subTask(Messages.build_analyzingDeltas);
		if (javaBuilder.hasBuildpathErrors()) {
			// if a mssing class file was detected in the last build, a build state was saved since its no longer fatal
			// but we need to rebuild every source file since problems were not recorded
			// AND to avoid the infinite build scenario if this project is involved in a cycle, see bug 160550
			// we need to avoid unnecessary deltas caused by doing a full build in this case
			javaBuilder.currentProject.deleteMarkers(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
			addAllSourceFiles(sourceFiles);
			notifier.updateProgressDelta(0.25f);
		} else {
			IResourceDelta sourceDelta = (IResourceDelta) deltas.get(javaBuilder.currentProject);
			if (sourceDelta != null)
				if (!findSourceFiles(sourceDelta)) return false;
			notifier.updateProgressDelta(0.10f);

			Object[] keyTable = deltas.keyTable;
			Object[] valueTable = deltas.valueTable;
			for (int i = 0, l = valueTable.length; i < l; i++) {
				IResourceDelta delta = (IResourceDelta) valueTable[i];
				if (delta != null) {
					IProject p = (IProject) keyTable[i];
					ClasspathLocation[] classFoldersAndJars = (ClasspathLocation[]) javaBuilder.binaryLocationsPerProject.get(p);
					if (classFoldersAndJars != null)
						if (!findAffectedSourceFiles(delta, classFoldersAndJars, p)) return false;
				}
			}
			notifier.updateProgressDelta(0.10f);

			notifier.subTask(Messages.build_analyzingSources);
			addAffectedSourceFiles();
			notifier.updateProgressDelta(0.05f);
		}

		this.compileLoop = 0;
		float increment = 0.40f;
		while (sourceFiles.size() > 0) { // added to in acceptResult
			if (++this.compileLoop > MaxCompileLoop) {
				if (JavaBuilder.DEBUG)
					System.out.println("ABORTING incremental build... exceeded loop count"); //$NON-NLS-1$
				return false;
			}
			notifier.checkCancel();

			SourceFile[] allSourceFiles = new SourceFile[sourceFiles.size()];
			sourceFiles.toArray(allSourceFiles);
			resetCollections();

			workQueue.addAll(allSourceFiles);
			notifier.setProgressPerCompilationUnit(increment / allSourceFiles.length);
			increment = increment / 2;
			compile(allSourceFiles);
			//removeSecondaryTypes();
			addAffectedSourceFiles();
		}
		if (this.hasStructuralChanges && javaBuilder.javaProject.hasCycleMarker())
			javaBuilder.mustPropagateStructuralChanges();
	} catch (AbortIncrementalBuildException e) {
		// abort the incremental build and let the batch builder handle the problem
		if (JavaBuilder.DEBUG)
			System.out.println("ABORTING incremental build... problem with " + e.qualifiedTypeName + //$NON-NLS-1$
				". Likely renamed inside its existing source file."); //$NON-NLS-1$
		return false;
	} catch (CoreException e) {
		throw internalException(e);
	} finally {
		cleanUp();
	}
	return true;
}

protected void buildAfterBatchBuild() {
	// called from a batch builder once all source files have been compiled AND some changes
	// need to be propagated incrementally (annotations, missing secondary types)

	if (JavaBuilder.DEBUG)
		System.out.println("INCREMENTAL build after batch build @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$

	// this is a copy of the incremental build loop
	try {
		addAffectedSourceFiles();
		while (this.sourceFiles.size() > 0) {
			notifier.checkCancel();
			SourceFile[] allSourceFiles = new SourceFile[this.sourceFiles.size()];
			this.sourceFiles.toArray(allSourceFiles);
			resetCollections();
			notifier.setProgressPerCompilationUnit(0.08f / allSourceFiles.length);
			this.workQueue.addAll(allSourceFiles);
			compile(allSourceFiles);
			//removeSecondaryTypes();
			addAffectedSourceFiles();
		}
//	} catch (Exception e) {
//		throw internalException(e);
	} finally {
		cleanUp();
	}
}

protected void addAffectedSourceFiles() {
	if (qualifiedStrings.elementSize == 0 && simpleStrings.elementSize == 0) return;

	addAffectedSourceFiles(qualifiedStrings, simpleStrings, null);
}

protected void addAffectedSourceFiles(StringSet qualifiedSet, StringSet simpleSet, StringSet affectedTypes) {
	// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are just 'X'
	char[][][] internedQualifiedNames = ReferenceCollection.internQualifiedNames(qualifiedSet);
	// if a well known qualified name was found then we can skip over these
	if (internedQualifiedNames.length < qualifiedSet.elementSize)
		internedQualifiedNames = null;
	char[][] internedSimpleNames = ReferenceCollection.internSimpleNames(simpleSet);
	// if a well known name was found then we can skip over these
	if (internedSimpleNames.length < simpleSet.elementSize)
		internedSimpleNames = null;

	Object[] keyTable = newState.references.keyTable;
	Object[] valueTable = newState.references.valueTable;
	next : for (int i = 0, l = valueTable.length; i < l; i++) {
		String typeLocator = (String) keyTable[i];
		if (typeLocator != null) {
			if (affectedTypes != null && !affectedTypes.includes(typeLocator)) continue next;
			ReferenceCollection refs = (ReferenceCollection) valueTable[i];
			if (refs.includes(internedQualifiedNames, internedSimpleNames)) {
				IFile file = javaBuilder.currentProject.getFile(typeLocator);
				SourceFile sourceFile = findSourceFile(file, true);
				if (sourceFile == null) continue next;
				if (sourceFiles.contains(sourceFile)) continue next;
				if (compiledAllAtOnce && previousSourceFiles != null && previousSourceFiles.contains(sourceFile))
					continue next; // can skip previously compiled files since already saw hierarchy related problems

				if (JavaBuilder.DEBUG)
					System.out.println("  adding affected source file " + typeLocator); //$NON-NLS-1$
				sourceFiles.add(sourceFile);
			}
		}
	}
}

protected void addDependentsOf(IPath path, boolean isStructuralChange) {
	if (isStructuralChange && !this.hasStructuralChanges) {
		newState.tagAsStructurallyChanged();
		this.hasStructuralChanges = true;
	}
	// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are just 'X'
	path = path.setDevice(null);
	String packageName = path.removeLastSegments(1).toString();
	qualifiedStrings.add(packageName);
	String typeName = path.lastSegment();
	if (simpleStrings.add(typeName) && JavaBuilder.DEBUG)
		System.out.println("  will look for dependents of " //$NON-NLS-1$
			+ typeName + " in " + packageName); //$NON-NLS-1$
}

protected boolean checkForClassFileChanges(IResourceDelta binaryDelta, ClasspathMultiDirectory md, int segmentCount) throws CoreException {
	IResource resource = binaryDelta.getResource();
	// remember that if inclusion & exclusion patterns change then a full build is done
	boolean isExcluded = (md.exclusionPatterns != null || md.inclusionPatterns != null)
		&& Util.isExcluded(resource, md.inclusionPatterns, md.exclusionPatterns);
	switch(resource.getType()) {
		case IResource.FOLDER :
			if (isExcluded && md.inclusionPatterns == null)
		        return true; // no need to go further with this delta since its children cannot be included

			IResourceDelta[] children = binaryDelta.getAffectedChildren();
			for (int i = 0, l = children.length; i < l; i++)
				if (!checkForClassFileChanges(children[i], md, segmentCount))
					return false;
			return true;
		case IResource.FILE :
			if (!isExcluded && org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(resource.getName())) {
				// perform full build if a managed class file has been changed
				IPath typePath = resource.getFullPath().removeFirstSegments(segmentCount).removeFileExtension();
				if (newState.isKnownType(typePath.toString())) {
					if (JavaBuilder.DEBUG)
						System.out.println("MUST DO FULL BUILD. Found change to class file " + typePath); //$NON-NLS-1$
					return false;
				}
				return true;
			}
	}
	return true;
}

protected void cleanUp() {
	super.cleanUp();

	this.sourceFiles = null;
	this.previousSourceFiles = null;
	this.qualifiedStrings = null;
	this.simpleStrings = null;
	this.secondaryTypesToRemove = null;
	this.hasStructuralChanges = false;
	this.compileLoop = 0;
}

protected void compile(SourceFile[] units, SourceFile[] additionalUnits, boolean compilingFirstGroup) {
	if (compilingFirstGroup && additionalUnits != null) {
		// add any source file from additionalUnits to units if it defines secondary types
		// otherwise its possible during testing with MAX_AT_ONCE == 1 that a secondary type
		// can cause an infinite loop as it alternates between not found and defined, see bug 146324
		ArrayList extras = null;
		for (int i = 0, l = additionalUnits.length; i < l; i++) {
			SourceFile unit = additionalUnits[i];
			if (unit != null && newState.getDefinedTypeNamesFor(unit.typeLocator()) != null) {
				if (JavaBuilder.DEBUG)
					System.out.println("About to compile file with secondary types "+ unit.typeLocator()); //$NON-NLS-1$
				if (extras == null)
					extras = new ArrayList(3);
				extras.add(unit);
			}
		}
		if (extras != null) {
			int oldLength = units.length;
			int toAdd = extras.size();
			System.arraycopy(units, 0, units = new SourceFile[oldLength + toAdd], 0, oldLength);
			for (int i = 0; i < toAdd; i++)
				units[oldLength++] = (SourceFile) extras.get(i);
		}
	}
	super.compile(units, additionalUnits, compilingFirstGroup);
}

protected void deleteGeneratedFiles(IFile[] deletedGeneratedFiles) {
	// delete generated files and recompile any affected source files
//	try {
		for (int j = deletedGeneratedFiles.length; --j >= 0;) {
			IFile deletedFile = deletedGeneratedFiles[j];
			if (deletedFile.exists()) continue; // only delete .class files for source files that were actually deleted

			SourceFile sourceFile = findSourceFile(deletedFile, false);
			String typeLocator = sourceFile.typeLocator();
			int mdSegmentCount = sourceFile.sourceLocation.sourceFolder.getFullPath().segmentCount();
			IPath typePath = sourceFile.resource.getFullPath().removeFirstSegments(mdSegmentCount).removeFileExtension();
			addDependentsOf(typePath, true); // add dependents of the source file since its now deleted
			previousSourceFiles = null; // existing source files did not see it as deleted since they were compiled before it was
			char[][] definedTypeNames = newState.getDefinedTypeNamesFor(typeLocator);
//			if (definedTypeNames == null) { // defined a single type matching typePath
//				removeClassFile(typePath, sourceFile.sourceLocation.binaryFolder);
//			} else {
//				if (definedTypeNames.length > 0) { // skip it if it failed to successfully define a type
//					IPath packagePath = typePath.removeLastSegments(1);
//					for (int d = 0, l = definedTypeNames.length; d < l; d++)
//						removeClassFile(packagePath.append(new String(definedTypeNames[d])), sourceFile.sourceLocation.binaryFolder);
//				}
//			}
			this.newState.removeLocator(typeLocator);
		}
//	} catch (CoreException e) {
//		// must continue with compile loop so just log the CoreException
//		e.printStackTrace();
//	}
}

protected boolean findAffectedSourceFiles(IResourceDelta delta, ClasspathLocation[] classFoldersAndJars, IProject prereqProject) {
	for (int i = 0, l = classFoldersAndJars.length; i < l; i++) {
		ClasspathLocation bLocation = classFoldersAndJars[i];
		// either a .class file folder or a zip/jar file
		if (bLocation != null) { // skip unchanged output folder
			IPath p = bLocation.getProjectRelativePath();
			if (p != null) {
				IResourceDelta binaryDelta = delta.findMember(p);
				if (binaryDelta != null) {
					if (binaryDelta.getKind() == IResourceDelta.ADDED || binaryDelta.getKind() == IResourceDelta.REMOVED) {
						if (JavaBuilder.DEBUG)
							System.out.println("ABORTING incremental build... found added/removed binary folder"); //$NON-NLS-1$
						return false; // added/removed binary folder should not make it here (classpath change), but handle anyways
					}
					int segmentCount = binaryDelta.getFullPath().segmentCount();
					IResourceDelta[] children = binaryDelta.getAffectedChildren(); // .class files from class folder
					StringSet structurallyChangedTypes = null;
					if (bLocation.isOutputFolder())
						structurallyChangedTypes = this.newState.getStructurallyChangedTypes(javaBuilder.getLastState(prereqProject));
					for (int j = 0, m = children.length; j < m; j++)
						findAffectedSourceFiles(children[j], segmentCount, structurallyChangedTypes);
					notifier.checkCancel();
				}
			}
		}
	}
	return true;
}

protected void findAffectedSourceFiles(IResourceDelta binaryDelta, int segmentCount, StringSet structurallyChangedTypes) {
	// When a package becomes a type or vice versa, expect 2 deltas,
	// one on the folder & one on the class file
	IResource resource = binaryDelta.getResource();
	switch(resource.getType()) {
		case IResource.FOLDER :
			switch (binaryDelta.getKind()) {
				case IResourceDelta.ADDED :
				case IResourceDelta.REMOVED :
					IPath packagePath = resource.getFullPath().removeFirstSegments(segmentCount);
					String packageName = packagePath.toString();
					if (binaryDelta.getKind() == IResourceDelta.ADDED) {
						// see if any known source file is from the same package... classpath already includes new package
						if (!newState.isKnownPackage(packageName)) {
							if (JavaBuilder.DEBUG)
								System.out.println("Found added package " + packageName); //$NON-NLS-1$
							addDependentsOf(packagePath, false);
							return;
						}
						if (JavaBuilder.DEBUG)
							System.out.println("Skipped dependents of added package " + packageName); //$NON-NLS-1$
					} else {
						// see if the package still exists on the classpath
						if (!nameEnvironment.isPackage(packageName)) {
							if (JavaBuilder.DEBUG)
								System.out.println("Found removed package " + packageName); //$NON-NLS-1$
							addDependentsOf(packagePath, false);
							return;
						}
						if (JavaBuilder.DEBUG)
							System.out.println("Skipped dependents of removed package " + packageName); //$NON-NLS-1$
					}
					// fall thru & traverse the sub-packages and .class files
				case IResourceDelta.CHANGED :
					IResourceDelta[] children = binaryDelta.getAffectedChildren();
					for (int i = 0, l = children.length; i < l; i++)
						findAffectedSourceFiles(children[i], segmentCount, structurallyChangedTypes);
			}
			return;
		case IResource.FILE :
			if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(resource.getName())) {
				IPath typePath = resource.getFullPath().removeFirstSegments(segmentCount).removeFileExtension();
				switch (binaryDelta.getKind()) {
					case IResourceDelta.ADDED :
					case IResourceDelta.REMOVED :
						if (JavaBuilder.DEBUG)
							System.out.println("Found added/removed class file " + typePath); //$NON-NLS-1$
						addDependentsOf(typePath, false);
						return;
					case IResourceDelta.CHANGED :
						if ((binaryDelta.getFlags() & IResourceDelta.CONTENT) == 0)
							return; // skip it since it really isn't changed
						if (structurallyChangedTypes != null && !structurallyChangedTypes.includes(typePath.toString()))
							return; // skip since it wasn't a structural change
						if (JavaBuilder.DEBUG)
							System.out.println("Found changed class file " + typePath); //$NON-NLS-1$
						addDependentsOf(typePath, false);
				}
				return;
			}
	}
}

protected boolean findSourceFiles(IResourceDelta delta) throws CoreException {
	ArrayList visited = this.makeOutputFolderConsistent ? new ArrayList(sourceLocations.length) : null;
	for (int i = 0, l = sourceLocations.length; i < l; i++) {
		ClasspathMultiDirectory md = sourceLocations[i];
		if (this.makeOutputFolderConsistent && md.hasIndependentOutputFolder && !visited.contains(md.binaryFolder)) {
			// even a project which acts as its own source folder can have an independent/nested output folder
			visited.add(md.binaryFolder);
			IResourceDelta binaryDelta = delta.findMember(md.binaryFolder.getProjectRelativePath());
			if (binaryDelta != null) {
				int segmentCount = binaryDelta.getFullPath().segmentCount();
				IResourceDelta[] children = binaryDelta.getAffectedChildren();
				for (int j = 0, m = children.length; j < m; j++)
					if (!checkForClassFileChanges(children[j], md, segmentCount))
						return false;
			}
		}
		if (md.sourceFolder.equals(javaBuilder.currentProject)) {
			// skip nested source & output folders when the project is a source folder
			int segmentCount = delta.getFullPath().segmentCount();
			IResourceDelta[] children = delta.getAffectedChildren();
			for (int j = 0, m = children.length; j < m; j++)
				if (!isExcludedFromProject(children[j].getFullPath()))
					if (!findSourceFiles(children[j], md, segmentCount))
						return false;
		} else {
			IResourceDelta sourceDelta = delta.findMember(md.sourceFolder.getProjectRelativePath());
			if (sourceDelta != null) {
				if (sourceDelta.getKind() == IResourceDelta.REMOVED) {
					if (JavaBuilder.DEBUG)
						System.out.println("ABORTING incremental build... found removed source folder"); //$NON-NLS-1$
					return false; // removed source folder should not make it here, but handle anyways (ADDED is supported)
				}
				int segmentCount = sourceDelta.getFullPath().segmentCount();
				IResourceDelta[] children = sourceDelta.getAffectedChildren();
				try {
					for (int j = 0, m = children.length; j < m; j++)
						if (!findSourceFiles(children[j], md, segmentCount))
							return false;
				} catch (CoreException e) {
					// catch the case that a package has been renamed and collides on disk with an as-yet-to-be-deleted package
					if (e.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
						if (JavaBuilder.DEBUG)
							System.out.println("ABORTING incremental build... found renamed package"); //$NON-NLS-1$
						return false;
					}
					throw e; // rethrow
				}
			}
		}
		notifier.checkCancel();
	}
	return true;
}

protected boolean findSourceFiles(IResourceDelta sourceDelta, ClasspathMultiDirectory md, int segmentCount) throws CoreException {
	// When a package becomes a type or vice versa, expect 2 deltas,
	// one on the folder & one on the source file
	IResource resource = sourceDelta.getResource();
	// remember that if inclusion & exclusion patterns change then a full build is done
	boolean isExcluded = (md.exclusionPatterns != null || md.inclusionPatterns != null)
		&& Util.isExcluded(resource, md.inclusionPatterns, md.exclusionPatterns);
	switch(resource.getType()) {
		case IResource.FOLDER :
			if (isExcluded && md.inclusionPatterns == null)
		        return true; // no need to go further with this delta since its children cannot be included

			switch (sourceDelta.getKind()) {
				case IResourceDelta.ADDED :
				    if (!isExcluded) {
						IPath addedPackagePath = resource.getFullPath().removeFirstSegments(segmentCount);
						// add dependents even when the package thinks it exists to be on the safe side
						if (JavaBuilder.DEBUG)
							System.out.println("Found added package " + addedPackagePath); //$NON-NLS-1$
						addDependentsOf(addedPackagePath, true);
				    }
					// fall thru & collect all the source files
				case IResourceDelta.CHANGED :
					IResourceDelta[] children = sourceDelta.getAffectedChildren();
					for (int i = 0, l = children.length; i < l; i++)
						if (!findSourceFiles(children[i], md, segmentCount))
							return false;
					return true;
				case IResourceDelta.REMOVED :
				    if (isExcluded) {
				    	// since this folder is excluded then there is nothing to delete (from this md), but must walk any included subfolders
						children = sourceDelta.getAffectedChildren();
						for (int i = 0, l = children.length; i < l; i++)
							if (!findSourceFiles(children[i], md, segmentCount))
								return false;
						return true;
				    }
					IPath removedPackagePath = resource.getFullPath().removeFirstSegments(segmentCount);
					if (sourceLocations.length > 1) {
						for (int i = 0, l = sourceLocations.length; i < l; i++) {
							if (sourceLocations[i].sourceFolder.getFolder(removedPackagePath).exists()) {
								// only a package fragment was removed, same as removing multiple source files
								IResourceDelta[] removedChildren = sourceDelta.getAffectedChildren();
								for (int j = 0, m = removedChildren.length; j < m; j++)
									if (!findSourceFiles(removedChildren[j], md, segmentCount))
										return false;
								return true;
							}
						}
					}
					IFolder removedPackageFolder = md.binaryFolder.getFolder(removedPackagePath);
//					if (removedPackageFolder.exists())
//						removedPackageFolder.delete(IResource.FORCE, null);
					// add dependents even when the package thinks it does not exist to be on the safe side
					if (JavaBuilder.DEBUG)
						System.out.println("Found removed package " + removedPackagePath); //$NON-NLS-1$
					addDependentsOf(removedPackagePath, true);
					newState.removePackage(sourceDelta);
			}
			return true;
		case IResource.FILE :
			if (isExcluded) return true;

			String resourceName = resource.getName();
			if (org.eclipse.wst.jsdt.internal.core.util.Util.isJavaLikeFileName(resourceName)) {
				IPath typePath = resource.getFullPath().removeFirstSegments(segmentCount).removeFileExtension();
				String typeLocator = resource.getProjectRelativePath().toString();
				switch (sourceDelta.getKind()) {
					case IResourceDelta.ADDED :
						if (JavaBuilder.DEBUG)
							System.out.println("Compile this added source file " + typeLocator); //$NON-NLS-1$
						if (!resource.isDerived())
							sourceFiles.add(new SourceFile((IFile) resource, md, true));
						String typeName = typePath.toString();
						if (!newState.isDuplicateLocator(typeName, typeLocator)) { // adding dependents results in 2 duplicate errors
							if (JavaBuilder.DEBUG)
								System.out.println("Found added source file " + typeName); //$NON-NLS-1$
							addDependentsOf(typePath, true);
						}
						return true;
					case IResourceDelta.REMOVED :
						char[][] definedTypeNames = newState.getDefinedTypeNamesFor(typeLocator);
						if (definedTypeNames == null) { // defined a single type matching typePath
//							removeClassFile(typePath, md.binaryFolder);
							if ((sourceDelta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
								// remove problems and tasks for a compilation unit that is being moved (to another package or renamed)
								// if the target file is a compilation unit, the new cu will be recompiled
								// if the target file is a non-java resource, then markers are removed
								// see bug 2857
								IResource movedFile = javaBuilder.workspaceRoot.getFile(sourceDelta.getMovedToPath());
								JavaBuilder.removeProblemsAndTasksFor(movedFile);
							}
						} else {
							if (JavaBuilder.DEBUG)
								System.out.println("Found removed source file " + typePath.toString()); //$NON-NLS-1$
							addDependentsOf(typePath, true); // add dependents of the source file since it may be involved in a name collision
//							if (definedTypeNames.length > 0) { // skip it if it failed to successfully define a type
//								IPath packagePath = typePath.removeLastSegments(1);
//								for (int i = 0, l = definedTypeNames.length; i < l; i++)
//									removeClassFile(packagePath.append(new String(definedTypeNames[i])), md.binaryFolder);
//							}
						}
						newState.removeLocator(typeLocator);
						return true;
					case IResourceDelta.CHANGED :
						if ((sourceDelta.getFlags() & IResourceDelta.CONTENT) == 0
								&& (sourceDelta.getFlags() & IResourceDelta.ENCODING) == 0)
							return true; // skip it since it really isn't changed
						if (JavaBuilder.DEBUG)
							System.out.println("Compile this changed source file " + typeLocator); //$NON-NLS-1$
						if (!resource.isDerived())
							sourceFiles.add(new SourceFile((IFile) resource, md, true));
				}
				return true;
			} else if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(resourceName)) {
				// perform full build if a managed class file has been changed
				if (this.makeOutputFolderConsistent) {
					IPath typePath = resource.getFullPath().removeFirstSegments(segmentCount).removeFileExtension();
					if (newState.isKnownType(typePath.toString())) {
						if (JavaBuilder.DEBUG)
							System.out.println("MUST DO FULL BUILD. Found change to class file " + typePath); //$NON-NLS-1$
						return false;
					}
				}
				return true;
			} else if (md.hasIndependentOutputFolder) {
				return true;
			}
	}
	return true;
}

protected void finishedWith(String sourceLocator, CompilationResult result, char[] mainTypeName, ArrayList definedTypeNames, ArrayList duplicateTypeNames) {
	char[][] previousTypeNames = newState.getDefinedTypeNamesFor(sourceLocator);
	if (previousTypeNames == null)
		previousTypeNames = new char[][] {mainTypeName};
	IPath packagePath = null;
	next : for (int i = 0, l = previousTypeNames.length; i < l; i++) {
		char[] previous = previousTypeNames[i];
		for (int j = 0, m = definedTypeNames.size(); j < m; j++)
			if (CharOperation.equals(previous, (char[]) definedTypeNames.get(j)))
				continue next;

		SourceFile sourceFile = (SourceFile) result.getCompilationUnit();
		if (packagePath == null) {
			int count = sourceFile.sourceLocation.sourceFolder.getFullPath().segmentCount();
			packagePath = sourceFile.resource.getFullPath().removeFirstSegments(count).removeLastSegments(1);
		}
		if (secondaryTypesToRemove == null)
			this.secondaryTypesToRemove = new SimpleLookupTable();
		ArrayList types = (ArrayList) secondaryTypesToRemove.get(sourceFile.sourceLocation.binaryFolder);
		if (types == null)
			types = new ArrayList(definedTypeNames.size());
		types.add(packagePath.append(new String(previous)));
		secondaryTypesToRemove.put(sourceFile.sourceLocation.binaryFolder, types);
	}
	super.finishedWith(sourceLocator, result, mainTypeName, definedTypeNames, duplicateTypeNames);
}

//protected void removeClassFile(IPath typePath, IContainer outputFolder) throws CoreException {
//	if (typePath.lastSegment().indexOf('$') == -1) { // is not a nested type
//		newState.removeQualifiedTypeName(typePath.toString());
//		// add dependents even when the type thinks it does not exist to be on the safe side
//		if (JavaBuilder.DEBUG)
//			System.out.println("Found removed type " + typePath); //$NON-NLS-1$
//		addDependentsOf(typePath, true); // when member types are removed, their enclosing type is structurally changed
//	}
//	IFile classFile = outputFolder.getFile(typePath.addFileExtension(SuffixConstants.EXTENSION_class));
//	if (classFile.exists()) {
//		if (JavaBuilder.DEBUG)
//			System.out.println("Deleting class file of removed type " + typePath); //$NON-NLS-1$
//		classFile.delete(IResource.FORCE, null);
//	}
//}

//protected void removeSecondaryTypes() throws CoreException {
//	if (secondaryTypesToRemove != null) { // delayed deleting secondary types until the end of the compile loop
//		Object[] keyTable = secondaryTypesToRemove.keyTable;
//		Object[] valueTable = secondaryTypesToRemove.valueTable;
//		for (int i = 0, l = keyTable.length; i < l; i++) {
//			IContainer outputFolder = (IContainer) keyTable[i];
//			if (outputFolder != null) {
//				ArrayList paths = (ArrayList) valueTable[i];
//				for (int j = 0, m = paths.size(); j < m; j++)
//					removeClassFile((IPath) paths.get(j), outputFolder);
//			}
//		}
//		this.secondaryTypesToRemove = null;
//		if (previousSourceFiles != null)
//			this.previousSourceFiles = null; // cannot optimize recompile case when a secondary type is deleted, see 181269
//	}
//}

protected void resetCollections() {
	if (this.sourceFiles == null) {
		this.sourceFiles = new ArrayList(33);
		this.previousSourceFiles = null;
		this.qualifiedStrings = new StringSet(3);
		this.simpleStrings = new StringSet(3);
		this.hasStructuralChanges = false;
		this.compileLoop = 0;
	} else {
		this.previousSourceFiles = this.sourceFiles.isEmpty() ? null : (ArrayList) this.sourceFiles.clone();

		this.sourceFiles.clear();
		this.qualifiedStrings.clear();
		this.simpleStrings.clear();
		this.workQueue.clear();
	}
}

protected void updateProblemsFor(SourceFile sourceFile, CompilationResult result) throws CoreException {
	IMarker[] markers = JavaBuilder.getProblemsFor(sourceFile.resource);
	CategorizedProblem[] problems = result.getProblems();
	if (problems == null && markers.length == 0) return;

	notifier.updateProblemCounts(markers, problems);
	JavaBuilder.removeProblemsFor(sourceFile.resource);
	storeProblemsFor(sourceFile, problems);
}

protected void updateTasksFor(SourceFile sourceFile, CompilationResult result) throws CoreException {
	IMarker[] markers = JavaBuilder.getTasksFor(sourceFile.resource);
	CategorizedProblem[] tasks = result.getTasks();
	if (tasks == null && markers.length == 0) return;

	JavaBuilder.removeTasksFor(sourceFile.resource);
	storeTasksFor(sourceFile, tasks);
}

public String toString() {
	return "incremental image builder for:\n\tnew state: " + newState; //$NON-NLS-1$
}


/* Debug helper

static void dump(IResourceDelta delta) {
	StringBuffer buffer = new StringBuffer();
	IPath path = delta.getFullPath();
	for (int i = path.segmentCount(); --i > 0;)
		buffer.append("  ");
	switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			buffer.append('+');
			break;
		case IResourceDelta.REMOVED:
			buffer.append('-');
			break;
		case IResourceDelta.CHANGED:
			buffer.append('*');
			break;
		case IResourceDelta.NO_CHANGE:
			buffer.append('=');
			break;
		default:
			buffer.append('?');
			break;
	}
	buffer.append(path);
	System.out.println(buffer.toString());
	IResourceDelta[] children = delta.getAffectedChildren();
	for (int i = 0, l = children.length; i < l; i++)
		dump(children[i]);
}
*/
}
