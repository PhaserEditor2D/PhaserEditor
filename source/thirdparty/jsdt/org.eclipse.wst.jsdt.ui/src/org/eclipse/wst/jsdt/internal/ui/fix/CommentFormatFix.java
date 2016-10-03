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
package org.eclipse.wst.jsdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.wst.jsdt.internal.ui.text.comment.CommentFormattingStrategy;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;

public class CommentFormatFix implements IFix {
	
	public static IFix createCleanUp(IJavaScriptUnit unit, boolean singleLine, boolean multiLine, boolean javaDoc, HashMap preferences) throws CoreException {
		if (!singleLine && !multiLine && !javaDoc)
			return null;
		
		String content= unit.getBuffer().getContents();
		Document document= new Document(content);
		
		final List edits= format(document, singleLine, multiLine, javaDoc, preferences);
		
		if (edits.size() == 0)
			return null;
		
		MultiTextEdit resultEdit= new MultiTextEdit();
		resultEdit.addChildren((TextEdit[])edits.toArray(new TextEdit[edits.size()]));
		
		TextChange change= new CompilationUnitChange(MultiFixMessages.CommentFormatFix_description, unit);
		change.setEdit(resultEdit);
		
		String label= MultiFixMessages.CommentFormatFix_description;
		CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
		group.addTextEdit(resultEdit);
		change.addTextEditGroup(group);
		
		return new CommentFormatFix(change, unit);
	}
	
	static String format(String input, boolean singleLine, boolean multiLine, boolean javaDoc) {
		if (!singleLine && !multiLine && !javaDoc)
			return input;
		
		HashMap preferences= new HashMap(JavaScriptCore.getOptions());
		Document document= new Document(input);
		List edits= format(document, singleLine, multiLine, javaDoc, preferences);
		
		if (edits.size() == 0)
			return input;
		
		MultiTextEdit resultEdit= new MultiTextEdit();
		resultEdit.addChildren((TextEdit[])edits.toArray(new TextEdit[edits.size()]));
		
		try {
			resultEdit.apply(document);
		} catch (MalformedTreeException e) {
			JavaScriptPlugin.log(e);
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
		return document.get();
	}
	
	private static List format(IDocument document, boolean singleLine, boolean multiLine, boolean javaDoc, HashMap preferences) {
		final List edits= new ArrayList();
		
		JavaScriptPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document, IJavaScriptPartitions.JAVA_PARTITIONING);
		
		String content= document.get();
		
		CommentFormattingStrategy formattingStrategy= new CommentFormattingStrategy();
		
		IFormattingContext context= new CommentFormattingContext();
		context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, preferences);
		context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.TRUE);
		context.setProperty(FormattingContextProperties.CONTEXT_MEDIUM, document);
		
		try {
			ITypedRegion[] regions= TextUtilities.computePartitioning(document, IJavaScriptPartitions.JAVA_PARTITIONING, 0, document.getLength(), false);
			for (int i= 0; i < regions.length; i++) {
				ITypedRegion region= regions[i];
				if (singleLine && region.getType().equals(IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT)) {
					TextEdit edit= format(region, context, formattingStrategy, content);
					if (edit != null)
						edits.add(edit);
				} else if (multiLine && region.getType().equals(IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT)) {
					TextEdit edit= format(region, context, formattingStrategy, content);
					if (edit != null)
						edits.add(edit);
				} else if (javaDoc && region.getType().equals(IJavaScriptPartitions.JAVA_DOC)) {
					TextEdit edit= format(region, context, formattingStrategy, content);
					if (edit != null)
						edits.add(edit);
				}
			}
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		} finally {
			context.dispose();
		}
		
		return edits;
	}
	
	private static TextEdit format(ITypedRegion region, IFormattingContext context, CommentFormattingStrategy formattingStrategy, String content) {
		TypedPosition typedPosition= new TypedPosition(region.getOffset(), region.getLength(), region.getType());
		context.setProperty(FormattingContextProperties.CONTEXT_PARTITION, typedPosition);
		formattingStrategy.formatterStarts(context);
		TextEdit edit= formattingStrategy.calculateTextEdit();
		formattingStrategy.formatterStops();
		if (edit == null)
			return null;
		
		if (!edit.hasChildren())
			return null;
		
		// Filter out noops
		TextEdit[] children= edit.getChildren();
		for (int i= 0; i < children.length; i++) {
			if (!(children[i] instanceof ReplaceEdit))
				return edit;
		}
		
		IDocument doc= new Document(content);
		try {
			edit.copy().apply(doc, TextEdit.NONE);
			if (content.equals(doc.get()))
				return null;
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		}
		
		return edit;
	}
	
	private final IJavaScriptUnit fCompilationUnit;
	private final TextChange fChange;
	
	public CommentFormatFix(TextChange change, IJavaScriptUnit compilationUnit) {
		fChange= change;
		fCompilationUnit= compilationUnit;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public TextChange createChange() throws CoreException {
		return fChange;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IJavaScriptUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return MultiFixMessages.CommentFormatFix_description;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IStatus getStatus() {
		return StatusInfo.OK_STATUS;
	}
}
