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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.search.SearchEngine;


public class SearchBrokenNLSKeysUtil {
	
	public static void search(String scopeName, IType[] accessorClasses, IFile[] propertieFiles) {
		NLSSearchQuery query= new NLSSearchQuery(accessorClasses, propertieFiles, SearchEngine.createWorkspaceScope(), scopeName);
		NewSearchUI.runQueryInBackground(query);
	}

}
