/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Michael Spector <spektom@gmail.com> -  Bug 243886
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.problem;

import java.io.CharConversionException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.wst.jsdt.internal.compiler.IProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.BranchStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.Literal;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.NumberLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Reference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveryScanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.wst.jsdt.internal.compiler.util.Messages;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

public class ProblemReporter extends ProblemHandler {

	public ReferenceContext referenceContext;
	private Scanner positionScanner;

public static long getIrritant(int problemID) {
	switch(problemID){

		case IProblem.UninitializedLocalVariable:
			return CompilerOptions.UninitializedLocalVariable;
			
		case IProblem.UninitializedGlobalVariable:
			return CompilerOptions.UninitializedGlobalVariable;
			
		case IProblem.MaskedCatch :
			return CompilerOptions.MaskedCatchBlock;

		case IProblem.MethodButWithConstructorName :
			return CompilerOptions.MethodWithConstructorName;

		/* START -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */

		case IProblem.UndefinedName:
			return CompilerOptions.UnresolvedType;
		case IProblem.UndefinedFunction:
 		case IProblem.UndefinedMethod:
 		case IProblem.UndefinedConstructor:
			return CompilerOptions.UnresolvedMethod;
		/* END -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */
 		case IProblem.OptionalSemiColon:
 				return CompilerOptions.OptionalSemicolon;
 		case IProblem.LooseVarDecl:
 				return CompilerOptions.LooseVariableDecl;
		/* START -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */


		/* END   -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */

		case IProblem.OverridingDeprecatedMethod :
		case IProblem.UsingDeprecatedType :
		case IProblem.UsingDeprecatedMethod :
		case IProblem.UsingDeprecatedConstructor :
		case IProblem.UsingDeprecatedField :
			return CompilerOptions.UsingDeprecatedAPI;

		case IProblem.LocalVariableIsNeverUsed :
			return CompilerOptions.UnusedLocalVariable;

		case IProblem.ArgumentIsNeverUsed :
			return CompilerOptions.UnusedArgument;

		case IProblem.NeedToEmulateFieldReadAccess :
		case IProblem.NeedToEmulateFieldWriteAccess :
		case IProblem.NeedToEmulateMethodAccess :
		case IProblem.NeedToEmulateConstructorAccess :
			return CompilerOptions.AccessEmulation;

		case IProblem.NonExternalizedStringLiteral :
		case IProblem.UnnecessaryNLSTag :
			return CompilerOptions.NonExternalizedString;

		case IProblem.UseAssertAsAnIdentifier :
			return CompilerOptions.AssertUsedAsAnIdentifier;

		case IProblem.UseEnumAsAnIdentifier :
			return CompilerOptions.EnumUsedAsAnIdentifier;

		case IProblem.NonStaticAccessToStaticMethod :
		case IProblem.NonStaticAccessToStaticField :
			return CompilerOptions.NonStaticAccessToStatic;

		case IProblem.IndirectAccessToStaticMethod :
		case IProblem.IndirectAccessToStaticField :
		case IProblem.IndirectAccessToStaticType :
			return CompilerOptions.IndirectStaticAccess;

		case IProblem.AssignmentHasNoEffect:
			return CompilerOptions.NoEffectAssignment;

		case IProblem.UnusedPrivateConstructor:
		case IProblem.UnusedPrivateMethod:
		case IProblem.UnusedPrivateField:
		case IProblem.UnusedPrivateType:
			return CompilerOptions.UnusedPrivateMember;

		case IProblem.LocalVariableHidingLocalVariable:
		case IProblem.LocalVariableHidingField:
		case IProblem.ArgumentHidingLocalVariable:
		case IProblem.ArgumentHidingField:
			return CompilerOptions.LocalVariableHiding;

		case IProblem.FieldHidingLocalVariable:
		case IProblem.FieldHidingField:
			return CompilerOptions.FieldHiding;

		case IProblem.TypeHidingType:
			return CompilerOptions.TypeHiding;

		case IProblem.PossibleAccidentalBooleanAssignment:
			return CompilerOptions.AccidentalBooleanAssign;

		case IProblem.SuperfluousSemicolon:
		case IProblem.EmptyControlFlowStatement:
			return CompilerOptions.EmptyStatement;

		case IProblem.UndocumentedEmptyBlock:
			return CompilerOptions.UndocumentedEmptyBlock;

		case IProblem.UnnecessaryInstanceof:
			return CompilerOptions.UnnecessaryTypeCheck;

		case IProblem.FinallyMustCompleteNormally:
			return CompilerOptions.FinallyBlockNotCompleting;

		case IProblem.UnusedMethodDeclaredThrownException:
		case IProblem.UnusedConstructorDeclaredThrownException:
			return CompilerOptions.UnusedDeclaredThrownException;

		case IProblem.UnqualifiedFieldAccess:
			return CompilerOptions.UnqualifiedFieldAccess;

		case IProblem.UnnecessaryElse:
			return CompilerOptions.UnnecessaryElse;

		case IProblem.ForbiddenReference:
			return CompilerOptions.ForbiddenReference;

		case IProblem.DiscouragedReference:
			return CompilerOptions.DiscouragedReference;

		case IProblem.NullLocalVariableReference:
			return CompilerOptions.NullReference;

		case IProblem.PotentialNullLocalVariableReference:
			return CompilerOptions.PotentialNullReference;
			
		case IProblem.RedefinedLocal:
			return CompilerOptions.DuplicateLocalVariables;

		case IProblem.RedundantLocalVariableNullAssignment:
		case IProblem.RedundantNullCheckOnNonNullLocalVariable:
		case IProblem.RedundantNullCheckOnNullLocalVariable:
		case IProblem.NonNullLocalVariableComparisonYieldsFalse:
		case IProblem.NullLocalVariableComparisonYieldsFalse:
		case IProblem.NullLocalVariableInstanceofYieldsFalse:
			return CompilerOptions.RedundantNullCheck;

		case IProblem.UnusedLabel :
			return CompilerOptions.UnusedLabel;

		case IProblem.JavadocUnexpectedTag:
		case IProblem.JavadocDuplicateReturnTag:
		case IProblem.JavadocInvalidThrowsClass:
		case IProblem.JavadocInvalidSeeReference:
		case IProblem.JavadocInvalidParamTagName:
		case IProblem.JavadocMalformedSeeReference:
		case IProblem.JavadocInvalidSeeHref:
		case IProblem.JavadocInvalidSeeArgs:
		case IProblem.JavadocInvalidTag:
		case IProblem.JavadocUnterminatedInlineTag:
		case IProblem.JavadocMissingHashCharacter:
		case IProblem.JavadocEmptyReturnTag:
		case IProblem.JavadocUnexpectedText:
		case IProblem.JavadocInvalidParamName:
		case IProblem.JavadocDuplicateParamName:
		case IProblem.JavadocMissingParamName:
		case IProblem.JavadocMissingIdentifier:
		case IProblem.JavadocInvalidMemberTypeQualification:
		case IProblem.JavadocInvalidThrowsClassName:
		case IProblem.JavadocDuplicateThrowsClassName:
		case IProblem.JavadocMissingThrowsClassName:
		case IProblem.JavadocMissingSeeReference:
		case IProblem.JavadocUndefinedField:
		case IProblem.JavadocAmbiguousField:
		case IProblem.JavadocUndefinedConstructor:
		case IProblem.JavadocAmbiguousConstructor:
		case IProblem.JavadocUndefinedMethod:
		case IProblem.JavadocAmbiguousMethod:
		case IProblem.JavadocParameterMismatch:
		case IProblem.JavadocUndefinedType:
		case IProblem.JavadocAmbiguousType:
		case IProblem.JavadocInternalTypeNameProvided:
		case IProblem.JavadocNoMessageSendOnArrayType:
		case IProblem.JavadocNoMessageSendOnBaseType:
		case IProblem.JavadocInheritedMethodHidesEnclosingName:
		case IProblem.JavadocInheritedFieldHidesEnclosingName:
		case IProblem.JavadocInheritedNameHidesEnclosingTypeName:
		case IProblem.JavadocNonStaticTypeFromStaticInvocation:
		case IProblem.JavadocNotVisibleField:
		case IProblem.JavadocNotVisibleConstructor:
		case IProblem.JavadocNotVisibleMethod:
		case IProblem.JavadocNotVisibleType:
		case IProblem.JavadocUsingDeprecatedField:
		case IProblem.JavadocUsingDeprecatedConstructor:
		case IProblem.JavadocUsingDeprecatedMethod:
		case IProblem.JavadocUsingDeprecatedType:
		case IProblem.JavadocHiddenReference:
			return CompilerOptions.InvalidJavadoc;

		case IProblem.JavadocMissingParamTag:
		case IProblem.JavadocMissingReturnTag:
		case IProblem.JavadocMissingThrowsTag:
			return CompilerOptions.MissingJavadocTags;

		case IProblem.JavadocMissing:
			return CompilerOptions.MissingJavadocComments;

		case IProblem.ParameterAssignment:
			return CompilerOptions.ParameterAssignment;

		case IProblem.FallthroughCase:
			return CompilerOptions.FallthroughCase;

		case IProblem.OverridingMethodWithoutSuperInvocation:
			return CompilerOptions.OverridingMethodWithoutSuperInvocation;
		case IProblem.UndefinedField:
			return CompilerOptions.UndefinedField;

		case IProblem.WrongNumberOfArguments:
			return CompilerOptions.WrongNumberOfArguments;
			
		case IProblem.MissingSemiColon:
			return CompilerOptions.OptionalSemicolon;

	
	}
	return 0;
}
/**
 * Compute problem category ID based on problem ID
 * @param problemID
 * @return a category ID
 * @see CategorizedProblem
 */
public static int getProblemCategory(int severity, int problemID) {
	categorizeOnIrritant: {
		// fatal problems even if optional are all falling into same category (not irritant based)
		if ((severity & ProblemSeverities.Fatal) != 0)
			break categorizeOnIrritant;
		long irritant = getIrritant(problemID);
		int irritantInt = (int) irritant;
		if (irritantInt == irritant) {
			switch (irritantInt) {
				case (int)CompilerOptions.MethodWithConstructorName:
				case (int)CompilerOptions.AccessEmulation:
				case (int)CompilerOptions.AssertUsedAsAnIdentifier:
				case (int)CompilerOptions.NonStaticAccessToStatic:
				case (int)CompilerOptions.UnqualifiedFieldAccess:
				case (int)CompilerOptions.UndocumentedEmptyBlock:
				case (int)CompilerOptions.IndirectStaticAccess:
					return CategorizedProblem.CAT_CODE_STYLE;

				case (int)CompilerOptions.MaskedCatchBlock:
				case (int)CompilerOptions.NoEffectAssignment:
				case (int)CompilerOptions.AccidentalBooleanAssign:
				case (int)CompilerOptions.EmptyStatement:
				case (int)CompilerOptions.FinallyBlockNotCompleting:
				case (int)CompilerOptions.UndefinedField:
					return CategorizedProblem.CAT_POTENTIAL_PROGRAMMING_PROBLEM;

				case (int)CompilerOptions.LocalVariableHiding:
				case (int)CompilerOptions.FieldHiding:
					return CategorizedProblem.CAT_NAME_SHADOWING_CONFLICT;

				case (int)CompilerOptions.UnusedLocalVariable:
				case (int)CompilerOptions.UnusedArgument:
				case (int)CompilerOptions.UnusedPrivateMember:
				case (int)CompilerOptions.UnusedDeclaredThrownException:
				case (int)CompilerOptions.UnnecessaryTypeCheck:
				case (int)CompilerOptions.UnnecessaryElse:
					return CategorizedProblem.CAT_UNNECESSARY_CODE;

				case (int)CompilerOptions.Task:
					return CategorizedProblem.CAT_UNSPECIFIED; // TODO may want to improve

				case (int)CompilerOptions.MissingJavadocComments:
				case (int)CompilerOptions.MissingJavadocTags:
				case (int)CompilerOptions.InvalidJavadoc:
				case (int)(CompilerOptions.InvalidJavadoc | CompilerOptions.UsingDeprecatedAPI):
					return CategorizedProblem.CAT_JAVADOC;

				default:
					break categorizeOnIrritant;
			}
		} else {
			irritantInt = (int)(irritant >>> 32);
			switch (irritantInt) {
				case (int)(CompilerOptions.FinalParameterBound >>> 32):
				case (int)(CompilerOptions.EnumUsedAsAnIdentifier >>> 32):
				case (int)(CompilerOptions.ParameterAssignment >>> 32):
					return CategorizedProblem.CAT_CODE_STYLE;

				case (int)(CompilerOptions.NullReference >>> 32):
				case (int)(CompilerOptions.PotentialNullReference >>> 32):
				case (int)(CompilerOptions.DuplicateLocalVariables >>> 32):
				case (int)(CompilerOptions.RedundantNullCheck >>> 32):
				case (int)(CompilerOptions.FallthroughCase >>> 32):
				case (int)(CompilerOptions.OverridingMethodWithoutSuperInvocation >>> 32):
				case (int)(CompilerOptions.UninitializedLocalVariable >>> 32):
				case (int)(CompilerOptions.UninitializedGlobalVariable >>> 32):
					return CategorizedProblem.CAT_POTENTIAL_PROGRAMMING_PROBLEM;

				case (int)(CompilerOptions.TypeHiding >>> 32):
					return CategorizedProblem.CAT_NAME_SHADOWING_CONFLICT;

				case (int)(CompilerOptions.UnusedLabel >>> 32):
					return CategorizedProblem.CAT_UNNECESSARY_CODE;

				case (int)(CompilerOptions.ForbiddenReference >>> 32):
				case (int)(CompilerOptions.DiscouragedReference >>> 32):
					return CategorizedProblem.CAT_RESTRICTION;

				default:
					break categorizeOnIrritant;
			}
		}
	}
	// categorize fatal problems per ID
	switch (problemID) {
		case IProblem.IsClassPathCorrect :
		case IProblem.CorruptedSignature :
			return CategorizedProblem.CAT_BUILDPATH;

		default :
			if ((problemID & IProblem.Syntax) != 0)
				return CategorizedProblem.CAT_SYNTAX;
			if ((problemID & IProblem.ImportRelated) != 0)
				return CategorizedProblem.CAT_IMPORT;
			if ((problemID & IProblem.TypeRelated) != 0)
				return CategorizedProblem.CAT_TYPE;
			if ((problemID & (IProblem.FieldRelated|IProblem.MethodRelated|IProblem.ConstructorRelated)) != 0)
				return CategorizedProblem.CAT_MEMBER;
	}
	return CategorizedProblem.CAT_INTERNAL;
}
public ProblemReporter(IErrorHandlingPolicy policy, CompilerOptions options, IProblemFactory problemFactory) {
	super(policy, options, problemFactory);
}
public void abortDueToInternalError(String errorMessage) {
	this.abortDueToInternalError(errorMessage, null);
}
public void abortDueToInternalError(String errorMessage, ASTNode location) {
	String[] arguments = new String[] {errorMessage};
	this.handle(
		IProblem.Unclassified,
		arguments,
		arguments,
		ProblemSeverities.Error | ProblemSeverities.Abort | ProblemSeverities.Fatal,
		location == null ? 0 : location.sourceStart,
		location == null ? 0 : location.sourceEnd);
}
public void alreadyDefinedLabel(char[] labelName, ASTNode location) {
	String[] arguments = new String[] {new String(labelName)};
	this.handle(
		IProblem.DuplicateLabel,
		arguments,
		arguments,
		location.sourceStart,
		location.sourceEnd);
}
public void assignmentHasNoEffect(AbstractVariableDeclaration location, char[] name){
	int severity = computeSeverity(IProblem.AssignmentHasNoEffect);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] { new String(name) };
	int start = location.sourceStart;
	int end = location.sourceEnd;
	if (location.initialization != null) {
		end = location.initialization.sourceEnd;
	}
	this.handle(
			IProblem.AssignmentHasNoEffect,
			arguments,
			arguments,
			severity,
			start,
			end);
}
public void assignmentHasNoEffect(Assignment location, char[] name){
	int severity = computeSeverity(IProblem.AssignmentHasNoEffect);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] { new String(name) };
	this.handle(
			IProblem.AssignmentHasNoEffect,
			arguments,
			arguments,
			severity,
			location.sourceStart,
			location.sourceEnd);
}
public void cannotReadSource(CompilationUnitDeclaration unit, AbortCompilationUnit abortException, boolean verbose) {
	String fileName = new String(unit.compilationResult.fileName);
	if (abortException.exception instanceof CharConversionException) {
		// specific encoding issue
		String encoding = abortException.encoding;
		if (encoding == null) {
			encoding = System.getProperty("file.encoding"); //$NON-NLS-1$
		}
		String[] arguments = new String[]{ fileName, encoding, };
		this.handle(
				IProblem.InvalidEncoding,
				arguments,
				arguments,
				0,
				0);
		return;
	}
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	if (verbose) {
		abortException.exception.printStackTrace(writer);
	} else {
		writer.print(abortException.exception.getClass().getName());
		writer.print(':');
		writer.print(abortException.exception.getMessage());
	}
	String exceptionTrace = stringWriter.toString();
	String[] arguments = new String[]{ fileName, exceptionTrace, };
	this.handle(
			IProblem.CannotReadSource,
			arguments,
			arguments,
			0,
			0);
}
public void cannotReturnOutsideFunction(ASTNode location) {
	this.handle(
		IProblem.CannotReturnOutsideFunction,
		NoArgument,
		NoArgument,
		location.sourceStart,
		location.sourceEnd);
}
/*
 * Given the current configuration, answers which category the problem
 * falls into:
 *		ProblemSeverities.Error | ProblemSeverities.Warning | ProblemSeverities.Ignore
 * when different from Ignore, severity can be coupled with ProblemSeverities.Optional
 * to indicate that this problem is configurable through options
 */
