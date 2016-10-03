/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;
import org.eclipse.wst.jsdt.ui.text.java.IQuickFixProcessor;

/**
  */
public class QuickFixProcessor implements IQuickFixProcessor {


	public boolean hasCorrections(IJavaScriptUnit cu, int problemId) {
		switch (problemId) {
			case IProblem.UnterminatedString:
//			case IProblem.UnusedImport:
//			case IProblem.DuplicateImport:
//			case IProblem.CannotImportPackage:
//			case IProblem.ConflictingImport:
//			case IProblem.ImportNotFound:
//			case IProblem.UndefinedMethod:
			case IProblem.UndefinedConstructor:
			case IProblem.ParameterMismatch:
			case IProblem.MethodButWithConstructorName:
			case IProblem.UndefinedField:
			case IProblem.UndefinedName:
			//case IProblem.PublicClassMustMatchFileName:
			//case IProblem.PackageIsNotExpectedPackage:
			case IProblem.UndefinedType:
			case IProblem.TypeMismatch:
			case IProblem.UnhandledException:
			case IProblem.UnreachableCatch:
			case IProblem.VoidMethodReturnsValue:
			case IProblem.ShouldReturnValue:
			case IProblem.MissingReturnType:
			//case IProblem.NonExternalizedStringLiteral:
			case IProblem.NonStaticAccessToStaticField:
			case IProblem.NonStaticAccessToStaticMethod:
			case IProblem.StaticMethodRequested:
			case IProblem.NonStaticFieldFromStaticInvocation:
			case IProblem.InstanceMethodDuringConstructorInvocation:
			case IProblem.InstanceFieldDuringConstructorInvocation:
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleType:
			case IProblem.NotVisibleField:
			//case IProblem.BodyForAbstractMethod:
			//case IProblem.AbstractMethodInAbstractClass:
			//case IProblem.AbstractMethodMustBeImplemented:
			case IProblem.BodyForNativeMethod:
			//case IProblem.OuterLocalMustBeFinal:
			case IProblem.UninitializedLocalVariable:
			case IProblem.UndefinedConstructorInDefaultConstructor:
			case IProblem.UnhandledExceptionInDefaultConstructor:
			//case IProblem.NotVisibleConstructorInDefaultConstructor:
			case IProblem.AmbiguousType:
			case IProblem.UnusedPrivateMethod:
			//case IProblem.UnusedPrivateConstructor:
			case IProblem.UnusedPrivateField:
			case IProblem.UnusedPrivateType:
			case IProblem.LocalVariableIsNeverUsed:
			case IProblem.ArgumentIsNeverUsed:
			case IProblem.MethodRequiresBody:
			case IProblem.NeedToEmulateFieldReadAccess:
			case IProblem.NeedToEmulateFieldWriteAccess:
			case IProblem.NeedToEmulateMethodAccess:
			case IProblem.NeedToEmulateConstructorAccess:
			case IProblem.SuperfluousSemicolon:
			case IProblem.UnnecessaryInstanceof:
			case IProblem.IndirectAccessToStaticField:
			case IProblem.IndirectAccessToStaticMethod:
			case IProblem.Task:
			case IProblem.UnusedMethodDeclaredThrownException:
			case IProblem.UnusedConstructorDeclaredThrownException:
			case IProblem.UnqualifiedFieldAccess:
			case IProblem.JavadocMissing:
			case IProblem.JavadocMissingParamTag:
			case IProblem.JavadocMissingReturnTag:
			case IProblem.JavadocMissingThrowsTag:
			case IProblem.JavadocUndefinedType:
			case IProblem.JavadocAmbiguousType:
			case IProblem.JavadocNotVisibleType:
			case IProblem.JavadocInvalidThrowsClassName:
			case IProblem.JavadocDuplicateThrowsClassName:
			case IProblem.JavadocDuplicateReturnTag:
			case IProblem.JavadocDuplicateParamName:
			case IProblem.JavadocInvalidParamName:
			case IProblem.JavadocUnexpectedTag:
			case IProblem.JavadocInvalidTag:
			//case IProblem.NonBlankFinalLocalAssignment:
			case IProblem.DuplicateFinalLocalInitialization:
			case IProblem.FinalFieldAssignment:
			case IProblem.DuplicateBlankFinalFieldInitialization:
			case IProblem.InheritedMethodReducesVisibility:
			case IProblem.MethodReducesVisibility:
			//case IProblem.OverridingNonVisibleMethod:
			case IProblem.CannotOverrideAStaticMethodWithAnInstanceMethod:
			case IProblem.CannotHideAnInstanceMethodWithAStaticMethod:
			case IProblem.UnexpectedStaticModifierForMethod:
			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
			case IProblem.DuplicateField:
			case IProblem.DuplicateMethod:
			case IProblem.DuplicateNestedType:
			case IProblem.IllegalModifierForClass:
			case IProblem.IllegalModifierForMemberClass:
			case IProblem.IllegalModifierForLocalClass:
			case IProblem.IllegalModifierForField:
			case IProblem.IllegalModifierForMethod:
			case IProblem.UnexpectedStaticModifierForField:
			case IProblem.IllegalModifierCombinationFinalVolatileForField:
			case IProblem.IncompatibleReturnType:
			case IProblem.IncompatibleExceptionInThrowsClause:
			case IProblem.NoMessageSendOnArrayType:
			case IProblem.InvalidOperator:
			case IProblem.UnnecessaryElse:
			case IProblem.SuperclassMustBeAClass:
			case IProblem.UseAssertAsAnIdentifier:
			case IProblem.UseEnumAsAnIdentifier:
			case IProblem.RedefinedLocal:
			case IProblem.RedefinedArgument:
			case IProblem.CodeCannotBeReached:
			case IProblem.InvalidUsageOfForeachStatements:
			case IProblem.OverridingDeprecatedMethod:
			case IProblem.IsClassPathCorrect:
			case IProblem.MethodReturnsVoid:
			case IProblem.ForbiddenReference:
			case IProblem.DiscouragedReference:
			case IProblem.UnnecessaryNLSTag:
			case IProblem.AssignmentHasNoEffect:
			case IProblem.FallthroughCase:
				return true;
			default:
//				if (JavaModelUtil.is50OrHigher(cu.getJavaScriptProject())) {
//					return SuppressWarningsSubProcessor.hasSuppressWarningsProposal(problemId);
//				}
				return false;
		}
	}

