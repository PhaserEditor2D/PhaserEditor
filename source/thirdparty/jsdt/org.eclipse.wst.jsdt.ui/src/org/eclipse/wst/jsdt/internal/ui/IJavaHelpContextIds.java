/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> bug 38692
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.wst.jsdt.ui.JavaScriptUI;


/**
 * Help context ids for the Java UI.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 * 
 */
public interface IJavaHelpContextIds {
	public static final String PREFIX= JavaScriptUI.ID_PLUGIN + '.';

	// Actions
	public static final String GETTERSETTER_ACTION= 											PREFIX + "getter_setter_action_context"; //$NON-NLS-1$
	public static final String ADD_UNIMPLEMENTED_METHODS_ACTION= 					PREFIX + "add_unimplemented_methods_action_context"; //$NON-NLS-1$
	/*  */
	public static final String GENERATE_HASHCODE_EQUALS_ACTION=						PREFIX + "add_hashcode_equals_action_context"; //$NON-NLS-1$
	public static final String ADD_UNIMPLEMENTED_CONSTRUCTORS_ACTION= 			PREFIX + "add_unimplemented_constructors_action_context"; //$NON-NLS-1$	
	public static final String CREATE_NEW_CONSTRUCTOR_ACTION= 					PREFIX + "create_new_constructor_action_context"; //$NON-NLS-1$	
	public static final String SHOW_IN_PACKAGEVIEW_ACTION= 								PREFIX + "show_in_packageview_action_context"; //$NON-NLS-1$
	public static final String SHOW_IN_HIERARCHYVIEW_ACTION= 							PREFIX + "show_in_hierarchyview_action_context"; //$NON-NLS-1$
	public static final String FOCUS_ON_SELECTION_ACTION= 								PREFIX + "focus_on_selection_action"; //$NON-NLS-1$
	public static final String FOCUS_ON_TYPE_ACTION= 										PREFIX + "focus_on_type_action"; //$NON-NLS-1$

	public static final String TYPEHIERARCHY_HISTORY_ACTION= 							PREFIX + "typehierarchy_history_action"; //$NON-NLS-1$
	public static final String FILTER_PUBLIC_ACTION= 											PREFIX + "filter_public_action"; //$NON-NLS-1$
	public static final String FILTER_FIELDS_ACTION= 											PREFIX + "filter_fields_action"; //$NON-NLS-1$
	public static final String FILTER_STATIC_ACTION= 											PREFIX + "filter_static_action"; //$NON-NLS-1$
	public static final String FILTER_LOCALTYPES_ACTION=											PREFIX + "filter_localtypes_action"; //$NON-NLS-1$
	
	public static final String SHOW_INHERITED_ACTION= 										PREFIX + "show_inherited_action"; //$NON-NLS-1$
	public static final String SHOW_SUPERTYPES= 												PREFIX + "show_supertypes_action"; //$NON-NLS-1$
	public static final String SHOW_SUBTYPES= 													PREFIX + "show_subtypes_action"; //$NON-NLS-1$
	public static final String SHOW_HIERARCHY= 													PREFIX + "show_hierarchy_action"; //$NON-NLS-1$
	public static final String ENABLE_METHODFILTER_ACTION= 								PREFIX + "enable_methodfilter_action"; //$NON-NLS-1$
	public static final String ADD_IMPORT_ON_SELECTION_ACTION= 						PREFIX + "add_imports_on_selection_action_context"; //$NON-NLS-1$
	public static final String ORGANIZE_IMPORTS_ACTION= 									PREFIX + "organize_imports_action_context"; //$NON-NLS-1$
	public static final String ADD_TO_CLASSPATH_ACTION=														PREFIX + "addjtoclasspath_action_context"; //$NON-NLS-1$
	public static final String REMOVE_FROM_CLASSPATH_ACTION=														PREFIX + "removefromclasspath_action_context"; //$NON-NLS-1$

	public static final String TOGGLE_PRESENTATION_ACTION= 								PREFIX + "toggle_presentation_action_context"; //$NON-NLS-1$
	public static final String TOGGLE_MARK_OCCURRENCES_ACTION= 								PREFIX + "toggle_mark_occurrences_action_context"; //$NON-NLS-1$
	public static final String TOGGLE_TEXTHOVER_ACTION= 									PREFIX + "toggle_texthover_action_context"; //$NON-NLS-1$

	public static final String OPEN_CLASS_WIZARD_ACTION= 									PREFIX + "open_class_wizard_action"; //$NON-NLS-1$
	public static final String OPEN_INTERFACE_WIZARD_ACTION= 							PREFIX + "open_interface_wizard_action"; //$NON-NLS-1$
	/**  */
	public static final String OPEN_ENUM_WIZARD_ACTION= 							PREFIX + "open_enum_wizard_action"; //$NON-NLS-1$
	/**  */
	public static final String OPEN_ANNOTATION_WIZARD_ACTION= 							PREFIX + "open_annotation_wizard_action"; //$NON-NLS-1$

	public static final String SORT_MEMBERS_ACTION=											PREFIX + "sort_members_action"; //$NON-NLS-1$	

	public static final String OPEN_PACKAGE_WIZARD_ACTION= 								PREFIX + "open_package_wizard_action"; //$NON-NLS-1$
	public static final String OPEN_PROJECT_WIZARD_ACTION= 								PREFIX + "open_project_wizard_action"; //$NON-NLS-1$
	/**  */
	public static final String OPEN_SOURCEFOLDER_WIZARD_ACTION= 							PREFIX + "open_sourcefolder_wizard_action"; //$NON-NLS-1$