public int computeSeverity(int problemID){
	/*
	 * If semantic validation is not enabled and this is anything but a
	 * syntax, documentation, or task problem, ignore.
	 */
	if (!this.options.enableSemanticValidation && (problemID & IProblem.Syntax) == 0 && (problemID & IProblem.Javadoc) == 0 && problemID != IProblem.Task) {
		return ProblemSeverities.Ignore;
	}
	
	switch (problemID) {
		case IProblem.Task :
			return ProblemSeverities.Warning;
 		case IProblem.TypeCollidesWithPackage :
			return ProblemSeverities.Warning;

		/*
		 * JS Type mismatch is set to default as Warning
		 */
 		case IProblem.NotAFunction:
 		case IProblem.TypeMismatch:
 			return ProblemSeverities.Warning;

// 		case IProblem.UndefinedName:
// 		case IProblem.UndefinedFunction:
// 		case IProblem.UndefinedMethod:
// 		case IProblem.UndefinedField:
// 		case IProblem.UndefinedConstructor:
// 			break;
		/*
		 * Javadoc tags resolved references errors
		 */
		case IProblem.JavadocInvalidParamName:
		case IProblem.JavadocDuplicateParamName:
		case IProblem.JavadocMissingParamName:
		case IProblem.JavadocMissingIdentifier:
		case IProblem.JavadocInvalidMemberTypeQualification:
		case IProblem.JavadocInvalidThrowsClassName:
		case IProblem.JavadocDuplicateThrowsClassName:
		case IProblem.JavadocMissingThrowsClassName:
		case IProblem.JavadocMissingSeeReference:
		case IProblem.JavadocUndefinedField:
		case IProblem.JavadocAmbiguousField:
		case IProblem.JavadocUndefinedConstructor:
		case IProblem.JavadocAmbiguousConstructor:
		case IProblem.JavadocUndefinedMethod:
		case IProblem.JavadocAmbiguousMethod:
		case IProblem.JavadocParameterMismatch:
		case IProblem.JavadocUndefinedType:
		case IProblem.JavadocAmbiguousType:
		case IProblem.JavadocInternalTypeNameProvided:
		case IProblem.JavadocNoMessageSendOnArrayType:
		case IProblem.JavadocNoMessageSendOnBaseType:
		case IProblem.JavadocInheritedMethodHidesEnclosingName:
		case IProblem.JavadocInheritedFieldHidesEnclosingName:
		case IProblem.JavadocInheritedNameHidesEnclosingTypeName:
		case IProblem.JavadocNonStaticTypeFromStaticInvocation:
		case IProblem.JavadocEmptyReturnTag:
			if (!this.options.reportInvalidJavadocTags) {
				return ProblemSeverities.Ignore;
			}
			break;
		/*
		 * Javadoc invalid tags due to deprecated references
		 */
		case IProblem.JavadocUsingDeprecatedField:
		case IProblem.JavadocUsingDeprecatedConstructor:
		case IProblem.JavadocUsingDeprecatedMethod:
		case IProblem.JavadocUsingDeprecatedType:
			if (!(this.options.reportInvalidJavadocTags && this.options.reportInvalidJavadocTagsDeprecatedRef)) {
				return ProblemSeverities.Ignore;
			}
			break;
		/*
		 * Javadoc invalid tags due to non-visible references
		 */
		case IProblem.JavadocNotVisibleField:
		case IProblem.JavadocNotVisibleConstructor:
		case IProblem.JavadocNotVisibleMethod:
		case IProblem.JavadocNotVisibleType:
		case IProblem.JavadocHiddenReference:
			if (!(this.options.reportInvalidJavadocTags && this.options.reportInvalidJavadocTagsNotVisibleRef)) {
				return ProblemSeverities.Ignore;
			}
			break;
	}
	long irritant = getIrritant(problemID);
	if (irritant != 0) {
		if ((problemID & IProblem.Javadoc) != 0 && !this.options.docCommentSupport)
			return ProblemSeverities.Ignore;
		int severity = this.options.getSeverity(irritant);
		return (!options.enableSemanticValidation && ((severity & ProblemSeverities.Optional) != 0) ?
					ProblemSeverities.Ignore : 
						severity);
	}
	return ProblemSeverities.Error | ProblemSeverities.Fatal;
}
public void constantOutOfFormat(NumberLiteral literal) {
	// the literal is not in a correct format
	// this code is called on IntLiteral
	// example 000811 ...the 8 is uncorrect.

	if (literal instanceof IntLiteral) {
		char[] source = literal.source();
		try {
			final String Radix;
			final int radix;
			if ((source[1] == 'x') || (source[1] == 'X')) {
				radix = 16;
				Radix = "Hex"; //$NON-NLS-1$
			} else {
				radix = 8;
				Radix = "Octal"; //$NON-NLS-1$
			}
			//look for the first digit that is incorrect
			int place = -1;
			label : for (int i = radix == 8 ? 1 : 2; i < source.length; i++) {
				if (ScannerHelper.digit(source[i], radix) == -1) {
					place = i;
					break label;
				}
			}
			String[] arguments = new String[] {
				new String(literal.literalType(null).readableName()), // numeric literals do not need scope to reach type
				Radix + " " + new String(source) + " (digit " + new String(new char[] {source[place]}) + ")"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			this.handle(
				IProblem.NumericValueOutOfRange,
				arguments,
				arguments,
				ProblemSeverities.Ignore,
				literal.sourceStart,
				literal.sourceEnd);
			return;
		} catch (IndexOutOfBoundsException ex) {
			// should never happen
		}

		// just in case .... use a predefined error..
		// we should never come here...(except if the code changes !)
		this.constantOutOfRange(literal, literal.literalType(null)); // numeric literals do not need scope to reach type
	}
}
public void constantOutOfRange(Literal literal, TypeBinding literalType) {
	String[] arguments = new String[] {new String(literalType.readableName()), new String(literal.source())};
	this.handle(
		IProblem.NumericValueOutOfRange,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		literal.sourceStart,
		literal.sourceEnd);
}
public void corruptedSignature(TypeBinding enclosingType, char[] signature, int position) {
	this.handle(
		IProblem.CorruptedSignature,
		new String[] { new String(enclosingType.readableName()), new String(signature), String.valueOf(position) },
		new String[] { new String(enclosingType.shortReadableName()), new String(signature), String.valueOf(position) },
		ProblemSeverities.Ignore,
		0,
		0);
}
public void deprecatedField(FieldBinding field, ASTNode location) {
	int severity = computeSeverity(IProblem.UsingDeprecatedField);
	if (severity == ProblemSeverities.Ignore) return;
	this.handle(
		IProblem.UsingDeprecatedField,
		new String[] {new String(field.declaringClass.readableName()), new String(field.name)},
		new String[] {new String(field.declaringClass.shortReadableName()), new String(field.name)},
		ProblemSeverities.Ignore,
		nodeSourceStart(field, location),
		nodeSourceEnd(field, location));
}
public void deprecatedMethod(MethodBinding method, ASTNode location) {
	boolean isConstructor = method.isConstructor();
	int severity = computeSeverity(isConstructor ? IProblem.UsingDeprecatedConstructor : IProblem.UsingDeprecatedMethod);
	if (severity == ProblemSeverities.Ignore) return;
	if (isConstructor) {
		this.handle(
			IProblem.UsingDeprecatedConstructor,
			new String[] {new String(method.declaringClass.readableName()), typesAsString(method.isVarargs(), method.parameters, false)},
			new String[] {new String(method.declaringClass.shortReadableName()), typesAsString(method.isVarargs(), method.parameters, true)},
			ProblemSeverities.Ignore,
			location.sourceStart,
			location.sourceEnd);
	} else {
		this.handle(
			IProblem.UsingDeprecatedMethod,
			new String[] {new String(method.declaringClass.readableName()), new String(method.selector), typesAsString(method.isVarargs(), method.parameters, false)},
			new String[] {new String(method.declaringClass.shortReadableName()), new String(method.selector), typesAsString(method.isVarargs(), method.parameters, true)},
			ProblemSeverities.Ignore,
			location.sourceStart,
			location.sourceEnd);
	}
}
public void deprecatedType(TypeBinding type, ASTNode location) {
	if (location == null) return; // 1G828DN - no type ref for synthetic arguments
	int severity = computeSeverity(IProblem.UsingDeprecatedType);
	if (severity == ProblemSeverities.Ignore) return;
	type = type.leafComponentType();
	this.handle(
		IProblem.UsingDeprecatedType,
		new String[] {new String(type.readableName())},
		new String[] {new String(type.shortReadableName())},
		ProblemSeverities.Ignore,
		location.sourceStart,
		nodeSourceEnd(null, location));
}
public void duplicateCase(CaseStatement caseStatement) {
	this.handle(
		IProblem.DuplicateCase,
		NoArgument,
		NoArgument,
		caseStatement.sourceStart,
		caseStatement.sourceEnd);
}
public void duplicateDefaultCase(ASTNode statement) {
	this.handle(
		IProblem.DuplicateDefaultCase,
		NoArgument,
		NoArgument,
		statement.sourceStart,
		statement.sourceEnd);
}
public void duplicateFieldInType(SourceTypeBinding type, FieldDeclaration fieldDecl) {
	this.handle(
		IProblem.DuplicateField,
		new String[] {new String(type.sourceName()), new String(fieldDecl.name)},
		new String[] {new String(type.shortReadableName()), new String(fieldDecl.name)},
		ProblemSeverities.Ignore,
		fieldDecl.sourceStart,
		fieldDecl.sourceEnd);
}

public void duplicateFieldInType(SourceTypeBinding type, InferredAttribute fieldDecl) {
	this.handle(
		IProblem.DuplicateField,
		new String[] {new String(type.sourceName()), new String(fieldDecl.name)},
		new String[] {new String(type.shortReadableName()), new String(fieldDecl.name)},
		ProblemSeverities.Ignore,
		fieldDecl.sourceStart,
		fieldDecl.sourceEnd);
}
public void duplicateMethodInType( Binding type, AbstractMethodDeclaration methodDecl) {
    MethodBinding method = methodDecl.getBinding();
    
	this.handle(
		IProblem.DuplicateMethod,
		new String[] {
	        new String(methodDecl.getName()),
			new String(method.declaringClass.readableName()),
			typesAsString(method.isVarargs(), method.parameters, false)},
		new String[] {
			new String(methodDecl.getName()),
			new String(method.declaringClass.shortReadableName()),
			typesAsString(method.isVarargs(), method.parameters, true)},
			ProblemSeverities.Ignore,
		methodDecl.sourceStart,
		methodDecl.sourceEnd);
    
}
public void duplicateTypes(CompilationUnitDeclaration compUnitDecl, InferredType typeDecl) {
	String[] arguments = new String[] {new String(compUnitDecl.getFileName()), new String(typeDecl.getName())};
	this.referenceContext = compUnitDecl; // report the problem against the type not the entire compilation unit
	this.handle(
		IProblem.DuplicateTypes,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		typeDecl.sourceStart,
		typeDecl.sourceEnd,
		this.referenceContext,
		compUnitDecl.compilationResult);
}
public void emptyControlFlowStatement(int sourceStart, int sourceEnd) {
	this.handle(
		IProblem.EmptyControlFlowStatement,
		NoArgument,
		NoArgument,
		sourceStart,
		sourceEnd);
}
public void expressionShouldBeAVariable(Expression expression) {
	this.handle(
		IProblem.ExpressionShouldBeAVariable,
		NoArgument,
		NoArgument,
		expression.sourceStart,
		expression.sourceEnd);
}
public void fieldHiding(FieldDeclaration fieldDecl, Binding hiddenVariable) {
	FieldBinding field = fieldDecl.binding;
	boolean isLocal = hiddenVariable instanceof LocalVariableBinding;
	int severity = computeSeverity(isLocal ? IProblem.FieldHidingLocalVariable : IProblem.FieldHidingField);
	if (severity == ProblemSeverities.Ignore) return;
	if (isLocal) {
		this.handle(
			IProblem.FieldHidingLocalVariable,
			new String[] {new String(field.declaringClass.readableName()), new String(field.name) },
			new String[] {new String(field.declaringClass.shortReadableName()), new String(field.name) },
			ProblemSeverities.Ignore,
			nodeSourceStart(hiddenVariable, fieldDecl),
			nodeSourceEnd(hiddenVariable, fieldDecl));
	} else if (hiddenVariable instanceof FieldBinding) {
		FieldBinding hiddenField = (FieldBinding) hiddenVariable;
		this.handle(
			IProblem.FieldHidingField,
			new String[] {new String(field.declaringClass.readableName()), new String(field.name) , new String(hiddenField.declaringClass.readableName())  },
			new String[] {new String(field.declaringClass.shortReadableName()), new String(field.name) , new String(hiddenField.declaringClass.shortReadableName()) },
			ProblemSeverities.Ignore,
			nodeSourceStart(hiddenField, fieldDecl),
			nodeSourceEnd(hiddenField, fieldDecl));
	}
}
public void finallyMustCompleteNormally(Block finallyBlock) {
	this.handle(
		IProblem.FinallyMustCompleteNormally,
		NoArgument,
		NoArgument,
		finallyBlock.sourceStart,
		finallyBlock.sourceEnd);
}
public void forbiddenReference(FieldBinding field, ASTNode location,
		String messageTemplate, int problemId) {
	this.handle(
		problemId,
		new String[] { new String(field.readableName()) }, // distinct from msg arg for quickfix purpose
		new String[] {
			MessageFormat.format(messageTemplate,
				new String[]{
					new String(field.shortReadableName()),
			        new String(field.declaringClass.shortReadableName())})},
			        ProblemSeverities.Ignore,
		nodeSourceStart(field, location),
		nodeSourceEnd(field, location));
}
public void forbiddenReference(MethodBinding method, ASTNode location,
		String messageTemplate, int problemId) {
	if (method.isConstructor())
		this.handle(
			problemId,
			new String[] { new String(method.readableName()) }, // distinct from msg arg for quickfix purpose
			new String[] {
				MessageFormat.format(messageTemplate,
						new String[]{new String(method.shortReadableName())})},
						ProblemSeverities.Ignore,
			location.sourceStart,
			location.sourceEnd);
	else
		this.handle(
			problemId,
			new String[] { new String(method.readableName()) }, // distinct from msg arg for quickfix purpose
			new String[] {
				MessageFormat.format(messageTemplate,
					new String[]{
						new String(method.shortReadableName()),
				        new String(method.declaringClass.shortReadableName())})},
				        ProblemSeverities.Ignore,
			location.sourceStart,
			location.sourceEnd);
}
public void forbiddenReference(TypeBinding type, ASTNode location, String messageTemplate, int problemId) {
	if (location == null) return;
	int severity = computeSeverity(problemId);
	if (severity == ProblemSeverities.Ignore) return;
	// this problem has a message template extracted from the access restriction rule
	this.handle(
		problemId,
		new String[] { new String(type.readableName()) }, // distinct from msg arg for quickfix purpose
		new String[] { MessageFormat.format(messageTemplate, new String[]{ new String(type.shortReadableName())})},
		ProblemSeverities.Ignore,
		location.sourceStart,
		location.sourceEnd);
}
public void forwardReference(Reference reference, int indexInQualification, TypeBinding type) {
	this.handle(
		IProblem.ReferenceToForwardField,
		NoArgument,
		NoArgument,
		ProblemSeverities.Ignore,
		reference.sourceStart,
		reference.sourceEnd);
}
// use this private API when the compilation unit result can be found through the
// reference context. Otherwise, use the other API taking a problem and a compilation result
// as arguments
private void handle(
	int problemId,
	String[] problemArguments,
	String[] messageArguments,
	int problemStartPosition,
	int problemEndPosition){

	this.handle(
			problemId,
			problemArguments,
			messageArguments,
			problemStartPosition,
			problemEndPosition,
			this.referenceContext,
			this.referenceContext == null ? null : this.referenceContext.compilationResult());
	this.referenceContext = null;
}
// use this private API when the compilation unit result cannot be found through the
// reference context.
private void handle(
	int problemId,
	String[] problemArguments,
	String[] messageArguments,
	int problemStartPosition,
	int problemEndPosition,
	CompilationResult unitResult){

	this.handle(
			problemId,
			problemArguments,
			messageArguments,
			problemStartPosition,
			problemEndPosition,
			this.referenceContext,
			unitResult);
	this.referenceContext = null;
}
// use this private API when the compilation unit result can be found through the
// reference context. Otherwise, use the other API taking a problem and a compilation result
// as arguments
private void handle(
	int problemId,
	String[] problemArguments,
	String[] messageArguments,
	int severity,
	int problemStartPosition,
	int problemEndPosition){

	this.handle(
			problemId,
			problemArguments,
			messageArguments,
			severity,
			problemStartPosition,
			problemEndPosition,
			this.referenceContext,
			this.referenceContext == null ? null : this.referenceContext.compilationResult());
	this.referenceContext = null;
}
public void hierarchyCircularity(SourceTypeBinding sourceType, ReferenceBinding superType, TypeReference reference) {
	int start = 0;
	int end = 0;

	if (reference == null) {	// can only happen when java.lang.Object is busted
		start = sourceType.sourceStart();
		end = sourceType.sourceEnd();
	} else {
		start = reference.sourceStart;
		end = reference.sourceEnd;
	}

	if (sourceType == superType)
		this.handle(
			IProblem.HierarchyCircularitySelfReference,
			new String[] {new String(sourceType.readableName()) },
			new String[] {new String(sourceType.shortReadableName()) },
			start,
			end);
	else
		this.handle(
			IProblem.HierarchyCircularity,
			new String[] {new String(sourceType.readableName()), new String(superType.readableName())},
			new String[] {new String(sourceType.shortReadableName()), new String(superType.shortReadableName())},
			start,
			end);
}

public void hierarchyHasProblems(SourceTypeBinding type) {
	String[] arguments = new String[] {new String(type.sourceName())};
	this.handle(
		IProblem.HierarchyHasProblems,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		type.sourceStart(),
		type.sourceEnd());
}
public void incompatibleReturnType(MethodBinding currentMethod, MethodBinding inheritedMethod) {
	StringBuffer methodSignature = new StringBuffer();
	methodSignature
		.append(inheritedMethod.declaringClass.readableName())
		.append('.')
		.append(inheritedMethod.readableName());

	StringBuffer shortSignature = new StringBuffer();
	shortSignature
		.append(inheritedMethod.declaringClass.shortReadableName())
		.append('.')
		.append(inheritedMethod.shortReadableName());

	int id;
	final ReferenceBinding declaringClass = currentMethod.declaringClass;
	
	id = IProblem.IncompatibleReturnType;
	
	AbstractMethodDeclaration method = currentMethod.sourceMethod();
	int sourceStart = 0;
	int sourceEnd = 0;
	if (method == null) {
		if (declaringClass instanceof SourceTypeBinding && ((SourceTypeBinding) declaringClass).classScope != null) {
			SourceTypeBinding sourceTypeBinding = (SourceTypeBinding) declaringClass;
			sourceStart = sourceTypeBinding.sourceStart();
			sourceEnd = sourceTypeBinding.sourceEnd();
		}
	} else if (method.isConstructor() || ((MethodDeclaration) method).returnType==null){
		sourceStart = method.sourceStart;
		sourceEnd = method.sourceEnd;
	} else {
		TypeReference returnType = ((MethodDeclaration) method).returnType;
		sourceStart = returnType.sourceStart;
		sourceEnd = returnType.sourceEnd;
	}
	this.handle(
		id,
		new String[] {methodSignature.toString()},
		new String[] {shortSignature.toString()},
		ProblemSeverities.Ignore,
		sourceStart,
		sourceEnd);
}
public void indirectAccessToStaticField(ASTNode location, FieldBinding field){
	int severity = computeSeverity(IProblem.IndirectAccessToStaticField);
	if (severity == ProblemSeverities.Ignore) return;
	this.handle(
		IProblem.IndirectAccessToStaticField,
		new String[] {new String(field.declaringClass.readableName()), new String(field.name)},
		new String[] {new String(field.declaringClass.shortReadableName()), new String(field.name)},
		ProblemSeverities.Ignore,
		nodeSourceStart(field, location),
		nodeSourceEnd(field, location));
}
public void indirectAccessToStaticMethod(ASTNode location, MethodBinding method) {
	int severity = computeSeverity(IProblem.IndirectAccessToStaticMethod);
	if (severity == ProblemSeverities.Ignore) return;
	this.handle(
		IProblem.IndirectAccessToStaticMethod,
		new String[] {new String(method.declaringClass.readableName()), new String(method.selector), typesAsString(method.isVarargs(), method.parameters, false)},
		new String[] {new String(method.declaringClass.shortReadableName()), new String(method.selector), typesAsString(method.isVarargs(), method.parameters, true)},
		ProblemSeverities.Ignore,
		location.sourceStart,
		location.sourceEnd);
}
public void inheritedMethodsHaveIncompatibleReturnTypes(SourceTypeBinding type, MethodBinding[] inheritedMethods, int length) {
	StringBuffer methodSignatures = new StringBuffer();
	StringBuffer shortSignatures = new StringBuffer();
	for (int i = length; --i >= 0;) {
		methodSignatures
			.append(inheritedMethods[i].declaringClass.readableName())
			.append('.')
			.append(inheritedMethods[i].readableName());
		shortSignatures
			.append(inheritedMethods[i].declaringClass.shortReadableName())
			.append('.')
			.append(inheritedMethods[i].shortReadableName());
		if (i != 0){
			methodSignatures.append(", "); //$NON-NLS-1$
			shortSignatures.append(", "); //$NON-NLS-1$
		}
	}

	this.handle(
		// Return type is incompatible with %1
		// 9.4.2 - The return type from the method is incompatible with the declaration.
		IProblem.IncompatibleReturnType,
		new String[] {methodSignatures.toString()},
		new String[] {shortSignatures.toString()},
		ProblemSeverities.Ignore,
		type.sourceStart(),
		type.sourceEnd());
}
public void invalidBreak(ASTNode location) {
	this.handle(
		IProblem.InvalidBreak,
		NoArgument,
		NoArgument,
		location.sourceStart,
		location.sourceEnd);
}
public void invalidConstructor(Statement statement, MethodBinding targetConstructor) {
	boolean insideDefaultConstructor =
		(this.referenceContext instanceof ConstructorDeclaration)
			&& ((ConstructorDeclaration)this.referenceContext).isDefaultConstructor();
	boolean insideImplicitConstructorCall =
		(statement instanceof ExplicitConstructorCall)
			&& (((ExplicitConstructorCall) statement).accessMode == ExplicitConstructorCall.ImplicitSuper);

	int sourceStart = statement.sourceStart;
	int sourceEnd = statement.sourceEnd;

	int id = IProblem.UndefinedConstructor; //default...
    MethodBinding shownConstructor = targetConstructor;
	switch (targetConstructor.problemId()) {
		case ProblemReasons.NotFound :
			if (insideDefaultConstructor){
				id = IProblem.UndefinedConstructorInDefaultConstructor;
			} else if (insideImplicitConstructorCall){
				id = IProblem.UndefinedConstructorInImplicitConstructorCall;
			} else {
				id = IProblem.UndefinedConstructor;
			}
			break;
		case ProblemReasons.NotVisible :
			if (insideDefaultConstructor){
				id = IProblem.NotVisibleConstructorInDefaultConstructor;
			} else if (insideImplicitConstructorCall){
				id = IProblem.NotVisibleConstructorInImplicitConstructorCall;
			} else {
				id = IProblem.NotVisibleConstructor;
			}
			ProblemMethodBinding problemConstructor = (ProblemMethodBinding) targetConstructor;
			if (problemConstructor.closestMatch != null) {
			    shownConstructor = problemConstructor.closestMatch.original();
		    }
			break;
		case ProblemReasons.Ambiguous :
			if (insideDefaultConstructor){
				id = IProblem.AmbiguousConstructorInDefaultConstructor;
			} else if (insideImplicitConstructorCall){
				id = IProblem.AmbiguousConstructorInImplicitConstructorCall;
			} else {
				id = IProblem.AmbiguousConstructor;
			}
			break;

		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}

	this.handle(
		id,
		new String[] {new String(targetConstructor.declaringClass.readableName()), typesAsString(shownConstructor.isVarargs(), shownConstructor.parameters, false)},
		new String[] {new String(targetConstructor.declaringClass.shortReadableName()), typesAsString(shownConstructor.isVarargs(), shownConstructor.parameters, true)},
		ProblemSeverities.Ignore,
		sourceStart,
		sourceEnd);
}
public void invalidContinue(ASTNode location) {
	this.handle(
		IProblem.InvalidContinue,
		NoArgument,
		NoArgument,
		location.sourceStart,
		location.sourceEnd);
}
public void invalidField(FieldReference fieldRef, TypeBinding searchedType) {
	if(isRecoveredName(fieldRef.token)) return;

	int id = IProblem.UndefinedField;
	FieldBinding field = fieldRef.binding;
	switch (field.problemId()) {
		case ProblemReasons.NotFound :
			id = IProblem.UndefinedField;
/* also need to check that the searchedType is the receiver type
			if (searchedType.isHierarchyInconsistent())
				severity = SecondaryError;
*/
			break;
		case ProblemReasons.NotVisible :
			this.handle(
				IProblem.NotVisibleField,
				new String[] {new String(fieldRef.token), new String(field.declaringClass.readableName())},
				new String[] {new String(fieldRef.token), new String(field.declaringClass.shortReadableName())},
				ProblemSeverities.Ignore,
				nodeSourceStart(field, fieldRef),
				nodeSourceEnd(field, fieldRef));
			return;
		case ProblemReasons.Ambiguous :
			id = IProblem.AmbiguousField;
			break;
		case ProblemReasons.NonStaticReferenceInStaticContext :
			id = IProblem.NonStaticFieldFromStaticInvocation;
			break;
		case ProblemReasons.NonStaticReferenceInConstructorInvocation :
			id = IProblem.InstanceFieldDuringConstructorInvocation;
			break;
		case ProblemReasons.InheritedNameHidesEnclosingName :
			id = IProblem.InheritedFieldHidesEnclosingName;
			break;
		case ProblemReasons.ReceiverTypeNotVisible :
			this.handle(
				IProblem.NotVisibleType, // cannot occur in javadoc comments
				new String[] {new String(searchedType.leafComponentType().readableName())},
				new String[] {new String(searchedType.leafComponentType().shortReadableName())},
				ProblemSeverities.Ignore,
				fieldRef.receiver.sourceStart,
				fieldRef.receiver.sourceEnd);
			return;

		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}

	String[] arguments = new String[] {new String(field.readableName())};
	this.handle(
		id,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(field, fieldRef),
		nodeSourceEnd(field, fieldRef));
}
public void invalidField(NameReference nameRef, FieldBinding field) {
	if (nameRef instanceof QualifiedNameReference) {
		QualifiedNameReference ref = (QualifiedNameReference) nameRef;
		if (isRecoveredName(ref.tokens)) return;
	} else {
		SingleNameReference ref = (SingleNameReference) nameRef;
		if (isRecoveredName(ref.token)) return;
	}
	int id = IProblem.UndefinedField;
	switch (field.problemId()) {
		case ProblemReasons.NotFound :
			id = IProblem.UndefinedField;
			break;
		case ProblemReasons.NotVisible :
			char[] name = field.readableName();
			name = CharOperation.lastSegment(name, '.');
			this.handle(
				IProblem.NotVisibleField,
				new String[] {new String(name), new String(field.declaringClass.readableName())},
				new String[] {new String(name), new String(field.declaringClass.shortReadableName())},
				ProblemSeverities.Ignore,
				nodeSourceStart(field, nameRef),
				nodeSourceEnd(field, nameRef));
			return;
		case ProblemReasons.Ambiguous :
			id = IProblem.AmbiguousField;
			break;
		case ProblemReasons.NonStaticReferenceInStaticContext :
			id = IProblem.NonStaticFieldFromStaticInvocation;
			break;
		case ProblemReasons.NonStaticReferenceInConstructorInvocation :
			id = IProblem.InstanceFieldDuringConstructorInvocation;
			break;
		case ProblemReasons.InheritedNameHidesEnclosingName :
			id = IProblem.InheritedFieldHidesEnclosingName;
			break;
		case ProblemReasons.ReceiverTypeNotVisible :
			this.handle(
				IProblem.NotVisibleType,
				new String[] {new String(field.declaringClass.leafComponentType().readableName())},
				new String[] {new String(field.declaringClass.leafComponentType().shortReadableName())},
				ProblemSeverities.Ignore,
				nameRef.sourceStart,
				nameRef.sourceEnd);
			return;
		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}
	String[] arguments = new String[] {new String(field.readableName())};
	this.handle(
		id,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nameRef.sourceStart,
		nameRef.sourceEnd);
}
public void invalidField(QualifiedNameReference nameRef, FieldBinding field, int index, TypeBinding searchedType) {
	//the resolution of the index-th field of qname failed
	//qname.otherBindings[index] is the binding that has produced the error

	//The different targetted errors should be :
	//UndefinedField
	//NotVisibleField
	//AmbiguousField

	if (isRecoveredName(nameRef.tokens)) return;

	if (searchedType.isBaseType()) {
		this.handle(
			IProblem.NoFieldOnBaseType,
			new String[] {
				new String(searchedType.readableName()),
				CharOperation.toString(CharOperation.subarray(nameRef.tokens, 0, index)),
				new String(nameRef.tokens[index])},
			new String[] {
				new String(searchedType.sourceName()),
				CharOperation.toString(CharOperation.subarray(nameRef.tokens, 0, index)),
				new String(nameRef.tokens[index])},
				ProblemSeverities.Ignore,
			nameRef.sourceStart,
			(int) nameRef.sourcePositions[index]);
		return;
	}

	int id = IProblem.UndefinedField;
	switch (field.problemId()) {
		case ProblemReasons.NotFound :
			id = IProblem.UndefinedField;
/* also need to check that the searchedType is the receiver type
			if (searchedType.isHierarchyInconsistent())
				severity = SecondaryError;
*/
			break;
		case ProblemReasons.NotVisible :
			String fieldName = new String(nameRef.tokens[index]);
			this.handle(
				IProblem.NotVisibleField,
				new String[] {fieldName, new String(field.declaringClass.readableName())},
				new String[] {fieldName, new String(field.declaringClass.shortReadableName())},
				ProblemSeverities.Ignore,
				nodeSourceStart(field, nameRef),
				nodeSourceEnd(field, nameRef));
			return;
		case ProblemReasons.Ambiguous :
			id = IProblem.AmbiguousField;
			break;
		case ProblemReasons.NonStaticReferenceInStaticContext :
			id = IProblem.NonStaticFieldFromStaticInvocation;
			break;
		case ProblemReasons.NonStaticReferenceInConstructorInvocation :
			id = IProblem.InstanceFieldDuringConstructorInvocation;
			break;
		case ProblemReasons.InheritedNameHidesEnclosingName :
			id = IProblem.InheritedFieldHidesEnclosingName;
			break;
		case ProblemReasons.ReceiverTypeNotVisible :
			this.handle(
				IProblem.NotVisibleType,
				new String[] {new String(searchedType.leafComponentType().readableName())},
				new String[] {new String(searchedType.leafComponentType().shortReadableName())},
				ProblemSeverities.Ignore,
				nameRef.sourceStart,
				nameRef.sourceEnd);
			return;
		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}
	String[] arguments = new String[] {CharOperation.toString(CharOperation.subarray(nameRef.tokens, 0, index + 1))};
	this.handle(
		id,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nameRef.sourceStart,
		(int) nameRef.sourcePositions[index]);
}
public void invalidMethod(MessageSend messageSend, MethodBinding method) {
	if (isRecoveredName(messageSend.selector)) return;

	boolean isFunction = method.declaringClass==null;
	int id = isFunction? IProblem.UndefinedFunction : IProblem.UndefinedMethod; //default...
    MethodBinding shownMethod = method;

	switch (method.problemId()) {
		case ProblemReasons.NotFound :
			id = isFunction? IProblem.UndefinedFunction : IProblem.UndefinedMethod;
			ProblemMethodBinding problemMethod = (ProblemMethodBinding) method;
			if (problemMethod.closestMatch != null) {
			    	shownMethod = problemMethod.closestMatch;
					String closestParameterTypeNames = typesAsString(shownMethod.isVarargs(), shownMethod.parameters, false);
					String parameterTypeNames = typesAsString(false, problemMethod.parameters, false);
					String closestParameterTypeShortNames = typesAsString(shownMethod.isVarargs(), shownMethod.parameters, true);
					String parameterTypeShortNames = typesAsString(false, problemMethod.parameters, true);
					this.handle(
						IProblem.ParameterMismatch,
						new String[] {
							new String(shownMethod.declaringClass.readableName()),
							new String(shownMethod.selector),
							closestParameterTypeNames,
							parameterTypeNames
						},
						new String[] {
							new String(shownMethod.declaringClass.shortReadableName()),
							new String(shownMethod.selector),
							closestParameterTypeShortNames,
							parameterTypeShortNames
						},
						ProblemSeverities.Ignore,
						(int) (messageSend.nameSourcePosition >>> 32),
						(int) messageSend.nameSourcePosition);
					return;
			}
			break;
		case ProblemReasons.NotVisible :
			id = IProblem.NotVisibleMethod;
			problemMethod = (ProblemMethodBinding) method;
			if (problemMethod.closestMatch != null) {
			    shownMethod = problemMethod.closestMatch.original();
		    }
			break;
		case ProblemReasons.Ambiguous :
			id = IProblem.AmbiguousMethod;
			break;
		case ProblemReasons.NotAFunction :
			id = IProblem.NotAFunction;
			break;
		case ProblemReasons.InheritedNameHidesEnclosingName :
			id = IProblem.InheritedMethodHidesEnclosingName;
			break;
		case ProblemReasons.NonStaticReferenceInConstructorInvocation :
			id = IProblem.InstanceMethodDuringConstructorInvocation;
			break;
		case ProblemReasons.NonStaticReferenceInStaticContext :
			id = IProblem.StaticMethodRequested;
			break;
		case ProblemReasons.ReceiverTypeNotVisible :
			this.handle(
				IProblem.NotVisibleType,	// cannot occur in javadoc comments
				new String[] {new String(method.declaringClass.leafComponentType().readableName())},
				new String[] {new String(method.declaringClass.leafComponentType().shortReadableName())},
				ProblemSeverities.Ignore,
				messageSend.receiver.sourceStart,
				messageSend.receiver.sourceEnd);
			return;
		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}

	String shortName=""; //$NON-NLS-1$
	String readableName=""; //$NON-NLS-1$
	String methodName=(shownMethod.selector!=null)? new String(shownMethod.selector):""; //$NON-NLS-1$
	if (method.declaringClass!=null)
	{
		shortName=		readableName=new String(method.declaringClass.readableName());

	}
	this.handle(
		id,
		new String[] {
				readableName,
				shortName, typesAsString(shownMethod.isVarargs(), shownMethod.parameters, false)},
		new String[] {
				shortName,
				methodName, typesAsString(shownMethod.isVarargs(), shownMethod.parameters, true)},
				ProblemSeverities.Ignore,
		(int) (messageSend.nameSourcePosition >>> 32),
		(int) messageSend.nameSourcePosition);
}
public void wrongNumberOfArguments(MessageSend functionCall, MethodBinding binding) {
	String functionName =  new String(functionCall.selector);
	int actualArguments=(functionCall.arguments!=null) ? functionCall.arguments.length : 0;
    String actualNumber=String.valueOf(actualArguments);
    String expectingNumber=String.valueOf(binding.parameters.length);
	
	this.handle(
		IProblem.WrongNumberOfArguments,
		new String[] {
				functionName,expectingNumber,actualNumber}, //$NON-NLS-1$
		new String[] {
				functionName,expectingNumber,actualNumber}, //$NON-NLS-1$
		ProblemSeverities.Ignore,
		functionCall.sourceStart,
		functionCall.sourceEnd);
}

public void wrongNumberOfArguments(AllocationExpression allocationExpression, MethodBinding binding) {
	char[] typeName = Util.getTypeName(allocationExpression.member);
	String functionName =  typeName!=null ? new String(typeName) : "";
	int actualArguments=(allocationExpression.arguments!=null) ? allocationExpression.arguments.length : 0;
    String actualNumber=String.valueOf(actualArguments);
    String expectingNumber=String.valueOf(binding.parameters.length);
	
	this.handle(
		IProblem.WrongNumberOfArguments,
		new String[] {
				functionName,expectingNumber,actualNumber}, //$NON-NLS-1$
		new String[] {
				functionName,expectingNumber,actualNumber}, //$NON-NLS-1$
				ProblemSeverities.Ignore,
				allocationExpression.sourceStart,
				allocationExpression.sourceEnd);
}


public void invalidOperator(BinaryExpression expression, TypeBinding leftType, TypeBinding rightType) {
	String leftName = new String(leftType.readableName());
	String rightName = new String(rightType.readableName());
	String leftShortName = new String(leftType.shortReadableName());
	String rightShortName = new String(rightType.shortReadableName());
	if (leftShortName.equals(rightShortName)){
		leftShortName = leftName;
		rightShortName = rightName;
	}
	this.handle(
		IProblem.InvalidOperator,
		new String[] {
			expression.operatorToString(),
			leftName + ", " + rightName}, //$NON-NLS-1$
		new String[] {
			expression.operatorToString(),
			leftShortName + ", " + rightShortName}, //$NON-NLS-1$
			ProblemSeverities.Ignore,
		expression.sourceStart,
		expression.sourceEnd);
}
public void invalidOperator(CompoundAssignment assign, TypeBinding leftType, TypeBinding rightType) {
	String leftName = new String(leftType.readableName());
	String rightName = new String(rightType.readableName());
	String leftShortName = new String(leftType.shortReadableName());
	String rightShortName = new String(rightType.shortReadableName());
	if (leftShortName.equals(rightShortName)){
		leftShortName = leftName;
		rightShortName = rightName;
	}
	this.handle(
		IProblem.InvalidOperator,
		new String[] {
			assign.operatorToString(),
			leftName + ", " + rightName}, //$NON-NLS-1$
		new String[] {
			assign.operatorToString(),
			leftShortName + ", " + rightShortName}, //$NON-NLS-1$
			ProblemSeverities.Ignore,
		assign.sourceStart,
		assign.sourceEnd);
}
public void invalidOperator(UnaryExpression expression, TypeBinding type) {
	this.handle(
		IProblem.InvalidOperator,
		new String[] {expression.operatorToString(), new String(type.readableName())},
		new String[] {expression.operatorToString(), new String(type.shortReadableName())},
		ProblemSeverities.Ignore,
		expression.sourceStart,
		expression.sourceEnd);
}
public void invalidType(ASTNode location, TypeBinding type) {
	if (type instanceof ReferenceBinding) {
		if (isRecoveredName(((ReferenceBinding)type).compoundName)) return;
	}
	else if (type instanceof ArrayBinding) {
		TypeBinding leafType = ((ArrayBinding)type).leafComponentType;
		if (leafType instanceof ReferenceBinding) {
			if (isRecoveredName(((ReferenceBinding)leafType).compoundName)) return;
		}
	}

	int id = IProblem.UndefinedType; // default
	switch (type.problemId()) {
		case ProblemReasons.NotFound :
			id = IProblem.UndefinedType;
			break;
		case ProblemReasons.NotVisible :
			id = IProblem.NotVisibleType;
			break;
		case ProblemReasons.Ambiguous :
			id = IProblem.AmbiguousType;
			break;
		case ProblemReasons.InternalNameProvided :
			id = IProblem.InternalTypeNameProvided;
			break;
		case ProblemReasons.InheritedNameHidesEnclosingName :
			id = IProblem.InheritedTypeHidesEnclosingName;
			break;
		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}

	int end = location.sourceEnd;
	if (location instanceof QualifiedNameReference) {
		QualifiedNameReference ref = (QualifiedNameReference) location;
		if (isRecoveredName(ref.tokens)) return;
		if (ref.indexOfFirstFieldBinding >= 1)
			end = (int) ref.sourcePositions[ref.indexOfFirstFieldBinding - 1];
	} else if (location instanceof ArrayQualifiedTypeReference) {
		ArrayQualifiedTypeReference arrayQualifiedTypeReference = (ArrayQualifiedTypeReference) location;
		if (isRecoveredName(arrayQualifiedTypeReference.tokens)) return;
		long[] positions = arrayQualifiedTypeReference.sourcePositions;
		end = (int) positions[positions.length - 1];
	} else if (location instanceof QualifiedTypeReference) {
		QualifiedTypeReference ref = (QualifiedTypeReference) location;
		if (isRecoveredName(ref.tokens)) return;
		if (type instanceof ReferenceBinding) {
			char[][] name = ((ReferenceBinding) type).compoundName;
			if (name.length <= ref.sourcePositions.length)
				end = (int) ref.sourcePositions[name.length - 1];
		}
	} else if (location instanceof ImportReference) {
		ImportReference ref = (ImportReference) location;
		if (isRecoveredName(ref.tokens)) return;
		if (type instanceof ReferenceBinding) {
			char[][] name = ((ReferenceBinding) type).compoundName;
			end = (int) ref.sourcePositions[name.length - 1];
		}
	} else if (location instanceof ArrayTypeReference) {
		ArrayTypeReference arrayTypeReference = (ArrayTypeReference) location;
		if (isRecoveredName(arrayTypeReference.token)) return;
		end = arrayTypeReference.originalSourceEnd;
	}
	this.handle(
		id,
		new String[] {new String(type.leafComponentType().readableName()) },
		new String[] {new String(type.leafComponentType().shortReadableName())},
		ProblemSeverities.Ignore,
		location.sourceStart,
		end);
}
public void invalidUnaryExpression(Expression expression) {
	this.handle(
		IProblem.InvalidUnaryExpression,
		NoArgument,
		NoArgument,
		expression.sourceStart,
		expression.sourceEnd);
}
public void invalidValueForGetterSetter(Expression expression, boolean isSetter) {
	int problemID;
 	if (isSetter) {
		problemID = IProblem.InvalidValueForSetter;
	} else {
		problemID = IProblem.InvalidValueForGetter;
	}
	this.handle(
		problemID,
		NoArgument,
		NoArgument,
		expression.sourceStart,
		expression.sourceEnd);
}
private boolean isIdentifier(int token) {
	return token == TerminalTokens.TokenNameIdentifier;
}
private boolean isKeyword(int token) {
	switch(token) {
		case TerminalTokens.TokenNameabstract:
		case TerminalTokens.TokenNamebyte:
		case TerminalTokens.TokenNamebreak:
		case TerminalTokens.TokenNameboolean:
		case TerminalTokens.TokenNamecase:
		case TerminalTokens.TokenNamechar:
		case TerminalTokens.TokenNamecatch:
		case TerminalTokens.TokenNameclass:
		case TerminalTokens.TokenNamecontinue:
		case TerminalTokens.TokenNamedo:
		case TerminalTokens.TokenNamedouble:
		case TerminalTokens.TokenNamedefault:
		case TerminalTokens.TokenNameelse:
		case TerminalTokens.TokenNameextends:
		case TerminalTokens.TokenNamefor:
		case TerminalTokens.TokenNamefinal:
		case TerminalTokens.TokenNamefloat:
		case TerminalTokens.TokenNamefalse:
		case TerminalTokens.TokenNamefinally:
		case TerminalTokens.TokenNameif:
		case TerminalTokens.TokenNameint:
		case TerminalTokens.TokenNameimport:
		case TerminalTokens.TokenNameinterface:
		case TerminalTokens.TokenNameimplements:
		case TerminalTokens.TokenNameinstanceof:
		case TerminalTokens.TokenNamelong:
		case TerminalTokens.TokenNamenew:
		case TerminalTokens.TokenNamenull:
		case TerminalTokens.TokenNamenative:
		case TerminalTokens.TokenNamepublic:
		case TerminalTokens.TokenNamepackage:
		case TerminalTokens.TokenNameprivate:
		case TerminalTokens.TokenNameprotected:
		case TerminalTokens.TokenNamereturn:
		case TerminalTokens.TokenNameshort:
		case TerminalTokens.TokenNamesuper:
		case TerminalTokens.TokenNamestatic:
		case TerminalTokens.TokenNameswitch:
		case TerminalTokens.TokenNamesynchronized:
		case TerminalTokens.TokenNametry:
		case TerminalTokens.TokenNamethis:
		case TerminalTokens.TokenNametrue:
		case TerminalTokens.TokenNamethrow:
		case TerminalTokens.TokenNamethrows:
		case TerminalTokens.TokenNametransient:
		case TerminalTokens.TokenNamevoid:
		case TerminalTokens.TokenNamevolatile:
		case TerminalTokens.TokenNamewhile:
		case TerminalTokens.TokenNamedelete :
		case TerminalTokens.TokenNamedebugger :
		case TerminalTokens.TokenNameexport :
		case TerminalTokens.TokenNamefunction :
		case TerminalTokens.TokenNamein :
//		case TerminalTokens.TokenNameinfinity :
		case TerminalTokens.TokenNameundefined :
		case TerminalTokens.TokenNamewith :
			return true;
		default:
			return false;
	}
}
private boolean isLiteral(int token) {
	switch(token) {
		case TerminalTokens.TokenNameIntegerLiteral:
		case TerminalTokens.TokenNameLongLiteral:
		case TerminalTokens.TokenNameFloatingPointLiteral:
		case TerminalTokens.TokenNameDoubleLiteral:
		case TerminalTokens.TokenNameStringLiteral:
		case TerminalTokens.TokenNameCharacterLiteral:
			return true;
		default:
			return false;
	}
}
private boolean isRecoveredName(char[] simpleName) {
	return simpleName == RecoveryScanner.FAKE_IDENTIFIER;
}
private boolean isRecoveredName(char[][] qualifiedName) {
	if(qualifiedName == null) return false;

	for (int i = 0; i < qualifiedName.length; i++) {
		if(qualifiedName[i] == RecoveryScanner.FAKE_IDENTIFIER) return true;
	}

	return false;
}

public void javadocDeprecatedField(FieldBinding field, ASTNode location, int modifiers) {
	int severity = computeSeverity(IProblem.JavadocUsingDeprecatedField);
	if (severity == ProblemSeverities.Ignore) return;
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		this.handle(
			IProblem.JavadocUsingDeprecatedField,
			new String[] {new String(field.declaringClass.readableName()), new String(field.name)},
			new String[] {new String(field.declaringClass.shortReadableName()), new String(field.name)},
			ProblemSeverities.Ignore,
			nodeSourceStart(field, location),
			nodeSourceEnd(field, location));
	}
}

