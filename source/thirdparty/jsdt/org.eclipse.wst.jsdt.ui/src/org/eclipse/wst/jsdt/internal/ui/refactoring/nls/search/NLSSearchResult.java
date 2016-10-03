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

package org.eclipse.wst.jsdt.internal.ui.refactoring.nls.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;

public class NLSSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {

	private static final Match[] NO_MATCHES= new Match[0];

	/*
	 * Element (group key) is always IJavaScriptElement or FileEntry.
	 */
	private NLSSearchQuery fQuery;
	private final List fFileEntryGroups;
	private final List fCompilationUnitGroups;

	public NLSSearchResult(NLSSearchQuery query) {
		fQuery= query;
		fFileEntryGroups= new ArrayList();
		fCompilationUnitGroups= new ArrayList();
	}
	
	public void addFileEntryGroup(FileEntry group) {
		fFileEntryGroups.add(group);
	}
	
	public void addCompilationUnitGroup(CompilationUnitEntry group) {
		fCompilationUnitGroups.add(group);
	}
	
	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#findContainedMatches(org.eclipse.ui.IEditorPart)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		//TODO: copied from JavaSearchResult:
		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput instanceof IFileEditorInput)  {
			IFileEditorInput fileEditorInput= (IFileEditorInput) editorInput;
			return computeContainedMatches(result, fileEditorInput.getFile());
		} else if (editorInput instanceof IClassFileEditorInput) {
			IClassFileEditorInput classFileEditorInput= (IClassFileEditorInput) editorInput;
			Set matches= new HashSet();
			collectMatches(matches, classFileEditorInput.getClassFile());
			return (Match[]) matches.toArray(new Match[matches.size()]);
		}
		return NO_MATCHES;
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#findContainedMatches(org.eclipse.core.resources.IFile)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		Set matches= new HashSet();
		for (Iterator iter= fFileEntryGroups.iterator(); iter.hasNext();) {
			FileEntry element= (FileEntry)iter.next();
			if (element.getPropertiesFile().equals(file)) {
				matches.addAll(Arrays.asList(getMatches(element)));
			}
		}
		if (matches.size() > 0)
			return (Match[]) matches.toArray(new Match[matches.size()]);
		
		try {
			for (Iterator iter= fCompilationUnitGroups.iterator(); iter.hasNext();) {
				CompilationUnitEntry element= (CompilationUnitEntry)iter.next();
				IJavaScriptUnit cu= element.getCompilationUnit();
				if (cu.exists() && file.equals(cu.getCorrespondingResource())) {
					matches.addAll(Arrays.asList(getMatches(element)));
				}
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
			return NO_MATCHES;
		}
		
		//TODO: copied from JavaSearchResult:
		IJavaScriptElement javaElement= JavaScriptCore.create(file);
		collectMatches(matches, javaElement);
		return (Match[]) matches.toArray(new Match[matches.size()]);
	}
	
	private void collectMatches(Set matches, IJavaScriptElement element) {
		//TODO: copied from JavaSearchResult:
		Match[] m= getMatches(element);
		if (m.length != 0) {
			for (int i= 0; i < m.length; i++) {
				matches.add(m[i]);
			}
		}
		if (element instanceof IParent) {
			IParent parent= (IParent) element;
			try {
				IJavaScriptElement[] children= parent.getChildren();
				for (int i= 0; i < children.length; i++) {
					collectMatches(matches, children[i]);
				}
			} catch (JavaScriptModelException e) {
				// we will not be tracking these results
			}
		}
	}
	
	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getFile(java.lang.Object)
	 */
	public IFile getFile(Object element) {
		if (element instanceof FileEntry) {
			return ((FileEntry) element).getPropertiesFile();
		} else {
			IJavaScriptElement javaElement= null;
			if (element instanceof CompilationUnitEntry) {
				javaElement= ((CompilationUnitEntry)element).getCompilationUnit();
			} else {
				javaElement= (IJavaScriptElement) element;
			}
			IResource resource= null;
			try {
				resource= javaElement.getCorrespondingResource();
			} catch (JavaScriptModelException e) {
				// no resource
			}
			if (resource instanceof IFile)
				return (IFile) resource;
			else
				return null;
		}
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#isShownInEditor(org.eclipse.search.ui.text.Match, org.eclipse.ui.IEditorPart)
	 */
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		IEditorInput editorInput= editor.getEditorInput();
		if (match.getElement() instanceof FileEntry) {
			IFile file= ((FileEntry) match.getElement()).getPropertiesFile();
			if (editorInput instanceof IFileEditorInput) {
				return ((IFileEditorInput) editorInput).getFile().equals(file);
			}
		} else if (match.getElement() instanceof IJavaScriptElement || match.getElement() instanceof CompilationUnitEntry) {
			IJavaScriptElement je= null;
			if (match.getElement() instanceof IJavaScriptElement) {
				je= (IJavaScriptElement) match.getElement();
			} else {
				je= ((CompilationUnitEntry)match.getElement()).getCompilationUnit();
			}
			if (editorInput instanceof IFileEditorInput) {
				try {
					IJavaScriptUnit cu= (IJavaScriptUnit) je.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
					if (cu == null)
						return false;
					else
						return ((IFileEditorInput) editorInput).getFile().equals(cu.getCorrespondingResource());
				} catch (JavaScriptModelException e) {
					return false;
				}
			} else if (editorInput instanceof IClassFileEditorInput) {
				return ((IClassFileEditorInput) editorInput).getClassFile().equals(je.getAncestor(IJavaScriptElement.CLASS_FILE));
			}
		}
		return false;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchResult#getLabel()
	 */
	public String getLabel() {
		return fQuery.getResultLabel(getMatchCount());
	}

	/*
	 * @see org.eclipse.search.ui.ISearchResult#getTooltip()
	 */
	public String getTooltip() {
		return getLabel();
	}

	/*
	 * @see org.eclipse.search.ui.ISearchResult#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	public ISearchQuery getQuery() {
		return fQuery;
	}

	public IFileMatchAdapter getFileMatchAdapter() {
		return this;
	}
	
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

}
