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
package org.eclipse.wst.jsdt.internal.ui.text.java.hover;

import java.util.Properties;

import org.eclipse.core.resources.IStorage;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLPrinter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSHintHelper;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

/**
 * Provides externalized string as hover info for NLS key.
 *
 * 
 */
public class NLSStringHover extends AbstractJavaEditorTextHover {


	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (!(getEditor() instanceof JavaEditor))
			return null;

		IJavaScriptElement je= getEditorInputJavaElement();
		if (je == null)
			return null;

		// Never wait for an AST in UI thread.
		JavaScriptUnit ast= JavaScriptPlugin.getDefault().getASTProvider().getAST(je, ASTProvider.WAIT_NO, null);
		if (ast == null)
			return null;

		ASTNode node= NodeFinder.perform(ast, offset, 1);
		if (node instanceof StringLiteral) {
			StringLiteral stringLiteral= (StringLiteral)node;
			return new Region(stringLiteral.getStartPosition(), stringLiteral.getLength());
		} else if (node instanceof SimpleName) {
			SimpleName simpleName= (SimpleName)node;
			return new Region(simpleName.getStartPosition(), simpleName.getLength());
		}

		return null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (!(getEditor() instanceof JavaEditor))
			return null;

		IJavaScriptElement je= getEditorInputJavaElement();
		if (je == null)
			return null;

		JavaScriptUnit ast= JavaScriptPlugin.getDefault().getASTProvider().getAST(je, ASTProvider.WAIT_ACTIVE_ONLY, null);
		if (ast == null)
			return null;

		ASTNode node= NodeFinder.perform(ast, hoverRegion.getOffset(), hoverRegion.getLength());
		if (!(node instanceof StringLiteral) && !(node instanceof SimpleName))
			return null;
		
		if (node.getLocationInParent() == QualifiedName.QUALIFIER_PROPERTY)
			return null;

		AccessorClassReference ref= NLSHintHelper.getAccessorClassReference(ast, hoverRegion);
		if (ref == null)
			return null;

		IStorage propertiesFile;
		try {
			propertiesFile= NLSHintHelper.getResourceBundle(je.getJavaScriptProject(), ref);
			if (propertiesFile == null)
				return toHtml(JavaHoverMessages.NLSStringHover_NLSStringHover_PropertiesFileNotDetectedWarning, ""); //$NON-NLS-1$
		} catch (JavaScriptModelException ex) {
			return null;
		}

		final String propertiesFileName= propertiesFile.getName();
		Properties properties= NLSHintHelper.getProperties(propertiesFile);
		if (properties == null)
			return null;
		if (properties.isEmpty())
			return toHtml(propertiesFileName, JavaHoverMessages.NLSStringHover_NLSStringHover_missingKeyWarning);

		String identifier= null;
		if (node instanceof StringLiteral) {
			identifier= ((StringLiteral)node).getLiteralValue();
		} else {
			identifier= ((SimpleName)node).getIdentifier();
		}
		if (identifier == null)
			return null;
		
		String value= properties.getProperty(identifier, null);
		if (value != null)
			value= HTMLPrinter.convertToHTMLContent(value);
		else
			value= JavaHoverMessages.NLSStringHover_NLSStringHover_missingKeyWarning;

		return toHtml(propertiesFileName, value);
	}

	private String toHtml(String header, String string) {

		StringBuffer buffer= new StringBuffer();

		HTMLPrinter.addSmallHeader(buffer, header);
		HTMLPrinter.addParagraph(buffer, string);
		HTMLPrinter.insertPageProlog(buffer, 0);
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	private IJavaScriptElement getEditorInputJavaElement() {
		if (getEditor() instanceof CompilationUnitEditor)
			return JavaScriptPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(getEditor().getEditorInput());
		else if (getEditor() instanceof ClassFileEditor) {
			IEditorInput editorInput= getEditor().getEditorInput();
			if (editorInput instanceof IClassFileEditorInput)
				return ((IClassFileEditorInput)editorInput).getClassFile();

		}
		return null;
	}

}