	public static final String EDIT_WORKING_SET_ACTION= 									PREFIX + "edit_working_set_action"; //$NON-NLS-1$
	public static final String CLEAR_WORKING_SET_ACTION= 									PREFIX + "clear_working_set_action"; //$NON-NLS-1$
	public static final String GOTO_MARKER_ACTION= 											PREFIX + "goto_marker_action"; //$NON-NLS-1$
	public static final String GOTO_PACKAGE_ACTION= 											PREFIX + "goto_package_action"; //$NON-NLS-1$
	public static final String GOTO_TYPE_ACTION= 											PREFIX + "goto_type_action"; //$NON-NLS-1$
	public static final String GOTO_MATCHING_BRACKET_ACTION=							PREFIX + "goto_matching_bracket_action"; 	 //$NON-NLS-1$Object[] FORMAT_ALL= null;

	/**
	 * 
	 */
	public static final String FORMAT_ALL=														PREFIX + "format_all_action"; 	 //$NON-NLS-1$

	public static final String GOTO_NEXT_MEMBER_ACTION=							PREFIX + "goto_next_member_action"; 	 //$NON-NLS-1$
	public static final String GOTO_PREVIOUS_MEMBER_ACTION=							PREFIX + "goto_previous_member_action"; 	 //$NON-NLS-1$
	public static final String HISTORY_ACTION= 													PREFIX + "history_action"; //$NON-NLS-1$
	public static final String HISTORY_LIST_ACTION= 											PREFIX + "history_list_action"; //$NON-NLS-1$
	public static final String LEXICAL_SORTING_OUTLINE_ACTION= 							PREFIX + "lexical_sorting_outline_action"; //$NON-NLS-1$
	public static final String LEXICAL_SORTING_BROWSING_ACTION= 						PREFIX + "lexical_sorting_browsing_action"; //$NON-NLS-1$
	public static final String OPEN_JAVA_PERSPECTIVE_ACTION= 							PREFIX + "open_java_perspective_action"; //$NON-NLS-1$
	public static final String ADD_DELEGATE_METHODS_ACTION= 										PREFIX + "add_delegate_methods_action"; //$NON-NLS-1$

	public static final String OPEN_JAVA_BROWSING_PERSPECTIVE_ACTION= 			PREFIX + "open_java_browsing_perspective_action"; //$NON-NLS-1$
	public static final String OPEN_PROJECT_ACTION= 											PREFIX + "open_project_action"; //$NON-NLS-1$

	public static final String OPEN_TYPE_ACTION= 												PREFIX + "open_type_action"; //$NON-NLS-1$
	public static final String OPEN_TYPE_IN_HIERARCHY_ACTION= 							PREFIX + "open_type_in_hierarchy_action"; //$NON-NLS-1$
	
	
	/**
	 * 
	 */
	public static final String CONFIG_CONTAINER_ACTION= 									PREFIX + "org.eclipse.wst.jsdt.ui.config_container_action"; //$NON-NLS-1$
	
	public static final String ADD_JAVADOC_STUB_ACTION= 									PREFIX + "add_javadoc_stub_action"; //$NON-NLS-1$
	public static final String ADD_TASK_ACTION= 												PREFIX + "add_task_action"; //$NON-NLS-1$
	public static final String EXTERNALIZE_STRINGS_ACTION= 								PREFIX + "externalize_strings_action"; //$NON-NLS-1$	
	public static final String EXTRACT_METHOD_ACTION= 										PREFIX + "extract_method_action"; //$NON-NLS-1$	
	public static final String EXTRACT_TEMP_ACTION= 											PREFIX + "extract_temp_action"; //$NON-NLS-1$	
	public static final String PROMOTE_TEMP_TO_FIELD_ACTION= 								PREFIX + "promote_temp_to_field_action"; //$NON-NLS-1$	
	public static final String CONVERT_ANONYMOUS_TO_NESTED_ACTION= 								PREFIX + "convert_anonymous_to_nested_action"; //$NON-NLS-1$	
	public static final String EXTRACT_CONSTANT_ACTION= 											PREFIX + "extract_constant_action"; //$NON-NLS-1$	
	public static final String INTRODUCE_PARAMETER_ACTION=								PREFIX + "introduce_parameter_action"; //$NON-NLS-1$	
	public static final String INTRODUCE_FACTORY_ACTION= 								PREFIX + "introduce_factory_action"; //$NON-NLS-1$
	/**
	 * 
	 */
	public static final String INTRODUCE_INDIRECTION_ACTION= 								PREFIX + "introduce_indirection_action"; //$NON-NLS-1$
	