public void javadocDeprecatedMethod(MethodBinding method, ASTNode location, int modifiers) {
	boolean isConstructor = method.isConstructor();
	int severity = computeSeverity(isConstructor ? IProblem.JavadocUsingDeprecatedConstructor : IProblem.JavadocUsingDeprecatedMethod);
	if (severity == ProblemSeverities.Ignore) return;
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		if (isConstructor) {
			this.handle(
				IProblem.JavadocUsingDeprecatedConstructor,
				new String[] {new String(method.declaringClass.readableName()), typesAsString(method.isVarargs(), method.parameters, false)},
				new String[] {new String(method.declaringClass.shortReadableName()), typesAsString(method.isVarargs(), method.parameters, true)},
				ProblemSeverities.Ignore,
				location.sourceStart,
				location.sourceEnd);
		} else {
			this.handle(
				IProblem.JavadocUsingDeprecatedMethod,
				new String[] {new String(method.declaringClass.readableName()), new String(method.selector), typesAsString(method.isVarargs(), method.parameters, false)},
				new String[] {new String(method.declaringClass.shortReadableName()), new String(method.selector), typesAsString(method.isVarargs(), method.parameters, true)},
				ProblemSeverities.Ignore,
				location.sourceStart,
				location.sourceEnd);
		}
	}
}
public void javadocDeprecatedType(TypeBinding type, ASTNode location, int modifiers) {
	if (location == null) return; // 1G828DN - no type ref for synthetic arguments
	int severity = computeSeverity(IProblem.JavadocUsingDeprecatedType);
	if (severity == ProblemSeverities.Ignore) return;
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		if (type.isMemberType() && type instanceof ReferenceBinding && !javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, ((ReferenceBinding)type).modifiers)) {
			this.handle(IProblem.JavadocHiddenReference, NoArgument, NoArgument, location.sourceStart, location.sourceEnd);
		} else {
			this.handle(
				IProblem.JavadocUsingDeprecatedType,
				new String[] {new String(type.readableName())},
				new String[] {new String(type.shortReadableName())},
				ProblemSeverities.Ignore,
				location.sourceStart,
				location.sourceEnd);
		}
	}
}
public void javadocDuplicatedParamTag(char[] token, int sourceStart, int sourceEnd, int modifiers) {
	int severity = computeSeverity(IProblem.JavadocDuplicateParamName);
	if (severity == ProblemSeverities.Ignore) return;
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		String[] arguments = new String[] {String.valueOf(token)};
		this.handle(
			IProblem.JavadocDuplicateParamName,
			arguments,
			arguments,
			severity,
			sourceStart,
			sourceEnd);
	}
}
public void javadocDuplicatedReturnTag(int sourceStart, int sourceEnd){
	this.handle(IProblem.JavadocDuplicateReturnTag, NoArgument, NoArgument, sourceStart, sourceEnd);
}
public void javadocEmptyReturnTag(int sourceStart, int sourceEnd, int modifiers) {
	int severity = computeSeverity(IProblem.JavadocEmptyReturnTag);
	if (severity == ProblemSeverities.Ignore) return;
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		this.handle(IProblem.JavadocEmptyReturnTag, NoArgument, NoArgument, sourceStart, sourceEnd);
	}
}
public void javadocHiddenReference(int sourceStart, int sourceEnd, Scope scope, int modifiers) {
	Scope currentScope = scope;
	while (currentScope.parent.kind != Scope.COMPILATION_UNIT_SCOPE ) {
		if (!javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, currentScope.getDeclarationModifiers())) {
			return;
		}
		currentScope = currentScope.parent;
	}
	String[] arguments = new String[] { this.options.getVisibilityString(this.options.reportInvalidJavadocTagsVisibility), this.options.getVisibilityString(modifiers) };
	this.handle(IProblem.JavadocHiddenReference, arguments, arguments, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocInvalidConstructor(Statement statement, MethodBinding targetConstructor, int modifiers) {

	if (!javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) return;
	int id = IProblem.JavadocUndefinedConstructor; //default...
	switch (targetConstructor.problemId()) {
		case ProblemReasons.NotFound :
			id = IProblem.JavadocUndefinedConstructor;
			break;
		case ProblemReasons.NotVisible :
			id = IProblem.JavadocNotVisibleConstructor;
			break;
		case ProblemReasons.Ambiguous :
			id = IProblem.JavadocAmbiguousConstructor;
			break;
		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}
	int severity = computeSeverity(id);
	if (severity == ProblemSeverities.Ignore) return;
	this.handle(
		id,
		new String[] {new String(targetConstructor.declaringClass.readableName()), typesAsString(targetConstructor.isVarargs(), targetConstructor.parameters, false)},
		new String[] {new String(targetConstructor.declaringClass.shortReadableName()), typesAsString(targetConstructor.isVarargs(), targetConstructor.parameters, true)},
		ProblemSeverities.Ignore,
		statement.sourceStart,
		statement.sourceEnd);
}
/*
 * Similar implementation than invalidField(FieldReference...)
 * Note that following problem id cannot occur for Javadoc:
 * 	- NonStaticReferenceInStaticContext :
 * 	- NonStaticReferenceInConstructorInvocation :
 * 	- ReceiverTypeNotVisible :
 */
public void javadocInvalidField(int sourceStart, int sourceEnd, Binding fieldBinding, TypeBinding searchedType, int modifiers) {
	int id = IProblem.JavadocUndefinedField;
	switch (fieldBinding.problemId()) {
		case ProblemReasons.NotFound :
			id = IProblem.JavadocUndefinedField;
			break;
		case ProblemReasons.NotVisible :
			id = IProblem.JavadocNotVisibleField;
			break;
		case ProblemReasons.Ambiguous :
			id = IProblem.JavadocAmbiguousField;
			break;
		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}
	int severity = computeSeverity(id);
	if (severity == ProblemSeverities.Ignore) return;
	// report issue
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		String[] arguments = new String[] {new String(fieldBinding.readableName())};
		handle(
			id,
			arguments,
			arguments,
			ProblemSeverities.Ignore,
			sourceStart,
			sourceEnd);
	}
}
/*
 * Similar implementation than invalidMethod(MessageSend...)
 * Note that following problem id cannot occur for Javadoc:
 * 	- NonStaticReferenceInStaticContext :
 * 	- NonStaticReferenceInConstructorInvocation :
 * 	- ReceiverTypeNotVisible :
 */
