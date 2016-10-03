/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;


public class BlankLinesTabPage extends FormatterTabPage {

	private final String PREVIEW=
	createPreviewHeader(FormatterMessages.BlankLinesTabPage_preview_header) + 
	"var data;\n" + //$NON-NLS-1$
	"var data2;\n" + //$NON-NLS-1$
	"var data3;\n" + //$NON-NLS-1$
	"function foo(otherdata) {\n" + //$NON-NLS-1$
	"// " + FormatterMessages.BlankLinesTabPage_preview_comment_between_here + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
	"\n\n\n\n\n" + //$NON-NLS-1$
	"// " + FormatterMessages.BlankLinesTabPage_preview_comment_and_here_are_5_blank_lines + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
	"  var first;\n" + //$NON-NLS-1$
	"  var second;\n" + //$NON-NLS-1$
	"  function foo2() {\n" + //$NON-NLS-1$
	"    var abc;\n" + //$NON-NLS-1$
	"    var xyz;\n" + //$NON-NLS-1$
	"  };\n" + //$NON-NLS-1$
	"}\n"; //$NON-NLS-1$
	
	private final static int MIN_NUMBER_LINES= 0;
	private final static int MAX_NUMBER_LINES= 99;
	

	private CompilationUnitPreview fPreview;
	
	/**
	 * Create a new BlankLinesTabPage.
	 * @param modifyDialog The main configuration dialog
	 * 
	 * @param workingValues The values wherein the options are stored. 
	 */
	public BlankLinesTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
	}

	protected void doCreatePreferences(Composite composite, int numColumns) {
				
	    Group group;
	    
//	  STP - Commented-out next 6 lines (20070207)
//		group= createGroup(numColumns, composite, FormatterMessages.BlankLinesTabPage_compilation_unit_group_title); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_compilation_unit_option_before_package, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_compilation_unit_option_after_package, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_compilation_unit_option_before_import, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_compilation_unit_option_after_import, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_compilation_unit_option_between_type_declarations, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_TYPE_DECLARATIONS); 
		
// STP - Commented-out next 7 lines (20070207)
//		group= createGroup(numColumns, composite, FormatterMessages.BlankLinesTabPage_class_group_title); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_class_option_before_first_decl, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_class_option_before_decls_of_same_kind, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK);
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_class_option_before_member_class_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_class_option_before_field_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_class_option_before_method_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD); 
//		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_class_option_at_beginning_of_method_body, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY); 

// STP - Added next 6 lines (20070207)
group= createGroup(numColumns, composite, FormatterMessages.BlankLinesTabPage_function_group_title); 
createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_function_option_before_first_decl, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION); 
createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_function_option_before_decls_of_same_kind, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK);
createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_function_option_before_field_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD); 
createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_function_option_before_function_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD); 
createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_function_option_at_beginning_of_function_body, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY); 

		group= createGroup(numColumns, composite, FormatterMessages.BlankLinesTabPage_blank_lines_group_title); 
		createBlankLineTextField(group, numColumns, FormatterMessages.BlankLinesTabPage_blank_lines_option_empty_lines_to_preserve, DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE); 
	}
	
	protected void initializePage() {
	    fPreview.setPreviewText(PREVIEW);
	}
	
	/*
	 * A helper method to create a number preference for blank lines.
	 */
	private void createBlankLineTextField(Composite composite, int numColumns, String message, String key) {
		createNumberPref(composite, numColumns, message, key, MIN_NUMBER_LINES, MAX_NUMBER_LINES);
	}

    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doCreateJavaPreview(org.eclipse.swt.widgets.Composite)
     */
    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fPreview= new CompilationUnitPreview(fWorkingValues, parent);
        return fPreview;
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doUpdatePreview()
     */
    protected void doUpdatePreview() {
    	super.doUpdatePreview();
        fPreview.update();
    }
}



