	public static final String EXTRACT_INTERFACE_ACTION= 									PREFIX + "extract_interface_action"; //$NON-NLS-1$	
	public static final String CHANGE_TYPE_ACTION= 											PREFIX + "change_type_action"; //$NON-NLS-1$
	public static final String MOVE_INNER_TO_TOP_ACTION= 									PREFIX + "move_inner_to_top_level_action"; //$NON-NLS-1$
	public static final String USE_SUPERTYPE_ACTION= 										PREFIX + "use_supertype_action"; //$NON-NLS-1$
	public static final String FIND_DECLARATIONS_IN_WORKSPACE_ACTION= 						PREFIX + "find_declarations_in_workspace_action"; //$NON-NLS-1$	
	public static final String FIND_DECLARATIONS_IN_PROJECT_ACTION= 						PREFIX + "find_declarations_in_project_action"; //$NON-NLS-1$	
	public static final String FIND_DECLARATIONS_IN_HIERARCHY_ACTION= 						PREFIX + "find_declarations_in_hierarchy_action"; //$NON-NLS-1$	
	public static final String FIND_DECLARATIONS_IN_WORKING_SET_ACTION= 						PREFIX + "find_declarations_in_working_set_action"; //$NON-NLS-1$	
	public static final String FIND_IMPLEMENTORS_IN_WORKSPACE_ACTION= 						PREFIX + "find_implementors_in_workspace_action"; //$NON-NLS-1$			
	public static final String FIND_IMPLEMENTORS_IN_PROJECT_ACTION= 						PREFIX + "find_implementors_in_project_action"; //$NON-NLS-1$			
	public static final String FIND_IMPLEMENTORS_IN_WORKING_SET_ACTION= 						PREFIX + "find_implementors_in_working_set_action"; //$NON-NLS-1$			
	public static final String FIND_REFERENCES_IN_WORKSPACE_ACTION= 							PREFIX + "find_references_in_workspace_action"; //$NON-NLS-1$			
	public static final String FIND_REFERENCES_IN_PROJECT_ACTION= 							PREFIX + "find_references_in_project_action"; //$NON-NLS-1$			
	public static final String FIND_REFERENCES_IN_HIERARCHY_ACTION= 							PREFIX + "find_references_in_hierarchy_action"; //$NON-NLS-1$			
	public static final String FIND_REFERENCES_IN_WORKING_SET_ACTION= 						PREFIX + "find_references_in_working_set_action"; //$NON-NLS-1$			
	public static final String FIND_READ_REFERENCES_IN_WORKSPACE_ACTION= 					PREFIX + "find_read_references_in_workspace_action"; //$NON-NLS-1$			
	public static final String FIND_READ_REFERENCES_IN_PROJECT_ACTION= 					PREFIX + "find_read_references_in_project_action"; //$NON-NLS-1$			
	public static final String FIND_READ_REFERENCES_IN_HIERARCHY_ACTION= 					PREFIX + "find_read_references_in_hierarchy_action"; //$NON-NLS-1$
	public static final String FIND_READ_REFERENCES_IN_WORKING_SET_ACTION= 					PREFIX + "find_read_references_in_working_set_action"; //$NON-NLS-1$
	public static final String FIND_WRITE_REFERENCES_IN_HIERARCHY_ACTION= 					PREFIX + "find_write_references_in_hierarchy_action"; //$NON-NLS-1$
	public static final String FIND_WRITE_REFERENCES_IN_PROJECT_ACTION= 					PREFIX + "find_write_references_in_project_action"; //$NON-NLS-1$
	public static final String FIND_WRITE_REFERENCES_IN_WORKING_SET_ACTION=					PREFIX + "find_write_references_in_working_set_action"; //$NON-NLS-1$
	public static final String FIND_WRITE_REFERENCES_IN_WORKSPACE_ACTION= 					PREFIX + "find_write_references_in_workspace_action"; //$NON-NLS-1$
	public static final String FIND_OCCURRENCES_IN_FILE_ACTION= 								PREFIX + "find_occurrences_in_file_action"; //$NON-NLS-1$
	public static final String FIND_EXCEPTION_OCCURRENCES= 								PREFIX + "find_exception_occurrences"; //$NON-NLS-1$
	public static final String FIND_IMPLEMENT_OCCURRENCES= 								PREFIX + "find_implement_occurrences"; //$NON-NLS-1$
	public static final String WORKING_SET_FIND_ACTION=										PREFIX + "working_set_find_action"; //$NON-NLS-1$
	public static final String FIND_STRINGS_TO_EXTERNALIZE_ACTION= 					PREFIX + "find_strings_to_externalize_action"; //$NON-NLS-1$
	public static final String INLINE_ACTION= 												PREFIX + "inline_action"; //$NON-NLS-1$
	/**
	 * 
	 */
	public static final String REPLACE_INVOCATIONS_ACTION= 							PREFIX + "replace_invocations_action"; //$NON-NLS-1$
	public static final String MODIFY_PARAMETERS_ACTION= 									PREFIX + "modify_parameters_action"; //$NON-NLS-1$
	public static final String MOVE_ACTION= 														PREFIX + "move_action"; //$NON-NLS-1$
	public static final String OPEN_ACTION= 														PREFIX + "open_action"; //$NON-NLS-1$
	public static final String OPEN_EXTERNAL_JAVADOC_ACTION= 							PREFIX + "open_external_javadoc_action"; //$NON-NLS-1$
	public static final String OPEN_INPUT_ACTION= 														PREFIX + "open_input_action"; //$NON-NLS-1$
	public static final String OPEN_SUPER_IMPLEMENTATION_ACTION= 					PREFIX + "open_super_implementation_action"; //$NON-NLS-1$
	public static final String PULL_UP_ACTION= 													PREFIX + "pull_up_action"; //$NON-NLS-1$
	/**
	 * 
	 */
	public static final String EXTRACT_SUPERTYPE_ACTION= 													PREFIX + "extract_supertype_action"; //$NON-NLS-1$
	public static final String PUSH_DOWN_ACTION= 													PREFIX + "push_down_action"; //$NON-NLS-1$
	public static final String REFRESH_ACTION= 													PREFIX + "refresh_action"; //$NON-NLS-1$
	public static final String RENAME_ACTION= 													PREFIX + "rename_action"; //$NON-NLS-1$
	public static final String SELF_ENCAPSULATE_ACTION=									PREFIX + "self_encapsulate_action"; //$NON-NLS-1$
	public static final String SHOW_IN_NAVIGATOR_VIEW_ACTION= 						PREFIX + "show_in_navigator_action"; //$NON-NLS-1$
	public static final String SURROUND_WITH_TRY_CATCH_ACTION= 						PREFIX + "surround_with_try_catch_action"; //$NON-NLS-1$	
	public static final String OPEN_RESOURCE_ACTION= 										PREFIX + "open_resource_action"; //$NON-NLS-1$	
	public static final String SELECT_WORKING_SET_ACTION= 								PREFIX + "select_working_set_action"; //$NON-NLS-1$	
	public static final String STRUCTURED_SELECTION_HISTORY_ACTION= 				PREFIX + "structured_selection_history_action"; //$NON-NLS-1$	
	public static final String STRUCTURED_SELECT_ENCLOSING_ACTION= 					PREFIX + "structured_select_enclosing_action"; //$NON-NLS-1$	
	public static final String STRUCTURED_SELECT_NEXT_ACTION= 							PREFIX + "structured_select_next_action"; //$NON-NLS-1$	
	public static final String STRUCTURED_SELECT_PREVIOUS_ACTION= 					PREFIX + "structured_select_previous_action"; //$NON-NLS-1$	
	public static final String TOGGLE_ORIENTATION_ACTION= 								PREFIX + "toggle_orientations_action"; //$NON-NLS-1$		
	public static final String CUT_ACTION= 															PREFIX + "cut_action"; //$NON-NLS-1$	
	public static final String COPY_ACTION= 														PREFIX + "copy_action"; //$NON-NLS-1$	
	public static final String PASTE_ACTION= 														PREFIX + "paste_action"; //$NON-NLS-1$	
	public static final String DELETE_ACTION= 													PREFIX + "delete_action"; //$NON-NLS-1$	
	public static final String SELECT_ALL_ACTION= 												PREFIX + "select_all_action"; //$NON-NLS-1$
	public static final String OPEN_TYPE_HIERARCHY_ACTION= 								PREFIX + "open_type_hierarchy_action"; //$NON-NLS-1$	
	public static final String COLLAPSE_ALL_ACTION= 								PREFIX + "open_type_hierarchy_action"; //$NON-NLS-1$
	public static final String GOTO_RESOURCE_ACTION=							PREFIX + "goto_resource_action"; 	 //$NON-NLS-1$
	public static final String LINK_EDITOR_ACTION=							PREFIX + "link_editor_action"; 	 //$NON-NLS-1$
	public static final String GO_INTO_TOP_LEVEL_TYPE_ACTION=							PREFIX + "go_into_top_level_type_action"; 	 //$NON-NLS-1$
	public static final String COMPARE_WITH_HISTORY_ACTION=							PREFIX + "compare_with_history_action"; 	 //$NON-NLS-1$
	public static final String REPLACE_WITH_PREVIOUS_FROM_HISTORY_ACTION=							PREFIX + "replace_with_previous_from_history_action"; 	 //$NON-NLS-1$
	public static final String REPLACE_WITH_HISTORY_ACTION=							PREFIX + "replace_with_history_action"; 	 //$NON-NLS-1$
	public static final String ADD_FROM_HISTORY_ACTION=							PREFIX + "add_from_history_action"; 	 //$NON-NLS-1$
	public static final String LAYOUT_FLAT_ACTION=							PREFIX + "layout_flat_action"; 	 //$NON-NLS-1$
	public static final String LAYOUT_HIERARCHICAL_ACTION=							PREFIX + "layout_hierarchical_action"; 	 //$NON-NLS-1$	
	// *** Don't delete this constants and the doc since it is still used in refactoring
	public static final String NEXT_CHANGE_ACTION=							PREFIX + "next_change_action"; 	 //$NON-NLS-1$	
	public static final String PREVIOUS_CHANGE_ACTION=							PREFIX + "previous_change_action"; 	 //$NON-NLS-1$
	public static final String NEXT_PROBLEM_ACTION=							PREFIX + "next_problem_action"; 	 //$NON-NLS-1$	
	public static final String PREVIOUS_PROBLEM_ACTION=							PREFIX + "previous_problem_action"; 	 //$NON-NLS-1$
	// *** end
	public static final String JAVA_SELECT_MARKER_RULER_ACTION=							PREFIX + "java_select_marker_ruler_action"; 	 //$NON-NLS-1$	
	public static final String SHOW_QUALIFIED_NAMES_ACTION=							PREFIX + "show_qualified_names_action"; 	 //$NON-NLS-1$	
	public static final String SORT_BY_DEFINING_TYPE_ACTION=							PREFIX + "sort_by_defining_type_action"; 	 //$NON-NLS-1$	
	public static final String FORMAT_ACTION=							PREFIX + "format_action"; 	 //$NON-NLS-1$	
	public static final String COMMENT_ACTION=							PREFIX + "comment_action"; 	 //$NON-NLS-1$	
	public static final String UNCOMMENT_ACTION=							PREFIX + "uncomment_action"; 	 //$NON-NLS-1$	
	/**
	 * 
	 */
	public static final String TOGGLE_COMMENT_ACTION=							PREFIX + "toggle_comment_action"; 	 //$NON-NLS-1$
	public static final String ADD_BLOCK_COMMENT_ACTION=				PREFIX + "add_block_comment_action"; 	//$NON-NLS-1$
	public static final String REMOVE_BLOCK_COMMENT_ACTION=				PREFIX + "remove_block_comment_action";	//$NON-NLS-1$
	public static final String QUICK_FIX_ACTION= 						PREFIX + "quick_fix_action"; 	 //$NON-NLS-1$	
	public static final String CONTENT_ASSIST_ACTION= 						PREFIX + "content_assist_action"; 	 //$NON-NLS-1$	
	public static final String PARAMETER_HINTS_ACTION= 						PREFIX + "parameter_hints_action"; 	 //$NON-NLS-1$	
	public static final String SHOW_OUTLINE_ACTION= 						PREFIX + "show_outline_action"; 	 //$NON-NLS-1$	
	public static final String OPEN_STRUCTURE_ACTION= 						PREFIX + "open_structure_action"; 	 //$NON-NLS-1$	
	public static final String OPEN_HIERARCHY_ACTION= 						PREFIX + "open_hierarchy_action"; 	 //$NON-NLS-1$	
	public static final String TOGGLE_SMART_TYPING_ACTION= 					PREFIX + "toggle_smart_typing_action"; //$NON-NLS-1$
	public static final String INDENT_ACTION= 								PREFIX + "indent_action"; //$NON-NLS-1$

