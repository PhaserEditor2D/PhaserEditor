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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;


public class ProfileVersioner implements IProfileVersioner {
	
	public static final String CODE_FORMATTER_PROFILE_KIND= "CodeFormatterProfile"; //$NON-NLS-1$
	
	private static final int VERSION_1= 1; // < 20040113 (includes M6)
	private static final int VERSION_2= 2; // before renaming almost all
	private static final int VERSION_3= 3; // after renaming almost all
	private static final int VERSION_4= 4; 
	private static final int VERSION_5= 5; // after splitting of FORMATTER_INDENT_BLOCK_STATEMENTS
	private static final int VERSION_6= 6; // after splitting of new_line_in_control_statements
	private static final int VERSION_7= 7; // after moving comment formatter to JSDT Core
	private static final int VERSION_8= 8; // fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=89739
	private static final int VERSION_9= 9; // after storing project profile names in preferences
	private static final int VERSION_10= 10; // splitting options for annotation types
	private static final int VERSION_11= 11; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=49412
	
	private static final int CURRENT_VERSION= VERSION_11;
	
	public int getFirstVersion() {
	    return VERSION_1;
    }

	public int getCurrentVersion() {
	    return CURRENT_VERSION;
    }
	
	/**
     * {@inheritDoc}
     */
    public String getProfileKind() {
	    return CODE_FORMATTER_PROFILE_KIND;
    }

	public void update(CustomProfile profile) {
		final Map oldSettings= profile.getSettings();
		Map newSettings= updateAndComplete(oldSettings, profile.getVersion());
		profile.setVersion(CURRENT_VERSION);
		profile.setSettings(newSettings);
	}
	
	private static Map updateAndComplete(Map oldSettings, int version) {
		final Map newSettings= FormatterProfileManager.getDefaultSettings();
		
		switch (version) {

		case VERSION_1:
			version1to2(oldSettings);
			
		case VERSION_2:
			version2to3(oldSettings);
			
		case VERSION_3:
		    version3to4(oldSettings);
		    
		case VERSION_4:
		    version4to5(oldSettings);
		    
		case VERSION_5:
		    version5to6(oldSettings);
			
		case VERSION_6:
		    version6to7(oldSettings);
		
		case VERSION_7:
		case VERSION_8:
		case VERSION_9:
		    version9to10(oldSettings);

		case VERSION_10 :
			version10to11(oldSettings);
		    
		default:
		    for (final Iterator iter= oldSettings.keySet().iterator(); iter.hasNext(); ) {
		        final String key= (String)iter.next();
		        if (!newSettings.containsKey(key)) 
		            continue;
		        
		        final String value= (String)oldSettings.get(key);
		        if (value != null) {
		            newSettings.put(key, value);
		        }
		    }

		}
		setLatestCompliance(newSettings);
		return newSettings;
	}
	
	/**
	 * Updates the map to use the latest the source compliance
	 * @param map The map to update
	 */
	public static void setLatestCompliance(Map map) {
		JavaModelUtil.set50CompilanceOptions(map);
	}
		
	private static void version1to2(final Map oldSettings) {
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND,	
			FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND,
			FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			JavaScriptCore.PLUGIN_ID + ".formatter.inset_space_between_empty_arguments", //$NON-NLS-1$
			FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN,
			FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
	}

	public static int getVersionStatus(CustomProfile profile) {
		final int version= profile.getVersion();
		if (version < CURRENT_VERSION) 
			return -1;
		else if (version > CURRENT_VERSION)
			return 1;
		else 
			return 0;
	}
	
	
	private static void mapOldValueRangeToNew(Map settings, String oldKey, String [] oldValues,
		String newKey, String [] newValues) {

		if (!settings.containsKey(oldKey)) 
			return;
		
		final String value= ((String)settings.get(oldKey));

		if (value == null) 
			return;
		
		for (int i = 0; i < oldValues.length; i++) {
			if (value.equals(oldValues[i])) {
				settings.put(newKey, newValues[i]);
			}
		}

	}
	
	private static void duplicate(Map settings, String existingKey, String newKey) {
		checkAndReplace(settings, existingKey, new String [] {newKey});
	}
	
	private static void checkAndReplace(Map settings, String oldKey, String newKey) {
		checkAndReplace(settings, oldKey, new String [] {newKey});
	}
	
	private static void checkAndReplace(Map settings, String oldKey, String newKey1, String newKey2) {
		checkAndReplace(settings, oldKey, new String [] {newKey1, newKey2});
	}

	private static void checkAndReplace(Map settings, String oldKey, String [] newKeys) {
		if (!settings.containsKey(oldKey)) 
			return;
		
		final String value= (String)settings.get(oldKey);

		if (value == null) 
			return;
		
		for (int i = 0; i < newKeys.length; i++) {
			settings.put(newKeys[i], value);
		}
	}
	
