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

package org.eclipse.wst.jsdt.internal.ui.search;

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
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;


public class OccurrencesSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {

	protected static final Match[] NO_MATCHES= new Match[0];
	private OccurrencesSearchQuery fQuery;

	public OccurrencesSearchResult(OccurrencesSearchQuery query) {
		fQuery= query;
	}
	
	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#findContainedMatches(org.eclipse.core.resources.IFile)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		Object[] elements= getElements();
		if (elements.length == 0)
			return NO_MATCHES;
		//all matches from same file:
		JavaElementLine jel= (JavaElementLine) elements[0];
		try {
			if (file.equals(jel.getJavaElement().getCorrespondingResource()))
				return collectMatches(elements);
		} catch (JavaScriptModelException e) {
			// no resource
		}
		return NO_MATCHES;
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#findContainedMatches(org.eclipse.ui.IEditorPart)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		//TODO same code in JavaSearchResult
		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput instanceof IFileEditorInput)  {
			IFileEditorInput fileEditorInput= (IFileEditorInput) editorInput;
			return computeContainedMatches(result, fileEditorInput.getFile());
			
		} else if (editorInput instanceof IClassFileEditorInput) {
			IClassFileEditorInput classFileEditorInput= (IClassFileEditorInput) editorInput;
			IClassFile classFile= classFileEditorInput.getClassFile();
			
			Object[] elements= getElements();
			if (elements.length == 0)
				return NO_MATCHES;
			//all matches from same file:
			JavaElementLine jel= (JavaElementLine) elements[0];
			if (jel.getJavaElement().equals(classFile))
				return collectMatches(elements);
		}
		return NO_MATCHES;
	}
	
	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getFile(java.lang.Object)
	 */
	public IFile getFile(Object element) {
		JavaElementLine jel= (JavaElementLine) element;
		IResource resource= null;
		try {
			resource= jel.getJavaElement().getCorrespondingResource();
		} catch (JavaScriptModelException e) {
			// no resource
		}
		if (resource instanceof IFile)
			return (IFile) resource;
		else
			return null;
	}
	
	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#isShownInEditor(org.eclipse.search.ui.text.Match, org.eclipse.ui.IEditorPart)
	 */
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		Object element= match.getElement();
		IJavaScriptElement je= ((JavaElementLine) element).getJavaElement();
		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput instanceof IFileEditorInput) {
			try {
				return ((IFileEditorInput)editorInput).getFile().equals(je.getCorrespondingResource());
			} catch (JavaScriptModelException e) {
				return false;
			}
		} else if (editorInput instanceof IClassFileEditorInput) {
			return ((IClassFileEditorInput)editorInput).getClassFile().equals(je);
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

	private Match[] collectMatches(Object[] elements) {
		Match[] matches= new Match[getMatchCount()];
		int writeIndex= 0;
		for (int i= 0; i < elements.length; i++) {
			Match[] perElement= getMatches(elements[i]);
			for (int j= 0; j < perElement.length; j++) {
				matches[writeIndex++]= perElement[j];
			}
		}
		return matches;
	}
}