	// Dialogs
	public static final String MAINTYPE_SELECTION_DIALOG= PREFIX + "maintype_selection_dialog_context"; //$NON-NLS-1$
	public static final String OPEN_TYPE_DIALOG= PREFIX + "open_type_dialog_context"; //$NON-NLS-1$
	public static final String TYPE_SELECTION_DIALOG2= PREFIX + "type_selection_dialog2_context"; //$NON-NLS-1$
	public static final String OPEN_PACKAGE_DIALOG= PREFIX + "open_package_dialog_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_DIALOG= PREFIX + "source_attachment_dialog_context"; //$NON-NLS-1$
	public static final String LIBRARIES_WORKBOOK_PAGE_ADVANCED_DIALOG= PREFIX + "advanced_dialog_context"; //$NON-NLS-1$
	public static final String CONFIRM_SAVE_MODIFIED_RESOURCES_DIALOG= PREFIX + "confirm_save_modified_resources_dialog_context"; //$NON-NLS-1$
	public static final String NEW_VARIABLE_ENTRY_DIALOG= PREFIX + "new_variable_dialog_context"; //$NON-NLS-1$
	public static final String NONNLS_DIALOG= PREFIX + "nonnls_dialog_context"; //$NON-NLS-1$
	public static final String MULTI_MAIN_TYPE_SELECTION_DIALOG= PREFIX + "multi_main_type_selection_dialog_context"; //$NON-NLS-1$
	public static final String MULTI_TYPE_SELECTION_DIALOG= PREFIX + "multi_type_selection_dialog_context"; //$NON-NLS-1$
	public static final String SUPER_INTERFACE_SELECTION_DIALOG= PREFIX + "super_interface_selection_dialog_context"; //$NON-NLS-1$
	
