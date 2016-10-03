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
package org.eclipse.wst.jsdt.internal.core.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IJavaScriptModelMarker;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.BuildContext;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.Compiler;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.ICompilerRequestor;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * The abstract superclass of Java builders.
 * Provides the building and compilation mechanism
 * in common with the batch and incremental builders.
 */
public abstract class AbstractImageBuilder implements ICompilerRequestor, ICompilationUnitLocator {

protected JavaBuilder javaBuilder;
protected State newState;

// local copies
protected NameEnvironment nameEnvironment;
protected ClasspathMultiDirectory[] sourceLocations;
protected BuildNotifier notifier;

protected Compiler compiler;
protected WorkQueue workQueue;
protected ArrayList problemSourceFiles;
protected boolean compiledAllAtOnce;

private boolean inCompiler;
protected boolean keepStoringProblemMarkers;

public static int MAX_AT_ONCE = 2000; // best compromise between space used and speed
public final static String[] JAVA_PROBLEM_MARKER_ATTRIBUTE_NAMES = {
	IMarker.MESSAGE,
	IMarker.SEVERITY,
	IJavaScriptModelMarker.ID,
	IMarker.CHAR_START,
	IMarker.CHAR_END,
	IMarker.LINE_NUMBER,
	IJavaScriptModelMarker.ARGUMENTS,
	IJavaScriptModelMarker.CATEGORY_ID,
};
public final static String[] JAVA_TASK_MARKER_ATTRIBUTE_NAMES = {
	IMarker.MESSAGE,
	IMarker.PRIORITY,
	IJavaScriptModelMarker.ID,
	IMarker.CHAR_START,
	IMarker.CHAR_END,
	IMarker.LINE_NUMBER,
	IMarker.USER_EDITABLE,
	IMarker.SOURCE_ID,
};
public final static Integer S_ERROR = Integer.valueOf(IMarker.SEVERITY_ERROR);
public final static Integer S_WARNING = Integer.valueOf(IMarker.SEVERITY_WARNING);
public final static Integer P_HIGH = Integer.valueOf(IMarker.PRIORITY_HIGH);
public final static Integer P_NORMAL = Integer.valueOf(IMarker.PRIORITY_NORMAL);
public final static Integer P_LOW = Integer.valueOf(IMarker.PRIORITY_LOW);

protected AbstractImageBuilder(JavaBuilder javaBuilder, boolean buildStarting, State newState) {
	// local copies
	this.javaBuilder = javaBuilder;
	this.nameEnvironment = javaBuilder.nameEnvironment;
	this.sourceLocations = this.nameEnvironment.sourceLocations;
	this.notifier = javaBuilder.notifier;
	this.keepStoringProblemMarkers = true; // may get disabled when missing classfiles are encountered

	if (buildStarting) {
		this.newState = newState == null ? new State(javaBuilder) : newState;
		this.compiler = newCompiler();
		this.workQueue = new WorkQueue();
		this.problemSourceFiles = new ArrayList(3);
	}
}

public void acceptResult(CompilationResult result) {
	// In Batch mode, we write out the class files, hold onto the dependency info
	// & additional types and report problems.

	// In Incremental mode, when writing out a class file we need to compare it
	// against the previous file, remembering if structural changes occured.
	// Before reporting the new problems, we need to update the problem count &
	// remove the old problems. Plus delete additional class files that no longer exist.

	SourceFile compilationUnit = (SourceFile) result.getCompilationUnit(); // go directly back to the sourceFile
	if (!workQueue.isCompiled(compilationUnit)) {
		workQueue.finished(compilationUnit);

		try {
			updateProblemsFor(compilationUnit, result); // record compilation problems before potentially adding duplicate errors
			updateTasksFor(compilationUnit, result); // record tasks
		} catch (CoreException e) {
			throw internalException(e);
		}

		if (result.hasInconsistentToplevelHierarchies)
			// ensure that this file is always retrieved from source for the rest of the build
			if (!problemSourceFiles.contains(compilationUnit))
				problemSourceFiles.add(compilationUnit);

		String typeLocator = compilationUnit.typeLocator();

		finishedWith(typeLocator, result, compilationUnit.getMainTypeName(), new ArrayList(), new ArrayList());
		notifier.compiled(compilationUnit);
	}
}


protected void addAllSourceFiles(final ArrayList sourceFiles) throws CoreException {
	for (int i = 0, l = sourceLocations.length; i < l; i++) {
		final ClasspathMultiDirectory sourceLocation = sourceLocations[i];
		final char[][] exclusionPatterns = sourceLocation.exclusionPatterns;
		final char[][] inclusionPatterns = sourceLocation.inclusionPatterns;
		final boolean isAlsoProject = sourceLocation.sourceFolder.equals(javaBuilder.currentProject);
		final int segmentCount = sourceLocation.sourceFolder.getFullPath().segmentCount();
		final IContainer outputFolder = sourceLocation.binaryFolder;
		final boolean isOutputFolder = sourceLocation.sourceFolder.equals(outputFolder);
		sourceLocation.sourceFolder.accept(
			new IResourceProxyVisitor() {
				public boolean visit(IResourceProxy proxy) throws CoreException {
					if (proxy.isDerived())
						return false;
					switch(proxy.getType()) {
						case IResource.FILE :
							if (org.eclipse.wst.jsdt.internal.core.util.Util.isJavaLikeFileName(proxy.getName())) {
								IResource resource = proxy.requestResource();
								if (exclusionPatterns != null || inclusionPatterns != null)
									if (Util.isExcluded(resource.getFullPath(), inclusionPatterns, exclusionPatterns, false))
										return false;
								sourceFiles.add(new SourceFile((IFile) resource, sourceLocation));
							}
							return false;
						case IResource.FOLDER :
							IPath folderPath = null;
							if (isAlsoProject)
								if (isExcludedFromProject(folderPath = proxy.requestFullPath()))
									return false;
							if (JavaScriptCore.isReadOnly(proxy.requestResource()))
								return false;
							if (exclusionPatterns != null) {
								if (folderPath == null)
									folderPath = proxy.requestFullPath();
								if (Util.isExcluded(folderPath, inclusionPatterns, exclusionPatterns, true)) {
									// must walk children if inclusionPatterns != null, can skip them if == null
									// but folder is excluded so do not create it in the output folder
									return inclusionPatterns != null;
								}
							}
					}
					return true;
				}
			},
			IResource.NONE
		);
		notifier.checkCancel();
	}
}

protected void cleanUp() {
	this.nameEnvironment.cleanup();

	this.javaBuilder = null;
	this.nameEnvironment = null;
	this.sourceLocations = null;
	this.notifier = null;
	this.compiler = null;
	this.workQueue = null;
	this.problemSourceFiles = null;
}

/* Compile the given elements, adding more elements to the work queue
* if they are affected by the changes.
*/
protected void compile(SourceFile[] units) {
	// notify validationParticipants which source files are about to be compiled
	BuildContext[] participantResults = this.javaBuilder.participants == null ? null : notifyParticipants(units);
	if (participantResults != null && participantResults.length > units.length) {
		units = new SourceFile[participantResults.length];
		for (int i = participantResults.length; --i >= 0;)
			units[i] = participantResults[i].sourceFile;
	}

	int unitsLength = units.length;
	this.compiledAllAtOnce = unitsLength <= MAX_AT_ONCE;
	if (this.compiledAllAtOnce) {
		// do them all now
		if (JavaBuilder.DEBUG)
			for (int i = 0; i < unitsLength; i++)
				System.out.println("About to compile " + units[i].typeLocator()); //$NON-NLS-1$
		compile(units, null, true);
	} else {
		SourceFile[] remainingUnits = new SourceFile[unitsLength]; // copy of units, removing units when about to compile
		System.arraycopy(units, 0, remainingUnits, 0, unitsLength);
		int doNow = unitsLength < MAX_AT_ONCE ? unitsLength : MAX_AT_ONCE;
		SourceFile[] toCompile = new SourceFile[doNow];
		int remainingIndex = 0;
		boolean compilingFirstGroup = true;
		while (remainingIndex < unitsLength) {
			int count = 0;
			while (remainingIndex < unitsLength && count < doNow) {
				// Although it needed compiling when this method was called, it may have
				// already been compiled when it was referenced by another unit.
				SourceFile unit = remainingUnits[remainingIndex];
				if (unit != null && (compilingFirstGroup || this.workQueue.isWaiting(unit))) {
					if (JavaBuilder.DEBUG)
						System.out.println("About to compile #" + remainingIndex + " : "+ unit.typeLocator()); //$NON-NLS-1$ //$NON-NLS-2$
					toCompile[count++] = unit;
				}
				remainingUnits[remainingIndex++] = null;
			}
			if (count < doNow)
				System.arraycopy(toCompile, 0, toCompile = new SourceFile[count], 0, count);
			if (!compilingFirstGroup)
				for (int a = remainingIndex; a < unitsLength; a++)
					if (remainingUnits[a] != null && this.workQueue.isCompiled(remainingUnits[a]))
						remainingUnits[a] = null; // use the class file for this source file since its been compiled
			compile(toCompile, remainingUnits, compilingFirstGroup);
			compilingFirstGroup = false;
		}
	}

	if (participantResults != null) {
		for (int i = participantResults.length; --i >= 0;)
			if (participantResults[i] != null)
				recordParticipantResult(participantResults[i]);
	}
}

protected void compile(SourceFile[] units, SourceFile[] additionalUnits, boolean compilingFirstGroup) {
	if (units.length == 0) return;
	notifier.aboutToCompile(units[0]); // just to change the message

	// extend additionalFilenames with all hierarchical problem types found during this entire build
	if (!problemSourceFiles.isEmpty()) {
		int toAdd = problemSourceFiles.size();
		int length = additionalUnits == null ? 0 : additionalUnits.length;
		if (length == 0)
			additionalUnits = new SourceFile[toAdd];
		else
			System.arraycopy(additionalUnits, 0, additionalUnits = new SourceFile[length + toAdd], 0, length);
		for (int i = 0; i < toAdd; i++)
			additionalUnits[length + i] = (SourceFile) problemSourceFiles.get(i);
	}
//	String[] initialTypeNames = new String[units.length];
//	for (int i = 0, l = units.length; i < l; i++)
//		initialTypeNames[i] = units[i].initialTypeName;
//	nameEnvironment.setNames(initialTypeNames, additionalUnits);
	notifier.checkCancel();
	try {
		inCompiler = true;
		compiler.compile(units);
	} catch (AbortCompilation ignored) {
		// ignore the AbortCompilcation coming from BuildNotifier.checkCancelWithinCompiler()
		// the Compiler failed after the user has chose to cancel... likely due to an OutOfMemory error
	} finally {
		inCompiler = false;
	}
	// Check for cancel immediately after a compile, because the compiler may
	// have been cancelled but without propagating the correct exception
	notifier.checkCancel();
}

protected void createProblemFor(IResource resource, IMember javaElement, String message, String problemSeverity) {
	try {
		IMarker marker = resource.createMarker(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER);
		int severity = problemSeverity.equals(JavaScriptCore.WARNING) ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_ERROR;

		ISourceRange range = javaElement == null ? null : javaElement.getNameRange();
		int start = range == null ? 0 : range.getOffset();
		int end = range == null ? 1 : start + range.getLength();
		marker.setAttributes(
			new String[] {IMarker.MESSAGE, IMarker.SEVERITY, IMarker.CHAR_START, IMarker.CHAR_END, IMarker.SOURCE_ID},
			new Object[] {message, Integer.valueOf(severity), Integer.valueOf(start), Integer.valueOf(end), JavaBuilder.SOURCE_ID});
	} catch (CoreException e) {
		throw internalException(e);
	}
}

protected void deleteGeneratedFiles(IFile[] deletedGeneratedFiles) {
	// no op by default
}

protected SourceFile findSourceFile(IFile file, boolean mustExist) {
	if (mustExist && !file.exists()) return null;
	if (file.isDerived()) return null;

	// assumes the file exists in at least one of the source folders & is not excluded
	ClasspathMultiDirectory md = sourceLocations[0];
	if (sourceLocations.length > 1) {
		IPath sourceFileFullPath = file.getFullPath();
		for (int j = 0, m = sourceLocations.length; j < m; j++) {
			if (sourceLocations[j].sourceFolder.getFullPath().isPrefixOf(sourceFileFullPath)) {
				md = sourceLocations[j];
				if (md.exclusionPatterns == null && md.inclusionPatterns == null)
					break;
				if (!Util.isExcluded(file, md.inclusionPatterns, md.exclusionPatterns))
					break;
			}
		}
	}
	return new SourceFile(file, md);
}

protected void finishedWith(String sourceLocator, CompilationResult result, char[] mainTypeName, ArrayList definedTypeNames, ArrayList duplicateTypeNames) {
	if (duplicateTypeNames == null) {
		newState.record(sourceLocator, result.qualifiedReferences, result.simpleNameReferences, mainTypeName, definedTypeNames);
		return;
	}

	char[][][] qualifiedRefs = result.qualifiedReferences;
	char[][] simpleRefs = result.simpleNameReferences;
	// for each duplicate type p1.p2.A, add the type name A (package was already added)
	next : for (int i = 0, l = duplicateTypeNames.size(); i < l; i++) {
		char[][] compoundName = (char[][]) duplicateTypeNames.get(i);
		char[] typeName = compoundName[compoundName.length - 1];
		int sLength = simpleRefs.length;
		for (int j = 0; j < sLength; j++)
			if (CharOperation.equals(simpleRefs[j], typeName))
				continue next;
		System.arraycopy(simpleRefs, 0, simpleRefs = new char[sLength + 1][], 0, sLength);
		simpleRefs[sLength] = typeName;
	}
	newState.record(sourceLocator, qualifiedRefs, simpleRefs, mainTypeName, definedTypeNames);
}





/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.core.builder.ICompilationUnitLocator#fromIFile(org.eclipse.core.resources.IFile)
 */
public ICompilationUnit fromIFile(IFile file) {
	return findSourceFile(file, true);
}

protected RuntimeException internalException(CoreException t) {
	ImageBuilderInternalException imageBuilderException = new ImageBuilderInternalException(t);
	if (inCompiler)
		return new AbortCompilation(true, imageBuilderException);
	return imageBuilderException;
}

protected boolean isExcludedFromProject(IPath childPath) throws JavaScriptModelException {
	// answer whether the folder should be ignored when walking the project as a source folder
	if (childPath.segmentCount() > 2) return false; // is a subfolder of a package

	for (int j = 0, k = sourceLocations.length; j < k; j++) {
		if (childPath.equals(sourceLocations[j].binaryFolder.getFullPath())) return true;
		if (childPath.equals(sourceLocations[j].sourceFolder.getFullPath())) return true;
	}
	// skip default output folder which may not be used by any source folder
	return childPath.equals(javaBuilder.javaProject.getOutputLocation());
}

protected Compiler newCompiler() {
	// disable entire javadoc support if not interested in diagnostics
	Map projectOptions = javaBuilder.javaProject.getOptions(true);
	String option = (String) projectOptions.get(JavaScriptCore.COMPILER_PB_INVALID_JAVADOC);
	if (option == null || option.equals(JavaScriptCore.IGNORE)) { // TODO (frederic) see why option is null sometimes while running model tests!?
		option = (String) projectOptions.get(JavaScriptCore.COMPILER_PB_MISSING_JAVADOC_TAGS);
		if (option == null || option.equals(JavaScriptCore.IGNORE)) {
			option = (String) projectOptions.get(JavaScriptCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS);
			if (option == null || option.equals(JavaScriptCore.IGNORE)) {
				option = (String) projectOptions.get(JavaScriptCore.COMPILER_PB_UNUSED_IMPORT);
				if (option == null || option.equals(JavaScriptCore.IGNORE)) { // Unused import need also to look inside javadoc comment
					projectOptions.put(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT, JavaScriptCore.DISABLED);
				}
			}
		}
	}

	// called once when the builder is initialized... can override if needed
	CompilerOptions compilerOptions = new CompilerOptions(projectOptions);
	compilerOptions.performMethodsFullRecovery = true;
	compilerOptions.performStatementsRecovery = true;
	Compiler newCompiler = new Compiler(
		nameEnvironment,
		DefaultErrorHandlingPolicies.proceedWithAllProblems(),
		compilerOptions,
		this,
		ProblemFactory.getProblemFactory(Locale.getDefault()));
	CompilerOptions options = newCompiler.options;

	// enable the compiler reference info support
	options.produceReferenceInfo = true;

	return newCompiler;
}

protected BuildContext[] notifyParticipants(SourceFile[] unitsAboutToCompile) {
	BuildContext[] results = new BuildContext[unitsAboutToCompile.length];
	for (int i = unitsAboutToCompile.length; --i >= 0;)
		results[i] = new BuildContext(unitsAboutToCompile[i]);

	// TODO (kent) do we expect to have more than one participant?
	// and if so should we pass the generated files from the each processor to the others to process?
	// and what happens if some participants do not expect to be called with only a few files, after seeing 'all' the files?
	for (int i = 0, l = this.javaBuilder.participants.length; i < l; i++)
		this.javaBuilder.participants[i].buildStarting(results, this instanceof BatchImageBuilder);

	SimpleSet uniqueFiles = null;
	ValidationParticipantResult[] toAdd = null;
	int added = 0;
	for (int i = results.length; --i >= 0;) {
		ValidationParticipantResult result = results[i];
		if (result == null) continue;

		IFile[] deletedGeneratedFiles = result.deletedFiles;
		if (deletedGeneratedFiles != null)
			deleteGeneratedFiles(deletedGeneratedFiles);

		IFile[] addedGeneratedFiles = result.addedFiles;
		if (addedGeneratedFiles != null) {
			for (int j = addedGeneratedFiles.length; --j >= 0;) {
				SourceFile sourceFile = findSourceFile(addedGeneratedFiles[j], true);
				if (sourceFile == null) continue;
				if (uniqueFiles == null) {
					uniqueFiles = new SimpleSet(unitsAboutToCompile.length + 3);
					for (int f = unitsAboutToCompile.length; --f >= 0;)
						uniqueFiles.add(unitsAboutToCompile[f]);
				}
				if (uniqueFiles.addIfNotIncluded(sourceFile) == sourceFile) {
					ValidationParticipantResult newResult = new BuildContext(sourceFile);
					// is there enough room to add all the addedGeneratedFiles.length ?
					if (toAdd == null) {
						toAdd = new ValidationParticipantResult[addedGeneratedFiles.length];
					} else {
						int length = toAdd.length;
						if (added == length)
							System.arraycopy(toAdd, 0, toAdd = new ValidationParticipantResult[length + addedGeneratedFiles.length], 0, length);
					}
					toAdd[added++] = newResult;
				}
			}
		}
	}

	if (added >0 ) {
		int length = results.length;
		System.arraycopy(results, 0, results = new BuildContext[length + added], 0 , length);
		System.arraycopy(toAdd, 0, results, length, added);
	}
	return results;
}

protected void recordParticipantResult(ValidationParticipantResult result) {
	// any added/changed/deleted generated files have already been taken care
	// just record the problems and dependencies - do not expect there to be many
	// must be called after we're finished with the compilation unit results but before incremental loop adds affected files
	CategorizedProblem[] problems = result.problems;
	if (problems != null && problems.length > 0) {
		// existing problems have already been removed so just add these as new problems
		this.notifier.updateProblemCounts(problems);
		try {
			storeProblemsFor(result.sourceFile, problems);
		} catch (CoreException e) {
			// must continue with compile loop so just log the CoreException
			e.printStackTrace();
		}
	}

	String[] dependencies = result.dependencies;
	if (dependencies != null) {
		ReferenceCollection refs = (ReferenceCollection) this.newState.references.get(result.sourceFile.typeLocator());
		if (refs != null)
			refs.addDependencies(dependencies);
	}
}

/**
 * Creates a marker from each problem and adds it to the resource.
 * The marker is as follows:
 *   - its type is T_PROBLEM
 *   - its plugin ID is the JavaBuilder's plugin ID
 *	 - its message is the problem's message
 *	 - its priority reflects the severity of the problem
 *	 - its range is the problem's range
 *	 - it has an extra attribute "ID" which holds the problem's id
 *   - it's GENERATED_BY attribute is positioned to JavaBuilder.GENERATED_BY if
 *     the problem was generated by JDT; else the GENERATED_BY attribute is
 *     carried from the problem to the marker in extra attributes, if present.
 */
protected void storeProblemsFor(SourceFile sourceFile, CategorizedProblem[] problems) throws CoreException {
	if (sourceFile == null || problems == null || problems.length == 0) return;
	 // once a classpath error is found, ignore all other problems for this project so the user can see the main error
	// but still try to compile as many source files as possible to help the case when the base libraries are in source
	if (!this.keepStoringProblemMarkers) return; // only want the one error recorded on this source file

	IResource resource = sourceFile.resource;
	IResource container=(resource instanceof IFile)? resource.getParent():resource;
	if (JavaScriptCore.isReadOnly(container))
		return;
	HashSet managedMarkerTypes = JavaModelManager.getJavaModelManager().validationParticipants.managedMarkerTypes();
	for (int i = 0, l = problems.length; i < l; i++) {
		CategorizedProblem problem = problems[i];
		int id = problem.getID();

		// handle missing classfile situation
		if (id == IProblem.IsClassPathCorrect) {
			String missingClassfileName = problem.getArguments()[0];
			if (JavaBuilder.DEBUG)
				System.out.println(Messages.bind(Messages.build_incompleteClassPath, missingClassfileName));
			boolean isInvalidClasspathError = JavaScriptCore.ERROR.equals(javaBuilder.javaProject.getOption(JavaScriptCore.CORE_INCOMPLETE_CLASSPATH, true));
			// insert extra classpath problem, and make it the only problem for this project (optional)
			if (isInvalidClasspathError && JavaScriptCore.ABORT.equals(javaBuilder.javaProject.getOption(JavaScriptCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, true))) {
				JavaBuilder.removeProblemsAndTasksFor(javaBuilder.currentProject); // make this the only problem for this project
				this.keepStoringProblemMarkers = false;
			}
			IMarker marker = this.javaBuilder.currentProject.createMarker(IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER);
			marker.setAttributes(
				new String[] {IMarker.MESSAGE, IMarker.SEVERITY, IJavaScriptModelMarker.CATEGORY_ID, IMarker.SOURCE_ID},
				new Object[] {
					Messages.bind(Messages.build_incompleteClassPath, missingClassfileName),
					Integer.valueOf(isInvalidClasspathError ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING),
					Integer.valueOf(CategorizedProblem.CAT_BUILDPATH),
					JavaBuilder.SOURCE_ID
				}
			);
			// even if we're not keeping more markers, still fall through rest of the problem reporting, so that offending
			// IsClassPathCorrect problem gets recorded since it may help locate the offending reference
		}

		String markerType = problem.getMarkerType();
		boolean managedProblem = false;
		if (IJavaScriptModelMarker.JAVASCRIPT_MODEL_PROBLEM_MARKER.equals(markerType)
				|| (managedProblem = managedMarkerTypes.contains(markerType))) {
			IMarker marker = resource.createMarker(markerType);

			String[] attributeNames = JAVA_PROBLEM_MARKER_ATTRIBUTE_NAMES;
			int standardLength = attributeNames.length;
			String[] allNames = attributeNames;
			int managedLength = managedProblem ? 0 : 1;
			String[] extraAttributeNames = problem.getExtraMarkerAttributeNames();
			int extraLength = extraAttributeNames == null ? 0 : extraAttributeNames.length;
			if (managedLength > 0 || extraLength > 0) {
				allNames = new String[standardLength + managedLength + extraLength];
				System.arraycopy(attributeNames, 0, allNames, 0, standardLength);
				if (managedLength > 0)
					allNames[standardLength] = IMarker.SOURCE_ID;
				System.arraycopy(extraAttributeNames, 0, allNames, standardLength + managedLength, extraLength);
			}

			Object[] allValues = new Object[allNames.length];
			// standard attributes
			int index = 0;
			allValues[index++] = problem.getMessage(); // message
			allValues[index++] = problem.isError() ? S_ERROR : S_WARNING; // severity
			allValues[index++] = Integer.valueOf(id); // ID
			allValues[index++] = Integer.valueOf(problem.getSourceStart()); // start
			allValues[index++] = Integer.valueOf(problem.getSourceEnd() + 1); // end
			allValues[index++] = Integer.valueOf(problem.getSourceLineNumber()); // line
			allValues[index++] = Util.getProblemArgumentsForMarker(problem.getArguments()); // arguments
			allValues[index++] = Integer.valueOf(problem.getCategoryID()); // category ID
			// GENERATED_BY attribute for JDT problems
			if (managedLength > 0)
				allValues[index++] = JavaBuilder.SOURCE_ID;
			// optional extra attributes
			if (extraLength > 0)
				System.arraycopy(problem.getExtraMarkerAttributeValues(), 0, allValues, index, extraLength);

			marker.setAttributes(allNames, allValues);

			if (!this.keepStoringProblemMarkers) return; // only want the one error recorded on this source file
		}
	}
}

protected void storeTasksFor(SourceFile sourceFile, CategorizedProblem[] tasks) throws CoreException {
	if (sourceFile == null || tasks == null || tasks.length == 0) return;

	IResource resource = sourceFile.resource;
	for (int i = 0, l = tasks.length; i < l; i++) {
		CategorizedProblem task = tasks[i];
		if (task.getID() == IProblem.Task) {
			IMarker marker = resource.createMarker(IJavaScriptModelMarker.TASK_MARKER);
			Integer priority = P_NORMAL;
			String compilerPriority = task.getArguments()[2];
			if (JavaScriptCore.COMPILER_TASK_PRIORITY_HIGH.equals(compilerPriority))
				priority = P_HIGH;
			else if (JavaScriptCore.COMPILER_TASK_PRIORITY_LOW.equals(compilerPriority))
				priority = P_LOW;

			String[] attributeNames = JAVA_TASK_MARKER_ATTRIBUTE_NAMES;
			int standardLength = attributeNames.length;
			String[] allNames = attributeNames;
			String[] extraAttributeNames = task.getExtraMarkerAttributeNames();
			int extraLength = extraAttributeNames == null ? 0 : extraAttributeNames.length;
			if (extraLength > 0) {
				allNames = new String[standardLength + extraLength];
				System.arraycopy(attributeNames, 0, allNames, 0, standardLength);
				System.arraycopy(extraAttributeNames, 0, allNames, standardLength, extraLength);
			}

			Object[] allValues = new Object[allNames.length];
			// standard attributes
			int index = 0;
			allValues[index++] = task.getMessage();
			allValues[index++] = priority;
			allValues[index++] = Integer.valueOf(task.getID());
			allValues[index++] = Integer.valueOf(task.getSourceStart());
			allValues[index++] = Integer.valueOf(task.getSourceEnd() + 1);
			allValues[index++] = Integer.valueOf(task.getSourceLineNumber());
			allValues[index++] = Boolean.FALSE;
			allValues[index++] = JavaBuilder.SOURCE_ID;
			// optional extra attributes
			if (extraLength > 0)
				System.arraycopy(task.getExtraMarkerAttributeValues(), 0, allValues, index, extraLength);

			marker.setAttributes(allNames, allValues);
		}
	}
}

protected void updateProblemsFor(SourceFile sourceFile, CompilationResult result) throws CoreException {
	CategorizedProblem[] problems = result.getProblems();
	if (problems == null || problems.length == 0) return;

	notifier.updateProblemCounts(problems);
	storeProblemsFor(sourceFile, problems);
}

protected void updateTasksFor(SourceFile sourceFile, CompilationResult result) throws CoreException {
	CategorizedProblem[] tasks = result.getTasks();
	if (tasks == null || tasks.length == 0) return;

	storeTasksFor(sourceFile, tasks);
}

}
