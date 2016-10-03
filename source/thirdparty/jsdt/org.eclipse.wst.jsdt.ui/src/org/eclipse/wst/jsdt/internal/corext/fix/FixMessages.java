/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.fix;

import org.eclipse.osgi.util.NLS;

public final class FixMessages extends NLS {
	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.corext.fix.FixMessages"; //$NON-NLS-1$

	private FixMessages() {
	}

	public static String CleanUpPostSaveListener_name;
	public static String CleanUpPostSaveListener_SaveAction_ChangeName;
	public static String CleanUpPostSaveListener_unknown_profile_error_message;
	
	public static String CleanUpRefactoring_checkingPostConditions_message;
	public static String CleanUpRefactoring_clean_up_multi_chang_name;
	public static String CleanUpRefactoring_could_not_retrive_profile;
	public static String CleanUpRefactoring_Parser_Startup_message;
	public static String CleanUpRefactoring_Refactoring_name;
	public static String CleanUpRefactoring_ProcessingCompilationUnit_message;
	public static String CleanUpRefactoring_Initialize_message;
	public static String CodeStyleFix_change_name;
	public static String ControlStatementsFix_change_name;
	
	public static String ConvertIterableLoopOperation_RemoveUpdateExpression_Warning;
	public static String ConvertIterableLoopOperation_RemoveUpdateExpressions_Warning;
	public static String ConvertIterableLoopOperation_semanticChangeWarning;
	public static String ExpressionsFix_add_parenthesis_change_name;
	public static String ExpressionsFix_remove_parenthesis_change_name;
	public static String ImportsFix_OrganizeImports_Description;
	
	public static String SortMembersFix_Change_description;
	public static String SortMembersFix_Fix_description;
	public static String UnusedCodeFix_change_name;
	public static String UnusedCodeFix_RemoveFieldOrLocal_AlteredAssignments_preview;
	
	public static String UnusedCodeFix_RemoveFieldOrLocal_description;
	public static String UnusedCodeFix_RemoveFieldOrLocal_RemovedAssignments_preview;
	public static String UnusedCodeFix_RemoveFieldOrLocalWithInitializer_description;
	public static String UnusedCodeFix_RemoveMethod_description;
	public static String UnusedCodeFix_RemoveConstructor_description;
	public static String UnusedCodeFix_RemoveType_description;
	public static String UnusedCodeFix_RemoveImport_description;
	public static String UnusedCodeFix_RemoveCast_description;
	public static String UnusedCodeFix_RemoveUnusedType_description;
	public static String UnusedCodeFix_RemoveUnusedConstructor_description;
	public static String UnusedCodeFix_RemoveUnusedPrivateMethod_description;
	public static String UnusedCodeFix_RemoveUnusedField_description;
	public static String UnusedCodeFix_RemoveUnusedVariabl_description;
	
	public static String Java50Fix_ConvertToEnhancedForLoop_description;
	public static String Java50Fix_AddTypeParameters_description;
	
	public static String StringFix_AddRemoveNonNls_description;
	public static String StringFix_AddNonNls_description;
	public static String StringFix_RemoveNonNls_description;
	
	public static String CodeStyleFix_ChangeAccessToStatic_description;
	public static String CodeStyleFix_QualifyWithThis_description;
	public static String CodeStyleFix_ChangeAccessToStaticUsingInstanceType_description;
	public static String CodeStyleFix_ChangeStaticAccess_description;
	public static String CodeStyleFix_ChangeIfToBlock_desription;
	public static String CodeStyleFix_ChangeElseToBlock_description;
	public static String CodeStyleFix_ChangeControlToBlock_description;
	public static String CodeStyleFix_ChangeAccessUsingDeclaring_description;
	public static String CodeStyleFix_QualifyFieldWithThis_description;
	public static String CodeStyleFix_QualifyMethodWithDeclClass_description;
	public static String CodeStyleFix_QualifyFieldWithDeclClass_description;
	
	public static String ControlStatementsFix_removeIfBlock_proposalDescription;
	public static String ControlStatementsFix_removeElseBlock_proposalDescription;
	public static String ControlStatementsFix_removeIfElseBlock_proposalDescription;
	public static String ControlStatementsFix_removeBrackets_proposalDescription;

	public static String ExpressionsFix_addParanoiacParenthesis_description;
	public static String ExpressionsFix_removeUnnecessaryParenthesis_description;
	public static String VariableDeclarationFix_add_final_change_name;
	
	public static String VariableDeclarationFix_changeModifierOfUnknownToFinal_description;
	public static String VariableDeclarationFix_ChangeMidifiersToFinalWherPossible_description;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, FixMessages.class);
	}
}
