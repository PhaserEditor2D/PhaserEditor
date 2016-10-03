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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls;

import java.util.List;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSSubstitution;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 *
 */
public class RenameKeysDialog extends StatusDialog {

	private StringDialogField fNameField;
	private List fSelectedSubstitutions;
	private int fCommonPrefixLength;
	
	/**
	 * @param parent
	 */
	public RenameKeysDialog(Shell parent, List selectedSubstitutions) {
		super(parent);
		setTitle(NLSUIMessages.RenameKeysDialog_title); 

		fSelectedSubstitutions= selectedSubstitutions;
		String prefix= getInitialPrefix(selectedSubstitutions);
		fCommonPrefixLength= prefix.length();
		
		fNameField= new StringDialogField();
		fNameField.setText(prefix);
		
		if (prefix.length() == 0) {
			fNameField.setLabelText(NLSUIMessages.RenameKeysDialog_description_noprefix); 
		} else {
			fNameField.setLabelText(NLSUIMessages.RenameKeysDialog_description_withprefix + prefix + ':'); 
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		
		fNameField.doFillIntoGrid(composite, 2);
		LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		String prefix= fNameField.getText();
		for (int i= 0; i < fSelectedSubstitutions.size(); i++) {
			NLSSubstitution sub= (NLSSubstitution) fSelectedSubstitutions.get(i);
			String newKey= prefix + sub.getKey().substring(fCommonPrefixLength);
			sub.setKey(newKey);
		}
		super.okPressed();
	}
		
	private String getInitialPrefix(List selectedSubstitutions) {
		String prefix= null;
		for (int i= 0; i < selectedSubstitutions.size(); i++) {
			NLSSubstitution sub= (NLSSubstitution) selectedSubstitutions.get(i);
			String curr= sub.getKey();
			if (prefix == null) {
				prefix= curr;
			} else if (!curr.startsWith(prefix)) {
				prefix= getCommonPrefix(prefix, curr);
				if (prefix.length() == 0) {
					return prefix;
				}
			}
		}
		return prefix;
	}

	private String getCommonPrefix(String a, String b) {
		String shorter= a.length() <= b.length() ? a : b;
		int len= shorter.length();
		for (int i= 0; i < len; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return shorter;
	}
	

}
