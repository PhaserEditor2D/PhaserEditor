/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.compiler;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.jsdt.internal.core.builder.ValidationParticipantResult;
import org.eclipse.wst.jsdt.internal.core.builder.SourceFile;

/**
 * The context of a validation event that is notified to interested validation
 * participants when {@link ValidationParticipant#buildStarting(BuildContext[], boolean) a build is starting.
 * <p>
 * This class is not intended to be instanciated or subclassed by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class BuildContext extends ValidationParticipantResult {

/**
 * Creates a build context for the given source file.
 * <p>
 * This constructor is not intended to be called by clients.
 * </p>
 *
 * @param sourceFile the source file being built
 */
public BuildContext(SourceFile sourceFile) {
	super(sourceFile);
}

/**
 * Returns the contents of the javaScript unit.
 *
 * @return the contents of the javaScript unit
 */
public char[] getContents() {
	return this.sourceFile.getContents();
}

/**
 * Returns the <code>IFile</code> representing the javaScript unit.
 *
 * @return the <code>IFile</code> representing the javaScript unit
 */
public IFile getFile() {
	return this.sourceFile.resource;
}

/**
 * Record the added/changed generated files that need to be compiled.
 *
 * @param addedGeneratedFiles the added/changed files
 */
public void recordAddedGeneratedFiles(IFile[] addedGeneratedFiles) {
	int length2 = addedGeneratedFiles.length;
	if (length2 == 0) return;

	int length1 = this.addedFiles == null ? 0 : this.addedFiles.length;
	IFile[] merged = new IFile[length1 + length2];
	if (length1 > 0) // always make a copy even if currently empty
		System.arraycopy(this.addedFiles, 0, merged, 0, length1);
	System.arraycopy(addedGeneratedFiles, 0, merged, length1, length2);
	this.addedFiles = merged;
}

/**
 * Record the generated files that need to be deleted.
 *
 * @param deletedGeneratedFiles the files that need to be deleted
 */
public void recordDeletedGeneratedFiles(IFile[] deletedGeneratedFiles) {
	int length2 = deletedGeneratedFiles.length;
	if (length2 == 0) return;

	int length1 = this.deletedFiles == null ? 0 : this.deletedFiles.length;
	IFile[] merged = new IFile[length1 + length2];
	if (length1 > 0) // always make a copy even if currently empty
		System.arraycopy(this.deletedFiles, 0, merged, 0, length1);
	System.arraycopy(deletedGeneratedFiles, 0, merged, length1, length2);
	this.deletedFiles = merged;
}

/**
 * Record the fully-qualified type names of any new dependencies, each name is of the form "p1.p2.A.B".
 *
 * @param typeNameDependencies the fully-qualified type names of new dependencies
 */
public void recordDependencies(String[] typeNameDependencies) {
	int length2 = typeNameDependencies.length;
	if (length2 == 0) return;

	int length1 = this.dependencies == null ? 0 : this.dependencies.length;
	String[] merged = new String[length1 + length2];
	if (length1 > 0) // always make a copy even if currently empty
		System.arraycopy(this.dependencies, 0, merged, 0, length1);
	System.arraycopy(typeNameDependencies, 0, merged, length1, length2);
	this.dependencies = merged;
}

/**
 * Record new problems to report against this compilationUnit.
 * Markers are persisted for these problems only for the declared managed marker type
 * (see the 'validationParticipant' extension point).
 *
 * @param newProblems the problems to report
 */
public void recordNewProblems(CategorizedProblem[] newProblems) {
	int length2 = newProblems.length;
	if (length2 == 0) return;

	int length1 = this.problems == null ? 0 : this.problems.length;
	CategorizedProblem[] merged = new CategorizedProblem[length1 + length2];
	if (length1 > 0) // always make a copy even if currently empty
		System.arraycopy(this.problems, 0, merged, 0, length1);
	System.arraycopy(newProblems, 0, merged, length1, length2);
	this.problems = merged;
}

}
