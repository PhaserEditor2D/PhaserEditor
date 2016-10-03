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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * @see IPackageFragmentRoot
 */
public class PackageFragmentRoot extends Openable implements IPackageFragmentRoot,  IVirtualParent  {

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.JavaElement#isVirtual()
	 */
	public boolean isVirtual() {
		return false;
	}

	/**
	 * The delimiter between the source path and root path in the
	 * attachment server property.
	 */
	protected final static char ATTACHMENT_PROPERTY_DELIMITER= '*';
	/*
	 * No source attachment property
	 */
	public final static String NO_SOURCE_ATTACHMENT = ""; //$NON-NLS-1$

	/**
	 * The resource associated with this root.
	 * (an IResource or a java.io.File (for external jar only))
	 */
	protected Object resource;

/**
 * Constructs a package fragment root which is the root of the java package
 * directory hierarchy.
 */
protected PackageFragmentRoot(IResource resource, JavaProject project) {
	super(project);
	this.resource = resource;
}

/**
 * @see IPackageFragmentRoot
 */
public void attachSource(IPath sourcePath, IPath rootPath, IProgressMonitor monitor) throws JavaScriptModelException {
	try {
		verifyAttachSource(sourcePath);
		if (monitor != null) {
			monitor.beginTask(Messages.element_attachingSource, 2);
		}
		SourceMapper oldMapper= getSourceMapper();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		boolean rootNeedsToBeClosed= false;

		if (sourcePath == null) {
			//source being detached
			rootNeedsToBeClosed= true;
			setSourceMapper(null);
		/* Disable deltas (see 1GDTUSD)
			// fire a delta to notify the UI about the source detachement.
			JavaModelManager manager = (JavaModelManager) JavaModelManager.getJavaModelManager();
			JavaModel model = (JavaModel) getJavaModel();
			JavaElementDelta attachedSourceDelta = new JavaElementDelta(model);
			attachedSourceDelta .sourceDetached(this); // this would be a PackageFragmentRoot
			manager.registerResourceDelta(attachedSourceDelta );
			manager.fire(); // maybe you want to fire the change later. Let us know about it.
		*/
		} else {
		/*
			// fire a delta to notify the UI about the source attachement.
			JavaModelManager manager = (JavaModelManager) JavaModelManager.getJavaModelManager();
			JavaModel model = (JavaModel) getJavaModel();
			JavaElementDelta attachedSourceDelta = new JavaElementDelta(model);
			attachedSourceDelta .sourceAttached(this); // this would be a PackageFragmentRoot
			manager.registerResourceDelta(attachedSourceDelta );
			manager.fire(); // maybe you want to fire the change later. Let us know about it.
		 */

			//check if different from the current attachment
			IPath storedSourcePath= getSourceAttachmentPath();
			IPath storedRootPath= getSourceAttachmentRootPath();
			if (monitor != null) {
				monitor.worked(1);
			}
			if (storedSourcePath != null) {
				if (!(storedSourcePath.equals(sourcePath) && (rootPath != null && rootPath.equals(storedRootPath)) || storedRootPath == null)) {
					rootNeedsToBeClosed= true;
				}
			}
			// check if source path is valid
			Object target = JavaModel.getTarget(workspace.getRoot(), sourcePath, false);
			if (target == null) {
				throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_PATH, sourcePath));
			}
			SourceMapper mapper = createSourceMapper(sourcePath, rootPath);
			if (rootPath == null && mapper.rootPath != null) {
				// as a side effect of calling the SourceMapper constructor, the root path was computed
				rootPath = new Path(mapper.rootPath);
			}
			setSourceMapper(mapper);
		}
		if (sourcePath == null) {
			Util.setSourceAttachmentProperty(getPath(), null); //remove the property
		} else {
			//set the property to the path of the mapped source
			Util.setSourceAttachmentProperty(
				getPath(),
				sourcePath.toString()
				+ (rootPath == null ? "" : (ATTACHMENT_PROPERTY_DELIMITER + rootPath.toString()))); //$NON-NLS-1$
		}
		if (rootNeedsToBeClosed) {
			if (oldMapper != null) {
				oldMapper.close();
			}
			BufferManager manager= BufferManager.getDefaultBufferManager();
			Enumeration openBuffers= manager.getOpenBuffers();
			while (openBuffers.hasMoreElements()) {
				IBuffer buffer= (IBuffer) openBuffers.nextElement();
				IOpenable possibleMember= buffer.getOwner();
				if (isAncestorOf((IJavaScriptElement) possibleMember)) {
					buffer.close();
				}
			}
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	} catch (JavaScriptModelException e) {
		Util.setSourceAttachmentProperty(getPath(), null); // loose info - will be recomputed
		throw e;
	} finally {
		if (monitor != null) {
			monitor.done();
		}
	}
}

