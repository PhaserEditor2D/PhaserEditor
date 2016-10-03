/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java.hover;

import java.io.Reader;
import java.io.StringReader;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension4;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLPrinter;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLTextPresenter;
import org.eclipse.wst.jsdt.ui.JSdocContentAccess;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * Provides Javadoc as hover info for Java elements.
 *
 * 
 */
public class JavadocHover extends AbstractJavaEditorTextHover implements IInformationProviderExtension2, ITextHoverExtension {

	
	/**
	 * Presenter control creator.
	 * 
	 * 
	 */
	public static final class PresenterControlCreator extends AbstractReusableInformationControlCreator {
		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			int shellStyle= SWT.RESIZE | SWT.TOOL;
			int style= SWT.V_SCROLL | SWT.H_SCROLL;
			if (BrowserInformationControl.isAvailable(parent))
				return new BrowserInformationControl(parent, PreferenceConstants.APPEARANCE_JAVADOC_FONT, true);
			else
				return new DefaultInformationControl(parent, shellStyle, style, new HTMLTextPresenter(false));
		}
	}

	
	/**
	 * Hover control creator.
	 * 
	 * 
	 */
	public static final class HoverControlCreator extends AbstractReusableInformationControlCreator {
		private IInformationControlCreator fInformationPresenterControlCreator;

		public HoverControlCreator(IInformationControlCreator informationPresenterControlCreator) {
			fInformationPresenterControlCreator= informationPresenterControlCreator;
		}
		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			if (BrowserInformationControl.isAvailable(parent)) {
				BrowserInformationControl iControl = new BrowserInformationControl(parent, PreferenceConstants.APPEARANCE_JAVADOC_FONT, true) {
					public IInformationControlCreator getInformationPresenterControlCreator() {
						return fInformationPresenterControlCreator;
					}
				};
				iControl.setStatusText(EditorsUI.getTooltipAffordanceString());
				return iControl;
			}
			else {
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true), EditorsUI.getTooltipAffordanceString());
			}
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#canReuse(org.eclipse.jface.text.IInformationControl)
		 */
		public boolean canReuse(IInformationControl control) {
			if (!super.canReuse(control))
				return false;
			
			if (control instanceof IInformationControlExtension4)
				((IInformationControlExtension4)control).setStatusText(EditorsUI.getTooltipAffordanceString());
			
			return true;
		}
	}

	private final long LABEL_FLAGS=  JavaScriptElementLabels.ALL_FULLY_QUALIFIED
		| JavaScriptElementLabels.M_PRE_RETURNTYPE | JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.M_PARAMETER_NAMES | JavaScriptElementLabels.M_EXCEPTIONS
		| JavaScriptElementLabels.F_PRE_TYPE_SIGNATURE | JavaScriptElementLabels.M_PRE_TYPE_PARAMETERS | JavaScriptElementLabels.T_TYPE_PARAMETERS
		| JavaScriptElementLabels.USE_RESOLVED;
	private final long LOCAL_VARIABLE_FLAGS= LABEL_FLAGS & ~JavaScriptElementLabels.F_FULLY_QUALIFIED | JavaScriptElementLabels.F_POST_QUALIFIED;

	
	/**
	 * The hover control creator.
	 * 
	 * 
	 */
	private IInformationControlCreator fHoverControlCreator;
	/**
	 * The presentation control creator.
	 * 
	 * 
	 */
	private IInformationControlCreator fPresenterControlCreator;


	/*
	 * @see IInformationProviderExtension2#getInformationPresenterControlCreator()
	 * 
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fPresenterControlCreator == null)
			fPresenterControlCreator= new PresenterControlCreator();
		return fPresenterControlCreator;
	}

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * 
	 */
	public IInformationControlCreator getHoverControlCreator() {
		if (fHoverControlCreator == null)
			fHoverControlCreator= new HoverControlCreator(new JavadocHover.PresenterControlCreator());
		return fHoverControlCreator;
	}

	/*
	 * @see JavaElementHover
	 */
	protected String getHoverInfo(IJavaScriptElement[] result) {

		StringBuffer buffer= new StringBuffer();
		int nResults= result.length;
		if (nResults == 0)
			return null;

		boolean hasContents= false;
		if (nResults > 1) {

			for (int i= 0; i < result.length; i++) {
				HTMLPrinter.startBulletList(buffer);
				IJavaScriptElement curr= result[i];
				if (curr != null && (curr instanceof IMember || curr.getElementType() == IJavaScriptElement.LOCAL_VARIABLE)) {
					HTMLPrinter.addBullet(buffer, getInfoText(curr));
					hasContents= true;
				}
				HTMLPrinter.endBulletList(buffer);
			}

		} else {

			IJavaScriptElement curr= result[0];
			if (curr instanceof IMember) {
				IMember member= (IMember) curr;
				HTMLPrinter.addSmallHeader(buffer, getInfoText(member));
				Reader reader;
				try {
					reader= JSdocContentAccess.getHTMLContentReader(member, true, true);
					
					// Provide hint why there's no Javadoc
					if (reader == null && member.isBinary()) {
						boolean hasAttachedJavadoc= JavaDocLocations.getJavadocBaseLocation(member) != null;
						IPackageFragmentRoot root= (IPackageFragmentRoot)member.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
						boolean hasAttachedSource= root != null && root.getSourceAttachmentPath() != null;
						IOpenable openable= member.getOpenable();
						boolean hasSource= openable.getBuffer() != null;

						if (!hasAttachedSource && !hasAttachedJavadoc)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachments);
						else if (!hasAttachedJavadoc && !hasSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachedJavadoc);
						else if (!hasAttachedSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachedSource);
						else if (!hasSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noInformation);
					}
					
				} catch (JavaScriptModelException ex) {
					reader= new StringReader(JavaHoverMessages.JavadocHover_error_gettingJavadoc);
					JavaScriptPlugin.log(ex.getStatus());
				}
				
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}
				hasContents= true;
			} else if (curr != null && curr.getElementType() == IJavaScriptElement.LOCAL_VARIABLE) {
				Reader reader = null;
				try {
					reader= JSdocContentAccess.getHTMLContentReader((ILocalVariable)curr, false, true);
				}
				catch (JavaScriptModelException e) {
					reader= new StringReader(JavaHoverMessages.JavadocHover_error_gettingJavadoc);
					JavaScriptPlugin.log(e.getStatus());
				}
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}
				else {
					HTMLPrinter.addSmallHeader(buffer, getInfoText(curr));
				}
				
				hasContents= true;
			}
		}
		
		if (!hasContents)
			return null;

		if (buffer.length() > 0) {
			HTMLPrinter.insertPageProlog(buffer, 0, getStyleSheet());
			HTMLPrinter.addPageEpilog(buffer);
			return buffer.toString();
		}

		return null;
	}

	private String getInfoText(IJavaScriptElement member) {
		long flags= member.getElementType() == IJavaScriptElement.LOCAL_VARIABLE ? LOCAL_VARIABLE_FLAGS : LABEL_FLAGS;
		String label= JavaScriptElementLabels.getElementLabel(member, flags);
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < label.length(); i++) {
			char ch= label.charAt(i);
			if (ch == '<') {
				buf.append("&lt;"); //$NON-NLS-1$
			} else if (ch == '>') {
				buf.append("&gt;"); //$NON-NLS-1$
			} else {
				buf.append(ch);
			}
		}
		return buf.toString();
	}

}
