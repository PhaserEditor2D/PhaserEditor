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
package org.eclipse.wst.jsdt.internal.ui.text.template.contentassist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitContextType;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

public class TemplateEngine {

	private static final String $_LINE_SELECTION= "${" + GlobalTemplateVariables.LineSelection.NAME + "}"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String $_WORD_SELECTION= "${" + GlobalTemplateVariables.WordSelection.NAME + "}"; //$NON-NLS-1$ //$NON-NLS-2$

	/** The context type. */
	private TemplateContextType fContextType;
	/** The result proposals. */
	private ArrayList fProposals= new ArrayList();
	/** Positions created on the key documents to remove in reset. */
	private final Map fPositions= new HashMap();

	/**
	 * Creates the template engine for a particular context type.
	 * See <code>TemplateContext</code> for supported context types.
	 */
	public TemplateEngine(TemplateContextType contextType) {
		Assert.isNotNull(contextType);
		fContextType= contextType;
	}

	/**
	 * Empties the collector.
	 */
	public void reset() {
		fProposals.clear();
		for (Iterator it= fPositions.entrySet().iterator(); it.hasNext();) {
			Entry entry= (Entry) it.next();
			IDocument doc= (IDocument) entry.getKey();
			Position position= (Position) entry.getValue();
			doc.removePosition(position);
		}
		fPositions.clear();
	}

	/**
	 * Returns the array of matching templates.
	 */
	public TemplateProposal[] getResults() {
		return (TemplateProposal[]) fProposals.toArray(new TemplateProposal[fProposals.size()]);
	}

	/**
	 * Inspects the context of the compilation unit around <code>completionPosition</code>
	 * and feeds the collector with proposals.
	 * @param viewer the text viewer
	 * @param completionPosition the context position in the document of the text viewer
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 */
	public void complete(ITextViewer viewer, int completionPosition, IJavaScriptUnit compilationUnit) {
	    IDocument document= viewer.getDocument();

		if (!(fContextType instanceof CompilationUnitContextType))
			return;

		Point selection= viewer.getSelectedRange();
		Position position= new Position(completionPosition, selection.y);

		// remember selected text
		String selectedText= null;
		if (selection.y != 0) {
			try {
				selectedText= document.get(selection.x, selection.y);
				document.addPosition(position);
				fPositions.put(document, position);
			} catch (BadLocationException e) {}
		}

		CompilationUnitContext context= ((CompilationUnitContextType) fContextType).createContext(document, position, compilationUnit);
		context.setVariable("selection", selectedText); //$NON-NLS-1$
		int start= context.getStart();
		int end= context.getEnd();
		IRegion region= new Region(start, end - start);

		Template[] templates= JavaScriptPlugin.getDefault().getTemplateStore().getTemplates();

		if (selection.y == 0) {
			for (int i= 0; i != templates.length; i++)
				if (context.canEvaluate(templates[i]))
					fProposals.add(new TemplateProposal(templates[i], context, region, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)));

		} else {

			if (context.getKey().length() == 0)
				context.setForceEvaluation(true);

			boolean multipleLinesSelected= areMultipleLinesSelected(viewer);

			for (int i= 0; i != templates.length; i++) {
				Template template= templates[i];
				if (context.canEvaluate(template) &&
					template.getContextTypeId().equals(context.getContextType().getId()) &&
					(!multipleLinesSelected && template.getPattern().indexOf($_WORD_SELECTION) != -1 || (multipleLinesSelected && template.getPattern().indexOf($_LINE_SELECTION) != -1)))
				{
					fProposals.add(new TemplateProposal(templates[i], context, region, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)));
				}
			}
		}
	}

	/**
	 * Returns <code>true</code> if one line is completely selected or if multiple lines are selected.
	 * Being completely selected means that all characters except the new line characters are
	 * selected.
	 *
	 * @return <code>true</code> if one or multiple lines are selected
	 * 
	 */
	private boolean areMultipleLinesSelected(ITextViewer viewer) {
		if (viewer == null)
			return false;

		Point s= viewer.getSelectedRange();
		if (s.y == 0)
			return false;

		try {

			IDocument document= viewer.getDocument();
			int startLine= document.getLineOfOffset(s.x);
			int endLine= document.getLineOfOffset(s.x + s.y);
			IRegion line= document.getLineInformation(startLine);
			return startLine != endLine || (s.x == line.getOffset() && s.y == line.getLength());

		} catch (BadLocationException x) {
			return false;
		}
	}
}
