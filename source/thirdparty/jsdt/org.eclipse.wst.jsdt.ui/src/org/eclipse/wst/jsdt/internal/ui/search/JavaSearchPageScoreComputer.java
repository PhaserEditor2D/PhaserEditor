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

import org.eclipse.search.ui.ISearchPageScoreComputer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;

public class JavaSearchPageScoreComputer implements ISearchPageScoreComputer {

	public int computeScore(String id, Object element) {
		if (!JavaSearchPage.EXTENSION_POINT_ID.equals(id))
			// Can't decide
			return ISearchPageScoreComputer.UNKNOWN;
		
		if (element instanceof IJavaScriptElement || element instanceof IClassFileEditorInput || element instanceof LogicalPackage)
			return 90;
		
		return ISearchPageScoreComputer.LOWEST;
	}
}
