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
package org.eclipse.wst.jsdt.internal.ui.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContext2;
import org.eclipse.help.IHelpResource;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTML2TextReader;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.JSdocContentAccess;

import com.ibm.icu.text.BreakIterator;

public class JavadocHelpContext implements IContext2 {
	
	
	public static void displayHelp(String contextId, Object[] selected) throws CoreException {
		IContext context= HelpSystem.getContext(contextId);
		if (context != null) {
			if (selected != null && selected.length > 0) {
				context= new JavadocHelpContext(context, selected);
			}
			PlatformUI.getWorkbench().getHelpSystem().displayHelp(context);
		}
	}
	
	
	private static class JavaUIHelpResource implements IHelpResource {

		private IJavaScriptElement fElement;
		private String fUrl;

		public JavaUIHelpResource(IJavaScriptElement element, String url) {
			fElement= element;
			fUrl= url;
		}

		public String getHref() {
			return fUrl;
		}

		public String getLabel() {
			String label= JavaScriptElementLabels.getTextLabel(fElement, JavaScriptElementLabels.ALL_DEFAULT | JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
			return Messages.format(JavaUIMessages.JavaUIHelp_link_label, label); 
		}
	}	
	

	private IHelpResource[] fHelpResources;
	private String fText;
	private String fTitle;
	
	
	// see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=85719
	private static final boolean BUG_85719_FIXED= false; 

	public JavadocHelpContext(IContext context, Object[] elements) throws JavaScriptModelException {
		Assert.isNotNull(elements);
		if (context instanceof IContext2)
			fTitle= ((IContext2)context).getTitle();

		List helpResources= new ArrayList();

		String javadocSummary= null;
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IJavaScriptElement) {
				IJavaScriptElement element= (IJavaScriptElement) elements[i];
				// if element isn't on the build path skip it
				if (!ActionUtil.isOnBuildPath(element))
					continue;
				
				// Create Javadoc summary
				if (BUG_85719_FIXED) {
					if (javadocSummary == null) {
						javadocSummary= retrieveText(element);
						if (javadocSummary != null) {
							String elementLabel= JavaScriptElementLabels.getTextLabel(element, JavaScriptElementLabels.ALL_DEFAULT);
							
							// FIXME: needs to be NLSed once the code becomes active
							javadocSummary= "<b>Javadoc for " + elementLabel + ":</b><br>" + javadocSummary;   //$NON-NLS-1$//$NON-NLS-2$
						}
					} else {
						javadocSummary= ""; // no Javadoc summary for multiple selection //$NON-NLS-1$
					}	
				}
				
				URL url= JavaScriptUI.getJSdocLocation(element, true);
				if (url == null || doesNotExist(url)) {
					IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
					if (root != null) {
						url= JavaScriptUI.getJSdocBaseLocation(element);
						if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
							element= element.getJavaScriptProject();
						} else {
							element= root;
						}
						url= JavaScriptUI.getJSdocLocation(element, false);
					}
				}
				if (url != null) {
					IHelpResource javaResource= new JavaUIHelpResource(element, getURLString(url)); 
					helpResources.add(javaResource);
				}
			}
		}

		// Add static help topics
		if (context != null) {
			IHelpResource[] resources= context.getRelatedTopics();
			if (resources != null) {
				for (int j= 0; j < resources.length; j++) {
					helpResources.add(resources[j]);
				}
			}
		}
		
		fHelpResources= (IHelpResource[]) helpResources.toArray(new IHelpResource[helpResources.size()]);

		if (context != null)
			fText= context.getText();
		
		if (BUG_85719_FIXED) {
			if (javadocSummary != null && javadocSummary.length() > 0) {
				if (fText != null)
					fText= context.getText() + "<br><br>" + javadocSummary; //$NON-NLS-1$
				else
					fText= javadocSummary;
			}
		}
		
		if (fText == null)
			fText= "";  //$NON-NLS-1$
		
	}
	
	private String getURLString(URL url) {
		String location= url.toExternalForm();
		if (url.getRef() != null) {
			int anchorIdx= location.lastIndexOf('#');
			if (anchorIdx != -1) {
				return location.substring(0, anchorIdx) + "?noframes=true" + location.substring(anchorIdx); //$NON-NLS-1$
			}
		}
		return location + "?noframes=true"; //$NON-NLS-1$
	}

	private boolean doesNotExist(URL url) {
		if (url.getProtocol().equals("file")) { //$NON-NLS-1$
			File file= new File(url.getFile());
			return !file.exists();
		}
		return false;
	}

	private String retrieveText(IJavaScriptElement elem) throws JavaScriptModelException {
		if (elem instanceof IMember) {
			Reader reader= JSdocContentAccess.getHTMLContentReader((IMember)elem, true, true);
			if (reader != null)
				reader= new HTML2TextReader(reader, null);
			if (reader != null) {
				String str= getString(reader);
				BreakIterator breakIterator= BreakIterator.getSentenceInstance();
				breakIterator.setText(str);
				return str.substring(0, breakIterator.next());
			}
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * Gets the reader content as a String
	 */
	private static String getString(Reader reader) {
		StringBuffer buf= new StringBuffer();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}

	public IHelpResource[] getRelatedTopics() {
		return fHelpResources;
	}

	public String getText() {
		return fText;
	}

	public String getStyledText() {
		return fText;
	}

	public String getCategory(IHelpResource topic) {
		if (topic instanceof JavaUIHelpResource)
			return JavaUIMessages.JavaUIHelpContext_javaHelpCategory_label; 

		return null;
	}
	
	/*
	 * @see org.eclipse.help.IContext2#getTitle()
	 * 
	 */
	public String getTitle() {
		return fTitle;
	}

}

