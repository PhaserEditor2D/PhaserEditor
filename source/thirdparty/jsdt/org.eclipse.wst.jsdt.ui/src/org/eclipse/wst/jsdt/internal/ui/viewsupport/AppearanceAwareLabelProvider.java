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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * JavaUILabelProvider that respects settings from the Appearance preference page.
 * Triggers a viewer update when a preference changes.
 */
public class AppearanceAwareLabelProvider extends JavaUILabelProvider implements IPropertyChangeListener, IPropertyListener {

	public final static long DEFAULT_TEXTFLAGS= JavaScriptElementLabels.ROOT_VARIABLE | JavaScriptElementLabels.T_TYPE_PARAMETERS | JavaScriptElementLabels.M_PARAMETER_NAMES |  
		JavaScriptElementLabels.M_APP_TYPE_PARAMETERS | JavaScriptElementLabels.M_APP_RETURNTYPE  | JavaScriptElementLabels.REFERENCED_ROOT_POST_QUALIFIED;
	public final static int DEFAULT_IMAGEFLAGS= JavaElementImageProvider.OVERLAY_ICONS;
	
	private long fTextFlagMask;
	private int fImageFlagMask;
	
	/**
	 * Constructor for AppearanceAwareLabelProvider.
	 */
	public AppearanceAwareLabelProvider(long textFlags, int imageFlags) {
		super(textFlags, imageFlags);
		initMasks();
		PreferenceConstants.getPreferenceStore().addPropertyChangeListener(this);
		PlatformUI.getWorkbench().getEditorRegistry().addPropertyListener(this);
	}

	/**
	 * Creates a labelProvider with DEFAULT_TEXTFLAGS and DEFAULT_IMAGEFLAGS
	 */	
	public AppearanceAwareLabelProvider() {
		this(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
	}
	
	private void initMasks() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		fTextFlagMask= -1;
		if (!store.getBoolean(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE)) {
			fTextFlagMask ^= JavaScriptElementLabels.M_APP_RETURNTYPE;
		}
		if (!store.getBoolean(PreferenceConstants.APPEARANCE_METHOD_TYPEPARAMETERS)) {
			fTextFlagMask ^= JavaScriptElementLabels.M_APP_TYPE_PARAMETERS;
		}
		if (!store.getBoolean(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES)) {
			fTextFlagMask ^= JavaScriptElementLabels.P_COMPRESSED;
		}
		if (!store.getBoolean(PreferenceConstants.APPEARANCE_CATEGORY)) {
			fTextFlagMask ^= JavaScriptElementLabels.ALL_CATEGORY;
		}
		
		fImageFlagMask= -1;
	}

	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (property.equals(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE)
				|| property.equals(PreferenceConstants.APPEARANCE_METHOD_TYPEPARAMETERS)
				|| property.equals(PreferenceConstants.APPEARANCE_CATEGORY)
				|| property.equals(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW)
				|| property.equals(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES)) {
			initMasks();
			LabelProviderChangedEvent lpEvent= new LabelProviderChangedEvent(this, null); // refresh all
			fireLabelProviderChanged(lpEvent);
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object, int)
	 */
	public void propertyChanged(Object source, int propId) {
		if (propId == IEditorRegistry.PROP_CONTENTS) {
			fireLabelProviderChanged(new LabelProviderChangedEvent(this, null)); // refresh all
		}
	}

	/*
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		PreferenceConstants.getPreferenceStore().removePropertyChangeListener(this);
		PlatformUI.getWorkbench().getEditorRegistry().removePropertyListener(this);
		super.dispose();
	}

	/*
	 * @see JavaUILabelProvider#evaluateImageFlags()
	 */
	protected int evaluateImageFlags(Object element) {
		return getImageFlags() & fImageFlagMask;
	}

	/*
	 * @see JavaUILabelProvider#evaluateTextFlags()
	 */
	protected long evaluateTextFlags(Object element) {
		return getTextFlags() & fTextFlagMask;
	}

}
