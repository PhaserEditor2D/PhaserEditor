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
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.texteditor.templates.TemplatesView;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class JavaPerspectiveFactory implements IPerspectiveFactory {
	/**
	 * Constructs a new Default layout engine.
	 */
	public JavaPerspectiveFactory() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
 		String editorArea = layout.getEditorArea();
		
		IFolderLayout folder= layout.createFolder("left", IPageLayout.LEFT, (float)0.25, editorArea); //$NON-NLS-1$
		
		String explorerViewID = ProductProperties.getProperty(IProductConstants.PERSPECTIVE_EXPLORER_VIEW);
		// make sure the specified view ID is known
		if (PlatformUI.getWorkbench().getViewRegistry().find(explorerViewID) != null)
			folder.addView(explorerViewID);
		else
			folder.addView(ProductProperties.ID_PERSPECTIVE_EXPLORER_VIEW);
		
		folder.addPlaceholder(JavaScriptUI.ID_TYPE_HIERARCHY);
		folder.addPlaceholder(IPageLayout.ID_RES_NAV);
		folder.addPlaceholder(JavaScriptUI.ID_PACKAGES);
		
		IFolderLayout outputfolder= layout.createFolder("bottom", IPageLayout.BOTTOM, (float)0.75, editorArea); //$NON-NLS-1$
		outputfolder.addView(IPageLayout.ID_PROBLEM_VIEW);
		
		outputfolder.addView(JavaScriptUI.ID_JAVADOC_VIEW);
		outputfolder.addView(JavaScriptUI.ID_SOURCE_VIEW);
		outputfolder.addPlaceholder(TemplatesView.ID);
		outputfolder.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
		outputfolder.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		outputfolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		outputfolder.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
		outputfolder.addPlaceholder(IPageLayout.ID_TASK_LIST);
		outputfolder.addPlaceholder(IPageLayout.ID_PROP_SHEET);
		
		layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, (float)0.75, editorArea);
		
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(JavaScriptUI.ID_ACTION_SET);
		layout.addActionSet(JavaScriptUI.ID_ELEMENT_CREATION_ACTION_SET);
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);
		
		// views - java
		layout.addShowViewShortcut(JavaScriptUI.ID_PACKAGES);
		layout.addShowViewShortcut(JavaScriptUI.ID_TYPE_HIERARCHY);
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
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.NewJSWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewInterfaceCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewEnumCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewAnnotationCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewSourceFolderCreationWizard");	 //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.wst.jsdt.ui.wizards.NewSnippetFileCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.editors.wizards.UntitledTextFileWizard");//$NON-NLS-1$
	}
}
