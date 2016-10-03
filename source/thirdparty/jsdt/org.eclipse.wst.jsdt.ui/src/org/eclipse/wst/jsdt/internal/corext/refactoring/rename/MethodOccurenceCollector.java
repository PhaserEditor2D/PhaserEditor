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
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.search.MethodReferenceMatch;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.internal.corext.refactoring.CuCollectingSearchRequestor;

class MethodOccurenceCollector extends CuCollectingSearchRequestor {

	private final String fName;

	public MethodOccurenceCollector(String methodName) {
		fName= methodName;
	}

	public void acceptSearchMatch(IJavaScriptUnit unit, SearchMatch match) throws CoreException {
		if (match instanceof MethodReferenceMatch
				&& ((MethodReferenceMatch) match).isSuperInvocation()
				&& match.getAccuracy() == SearchMatch.A_INACCURATE) {
			return; // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=156491
		}
		
		if (match.isImplicit()) { // see bug 94062
			collectMatch(match);
			return;
		}
		
		int start= match.getOffset();
		int length= match.getLength();
		String matchText= unit.getBuffer().getText(start, length);
		
		//direct match:
		if (fName.equals(matchText)) {
			collectMatch(match);
			return;
		}
					
		//Not a standard reference -- use scanner to find last identifier token before left parenthesis:
		IScanner scanner= getScanner(unit);
		scanner.setSource(matchText.toCharArray());
		int simpleNameStart= -1;
		int simpleNameEnd= -1;
		try {
			int token = scanner.getNextToken();
			while (token != ITerminalSymbols.TokenNameEOF && token != ITerminalSymbols.TokenNameLPAREN) { // reference in code includes arguments in parentheses
				if (token == ITerminalSymbols.TokenNameIdentifier) {
					simpleNameStart= scanner.getCurrentTokenStartPosition();
					simpleNameEnd= scanner.getCurrentTokenEndPosition();
				}
				token = scanner.getNextToken();
			}
		} catch (InvalidInputException e){
			//ignore
		}	
		if (simpleNameStart != -1) {
			match.setOffset(start + simpleNameStart);
			match.setLength(simpleNameEnd + 1 - simpleNameStart);
		}
		collectMatch(match);
	}	
}
