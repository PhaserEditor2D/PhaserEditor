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
package org.eclipse.wst.jsdt.ui.actions;


/**
 * Action ids for standard actions, for groups in the menu bar, and
 * for actions in context menus of JDT views.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class JdtActionConstants {

	// Navigate menu
	
	/**
	 * Navigate menu: name of standard Goto Type global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.GoToType"</code>).
	 */
	public static final String GOTO_TYPE= "org.eclipse.wst.jsdt.ui.actions.GoToType"; //$NON-NLS-1$
	
	/**
	 * Navigate menu: name of standard Goto Package global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.GoToPackage"</code>).
	 */
	public static final String GOTO_PACKAGE= "org.eclipse.wst.jsdt.ui.actions.GoToPackage"; //$NON-NLS-1$
	
	/**
	 * Navigate menu: name of standard Open global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.Open"</code>).
	 */
	public static final String OPEN= "org.eclipse.wst.jsdt.ui.actions.Open"; //$NON-NLS-1$

	/**
	 * Navigate menu: name of standard Open Super Implementation global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.OpenSuperImplementation"</code>).
	 */
	public static final String OPEN_SUPER_IMPLEMENTATION= "org.eclipse.wst.jsdt.ui.actions.OpenSuperImplementation"; //$NON-NLS-1$
	
	/**
	 * Navigate menu: name of standard Open Type Hierarchy global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.OpenTypeHierarchy"</code>).
	 */
	public static final String OPEN_TYPE_HIERARCHY= "org.eclipse.wst.jsdt.ui.actions.OpenTypeHierarchy"; //$NON-NLS-1$

    /**
     * Navigate menu: name of standard Open Call Hierarchy global action
     * (value <code>"org.eclipse.wst.jsdt.ui.actions.OpenCallHierarchy"</code>).
     * 
     */
    public static final String OPEN_CALL_HIERARCHY= "org.eclipse.wst.jsdt.ui.actions.OpenCallHierarchy"; //$NON-NLS-1$

	/**
	 * Navigate menu: name of standard Open External Javadoc global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.OpenExternalJavaDoc"</code>).
	 */
	public static final String OPEN_EXTERNAL_JAVA_DOC= "org.eclipse.wst.jsdt.ui.actions.OpenExternalJavaDoc"; //$NON-NLS-1$
	
	/**
	 * Navigate menu: name of standard Show in Packages View global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ShowInPackagesView"</code>).
	 */
	public static final String SHOW_IN_PACKAGE_VIEW= "org.eclipse.wst.jsdt.ui.actions.ShowInPackagesView"; //$NON-NLS-1$

	/**
	 * Navigate menu: name of standard Show in Navigator View global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ShowInNaviagtorView"</code>).
	 */
	public static final String SHOW_IN_NAVIGATOR_VIEW= "org.eclipse.wst.jsdt.ui.actions.ShowInNaviagtorView"; //$NON-NLS-1$

	// Edit menu

	/**
	 * Edit menu: name of standard Code Assist global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ContentAssist"</code>).
	 */
	public static final String CONTENT_ASSIST= "org.eclipse.wst.jsdt.ui.actions.ContentAssist"; //$NON-NLS-1$

	// Source menu	
	
	/**
	 * Source menu: name of standard Comment global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.Comment"</code>).
	 */
	public static final String COMMENT= "org.eclipse.wst.jsdt.ui.actions.Comment"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Uncomment global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.Uncomment"</code>).
	 */
	public static final String UNCOMMENT= "org.eclipse.wst.jsdt.ui.actions.Uncomment"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard ToggleComment global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ToggleComment"</code>).
	 * 
	 */
	public static final String TOGGLE_COMMENT= "org.eclipse.wst.jsdt.ui.actions.ToggleComment"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Block Comment global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.AddBlockComment"</code>).
	 * 
	 * 
	 */
	public static final String ADD_BLOCK_COMMENT= "org.eclipse.wst.jsdt.ui.actions.AddBlockComment"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Block Uncomment global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.RemoveBlockComment"</code>).
	 * 
	 * 
	 */
	public static final String REMOVE_BLOCK_COMMENT= "org.eclipse.wst.jsdt.ui.actions.RemoveBlockComment"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Indent global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.Indent"</code>).
	 * 
	 * 
	 */
	public static final String INDENT= "org.eclipse.wst.jsdt.ui.actions.Indent"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Shift Right action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ShiftRight"</code>).
	 */
	public static final String SHIFT_RIGHT= "org.eclipse.wst.jsdt.ui.actions.ShiftRight"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Shift Left global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ShiftLeft"</code>).
	 */
	public static final String SHIFT_LEFT= "org.eclipse.wst.jsdt.ui.actions.ShiftLeft"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Format global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.Format"</code>).
	 */
	public static final String FORMAT= "org.eclipse.wst.jsdt.ui.actions.Format"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Format Element global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.FormatElement"</code>).
	 * 
	 */
	public static final String FORMAT_ELEMENT= "org.eclipse.wst.jsdt.ui.actions.FormatElement"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Add Import global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.AddImport"</code>).
	 */
	public static final String ADD_IMPORT= "org.eclipse.wst.jsdt.ui.actions.AddImport"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Organize Imports global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.OrganizeImports"</code>).
	 */
	public static final String ORGANIZE_IMPORTS= "org.eclipse.wst.jsdt.ui.actions.OrganizeImports"; //$NON-NLS-1$

	/**
	 * Source menu: name of standard Sort Members global action (value
	 * <code>"org.eclipse.wst.jsdt.ui.actions.SortMembers"</code>).
	 * 
	 */
	public static final String SORT_MEMBERS= "org.eclipse.wst.jsdt.ui.actions.SortMembers"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Surround with try/catch block global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.SurroundWithTryCatch"</code>).
	 */
	public static final String SURROUND_WITH_TRY_CATCH= "org.eclipse.wst.jsdt.ui.actions.SurroundWithTryCatch"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Override Methods global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.OverrideMethods"</code>).
	 */
	public static final String OVERRIDE_METHODS= "org.eclipse.wst.jsdt.ui.actions.OverrideMethods"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Generate Getter and Setter global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.GenerateGetterSetter"</code>).
	 */
	public static final String GENERATE_GETTER_SETTER= "org.eclipse.wst.jsdt.ui.actions.GenerateGetterSetter"; //$NON-NLS-1$

	/**
	 * Source menu: name of standard delegate methods global action (value
	 * <code>"org.eclipse.wst.jsdt.ui.actions.GenerateDelegateMethods"</code>).
	 * 
	 */
	public static final String GENERATE_DELEGATE_METHODS= "org.eclipse.wst.jsdt.ui.actions.GenerateDelegateMethods"; //$NON-NLS-1$

	/**
	 * Source menu: name of standard Add Constructor From Superclass global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.AddConstructorFromSuperclass"</code>).
	 */
	public static final String ADD_CONSTRUCTOR_FROM_SUPERCLASS= "org.eclipse.wst.jsdt.ui.actions.AddConstructorFromSuperclass"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Generate Constructor using Fields global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.GenerateConstructorUsingFields"</code>).
	 */
	public static final String GENERATE_CONSTRUCTOR_USING_FIELDS= "org.eclipse.wst.jsdt.ui.actions.GenerateConstructorUsingFields"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Generate hashCode() and equals() global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.GenerateHashCodeEquals"</code>).
	 * 
	 */
	public static final String GENERATE_HASHCODE_EQUALS= "org.eclipse.wst.jsdt.ui.actions.GenerateHashCodeEquals"; //$NON-NLS-1$

	/**
	 * Source menu: name of standard Add Javadoc Comment global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.AddJavaDocComment"</code>).
	 */
	public static final String ADD_JAVA_DOC_COMMENT= "org.eclipse.wst.jsdt.ui.actions.AddJavaDocComment"; //$NON-NLS-1$

	/**
	 * Source menu: name of standard Externalize Strings global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ExternalizeStrings"</code>).
	 */
	public static final String EXTERNALIZE_STRINGS= "org.eclipse.wst.jsdt.ui.actions.ExternalizeStrings"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Convert Line Delimiters To Windows global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ConvertLineDelimitersToWindows"</code>).
	 */
	public static final String CONVERT_LINE_DELIMITERS_TO_WINDOWS= "org.eclipse.wst.jsdt.ui.actions.ConvertLineDelimitersToWindows"; //$NON-NLS-1$

	/**
	 * Source menu: name of standard Convert Line Delimiters To UNIX global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ConvertLineDelimitersToUNIX"</code>).
	 */
	public static final String CONVERT_LINE_DELIMITERS_TO_UNIX= "org.eclipse.wst.jsdt.ui.actions.ConvertLineDelimitersToUNIX"; //$NON-NLS-1$

	/**
	 * Source menu: name of standardConvert Line Delimiters To Mac global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ConvertLineDelimitersToMac"</code>).
	 */
	public static final String CONVERT_LINE_DELIMITERS_TO_MAC= "org.eclipse.wst.jsdt.ui.actions.ConvertLineDelimitersToMac"; //$NON-NLS-1$
	
	/**
	 * Source menu: name of standard Clean up global action 
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.CleanUp"</code>).
	 * 
	 * 
	 */
	public static final String CLEAN_UP= "org.eclipse.wst.jsdt.ui.actions.CleanUp"; //$NON-NLS-1$

	// Refactor menu
	
	/**
	 * Refactor menu: name of standard Self Encapsulate Field global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.SelfEncapsulateField"</code>).
	 */
	public static final String SELF_ENCAPSULATE_FIELD= "org.eclipse.wst.jsdt.ui.actions.SelfEncapsulateField"; //$NON-NLS-1$
	
	/**
	 * Refactor menu: name of standard Modify Parameters global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ModifyParameters"</code>).
	 */
	public static final String MODIFY_PARAMETERS= "org.eclipse.wst.jsdt.ui.actions.ModifyParameters"; //$NON-NLS-1$
	
	/**
	 * Refactor menu: name of standard Pull Up global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.PullUp"</code>).
	 */
	public static final String PULL_UP= "org.eclipse.wst.jsdt.ui.actions.PullUp"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Push Down global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.PushDown"</code>).
	 * 
	 * 
	 */
	public static final String PUSH_DOWN= "org.eclipse.wst.jsdt.ui.actions.PushDown"; //$NON-NLS-1$
	
	/**
	 * Refactor menu: name of standard Move Element global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.Move"</code>).
	 */
	public static final String MOVE= "org.eclipse.wst.jsdt.ui.actions.Move"; //$NON-NLS-1$
	
	/**
	 * Refactor menu: name of standard Rename Element global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.Rename"</code>).
	 */
	public static final String RENAME= "org.eclipse.wst.jsdt.ui.actions.Rename"; //$NON-NLS-1$
	
	/**
	 * Refactor menu: name of standard Extract Temp global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ExtractTemp"</code>).
	 */
	public static final String EXTRACT_TEMP= "org.eclipse.wst.jsdt.ui.actions.ExtractTemp"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Extract Constant global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ExtractConstant"</code>).
	 * 
	 * 
	 */
	public static final String EXTRACT_CONSTANT= "org.eclipse.wst.jsdt.ui.actions.ExtractConstant"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Introduce Parameter global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.IntroduceParameter"</code>).
	 * 
	 * 
	 */
	public static final String INTRODUCE_PARAMETER= "org.eclipse.wst.jsdt.ui.actions.IntroduceParameter"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Introduce Factory global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.IntroduceFactory"</code>).
	 * 
	 * 
	 */
	public static final String INTRODUCE_FACTORY= "org.eclipse.wst.jsdt.ui.actions.IntroduceFactory"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Extract Method global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ExtractMethod"</code>).
	 */
	public static final String EXTRACT_METHOD= "org.eclipse.wst.jsdt.ui.actions.ExtractMethod"; //$NON-NLS-1$
	
	/**
	 * Refactor menu: name of standard Replace Invocations global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReplaceInvocations"</code>).
	 * 
	 * 
	 */
	public static final String REPLACE_INVOCATIONS="org.eclipse.wst.jsdt.ui.actions.ReplaceInvocations"; //$NON-NLS-1$
	
	/**
	 * Refactor menu: name of standard Introduce Indirection global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.IntroduceIndirection"</code>).
	 * 
	 * 
	 */
	public static final String INTRODUCE_INDIRECTION= "org.eclipse.wst.jsdt.ui.actions.IntroduceIndirection"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Inline global action 
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.Inline"</code>).
	 *
	 * 
	 */
	public static final String INLINE= "org.eclipse.wst.jsdt.ui.actions.Inline"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Extract Interface global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ExtractInterface"</code>).
	 * 
	 * 
	 */
	public static final String EXTRACT_INTERFACE= "org.eclipse.wst.jsdt.ui.actions.ExtractInterface"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Generalize Declared Type global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ChangeType"</code>).
	 * 
	 * 
	 */
	public static final String CHANGE_TYPE= "org.eclipse.wst.jsdt.ui.actions.ChangeType"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard global action to convert a nested type to a top level type
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.MoveInnerToTop"</code>).
	 * 
	 * 
	 */
	public static final String CONVERT_NESTED_TO_TOP= "org.eclipse.wst.jsdt.ui.actions.ConvertNestedToTop"; //$NON-NLS-1$
	
	/**
	 * Refactor menu: name of standard Use Supertype global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.UseSupertype"</code>).
	 * 
	 * 
	 */
	public static final String USE_SUPERTYPE= "org.eclipse.wst.jsdt.ui.actions.UseSupertype"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Infer Generic Type Arguments global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.InferTypeArguments"</code>).
	 * 
	 * 
	 */
	public static final String INFER_TYPE_ARGUMENTS= "org.eclipse.wst.jsdt.ui.actions.InferTypeArguments"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard global action to convert a local
	 * variable to a field (value <code>"org.eclipse.wst.jsdt.ui.actions.ConvertLocalToField"</code>).
	 * 
	 * 
	 */
	public static final String CONVERT_LOCAL_TO_FIELD= "org.eclipse.wst.jsdt.ui.actions.ConvertLocalToField"; //$NON-NLS-1$

	/**
	 * Refactor menu: name of standard Covert Anonymous to Nested global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ConvertAnonymousToNested"</code>).
	 * 
	 * 
	 */
	public static final String CONVERT_ANONYMOUS_TO_NESTED= "org.eclipse.wst.jsdt.ui.actions.ConvertAnonymousToNested"; //$NON-NLS-1$
	
	// Search Menu
	
	/**
	 * Search menu: name of standard Find References in Workspace global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReferencesInWorkspace"</code>).
	 */
	public static final String FIND_REFERENCES_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.actions.ReferencesInWorkspace"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find References in Project global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReferencesInProject"</code>).
	 */
	public static final String FIND_REFERENCES_IN_PROJECT= "org.eclipse.wst.jsdt.ui.actions.ReferencesInProject"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find References in Hierarchy global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReferencesInHierarchy"</code>).
	 */
	public static final String FIND_REFERENCES_IN_HIERARCHY= "org.eclipse.wst.jsdt.ui.actions.ReferencesInHierarchy"; //$NON-NLS-1$
	
	/**
	 * Search menu: name of standard Find References in Working Set global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReferencesInWorkingSet"</code>).
	 */
	public static final String FIND_REFERENCES_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.actions.ReferencesInWorkingSet"; //$NON-NLS-1$



	/**
	 * Search menu: name of standard Find Declarations in Workspace global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.DeclarationsInWorkspace"</code>).
	 */
	public static final String FIND_DECLARATIONS_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.actions.DeclarationsInWorkspace"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Declarations in Project global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.DeclarationsInProject"</code>).
	 */
	public static final String FIND_DECLARATIONS_IN_PROJECT= "org.eclipse.wst.jsdt.ui.actions.DeclarationsInProject"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Declarations in Hierarchy global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.DeclarationsInHierarchy"</code>).
	 */
	public static final String FIND_DECLARATIONS_IN_HIERARCHY= "org.eclipse.wst.jsdt.ui.actions.DeclarationsInHierarchy"; //$NON-NLS-1$
	
	/**
	 * Search menu: name of standard Find Declarations in Working Set global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.DeclarationsInWorkingSet"</code>).
	 */
	public static final String FIND_DECLARATIONS_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.actions.DeclarationsInWorkingSet"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Implementors in Workspace global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ImplementorsInWorkspace"</code>).
	 */
	public static final String FIND_IMPLEMENTORS_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.actions.ImplementorsInWorkspace"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Implementors in Project global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ImplementorsInProject"</code>).
	 */
	public static final String FIND_IMPLEMENTORS_IN_PROJECT= "org.eclipse.wst.jsdt.ui.actions.ImplementorsInProject"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Implementors in Working Set global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ImplementorsInWorkingSet"</code>).
	 */
	public static final String FIND_IMPLEMENTORS_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.actions.ImplementorsInWorkingSet"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Read Access in Workspace global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReadAccessInWorkspace"</code>).
	 */
	public static final String FIND_READ_ACCESS_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.actions.ReadAccessInWorkspace"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Read Access in Project global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReadAccessInProject"</code>).
	 */
	public static final String FIND_READ_ACCESS_IN_PROJECT= "org.eclipse.wst.jsdt.ui.actions.ReadAccessInProject"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Read Access in Hierarchy global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReadAccessInHierarchy"</code>).
	 */
	public static final String FIND_READ_ACCESS_IN_HIERARCHY= "org.eclipse.wst.jsdt.ui.actions.ReadAccessInHierarchy"; //$NON-NLS-1$
	
	/**
	 * Search menu: name of standard Find Read Access in Working Set global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.ReadAccessInWorkingSet"</code>).
	 */
	public static final String FIND_READ_ACCESS_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.actions.ReadAccessInWorkingSet"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Write Access in Workspace global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.WriteAccessInWorkspace"</code>).
	 */
	public static final String FIND_WRITE_ACCESS_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.actions.WriteAccessInWorkspace"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Write Access in Project global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.WriteAccessInProject"</code>).
	 */
	public static final String FIND_WRITE_ACCESS_IN_PROJECT= "org.eclipse.wst.jsdt.ui.actions.WriteAccessInProject"; //$NON-NLS-1$

	/**
	 * Search menu: name of standard Find Read Access in Hierarchy global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.WriteAccessInHierarchy"</code>).
	 */
	public static final String FIND_WRITE_ACCESS_IN_HIERARCHY= "org.eclipse.wst.jsdt.ui.actions.WriteAccessInHierarchy"; //$NON-NLS-1$
	
	/**
	 * Search menu: name of standard Find Read Access in Working Set global action
	 * (value <code>"org.eclipse.wst.jsdt.ui.actions.WriteAccessInWorkingSet"</code>).
	 */
	public static final String FIND_WRITE_ACCESS_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.actions.WriteAccessInWorkingSet"; //$NON-NLS-1$
	
	/**
	 * Search menu: name of standard Occurrences in File global action (value
	 * <code>"org.eclipse.wst.jsdt.ui.actions.OccurrencesInFile"</code>).
	 * 
	 * 
	 */
	public static final String FIND_OCCURRENCES_IN_FILE= "org.eclipse.wst.jsdt.ui.actions.OccurrencesInFile"; //$NON-NLS-1$
	
	/**
	 * Search menu: name of standard Find exception occurrences global action (value
	 * <code>"org.eclipse.wst.jsdt.ui.actions.ExceptionOccurrences"</code>).
	 * 
	 * 
	 */
	public static final String FIND_EXCEPTION_OCCURRENCES= "org.eclipse.wst.jsdt.ui.actions.ExceptionOccurrences"; //$NON-NLS-1$
	
	/**
	 * Search menu: name of standard Find implement occurrences global action (value
	 * <code>"org.eclipse.wst.jsdt.ui.actions.ImplementOccurrences"</code>).
	 * 
	 * 
	 */
	public static final String FIND_IMPLEMENT_OCCURRENCES= "org.eclipse.wst.jsdt.ui.actions.ImplementOccurrences"; //$NON-NLS-1$		


}
