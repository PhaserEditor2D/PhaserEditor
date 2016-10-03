/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tal Lev-Ami - added package cache for zip files
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.builder;

import java.io.File;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class ClasspathLibrary extends ClasspathLocation {



String filename; // keep for equals
IFile resource;
long lastModified;
boolean closeZipFileAtEnd;
SimpleSet knownPackageNames;
AccessRuleSet accessRuleSet;

ClasspathLibrary(IFile resource, AccessRuleSet accessRuleSet) {
	this.resource = resource;
	try {
		java.net.URI location = resource.getLocationURI();
		if (location == null) {
			this.filename = ""; //$NON-NLS-1$
		} else {
			File localFile = Util.toLocalFile(location, null);
			this.filename = localFile.getPath();
		}
	} catch (CoreException e) {
		// ignore
	}
	this.knownPackageNames = null;
	this.accessRuleSet = accessRuleSet;
}

ClasspathLibrary(String filename, long lastModified, AccessRuleSet accessRuleSet) {
	this.filename = filename;
	this.lastModified = lastModified;
	this.knownPackageNames = null;
	this.accessRuleSet = accessRuleSet;
}


public void cleanup() {
	this.knownPackageNames = null;
}

public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof ClasspathLibrary)) return false;

	ClasspathLibrary library = (ClasspathLibrary) o;
	if (this.accessRuleSet != library.accessRuleSet)
		if (this.accessRuleSet == null || !this.accessRuleSet.equals(library.accessRuleSet))
			return false;
	return this.filename.equals(library.filename) && this.lastModified() == library.lastModified();
}

public NameEnvironmentAnswer findClass(String binaryFileName, String qualifiedPackageName, String qualifiedBinaryFileName) {
//	if (!isPackage(qualifiedPackageName)) return null; // most common case
//
//	try {
//		ClassFileReader reader = ClassFileReader.read(this.zipFile, qualifiedBinaryFileName);
//		if (reader != null) {
//			if (this.accessRuleSet == null)
//				return new NameEnvironmentAnswer(reader, null);
//			String fileNameWithoutExtension = qualifiedBinaryFileName.substring(0, qualifiedBinaryFileName.length() - SuffixConstants.SUFFIX_CLASS.length);
//			return new NameEnvironmentAnswer(reader, this.accessRuleSet.getViolatedRestriction(fileNameWithoutExtension.toCharArray()));
//		}
//	} catch (Exception e) { // treat as if class file is missing
//	}
//	return null;
	//TODO: implement
	throw new org.eclipse.wst.jsdt.core.UnimplementedException();
}

public IPath getProjectRelativePath() {
	if (this.resource == null) return null;
	return	this.resource.getProjectRelativePath();
}

public boolean isPackage(String qualifiedPackageName) {
	return this.filename.endsWith(qualifiedPackageName);
//	if (this.knownPackageNames != null)
//		return this.knownPackageNames.includes(qualifiedPackageName);
//
//	try {
//		if (this.zipFile == null) {
//			if (org.eclipse.wst.jsdt.internal.core.JavaModelManager.ZIP_ACCESS_VERBOSE) {
//				System.out.println("(" + Thread.currentThread() + ") [ClasspathJar.isPackage(String)] Creating ZipFile on " + zipFilename); //$NON-NLS-1$	//$NON-NLS-2$
//			}
//			this.zipFile = new ZipFile(zipFilename);
//			this.closeZipFileAtEnd = true;
//		}
//		this.knownPackageNames = findPackageSet(this);
//	} catch(Exception e) {
//		this.knownPackageNames = new SimpleSet(); // assume for this build the zipFile is empty
//	}
//	return this.knownPackageNames.includes(qualifiedPackageName);
}

public long lastModified() {
	if (this.lastModified == 0)
		this.lastModified = new File(this.filename).lastModified();
	return this.lastModified;
}

public String toString() {
	String start = "Classpath library file " + this.filename; //$NON-NLS-1$
	if (this.accessRuleSet == null)
		return start;
	return start + " with " + this.accessRuleSet; //$NON-NLS-1$
}

public String debugPathString() {
	if (this.lastModified == 0)
		return this.filename;
	return this.filename + '(' + (new Date(this.lastModified)) + " : " + this.lastModified + ')'; //$NON-NLS-1$
}

}
