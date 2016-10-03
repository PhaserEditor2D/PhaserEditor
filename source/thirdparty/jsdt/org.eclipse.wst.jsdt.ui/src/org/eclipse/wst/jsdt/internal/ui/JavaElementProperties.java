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

package org.eclipse.wst.jsdt.internal.ui;


import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;

public class JavaElementProperties implements IPropertySource {
	
	private IJavaScriptElement fSource;
	
	// Property Descriptors
	private static final IPropertyDescriptor[] fgPropertyDescriptors= new IPropertyDescriptor[1];
	static {
		PropertyDescriptor descriptor;

		// resource name
		descriptor= new PropertyDescriptor(IBasicPropertyConstants.P_TEXT, JavaUIMessages.JavaElementProperties_name); 
		descriptor.setAlwaysIncompatible(true);
		fgPropertyDescriptors[0]= descriptor;
	}
	
	public JavaElementProperties(IJavaScriptElement source) {
		fSource= source;
	}
	
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return fgPropertyDescriptors;
	}
	
	public Object getPropertyValue(Object name) {
		if (name.equals(IBasicPropertyConstants.P_TEXT)) {
			return fSource.getDisplayName();
		}
		return null;
	}
	
	public void setPropertyValue(Object name, Object value) {
	}
	
	public Object getEditableValue() {
		return this;
	}
	
	public boolean isPropertySet(Object property) {
		return false;
	}
	
	public void resetPropertyValue(Object property) {
	}
}