/**
 * @see Openable
 */
protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaScriptModelException {

	// check whether this pkg fragment root can be opened
	IStatus status = validateOnClasspath();
	if (!status.isOK()) throw newJavaModelException(status);
	if (!isExternal() && !resourceExists()) throw newNotPresentException();

	((PackageFragmentRootInfo) info).setRootKind(determineKind(underlyingResource));
	return computeChildren(info, newElements);
}

SourceMapper createSourceMapper(IPath sourcePath, IPath rootPath) {
	SourceMapper mapper = new SourceMapper(
		sourcePath,
		rootPath == null ? null : rootPath.toOSString(),
		getJavaScriptProject().getOptions(true)); // cannot use workspace options if external jar is 1.5 jar and workspace options are 1.4 options
	return mapper;
}
/*
 * @see org.eclipse.wst.jsdt.core.IPackageFragmentRoot#delete
 */
public void delete(
	int updateResourceFlags,
	int updateModelFlags,
	IProgressMonitor monitor)
	throws JavaScriptModelException {

	DeletePackageFragmentRootOperation op = new DeletePackageFragmentRootOperation(this, updateResourceFlags, updateModelFlags);
	op.runOperation(monitor);
}

/**
 * Compute the package fragment children of this package fragment root.
 *
 * @exception JavaScriptModelException  The resource associated with this package fragment root does not exist
 */
protected boolean computeChildren(OpenableElementInfo info, Map newElements) throws JavaScriptModelException {
	// Note the children are not opened (so not added to newElements) for a regular package fragment root
	// Howver they are opened for a Jar package fragment root (see JarPackageFragmentRoot#computeChildren)
	try {
		// the underlying resource may be a folder or a project (in the case that the project folder
		// is actually the package fragment root)
		IResource underlyingResource = getResource();
		if (underlyingResource.getType() == IResource.FOLDER || underlyingResource.getType() == IResource.PROJECT) {
			ArrayList vChildren = new ArrayList(5);
			IContainer rootFolder = (IContainer) underlyingResource;
			char[][] inclusionPatterns = fullInclusionPatternChars();
			char[][] exclusionPatterns = fullExclusionPatternChars();
			computeFolderChildren(rootFolder, !Util.isExcluded(rootFolder, inclusionPatterns, exclusionPatterns), CharOperation.NO_STRINGS, vChildren, inclusionPatterns, exclusionPatterns);
			IJavaScriptElement[] children = new IJavaScriptElement[vChildren.size()];
			vChildren.toArray(children);
			info.setChildren(children);
		}
	} catch (JavaScriptModelException e) {
		//problem resolving children; structure remains unknown
		info.setChildren(new IJavaScriptElement[]{});
		throw e;
	}
	return true;
}

/**
 * Starting at this folder, create package fragments and add the fragments that are not exclused
 * to the collection of children.
 *
 * @exception JavaScriptModelException  The resource associated with this package fragment does not exist
 */