public void javadocInvalidMethod(MessageSend messageSend, MethodBinding method, int modifiers) {
	if (!javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) return;
	// set problem id
	ProblemMethodBinding problemMethod = null;
	int id = IProblem.JavadocUndefinedMethod; //default...
	switch (method.problemId()) {
		case ProblemReasons.NotFound :
			id = IProblem.JavadocUndefinedMethod;
			problemMethod = (ProblemMethodBinding) method;
			if (problemMethod.closestMatch != null) {
				int severity = computeSeverity(IProblem.JavadocParameterMismatch);
				if (severity == ProblemSeverities.Ignore) return;
				String closestParameterTypeNames = typesAsString(problemMethod.closestMatch.isVarargs(), problemMethod.closestMatch.parameters, false);
				String parameterTypeNames = typesAsString(method.isVarargs(), method.parameters, false);
				String closestParameterTypeShortNames = typesAsString(problemMethod.closestMatch.isVarargs(), problemMethod.closestMatch.parameters, true);
				String parameterTypeShortNames = typesAsString(method.isVarargs(), method.parameters, true);
				if (closestParameterTypeShortNames.equals(parameterTypeShortNames)){
					closestParameterTypeShortNames = closestParameterTypeNames;
					parameterTypeShortNames = parameterTypeNames;
				}
				this.handle(
					IProblem.JavadocParameterMismatch,
					new String[] {
						new String(problemMethod.closestMatch.declaringClass.readableName()),
						new String(problemMethod.closestMatch.selector),
						closestParameterTypeNames,
						parameterTypeNames
					},
					new String[] {
						new String(problemMethod.closestMatch.declaringClass.shortReadableName()),
						new String(problemMethod.closestMatch.selector),
						closestParameterTypeShortNames,
						parameterTypeShortNames
					},
					ProblemSeverities.Ignore,
					(int) (messageSend.nameSourcePosition >>> 32),
					(int) messageSend.nameSourcePosition);
				return;
			}
			break;
		case ProblemReasons.NotVisible :
			id = IProblem.JavadocNotVisibleMethod;
			break;
		case ProblemReasons.Ambiguous :
			id = IProblem.JavadocAmbiguousMethod;
			break;
		case ProblemReasons.NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}
	int severity = computeSeverity(id);
	if (severity == ProblemSeverities.Ignore) return;
	// report issue
	this.handle(
		id,
		new String[] {
			new String(method.declaringClass.readableName()),
			new String(method.selector), typesAsString(method.isVarargs(), method.parameters, false)},
		new String[] {
			new String(method.declaringClass.shortReadableName()),
			new String(method.selector), typesAsString(method.isVarargs(), method.parameters, true)},
			ProblemSeverities.Ignore,
		(int) (messageSend.nameSourcePosition >>> 32),
		(int) messageSend.nameSourcePosition);
}
public void javadocInvalidParamTagName(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocInvalidParamTagName, NoArgument, NoArgument, sourceStart, sourceEnd);
}
public void javadocInvalidReference(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocInvalidSeeReference, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocInvalidSeeReferenceArgs(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocInvalidSeeArgs, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocInvalidSeeUrlReference(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocInvalidSeeHref, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocInvalidTag(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocInvalidTag, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocInvalidThrowsClass(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocInvalidThrowsClass, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocInvalidThrowsClassName(TypeReference typeReference, int modifiers) {
	int severity = computeSeverity(IProblem.JavadocInvalidThrowsClassName);
	if (severity == ProblemSeverities.Ignore) return;
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		String[] arguments = new String[] {String.valueOf(typeReference.resolvedType.sourceName())};
		this.handle(
			IProblem.JavadocInvalidThrowsClassName,
			arguments,
			arguments,
			severity,
			typeReference.sourceStart,
			typeReference.sourceEnd);
	}
}
public void javadocInvalidType(ASTNode location, TypeBinding type, int modifiers) {
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		int id = IProblem.JavadocUndefinedType; // default
		switch (type.problemId()) {
			case ProblemReasons.NotFound :
				id = IProblem.JavadocUndefinedType;
				break;
			case ProblemReasons.NotVisible :
				id = IProblem.JavadocNotVisibleType;
				break;
			case ProblemReasons.Ambiguous :
				id = IProblem.JavadocAmbiguousType;
				break;
			case ProblemReasons.InternalNameProvided :
				id = IProblem.JavadocInternalTypeNameProvided;
				break;
			case ProblemReasons.InheritedNameHidesEnclosingName :
				id = IProblem.JavadocInheritedNameHidesEnclosingTypeName;
				break;
			case ProblemReasons.NonStaticReferenceInStaticContext :
				id = IProblem.JavadocNonStaticTypeFromStaticInvocation;
			    break;
			case ProblemReasons.NoError : // 0
			default :
				needImplementation(); // want to fail to see why we were here...
				break;
		}
		int severity = computeSeverity(id);
		if (severity == ProblemSeverities.Ignore) return;
		this.handle(
			id,
			new String[] {new String(type.readableName())},
			new String[] {new String(type.shortReadableName())},
			ProblemSeverities.Ignore,
			location.sourceStart,
			location.sourceEnd);
	}
}
public void javadocMalformedSeeReference(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocMalformedSeeReference, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocMissing(int sourceStart, int sourceEnd, int modifiers){
	int severity = computeSeverity(IProblem.JavadocMissing);
	if (severity == ProblemSeverities.Ignore) return;
	boolean overriding = (modifiers & (ExtraCompilerModifiers.AccImplementing|ExtraCompilerModifiers.AccOverriding)) != 0;
	boolean report = (this.options.getSeverity(CompilerOptions.MissingJavadocComments) != ProblemSeverities.Ignore)
					&& (!overriding || this.options.reportMissingJavadocCommentsOverriding);
	if (report) {
		String arg = javadocVisibilityArgument(this.options.reportMissingJavadocCommentsVisibility, modifiers);
		if (arg != null) {
			String[] arguments = new String[] { arg };
			this.handle(
				IProblem.JavadocMissing,
				arguments,
				arguments,
				ProblemSeverities.Ignore,
				sourceStart,
				sourceEnd);
		}
	}
}
public void javadocMissingHashCharacter(int sourceStart, int sourceEnd, String ref){
	int severity = computeSeverity(IProblem.JavadocMissingHashCharacter);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] { ref };
	this.handle(
		IProblem.JavadocMissingHashCharacter,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		sourceStart,
		sourceEnd);
}
public void javadocMissingIdentifier(int sourceStart, int sourceEnd, int modifiers){
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers))
		this.handle(IProblem.JavadocMissingIdentifier, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocMissingParamName(int sourceStart, int sourceEnd, int modifiers){
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers))
		this.handle(IProblem.JavadocMissingParamName, NoArgument, NoArgument, sourceStart, sourceEnd);
}
public void javadocMissingParamTag(char[] name, int sourceStart, int sourceEnd, int modifiers) {
	int severity = computeSeverity(IProblem.JavadocMissingParamTag);
	if (severity == ProblemSeverities.Ignore) return;
	boolean overriding = (modifiers & (ExtraCompilerModifiers.AccImplementing|ExtraCompilerModifiers.AccOverriding)) != 0;
	boolean report = (this.options.getSeverity(CompilerOptions.MissingJavadocTags) != ProblemSeverities.Ignore)
					&& (!overriding || this.options.reportMissingJavadocTagsOverriding);
	if (report && javadocVisibility(this.options.reportMissingJavadocTagsVisibility, modifiers)) {
		String[] arguments = new String[] { String.valueOf(name) };
		this.handle(
			IProblem.JavadocMissingParamTag,
			arguments,
			arguments,
			severity,
			sourceStart,
			sourceEnd);
	}
}
public void javadocMissingReference(int sourceStart, int sourceEnd, int modifiers){
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers))
		this.handle(IProblem.JavadocMissingSeeReference, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocMissingReturnTag(int sourceStart, int sourceEnd, int modifiers){
	boolean overriding = (modifiers & (ExtraCompilerModifiers.AccImplementing|ExtraCompilerModifiers.AccOverriding)) != 0;
	boolean report = (this.options.getSeverity(CompilerOptions.MissingJavadocTags) != ProblemSeverities.Ignore)
					&& (!overriding || this.options.reportMissingJavadocTagsOverriding);
	if (report && javadocVisibility(this.options.reportMissingJavadocTagsVisibility, modifiers)) {
		this.handle(IProblem.JavadocMissingReturnTag, NoArgument, NoArgument, sourceStart, sourceEnd);
	}
}
public void javadocMissingThrowsClassName(int sourceStart, int sourceEnd, int modifiers){
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers))
		this.handle(IProblem.JavadocMissingThrowsClassName, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocUndeclaredParamTagName(char[] token, int sourceStart, int sourceEnd, int modifiers) {
	int severity = computeSeverity(IProblem.JavadocInvalidParamName);
	if (severity == ProblemSeverities.Ignore) return;
	if (javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		String[] arguments = new String[] {String.valueOf(token)};
		this.handle(
			IProblem.JavadocInvalidParamName,
			arguments,
			arguments,
			severity,
			sourceStart,
			sourceEnd);
	}
}
public void javadocUnexpectedTag(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocUnexpectedTag, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocUnexpectedText(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocUnexpectedText, NoArgument, NoArgument, ProblemSeverities.Ignore, sourceStart, sourceEnd);
}
public void javadocUnterminatedInlineTag(int sourceStart, int sourceEnd) {
	this.handle(IProblem.JavadocUnterminatedInlineTag, NoArgument, NoArgument, sourceStart, sourceEnd);
}
private boolean javadocVisibility(int visibility, int modifiers) {
	if (modifiers < 0) return true;
	switch (modifiers & ExtraCompilerModifiers.AccVisibilityMASK) {
		case ClassFileConstants.AccPublic :
			return true;
		case ClassFileConstants.AccProtected:
			return (visibility != ClassFileConstants.AccPublic);
		case ClassFileConstants.AccDefault:
			return (visibility == ClassFileConstants.AccDefault || visibility == ClassFileConstants.AccPrivate);
		case ClassFileConstants.AccPrivate:
			return (visibility == ClassFileConstants.AccPrivate);
	}
	return true;
}
private String javadocVisibilityArgument(int visibility, int modifiers) {
	String argument = null;
	switch (modifiers & ExtraCompilerModifiers.AccVisibilityMASK) {
		case ClassFileConstants.AccPublic :
			argument = CompilerOptions.PUBLIC;
			break;
		case ClassFileConstants.AccProtected:
			if (visibility != ClassFileConstants.AccPublic) {
				argument = CompilerOptions.PROTECTED;
			}
			break;
		case ClassFileConstants.AccDefault:
			if (visibility == ClassFileConstants.AccDefault || visibility == ClassFileConstants.AccPrivate) {
				argument = CompilerOptions.DEFAULT;
			}
			break;
		case ClassFileConstants.AccPrivate:
			if (visibility == ClassFileConstants.AccPrivate) {
				argument = CompilerOptions.PRIVATE;
			}
			break;
	}
	return argument;
}
public void localVariableHiding(LocalDeclaration local, Binding hiddenVariable, boolean  isSpecialArgHidingField) {
	if (hiddenVariable instanceof LocalVariableBinding) {
		int id = (local instanceof Argument)
				? IProblem.ArgumentHidingLocalVariable
				: IProblem.LocalVariableHidingLocalVariable;
		int severity = computeSeverity(id);
		if (severity == ProblemSeverities.Ignore) return;
		String[] arguments = new String[] {new String(local.name)  };
		this.handle(
			id,
			arguments,
			arguments,
			severity,
			nodeSourceStart(hiddenVariable, local),
			nodeSourceEnd(hiddenVariable, local));
	} else if (hiddenVariable instanceof FieldBinding) {
		if (isSpecialArgHidingField && !this.options.reportSpecialParameterHidingField){
			return;
		}
		int id = (local instanceof Argument)
				? IProblem.ArgumentHidingField
				: IProblem.LocalVariableHidingField;
		int severity = computeSeverity(id);
		if (severity == ProblemSeverities.Ignore) return;
		FieldBinding field = (FieldBinding) hiddenVariable;
		this.handle(
			id,
			new String[] {new String(local.name) , new String(field.declaringClass.readableName()) },
			new String[] {new String(local.name), new String(field.declaringClass.shortReadableName()) },
			severity,
			local.sourceStart,
			local.sourceEnd);
	}
}
public void localVariableNonNullComparedToNull(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.NonNullLocalVariableComparisonYieldsFalse);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(local.name)  };
	this.handle(
		IProblem.NonNullLocalVariableComparisonYieldsFalse,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}
