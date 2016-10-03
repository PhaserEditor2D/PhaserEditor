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
package org.eclipse.wst.jsdt.internal.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJarEntryResource;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * A package fragment that represents a package fragment found in a JAR.
 *
 * @see org.eclipse.jdt.core.IPackageFragment
 */
class JarPackageFragment extends PackageFragment {
/**
 * Constructs a package fragment that is contained within a jar or a zip.
 */
protected JarPackageFragment(PackageFragmentRoot root, String[] names) {
	super(root, names);
}
/**
 * @see Openable
 */
protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaScriptModelException {
	JarPackageFragmentRoot root = (JarPackageFragmentRoot) getParent();
	JarPackageFragmentRootInfo parentInfo = (JarPackageFragmentRootInfo) root.getElementInfo();
	ArrayList[] entries = (ArrayList[]) parentInfo.rawPackageInfo.get(this.names);
	if (entries == null)
		throw newNotPresentException();
	JarPackageFragmentInfo fragInfo = (JarPackageFragmentInfo) info;

	// compute children
	fragInfo.setChildren(computeChildren(entries[0/*class files*/]));

	// compute non-Java resources
	fragInfo.setNonJavaResources(computeNonJavaResources(entries[1/*non Java resources*/]));

	newElements.put(this, fragInfo);
	return true;
}
/**
 * Compute the children of this package fragment. Children of jar package fragments
 * can only be IClassFile (representing .class files).
 */
private IJavaScriptElement[] computeChildren(ArrayList names) {
	int size = names.size();
	if (size == 0 && false) {
		File file = getPath().toFile();
		if (!file.isFile()) {
			IPath path = ResourcesPlugin.getWorkspace().getRoot().getFile(getPath()).getLocation();
			if (path != null)
				file = path.toFile();
		}

		if (file.isFile()) {
			String osPath = getPath().toOSString();
			List classFiles = new ArrayList();
			ZipFile zip = null;
			try {
				zip = new ZipFile(file);
				for (Enumeration e = zip.entries(); e.hasMoreElements();) {
					ZipEntry ze = (ZipEntry) e.nextElement();
					if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(ze.getName())) {
						classFiles.add(new ClassFile(this, ze.getName()));
					}
				}
			}
			catch (ZipException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			finally {
				if (zip != null) {
					try {
						zip.close();
					}
					catch (IOException e) {
						// do nothing
					}
				}
			}
			return (IJavaScriptElement[]) classFiles.toArray(new ClassFile[classFiles.size()]);
		}
	}
	IJavaScriptElement[] children = new IJavaScriptElement[size];
	for (int i = 0; i < size; i++) {
		String path = (String) names.get(i);
		children[i] = new ClassFile(this, path);
	}
	return children;
}
/**
 * Compute all the non-java resources according to the given entry names.
 */
private Object[] computeNonJavaResources(ArrayList entryNames) {
	int length = entryNames.size();
	if (length == 0)
		return JavaElementInfo.NO_NON_JAVA_RESOURCES;
	HashMap jarEntries = new HashMap(); // map from IPath to IJarEntryResource
	HashMap childrenMap = new HashMap(); // map from IPath to ArrayList<IJarEntryResource>
	ArrayList topJarEntries = new ArrayList();
	for (int i = 0; i < length; i++) {
		String resName = (String) entryNames.get(i);
		// consider that a .java file is not a non-java resource (see bug 12246 Packages view shows .class and .java files when JAR has source)
		if (!Util.isJavaLikeFileName(resName)) {
			IPath filePath = new Path(resName);
			IPath childPath = filePath.removeFirstSegments(this.names.length);
			if (jarEntries.containsKey(childPath)) {
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=222665
				continue;
			}
			JarEntryFile file = new JarEntryFile(filePath.lastSegment());
			jarEntries.put(childPath, file);
			if (childPath.segmentCount() == 1) {
				file.setParent(this);
				topJarEntries.add(file);
			} else {
				IPath parentPath = childPath.removeLastSegments(1);
				while (parentPath.segmentCount() > 0) {
					ArrayList parentChildren = (ArrayList) childrenMap.get(parentPath);
					if (parentChildren == null) {
						Object dir = new JarEntryDirectory(parentPath.lastSegment());
						jarEntries.put(parentPath, dir);
						childrenMap.put(parentPath, parentChildren = new ArrayList());
						parentChildren.add(childPath);
						if (parentPath.segmentCount() == 1) {
							topJarEntries.add(dir);
							break;
						}
						childPath = parentPath;
						parentPath = childPath.removeLastSegments(1);
					} else {
						parentChildren.add(childPath);
						break; // all parents are already registered
					}
				}
			}
		}
	}
	Iterator entries = childrenMap.entrySet().iterator();
	while (entries.hasNext()) {
		Map.Entry entry = (Map.Entry) entries.next();
		IPath entryPath = (IPath) entry.getKey();
		ArrayList entryValue =  (ArrayList) entry.getValue();
		JarEntryDirectory jarEntryDirectory = (JarEntryDirectory) jarEntries.get(entryPath);
		int size = entryValue.size();
		IJarEntryResource[] children = new IJarEntryResource[size];
		for (int i = 0; i < size; i++) {
			JarEntryResource child = (JarEntryResource) jarEntries.get(entryValue.get(i));
			child.setParent(jarEntryDirectory);
			children[i] = child;
		}
		jarEntryDirectory.setChildren(children);
		if (entryPath.segmentCount() == 1) {
			jarEntryDirectory.setParent(this);
		}
	}
	return topJarEntries.toArray(new Object[topJarEntries.size()]);
}
/**
 * Returns true if this fragment contains at least one java resource.
 * Returns false otherwise.
 */
public boolean containsJavaResources() throws JavaScriptModelException {
	return ((JarPackageFragmentInfo) getElementInfo()).containsJavaResources();
}
/**
 * @see org.eclipse.jdt.core.IPackageFragment
 */
public IJavaScriptUnit createCompilationUnit(String cuName, String contents, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
/**
 * @see JavaElement
 */
protected Object createElementInfo() {
	return new JarPackageFragmentInfo();
}
/**
 * @see org.eclipse.jdt.core.IPackageFragment
 */
public IClassFile[] getClassFiles() throws JavaScriptModelException {
	ArrayList list = getChildrenOfType(CLASS_FILE);
	IClassFile[] array= new IClassFile[list.size()];
	list.toArray(array);
	return array;
}
/**
 * @see IPackageFragment#getClassFile(String)
 * @exception IllegalArgumentException if the name does not end with ".class"
 */
public IClassFile getClassFile(String classFileName) {
	if (!org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(classFileName)
			&& !Util.isMetadataFileName(classFileName)) {
		throw new IllegalArgumentException(Messages.element_invalidClassFileName);
	}
	// don't hold on the .class file extension to save memory
	// also make sure to not use substring as the resulting String may hold on the underlying char[] which might be much bigger than necessary
//	int length = classFileName.length() - SUFFIX_CLASS.length;
//	char[] nameWithoutExtension = new char[length];
//	classFileName.getChars(0, length, nameWithoutExtension, 0);
	String filename= classFileName;
//	if (this.getResource()!=null && this.getResource().getLocation() != null)
//		filename= this.getResource().getLocation().toOSString()+IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR+classFileName;
//	else 
//		filename=getPath().toString() + IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR + classFileName;
	
	return (!Util.isMetadataFileName(classFileName)) ? (IClassFile)new ClassFile(this,filename) : (IClassFile)new MetadataFile(this,filename);
}
/**
 * A jar package fragment never contains compilation units.
 * @see org.eclipse.jdt.core.IPackageFragment
 */
public IJavaScriptUnit[] getCompilationUnits() {
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
public Object[] getNonJavaResources() throws JavaScriptModelException {
	if (isDefaultPackage()) {
		// We don't want to show non java resources of the default package (see PR #1G58NB8)
		return JavaElementInfo.NO_NON_JAVA_RESOURCES;
	} else {
		return storedNonJavaResources();
	}
}
/**
 * Jars and jar entries are all read only
 */
public boolean isReadOnly() {
	return true;
}
protected Object[] storedNonJavaResources() throws JavaScriptModelException {
	return ((JarPackageFragmentInfo) getElementInfo()).getNonJavaResources();
}
}
