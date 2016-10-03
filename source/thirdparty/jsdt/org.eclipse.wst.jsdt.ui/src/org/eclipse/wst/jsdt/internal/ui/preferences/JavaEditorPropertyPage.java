/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class JavaEditorPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
	
	public JavaEditorPropertyPage() {}
	
	protected Control createContents(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout());
		
		Link link= new Link(composite, SWT.WRAP);
		GridData data= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.widthHint= 300;
		link.setLayoutData(data);
		link.setText(PreferencesMessages.JavaEditorPropertyPage_SaveActionLink_Text);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer)getContainer();
				container.openPage(SaveParticipantPreferencePage.PROPERTY_PAGE_ID, null);
			}
		});
		
		return composite;
	}
	
}
