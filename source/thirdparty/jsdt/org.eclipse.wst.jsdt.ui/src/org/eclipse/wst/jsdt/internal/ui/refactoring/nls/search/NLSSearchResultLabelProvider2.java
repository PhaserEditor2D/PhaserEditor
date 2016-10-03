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

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.search.TextSearchLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;


class NLSSearchResultLabelProvider2 extends TextSearchLabelProvider {
	
	private AppearanceAwareLabelProvider fLabelProvider;
	
	public NLSSearchResultLabelProvider2(AbstractTextSearchViewPage page) {
		super(page);
		fLabelProvider= new AppearanceAwareLabelProvider(JavaScriptElementLabels.ALL_POST_QUALIFIED, 0);
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.search.TextSearchLabelProvider#doGetText(java.lang.Object)
	 */
	protected String doGetText(Object element) {
		if (element instanceof FileEntry) {
			FileEntry fileEntry= (FileEntry) element;
			return fileEntry.getMessage();
		} else if (element instanceof CompilationUnitEntry) {
			return ((CompilationUnitEntry)element).getMessage();
		} else {
			return Messages.format(NLSSearchMessages.NLSSearchResultLabelProvider2_undefinedKeys, fLabelProvider.getText(element));
		}
	}
	
	/*
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof FileEntry)
			element= ((FileEntry) element).getPropertiesFile();
		if (element instanceof CompilationUnitEntry)
			element= ((CompilationUnitEntry)element).getCompilationUnit();
		
		return fLabelProvider.getImage(element);
	}
	
	/*
	 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
	 */
	public void dispose() {
		fLabelProvider.dispose();
		fLabelProvider= null;
		super.dispose();
	}
}
