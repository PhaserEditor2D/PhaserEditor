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

package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSHintHelper;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;


/**
 * NLS hyperlink detector.
 *
 * 
 */
public class NLSKeyHyperlinkDetector extends AbstractHyperlinkDetector {


	/*
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		ITextEditor textEditor= (ITextEditor)getAdapter(ITextEditor.class);
		if (region == null || textEditor == null || canShowMultipleHyperlinks)
			return null;

		IEditorSite site= textEditor.getEditorSite();
		if (site == null)
			return null;

		IJavaScriptElement javaElement= getInputJavaElement(textEditor);
		if (javaElement == null)
			return null;

		JavaScriptUnit ast= JavaScriptPlugin.getDefault().getASTProvider().getAST(javaElement, ASTProvider.WAIT_NO, null);
		if (ast == null)
			return null;

		ASTNode node= NodeFinder.perform(ast, region.getOffset(), 1);
		if (!(node instanceof StringLiteral)  && !(node instanceof SimpleName))
			return null;
		
		if (node.getLocationInParent() == QualifiedName.QUALIFIER_PROPERTY)
			return null;

		IRegion nlsKeyRegion= new Region(node.getStartPosition(), node.getLength());
		AccessorClassReference ref= NLSHintHelper.getAccessorClassReference(ast, nlsKeyRegion);
		if (ref == null)
			return null;
		String keyName= null;
		if (node instanceof StringLiteral) {
			keyName= ((StringLiteral)node).getLiteralValue();
		} else {
			keyName= ((SimpleName)node).getIdentifier();
		}
		if (keyName != null)
			return new IHyperlink[] {new NLSKeyHyperlink(nlsKeyRegion, keyName, ref, textEditor)};

		return null;
	}

	private IJavaScriptElement getInputJavaElement(ITextEditor editor) {
		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)editorInput).getClassFile();

		if (editor instanceof CompilationUnitEditor)
			return JavaScriptPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput);

		return null;
	}

}
