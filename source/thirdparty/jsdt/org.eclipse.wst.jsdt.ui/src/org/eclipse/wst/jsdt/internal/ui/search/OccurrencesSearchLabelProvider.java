/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.search;

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

class OccurrencesSearchLabelProvider extends TextSearchLabelProvider {
	
	public OccurrencesSearchLabelProvider(AbstractTextSearchViewPage page) {
		super(page);
	}

	protected String doGetText(Object element) {
		JavaElementLine jel= (JavaElementLine) element;
		return jel.getLineContents().replace('\t', ' ');
	}
	
	public Image getImage(Object element) {
		if (element instanceof OccurrencesGroupKey) {
			OccurrencesGroupKey group= (OccurrencesGroupKey) element;
			if (group.isVariable()) {
				if (group.isWriteAccess())
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_WRITEACCESS);
				else
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_READACCESS);
			}
			
		} else if (element instanceof ExceptionOccurrencesGroupKey) {
			ExceptionOccurrencesGroupKey group= (ExceptionOccurrencesGroupKey) element;
			if (group.isException())
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		}
		
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
	}
}
