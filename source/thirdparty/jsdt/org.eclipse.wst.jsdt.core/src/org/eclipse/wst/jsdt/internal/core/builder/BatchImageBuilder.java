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
package org.eclipse.wst.jsdt.internal.core.builder;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

public class BatchImageBuilder extends AbstractImageBuilder {

	IncrementalImageBuilder incrementalBuilder; // if annotations or secondary types have to be processed after the compile loop
	ArrayList secondaryTypes; // qualified names for all secondary types found during batch compile
	StringSet typeLocatorsWithUndefinedTypes; // type locators for all source files with errors that may be caused by 'not found' secondary types

protected BatchImageBuilder(JavaBuilder javaBuilder, boolean buildStarting) {
	super(javaBuilder, buildStarting, null);
	this.nameEnvironment.isIncrementalBuild = false;
	this.incrementalBuilder = null;
	this.secondaryTypes = null;
	this.typeLocatorsWithUndefinedTypes = null;
}

public void build() {
	if (JavaBuilder.DEBUG)
		System.out.println("FULL build"); //$NON-NLS-1$

	try {
//		notifier.subTask(Messages.bind(Messages.build_cleaningOutput, this.javaBuilder.currentProject.getName()));
		JavaBuilder.removeProblemsAndTasksFor(javaBuilder.currentProject);
//		cleanOutputFolders(true);
		notifier.updateProgressDelta(0.05f);

		notifier.subTask(Messages.build_analyzingSources);
		ArrayList sourceFiles = new ArrayList(33);
		addAllSourceFiles(sourceFiles);
		notifier.updateProgressDelta(0.10f);

		if (sourceFiles.size() > 0) {
			SourceFile[] allSourceFiles = new SourceFile[sourceFiles.size()];
			sourceFiles.toArray(allSourceFiles);

			notifier.setProgressPerCompilationUnit(0.75f / allSourceFiles.length);
			workQueue.addAll(allSourceFiles);
			compile(allSourceFiles);

			if (this.typeLocatorsWithUndefinedTypes != null)
				if (this.secondaryTypes != null && !this.secondaryTypes.isEmpty())
					rebuildTypesAffectedBySecondaryTypes();
			if (this.incrementalBuilder != null)
				this.incrementalBuilder.buildAfterBatchBuild();
		}

		if (javaBuilder.javaProject.hasCycleMarker())
			javaBuilder.mustPropagateStructuralChanges();
	} catch (CoreException e) {
		throw internalException(e);
	} finally {
		cleanUp();
	}
}

protected void cleanUp() {
	this.incrementalBuilder = null;
	this.secondaryTypes = null;
	this.typeLocatorsWithUndefinedTypes = null;
	super.cleanUp();
}

protected void compile(SourceFile[] units, SourceFile[] additionalUnits, boolean compilingFirstGroup) {
	if (additionalUnits != null && this.secondaryTypes == null)
		this.secondaryTypes = new ArrayList(7);
	super.compile(units, additionalUnits, compilingFirstGroup);
}

protected IResource findOriginalResource(IPath partialPath) {
	for (int i = 0, l = sourceLocations.length; i < l; i++) {
		ClasspathMultiDirectory sourceLocation = sourceLocations[i];
		if (sourceLocation.hasIndependentOutputFolder) {
			IResource originalResource = sourceLocation.sourceFolder.getFile(partialPath);
			if (originalResource.exists()) return originalResource;
		}
	}
	return null;
}

protected void rebuildTypesAffectedBySecondaryTypes() {
	// to compile types that could not find 'missing' secondary types because of multiple
	// compile groups, we need to incrementally recompile all affected types as if the missing
	// secondary types have just been added, see bug 146324
	if (this.incrementalBuilder == null)
		this.incrementalBuilder = new IncrementalImageBuilder(this);

	for (int i = this.secondaryTypes.size(); --i >=0;) {
		char[] secondaryTypeName = (char[]) this.secondaryTypes.get(i);
		IPath path = new Path(null, new String(secondaryTypeName));
		this.incrementalBuilder.addDependentsOf(path, false);
	}
	this.incrementalBuilder.addAffectedSourceFiles(
		this.incrementalBuilder.qualifiedStrings,
		this.incrementalBuilder.simpleStrings,
		this.typeLocatorsWithUndefinedTypes);
}

protected void storeProblemsFor(SourceFile sourceFile, CategorizedProblem[] problems) throws CoreException {
	if (sourceFile == null || problems == null || problems.length == 0) return;

	for (int i = problems.length; --i >= 0;) {
		CategorizedProblem problem = problems[i];
		if (problem != null && problem.getID() == IProblem.UndefinedType) {
			if (this.typeLocatorsWithUndefinedTypes == null)
				this.typeLocatorsWithUndefinedTypes = new StringSet(3);
			this.typeLocatorsWithUndefinedTypes.add(sourceFile.typeLocator());
			break;
		}
	}

	super.storeProblemsFor(sourceFile, problems);
}

public String toString() {
	return "batch image builder for:\n\tnew state: " + newState; //$NON-NLS-1$
}
}