protected void computeFolderChildren(IContainer folder, boolean isIncluded, String[] pkgName, ArrayList vChildren, char[][] inclusionPatterns, char[][] exclusionPatterns) throws JavaScriptModelException {

	if (isIncluded) {
	    IPackageFragment pkg = getPackageFragment(pkgName);
		vChildren.add(pkg);
	}
	try {
		JavaProject javaProject = (JavaProject)getJavaScriptProject();
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		IResource[] members = folder.members();
		boolean hasIncluded = isIncluded;
		int length = members.length;
		if (length >0) {
			String sourceLevel = javaProject.getOption(JavaScriptCore.COMPILER_SOURCE, true);
			String complianceLevel = javaProject.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
			for (int i = 0; i < length; i++) {
				IResource member = members[i];
				String memberName = member.getName();

				switch(member.getType()) {

			    	case IResource.FOLDER:
			    		// recurse into sub folders even even parent not included as a sub folder could be included
			    		// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=65637)
			    		if (Util.isValidFolderNameForPackage(memberName, sourceLevel, complianceLevel)) {
			    			// eliminate binary output only if nested inside direct subfolders
			    			if (javaProject.contains(member)) {
			    				String[] newNames = Util.arrayConcat(pkgName, manager.intern(memberName));
			    				boolean isMemberIncluded = !Util.isExcluded(member, inclusionPatterns, exclusionPatterns);
			    				computeFolderChildren((IFolder) member, isMemberIncluded, newNames, vChildren, inclusionPatterns, exclusionPatterns);
			    			}
			    		}
			    		break;
			    	case IResource.FILE:
			    		// inclusion filter may only include files, in which case we still want to include the immediate parent package (lazily)
			    		if (!hasIncluded
			    				&& Util.isValidCompilationUnitName(memberName, sourceLevel, complianceLevel)
								&& !Util.isExcluded(member, inclusionPatterns, exclusionPatterns)) {
			    			hasIncluded = true;
			    			IPackageFragment pkg = getPackageFragment(pkgName);
			    			vChildren.add(pkg);
			    		}
			    		break;
				}
			}
		}
	} catch(IllegalArgumentException e){
		throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST); // could be thrown by ElementTree when path is not found
	} catch (CoreException e) {
		throw new JavaScriptModelException(e);
	}
}

/*
 * @see org.eclipse.wst.jsdt.core.IPackageFragmentRoot#copy
 */
public void copy(
	IPath destination,
	int updateResourceFlags,
	int updateModelFlags,
	IIncludePathEntry sibling,
	IProgressMonitor monitor)
	throws JavaScriptModelException {

	CopyPackageFragmentRootOperation op =
		new CopyPackageFragmentRootOperation(this, destination, updateResourceFlags, updateModelFlags, sibling);
	op.runOperation(monitor);
}

/**
 * Returns a new element info for this element.  Subclasses MUST return a subclass of PackageFragmentRootInfo
 */
protected Object createElementInfo() {
	return new PackageFragmentRootInfo();
}

/**
 * @see IPackageFragmentRoot
 */
public IPackageFragment createPackageFragment(String pkgName, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	CreatePackageFragmentOperation op = new CreatePackageFragmentOperation(this, pkgName, force);
	op.runOperation(monitor);
	return getPackageFragment(op.pkgName);
}

/**
 * Returns the root's kind - K_SOURCE or K_BINARY, defaults
 * to K_SOURCE if it is not on the classpath.
 *
 * @exception JavaScriptModelException if the project and root do
 * 		not exist.
 */
protected int determineKind(IResource underlyingResource) throws JavaScriptModelException {
	IIncludePathEntry entry = ((JavaProject)getJavaScriptProject()).getClasspathEntryFor(underlyingResource.getFullPath());
	if (entry != null) {
		return entry.getContentKind();
	}
	return org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(underlyingResource.getName()) ? IPackageFragmentRoot.K_BINARY : IPackageFragmentRoot.K_SOURCE;
}

/**
 * Compares two objects for equality;
 * for <code>PackageFragmentRoot</code>s, equality is having the
 * same parent, same resources, and occurrence count.
 *
 */
public boolean equals(Object o) {
	if (this == o)
		return true;
	if (!(o instanceof PackageFragmentRoot))
		return false;
	PackageFragmentRoot other = (PackageFragmentRoot) o;
	return this.resource.equals(other.resource) &&
			this.parent.equals(other.parent);
}

/**
 * @see IJavaScriptElement
 */
public boolean exists() {
	return super.exists() && validateOnClasspath().isOK();
}