public void localVariableNullComparedToNonNull(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.NullLocalVariableComparisonYieldsFalse);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(local.name)  };
	this.handle(
		IProblem.NullLocalVariableComparisonYieldsFalse,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}
public void localVariableNullInstanceof(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.NullLocalVariableInstanceofYieldsFalse);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(local.name)  };
	this.handle(
		IProblem.NullLocalVariableInstanceofYieldsFalse,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}
public void localVariableNullReference(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.NullLocalVariableReference);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(local.name)  };
	this.handle(
		IProblem.NullLocalVariableReference,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}
public void localVariablePotentialNullReference(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.PotentialNullLocalVariableReference);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(local.name)};
	this.handle(
		IProblem.PotentialNullLocalVariableReference,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}
public void localVariableRedundantCheckOnNonNull(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.RedundantNullCheckOnNonNullLocalVariable);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(local.name)  };
	this.handle(
		IProblem.RedundantNullCheckOnNonNullLocalVariable,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}
public void localVariableRedundantCheckOnNull(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.RedundantNullCheckOnNullLocalVariable);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(local.name)  };
	this.handle(
		IProblem.RedundantNullCheckOnNullLocalVariable,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}
public void localVariableRedundantNullAssignment(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.RedundantLocalVariableNullAssignment);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(local.name)  };
	this.handle(
		IProblem.RedundantLocalVariableNullAssignment,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}
