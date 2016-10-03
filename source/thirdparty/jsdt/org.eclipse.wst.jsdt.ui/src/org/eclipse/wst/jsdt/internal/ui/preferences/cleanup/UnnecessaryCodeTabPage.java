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
package org.eclipse.wst.jsdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.StringCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ModifyDialog;

public final class UnnecessaryCodeTabPage extends CleanUpTabPage {
	
    public UnnecessaryCodeTabPage(ModifyDialog dialog, Map values) {
	    this(dialog, values, false);
    }

    public UnnecessaryCodeTabPage(IModificationListener listener, Map values, boolean isSaveParticipantConfiguration) {
    	super(listener, values, isSaveParticipantConfiguration);
    }
    
    protected ICleanUp[] createPreviewCleanUps(Map values) {
    	return new ICleanUp[] {
        		new UnusedCodeCleanUp(values),
        		new UnnecessaryCodeCleanUp(values),
        		new StringCleanUp(values)
        };
    }

    protected void doCreatePreferences(Composite composite, int numColumns) {
    	
    	Group unusedCodeGroup= createGroup(5, composite, CleanUpMessages.UnnecessaryCodeTabPage_GroupName_UnusedCode);
    	
//    	CheckboxPreference removeImports= createCheckboxPref(unusedCodeGroup, 5, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedImports, CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpModifyDialog.FALSE_TRUE);
//    	registerPreference(removeImports);
    	
    	final CheckboxPreference unusedMembersPref= createCheckboxPref(unusedCodeGroup, 5, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedMembers, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpModifyDialog.FALSE_TRUE);    	
		intent(unusedCodeGroup);		
//		final CheckboxPreference typesPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedTypes, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpModifyDialog.FALSE_TRUE);
//		final CheckboxPreference constructorPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedConstructors, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference fieldsPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedFields, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference methodsPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedMethods, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(unusedMembersPref, new CheckboxPreference[] {/*typesPref, constructorPref, */fieldsPref, methodsPref});
    			
    	CheckboxPreference removeLocals= createCheckboxPref(unusedCodeGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedLocalVariables, CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(removeLocals);
    	
    	Group unnecessaryGroup= createGroup(numColumns, composite, CleanUpMessages.UnnecessaryCodeTabPage_GroupName_UnnecessaryCode);
//    	
//    	CheckboxPreference casts= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryCasts, CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpModifyDialog.FALSE_TRUE);
//    	registerPreference(casts);
    	
    	CheckboxPreference nls= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryNLSTags, CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(nls);
    }

}