private IIncludePathEntry findSourceAttachmentRecommendation() {
	try {
		IPath rootPath = this.getPath();
		IIncludePathEntry entry;
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

		// try on enclosing project first
		JavaProject parentProject = (JavaProject) getJavaScriptProject();
		try {
			entry = parentProject.getClasspathEntryFor(rootPath);
			if (entry != null){
				Object target = JavaModel.getTarget(workspaceRoot, entry.getSourceAttachmentPath(), true);
				if (target instanceof IResource) {
					if (target instanceof IFile) {
						IFile file = (IFile) target;
						if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(file.getName())){
							return entry;
						}
					} else if (target instanceof IContainer) {
						return entry;
					}
				} else if (target instanceof java.io.File){
					java.io.File file = JavaModel.getFile(target);
					if (file != null) {
						if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(file.getName())){
							return entry;
						}
					} else {
						// external directory
						return entry;
					}
				}
			}
		} catch(JavaScriptModelException e){
			// ignore
		}

		// iterate over all projects
		IJavaScriptModel model = getJavaScriptModel();
		IJavaScriptProject[] jProjects = model.getJavaScriptProjects();
		for (int i = 0, max = jProjects.length; i < max; i++){
			JavaProject jProject = (JavaProject) jProjects[i];
			if (jProject == parentProject) continue; // already done
			try {
				entry = jProject.getClasspathEntryFor(rootPath);
				if (entry != null){
					Object target = JavaModel.getTarget(workspaceRoot, entry.getSourceAttachmentPath(), true);
					if (target instanceof IResource) {
						if (target instanceof IFile){
							IFile file = (IFile) target;
							if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(file.getName())){
								return entry;
							}
						} else if (target instanceof IContainer) {
							return entry;
						}
					} else if (target instanceof java.io.File){
						java.io.File file = (java.io.File) target;
						if (file.isFile()) {
							if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(file.getName())){
								return entry;
							}
						} else {
							// external directory
							return entry;
						}
					}
				}
			} catch(JavaScriptModelException e){
				// ignore
			}
		}
	} catch(JavaScriptModelException e){
		// ignore
	}

	return null;
}

/*
 * Returns the exclusion patterns from the classpath entry associated with this root.
 */
public char[][] fullExclusionPatternChars() {
	try {
		if (this.isOpen() && this.getKind() != IPackageFragmentRoot.K_SOURCE) return null;
		ClasspathEntry entry = (ClasspathEntry)getRawIncludepathEntry();
		if (entry == null) {
			return null;
		} else {
			return entry.fullExclusionPatternChars();
		}
	} catch (JavaScriptModelException e) {
		return null;
	}
}

/*
 * Returns the inclusion patterns from the classpath entry associated with this root.
 */
public char[][] fullInclusionPatternChars() {
	try {
		if (this.isOpen() && this.getKind() != IPackageFragmentRoot.K_SOURCE) return null;
		ClasspathEntry entry = (ClasspathEntry)getRawIncludepathEntry();
		if (entry == null) {
			return null;
		} else {
			return entry.fullInclusionPatternChars();
		}
	} catch (JavaScriptModelException e) {
		return null;
	}
}
public String getElementName() {
	if (this.resource instanceof IFolder)
		return ((IFolder) this.resource).getName();
	return ""; //$NON-NLS-1$
}
/**
 * @see IJavaScriptElement
 */
public int getElementType() {
	return PACKAGE_FRAGMENT_ROOT;
}
/**
 * @see JavaElement#getHandleMemento()
 */
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_PACKAGEFRAGMENTROOT;
}
/*
 * @see JavaElement
 */
public IJavaScriptElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
	switch (token.charAt(0)) {
		case JEM_PACKAGEFRAGMENT:
			String pkgName;
			if (memento.hasMoreTokens()) {
				pkgName = memento.nextToken();
				char firstChar = pkgName.charAt(0);
				if (firstChar == JEM_CLASSFILE || firstChar == JEM_COMPILATIONUNIT || firstChar == JEM_COUNT) {
					token = pkgName;
					pkgName = IPackageFragment.DEFAULT_PACKAGE_NAME;
				} else {
					token = null;
				}
			} else {
				pkgName = IPackageFragment.DEFAULT_PACKAGE_NAME;
				token = null;
			}
			JavaElement pkg = (JavaElement)getPackageFragment(pkgName);
			if (token == null) {
				return pkg.getHandleFromMemento(memento, owner);
			} else {
				return pkg.getHandleFromMemento(token, memento, owner);
			}
	}
	return null;
}
/**
 * @see JavaElement#getHandleMemento(StringBuffer)
 */
