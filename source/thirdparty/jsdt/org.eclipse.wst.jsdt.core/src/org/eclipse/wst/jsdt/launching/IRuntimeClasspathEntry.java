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
package org.eclipse.wst.jsdt.launching;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

/**
 * Represents an entry on a runtime includepath. A runtime includepath entry
 * may refer to one of the following:
 * <ul>
 * 	<li>A JavaScript project (type <code>PROJECT</code>) - a project entry refers
 * 		to all of the built classes in a project, and resolves to the output
 * 		location(s) of the associated JavaScript project.</li>
 * 	<li>An archive (type <code>ARCHIVE</code>) - an archive refers to a jar, zip, or
 * 		folder in the workspace or in the local file system containing class
 * 		files. An archive may have attached source.</li>
 * 	<li>A variable (type <code>VARIABLE</code>) - a variable refers to a 
 * 		includepath variable, which may refer to a jar.</li>
 * 	<li>A library (type <code>CONTAINER</code>) - a container refers to includepath
 * 		container variable which refers to a collection of archives derived
 * 		dynamically, on a per project basis.</li>
 *  <li>A contributed includepath entry (type <code>OTHER</code>) - a contributed
 *      includepath entry is an extension contributed by a plug-in. The resolution
 *      of a contributed includepath entry is client defined. See
 * 		<code>IRuntimeClasspathEntry2</code>.
 * </ul>
 * <p>
 * Clients may implement this interface for contributed a includepath entry
 * types (i.e. type <code>OTHER</code>). Note, contributed includepath entries
 * are new in 3.0, and are only intended to be contributed by the JavaScript debugger.
 * </p>
 *  
 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry2
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IRuntimeClasspathEntry {
	
	/**
	 * Type identifier for project entries.
	 */
	public static final int PROJECT = 1;
	
	/**
	 * Type identifier for archive entries.
	 */
	public static final int ARCHIVE = 2;	
		
	/**
	 * Type identifier for variable entries.
	 */
	public static final int VARIABLE = 3;
	
	/**
	 * Type identifier for container entries.
	 */
	public static final int CONTAINER = 4;
	
	/**
	 * Type identifier for contributed entries.
	 *  
	 */
	public static final int OTHER = 5;	

	/**
	 * Classpath property identifier for entries that appear on the
	 * bootstrap path by default.
	 */
	public static final int STANDARD_CLASSES = 1;	
	
	/**
	 * Classpath property identifier for entries that should appear on the
	 * bootstrap path explicitly.
	 */
	public static final int BOOTSTRAP_CLASSES = 2;	
		
	/**
	 * Classpath property identifier for entries that should appear on the
	 * user includepath.
	 */
	public static final int USER_CLASSES = 3;	
	
	/**
	 * Returns this includepath entry's type. The type of a runtime includepath entry is
	 * identified by one of the following constants:
	 * <ul>
	 * <li><code>PROJECT</code></li>
	 * <li><code>ARCHIVE</code></li>
	 * <li><code>VARIABLE</code></li>
	 * <li><code>CONTAINER</code></li>
	 * <li><code>OTHER</code></li>
	 * </ul>
	 * <p>
	 * Since 3.0, a type of <code>OTHER</code> may be returned.
	 * </p>
	 * @return this includepath entry's type
	 */
	public int getType();
	
	/**
	 * Returns a memento for this includepath entry.
	 * <p>
	 * Since 3.0, the memento for a contributed includepath entry (i.e. of
	 * type <code>OTHER</code>), must be in the form of an XML document,
	 * with the following element structure:
	 * <pre>
	 * <runtimeClasspathEntry id="exampleId">
	 *    <memento
	 *       key1="value1"
	 * 		 ...>
	 *    </memento>
	 * </runtimeClasspathEntry>
	 * </pre>
	 * The <code>id</code> attribute is the unique identifier of the extension
	 * that contributed this runtime includepath entry type, via the extension
	 * point <code>org.eclipse.jdt.launching.runtimeClasspathEntries</code>.
	 * The <code>memento</code> element will be used to initialize a
	 * restored runtime includepath entry, via the method
	 * <code>IRuntimeClasspathEntry2.initializeFrom(Element memento)</code>. The 
	 * attributes of the <code>memento</code> element are client defined.
	 * </p>
	 * 
	 * @return a memento for this includepath entry
	 * @exception CoreException if an exception occurs generating a memento
	 */
	public String getMemento() throws CoreException;
	
	/**
	 * Returns the path associated with this entry, or <code>null</code>
	 * if none. The format of the
	 * path returned depends on this entry's type:
	 * <ul>
	 * <li><code>PROJECT</code> - a workspace relative path to the associated
	 * 		project.</li>
	 * <li><code>ARCHIVE</code> - the absolute path of the associated archive,
	 * 		which may or may not be in the workspace.</li>
	 * <li><code>VARIABLE</code> - the path corresponding to the associated
	 * 		includepath variable entry.</li>
	 * <li><code>CONTAINER</code> - the path corresponding to the associated
	 * 		includepath container variable entry.</li>
	 * <li><code>OTHER</code> - the path returned is client defined.</li>
	 * </ul>
	 * <p>
	 * Since 3.0, this method may return <code>null</code>.
	 * </p>
	 * @return the path associated with this entry, or <code>null</code>
	 * @see org.eclipse.IIncludePathEntry.core.IClasspathEntry#getPath()
	 */
	public IPath getPath();
		
	/**
	 * Returns the resource associated with this entry, or <code>null</code>
	 * if none. A project, archive, or folder entry may be associated
	 * with a resource.
	 * 
	 * @return the resource associated with this entry, or <code>null</code>
	 */ 
	public IResource getResource();
	
	/**
	 * Returns the path to the source archive associated with this
	 * entry, or <code>null</code> if this includepath entry has no
	 * source attachment.
	 * <p>
	 * Only archive and variable entries may have source attachments.
	 * For archive entries, the path (if present) locates a source
	 * archive. For variable entries, the path (if present) has
	 * an analogous form and meaning as the variable path, namely the first segment 
	 * is the name of a includepath variable.
	 * </p>
	 *
	 * @return the path to the source archive, or <code>null</code> if none
	 */
	public IPath getSourceAttachmentPath();

	/**
	 * Sets the path to the source archive associated with this
	 * entry, or <code>null</code> if this includepath entry has no
	 * source attachment.
	 * <p>
	 * Only archive and variable entries may have source attachments.
	 * For archive entries, the path refers to a source
	 * archive. For variable entries, the path has
	 * an analogous form and meaning as the variable path, namely the
	 * first segment  is the name of a includepath variable.
	 * </p>
	 * <p>
	 * Note that an empty path (<code>Path.EMPTY</code>) is considered
	 * <code>null</code>.
	 * </p>
	 *
	 * @param path the path to the source archive, or <code>null</code> if none
	 */
	public void setSourceAttachmentPath(IPath path);
	
	/**
	 * Returns the path within the source archive where package fragments
	 * are located. An empty path indicates that packages are located at
	 * the root of the source archive. Returns a non-<code>null</code> value
	 * if and only if <code>getSourceAttachmentPath</code> returns 
	 * a non-<code>null</code> value.
	 *
	 * @return root path within the source archive, or <code>null</code> if
	 *    not applicable
	 */
	public IPath getSourceAttachmentRootPath();
	
	/**
	 * Sets the path within the source archive where package fragments
	 * are located. A root path indicates that packages are located at
	 * the root of the source archive. Only valid if a source attachment
	 * path is also specified.
	 * <p>
	 * Note that an empty path (<code>Path.EMPTY</code>) is considered
	 * <code>null</code>.
	 * </p>
	 * 
	 * @param path root path within the source archive, or <code>null</code>
	 */	
	public void setSourceAttachmentRootPath(IPath path);
	
	/**
	 * Returns a constant indicating where this entry should appear on the 
	 * runtime includepath by default.
	 * The value returned is one of the following:
	 * <ul>
	 * <li><code>STANDARD_CLASSES</code> - a standard entry does not need to appear
	 * 		on the runtime includepath</li>
	 * <li><code>BOOTSTRAP_CLASSES</code> - a bootstrap entry should appear on the
	 * 		boot path</li>
	 * <li><code>USER_CLASSES</code> - a user entry should appear on the path
	 * 		containing user or application classes</li>
	 * </ul>
	 * 
	 * @return where this entry should appear on the runtime includepath
	 */
	public int getClasspathProperty();
	
	/**
	 * Sets whether this entry should appear on the bootstrap includepath,
	 * the user includepath, or whether this entry is a standard bootstrap entry
	 * that does not need to appear on the includepath.
	 * The location is one of:
	 * <ul>
	 * <li><code>STANDARD_CLASSES</code> - a standard entry does not need to appear
	 * 		on the runtime includepath</li>
	 * <li><code>BOOTSTRAP_CLASSES</code> - a bootstrap entry should appear on the
	 * 		boot path</li>
	 * <li><code>USER_CLASSES</code> - a user entry should appear on the path
	 * 		conatining user or application classes</li>
	 * </ul>
	 * 
	 * @param location a classpat property constant
	 */
	public void setClasspathProperty(int location);	
	
	/**
	 * Returns an absolute path in the local file system for this entry,
	 * or <code>null</code> if none, or if this entry is of type <code>CONTAINER</code>.
	 * 
	 * @return an absolute path in the local file system for this entry,
	 *  or <code>null</code> if none
	 */
	public String getLocation();
		
	/**
	 * Returns an absolute path in the local file system for the source
	 * attachment associated with this entry entry, or <code>null</code> if none.
	 * 
	 * @return an absolute path in the local file system for the source
	 *  attachment associated with this entry entry, or <code>null</code> if none
	 */
	public String getSourceAttachmentLocation();	
	
	/**
	 * Returns a path relative to this entry's source attachment path for the
	 * root location containing source, or <code>null</code> if none.
	 * 
	 * @return a path relative to this entry's source attachment path for the
	 *  root location containing source, or <code>null</code> if none
	 */
	public String getSourceAttachmentRootLocation();		
	
	/**
	 * Returns the first segment of the path associated with this entry, or <code>null</code>
	 * if this entry is not of type <code>VARIABLE</code> or <code>CONTAINER</code>.
	 * 
	 * @return the first segment of the path associated with this entry, or <code>null</code>
	 *  if this entry is not of type <code>VARIABLE</code> or <code>CONTAINER</code>
	 */
	public String getVariableName();
	
	/**
	 * Returns a includepath entry equivalent to this runtime includepath entry,
	 * or <code>null</code> if none.
	 * <p>
	 * Since 3.0, this method may return <code>null</code>.
	 * </p>
	 * @return a includepath entry equivalent to this runtime includepath entry,
	 *  or <code>null</code>
	 *  
	 */
	public IIncludePathEntry getClasspathEntry();
	
	/**
	 * Returns the JavaScript project associated with this runtime includepath entry
	 * or <code>null</code> if none. Runtime includepath entries of type
	 * <code>CONTAINER</code> may be associated with a project for the
	 * purposes of resolving the entries in a container. 
	 * 
	 * @return the JavaScript project associated with this runtime includepath entry
	 * or <code>null</code> if none
	 *  
	 */
	public IJavaScriptProject getJavaProject();
}