	private static void checkAndReplaceBooleanWithINSERT(Map settings, String oldKey, String newKey) {
		if (!settings.containsKey(oldKey)) 
			return;
		
		String value= (String)settings.get(oldKey);

		if (value == null) 
			return;
		
		if (DefaultCodeFormatterConstants.TRUE.equals(value))
			value= JavaScriptCore.INSERT;
		else
			value= JavaScriptCore.DO_NOT_INSERT;
		
		settings.put(newKey, value);
	}
	
	
	private static void version2to3(Map oldSettings) {

		checkAndReplace(oldSettings, 
			FORMATTER_ARRAY_INITIALIZER_CONTINUATION_INDENTATION,
			DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_BLOCK_CLOSE_BRACE,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_IN_CATCH_EXPRESSION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH);

		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_IN_FOR_PARENS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_IN_IF_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_IN_SWITCH_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_IN_SYNCHRONIZED_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_IN_WHILE_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE);

		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MESSAGESEND_ARGUMENTS,  
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS);

		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS);

		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_THROWS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_THROWS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_THROWS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_THROWS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);

		checkAndReplace(oldSettings, 
		    FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS,
		    DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);	 

		checkAndReplace(oldSettings, 
		    FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS,
		    DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);	 

		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MESSAGESEND_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_FIRST_ARGUMENT, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_ANONYMOUS_TYPE_OPEN_BRACE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_BLOCK_OPEN_BRACE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_CATCH_EXPRESSION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH);

		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_METHOD_OPEN_BRACE,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_FIRST_INITIALIZER, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER);	
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_FOR_PAREN, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_IF_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_SWITCH_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_SWITCH_OPEN_BRACE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_SYNCHRONIZED_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_TYPE_OPEN_BRACE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_WHILE_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_REFERENCE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE);

		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARRAY_INITIALIZER, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER);
			 
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_MESSAGESEND_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION);
			
		checkAndReplace(oldSettings, 
			FORMATTER_FORMAT_GUARDIAN_CLAUSE_ON_ONE_LINE, 
			DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_REFERENCE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE);
			
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_TYPE_REFERENCE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE);
		
		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATORS,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR); 

		checkAndReplace(oldSettings, 
			FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATORS,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR); 

		checkAndReplace(oldSettings,
			FORMATTER_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT, 
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_COMPACT_IF_ALIGNMENT, 
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF);
		
		checkAndReplace(oldSettings, 
			FORMATTER_MESSAGE_SEND_ARGUMENTS_ALIGNMENT ,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_QUALIFIED_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT ,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_BINARY_EXPRESSION_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_COMPACT_IF_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF);
		
		checkAndReplace(oldSettings, 
			FORMATTER_CONDITIONAL_EXPRESSION_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_ARRAY_INITIALIZER_EXPRESSIONS_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER);
		
		checkAndReplace(oldSettings, 
			FORMATTER_METHOD_DECLARATION_ARGUMENTS_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_MESSAGE_SEND_SELECTOR_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_TYPE_DECLARATION_SUPERCLASS_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_TYPE_DECLARATION_SUPERINTERFACES_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_METHOD_THROWS_CLAUSE_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_EXPLICIT_CONSTRUCTOR_ARGUMENTS_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL);

		
		mapOldValueRangeToNew(oldSettings, 
			FORMATTER_TYPE_MEMBER_ALIGNMENT, new String [] {FORMATTER_NO_ALIGNMENT,	FORMATTER_MULTICOLUMN}, 
			DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS, new String [] {DefaultCodeFormatterConstants.FALSE, DefaultCodeFormatterConstants.TRUE});


		checkAndReplace(oldSettings, 
			FORMATTER_ANONYMOUS_TYPE_DECLARATION_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_ARRAY_INITIALIZER_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER);
		
		checkAndReplace(oldSettings, 
			FORMATTER_BLOCK_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK);
		
		checkAndReplace(oldSettings, 
			FORMATTER_METHOD_DECLARATION_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION);
		
		checkAndReplace(oldSettings, 
			FORMATTER_TYPE_DECLARATION_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION);
		
		checkAndReplace(oldSettings,
			FORMATTER_SWITCH_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH);
		
	}
	
	private static void version3to4(Map oldSettings) {
		checkAndReplace(oldSettings, 
			"org.eclipse.wst.jsdt.core.align_type_members_on_columns", //$NON-NLS-1$
			DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS);
		
		checkAndReplace(oldSettings, 
			"org.eclipse.wst.jsdt.core.formatter.insert_space_after_comma__in_superinterfaces", //$NON-NLS-1$
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES);

		checkAndReplace(oldSettings,
			"org.eclipse.wst.jsdt.core.formatter.insert_space_before_comma__in_superinterfaces", //$NON-NLS-1$
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES);

		checkAndReplace(oldSettings, 
			"org.eclipse.wst.jsdt.core.formatter.insert_space_between_empty_arguments_in_method_invocation", //$NON-NLS-1$
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION);
	}
	
	private static void version4to5(Map oldSettings) {
		checkAndReplace(oldSettings,
			"org.eclipse.wst.jsdt.core.formatter.indent_block_statements", //$NON-NLS-1$
			new String[] { DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY, DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK });
	}
	
	private static void version5to6(Map oldSettings) {
		checkAndReplace(oldSettings,
			"org.eclipse.wst.jsdt.core.formatter.insert_new_line_in_control_statements", //$NON-NLS-1$
			new String[] {
					DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT
				});
	}
	
	private static void version6to7(Map oldSettings) {
		checkAndReplace(oldSettings, FORMATTER_COMMENT_FORMAT, FORMATTER_COMMENT_FORMAT2);
		checkAndReplace(oldSettings, FORMATTER_COMMENT_FORMATHEADER, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER);
		checkAndReplace(oldSettings, FORMATTER_COMMENT_FORMATSOURCE, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE);
		checkAndReplace(oldSettings, FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION); 
		checkAndReplace(oldSettings, FORMATTER_COMMENT_INDENTROOTTAGS, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS);
		checkAndReplace(oldSettings, FORMATTER_COMMENT_LINELENGTH, DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH); 
		checkAndReplace(oldSettings, FORMATTER_COMMENT_CLEARBLANKLINES, FORMATTER_COMMENT_CLEAR_BLANK_LINES);
		checkAndReplace(oldSettings, FORMATTER_COMMENT_FORMATHTML, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML);
		
		checkAndReplaceBooleanWithINSERT(oldSettings, FORMATTER_COMMENT_NEWLINEFORPARAMETER, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER); 
		checkAndReplaceBooleanWithINSERT(oldSettings, FORMATTER_COMMENT_SEPARATEROOTTAGS, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS); 
	}

	private static void version9to10(Map oldSettings) {
		duplicate(oldSettings, 
				DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION);
		duplicate(oldSettings, 
				DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER,
				DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ANNOTATION_DECLARATION_HEADER);
	}

	private static void version10to11(Map oldSettings) {
		checkAndReplace(oldSettings, 
				FORMATTER_COMMENT_FORMAT2,
				new String[] {
					DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT,
					DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT,
					DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT
				});
		checkAndReplace(oldSettings, FORMATTER_COMMENT_CLEAR_BLANK_LINES, 
				new String[] {
					DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT,
					DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT,
				});
	}
	
	/* old format constant values */

    private static final String FORMATTER_METHOD_DECLARATION_ARGUMENTS_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.method_declaration_arguments_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_MESSAGE_SEND_ARGUMENTS_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.message_send_arguments_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_MESSAGE_SEND_SELECTOR_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.message_send_selector_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_QUALIFIED_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.qualified_allocation_expression_arguments_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_TYPE_DECLARATION_SUPERCLASS_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.type_declaration_superclass_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_TYPE_DECLARATION_SUPERINTERFACES_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.type_declaration_superinterfaces_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_METHOD_THROWS_CLAUSE_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.method_throws_clause_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_CONDITIONAL_EXPRESSION_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.conditional_expression_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.allocation_expression_arguments_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_COMPACT_IF_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.compact_if_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_ARRAY_INITIALIZER_EXPRESSIONS_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.array_initializer_expressions_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_BINARY_EXPRESSION_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.binary_expression_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_EXPLICIT_CONSTRUCTOR_ARGUMENTS_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.explicit_constructor_arguments_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_ANONYMOUS_TYPE_DECLARATION_BRACE_POSITION = JavaScriptCore.PLUGIN_ID + ".formatter.anonymous_type_declaration_brace_position"; //$NON-NLS-1$
    private static final String FORMATTER_ARRAY_INITIALIZER_BRACE_POSITION = JavaScriptCore.PLUGIN_ID + ".formatter.array_initializer_brace_position"; //$NON-NLS-1$
    private static final String FORMATTER_BLOCK_BRACE_POSITION = JavaScriptCore.PLUGIN_ID + ".formatter.block_brace_position"; //$NON-NLS-1$
    private static final String FORMATTER_METHOD_DECLARATION_BRACE_POSITION = JavaScriptCore.PLUGIN_ID + ".formatter.method_declaration_brace_position"; //$NON-NLS-1$
    private static final String FORMATTER_TYPE_DECLARATION_BRACE_POSITION = JavaScriptCore.PLUGIN_ID + ".formatter.type_declaration_brace_position"; //$NON-NLS-1$
    private static final String FORMATTER_SWITCH_BRACE_POSITION = JavaScriptCore.PLUGIN_ID + ".formatter.switch_brace_position"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_constructor_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MESSAGESEND_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_messagesend_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_open_paren_in_parenthesized_expression"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_opening_paren_in_message_send"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_closing_paren_in_message_send"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_constructor_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MESSAGESEND_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_messagesend_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_message_send"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_closing_paren"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_FIRST_ARGUMENT = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_first_argument"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_between_empty_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_within_message_send"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_BLOCK_CLOSE_BRACE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_block_close_brace"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_ANONYMOUS_TYPE_OPEN_BRACE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_anonymous_type_open_brace"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_BLOCK_OPEN_BRACE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_block_open_brace"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_CATCH_EXPRESSION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_catch_expression"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_constructor_declaration_open_paren"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_FIRST_INITIALIZER = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_first_initializer"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_FOR_PAREN = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_for_paren"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_IF_CONDITION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_if_condition"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_method_declaration_open_paren"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_METHOD_OPEN_BRACE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_method_open_brace"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_open_paren_in_parenthesized_expression"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_SWITCH_CONDITION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_switch_condition"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_SWITCH_OPEN_BRACE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_switch_open_brace"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_SYNCHRONIZED_CONDITION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_synchronized_condition"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_TYPE_OPEN_BRACE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_type_open_brace"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_WHILE_CONDITION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_while_condition"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_REFERENCE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_between_brackets_in_array_reference";//$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARRAY_INITIALIZER = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_between_empty_array_initializer"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_MESSAGESEND_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_between_empty_messagesend_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_IN_CATCH_EXPRESSION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_in_catch_expression"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_IN_FOR_PARENS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_in_for_parens"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_IN_IF_CONDITION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_in_if_condition"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_IN_SWITCH_CONDITION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_in_switch_condition"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_IN_SYNCHRONIZED_CONDITION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_in_synchronized_condition"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_IN_WHILE_CONDITION = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_in_while_condition"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_REFERENCE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_bracket_in_array_reference";//$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_TYPE_REFERENCE = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_bracket_in_array_type_reference"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATORS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_assignment_operators"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATORS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_assignment_operators"; //$NON-NLS-1$
    private static final String FORMATTER_FORMAT_GUARDIAN_CLAUSE_ON_ONE_LINE = JavaScriptCore.PLUGIN_ID + ".formatter.format_guardian_clause_on_one_line"; //$NON-NLS-1$
    private static final String FORMATTER_ARRAY_INITIALIZER_CONTINUATION_INDENTATION = JavaScriptCore.PLUGIN_ID + ".formatter.array_initializer_continuation_indentation"; //$NON-NLS-1$
    private static final String FORMATTER_TYPE_MEMBER_ALIGNMENT = JavaScriptCore.PLUGIN_ID + ".formatter.type_member_alignment"; //$NON-NLS-1$
    private static final String FORMATTER_MULTICOLUMN = "256"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_THROWS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_method_throws"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_THROWS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_method_throws"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_method_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_method_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_explicitconstructorcall_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_explicitconstructorcall_arguments"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_THROWS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_constructor_throws"; //$NON-NLS-1$
    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_THROWS = JavaScriptCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_constructor_throws"; //$NON-NLS-1$
    private static final String FORMATTER_NO_ALIGNMENT = "0";//$NON-NLS-1$
	private static final String FORMATTER_COMMENT_FORMAT2= JavaScriptCore.PLUGIN_ID + ".formatter.comment.format_comments"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_CLEAR_BLANK_LINES= JavaScriptCore.PLUGIN_ID + ".formatter.comment.clear_blank_lines"; //$NON-NLS-1$ 

	// Old comment formatter constants
	private static final String FORMATTER_COMMENT_FORMATSOURCE= "comment_format_source_code"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION= "comment_indent_parameter_description"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_FORMATHEADER= "comment_format_header"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_INDENTROOTTAGS= "comment_indent_root_tags"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_FORMAT= "comment_format_comments"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_NEWLINEFORPARAMETER= "comment_new_line_for_parameter"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_SEPARATEROOTTAGS= "comment_separate_root_tags"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_CLEARBLANKLINES= "comment_clear_blank_lines"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_LINELENGTH= "comment_line_length"; //$NON-NLS-1$
	private static final String FORMATTER_COMMENT_FORMATHTML= "comment_format_html"; //$NON-NLS-1$
	
 }