protected void getHandleMemento(StringBuffer buff) {
	IPath path;
	IResource underlyingResource = getResource();
	if (underlyingResource != null) {
		// internal jar or regular root
		if (getResource().getProject().equals(getJavaScriptProject().getProject())) {
			path = underlyingResource.getProjectRelativePath();
		} else {
			path = underlyingResource.getFullPath();
		}
	} else {
		// external jar
		path = getPath();
	}
	((JavaElement)getParent()).getHandleMemento(buff);
	buff.append(getHandleMementoDelimiter());
	escapeMementoName(buff, path.toString());
}
/**
 * @see IPackageFragmentRoot
 */
public int getKind() throws JavaScriptModelException {
	return ((PackageFragmentRootInfo)getElementInfo()).getRootKind();
}

/**
 * Returns an array of non-java resources contained in the receiver.
 */
public Object[] getNonJavaScriptResources() throws JavaScriptModelException {
	return ((PackageFragmentRootInfo) getElementInfo()).getNonJavaResources(getJavaScriptProject(), getResource(), this);
}

/**
 * @see IPackageFragmentRoot
 */
public IPackageFragment getPackageFragment(String packageName) {
	// tolerate package names with spaces (e.g. 'x . y') (http://bugs.eclipse.org/bugs/show_bug.cgi?id=21957)
	String[] pkgName = Util.getTrimmedSimpleNames(packageName);
	return getPackageFragment(pkgName);
}
public PackageFragment getPackageFragment(String[] pkgName) {
	return new PackageFragment(this, pkgName);
}
/**
 * Returns the package name for the given folder
 * (which is a decendent of this root).
 */
protected String getPackageName(IFolder folder) {
	IPath myPath= getPath();
	IPath pkgPath= folder.getFullPath();
	int mySegmentCount= myPath.segmentCount();
	int pkgSegmentCount= pkgPath.segmentCount();
	StringBuffer pkgName = new StringBuffer(IPackageFragment.DEFAULT_PACKAGE_NAME);
	for (int i= mySegmentCount; i < pkgSegmentCount; i++) {
		if (i > mySegmentCount) {
			pkgName.append('.');
		}
		pkgName.append(pkgPath.segment(i));
	}
	return pkgName.toString();
}

/**
 * @see IJavaScriptElement
 */
public IPath getPath() {
	IResource resource = getResource();
	if(resource!=null) return resource.getFullPath();
	
	if(parent!=null) return parent.getPath();
	return null;
}

/*
 * @see IPackageFragmentRoot
 */
public IIncludePathEntry getRawIncludepathEntry() throws JavaScriptModelException {

	IIncludePathEntry rawEntry = null;
	JavaProject project = (JavaProject)this.getJavaScriptProject();
	project.getResolvedClasspath(); // force the reverse rawEntry cache to be populated
	Map rootPathToRawEntries = project.getPerProjectInfo().rootPathToRawEntries;
	if (rootPathToRawEntries != null) {
		rawEntry = (IIncludePathEntry) rootPathToRawEntries.get(this.getPath());
	}
	if(rawEntry!=null) return rawEntry;
	/* no raw entry, so this must be a packagefragmentroot of a project with undefined source folder */

	return JavaScriptCore.newLibraryEntry(getPath().makeAbsolute(), getPath().makeAbsolute(), getPath().makeAbsolute());
}

public IIncludePathEntry getResolvedIncludepathEntry() throws JavaScriptModelException {

	IIncludePathEntry rawEntry = null;
	JavaProject project = (JavaProject)this.getJavaScriptProject();
	project.getResolvedClasspath(); // force the reverse rawEntry cache to be populated
	Map rootPathToResolvedEntries = project.getPerProjectInfo().rootPathToResolvedEntries;
	if (rootPathToResolvedEntries != null) {
		rawEntry = (IIncludePathEntry) rootPathToResolvedEntries.get(this.getPath());
	}
	if(rawEntry!=null) return rawEntry;
	/* no raw entry, so this must be a packagefragmentroot of a project with undefined source folder */
	/* no luck */
	return null;
}