public void missingReturnType(AbstractMethodDeclaration methodDecl) {
	this.handle(
		IProblem.MissingReturnType,
		NoArgument,
		NoArgument,
		ProblemSeverities.Ignore,
		methodDecl.sourceStart,
		methodDecl.sourceEnd);
}
public void missingSemiColon(Expression expression, int start, int end){
	this.handle(
		IProblem.MissingSemiColon,
		NoArgument,
		NoArgument,
		start,
		end);
}
public void mustUseAStaticMethod(ASTNode messageSend, MethodBinding method) {
	this.handle(
		IProblem.StaticMethodRequested,
		new String[] {new String(method.declaringClass.readableName()), new String(method.selector != null ? method.selector : "".toCharArray()), typesAsString(method.isVarargs(), method.parameters, false)},
		new String[] {new String(method.declaringClass.shortReadableName()), new String(method.selector != null ? method.selector : "".toCharArray()), typesAsString(method.isVarargs(), method.parameters, true)},
		ProblemSeverities.Ignore,
		messageSend.sourceStart,
		messageSend.sourceEnd);
}
public void needImplementation() {
	this.abortDueToInternalError(Messages.abort_missingCode);
}
private int nodeSourceEnd(Binding field, ASTNode node) {
	return nodeSourceEnd(field, node, 0);
}
private int nodeSourceEnd(Binding field, ASTNode node, int index) {
	if (node instanceof ArrayTypeReference) {
		return ((ArrayTypeReference) node).originalSourceEnd;
	} else if (node instanceof QualifiedNameReference) {
		QualifiedNameReference ref = (QualifiedNameReference) node;
		if (ref.binding == field) {
			return (int) (ref.sourcePositions[ref.indexOfFirstFieldBinding-1]);
		}
		FieldBinding[] otherFields = ref.otherBindings;
		if (otherFields != null) {
			int offset = ref.indexOfFirstFieldBinding;
			for (int i = 0, length = otherFields.length; i < length; i++) {
				if (otherFields[i] == field)
					return (int) (ref.sourcePositions[i + offset]);
			}
		}
	} else if (node instanceof ArrayQualifiedTypeReference) {
		ArrayQualifiedTypeReference arrayQualifiedTypeReference = (ArrayQualifiedTypeReference) node;
		int length = arrayQualifiedTypeReference.sourcePositions.length;
		return (int) arrayQualifiedTypeReference.sourcePositions[length - 1];
	}
	return node.sourceEnd;
}
private int nodeSourceStart(Binding field, ASTNode node) {
	if (node instanceof FieldReference) {
		FieldReference fieldReference = (FieldReference) node;
		return (int) (fieldReference.nameSourcePosition >> 32);
	} else 	if (node instanceof QualifiedNameReference) {
		QualifiedNameReference ref = (QualifiedNameReference) node;
		if (ref.binding == field) {
			return (int) (ref.sourcePositions[ref.indexOfFirstFieldBinding-1] >> 32);
		}
		FieldBinding[] otherFields = ref.otherBindings;
		if (otherFields != null) {
			int offset = ref.indexOfFirstFieldBinding;
			for (int i = 0, length = otherFields.length; i < length; i++) {
				if (otherFields[i] == field)
					return (int) (ref.sourcePositions[i + offset] >> 32);
			}
		}
	}
	return node.sourceStart;
}
public void noMoreAvailableSpaceForArgument(LocalVariableBinding local, ASTNode location) {
	String[] arguments = new String[]{ new String(local.name) };
	this.handle(
		IProblem.TooManyArgumentSlots,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}

public void noMoreAvailableSpaceForLocal(LocalVariableBinding local, ASTNode location) {
	String[] arguments = new String[]{ new String(local.name) };
	this.handle(
		IProblem.TooManyLocalVariableSlots,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location));
}

