/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM - rewrote spec
 *
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;

public class ValidationParticipantResult {
	protected SourceFile sourceFile;
	protected IFile[] addedFiles; // added/changed generated source files that need to be compiled
	protected IFile[] deletedFiles; // previously generated source files that should be deleted
	protected CategorizedProblem[] problems; // new problems to report against this compilationUnit
	protected String[] dependencies; // fully-qualified type names of any new dependencies, each name is of the form 'p1.p2.A.B'

protected ValidationParticipantResult(SourceFile sourceFile) {
	this.sourceFile = sourceFile;
	this.addedFiles = null;
	this.deletedFiles = null;
	this.problems = null;
	this.dependencies = null;
}

void reset() {
	this.addedFiles = null;
	this.deletedFiles = null;
	this.problems = null;
	this.dependencies = null;
}

public String toString() {
	return this.sourceFile.toString();
}

}