	public static final String OVERRIDE_TREE_SELECTION_DIALOG= PREFIX + "override_tree_selection_dialog_context"; //$NON-NLS-1$
	public static final String ADD_GETTER_SETTER_SELECTION_DIALOG= PREFIX + "add_getter_setter_selection_dialog_context"; //$NON-NLS-1$
	public static final String ADD_DELEGATE_METHODS_SELECTION_DIALOG= PREFIX + "add_delegate_methods_selection_dialog_context"; //$NON-NLS-1$
	public static final String GENERATE_HASHCODE_EQUALS_SELECTION_DIALOG= PREFIX + "hash_code_equals_tree_selection_dialog_context"; //$NON-NLS-1$
	public static final String GENERATE_CONSTRUCTOR_USING_FIELDS_SELECTION_DIALOG= PREFIX + "generate_constructor_using_fields_selection_dialog_context"; //$NON-NLS-1$
	public static final String ADD_UNIMPLEMENTED_CONSTRUCTORS_DIALOG= PREFIX + "add_unimplemented_constructors_dialog_context"; //$NON-NLS-1$
	
	public static final String MOVE_DESTINATION_DIALOG= PREFIX + "move_destination_dialog_context"; //$NON-NLS-1$
	public static final String CHOOSE_VARIABLE_DIALOG= PREFIX + "choose_variable_dialog_context"; //$NON-NLS-1$	
	public static final String EDIT_TEMPLATE_DIALOG= PREFIX + "edit_template_dialog_context"; //$NON-NLS-1$	
	public static final String HISTORY_LIST_DIALOG= PREFIX + "history_list_dialog_context"; //$NON-NLS-1$	
	public static final String IMPORT_ORGANIZE_INPUT_DIALOG= PREFIX + "import_organize_input_dialog_context"; //$NON-NLS-1$
	public static final String TODO_TASK_INPUT_DIALOG= PREFIX + "todo_task_input_dialog_context"; //$NON-NLS-1$
	public static final String JAVADOC_PROPERTY_DIALOG= PREFIX + "javadoc_property_dialog_context"; //$NON-NLS-1$	
	public static final String NEW_CONTAINER_DIALOG= PREFIX + "new_container_dialog_context"; //$NON-NLS-1$	
	public static final String EXCLUSION_PATTERN_DIALOG= PREFIX + "exclusion_pattern_dialog_context"; //$NON-NLS-1$
	public static final String ACCESS_RULES_DIALOG= PREFIX + "access_rules_dialog_context"; //$NON-NLS-1$
	public static final String OUTPUT_LOCATION_DIALOG= PREFIX + "output_location_dialog_context"; //$NON-NLS-1$
	public static final String VARIABLE_CREATION_DIALOG= PREFIX + "variable_creation_dialog_context"; //$NON-NLS-1$	
	public static final String JAVA_SEARCH_PAGE= PREFIX + "java_search_page_context"; //$NON-NLS-1$
	public static final String NLS_SEARCH_PAGE= PREFIX + "nls_search_page_context"; //$NON-NLS-1$
	public static final String JAVA_EDITOR= PREFIX + "java_editor_context"; //$NON-NLS-1$
	public static final String GOTO_RESOURCE_DIALOG= PREFIX + "goto_resource_dialog";  //$NON-NLS-1$
	
	public static final String COMPARE_DIALOG= PREFIX + "compare_dialog_context"; //$NON-NLS-1$
	public static final String ADD_ELEMENT_FROM_HISTORY_DIALOG= PREFIX + "add_element_from_history_dialog_context"; //$NON-NLS-1$
	public static final String COMPARE_ELEMENT_WITH_HISTORY_DIALOG= PREFIX + "compare_element_with_history_dialog_context"; //$NON-NLS-1$
	public static final String REPLACE_ELEMENT_WITH_HISTORY_DIALOG= PREFIX + "replace_element_with_history_dialog_context"; //$NON-NLS-1$
	
