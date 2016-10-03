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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;


public class OccurrencesSearchResultPage extends AbstractTextSearchViewPage {

	private TextSearchTableContentProvider fContentProvider;

	public OccurrencesSearchResultPage() {
		super(AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT);
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#showMatch(org.eclipse.search.ui.text.Match, int, int)
	 */
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		JavaElementLine element= (JavaElementLine) match.getElement();
		IJavaScriptElement javaElement= element.getJavaElement();
		try {
			IEditorPart editor= JavaScriptUI.openInEditor(javaElement, activate, false);
			if (editor instanceof ITextEditor) {
				ITextEditor textEditor= (ITextEditor) editor;
				textEditor.selectAndReveal(currentOffset, currentLength);
			}
		} catch (PartInitException e1) {
			return;
		} catch (JavaScriptModelException e1) {
			return;
		}

	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#elementsChanged(java.lang.Object[])
	 */
	protected void elementsChanged(Object[] objects) {
		if (fContentProvider != null)
			fContentProvider.elementsChanged(objects);
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#clear()
	 */
	protected void clear() {
		if (fContentProvider != null)
			fContentProvider.clear();
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
	 */
	protected void configureTreeViewer(TreeViewer viewer) {
		throw new IllegalStateException("Doesn't support tree mode."); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTableViewer(org.eclipse.jface.viewers.TableViewer)
	 */
	protected void configureTableViewer(TableViewer viewer) {
		viewer.setComparator(new ViewerComparator() {
			public int compare(Viewer v, Object e1, Object e2) {
				JavaElementLine jel1= (JavaElementLine) e1;
				JavaElementLine jel2= (JavaElementLine) e2;
				return jel1.getLine() - jel2.getLine();
			}
		});
		viewer.setLabelProvider(new OccurrencesSearchLabelProvider(this));
		fContentProvider= new TextSearchTableContentProvider();
		viewer.setContentProvider(fContentProvider);
	}
	
}
