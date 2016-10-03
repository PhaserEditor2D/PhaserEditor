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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;

public abstract class FindOccurrencesEngine {
	
	private IOccurrencesFinder fFinder;
	
	private static class FindOccurencesClassFileEngine extends FindOccurrencesEngine {
		private IClassFile fClassFile;
		
		public FindOccurencesClassFileEngine(IClassFile file, IOccurrencesFinder finder) {
			super(finder);
			fClassFile= file;
		}
		protected JavaScriptUnit createAST() {
			return JavaScriptPlugin.getDefault().getASTProvider().getAST(fClassFile, ASTProvider.WAIT_YES, null);
		}
		protected IJavaScriptElement getInput() {
			return fClassFile;
		}
		protected ISourceReference getSourceReference() {
			return fClassFile;
		}
	}

	private static class FindOccurencesCUEngine extends FindOccurrencesEngine {
		private IJavaScriptUnit fCUnit;
		
		public FindOccurencesCUEngine(IJavaScriptUnit unit, IOccurrencesFinder finder) {
			super(finder);
			fCUnit= unit;
		}
		protected JavaScriptUnit createAST() {
			return JavaScriptPlugin.getDefault().getASTProvider().getAST(fCUnit, ASTProvider.WAIT_YES, null);
		}
		protected IJavaScriptElement getInput() {
			return fCUnit;
		}
		protected ISourceReference getSourceReference() {
			return fCUnit;
		}
	}
	
	protected FindOccurrencesEngine(IOccurrencesFinder finder) {
		fFinder= finder;
	}
	
	public static FindOccurrencesEngine create(IJavaScriptElement root, IOccurrencesFinder finder) {
		if (root == null || finder == null)
			return null;
		
		IJavaScriptUnit unit= (IJavaScriptUnit)root.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		if (unit != null)
			return new FindOccurencesCUEngine(unit, finder);
		IClassFile cf= (IClassFile)root.getAncestor(IJavaScriptElement.CLASS_FILE);
		if (cf != null)
			return new FindOccurencesClassFileEngine(cf, finder);
		return null;
	}

	protected abstract JavaScriptUnit createAST();
	
	protected abstract IJavaScriptElement getInput();
	
	protected abstract ISourceReference getSourceReference();
	
	protected IOccurrencesFinder getOccurrencesFinder() {
		return fFinder;
	}

	public String run(int offset, int length) throws JavaScriptModelException {
		ISourceReference sr= getSourceReference();
		if (sr.getSourceRange() == null) {
			return SearchMessages.FindOccurrencesEngine_noSource_text; 
		}
		
		final JavaScriptUnit root= createAST();
		if (root == null) {
			return SearchMessages.FindOccurrencesEngine_cannotParse_text; 
		}
		String message= fFinder.initialize(root, offset, length);
		if (message != null)
			return message;
		
		final IDocument document= new Document(getSourceReference().getSource());
		
		performNewSearch(fFinder, document, getInput());
		return null;
	}
	
	private void performNewSearch(IOccurrencesFinder finder, IDocument document, IJavaScriptElement element) {
		NewSearchUI.runQueryInBackground(new OccurrencesSearchQuery(finder, document, element));
	}
}
