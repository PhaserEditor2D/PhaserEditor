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
package org.eclipse.wst.jsdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;



public class NewLinesTabPage extends FormatterTabPage {
	
	/**
	 * Constant array for boolean selection 
	 */
	private static String[] FALSE_TRUE = {
		DefaultCodeFormatterConstants.FALSE,
		DefaultCodeFormatterConstants.TRUE
	};	
	
    /**
     * Constant array for insert / not_insert. 
     */
    private static String[] DO_NOT_INSERT_INSERT = {
        JavaScriptCore.DO_NOT_INSERT,
        JavaScriptCore.INSERT
    };
	
	
	private final String PREVIEW= 
	createPreviewHeader(FormatterMessages.NewLinesTabPage_preview_header) + 
	"var someData;\n" + //$NON-NLS-1$
	"var someMoreData = 5;\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"function foo(data) {\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"    var abc = 1;\n" + //$NON-NLS-1$
	"    var xyz = 'one';\n" + //$NON-NLS-1$
	"    var arr1 = ['123',function() {return 5},'abc'];\n" + //$NON-NLS-1$
	"    var obj1 = { make: 'Ford', model: 'Thunderbird', year: 1967 };\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"    if (data>5) {\n" + //$NON-NLS-1$
	"	     data = 5;\n" + //$NON-NLS-1$
	"    } else {\n" + //$NON-NLS-1$
	"	     data--;\n" + //$NON-NLS-1$
	"    }\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"    switch (data) {\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"        case 0:\n" + //$NON-NLS-1$
	"            abc = 0;\n" + //$NON-NLS-1$
	"            xyz = 'zero';\n" + //$NON-NLS-1$
	"            break;\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"        default:\n" + //$NON-NLS-1$
	"            abc = -1;\n" + //$NON-NLS-1$
	"            xyz = 'unknown';\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"     }\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"}"; //$NON-NLS-1$

	

	protected CheckboxPreference fThenStatementPref, fSimpleIfPref;

	private CompilationUnitPreview fPreview;

	public NewLinesTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
	}

	protected void doCreatePreferences(Composite composite, int numColumns) {
		
		final Group newlinesGroup= createGroup(numColumns, composite, FormatterMessages.NewLinesTabPage_newlines_group_title); 
//		createPref(newlinesGroup, numColumns, FormatterMessages.NewLinesTabPage_newlines_group_option_empty_class_body, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION, DO_NOT_INSERT_INSERT); 
//		createPref(newlinesGroup, numColumns, FormatterMessages.NewLinesTabPage_newlines_group_option_empty_anonymous_class_body, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION, DO_NOT_INSERT_INSERT); 
		createPref(newlinesGroup, numColumns, FormatterMessages.NewLinesTabPage_newlines_group_option_empty_method_body, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY, DO_NOT_INSERT_INSERT); 
		createPref(newlinesGroup, numColumns, FormatterMessages.NewLinesTabPage_newlines_group_option_empty_block, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, DO_NOT_INSERT_INSERT); 
//		createPref(newlinesGroup, numColumns, FormatterMessages.NewLinesTabPa/ge_newlines_group_option_empty_enum_declaration, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION, DO_NOT_INSERT_INSERT); 
//		createPref(newlinesGroup, numColumns, FormatterMessages.NewLinesTabPage_newlines_group_option_empty_enum_constant, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT, DO_NOT_INSERT_INSERT);
//		createPref(newlinesGroup, numColumns, FormatterMessages.NewLinesTabPage_newlines_group_option_empty_annotation_decl_body, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION, DO_NOT_INSERT_INSERT); 
		createPref(newlinesGroup, numColumns, FormatterMessages.NewLinesTabPage_newlines_group_option_empty_end_of_file, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AT_END_OF_FILE_IF_MISSING, DO_NOT_INSERT_INSERT); 
		
		final Group objectInitializerGroup= createGroup(numColumns, composite, FormatterMessages.NewLinesTabPage_objectInitializer_group_title); 
		createPref(objectInitializerGroup, numColumns, FormatterMessages.NewLinesTabPage_object_group_option_after_opening_brace_of_object_initializer, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_OBJLIT_INITIALIZER, DO_NOT_INSERT_INSERT); 
		createPref(objectInitializerGroup, numColumns, FormatterMessages.NewLinesTabPage_object_group_option_before_closing_brace_of_object_initializer, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_OBJLIT_INITIALIZER, DO_NOT_INSERT_INSERT);
		createPref(objectInitializerGroup, numColumns, FormatterMessages.NewLinesTabPage_object_group_option_after_comma_in_object_initializer, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_COMMA_IN_OBJLIT_INITIALIZER, DO_NOT_INSERT_INSERT);
		
		final Group arrayInitializerGroup= createGroup(numColumns, composite, FormatterMessages.NewLinesTabPage_arrayInitializer_group_title); 
		createPref(arrayInitializerGroup, numColumns, FormatterMessages.NewLinesTabPage_array_group_option_after_opening_brace_of_array_initializer, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, DO_NOT_INSERT_INSERT); 
		createPref(arrayInitializerGroup, numColumns, FormatterMessages.NewLinesTabPage_array_group_option_before_closing_brace_of_array_initializer, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, DO_NOT_INSERT_INSERT); 

		final Group emptyStatementsGroup= createGroup(numColumns, composite, FormatterMessages.NewLinesTabPage_empty_statement_group_title); 
		createPref(emptyStatementsGroup, numColumns, FormatterMessages.NewLinesTabPage_emtpy_statement_group_option_empty_statement_on_new_line, DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, FALSE_TRUE); 

//		final Group annotationsGroup= createGroup(numColumns, composite, FormatterMessages.NewLinesTabPage_annotations_group_title); 
//		createPref(annotationsGroup, numColumns, FormatterMessages.NewLinesTabPage_annotations_group_option_after_annotation, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION, DO_NOT_INSERT_INSERT); 

	}
	
	protected void initializePage() {
	    fPreview.setPreviewText(PREVIEW);
	}
	
    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fPreview= new CompilationUnitPreview(fWorkingValues, parent);
        return fPreview;
    }

    protected void doUpdatePreview() {
    	super.doUpdatePreview();
        fPreview.update();
    }

	private CheckboxPreference createPref(Composite composite, int numColumns, String message, String key, String[] values) {
		return createCheckboxPref(composite, numColumns, message, key, values);
	}
}
