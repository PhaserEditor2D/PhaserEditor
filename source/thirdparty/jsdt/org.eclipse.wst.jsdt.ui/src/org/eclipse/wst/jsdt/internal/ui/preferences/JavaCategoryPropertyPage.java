/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

/**
 * Top level node for Java property pages
 */
public class JavaCategoryPropertyPage extends PropertyPage {
	
	private IProject fProject;

	private final static String HREF_BUILDPATH= BuildPathsPropertyPage.PROP_ID;
	private final static String HREF_COMPILER= CompliancePreferencePage.PROP_ID;
	private final static String HREF_CODESTYLE= CodeStylePreferencePage.PROP_ID;
	private final static String HREF_JLOC= JavadocConfigurationPropertyPage.PROP_ID;
	private final static String HREF_TODO= TodoTaskPreferencePage.PROP_ID;
	private final static String HREF_PSEVERITIES= ProblemSeveritiesPreferencePage.PROP_ID;
	private final static String HREF_JAVADOC= JavadocProblemsPreferencePage.PROP_ID;
	private final static String HREF_FORMATTER= CodeFormatterPreferencePage.PROP_ID;
	private final static String HREF_TEMPLATES= ""; // Code //$NON-NLS-1$
	private final static String HREF_IMPORTORDER= ImportOrganizePreferencePage.PROP_ID;
	private final static String HREF_BUILDING= JavaBuildPreferencePage.PROP_ID;
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new TableWrapLayout());

        FormToolkit toolkit= new FormToolkit(parent.getDisplay());
        try {
	        String[] args= {
	        		fProject.getName(), HREF_BUILDPATH, HREF_COMPILER, HREF_TODO, HREF_PSEVERITIES, HREF_JAVADOC, HREF_BUILDING,
					HREF_CODESTYLE, HREF_FORMATTER, HREF_TEMPLATES, HREF_IMPORTORDER, HREF_JLOC
	        };
	        String msg= Messages.format(PreferencesMessages.JavaCategoryPropertyPage_text, args); 
	        
	        FormText formText = toolkit.createFormText(composite, true);
	        try {
	            formText.setText(msg, true, false);
	        } catch (SWTException e) {
	            formText.setText(e.getMessage(), false, false);
	        }
	        
	        formText.setBackground(null);
	        formText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
	        formText.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					doLinkActivated(e.data.toString());
				}
	        });
        } finally {
            toolkit.dispose();
        }
        
		Dialog.applyDialogFont(composite);
		return composite;
	}
	
	protected void doLinkActivated(String string) {
		if (string.length() > 0) {
			IPreferencePageContainer container= getContainer();
			if (container instanceof PreferenceDialog) {
				//see bug 80689: ((PreferenceDialog) container).setCurrentPageId(string);
			}
		}
	}



	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
	 */
	public IAdaptable getElement() {
		return fProject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPropertyPage#setElement(org.eclipse.core.runtime.IAdaptable)
	 */
	public void setElement(IAdaptable element) {
		fProject= (IProject) element.getAdapter(IResource.class);
	}

}
