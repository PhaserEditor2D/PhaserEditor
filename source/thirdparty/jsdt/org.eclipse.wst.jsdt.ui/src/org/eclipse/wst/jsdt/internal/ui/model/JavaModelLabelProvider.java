/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.model;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryControlConfiguration;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Label provider for Java models.
 * 
 * 
 */
public final class JavaModelLabelProvider extends AppearanceAwareLabelProvider {

	/** The name of the settings folder */
	private static final String NAME_SETTINGS_FOLDER= ".settings"; //$NON-NLS-1$

	/** The refactoring history label provider */
	private final RefactoringHistoryLabelProvider fHistoryLabelProvider= new RefactoringHistoryLabelProvider(new RefactoringHistoryControlConfiguration(null, false, false));

	/** The project preferences label */
	private final String fPreferencesLabel;

	/** The pending refactorings label */
	private final String fRefactoringsLabel;

	/** The project settings image, or <code>null</code> */
	private Image fSettingsImage= null;

	/**
	 * Creates a new java model label provider.
	 */
	public JavaModelLabelProvider() {
		this(ModelMessages.JavaModelLabelProvider_project_preferences_label, ModelMessages.JavaModelLabelProvider_refactorings_label);
	}

	/**
	 * Creates a new java model label provider.
	 * 
	 * @param preferences
	 *            the preferences label
	 * @param refactorings
	 *            the refactorings label
	 */
	public JavaModelLabelProvider(final String preferences, final String refactorings) {
		super(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaScriptElementLabels.P_COMPRESSED, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS);
		Assert.isNotNull(preferences);
		Assert.isNotNull(refactorings);
		fPreferencesLabel= preferences;
		fRefactoringsLabel= refactorings;
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		super.dispose();
		fHistoryLabelProvider.dispose();
		if (fSettingsImage != null && !fSettingsImage.isDisposed()) {
			fSettingsImage.dispose();
			fSettingsImage= null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Image getImage(final Object element) {
		if (element instanceof IFolder) {
			final IFolder folder= (IFolder) element;
			if (folder.getName().equals(NAME_SETTINGS_FOLDER)) {
				if (fSettingsImage == null || fSettingsImage.isDisposed())
					fSettingsImage= JavaPluginImages.DESC_OBJS_PROJECT_SETTINGS.createImage();
				return decorateImage(fSettingsImage, element);
			}
		}
		Image image= super.getImage(element);
		if (image == null) {
			if (element instanceof RefactoringHistory)
				image= fHistoryLabelProvider.getImage(element);
			else if (element instanceof RefactoringDescriptorProxy)
				image= fHistoryLabelProvider.getImage(element);
			return decorateImage(image, element);
		}
		return image;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getText(final Object element) {
		if (element instanceof IFolder) {
			final IFolder folder= (IFolder) element;
			if (folder.getName().equals(NAME_SETTINGS_FOLDER))
				return decorateText(fPreferencesLabel, element);
		}
		String text= super.getText(element);
		if (text == null || "".equals(text)) { //$NON-NLS-1$
			if (element instanceof RefactoringHistory)
				text= fRefactoringsLabel;
			else if (element instanceof RefactoringDescriptorProxy)
				text= fHistoryLabelProvider.getText(element);
			return decorateText(text, element);
		}
		return text;
	}
}