	public static final String SORT_MEMBERS_DIALOG= PREFIX + "sort_members_dialog_context"; //$NON-NLS-1$
	
	// view parts
	public static final String TYPE_HIERARCHY_VIEW= PREFIX + "type_hierarchy_view_context"; //$NON-NLS-1$
	public static final String PACKAGES_VIEW= PREFIX + "package_view_context"; //$NON-NLS-1$
	public static final String PROJECTS_VIEW= PREFIX + "projects_view_context"; //$NON-NLS-1$
	public static final String PACKAGES_BROWSING_VIEW= PREFIX + "packages_browsing_view_context"; //$NON-NLS-1$
	public static final String TYPES_VIEW= PREFIX + "types_view_context"; //$NON-NLS-1$
	public static final String MEMBERS_VIEW= PREFIX + "members_view_context"; //$NON-NLS-1$
	public static final String JAVADOC_VIEW= PREFIX + "javadoc_view_context"; //$NON-NLS-1$
	public static final String SOURCE_VIEW= PREFIX + "source_view_context"; //$NON-NLS-1$

	// Preference/Property pages
	public static final String APPEARANCE_PREFERENCE_PAGE= 			PREFIX + "appearance_preference_page_context"; //$NON-NLS-1$
	public static final String SORT_ORDER_PREFERENCE_PAGE=		    PREFIX + "sort_order_preference_page_context"; //$NON-NLS-1$
	public static final String TYPE_FILTER_PREFERENCE_PAGE=		    PREFIX + "type_filter_preference_page_context"; //$NON-NLS-1$
	public static final String BUILD_PATH_PROPERTY_PAGE= 				PREFIX + "build_path_property_page_context"; //$NON-NLS-1$
	public static final String CP_VARIABLES_PREFERENCE_PAGE= 		PREFIX + "cp_variables_preference_page_context"; //$NON-NLS-1$
	/**
	 * 
	 */
	public static final String CP_USERLIBRARIES_PREFERENCE_PAGE= 		PREFIX + "cp_userlibraries_preference_page_context"; //$NON-NLS-1$
	
	public static final String CODEFORMATTER_PREFERENCE_PAGE= 	PREFIX + "codeformatter_preference_page_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_PROPERTY_PAGE=	PREFIX + "source_attachment_property_page_context"; //$NON-NLS-1$
	public static final String COMPILER_PROPERTY_PAGE= PREFIX + "compiler_property_page_context"; //$NON-NLS-1$
	public static final String JAVA_BUILD_PROPERTY_PAGE= PREFIX + "java_build_property_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_PROBLEMS_PROPERTY_PAGE= PREFIX + "javadoc_problems_property_page_context"; //$NON-NLS-1$
	public static final String PROBLEM_SEVERITIES_PROPERTY_PAGE= PREFIX + "problem_severities_property_page_context"; //$NON-NLS-1$
	public static final String TODOTASK_PROPERTY_PAGE= PREFIX + "tasktags_property_page_context"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String CODE_TEMPLATES_PREFERENCE_PAGE= PREFIX + "code_templates_preference_context"; //$NON-NLS-1$

	public static final String CODE_MANIPULATION_PREFERENCE_PAGE= PREFIX + "code_manipulation_preference_context"; //$NON-NLS-1$
	public static final String ORGANIZE_IMPORTS_PREFERENCE_PAGE= PREFIX + "organizeimports_preference_page_context"; //$NON-NLS-1$
	public static final String JAVA_BASE_PREFERENCE_PAGE= PREFIX + "java_base_preference_page_context"; //$NON-NLS-1$
	public static final String REFACTORING_PREFERENCE_PAGE= PREFIX + "refactoring_preference_page_context"; //$NON-NLS-1$
	public static final String JAVA_EDITOR_PREFERENCE_PAGE= PREFIX + "java_editor_preference_page_context"; //$NON-NLS-1$
	public static final String SPELLING_CONFIGURATION_BLOCK= PREFIX + "spelling_configuration_block_context"; //$NON-NLS-1$
	public static final String PROPERTIES_FILE_EDITOR_PREFERENCE_PAGE= PREFIX + "properties_file_editor_preference_page_context"; //$NON-NLS-1$
	public static final String COMPILER_PREFERENCE_PAGE= PREFIX + "compiler_preference_page_context"; //$NON-NLS-1$
	public static final String JAVA_BUILD_PREFERENCE_PAGE= PREFIX + "java_build_preference_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_PROBLEMS_PREFERENCE_PAGE= PREFIX + "javadoc_problems_preference_page_context"; //$NON-NLS-1$
	public static final String PROBLEM_SEVERITIES_PREFERENCE_PAGE= PREFIX + "problem_severities_preference_page_context"; //$NON-NLS-1$
	public static final String TODOTASK_PREFERENCE_PAGE= PREFIX + "tasktags_preference_page_context"; //$NON-NLS-1$
	
	public static final String TEMPLATE_PREFERENCE_PAGE= PREFIX + "template_preference_page_context"; //$NON-NLS-1$
	public static final String NEW_JAVA_PROJECT_PREFERENCE_PAGE= PREFIX + "new_java_project_preference_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_CONFIGURATION_PROPERTY_PAGE= PREFIX + "javadoc_configuration_property_page_context"; //$NON-NLS-1$
	public static final String JAVA_ELEMENT_INFO_PAGE= PREFIX + "java_element_info_page_context"; //$NON-NLS-1$
		