/*
 * @see IJavaScriptElement
 */
public IResource getResource() {
	return (IResource)this.resource;
}

/**
 * @see IPackageFragmentRoot
 */
public IPath getSourceAttachmentPath() throws JavaScriptModelException {
	if (getKind() != K_BINARY) return null;

	// 1) look source attachment property (set iff attachSource(...) was called
	IPath path = getPath();
	String serverPathString= Util.getSourceAttachmentProperty(path);
	if (serverPathString != null) {
		int index= serverPathString.lastIndexOf(ATTACHMENT_PROPERTY_DELIMITER);
		if (index < 0) {
			// no root path specified
			return new Path(serverPathString);
		} else {
			String serverSourcePathString= serverPathString.substring(0, index);
			return new Path(serverSourcePathString);
		}
	}

	// 2) look at classpath entry
	IIncludePathEntry entry = ((JavaProject) getParent()).getClasspathEntryFor(path);
	IPath sourceAttachmentPath;
	if (entry != null && (sourceAttachmentPath = entry.getSourceAttachmentPath()) != null)
		return sourceAttachmentPath;

	// 3) look for a recommendation
	entry = findSourceAttachmentRecommendation();
	if (entry != null && (sourceAttachmentPath = entry.getSourceAttachmentPath()) != null) {
		return sourceAttachmentPath;
	}

	return null;
}

/**
 * For use by <code>AttachSourceOperation</code> only.
 * Sets the source mapper associated with this root.
 */
public void setSourceMapper(SourceMapper mapper) throws JavaScriptModelException {
	((PackageFragmentRootInfo) getElementInfo()).setSourceMapper(mapper);
}



/**
 * @see IPackageFragmentRoot
 */
public IPath getSourceAttachmentRootPath() throws JavaScriptModelException {
	if (getKind() != K_BINARY) return null;

	// 1) look source attachment property (set iff attachSource(...) was called
	IPath path = getPath();
	String serverPathString= Util.getSourceAttachmentProperty(path);
	if (serverPathString != null) {
		int index = serverPathString.lastIndexOf(ATTACHMENT_PROPERTY_DELIMITER);
		if (index == -1) return null;
		String serverRootPathString= IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH;
		if (index != serverPathString.length() - 1) {
			serverRootPathString= serverPathString.substring(index + 1);
		}
		return new Path(serverRootPathString);
	}

	// 2) look at classpath entry
	IIncludePathEntry entry = ((JavaProject) getParent()).getClasspathEntryFor(path);
	IPath sourceAttachmentRootPath;
	if (entry != null && (sourceAttachmentRootPath = entry.getSourceAttachmentRootPath()) != null)
		return sourceAttachmentRootPath;

	// 3) look for a recomendation
	entry = findSourceAttachmentRecommendation();
	if (entry != null && (sourceAttachmentRootPath = entry.getSourceAttachmentRootPath()) != null)
		return sourceAttachmentRootPath;

	return null;
}

/**
 * @see JavaElement
 */
public SourceMapper getSourceMapper() {
	SourceMapper mapper;
	try {
		PackageFragmentRootInfo rootInfo = (PackageFragmentRootInfo) getElementInfo();
		mapper = rootInfo.getSourceMapper();
		if (mapper == null) {
			// first call to this method
			IPath sourcePath= getSourceAttachmentPath();
			IPath rootPath= getSourceAttachmentRootPath();
			if (sourcePath == null)
				mapper = createSourceMapper(getPath(), rootPath); // attach root to itself
			else
				mapper = createSourceMapper(sourcePath, rootPath);
			rootInfo.setSourceMapper(mapper);
		}
	} catch (JavaScriptModelException e) {
		// no source can be attached
		mapper = null;
	}
	return mapper;
}

/**
 * @see IJavaScriptElement
 */
public IResource getUnderlyingResource() throws JavaScriptModelException {
	if (!exists()) throw newNotPresentException();
	return getResource();
}

/**
 * @see org.eclipse.wst.jsdt.core.IParent
 */
public boolean hasChildren() throws JavaScriptModelException {
	// a package fragment root always has the default package as a child
	return true;
}

