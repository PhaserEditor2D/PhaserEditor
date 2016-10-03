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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;


public class JavaBrowsingPerspectiveFactory implements IPerspectiveFactory {

	/*
	 * XXX: This is a workaround for: http://dev.eclipse.org/bugs/show_bug.cgi?id=13070
	 */
	static IJavaScriptElement fgJavaElementFromAction;

	/**
	 * Constructs a new Default layout engine.
	 */
	public JavaBrowsingPerspectiveFactory() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
		if (stackBrowsingViewsVertically())
			createVerticalLayout(layout);
		else
			createHorizontalLayout(layout);

		// action sets
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(JavaScriptUI.ID_ACTION_SET);
		layout.addActionSet(JavaScriptUI.ID_ELEMENT_CREATION_ACTION_SET);
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

		// views - java
		layout.addShowViewShortcut(JavaScriptUI.ID_TYPE_HIERARCHY);
		layout.addShowViewShortcut(JavaScriptUI.ID_PACKAGES);
		layout.addShowViewShortcut(JavaScriptUI.ID_PROJECTS_VIEW);
		layout.addShowViewShortcut(JavaScriptUI.ID_PACKAGES_VIEW);
		layout.addShowViewShortcut(JavaScriptUI.ID_TYPES_VIEW);
		layout.addShowViewShortcut(JavaScriptUI.ID_MEMBERS_VIEW);
		layout.addShowViewShortcut(JavaScriptUI.ID_SOURCE_VIEW);
		layout.addShowViewShortcut(JavaScriptUI.ID_JAVADOC_VIEW);

		// views - search
		layout.addShowViewShortcut(NewSearchUI.SEARCH_VIEW_ID);

		// views - debugging
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);

		// views - standard workbench
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
		layout.addShowViewShortcut(IProgressConstants.PROGRESS_VIEW_ID);

		// new actions - Java project creation wizard
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.JavaProjectWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewPackageCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewClassCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewInterfaceCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewEnumCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewAnnotationCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewSourceFolderCreationWizard");	 //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewSnippetFileCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.editors.wizards.UntitledTextFileWizard");//$NON-NLS-1$
	}

	private void createVerticalLayout(IPageLayout layout) {
		String relativePartId= IPageLayout.ID_EDITOR_AREA;
		int relativePos= IPageLayout.LEFT;

		IPlaceholderFolderLayout placeHolderLeft= layout.createPlaceholderFolder("left", IPageLayout.LEFT, (float)0.25, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		placeHolderLeft.addPlaceholder(JavaScriptUI.ID_TYPE_HIERARCHY);
		placeHolderLeft.addPlaceholder(IPageLayout.ID_OUTLINE);
		placeHolderLeft.addPlaceholder(JavaScriptUI.ID_PACKAGES);
		placeHolderLeft.addPlaceholder(IPageLayout.ID_RES_NAV);

		if (shouldShowProjectsView()) {
			layout.addView(JavaScriptUI.ID_PROJECTS_VIEW, IPageLayout.LEFT, (float)0.25, IPageLayout.ID_EDITOR_AREA);
			relativePartId= JavaScriptUI.ID_PROJECTS_VIEW;
			relativePos= IPageLayout.BOTTOM;
		}
		if (shouldShowPackagesView()) {
			layout.addView(JavaScriptUI.ID_PACKAGES_VIEW, relativePos, (float)0.25, relativePartId);
			relativePartId= JavaScriptUI.ID_PACKAGES_VIEW;
			relativePos= IPageLayout.BOTTOM;
		}
		layout.addView(JavaScriptUI.ID_TYPES_VIEW, relativePos, (float)0.33, relativePartId);
		layout.addView(JavaScriptUI.ID_MEMBERS_VIEW, IPageLayout.BOTTOM, (float)0.50, JavaScriptUI.ID_TYPES_VIEW);

		IPlaceholderFolderLayout placeHolderBottom= layout.createPlaceholderFolder("bottom", IPageLayout.BOTTOM, (float)0.75, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		placeHolderBottom.addPlaceholder(IPageLayout.ID_PROBLEM_VIEW);
		placeHolderBottom.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
		placeHolderBottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		placeHolderBottom.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		placeHolderBottom.addPlaceholder(JavaScriptUI.ID_SOURCE_VIEW);
		placeHolderBottom.addPlaceholder(JavaScriptUI.ID_JAVADOC_VIEW);
		placeHolderBottom.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
	}

	private void createHorizontalLayout(IPageLayout layout) {
		String relativePartId= IPageLayout.ID_EDITOR_AREA;
		int relativePos= IPageLayout.TOP;

		if (shouldShowProjectsView()) {
			layout.addView(JavaScriptUI.ID_PROJECTS_VIEW, IPageLayout.TOP, (float)0.25, IPageLayout.ID_EDITOR_AREA);
			relativePartId= JavaScriptUI.ID_PROJECTS_VIEW;
			relativePos= IPageLayout.RIGHT;
		}
		if (shouldShowPackagesView()) {
			layout.addView(JavaScriptUI.ID_PACKAGES_VIEW, relativePos, (float)0.25, relativePartId);
			relativePartId= JavaScriptUI.ID_PACKAGES_VIEW;
			relativePos= IPageLayout.RIGHT;
		}
		layout.addView(JavaScriptUI.ID_TYPES_VIEW, relativePos, (float)0.33, relativePartId);
		layout.addView(JavaScriptUI.ID_MEMBERS_VIEW, IPageLayout.RIGHT, (float)0.50, JavaScriptUI.ID_TYPES_VIEW);

		IPlaceholderFolderLayout placeHolderLeft= layout.createPlaceholderFolder("left", IPageLayout.LEFT, (float)0.25, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		placeHolderLeft.addPlaceholder(JavaScriptUI.ID_TYPE_HIERARCHY);
		placeHolderLeft.addPlaceholder(IPageLayout.ID_OUTLINE);
		placeHolderLeft.addPlaceholder(JavaScriptUI.ID_PACKAGES);
		placeHolderLeft.addPlaceholder(IPageLayout.ID_RES_NAV);


		IPlaceholderFolderLayout placeHolderBottom= layout.createPlaceholderFolder("bottom", IPageLayout.BOTTOM, (float)0.75, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		placeHolderBottom.addPlaceholder(IPageLayout.ID_PROBLEM_VIEW);
		placeHolderBottom.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
		placeHolderBottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		placeHolderBottom.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		placeHolderBottom.addPlaceholder(JavaScriptUI.ID_SOURCE_VIEW);
		placeHolderBottom.addPlaceholder(JavaScriptUI.ID_JAVADOC_VIEW);
		placeHolderBottom.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
	}

	private boolean shouldShowProjectsView() {
		return fgJavaElementFromAction == null || fgJavaElementFromAction.getElementType() == IJavaScriptElement.JAVASCRIPT_MODEL;
	}

	private boolean shouldShowPackagesView() {
		if (fgJavaElementFromAction == null)
			return true;
		int type= fgJavaElementFromAction.getElementType();
		return type == IJavaScriptElement.JAVASCRIPT_MODEL || type == IJavaScriptElement.JAVASCRIPT_PROJECT || type == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT;
	}

	private boolean stackBrowsingViewsVertically() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.BROWSING_STACK_VERTICALLY);
	}

	/*
	 * XXX: This is a workaround for: http://dev.eclipse.org/bugs/show_bug.cgi?id=13070
	 */
	static void setInputFromAction(IAdaptable input) {
		if (input instanceof IJavaScriptElement)
			fgJavaElementFromAction= (IJavaScriptElement)input;
		else
			fgJavaElementFromAction= null;
	}
}