	// Wizard pages
	public static final String NEW_JAVAPROJECT_WIZARD_PAGE= PREFIX + "new_javaproject_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_PACKAGE_WIZARD_PAGE= PREFIX + "new_package_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_CLASS_WIZARD_PAGE= PREFIX + "new_class_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_INTERFACE_WIZARD_PAGE= PREFIX + "new_interface_wizard_page_context"; //$NON-NLS-1$
	
	// since 3.1
	public static final String NEW_ENUM_WIZARD_PAGE= PREFIX + "new_enum_wizard_page_context"; //$NON-NLS-1$
	// since 3.1
	public static final String NEW_ANNOTATION_WIZARD_PAGE= PREFIX + "new_annotation_wizard_page_context"; //$NON-NLS-1$

	public static final String NEW_PACKAGEROOT_WIZARD_PAGE= PREFIX + "new_packageroot_wizard_page_context"; //$NON-NLS-1$
	public static final String JARPACKAGER_WIZARD_PAGE= PREFIX + "jar_packager_wizard_page_context"; //$NON-NLS-1$
	// since 3.2
	public static final String JARPACKAGER_REFACTORING_DIALOG= PREFIX + "jar_packager_refactoring_dialog_context"; //$NON-NLS-1$
	// since 3.2
	public static final String JARIMPORT_WIZARD_PAGE= PREFIX + "jar_import_wizard_page_context"; //$NON-NLS-1$
	// since 3.2
	public static final String INCLUSION_EXCLUSION_WIZARD_PAGE= PREFIX + "edit_inclusion_exlusion_filter_wizard_page_context"; //$NON-NLS-1$
	public static final String JARMANIFEST_WIZARD_PAGE= PREFIX + "jar_manifest_wizard_page_context"; //$NON-NLS-1$
	public static final String JAROPTIONS_WIZARD_PAGE= PREFIX + "jar_options_wizard_page_context"; //$NON-NLS-1$
	public static final String JAVA_WORKING_SET_PAGE= PREFIX + "java_working_set_page_context"; //$NON-NLS-1$

	public static final String CLASSPATH_CONTAINER_DEFAULT_PAGE= PREFIX + "classpath_container_default_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_STANDARD_PAGE= PREFIX + "javadoc_standard_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_SPECIFICS_PAGE= PREFIX + "javadoc_specifics_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_TREE_PAGE= PREFIX + "javadoc_tree_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_COMMAND_PAGE= PREFIX + "javadoc_command_page_context"; //$NON-NLS-1$

	// Same help for all refactoring error pages. Indidivual help can
	// be provided per a single refactoring status.	
	// *** Don't delete this constants and the doc since it is still used in refactoring
	public static final String REFACTORING_ERROR_WIZARD_PAGE=	PREFIX + "refactoring_error_wizard_page_context";  //$NON-NLS-1$
	// same help for all refactoring preview pages
	// *** Don't delete this constants and the doc since it is still used in refactoring
	public static final String REFACTORING_PREVIEW_WIZARD_PAGE= PREFIX + "refactoring_preview_wizard_page_context"; //$NON-NLS-1$
	
	public static final String RENAME_PARAMS_WIZARD_PAGE= 						PREFIX + "rename_params_wizard_page"; //$NON-NLS-1$
	public static final String EXTERNALIZE_WIZARD_KEYVALUE_PAGE= 				PREFIX + "externalize_wizard_keyvalue_page_context"; //$NON-NLS-1$
	public static final String EXTERNALIZE_WIZARD_PROPERTIES_FILE_PAGE= 	PREFIX + "externalize_wizard_properties_file_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_INTERFACE_WIZARD_PAGE= 					PREFIX + "extract_interface_temp_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_METHOD_WIZARD_PAGE= 					PREFIX + "extract_method_wizard_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_TEMP_WIZARD_PAGE= 						PREFIX + "extract_temp_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_CONSTANT_WIZARD_PAGE= 						PREFIX + "extract_constant_page_context"; //$NON-NLS-1$
	public static final String INTRODUCE_PARAMETER_WIZARD_PAGE= 						PREFIX + "introduce_parameter_page_context"; //$NON-NLS-1$
	public static final String INTRODUCE_FACTORY_WIZARD_PAGE= 							PREFIX + "introduce_factory_wizard_page_context"; //$NON-NLS-1$
	/**
	 * 
	 */
	public static final String INTRODUCE_INDIRECTION_WIZARD_PAGE= 							PREFIX + "introduce_indirection_wizard_page_context"; //$NON-NLS-1$
	
