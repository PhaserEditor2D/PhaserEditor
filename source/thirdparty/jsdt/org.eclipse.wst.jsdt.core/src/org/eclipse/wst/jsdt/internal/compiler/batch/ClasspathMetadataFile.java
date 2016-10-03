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
package org.eclipse.wst.jsdt.internal.compiler.batch;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.eclipse.wst.jsdt.core.UnimplementedException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.oaametadata.LibraryAPIs;
import org.eclipse.wst.jsdt.internal.oaametadata.MetadataReader;

public class ClasspathMetadataFile extends ClasspathLocation {

protected File file;
protected Hashtable packageCache;
String packageName;
protected char[] normalizedPath;
String encoding;
LibraryAPIs apis;

NameEnvironmentAnswer foundAnswer;

public ClasspathMetadataFile(File file, String encoding,
		AccessRuleSet accessRuleSet, String destinationPath) {
	super(accessRuleSet,destinationPath);
	this.file = file;
	this.packageName=file.getName();
	this.encoding=encoding;
}

public NameEnvironmentAnswer findBinding(char[] typeName, String qualifiedPackageName, int type, ITypeRequestor requestor) {
//	if (!qualifiedPackageName.equals(this.packageName))
	//	return null;
	if (foundAnswer==null)
	{
		parseFile(requestor);
	}
	if (this.apis==null)
		return null;
	String name = new String(typeName);
	if ((type&(Binding.VARIABLE|Binding.FIELD))!=0)
	{
		if (this.apis.getGlobalVar(name)!=null)
			return foundAnswer;
	}
	if ((type&Binding.TYPE)!=0)
	{
		if (this.apis.getClass(name)!=null)
			return foundAnswer;
	}
	if ((type&Binding.METHOD)!=0)
	{
		if (this.apis.getGlobalMethod(name)!=null)
			return foundAnswer;
	}
	return null;
	}

private void parseFile(ITypeRequestor requestor) {
	CompilationUnit compilationUnit = new CompilationUnit(null,
			file.getAbsolutePath(), this.encoding);
	compilationUnit.packageName=new char [][]{packageName.toCharArray()};



		apis=MetadataReader.readAPIsFromFile(this.file.getAbsolutePath());
		foundAnswer =
			 new NameEnvironmentAnswer(apis);

}

public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String qualifiedBinaryFileName) {
	return findClass(typeName,qualifiedPackageName,qualifiedBinaryFileName,false);
}
public NameEnvironmentAnswer findClass(char[] typeName,
			String qualifiedPackageName, String qualifiedBinaryFileName,
			boolean asBinaryOnly) {
	if (!isPackage(qualifiedPackageName))
		return null; // most common case
	throw new org.eclipse.wst.jsdt.core.UnimplementedException();

//	return null;
}
public void initialize() throws IOException {
}
public boolean isPackage(String qualifiedPackageName) {
		return packageName.equals(qualifiedPackageName);
}
public void reset() {

	this.packageCache = null;
}
public String toString() {
	return "Classpath for file " + this.file.getPath(); //$NON-NLS-1$
}
public char[] normalizedPath() {
	if (this.normalizedPath == null) {
		char[] rawName = this.file.getPath().toCharArray();
		if (File.separatorChar == '\\') {
			CharOperation.replace(rawName, '\\', '/');
		}
		this.normalizedPath = CharOperation.subarray(rawName, 0, CharOperation.lastIndexOf('.', rawName));
	}
	return this.normalizedPath;
}
public String getPath(){
	return this.file.getPath();
}

public char[][][] findTypeNames(String qualifiedPackageName) {
	throw new UnimplementedException("implement"); //$NON-NLS-1$
}
}