public void nonExternalizedStringLiteral(ASTNode location) {
	this.handle(
		IProblem.NonExternalizedStringLiteral,
		NoArgument,
		NoArgument,
		location.sourceStart,
		location.sourceEnd);
}
public void nonStaticAccessToStaticField(ASTNode location, FieldBinding field) {
	int severity = computeSeverity(IProblem.NonStaticAccessToStaticField);
	if (severity == ProblemSeverities.Ignore) return;
	this.handle(
		IProblem.NonStaticAccessToStaticField,
		new String[] {new String(field.declaringClass.readableName()), new String(field.name)},
		new String[] {new String(field.declaringClass.shortReadableName()), new String(field.name)},
		ProblemSeverities.Ignore,
		nodeSourceStart(field, location),
		nodeSourceEnd(field, location));
}
public void nonStaticAccessToStaticMethod(ASTNode location, MethodBinding method) {
	String methodSelector = method.selector != null ? new String(method.selector) : ""; //$NON-NLS-1$
	this.handle(
		IProblem.NonStaticAccessToStaticMethod,
		new String[] {new String(method.declaringClass.readableName()), methodSelector, typesAsString(method.isVarargs(), method.parameters, false)},
		new String[] {new String(method.declaringClass.shortReadableName()), methodSelector, typesAsString(method.isVarargs(), method.parameters, true)},
		ProblemSeverities.Ignore,
		location.sourceStart,
		location.sourceEnd);
}


public void looseVariableDecleration(ASTNode location, Assignment assignment) {
	String[] arguments = new String[] {assignment.lhs.toString()};
	this.handle(
			IProblem.LooseVarDecl,
			 arguments,
			 arguments,
			assignment.sourceStart,
			assignment.sourceEnd);

}

public void optionalSemicolon(ASTNode location) {
	// Do something else
	System.out.println("Optional Semi"); //$NON-NLS-1$
}
public void notCompatibleTypesError(InstanceOfExpression expression, TypeBinding leftType, TypeBinding rightType) {
	String leftName = new String(leftType.readableName());
	String rightName = new String(rightType.readableName());
	String leftShortName = new String(leftType.shortReadableName());
	String rightShortName = new String(rightType.shortReadableName());
	if (leftShortName.equals(rightShortName)){
		leftShortName = leftName;
		rightShortName = rightName;
	}
	this.handle(
		IProblem.IncompatibleTypesInConditionalOperator,
		new String[] {leftName, rightName },
		new String[] {leftShortName, rightShortName },
		ProblemSeverities.Ignore,
		expression.sourceStart,
		expression.sourceEnd);
}
public void operatorOnlyValidOnNumericType(CompoundAssignment  assignment, TypeBinding leftType, TypeBinding rightType) {
	String leftName = new String(leftType.readableName());
	String rightName = new String(rightType.readableName());
	String leftShortName = new String(leftType.shortReadableName());
	String rightShortName = new String(rightType.shortReadableName());
	if (leftShortName.equals(rightShortName)){
		leftShortName = leftName;
		rightShortName = rightName;
	}
	this.handle(
		IProblem.TypeMismatch,
		new String[] {leftName, rightName },
		new String[] {leftShortName, rightShortName },
		ProblemSeverities.Ignore,
		assignment.sourceStart,
		assignment.sourceEnd);
}
public void overridesDeprecatedMethod(MethodBinding localMethod, MethodBinding inheritedMethod) {
	this.handle(
		IProblem.OverridingDeprecatedMethod,
		new String[] {
			new String(
					CharOperation.concat(
						localMethod.declaringClass.readableName(),
						localMethod.readableName(),
						'.')),
			new String(inheritedMethod.declaringClass.readableName())},
		new String[] {
			new String(
					CharOperation.concat(
						localMethod.declaringClass.shortReadableName(),
						localMethod.shortReadableName(),
						'.')),
			new String(inheritedMethod.declaringClass.shortReadableName())},
			ProblemSeverities.Ignore,
		localMethod.sourceStart(),
		localMethod.sourceEnd());
}
public void parameterAssignment(LocalVariableBinding local, ASTNode location) {
	int severity = computeSeverity(IProblem.ParameterAssignment);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] { new String(local.readableName())};
	this.handle(
		IProblem.ParameterAssignment,
		arguments,
		arguments,
		severity,
		nodeSourceStart(local, location),
		nodeSourceEnd(local, location)); // should never be a qualified name reference
}
public void parseError(
	int startPosition,
	int endPosition,
	int currentToken,
	char[] currentTokenSource,
	String errorTokenName,
	String[] possibleTokens) {

	if (possibleTokens.length == 0) { //no suggestion available
		if (isKeyword(currentToken)) {
			String[] arguments = new String[] {new String(currentTokenSource)};
			this.handle(
				IProblem.ParsingErrorOnKeywordNoSuggestion,
				arguments,
				arguments,
				// this is the current -invalid- token position
				startPosition,
				endPosition);
			return;
		} else {
			String[] arguments = new String[] {errorTokenName};
			this.handle(
				IProblem.ParsingErrorNoSuggestion,
				arguments,
				arguments,
				// this is the current -invalid- token position
				startPosition,
				endPosition);
			return;
		}
	}

	//build a list of probable right tokens
	StringBuffer list = new StringBuffer(20);
	for (int i = 0, max = possibleTokens.length; i < max; i++) {
		if (i > 0)
			list.append(", "); //$NON-NLS-1$
		list.append('"');
		list.append(possibleTokens[i]);
		list.append('"');
	}

	if (isKeyword(currentToken)) {
		String[] arguments = new String[] {new String(currentTokenSource), list.toString()};
		this.handle(
			IProblem.ParsingErrorOnKeyword,
			arguments,
			arguments,
			// this is the current -invalid- token position
			startPosition,
			endPosition);
		return;
	}
	//extract the literal when it's a literal
	if (isLiteral(currentToken) ||
		isIdentifier(currentToken)) {
			errorTokenName = new String(currentTokenSource);
	}

	String[] arguments = new String[] {errorTokenName, list.toString()};
	this.handle(
		IProblem.ParsingError,
		arguments,
		arguments,
		// this is the current -invalid- token position
		startPosition,
		endPosition);
}
public void parseErrorDeleteToken(
	int start,
	int end,
	int currentKind,
	char[] errorTokenSource,
	String errorTokenName){
	this.syntaxError(
		IProblem.ParsingErrorDeleteToken,
		start,
		end,
		currentKind,
		errorTokenSource,
		errorTokenName,
		null);
}

public void parseErrorDeleteTokens(
	int start,
	int end){
	this.handle(
		IProblem.ParsingErrorDeleteTokens,
		NoArgument,
		NoArgument,
		start,
		end);
}
public void parseErrorInsertAfterToken(
	int start,
	int end,
	int currentKind,
	char[] errorTokenSource,
	String errorTokenName,
	String expectedToken){
	this.syntaxError(
		IProblem.ParsingErrorInsertTokenAfter,
		start,
		end,
		currentKind,
		errorTokenSource,
		errorTokenName,
		expectedToken);
}
public void parseErrorInsertBeforeToken(
	int start,
	int end,
	int currentKind,
	char[] errorTokenSource,
	String errorTokenName,
	String expectedToken){
	this.syntaxError(
		IProblem.ParsingErrorInsertTokenBefore,
		start,
		end,
		currentKind,
		errorTokenSource,
		errorTokenName,
		expectedToken);
}
public void parseErrorInsertToComplete(
	int start,
	int end,
	String inserted,
	String completed){
	String[] arguments = new String[] {inserted, completed};
	if (";".equals(inserted))	// ignore missing semicolon error //$NON-NLS-1$
		return;
	this.handle(
		IProblem.ParsingErrorInsertToComplete,
		arguments,
		arguments,
		start,
		end);
}