public int hashCode() {
	if(this.resource!=null) return this.resource.hashCode();
	return super.hashCode();
}

/**
 * @see IPackageFragmentRoot
 */
public boolean isArchive() {
	return false;
}


public boolean isResourceContainer()
{
	return !isArchive();
}

/**
 * @see IPackageFragmentRoot
 */
public boolean isExternal() {
	return false;
}

/*
 * Validate whether this package fragment root is on the classpath of its project.
 */
protected IStatus validateOnClasspath() {
	return Status.OK_STATUS;
//	IPath path = this.getPath();
//	try {
//		// check package fragment root on classpath of its project
//		JavaProject project = (JavaProject) getJavaScriptProject();
//		IIncludePathEntry entry = project.getClasspathEntryFor(path);
//		if (entry != null) {
//			return Status.OK_STATUS;
//		}
//	} catch(JavaScriptModelException e){
//		// could not read classpath, then assume it is outside
//		return e.getJavaScriptModelStatus();
//	}
//	return new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH, this);
}
/*
 * @see org.eclipse.wst.jsdt.core.IPackageFragmentRoot#move
 */
public void move(
	IPath destination,
	int updateResourceFlags,
	int updateModelFlags,
	IIncludePathEntry sibling,
	IProgressMonitor monitor)
	throws JavaScriptModelException {

	MovePackageFragmentRootOperation op =
		new MovePackageFragmentRootOperation(this, destination, updateResourceFlags, updateModelFlags, sibling);
	op.runOperation(monitor);
}

/**
 * @private Debugging purposes
 */
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(this.tabString(tab));
	IPath path = getPath();
	if (getJavaScriptProject().getElementName().equals(path.segment(0))) {
	    if (path.segmentCount() == 1) {
			buffer.append("<project root>"); //$NON-NLS-1$
	    } else {
			buffer.append(path.removeFirstSegments(1).makeRelative());
	    }
	} else {
	    if (isExternal()) {
			buffer.append(path.toOSString());
	    } else {
			buffer.append(path);
	    }
	}
	if (info == null) {
		buffer.append(" (not open)"); //$NON-NLS-1$
	}
}

/**
 * Possible failures: <ul>
 *  <li>ELEMENT_NOT_PRESENT - the root supplied to the operation
 *      does not exist
 *  <li>INVALID_ELEMENT_TYPES - the root is not of kind K_BINARY
 *   <li>RELATIVE_PATH - the path supplied to this operation must be
 *      an absolute path
 *  </ul>
 */
protected void verifyAttachSource(IPath sourcePath) throws JavaScriptModelException {
	if (!exists()) {
		throw newNotPresentException();
	} else if (this.getKind() != K_BINARY) {
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, this));
	} else if (sourcePath != null && !sourcePath.isAbsolute()) {
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.RELATIVE_PATH, sourcePath));
	}
}

/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.core.IPackageFragmentRoot#isLanguageRuntime()
 */
public boolean isLanguageRuntime() {
	return false;
}

public IIncludePathAttribute[] getIncludepathAttributes() {
	IJavaScriptProject javaProject = getJavaScriptProject();
	IIncludePathEntry[] entries=null;
	IPath rootPath = getPath();
	if(rootPath==null) return new IIncludePathAttribute[0];
	try {
		entries = javaProject.getResolvedIncludepath(true);
	} catch (JavaScriptModelException ex) {}

	IIncludePathAttribute[] attribs=null;
	for(int i = 0;entries!=null && i<entries.length;i++) {
		if(rootPath.equals(entries[i].getPath())){
			attribs =  entries[i].getExtraAttributes();
			break;
		}
	}
	if(attribs!=null) return attribs;
	return new IIncludePathAttribute[0];
}
public boolean isLibrary() {
	return false;
}
public JsGlobalScopeContainerInitializer getContainerInitializer() {
	IIncludePathEntry fClassPathEntry=null;

	try {
		fClassPathEntry =  getRawIncludepathEntry();
	} catch (JavaScriptModelException ex) {}

	if(fClassPathEntry==null) return null;


	return  JavaScriptCore.getJsGlobalScopeContainerInitializer(fClassPathEntry.getPath().segment(0));

}

public IPath getLocation() {
	return getResource().getLocation();
}
}
