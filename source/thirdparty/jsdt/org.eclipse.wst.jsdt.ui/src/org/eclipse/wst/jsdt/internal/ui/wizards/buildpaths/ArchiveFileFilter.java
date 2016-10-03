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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Viewer filter for archive selection dialogs.
 * Archives are files with file extension 'jar' and 'zip'.
 * The filter is not case sensitive.
 */
public class ArchiveFileFilter extends ViewerFilter {

	//public static final String[] FILTER_EXTENSIONS= new String[] {"*.jar;*.zip"}; //$NON-NLS-1$
	public static final String[] FILTER_EXTENSIONS= new String[] {"*.js"}; //$NON-NLS-1$
	private static final String[] fgArchiveExtensions= { "js"}; //$NON-NLS-1$ //$NON-NLS-2$
	//private static final String[] fgArchiveExtensions= {"*jar","zip"}; //$NON-NLS-1$ //$NON-NLS-2$
	private List fExcludes;
	private boolean fRecursive;
	
	/**
	 * @param excludedFiles Excluded files will not pass the filter.
	 * <code>null</code> is allowed if no files should be excluded. 
	 * @param recusive Folders are only shown if, searched recursively, contain
	 * an archive
	 */
	public ArchiveFileFilter(IFile[] excludedFiles, boolean recusive) {
		if (excludedFiles != null) {
			fExcludes= Arrays.asList(excludedFiles);
		} else {
			fExcludes= null;
		}
		fRecursive= recusive;
	}
	
	public ArchiveFileFilter(List excludedFiles, boolean recusive) {
		fExcludes= excludedFiles;
		fRecursive= recusive;
	}
	
	/*
	 * @see ViewerFilter#select
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile) {
			if (fExcludes != null && fExcludes.contains(element)) {
				return false;
			}
			return isArchivePath(((IFile)element).getFullPath());
		} else if (element instanceof IContainer) { // IProject, IFolder
			if (!fRecursive) {
				return true;
			}
			// Ignore closed projects
			if (element instanceof IProject && !((IProject)element).isOpen())
				return false;
			try {
				IResource[] resources= ((IContainer)element).members();
				for (int i= 0; i < resources.length; i++) {
					// recursive! Only show containers that contain an archive
					if (select(viewer, parent, resources[i])) {
						return true;
					}
				}
			} catch (CoreException e) {
				JavaScriptPlugin.log(e.getStatus());
			}				
		}
		return false;
	}
	
	public static boolean isArchivePath(IPath path) {
		String ext= path.getFileExtension();
		if (ext != null && ext.length() != 0) {
			return isArchiveFileExtension(ext);
		}
		return false;
	}
	
	public static boolean isArchiveFileExtension(String ext) {
		for (int i= 0; i < fgArchiveExtensions.length; i++) {
			if (ext.equalsIgnoreCase(fgArchiveExtensions[i])) {
				return true;
			}
		}
		return false;
	}
			
	
			
}