	private static int moveBack(int offset, int start, String ignoreCharacters, IJavaScriptUnit cu) {
		try {
			IBuffer buf= cu.getBuffer();
			while (offset >= start) {
				if (ignoreCharacters.indexOf(buf.getChar(offset - 1)) == -1) { 
					return offset;
				}
				offset--;
			}
		} catch(JavaScriptModelException e) {
		}
		return start;
	}


	/* (non-Javadoc)
	 * @see IAssistProcessor#getCorrections(org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistContext, org.eclipse.wst.jsdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		if (locations == null || locations.length == 0) {
			return null;
		}

		HashSet handledProblems= new HashSet(locations.length);
		ArrayList resultingCollections= new ArrayList();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation curr= locations[i];
			Integer id= Integer.valueOf(curr.getProblemId());
			if (handledProblems.add(id)) {
				process(context, curr, resultingCollections);
			}
		}
		return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

	private void process(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		int id= problem.getProblemId();
		if (id == 0) { // no proposals for none-problem locations
			return;
		}
		switch (id) {
			case IProblem.UnterminatedString:
				String quoteLabel= CorrectionMessages.JavaCorrectionProcessor_addquote_description;
				int pos= moveBack(problem.getOffset() + problem.getLength(), problem.getOffset(), "\n\r", context.getCompilationUnit()); //$NON-NLS-1$
				proposals.add(new ReplaceCorrectionProposal(quoteLabel, context.getCompilationUnit(), pos, 0, "\"", 0)); //$NON-NLS-1$
				break;
			case IProblem.DuplicateImport:
			case IProblem.CannotImportPackage:
			case IProblem.ConflictingImport:
				ReorgCorrectionsSubProcessor.removeImportStatementProposals(context, problem, proposals);
				break;
			case IProblem.ImportNotFound:
				ReorgCorrectionsSubProcessor.importNotFoundProposals(context, problem, proposals);
				ReorgCorrectionsSubProcessor.removeImportStatementProposals(context, problem, proposals);
				break;
			case IProblem.UndefinedMethod:
			case IProblem.UndefinedFunction:
				UnresolvedElementsSubProcessor.getMethodProposals(context, problem, false, proposals);
				break;
			case IProblem.UndefinedConstructor:
				UnresolvedElementsSubProcessor.getConstructorProposals(context, problem, proposals);
				break;
			case IProblem.ParameterMismatch:
				UnresolvedElementsSubProcessor.getMethodProposals(context, problem, true, proposals);
				break;
			case IProblem.MethodButWithConstructorName:
				ReturnTypeSubProcessor.addMethodWithConstrNameProposals(context, problem, proposals);
				break;
			case IProblem.UndefinedField:
			case IProblem.UndefinedName:
				UnresolvedElementsSubProcessor.getVariableProposals(context, problem, null, proposals);
				break;
			case IProblem.AmbiguousType:
			case IProblem.JavadocAmbiguousType:
				UnresolvedElementsSubProcessor.getAmbiguosTypeReferenceProposals(context, problem, proposals);
				break;
			case IProblem.UndefinedType:
			case IProblem.JavadocUndefinedType:
				UnresolvedElementsSubProcessor.getTypeProposals(context, problem, proposals);
				break;
			case IProblem.TypeMismatch:
				TypeMismatchSubProcessor.addTypeMismatchProposals(context, problem, proposals);
				break;
			case IProblem.IncompatibleReturnType:
				TypeMismatchSubProcessor.addIncompatibleReturnTypeProposals(context, problem, proposals);
				break;
			case IProblem.IncompatibleExceptionInThrowsClause:
				TypeMismatchSubProcessor.addIncompatibleThrowsProposals(context, problem, proposals);
				break;
			case IProblem.UnhandledException:
				LocalCorrectionsSubProcessor.addUncaughtExceptionProposals(context, problem, proposals);
				break;
			case IProblem.UnreachableCatch:
				LocalCorrectionsSubProcessor.addUnreachableCatchProposals(context, problem, proposals);
				break;
			case IProblem.VoidMethodReturnsValue:
				ReturnTypeSubProcessor.addVoidMethodReturnsProposals(context, problem, proposals);
				break;
			case IProblem.MethodReturnsVoid:
				ReturnTypeSubProcessor.addMethodRetunsVoidProposals(context, problem, proposals);
				break;
			case IProblem.MissingReturnType:
				ReturnTypeSubProcessor.addMissingReturnTypeProposals(context, problem, proposals);
				break;
			case IProblem.ShouldReturnValue:
				ReturnTypeSubProcessor.addMissingReturnStatementProposals(context, problem, proposals);
				break;
			case IProblem.NonExternalizedStringLiteral:
				LocalCorrectionsSubProcessor.addNLSProposals(context, problem, proposals);
				break;
			case IProblem.UnnecessaryNLSTag:
				LocalCorrectionsSubProcessor.getUnnecessaryNLSTagProposals(context, problem, proposals);
				break;
			case IProblem.NonStaticAccessToStaticField:
			case IProblem.NonStaticAccessToStaticMethod:
			case IProblem.IndirectAccessToStaticField:
			case IProblem.IndirectAccessToStaticMethod:
				LocalCorrectionsSubProcessor.addCorrectAccessToStaticProposals(context, problem, proposals);
				break;
			case IProblem.StaticMethodRequested:
			case IProblem.NonStaticFieldFromStaticInvocation:
			case IProblem.InstanceMethodDuringConstructorInvocation:
			case IProblem.InstanceFieldDuringConstructorInvocation:
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_STATIC, 5);
				break;
			case IProblem.NonBlankFinalLocalAssignment:
			case IProblem.DuplicateFinalLocalInitialization:
			case IProblem.FinalFieldAssignment:
			case IProblem.DuplicateBlankFinalFieldInitialization:
			case IProblem.ClassExtendFinalClass:
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_FINAL, 9);
				break;
			case IProblem.InheritedMethodReducesVisibility:
			case IProblem.MethodReducesVisibility:
			case IProblem.OverridingNonVisibleMethod:
				ModifierCorrectionSubProcessor.addChangeOverriddenModfierProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_VISIBLE);
				break;
			case IProblem.CannotOverrideAStaticMethodWithAnInstanceMethod:
				ModifierCorrectionSubProcessor.addChangeOverriddenModfierProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_STATIC);
				break;
			case IProblem.CannotHideAnInstanceMethodWithAStaticMethod:
			case IProblem.IllegalModifierForClass:
			case IProblem.UnexpectedStaticModifierForField:
			case IProblem.IllegalModifierCombinationFinalVolatileForField:
			case IProblem.IllegalModifierForMemberClass:
			case IProblem.IllegalModifierForLocalClass:
			case IProblem.IllegalModifierForField:
			case IProblem.IllegalModifierForMethod:
			case IProblem.UnexpectedStaticModifierForMethod:
				ModifierCorrectionSubProcessor.addRemoveInvalidModfiersProposal(context, problem, proposals, 5);
				break;
			case IProblem.NotVisibleField:
				ModifierCorrectionSubProcessor.addGetterSetterProposal(context,problem,proposals, 15);
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_VISIBLE, 10);
				break;
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleType:
			case IProblem.JavadocNotVisibleType:
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_VISIBLE, 10);
				break;
			case IProblem.BodyForAbstractMethod:
			case IProblem.AbstractMethodInAbstractClass:
				ModifierCorrectionSubProcessor.addAbstractMethodProposals(context, problem, proposals);
				break;
			case IProblem.AbstractMethodMustBeImplemented:
				LocalCorrectionsSubProcessor.addUnimplementedMethodsProposals(context, problem, proposals);
				break;			
			case IProblem.BodyForNativeMethod:
				ModifierCorrectionSubProcessor.addNativeMethodProposals(context, problem, proposals);
				break;
			case IProblem.MethodRequiresBody:
				ModifierCorrectionSubProcessor.addMethodRequiresBodyProposals(context, problem, proposals);
				break;
			case IProblem.OuterLocalMustBeFinal:
				ModifierCorrectionSubProcessor.addNonFinalLocalProposal(context, problem, proposals);
				break;
			case IProblem.UninitializedLocalVariable:
				LocalCorrectionsSubProcessor.addUninitializedLocalVariableProposal(context, problem, proposals);
				break;
			case IProblem.UnhandledExceptionInDefaultConstructor:
			case IProblem.UndefinedConstructorInDefaultConstructor:
			case IProblem.NotVisibleConstructorInDefaultConstructor:
				LocalCorrectionsSubProcessor.addConstructorFromSuperclassProposal(context, problem, proposals);
				break;
			case IProblem.UnusedPrivateMethod:
			case IProblem.UnusedPrivateConstructor:
			case IProblem.UnusedPrivateField:
			case IProblem.UnusedPrivateType:
			case IProblem.LocalVariableIsNeverUsed:
			case IProblem.ArgumentIsNeverUsed:
				LocalCorrectionsSubProcessor.addUnusedMemberProposal(context, problem, proposals);
				break;
			case IProblem.NeedToEmulateFieldReadAccess:
			case IProblem.NeedToEmulateFieldWriteAccess:
			case IProblem.NeedToEmulateMethodAccess:
			case IProblem.NeedToEmulateConstructorAccess:
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_PRIVATE, 5);
				break;
			case IProblem.SuperfluousSemicolon:
				LocalCorrectionsSubProcessor.addSuperfluousSemicolonProposal(context, problem, proposals);
				break;
			case IProblem.UnnecessaryInstanceof:
				LocalCorrectionsSubProcessor.addUnnecessaryInstanceofProposal(context, problem, proposals);
				break;
			case IProblem.UnusedMethodDeclaredThrownException:
			case IProblem.UnusedConstructorDeclaredThrownException:
				LocalCorrectionsSubProcessor.addUnnecessaryThrownExceptionProposal(context, problem, proposals);
				break;
			case IProblem.UnqualifiedFieldAccess:
				ModifierCorrectionSubProcessor.addGetterSetterProposal(context, problem, proposals, 15);
//				LocalCorrectionsSubProcessor.addUnqualifiedFieldAccessProposal(context, problem, proposals);
				break;
			case IProblem.Task:
				proposals.add(new TaskMarkerProposal(context.getCompilationUnit(), problem, 10));
				break;
			case IProblem.JavadocMissing:
				JavadocTagsSubProcessor.getMissingJavadocCommentProposals(context, problem, proposals);
				break;
			case IProblem.JavadocMissingParamTag:
			case IProblem.JavadocMissingReturnTag:
			case IProblem.JavadocMissingThrowsTag:
				JavadocTagsSubProcessor.getMissingJavadocTagProposals(context, problem, proposals);
				break;
			case IProblem.JavadocInvalidThrowsClassName:
			case IProblem.JavadocDuplicateThrowsClassName:
			case IProblem.JavadocDuplicateReturnTag:
			case IProblem.JavadocDuplicateParamName:
			case IProblem.JavadocInvalidParamName:
			case IProblem.JavadocUnexpectedTag:
			case IProblem.JavadocInvalidTag:
				JavadocTagsSubProcessor.getRemoveJavadocTagProposals(context, problem, proposals);
				break;
			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
			case IProblem.UseAssertAsAnIdentifier:
			case IProblem.UseEnumAsAnIdentifier:
			case IProblem.RedefinedLocal:
			case IProblem.RedefinedArgument:
			case IProblem.DuplicateField:
			case IProblem.DuplicateMethod:
			case IProblem.DuplicateNestedType:
				LocalCorrectionsSubProcessor.addInvalidVariableNameProposals(context, problem, proposals);
				break;
			case IProblem.NoMessageSendOnArrayType:
				UnresolvedElementsSubProcessor.getArrayAccessProposals(context, problem, proposals);
				break;
			case IProblem.InvalidOperator:
				LocalCorrectionsSubProcessor.getInvalidOperatorProposals(context, problem, proposals);
				break;
			case IProblem.UnnecessaryElse:
				LocalCorrectionsSubProcessor.getUnnecessaryElseProposals(context, problem, proposals);
				break;
			case IProblem.CodeCannotBeReached:
				LocalCorrectionsSubProcessor.getUnreachableCodeProposals(context, problem, proposals);
				break;
			case IProblem.InvalidUsageOfForeachStatements:
				ReorgCorrectionsSubProcessor.getNeed50ComplianceProposals(context, problem, proposals);
				break;
			case IProblem.OverridingDeprecatedMethod:
				ModifierCorrectionSubProcessor.addOverridingDeprecatedMethodProposal(context, problem, proposals);
				break;
			case IProblem.IsClassPathCorrect:
				ReorgCorrectionsSubProcessor.getIncorrectBuildPathProposals(context, problem, proposals);
				break;
			case IProblem.ForbiddenReference:
			case IProblem.DiscouragedReference:
				ReorgCorrectionsSubProcessor.getAccessRulesProposals(context, problem, proposals);
				break;
			case IProblem.AssignmentHasNoEffect:
				LocalCorrectionsSubProcessor.getAssignmentHasNoEffectProposals(context, problem, proposals);
				break;
			case IProblem.FallthroughCase:
				LocalCorrectionsSubProcessor.addFallThroughProposals(context, problem, proposals);
				break;
			default:
		}
//		if (JavaModelUtil.is50OrHigher(context.getCompilationUnit().getJavaScriptProject())) {
//			SuppressWarningsSubProcessor.addSuppressWarningsProposals(context, problem, proposals);
//		}
	}
}
