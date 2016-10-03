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
package org.eclipse.wst.jsdt.internal.core.search.matching;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.core.LocalVariable;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.wst.jsdt.internal.core.search.JavaSearchScope;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class LocalVariablePattern extends VariablePattern  {

LocalVariable localVariable;


public LocalVariablePattern(boolean findDeclarations, boolean readAccess, boolean writeAccess, LocalVariable localVariable, int matchRule) {
	super(LOCAL_VAR_PATTERN, findDeclarations, readAccess, writeAccess, localVariable.getElementName().toCharArray(), matchRule,localVariable);
	this.localVariable=localVariable;
}


public LocalVariablePattern(boolean findDeclarations, boolean readAccess, boolean writeAccess,char [] name, int matchRule) {
	super(LOCAL_VAR_PATTERN, findDeclarations, readAccess, writeAccess,name, matchRule,null);
}


public void findIndexMatches(Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaScriptSearchScope scope, IProgressMonitor progressMonitor) throws IOException {
	if (this.localVariable!=null)
	{

//    IPackageFragmentRoot root = (IPackageFragmentRoot)this.localVariable.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
	String documentPath;
	String relativePath;
//    if (root.isArchive()) {
//        IType type = (IType)this.localVariable.getAncestor(IJavaScriptElement.TYPE);
//        relativePath = (type.getFullyQualifiedName('/')).replace('.', '/') + SuffixConstants.SUFFIX_STRING_java;
//        documentPath = root.getPath() + IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR + relativePath;
//    } else 
    {
		IPath path = this.localVariable.getPath();
        documentPath = path.toString();
		relativePath = Util.relativePath(path, 1/*remove project segment*/);
    }

	if (scope instanceof JavaSearchScope) {
		JavaSearchScope javaSearchScope = (JavaSearchScope) scope;
		// Get document path access restriction from java search scope
		// Note that requestor has to verify if needed whether the document violates the access restriction or not
		AccessRuleSet access = javaSearchScope.getAccessRuleSet(relativePath, index.containerPath);
		if (access != JavaSearchScope.NOT_ENCLOSED) { // scope encloses the path
			if (!requestor.acceptIndexMatch(documentPath, this, participant, access))
				throw new OperationCanceledException();
		}
	} else if (scope.encloses(documentPath)) {
		if (!requestor.acceptIndexMatch(documentPath, this, participant, null))
			throw new OperationCanceledException();
	}
	}
	else
	{
		super.findIndexMatches( index,  requestor,  participant,  scope,  progressMonitor);
	}
}


protected static char[][] REF_CATEGORIES = { REF };
protected static char[][] REF_AND_DECL_CATEGORIES = { REF, VAR_DECL };
protected static char[][] DECL_CATEGORIES = { VAR_DECL };

public char[][] getIndexCategories() {
	if (this.findReferences)
		return this.findDeclarations || this.writeAccess ? REF_AND_DECL_CATEGORIES : REF_CATEGORIES;
	if (this.findDeclarations)
		return DECL_CATEGORIES;
	return CharOperation.NO_CHAR_CHAR;
}
public char[] getIndexKey() {
	return this.name;
}
public SearchPattern getBlankPattern() {
	return new LocalVariablePattern(false, false, false, (char [])null,  R_EXACT_MATCH | R_CASE_SENSITIVE);
}
public void decodeIndexKey(char[] key) {
	this.name = key;
}
protected StringBuffer print(StringBuffer output) {
	if (this.findDeclarations) {
		output.append(this.findReferences
			? "LocalVarCombinedPattern: " //$NON-NLS-1$
			: "LocalVarDeclarationPattern: "); //$NON-NLS-1$
	} else {
		output.append("LocalVarReferencePattern: "); //$NON-NLS-1$
	}
	output.append(this.localVariable.toStringWithAncestors());
	return super.print(output);
}
}
