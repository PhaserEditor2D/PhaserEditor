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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.search.FieldReferenceMatch;
import org.eclipse.wst.jsdt.core.search.LocalVariableReferenceMatch;
import org.eclipse.wst.jsdt.core.search.MethodReferenceMatch;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;

public class NewSearchResultCollector extends SearchRequestor {
	private JavaSearchResult fSearch;
	private boolean fIgnorePotentials;

	public NewSearchResultCollector(JavaSearchResult search, boolean ignorePotentials) {
		super();
		fSearch= search;
		fIgnorePotentials= ignorePotentials;
	}
	
	public void acceptSearchMatch(SearchMatch match) throws CoreException {
		IJavaScriptElement enclosingElement= (IJavaScriptElement) match.getElement();
		if (enclosingElement != null) {
			if (fIgnorePotentials && (match.getAccuracy() == SearchMatch.A_INACCURATE))
				return;
			boolean isWriteAccess= false;
			boolean isReadAccess= false;
			if (match instanceof FieldReferenceMatch) {
				FieldReferenceMatch fieldRef= ((FieldReferenceMatch) match);
				isWriteAccess= fieldRef.isWriteAccess();
				isReadAccess= fieldRef.isReadAccess();
			} else if (match instanceof LocalVariableReferenceMatch) {
				LocalVariableReferenceMatch localVarRef= ((LocalVariableReferenceMatch) match);
				isWriteAccess= localVarRef.isWriteAccess();
				isReadAccess= localVarRef.isReadAccess();
			}
			boolean isSuperInvocation= false;
			if (match instanceof MethodReferenceMatch) {
				MethodReferenceMatch methodRef= (MethodReferenceMatch) match;
				isSuperInvocation= methodRef.isSuperInvocation();
			}
			fSearch.addMatch(new JavaElementMatch(enclosingElement, match.getRule(), match.getOffset(), match.getLength(), match.getAccuracy(), isReadAccess, isWriteAccess, match.isInsideDocComment(), isSuperInvocation));
		}
	}

	public void beginReporting() {
	}

	public void endReporting() {
	}

	public void enterParticipant(SearchParticipant participant) {
	}

	public void exitParticipant(SearchParticipant participant) {
	}

}
