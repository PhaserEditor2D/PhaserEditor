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
package org.eclipse.wst.jsdt.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * A package fragment that represents a package fragment found in a JAR.
 *
 * @see org.eclipse.wst.jsdt.core.IPackageFragment
 */
public class LibraryPackageFragment extends PackageFragment implements SuffixConstants {
final static String[]DEFAULT_PACKAGE={""}; //$NON-NLS-1$
/**
 * Constructs a package fragment that is contained within a jar or a zip.
 */
protected LibraryPackageFragment(PackageFragmentRoot root, String[] names) {
	super(root, names);
}
/**
 * Compute the children of this package fragment. Children of jar package fragments
 * can only be IClassFile (representing .class files).
 */
protected boolean computeChildren(OpenableElementInfo info) {
	String name = this.getPackageFragmentRoot().getPath().toOSString();
		ClassFile classFile = new ClassFile(this,name);
//	JavaScriptUnit cu= new JavaScriptUnit(this, this.getPackageFragmentRoot().getPath().toOSString(), DefaultWorkingCopyOwner.PRIMARY);
		IJavaScriptElement[] children= new IJavaScriptElement[]{classFile};
		info.setChildren(children);
		return true;
}

/**
 * Returns true if this fragment contains at least one java resource.
 * Returns false otherwise.
 */
public boolean containsJavaResources() throws JavaScriptModelException {
	return true;
}
/**
 * @see org.eclipse.wst.jsdt.core.IPackageFragment
 */
public IJavaScriptUnit createCompilationUnit(String cuName, String contents, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
/**
 * @see JavaElement
 */
protected Object createElementInfo() {
	return new LibraryPackageFragmentInfo();  
}
/*
 * @see JavaElement#generateInfos
 */
protected void generateInfos(Object info, HashMap newElements, IProgressMonitor pm) throws JavaScriptModelException {
	// Open my jar: this creates all the pkg infos
	
	if (!getLibraryFragmentRoot().isDirectory())
	{
		Openable openableParent = (Openable)this.parent;
		if (!openableParent.isOpen()) {
			openableParent.generateInfos(openableParent.createElementInfo(), newElements, pm);
		}
	}
	else 
		super.generateInfos(info, newElements, pm);
}


protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaScriptModelException {

	if (!getLibraryFragmentRoot().isDirectory())
		return true;	// already built
	// check whether this pkg can be opened

	// add compilation units/class files from resources
	HashSet vChildren = new HashSet();
	try {
	    File file = getUnderlyingFile();
		File[] members = file.listFiles();
		if (members != null && members.length > 0) {
			for (int i = 0; i < members.length; i++) {
				File child = members[i];
				if (child.isFile()) {
					IJavaScriptElement childElement;
					if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isJavaFileName(child.getName())) {
						childElement = getClassFile(child.getName());
						vChildren.add(childElement);
					} else if (Util.isMetadataFileName(child.getName())) {
						childElement = getClassFile(child.getName());
						vChildren.add(childElement);
					}
				}
			}
		}
	} catch (CoreException e) {
		throw new JavaScriptModelException(e);
	}


	IJavaScriptElement[] children = new IJavaScriptElement[vChildren.size()];
	vChildren.toArray(children);
	info.setChildren(children);
	return true;
}



/**
 * @see org.eclipse.wst.jsdt.core.IPackageFragment
 */
public IClassFile[] getClassFiles() throws JavaScriptModelException {
	ArrayList list = getChildrenOfType(CLASS_FILE);
	IClassFile[] array= new IClassFile[list.size()];
	list.toArray(array);
	return array;
}
/**
 * A jar package fragment never contains compilation units.
 * @see org.eclipse.wst.jsdt.core.IPackageFragment
 */
public IJavaScriptUnit[] getJavaScriptUnits() {
	return NO_COMPILATION_UNITS;
}
/**
 * A package fragment in a jar has no corresponding resource.
 *
 * @see IJavaScriptElement
 */
public IResource getCorrespondingResource() {
	return null;
}
/**
 * Returns an array of non-java resources contained in the receiver.
 */
public Object[] getNonJavaScriptResources() throws JavaScriptModelException {
	if (this.isDefaultPackage()) {
		// We don't want to show non java resources of the default package (see PR #1G58NB8)
		return JavaElementInfo.NO_NON_JAVA_RESOURCES;
	} else {
		return this.storedNonJavaResources();
	}
}
/**
 * Jars and jar entries are all read only
 */
public boolean isReadOnly() {
	return true;
}
 static final Object[] NO_OBJECTS={};
protected Object[] storedNonJavaResources() throws JavaScriptModelException {
	return NO_OBJECTS;
}

protected boolean resourceExists() {
		return getPackageFragmentRoot().resourceExists();
}

protected LibraryFragmentRoot getLibraryFragmentRoot()
{
	return (LibraryFragmentRoot) getPackageFragmentRoot();
}

public IClassFile getClassFile(String classFileName) {
	if (!org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(classFileName)
			&& !Util.isMetadataFileName(classFileName)) {
		throw new IllegalArgumentException(Messages.element_invalidClassFileName);
	}

	LibraryFragmentRoot libraryFragmentRoot = getLibraryFragmentRoot();
	if (!libraryFragmentRoot.isDirectory())
	{
		IPath path = getLibraryFragmentRoot().getPath();
//		if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(path.lastSegment().toCharArray())
//				&& path.lastSegment().equalsIgnoreCase(classFileName))
//		{
		if(path.toOSString().endsWith(classFileName)
				|| path.isPrefixOf(new Path(classFileName))) {
			return new ClassFile(this, path.toOSString());
		}
	}
	else
	{
		String filename= "";
		if (this.getFile()!=null)
		  filename= this.getFile().getAbsolutePath()+File.separator+classFileName;
		else 
			filename=classFileName;
		
		return (!Util.isMetadataFileName(classFileName)) ? (IClassFile)new ClassFile(this,filename) : (IClassFile)new MetadataFile(this,filename);

	}
	return super.getClassFile(classFileName);
}
public String getDisplayName() {
	if(parent instanceof LibraryFragmentRoot) {
		JsGlobalScopeContainerInitializer initializer = ((LibraryFragmentRoot)parent).getContainerInitializer();
		if(initializer==null) return getPath().removeLastSegments(1).toString();
		String name = initializer.getDescription(getPath(), getJavaScriptProject());
		if(name!=null) return name;
	}
	return  parent.getPath().lastSegment();
}

public File getUnderlyingFile() throws JavaScriptModelException {
	File file = getLibraryFragmentRoot().getFile();
	if (file == null) {
		return null;
	}
	if (this.names.length == 0) 
		return file;
	// the underlying resource may be a folder or a project (in the case that the project folder
	// is atually the package fragment root)
		String[] segs = this.names;
		for (int i = 0; i < segs.length; ++i) {
			File child = new File(file,segs[i]);
			if (!child.exists() ) {
				throw newNotPresentException();
			}
			file = child;
		}
		return file;
}

public File getFile() {
	File file = getLibraryFragmentRoot().getFile();
		int length = this.names.length;
		if (length == 0) {
			return file;
		} else {
			String[] segs = this.names;
			for (int i = 0; i < segs.length; ++i) {
				File child = new File(file,segs[i]);
				if (!child.exists() ) {
					return null;
				}
				file = child;
			}
			return file;
		}
}

}
