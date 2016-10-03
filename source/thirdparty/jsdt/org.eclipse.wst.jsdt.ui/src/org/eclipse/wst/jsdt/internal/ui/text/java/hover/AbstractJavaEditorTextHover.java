/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java.hover;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLPrinter;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLTextPresenter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.wst.jsdt.core.ICodeAssist;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.WorkingCopyManager;
import org.eclipse.wst.jsdt.internal.ui.text.JavaWordFinder;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.java.hover.IJavaEditorTextHover;
import org.osgi.framework.Bundle;

/**
 * Abstract class for providing hover information for Java elements.
 *
 * 
 */
public abstract class AbstractJavaEditorTextHover implements IJavaEditorTextHover, ITextHoverExtension {
	/**
	 * The style sheet (css).
	 * 
	 */
	private static String fgStyleSheet;
	private IEditorPart fEditor;

	/*
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		fEditor= editor;
	}

	protected IEditorPart getEditor() {
		return fEditor;
	}

	protected ICodeAssist getCodeAssist() {
		if (fEditor != null) {
			IEditorInput input= fEditor.getEditorInput();
			if (input instanceof IClassFileEditorInput) {
				IClassFileEditorInput cfeInput= (IClassFileEditorInput) input;
				return cfeInput.getClassFile();
			}

			WorkingCopyManager manager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
			return manager.getWorkingCopy(input, false);
		}

		return null;
	}

	/*
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return JavaWordFinder.findWord(textViewer.getDocument(), offset);
	}

	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		
		/*
		 * The region should be a word region an not of length 0.
		 * This check is needed because codeSelect(...) also finds
		 * the Java element if the offset is behind the word.
		 */
		if (hoverRegion.getLength() == 0)
			return null;
		
		ICodeAssist resolve= getCodeAssist();
		if (resolve != null) {
			try {
				IJavaScriptElement[] result= resolve.codeSelect(hoverRegion.getOffset(), hoverRegion.getLength());
				if (result == null)
					return null;

				int nResults= result.length;
				if (nResults == 0)
					return null;

				return getHoverInfo(result);

			} catch (JavaScriptModelException x) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Provides hover information for the given Java elements.
	 *
	 * @param javaElements the Java elements for which to provide hover information
	 * @return the hover information string
	 * 
	 */
	protected String getHoverInfo(IJavaScriptElement[] javaElements) {
		return null;
	}

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * 
	 */
	public IInformationControlCreator getHoverControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true), EditorsUI.getTooltipAffordanceString());
			}
		};
	}

	protected static String getStyleSheet() {
		if (fgStyleSheet == null)
			fgStyleSheet= loadStyleSheet();
		String css= fgStyleSheet;
		if (css != null) {
			FontData fontData= JFaceResources.getFontRegistry().getFontData(PreferenceConstants.APPEARANCE_JAVADOC_FONT)[0];
			css= HTMLPrinter.convertTopLevelFont(css, fontData);
		}

		return css;
	}
	
	private static String loadStyleSheet() {
		Bundle bundle= Platform.getBundle(JavaScriptPlugin.getPluginId());
		URL styleSheetURL= bundle.getEntry("/JavadocHoverStyleSheet.css"); //$NON-NLS-1$
		if (styleSheetURL != null) {
			try {
				styleSheetURL= FileLocator.toFileURL(styleSheetURL);
				StringBuffer buffer= new StringBuffer(200);
				BufferedReader reader= new BufferedReader(new InputStreamReader(styleSheetURL.openStream()));
				try {
					String line= reader.readLine();
					while (line != null) {
						buffer.append(line);
						buffer.append('\n');
						line= reader.readLine();
					}
				} finally {
					reader.close();
				}
				return buffer.toString();
			} catch (IOException ex) {
				JavaScriptPlugin.log(ex);
				return ""; //$NON-NLS-1$
			}
		}
		return null;
	}
}