	public static final String PROMOTE_TEMP_TO_FIELD_WIZARD_PAGE= 					PREFIX + "promote_temp_to_field_page_context"; //$NON-NLS-1$
	public static final String CONVERT_ANONYMOUS_TO_NESTED_WIZARD_PAGE= 				PREFIX + "convert_anonymous_to_nested_page_context"; //$NON-NLS-1$
	public static final String MODIFY_PARAMETERS_WIZARD_PAGE= 				PREFIX + "modify_parameters_wizard_page_context"; //$NON-NLS-1$
	public static final String MOVE_MEMBERS_WIZARD_PAGE= 						PREFIX + "move_members_wizard_page_context"; //$NON-NLS-1$
	public static final String MOVE_INNER_TO_TOP_WIZARD_PAGE= 				PREFIX + "move_inner_to_top_wizard_page_context"; //$NON-NLS-1$
	public static final String PULL_UP_WIZARD_PAGE= 									PREFIX + "pull_up_wizard_page_context"; //$NON-NLS-1$
	/**
	 * 
	 */
	public static final String EXTRACT_SUPERTYPE_WIZARD_PAGE= 									PREFIX + "extract_supertype_wizard_page_context"; //$NON-NLS-1$
	public static final String PUSH_DOWN_WIZARD_PAGE= 									PREFIX + "push_down_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_PACKAGE_WIZARD_PAGE= 						PREFIX + "rename_package_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_TYPE_PARAMETER_WIZARD_PAGE= 						PREFIX + "rename_type_parameter_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_LOCAL_VARIABLE_WIZARD_PAGE=  							PREFIX + "rename_local_variable_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_CU_WIZARD_PAGE= 								PREFIX + "rename_cu_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_METHOD_WIZARD_PAGE= 						PREFIX + "rename_method_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_TYPE_WIZARD_PAGE= 							PREFIX + "rename_type_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_FIELD_WIZARD_PAGE= 							PREFIX + "rename_field_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_RESOURCE_WIZARD_PAGE= 							PREFIX + "rename_resource_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_JAVA_PROJECT_WIZARD_PAGE= 							PREFIX + "rename_java_project_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_SOURCE_FOLDER_WIZARD_PAGE= 							PREFIX + "rename_source_folder_wizard_page_context"; //$NON-NLS-1$
	public static final String SEF_WIZARD_PAGE= 										PREFIX + "self_encapsulate_field_wizard_page_context"; //$NON-NLS-1$
	public static final String USE_SUPERTYPE_WIZARD_PAGE= 						PREFIX + "use_supertype_wizard_page_context"; //$NON-NLS-1$
	public static final String INLINE_METHOD_WIZARD_PAGE=				PREFIX + "inline_method_wizard_page_context"; //$NON-NLS-1$
	public static final String INLINE_CONSTANT_WIZARD_PAGE=				PREFIX + "inline_constant_wizard_page_context"; //$NON-NLS-1$
	public static final String SELECT_CLEAN_UPS_PAGE= 					PREFIX + "select_clean_ups_wizard_page_context"; //$NON-NLS-1$
	
	public static final String INFER_TYPE_ARGUMENTS_WIZARD_PAGE= PREFIX + "infer_type_arguments_wizard_page_context"; //$NON-NLS-1$
	public static final String CHANGE_TYPE_WIZARD_PAGE= PREFIX + "change_type_wizard_page_context"; //$NON-NLS-1$
	public static final String INLINE_TEMP_WIZARD_PAGE= PREFIX + "inline_temp_wizard_page_context"; //$NON-NLS-1$
	
	// reused ui-blocks
	public static final String BUILD_PATH_BLOCK= PREFIX + "build_paths_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_BLOCK= PREFIX + "source_attachment_context"; //$NON-NLS-1$
	
	// Custom Filters
	public static final String CUSTOM_FILTERS_DIALOG= PREFIX + "open_custom_filters_dialog_context"; //$NON-NLS-1$
    
    // Call Hierarchy
    public static final String CALL_HIERARCHY_VIEW= PREFIX + "call_hierarchy_view_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_FILTERS_DIALOG= PREFIX + "call_hierarchy_filters_dialog_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_FOCUS_ON_SELECTION_ACTION= PREFIX + "call_hierarchy_focus_on_selection_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_HISTORY_ACTION= PREFIX + "call_hierarchy_history_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_HISTORY_DROP_DOWN_ACTION= PREFIX + "call_hierarchy_history_drop_down_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_REFRESH_ACTION= PREFIX + "call_hierarchy_refresh_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_SEARCH_SCOPE_ACTION= PREFIX + "call_hierarchy_search_scope_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_TOGGLE_CALL_MODE_ACTION= PREFIX + "call_hierarchy_toggle_call_mode_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_TOGGLE_JAVA_LABEL_FORMAT_ACTION= PREFIX + "call_hierarchy_toggle_java_label_format_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_TOGGLE_ORIENTATION_ACTION= PREFIX + "call_hierarchy_toggle_call_mode_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_COPY_ACTION= PREFIX + "call_hierarchy_copy_action_context"; //$NON-NLS-1$
	public static final String CALL_HIERARCHY_TOGGLE_IMPLEMENTORS_ACTION= PREFIX + "call_hierarchy_toggle_implementors_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_OPEN_ACTION= PREFIX + "call_hierarchy_open_action_context"; //$NON-NLS-1$
    public static final String CALL_HIERARCHY_CANCEL_SEARCH_ACTION= PREFIX + "call_hierarchy_cancel_search_action_context"; //$NON-NLS-1$

    /**
     * 
     */
    //User library preference page
	public static final String CP_EDIT_USER_LIBRARY= PREFIX + "cp_edit_user_library"; //$NON-NLS-1$
	public static final String CP_EXPORT_USER_LIBRARY= PREFIX + "cp_export_user_library"; //$NON-NLS-1$
	public static final String CP_IMPORT_USER_LIBRARY= PREFIX + "cp_import_user_library"; //$NON-NLS-1$
	//Code style preference page
	public static final String CODE_STYLE_EDIT_PREFIX_SUFFIX= PREFIX + "code_style_edit_prefix_suffix"; //$NON-NLS-1$
	//Task tag preference page
	public static final String TASK_TAG_INPUT_DIALOG= PREFIX + "todo_task_input_dialog_context"; //$NON-NLS-1$
	//Build path
	public static final String BP_CHOOSE_EXISTING_FOLDER_TO_MAKE_SOURCE_FOLDER= PREFIX + "bp_choose_existing_folder_to_make_source_folder"; //$NON-NLS-1$
	public static final String BP_CREATE_NEW_FOLDER= PREFIX + "bp_create_new_folder_dialog"; //$NON-NLS-1$
	public static final String BP_SELECT_DEFAULT_OUTPUT_FOLDER_DIALOG= PREFIX + "bp_select_default_output_folder_dialog"; //$NON-NLS-1$
	public static final String BP_SELECT_CLASSPATH_CONTAINER= PREFIX + "bp_select_classpath_container"; //$NON-NLS-1$
}