public void parseErrorInsertToCompletePhrase(
	int start,
	int end,
	String inserted){
	String[] arguments = new String[] {inserted};
	this.handle(
		IProblem.ParsingErrorInsertToCompletePhrase,
		arguments,
		arguments,
		start,
		end);
}
public void parseErrorInsertToCompleteScope(
	int start,
	int end,
	String inserted){
	String[] arguments = new String[] {inserted};
	this.handle(
		IProblem.ParsingErrorInsertToCompleteScope,
		arguments,
		arguments,
		start,
		end);
}
public void parseErrorInvalidToken(
	int start,
	int end,
	int currentKind,
	char[] errorTokenSource,
	String errorTokenName,
	String expectedToken){
	this.syntaxError(
		IProblem.ParsingErrorInvalidToken,
		start,
		end,
		currentKind,
		errorTokenSource,
		errorTokenName,
		expectedToken);
}
public void parseErrorMergeTokens(
	int start,
	int end,
	String expectedToken){
	String[] arguments = new String[] {expectedToken};
	this.handle(
		IProblem.ParsingErrorMergeTokens,
		arguments,
		arguments,
		start,
		end);
}
public void parseErrorMisplacedConstruct(
	int start,
	int end){
	this.handle(
		IProblem.ParsingErrorMisplacedConstruct,
		NoArgument,
		NoArgument,
		start,
		end);
}
public void parseErrorNoSuggestion(
	int start,
	int end,
	int currentKind,
	char[] errorTokenSource,
	String errorTokenName){
	this.syntaxError(
		IProblem.ParsingErrorNoSuggestion,
		start,
		end,
		currentKind,
		errorTokenSource,
		errorTokenName,
		null);
}
public void parseErrorNoSuggestionForTokens(
	int start,
	int end){
	this.handle(
		IProblem.ParsingErrorNoSuggestionForTokens,
		NoArgument,
		NoArgument,
		start,
		end);
}
public void parseErrorReplaceToken(
	int start,
	int end,
	int currentKind,
	char[] errorTokenSource,
	String errorTokenName,
	String expectedToken){
	this.syntaxError(
		IProblem.ParsingError,
		start,
		end,
		currentKind,
		errorTokenSource,
		errorTokenName,
		expectedToken);
}
public void parseErrorReplaceTokens(
	int start,
	int end,
	String expectedToken){
	String[] arguments = new String[] {expectedToken};
	this.handle(
		IProblem.ParsingErrorReplaceTokens,
		arguments,
		arguments,
		start,
		end);
}
public void parseErrorUnexpectedEnd(
	int start,
	int end){

	String[] arguments;
	if(this.referenceContext instanceof ConstructorDeclaration) {
		arguments = new String[] {Messages.parser_endOfConstructor};
	} else if(this.referenceContext instanceof MethodDeclaration) {
		arguments = new String[] {Messages.parser_endOfMethod};
	} else if(this.referenceContext instanceof TypeDeclaration) {
		arguments = new String[] {Messages.parser_endOfInitializer};
	} else {
		arguments = new String[] {Messages.parser_endOfFile};
	}
	this.handle(
		IProblem.ParsingErrorUnexpectedEOF,
		arguments,
		arguments,
		start,
		end);
}
public void possibleAccidentalBooleanAssignment(Assignment assignment) {
	this.handle(
		IProblem.PossibleAccidentalBooleanAssignment,
		NoArgument,
		NoArgument,
		ProblemSeverities.Ignore,
		assignment.sourceStart,
		assignment.sourceEnd);
}
public void possibleFallThroughCase(CaseStatement caseStatement) {
	// as long as we consider fake reachable as reachable, better keep 'possible' in the name
	this.handle(
		IProblem.FallthroughCase,
		NoArgument,
		NoArgument,
		caseStatement.sourceStart,
		caseStatement.sourceEnd);
}
public void redefineArgument(Argument arg) {
	String[] arguments = new String[] {new String(arg.name)};
	this.handle(
		IProblem.RedefinedArgument,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		arg.sourceStart,
		arg.sourceEnd);
}
public void redefineLocal(LocalDeclaration localDecl) {
	String[] arguments = new String[] {new String(localDecl.name)};
	this.handle(
		IProblem.RedefinedLocal,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		localDecl.sourceStart,
		localDecl.sourceEnd);
}
public void reset() {
	this.positionScanner = null;
}
private int retrieveEndingPositionAfterOpeningParenthesis(int sourceStart, int sourceEnd, int numberOfParen) {
	if (this.referenceContext == null) return sourceEnd;
	CompilationResult compilationResult = this.referenceContext.compilationResult();
	if (compilationResult == null) return sourceEnd;
	ICompilationUnit compilationUnit = compilationResult.getCompilationUnit();
	if (compilationUnit == null) return sourceEnd;
	char[] contents = compilationUnit.getContents();
	if (contents.length == 0) return sourceEnd;
	if (this.positionScanner == null) {
		this.positionScanner = new Scanner(false, false, false, this.options.sourceLevel, this.options.complianceLevel, null, null, false);
	}
	this.positionScanner.setSource(contents);
	this.positionScanner.resetTo(sourceStart, sourceEnd);
	try {
		int token;
		int previousSourceEnd = sourceEnd;
		while ((token = this.positionScanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
			switch(token) {
				case TerminalTokens.TokenNameRPAREN:
					return previousSourceEnd;
				default :
					previousSourceEnd = this.positionScanner.currentPosition - 1;
			}
		}
	} catch(InvalidInputException e) {
		// ignore
	}
	return sourceEnd;
}
private int retrieveStartingPositionAfterOpeningParenthesis(int sourceStart, int sourceEnd, int numberOfParen) {
	if (this.referenceContext == null) return sourceStart;
	CompilationResult compilationResult = this.referenceContext.compilationResult();
	if (compilationResult == null) return sourceStart;
	ICompilationUnit compilationUnit = compilationResult.getCompilationUnit();
	if (compilationUnit == null) return sourceStart;
	char[] contents = compilationUnit.getContents();
	if (contents.length == 0) return sourceStart;
	if (this.positionScanner == null) {
		this.positionScanner = new Scanner(false, false, false, this.options.sourceLevel, this.options.complianceLevel, null, null, false);
	}
	this.positionScanner.setSource(contents);
	this.positionScanner.resetTo(sourceStart, sourceEnd);
	int count = 0;
	try {
		int token;
		while ((token = this.positionScanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
			switch(token) {
				case TerminalTokens.TokenNameLPAREN:
					count++;
					if (count == numberOfParen) {
						this.positionScanner.getNextToken();
						return this.positionScanner.startPosition;
					}
			}
		}
	} catch(InvalidInputException e) {
		// ignore
	}
	return sourceStart;
}
public void scannerError(Parser parser, String errorTokenName) {
	Scanner scanner = parser.scanner;

	int flag = IProblem.ParsingErrorNoSuggestion;
	int startPos = scanner.startPosition;
	int endPos = scanner.currentPosition - 1;

	//special treatment for recognized errors....
	if (errorTokenName.equals(Scanner.END_OF_SOURCE))
		flag = IProblem.EndOfSource;
	else if (errorTokenName.equals(Scanner.INVALID_HEXA))
		flag = IProblem.InvalidHexa;
	else if (errorTokenName.equals(Scanner.INVALID_OCTAL))
		flag = IProblem.InvalidOctal;
	else if (errorTokenName.equals(Scanner.INVALID_CHARACTER_CONSTANT))
		flag = IProblem.InvalidCharacterConstant;
	else if (errorTokenName.equals(Scanner.INVALID_ESCAPE))
		flag = IProblem.InvalidEscape;
	else if (errorTokenName.equals(Scanner.INVALID_UNICODE_ESCAPE)){
		flag = IProblem.InvalidUnicodeEscape;
		// better locate the error message
		char[] source = scanner.source;
		int checkPos = scanner.currentPosition - 1;
		if (checkPos >= source.length) checkPos = source.length - 1;
		while (checkPos >= startPos){
			if (source[checkPos] == '\\') break;
			checkPos --;
		}
		startPos = checkPos;
	} else if (errorTokenName.equals(Scanner.INVALID_LOW_SURROGATE)) {
		flag = IProblem.InvalidLowSurrogate;
	} else if (errorTokenName.equals(Scanner.INVALID_HIGH_SURROGATE)) {
		flag = IProblem.InvalidHighSurrogate;
		// better locate the error message
		char[] source = scanner.source;
		int checkPos = scanner.startPosition + 1;
		while (checkPos <= endPos){
			if (source[checkPos] == '\\') break;
			checkPos ++;
		}
		endPos = checkPos - 1;
	} else if (errorTokenName.equals(Scanner.INVALID_FLOAT))
		flag = IProblem.InvalidFloat;
	else if (errorTokenName.equals(Scanner.UNTERMINATED_STRING))
		flag = IProblem.UnterminatedString;
	else if (errorTokenName.equals(Scanner.UNTERMINATED_COMMENT))
		flag = IProblem.UnterminatedComment;
	else if (errorTokenName.equals(Scanner.INVALID_CHAR_IN_STRING))
		flag = IProblem.UnterminatedString;
	else if (errorTokenName.equals(Scanner.INVALID_DIGIT))
		flag = IProblem.InvalidDigit;

	String[] arguments = flag == IProblem.ParsingErrorNoSuggestion
			? new String[] {errorTokenName}
			: NoArgument;
	this.handle(
		flag,
		arguments,
		arguments,
		// this is the current -invalid- token position
		startPos,
		endPos,
		parser.compilationUnit.compilationResult);
}
public void shouldReturn(TypeBinding returnType, ASTNode location) {
	this.handle(
		IProblem.ShouldReturnValue,
		new String[] { new String (returnType.readableName())},
		new String[] { new String (returnType.shortReadableName())},
		ProblemSeverities.Ignore,
		location.sourceStart,
		location.sourceEnd);
}
public void staticAndInstanceConflict(MethodBinding currentMethod, MethodBinding inheritedMethod) {
	if (currentMethod.isStatic())
		this.handle(
			// This static method cannot hide the instance method from %1
			// 8.4.6.4 - If a class inherits more than one method with the same signature a static (non-abstract) method cannot hide an instance method.
			IProblem.CannotHideAnInstanceMethodWithAStaticMethod,
			new String[] {new String(inheritedMethod.declaringClass.readableName())},
			new String[] {new String(inheritedMethod.declaringClass.shortReadableName())},
			ProblemSeverities.Ignore,
			currentMethod.sourceStart(),
			currentMethod.sourceEnd());
	else
		this.handle(
			// This instance method cannot override the static method from %1
			// 8.4.6.4 - If a class inherits more than one method with the same signature an instance (non-abstract) method cannot override a static method.
			IProblem.CannotOverrideAStaticMethodWithAnInstanceMethod,
			new String[] {new String(inheritedMethod.declaringClass.readableName())},
			new String[] {new String(inheritedMethod.declaringClass.shortReadableName())},
			currentMethod.sourceStart(),
			currentMethod.sourceEnd());
}
public void staticFieldAccessToNonStaticVariable(ASTNode location, FieldBinding field) {
	String[] arguments = new String[] {new String(field.readableName())};
	this.handle(
		IProblem.NonStaticFieldFromStaticInvocation,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(field,location),
		nodeSourceEnd(field, location));
}
public void superfluousSemicolon(int sourceStart, int sourceEnd) {
	this.handle(
		IProblem.SuperfluousSemicolon,
		NoArgument,
		NoArgument,
		sourceStart,
		sourceEnd);
}
private void syntaxError(
	int id,
	int startPosition,
	int endPosition,
	int currentKind,
	char[] currentTokenSource,
	String errorTokenName,
	String expectedToken) {

	String eTokenName;
	if (isKeyword(currentKind) ||
		isLiteral(currentKind) ||
		isIdentifier(currentKind)) {
			eTokenName = new String(currentTokenSource);
	} else {
		eTokenName = errorTokenName;
	}

	String[] arguments;
	if(expectedToken != null) {
		arguments = new String[] {eTokenName, expectedToken};
	} else {
		arguments = new String[] {eTokenName};
	}
	this.handle(
		id,
		arguments,
		arguments,
		(isKeyword(currentKind) && !this.options.strictOnKeywordUsage) ? ProblemSeverities.Ignore : computeSeverity(id),
		startPosition,
		endPosition);
}
public void task(String tag, String message, String priority, int start, int end){
	this.handle(
		IProblem.Task,
		new String[] { tag, message, priority/*secret argument that is not surfaced in getMessage()*/},
		new String[] { tag, message, priority/*secret argument that is not surfaced in getMessage()*/},
		start,
		end);
}
public void typeMismatchError(TypeBinding actualType, TypeBinding expectedType, ASTNode location) {
	this.handle(
		IProblem.TypeMismatch,
		new String[] {new String(actualType.readableName()), new String(expectedType.readableName())},
		new String[] {new String(actualType.shortReadableName()), new String(expectedType.shortReadableName())},
		location.sourceStart,
		location.sourceEnd);
}
private String typesAsString(boolean isVarargs, TypeBinding[] types, boolean makeShort) {
	StringBuffer buffer = new StringBuffer(10);
	for (int i = 0, length = types.length; i < length; i++) {
		if (i != 0)
			buffer.append(", "); //$NON-NLS-1$
		TypeBinding type = types[i];
		boolean isVarargType = isVarargs && i == length-1;
		if (isVarargType) type = ((ArrayBinding)type).elementsType();
		buffer.append(new String(makeShort ? type.shortReadableName() : type.readableName()));
		if (isVarargType) buffer.append("..."); //$NON-NLS-1$
	}
	return buffer.toString();
}
public void undefinedLabel(BranchStatement statement) {
	if (isRecoveredName(statement.label)) return;
	String[] arguments = new String[] {new String(statement.label)};
	this.handle(
		IProblem.UndefinedLabel,
		arguments,
		arguments,
		statement.sourceStart,
		statement.sourceEnd);
}
public void undocumentedEmptyBlock(int blockStart, int blockEnd) {
	this.handle(
		IProblem.UndocumentedEmptyBlock,
		NoArgument,
		NoArgument,
		blockStart,
		blockEnd);
}
public void uninitializedLocalVariable(LocalVariableBinding binding, ASTNode location) {
	int severity = computeSeverity(IProblem.UninitializedLocalVariable);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(binding.readableName())};
	this.handle(
		IProblem.UninitializedLocalVariable,
		arguments,
		arguments,
		severity,
		nodeSourceStart(binding, location),
		nodeSourceEnd(binding, location));
}
public void uninitializedGlobalVariable(LocalVariableBinding binding, ASTNode location) {
	int severity = computeSeverity(IProblem.UninitializedGlobalVariable);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(binding.readableName())};
	this.handle(
		IProblem.UninitializedGlobalVariable,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nodeSourceStart(binding, location),
		nodeSourceEnd(binding, location));
}
public void unnecessaryElse(ASTNode location) {
	this.handle(
		IProblem.UnnecessaryElse,
		NoArgument,
		NoArgument,
		location.sourceStart,
		location.sourceEnd);
}
public void unnecessaryNLSTags(int sourceStart, int sourceEnd) {
	this.handle(
		IProblem.UnnecessaryNLSTag,
		NoArgument,
		NoArgument,
		sourceStart,
		sourceEnd);
}
public void unreachableCode(Statement statement) {
	int sourceStart = statement.sourceStart;
	int sourceEnd = statement.sourceEnd;
	if (statement instanceof LocalDeclaration) {
		LocalDeclaration declaration = (LocalDeclaration) statement;
		sourceStart = declaration.declarationSourceStart;
		sourceEnd = declaration.declarationSourceEnd;
	} else if (statement instanceof Expression) {
		int statemendEnd = ((Expression) statement).statementEnd;
		if (statemendEnd != -1) sourceEnd = statemendEnd;
	}
	this.handle(
		IProblem.CodeCannotBeReached,
		NoArgument,
		NoArgument,
		ProblemSeverities.Ignore,
		sourceStart,
		sourceEnd);
}
public void unresolvableReference(NameReference nameRef, Binding binding) {
/* also need to check that the searchedType is the receiver type
	if (binding instanceof ProblemBinding) {
		ProblemBinding problem = (ProblemBinding) binding;
		if (problem.searchType != null && problem.searchType.isHierarchyInconsistent())
			severity = SecondaryError;
	}
*/
	String[] arguments = new String[] {new String(binding.readableName())};
	int end = nameRef.sourceEnd;
	if (nameRef instanceof QualifiedNameReference) {
		QualifiedNameReference ref = (QualifiedNameReference) nameRef;
		if (isRecoveredName(ref.tokens)) return;
		if (ref.indexOfFirstFieldBinding >= 1)
			end = (int) ref.sourcePositions[ref.indexOfFirstFieldBinding - 1];
	} else {
		SingleNameReference ref = (SingleNameReference) nameRef;
		if (isRecoveredName(ref.token)) return;
	}
	this.handle(
		IProblem.UndefinedName,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		nameRef.sourceStart,
		end);
}
public void unusedArgument(LocalDeclaration localDecl) {
	int severity = computeSeverity(IProblem.ArgumentIsNeverUsed);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(localDecl.name)};
	this.handle(
		IProblem.ArgumentIsNeverUsed,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		localDecl.sourceStart,
		localDecl.sourceEnd);
}
public void unusedLabel(LabeledStatement statement) {
	int severity = computeSeverity(IProblem.UnusedLabel);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(statement.label)};
	this.handle(
		IProblem.UnusedLabel,
		arguments,
		arguments,
		ProblemSeverities.Ignore,
		statement.sourceStart,
		statement.labelEnd);
}
public void unusedLocalVariable(LocalDeclaration localDecl) {
	int severity = computeSeverity(IProblem.LocalVariableIsNeverUsed);
	if (severity == ProblemSeverities.Ignore) return;
	String[] arguments = new String[] {new String(localDecl.name)};
	this.handle(
		IProblem.LocalVariableIsNeverUsed,
		arguments,
		arguments,
		severity,
		localDecl.sourceStart,
		localDecl.sourceEnd);
}
public void unusedPrivateField(FieldDeclaration fieldDecl) {

	int severity = computeSeverity(IProblem.UnusedPrivateField);
	if (severity == ProblemSeverities.Ignore) return;

	FieldBinding field = fieldDecl.binding;

	this.handle(
			IProblem.UnusedPrivateField,
		new String[] {
			new String(field.declaringClass.readableName()),
			new String(field.name),
		 },
		new String[] {
			new String(field.declaringClass.shortReadableName()),
			new String(field.name),
		 },
		 ProblemSeverities.Ignore,
		nodeSourceStart(field, fieldDecl),
		nodeSourceEnd(field, fieldDecl));
}
public void unusedPrivateMethod(AbstractMethodDeclaration methodDecl) {

	int severity = computeSeverity(IProblem.UnusedPrivateMethod);
	if (severity == ProblemSeverities.Ignore) return;

	MethodBinding method = methodDecl.getBinding();
	char[] methodSelector = method.selector;
	if(methodSelector == null)
		methodSelector = methodDecl.getName();

	// no report for serialization support 'Object readResolve()'
	if (!method.isStatic()
			&& TypeIds.T_JavaLangObject == method.returnType.id
			&& method.parameters.length == 0
			&& CharOperation.equals(methodSelector, TypeConstants.READRESOLVE)) {
		return;
	}
	// no report for serialization support 'Object writeReplace()'
	if (!method.isStatic()
			&& TypeIds.T_JavaLangObject == method.returnType.id
			&& method.parameters.length == 0
			&& CharOperation.equals(methodSelector, TypeConstants.WRITEREPLACE)) {
		return;
	}
	this.handle(
			IProblem.UnusedPrivateMethod,
		new String[] {
			new String(method.declaringClass.readableName()),
			new String(methodSelector),
			typesAsString(method.isVarargs(), method.parameters, false)
		 },
		new String[] {
			new String(method.declaringClass.shortReadableName()),
			new String(methodSelector),
			typesAsString(method.isVarargs(), method.parameters, true)
		 },
		 ProblemSeverities.Ignore,
		methodDecl.sourceStart,
		methodDecl.sourceEnd);
}
public void unusedPrivateType(TypeDeclaration typeDecl) {
	int severity = computeSeverity(IProblem.UnusedPrivateType);
	if (severity == ProblemSeverities.Ignore) return;

	ReferenceBinding type = typeDecl.binding;
	this.handle(
			IProblem.UnusedPrivateType,
		new String[] {
			new String(type.readableName()),
		 },
		new String[] {
			new String(type.shortReadableName()),
		 },
		 ProblemSeverities.Ignore,
		typeDecl.sourceStart,
		typeDecl.sourceEnd);
}
public void visibilityConflict(MethodBinding currentMethod, MethodBinding inheritedMethod) {
	this.handle(
		//	Cannot reduce the visibility of the inherited method from %1
		// 8.4.6.3 - The access modifier of an hiding method must provide at least as much access as the hidden method.
		// 8.4.6.3 - The access modifier of an overiding method must provide at least as much access as the overriden method.
		IProblem.MethodReducesVisibility,
		new String[] {new String(inheritedMethod.declaringClass.readableName())},
		new String[] {new String(inheritedMethod.declaringClass.shortReadableName())},
		ProblemSeverities.Ignore,
		currentMethod.sourceStart(),
		currentMethod.sourceEnd());
}
}
