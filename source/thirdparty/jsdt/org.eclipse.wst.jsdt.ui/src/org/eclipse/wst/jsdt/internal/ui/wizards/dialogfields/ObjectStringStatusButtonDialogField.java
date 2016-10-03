/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * @author childsb
 *
 */
public class ObjectStringStatusButtonDialogField extends StringButtonDialogField {

	Object enclosedObject;
	String description;

	public ObjectStringStatusButtonDialogField(IStringButtonAdapter adapter) {
		super(adapter);
		//super.setTextFieldEditable(false);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField#getTextControl(org.eclipse.swt.widgets.Composite)
	 */
	public Text getTextControl(Composite parent) {
		
		Text superTextControl =  super.getTextControl(parent);
		if(superTextControl!=null) superTextControl.setEditable(false);
		return superTextControl;
	}


	public void setDescription(String description) {
		this.description=description;
	}
	
	public String getDescription() {
		if(description==null && enclosedObject==null)
			return ""; //$NON-NLS-1$
		return description==null?enclosedObject.toString():description;
	}
	
	public void setValue(Object newValue) {
		this.enclosedObject = newValue;
		updateStatusField();
	}
	
	private void updateStatusField() {
		if(description==null && enclosedObject==null)
			return;
		
		setText(description==null?enclosedObject.toString():description);
	}
	
	public Object getValue() {
		return enclosedObject;
	}
	
	
	
}
