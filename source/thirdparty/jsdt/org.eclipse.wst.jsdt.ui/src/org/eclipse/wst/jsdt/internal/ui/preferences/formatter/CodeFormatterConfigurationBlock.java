/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Aaron Luchko, aluchko@redhat.com - 105926 [Formatter] Exporting Unnamed profile fails silently
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences.formatter;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;



/**
 * The code formatter preference page. 
 */

public class CodeFormatterConfigurationBlock extends ProfileConfigurationBlock {
    
    private static final String FORMATTER_DIALOG_PREFERENCE_KEY= "formatter_page"; //$NON-NLS-1$

	private static final String DIALOGSTORE_LASTSAVELOADPATH= JavaScriptUI.ID_PLUGIN + ".codeformatter"; //$NON-NLS-1$
    
	/**
     * Some Java source code used for preview.
     */
    protected final String PREVIEW= "/**\n* " + //$NON-NLS-1$
    		FormatterMessages.CodingStyleConfigurationBlock_preview_title + 
    		"\n*/\n\n" + //$NON-NLS-1$
    		"" + //$NON-NLS-1$
    		"var index=0;" + //$NON-NLS-1$
    		"var arr=[];" + //$NON-NLS-1$
    		"var obj={x:\"\",y:function(){}};" + //$NON-NLS-1$
    		"function foo(){var1= new Object();}" + //$NON-NLS-1$
    		"function pop(){return arr[index--];}" + //$NON-NLS-1$
    		"function push( elem){arr[index++]=elem;}" + //$NON-NLS-1$
    		"function isEmpty() {return index==0;}" + //$NON-NLS-1$
    		""; //$NON-NLS-1$
    
	private class PreviewController implements Observer {

		public PreviewController(ProfileManager profileManager) {
			profileManager.addObserver(this);
			fJavaPreview.setWorkingValues(profileManager.getSelected().getSettings());
			fJavaPreview.update();
		}

		public void update(Observable o, Object arg) {
			final int value= ((Integer)arg).intValue();
			switch (value) {
			case ProfileManager.PROFILE_CREATED_EVENT:
			case ProfileManager.PROFILE_DELETED_EVENT:
			case ProfileManager.SELECTION_CHANGED_EVENT:
			case ProfileManager.SETTINGS_CHANGED_EVENT:
				fJavaPreview.setWorkingValues(((ProfileManager)o).getSelected().getSettings());
				fJavaPreview.update();
			}
		}

	}

    
    /**
	 * The JavaPreview.
	 */
	private JavaPreview fJavaPreview;

	/**
	 * Create a new <code>CodeFormatterConfigurationBlock</code>.
	 */
	public CodeFormatterConfigurationBlock(IProject project, PreferencesAccess access) {
		super(project, access, DIALOGSTORE_LASTSAVELOADPATH);
	}

	protected IProfileVersioner createProfileVersioner() {
	    return new ProfileVersioner();
    }
	
	protected ProfileStore createProfileStore(IProfileVersioner versioner) {
	    return new FormatterProfileStore(versioner);
    }
	
	protected ProfileManager createProfileManager(List profiles, IScopeContext context, PreferencesAccess access, IProfileVersioner profileVersioner) {
	    return new FormatterProfileManager(profiles, context, access, profileVersioner);
    }
	
	protected void configurePreview(Composite composite, int numColumns, ProfileManager profileManager) {
		createLabel(composite, FormatterMessages.CodingStyleConfigurationBlock_preview_label_text, numColumns);
		CompilationUnitPreview result= new CompilationUnitPreview(profileManager.getSelected().getSettings(), composite);
        result.setPreviewText(PREVIEW);
		fJavaPreview= result;

		final GridData gd = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = numColumns;
		gd.verticalSpan= 7;
		gd.widthHint = 0;
		gd.heightHint = 0;
		fJavaPreview.getControl().setLayoutData(gd);
		
		new PreviewController(profileManager);
	}

    
    protected ModifyDialog createModifyDialog(Shell shell, Profile profile, ProfileManager profileManager, ProfileStore profileStore, boolean newProfile) {
        return new FormatterModifyDialog(shell, profile, profileManager, profileStore, newProfile, FORMATTER_DIALOG_PREFERENCE_KEY, DIALOGSTORE_LASTSAVELOADPATH);
    }
}
