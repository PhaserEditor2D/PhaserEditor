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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RippleMethodFinder2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * 
 * This refactoring creates a wrapper around a certain method and redirects callers of the original
 * method to the newly created method (called "intermediary method").
 *  
 * If invoked on a method call, the user may select whether to only update the selected call or
 * all other calls as well. If invoked on a method declaration, the user may select whether to 
 * update calls at all. An intermediary method will be created in both cases.
 *  
 * Creating indirections is possible for both source and binary methods. Select an invocation of the method or
 * the declaring method itself, for example in the outline view.
 *  
 * Note that in case of methods inside generic types, the parameters of the declaring type of the selected method
 * will be added to the method definition, rendering it generic as well.
 *  
 * If any of the calls cannot see the intermediary method due to visibility problems with enclosing types
 * of the intermediary method, visibility will be adjusted. If the intermediary method is not able to
 * see the target method, this refactoring will try to adjust the visibility of the target method and
 * enclosing types as well. However, the latter is only possible if the target method is from source.
 * 
 * 
 * 
 */
public class IntroduceIndirectionRefactoring extends ScriptableRefactoring {

	/**
	 * The compilation unit in which the user invoked this refactoring (if any)
	 */
	private IJavaScriptUnit fSelectionCompilationUnit;
	/**
	 * The class file (with source) in which the user invoked this refactoring (if any)
	 */
	private IClassFile fSelectionClassFile;
	/**
	 * The start of the user selection inside the selected
	 * compilation unit (if any)
	 */
	private int fSelectionStart;
	/**
	 * The length of the user selection inside the selected
	 * compilation unit (if any)
	 */
	private int fSelectionLength;
	/**
	 * The selected FunctionInvocation (if any). This field is used
	 * to update this particular invocation in non-reference mode.
	 */
	private FunctionInvocation fSelectionMethodInvocation;

	// Intermediary information:

	/**
	 * The class in which to place the intermediary method
	 */
	private IType fIntermediaryClass;
	/**
	 * The binding of the intermediary class
	 */
	private ITypeBinding fIntermediaryClassBinding;
	/**
	 * The name of the intermediary method
	 */
	private String fIntermediaryMethodName;
	/**
	 * The type for the additional parameter for the intermediary. This
	 * type is determined from all known references.
	 */
	private ITypeBinding fIntermediaryFirstParameterType;

	// Target information:

	/**
	 * The originally selected target method (i.e., the one to be encapsulated)
	 */
	private IFunction fTargetMethod;
	/**
	 * The binding of the originally selected target method
	 */
	private IFunctionBinding fTargetMethodBinding;

	// Other information:

	/**
	 * If true, all references to the target method are replaced with calls to
	 * the intermediary.
	 */
	private boolean fUpdateReferences;
	/**
	 * CompilationUnitRewrites for all affected cus
	 */
	private Map/* <IJavaScriptUnit,CompilationUnitRewrite> */fRewrites;
	/**
	 * Text change manager (actually a CompilationUnitChange manager) which
	 * manages all changes.
	 */
	private TextChangeManager fTextChangeManager;
	
	// Visibility
	
	/**
	 * The visibility adjustor
	 */
	private MemberVisibilityAdjustor fAdjustor;
	/**
	 * Visibility adjustments for the intermediary
	 */
	private Map/*IMember, IVisibilityAdjustment*/ fIntermediaryAdjustments;


	private class NoOverrideProgressMonitor extends SubProgressMonitor {

		public NoOverrideProgressMonitor(IProgressMonitor monitor, int ticks) {
			super(monitor, ticks, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
		}

		public void setTaskName(String name) {
			// do nothing
		}
	}

	// ********* CONSTRUCTORS AND CLASS CREATION ************

	public IntroduceIndirectionRefactoring(IJavaScriptUnit unit, int offset, int length) {
		fSelectionCompilationUnit= unit;
		initialize(offset, length);
	}

	public IntroduceIndirectionRefactoring(IClassFile file, int offset, int length) {
		fSelectionClassFile= file;
		initialize(offset, length);
	}

	public IntroduceIndirectionRefactoring(IFunction method) {
		fTargetMethod= method;
		initialize(0, 0);
	}

	private void initialize(int offset, int length) {
		fSelectionStart= offset;
		fSelectionLength= length;
		fUpdateReferences= true;
	}

	// ********* UI INTERACTION AND STARTUP OPTIONS ************

	public String getName() {
		return RefactoringCoreMessages.IntroduceIndirectionRefactoring_introduce_indirection_name;
	}

	public IJavaScriptProject getProject() {
		if (fSelectionCompilationUnit != null)
			return fSelectionCompilationUnit.getJavaScriptProject();
		if (fSelectionClassFile != null)
			return fSelectionClassFile.getJavaScriptProject();
		if (fTargetMethod != null)
			return fTargetMethod.getJavaScriptProject();
		return null;
	}

	public IPackageFragment getInvocationPackage() {
		return fSelectionCompilationUnit != null ? (IPackageFragment) fSelectionCompilationUnit.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT) : null;
	}

	public boolean canEnableUpdateReferences() {
		return true;
	}

	public void setEnableUpdateReferences(boolean updateReferences) {
		fUpdateReferences= updateReferences;
	}

	public RefactoringStatus setIntermediaryMethodName(String newMethodName) {
		Assert.isNotNull(newMethodName);
		fIntermediaryMethodName= newMethodName;
		RefactoringStatus stat= Checks.checkMethodName(newMethodName);
		stat.merge(checkOverloading());
		return stat;
	}

	private RefactoringStatus checkOverloading() {
		try {
			if (fIntermediaryClass != null) {
				IFunction[] toCheck= fIntermediaryClass.getFunctions();
				for (int i= 0; i < toCheck.length; i++) {
					IFunction method= toCheck[i];
					if (method.getElementName().equals(fIntermediaryMethodName))
						return RefactoringStatus.createWarningStatus(Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_duplicate_method_name_in_declaring_class_error,
								fIntermediaryMethodName));
				}
			}
		} catch (JavaScriptModelException e) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_could_not_parse_declaring_class_error);
		}
		return new RefactoringStatus();
	}

	public String getIntermediaryMethodName() {
		return fIntermediaryMethodName;
	}

	/**
	 * @param fullyQualifiedTypeName
	 * @return status for type name. Use {@link #setIntermediaryMethodName(String)} to check for overridden methods.
	 */
	public RefactoringStatus setIntermediaryClassName(String fullyQualifiedTypeName) {
		IType target= null;

		try {
			if (fullyQualifiedTypeName.length() == 0)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_class_not_selected_error);
			
			// find type (now including secondaries)
			target= getProject().findType(fullyQualifiedTypeName, new NullProgressMonitor());
			if (target == null || !target.exists())
				return RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_class_does_not_exist_error, fullyQualifiedTypeName));
		} catch (JavaScriptModelException e) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_unable_determine_declaring_type);
		}

		if (target.isReadOnly())
			return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_cannot_create_in_readonly);

		if (target.isBinary())
			return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_cannot_create_in_binary);

		fIntermediaryClass= target;

		return new RefactoringStatus();
	}

	/**
	 * Returns the class name of the intermediary class, or the empty string if none has been set yet.
	 * @return the intermediary class name or the empty string
	 */
	public String getIntermediaryClassName() {
		return fIntermediaryClass != null ? JavaModelUtil.getFullyQualifiedName(fIntermediaryClass) : ""; //$NON-NLS-1$
	}

	// ********** CONDITION CHECKING **********

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			pm.beginTask(RefactoringCoreMessages.IntroduceIndirectionRefactoring_checking_activation, 1);
			fRewrites= new HashMap();

			// This refactoring has been invoked on 
			// (1) a TextSelection inside an IJavaScriptUnit or inside an IClassFile (definitely with source), or 
			// (2) an IFunction inside a IJavaScriptUnit or inside an IClassFile (with or without source)

			if (fTargetMethod == null) {
				// (1) invoked on a text selection

				if (fSelectionStart == 0)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_not_available_on_this_selection);

				// if a text selection exists, source is available.
				JavaScriptUnit selectionCURoot;
				ASTNode selectionNode;
				if (fSelectionCompilationUnit != null) {
					// compilation unit - could use CuRewrite later on
					selectionCURoot= getCachedCURewrite(fSelectionCompilationUnit).getRoot();
					selectionNode= getSelectedNode(fSelectionCompilationUnit, selectionCURoot, fSelectionStart, fSelectionLength);
				} else {
					// binary class file - no cu rewrite
					ASTParser parser= ASTParser.newParser(AST.JLS3);
					parser.setResolveBindings(true);
					parser.setSource(fSelectionClassFile);
					selectionCURoot= (JavaScriptUnit) parser.createAST(null);
					selectionNode= getSelectedNode(null, selectionCURoot, fSelectionStart, fSelectionLength);
				}

				if (selectionNode == null)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_not_available_on_this_selection);

				IFunctionBinding targetMethodBinding= null;

				if (selectionNode.getNodeType() == ASTNode.FUNCTION_INVOCATION) {
					targetMethodBinding= ((FunctionInvocation) selectionNode).resolveMethodBinding();
				} else if (selectionNode.getNodeType() == ASTNode.FUNCTION_DECLARATION) {
					targetMethodBinding= ((FunctionDeclaration) selectionNode).resolveBinding();
				} else if (selectionNode.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
					// Allow invocation on super methods calls. makes sense as other
					// calls or even only the declaration can be updated.
					targetMethodBinding= ((SuperMethodInvocation) selectionNode).resolveMethodBinding();
				} else {
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_not_available_on_this_selection);
				}
				fTargetMethodBinding= targetMethodBinding.getMethodDeclaration(); // resolve generics
				fTargetMethod= (IFunction) fTargetMethodBinding.getJavaElement();

				//allow single updating mode if an invocation was selected and the invocation can be updated
				if (selectionNode instanceof FunctionInvocation && fSelectionCompilationUnit != null)
					fSelectionMethodInvocation= (FunctionInvocation) selectionNode;

			} else {
				// (2) invoked on an IFunction: Source may not be available

//				if (fTargetMethod.getDeclaringType().isAnnotation())
//					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_not_available_on_annotation);

				if (fTargetMethod.getJavaScriptUnit() != null) {
					// source method
					JavaScriptUnit selectionCURoot= getCachedCURewrite(fTargetMethod.getJavaScriptUnit()).getRoot();
					FunctionDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fTargetMethod, selectionCURoot);
					fTargetMethodBinding= declaration.resolveBinding().getMethodDeclaration();
				} else {
					// binary method - no CURewrite available (and none needed as we cannot update the method anyway)
					ASTParser parser= ASTParser.newParser(AST.JLS3);
					parser.setProject(fTargetMethod.getJavaScriptProject());
					IBinding[] bindings= parser.createBindings(new IJavaScriptElement[] { fTargetMethod }, null);
					fTargetMethodBinding= ((IFunctionBinding) bindings[0]).getMethodDeclaration();
				}
			}

			if (fTargetMethod == null || fTargetMethodBinding == null || (!RefactoringAvailabilityTester.isIntroduceIndirectionAvailable(fTargetMethod)))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_not_available_on_this_selection);

//			if (fTargetMethod.getDeclaringType().isLocal() || fTargetMethod.getDeclaringType().isAnonymous())
//				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_not_available_for_local_or_anonymous_types);

			if (fTargetMethod.isConstructor())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_not_available_for_constructors);

			if (fIntermediaryMethodName == null)
				fIntermediaryMethodName= fTargetMethod.getElementName();

			if (fIntermediaryClass == null) {
				if (fSelectionCompilationUnit != null && !fSelectionCompilationUnit.isReadOnly())
					fIntermediaryClass= getEnclosingInitialSelectionMember().getDeclaringType();
				else if (!fTargetMethod.isBinary() && !fTargetMethod.isReadOnly())
					fIntermediaryClass= fTargetMethod.getDeclaringType();
			}

			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		RefactoringStatus result= new RefactoringStatus();
		fTextChangeManager= new TextChangeManager();
		fIntermediaryFirstParameterType= null;
		fIntermediaryClassBinding= null;
		for (Iterator iter= fRewrites.values().iterator(); iter.hasNext();)
			((CompilationUnitRewrite) iter.next()).clearASTAndImportRewrites();

		int startupTicks= 5;
		int hierarchyTicks= 5;
		int visibilityTicks= 5;
		int referenceTicks= fUpdateReferences ? 30 : 5;
		int creationTicks= 5;

		pm.beginTask("", startupTicks + hierarchyTicks + visibilityTicks + referenceTicks + creationTicks); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.IntroduceIndirectionRefactoring_checking_conditions);

		result.merge(Checks.checkMethodName(fIntermediaryMethodName));
		if (result.hasFatalError())
			return result;
		
		if (fIntermediaryClass == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_cannot_run_without_intermediary_type);

		// intermediary class is already non binary/non-enum/non-interface.
		CompilationUnitRewrite imRewrite= getCachedCURewrite(fIntermediaryClass.getJavaScriptUnit());
		fIntermediaryClassBinding= typeToBinding(fIntermediaryClass, imRewrite.getRoot());

		fAdjustor= new MemberVisibilityAdjustor(fIntermediaryClass, fIntermediaryClass);
		fIntermediaryAdjustments= new HashMap();
		
		// check static method in non-static nested type
		if (fIntermediaryClassBinding.isNested() && !Modifier.isStatic(fIntermediaryClassBinding.getModifiers()))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceIndirectionRefactoring_cannot_create_in_nested_nonstatic, JavaStatusContext.create(fIntermediaryClass));

		pm.worked(startupTicks);
		if (pm.isCanceled())
			throw new OperationCanceledException();

		if (fUpdateReferences) {
			pm.setTaskName(RefactoringCoreMessages.IntroduceIndirectionRefactoring_checking_conditions + " " + RefactoringCoreMessages.IntroduceIndirectionRefactoring_looking_for_references); //$NON-NLS-1$
			result.merge(updateReferences(new NoOverrideProgressMonitor(pm, referenceTicks)));
			pm.setTaskName(RefactoringCoreMessages.IntroduceIndirectionRefactoring_checking_conditions);
		} else {
			// only update the declaration and/or a selected method invocation
			if (fSelectionMethodInvocation != null) {
				fIntermediaryFirstParameterType= getExpressionType(fSelectionMethodInvocation);
				final IMember enclosing= getEnclosingInitialSelectionMember();
				// create an edit for this particular call
				result.merge(updateMethodInvocation(fSelectionMethodInvocation, enclosing, getCachedCURewrite(fSelectionCompilationUnit)));

				if (!isRewriteKept(fSelectionCompilationUnit))
					createChangeAndDiscardRewrite(fSelectionCompilationUnit);

				// does call see the intermediary method?
				// => increase visibility of the type of the intermediary method.
				result.merge(adjustVisibility(fIntermediaryClass, enclosing.getDeclaringType(), new NoOverrideProgressMonitor(pm, 0)));
			}
			pm.worked(referenceTicks);
		}

		if (pm.isCanceled())
			throw new OperationCanceledException();

		if (fIntermediaryFirstParameterType == null)
			fIntermediaryFirstParameterType= fTargetMethodBinding.getDeclaringClass();

		// The target type and method may have changed - update them

		IType actualTargetType= (IType) fIntermediaryFirstParameterType.getJavaElement();
		if (fTargetMethod.getDeclaringType()==null || !fTargetMethod.getDeclaringType().equals(actualTargetType)) {
			IFunction actualTargetMethod= new MethodOverrideTester(actualTargetType, actualTargetType.newSupertypeHierarchy(null)).findOverriddenMethodInHierarchy(actualTargetType, fTargetMethod);
			fTargetMethod= actualTargetMethod;
			fTargetMethodBinding= findMethodBindingInHierarchy(fIntermediaryFirstParameterType, actualTargetMethod);
			Assert.isNotNull(fTargetMethodBinding);
		}

		result.merge(checkCanCreateIntermediaryMethod());
		createIntermediaryMethod();
		pm.worked(creationTicks);

		pm.setTaskName(RefactoringCoreMessages.IntroduceIndirectionRefactoring_checking_conditions + " " + RefactoringCoreMessages.IntroduceIndirectionRefactoring_adjusting_visibility); //$NON-NLS-1$
		result.merge(updateTargetVisibility(new NoOverrideProgressMonitor(pm, 0)));
		result.merge(updateIntermediaryVisibility(new NoOverrideProgressMonitor(pm, 0)));
		pm.worked(visibilityTicks);
		pm.setTaskName(RefactoringCoreMessages.IntroduceIndirectionRefactoring_checking_conditions);

		createChangeAndDiscardRewrite(fIntermediaryClass.getJavaScriptUnit());

		result.merge(Checks.validateModifiesFiles(getAllFilesToModify(), getValidationContext()));
		pm.done();

		return result;
	}

	private RefactoringStatus updateTargetVisibility(IProgressMonitor monitor) throws JavaScriptModelException, CoreException {

		RefactoringStatus result= new RefactoringStatus();

		// Adjust the visibility of the method and of the referenced type. Note that
		// the target method may not be in the target type; and in this case, the type
		// of the target method does not need a visibility adjustment.

		// This method is called after all other changes have been 
		// created. Changes induced by this method will be attached to those changes.

		result.merge(adjustVisibility((IType) fIntermediaryFirstParameterType.getJavaElement(), fIntermediaryClass, monitor));
		if (result.hasError())
			return result; // binary

		ModifierKeyword neededVisibility= getNeededVisibility(fTargetMethod, fIntermediaryClass);
		if (neededVisibility != null) {

			result.merge(adjustVisibility(fTargetMethod, neededVisibility,  monitor));
			if (result.hasError())
				return result; // binary

			if (fTargetMethod.getDeclaringType()!=null) {
				// Need to adjust the overridden methods of the target method.
				ITypeHierarchy hierarchy = fTargetMethod.getDeclaringType()
						.newTypeHierarchy(null);
				MethodOverrideTester tester = new MethodOverrideTester(
						fTargetMethod.getDeclaringType(), hierarchy);
				IType[] subtypes = hierarchy.getAllSubtypes(fTargetMethod
						.getDeclaringType());
				for (int i = 0; i < subtypes.length; i++) {
					IFunction method = tester.findOverridingMethodInType(
							subtypes[i], fTargetMethod);
					if (method != null && method.exists()) {
					result.merge(adjustVisibility(method, neededVisibility, monitor));
					if (monitor.isCanceled())
						throw new OperationCanceledException();
					
						if (result.hasError())
							return result; // binary
					}
				}
			}			
		}

		return result;
	}
	
	private RefactoringStatus updateIntermediaryVisibility(NoOverrideProgressMonitor monitor) throws JavaScriptModelException {
		return rewriteVisibility(fIntermediaryAdjustments, fRewrites, monitor);
	}

	private RefactoringStatus updateReferences(IProgressMonitor monitor) throws CoreException {

		RefactoringStatus result= new RefactoringStatus();

		monitor.beginTask("", 90); //$NON-NLS-1$

		if (monitor.isCanceled())
			throw new OperationCanceledException();

		IFunction[] ripple= RippleMethodFinder2.getRelatedMethods(fTargetMethod, false, new NoOverrideProgressMonitor(monitor, 10), null);

		if (monitor.isCanceled())
			throw new OperationCanceledException();

		SearchResultGroup[] references= Checks.excludeCompilationUnits(getReferences(ripple, new NoOverrideProgressMonitor(monitor, 10), result), result);

		if (result.hasFatalError())
			return result;

		result.merge(Checks.checkCompileErrorsInAffectedFiles(references));

		if (monitor.isCanceled())
			throw new OperationCanceledException();

		int ticksPerCU= references.length == 0 ? 0 : 70 / references.length;

		for (int i= 0; i < references.length; i++) {
			SearchResultGroup group= references[i];
			SearchMatch[] searchResults= group.getSearchResults();
			CompilationUnitRewrite currentCURewrite= getCachedCURewrite(group.getCompilationUnit());

			for (int j= 0; j < searchResults.length; j++) {

				SearchMatch match= searchResults[j];
				if (match.isInsideDocComment())
					continue;

				IMember enclosingMember= (IMember) match.getElement();
				ASTNode target= getSelectedNode(group.getCompilationUnit(), currentCURewrite.getRoot(), match.getOffset(), match.getLength());

				if (target instanceof SuperMethodInvocation) {
					// Cannot retarget calls to super - add a warning
					result.merge(createWarningAboutCall(enclosingMember, target, RefactoringCoreMessages.IntroduceIndirectionRefactoring_call_warning_super_keyword));
					continue;
				}

				Assert.isTrue(target instanceof FunctionInvocation, "Element of call should be a FunctionInvocation."); //$NON-NLS-1$

				FunctionInvocation invocation= (FunctionInvocation) target;
				ITypeBinding typeBinding= getExpressionType(invocation);

				if (fIntermediaryFirstParameterType == null) {
					// no highest type yet
					fIntermediaryFirstParameterType= typeBinding.getTypeDeclaration();
				} else {
					// check if current type is higher
					result.merge(findCommonParent(typeBinding.getTypeDeclaration()));
				}

				if (result.hasFatalError())
					return result;

				// create an edit for this particular call
				result.merge(updateMethodInvocation(invocation, enclosingMember, currentCURewrite));

				// does call see the intermediary method?
				// => increase visibility of the type of the intermediary method.
				result.merge(adjustVisibility(fIntermediaryClass, enclosingMember.getDeclaringType(), new NoOverrideProgressMonitor(monitor, 0)));

				if (monitor.isCanceled())
					throw new OperationCanceledException();
			}

			if (!isRewriteKept(group.getCompilationUnit()))
				createChangeAndDiscardRewrite(group.getCompilationUnit());

			monitor.worked(ticksPerCU);
		}

		monitor.done();
		return result;
	}

	private RefactoringStatus findCommonParent(ITypeBinding typeBinding) throws JavaScriptModelException {

		RefactoringStatus status= new RefactoringStatus();

		ITypeBinding highest= fIntermediaryFirstParameterType;
		ITypeBinding current= typeBinding;

		if (current.equals(highest) || Bindings.isSuperType(highest, current))
			// current is the same as highest or highest is already a supertype of current in the same hierarchy => no change
			return status;

		// find lowest common supertype with the method
		// search in bottom-up order 
		ITypeBinding[] currentAndSupers= getTypeAndAllSuperTypes(current);
		ITypeBinding[] highestAndSupers= getTypeAndAllSuperTypes(highest);

		ITypeBinding foundBinding= null;
		for (int i1= 0; i1 < currentAndSupers.length; i1++) {
			for (int i2= 0; i2 < highestAndSupers.length; i2++) {
				if (highestAndSupers[i2].isEqualTo(currentAndSupers[i1])
						&& (Bindings.findMethodInHierarchy(highestAndSupers[i2], fTargetMethodBinding.getName(), fTargetMethodBinding.getParameterTypes()) != null)) {
					foundBinding= highestAndSupers[i2];
					break;
				}
			}
			if (foundBinding != null)
				break;
		}

		if (foundBinding != null) {
			fIntermediaryFirstParameterType= foundBinding;
		} else {
			String type1= fIntermediaryFirstParameterType.getQualifiedName();
			String type2= current.getQualifiedName();
			status.addFatalError(Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_open_hierarchy_error, new String[] { type1, type2 }));
		}

		return status;
	}

	// ******************** CHANGE CREATION ***********************

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		final Map arguments= new HashMap();
		String project= null;
		IJavaScriptProject javaProject= fTargetMethod.getJavaScriptProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
		final IType declaring= fTargetMethod.getDeclaringType();
		try {
			if (declaring.isLocal() || declaring.isAnonymous())
				flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		} catch (JavaScriptModelException exception) {
			JavaScriptPlugin.log(exception);
		}
		final String description= Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_descriptor_description_short, fTargetMethod.getElementName());
		final String header= Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_descriptor_description, new String[] { JavaScriptElementLabels.getTextLabel(fTargetMethod, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(declaring, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_original_pattern, JavaScriptElementLabels.getTextLabel(fTargetMethod, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
		comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_method_pattern, fIntermediaryMethodName));
		comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_declaring_pattern, JavaScriptElementLabels.getTextLabel(fIntermediaryClass, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
		if (fUpdateReferences)
			comment.addSetting(RefactoringCoreMessages.JavaRefactoringDescriptor_update_references);
		final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.INTRODUCE_INDIRECTION, project, description, comment.asString(), arguments, flags);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fTargetMethod));
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fIntermediaryMethodName);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1, descriptor.elementToHandle(fIntermediaryClass));
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
		return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.IntroduceIndirectionRefactoring_introduce_indirection, fTextChangeManager.getAllChanges());
	}

	// ******************* CREATE INTERMEDIARY **********************

	/**
	 * Checks whether the target method can be created. Note that this
	 * can only be done after fDelegateParameterType has been initialized.
	 * @return resulting status
	 * @throws JavaScriptModelException 
	 */
	private RefactoringStatus checkCanCreateIntermediaryMethod() throws JavaScriptModelException {
		// check if method already exists:
		List parameterBindings= new ArrayList();
		if (!isStaticTarget())
			parameterBindings.add(fIntermediaryFirstParameterType);
		parameterBindings.addAll(Arrays.asList(fTargetMethodBinding.getParameterTypes()));
		return Checks.checkMethodInType(fIntermediaryClassBinding, fIntermediaryMethodName, (ITypeBinding[]) parameterBindings.toArray(new ITypeBinding[parameterBindings.size()]));
	}

	private void createIntermediaryMethod() throws CoreException {

		CompilationUnitRewrite imRewrite= getCachedCURewrite(fIntermediaryClass.getJavaScriptUnit());
		AST ast= imRewrite.getAST();
		FunctionDeclaration intermediary= ast.newFunctionDeclaration();

		// Name
		intermediary.setName(ast.newSimpleName(fIntermediaryMethodName));

		// Flags
		List modifiers= intermediary.modifiers();
		modifiers.add(imRewrite.getAST().newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		modifiers.add(imRewrite.getAST().newModifier(ModifierKeyword.STATIC_KEYWORD));

		// Parameters
		String targetParameterName= StubUtility.suggestArgumentName(getProject(), fIntermediaryFirstParameterType.getName(), fTargetMethod.getParameterNames());

		if (!isStaticTarget()) {
			// Add first param
			SingleVariableDeclaration parameter= imRewrite.getAST().newSingleVariableDeclaration();
			Type t= imRewrite.getImportRewrite().addImport(fIntermediaryFirstParameterType, imRewrite.getAST());
			parameter.setType(t);
			parameter.setName(imRewrite.getAST().newSimpleName(targetParameterName));
			intermediary.parameters().add(parameter);
		}
		// Add other params
		copyArguments(intermediary, imRewrite);

		// Return type
		intermediary.setReturnType2(imRewrite.getImportRewrite().addImport(fTargetMethodBinding.getReturnType(), ast));

		// Body
		FunctionInvocation invocation= imRewrite.getAST().newFunctionInvocation();
		invocation.setName(imRewrite.getAST().newSimpleName(fTargetMethod.getElementName()));
		if (isStaticTarget()) {
			Type type= imRewrite.getImportRewrite().addImport(fTargetMethodBinding.getDeclaringClass(), ast);
			invocation.setExpression(ASTNodeFactory.newName(ast, ASTNodes.asString(type)));
		} else {
			invocation.setExpression(imRewrite.getAST().newSimpleName(targetParameterName));
		}
		copyInvocationParameters(invocation, ast);
		Statement call= encapsulateInvocation(intermediary, invocation);

		final Block body= imRewrite.getAST().newBlock();
		body.statements().add(call);
		intermediary.setBody(body);

		// method comment
		IJavaScriptUnit targetCU= imRewrite.getCu();
		if (StubUtility.doAddComments(targetCU.getJavaScriptProject())) {
			String comment= CodeGeneration.getMethodComment(targetCU, getIntermediaryClassName(), intermediary, null, StubUtility.getLineDelimiterUsed(targetCU));
			if (comment != null) {
				JSdoc javadoc= (JSdoc) imRewrite.getASTRewrite().createStringPlaceholder(comment, ASTNode.JSDOC);
				intermediary.setJavadoc(javadoc);
			}
		}

		// Add the completed method to the intermediary type:

		// Intermediary class is non-anonymous
		AbstractTypeDeclaration type= (AbstractTypeDeclaration) typeToDeclaration(fIntermediaryClass, imRewrite.getRoot());
		ChildListPropertyDescriptor typeBodyDeclarationsProperty= typeToBodyDeclarationProperty(fIntermediaryClass, imRewrite.getRoot());

		ListRewrite bodyDeclarationsListRewrite= imRewrite.getASTRewrite().getListRewrite(type, typeBodyDeclarationsProperty);
		bodyDeclarationsListRewrite.insertAt(intermediary, ASTNodes.getInsertionIndex(intermediary, type.bodyDeclarations()), imRewrite
				.createGroupDescription(RefactoringCoreMessages.IntroduceIndirectionRefactoring_group_description_create_new_method));
	}

	private Statement encapsulateInvocation(FunctionDeclaration declaration, FunctionInvocation invocation) {
		final Type type= declaration.getReturnType2();

		if (type == null || (type instanceof PrimitiveType && PrimitiveType.VOID.equals( ((PrimitiveType) type).getPrimitiveTypeCode())))
			return invocation.getAST().newExpressionStatement(invocation);

		ReturnStatement statement= invocation.getAST().newReturnStatement();
		statement.setExpression(invocation);
		return statement;
	}

	private void copyInvocationParameters(FunctionInvocation invocation, AST ast) throws JavaScriptModelException {
		String[] names= fTargetMethod.getParameterNames();
		for (int i= 0; i < names.length; i++)
			invocation.arguments().add(ast.newSimpleName(names[i]));
	}

	private void copyArguments(FunctionDeclaration intermediary, CompilationUnitRewrite rew) throws JavaScriptModelException {
		String[] names= fTargetMethod.getParameterNames();
		ITypeBinding[] types= fTargetMethodBinding.getParameterTypes();
		for (int i= 0; i < names.length; i++) {
			ITypeBinding typeBinding= types[i];
			SingleVariableDeclaration newElement= rew.getAST().newSingleVariableDeclaration();
			newElement.setName(rew.getAST().newSimpleName(names[i]));

			if (i == (names.length - 1) && fTargetMethodBinding.isVarargs()) {
				newElement.setVarargs(true);
				if (typeBinding.isArray())
					typeBinding= typeBinding.getComponentType();
			}

			newElement.setType(rew.getImportRewrite().addImport(typeBinding, rew.getAST()));
			intermediary.parameters().add(newElement);
		}
	}

	// ******************* UPDATE CALLS **********************

	private RefactoringStatus updateMethodInvocation(FunctionInvocation originalInvocation, IMember enclosing, CompilationUnitRewrite unitRewriter) throws JavaScriptModelException {

		RefactoringStatus status= new RefactoringStatus();

		// If the method invocation utilizes type arguments, skip this
		// call as the new target method may have additional parameters
		if (originalInvocation.typeArguments().size() > 0)
			return createWarningAboutCall(enclosing, originalInvocation, RefactoringCoreMessages.IntroduceIndirectionRefactoring_call_warning_type_arguments);

		FunctionInvocation newInvocation= unitRewriter.getAST().newFunctionInvocation();
		List newInvocationArgs= newInvocation.arguments();
		List originalInvocationArgs= originalInvocation.arguments();

		// static call => always use a qualifier
		String qualifier= unitRewriter.getImportRewrite().addImport(fIntermediaryClassBinding);
		newInvocation.setExpression(ASTNodeFactory.newName(unitRewriter.getAST(), qualifier));
		newInvocation.setName(unitRewriter.getAST().newSimpleName(getIntermediaryMethodName()));

		final Expression expression= originalInvocation.getExpression();

		if (!isStaticTarget()) {
			// Add the expression as the first parameter
			if (expression == null) {
				// There is no expression for this call. Use a (possibly qualified) "this" expression.
				ThisExpression expr= unitRewriter.getAST().newThisExpression();
				RefactoringStatus qualifierStatus= qualifyThisExpression(expr, originalInvocation, enclosing, unitRewriter);
				status.merge(qualifierStatus);
				if (qualifierStatus.hasEntries())
					// warning means don't include this invocation
					return status;
				newInvocationArgs.add(expr);
			} else {
				ASTNode expressionAsParam= unitRewriter.getASTRewrite().createMoveTarget(expression);
				newInvocationArgs.add(expressionAsParam);
			}
		} else {
			if (expression != null) {
				// Check if expression is the class name. If not, there may
				// be side effects (e.g. inside methods) -> don't update
				if (! (expression instanceof Name) || ASTNodes.getTypeBinding((Name) expression) == null)
					return createWarningAboutCall(enclosing, originalInvocation, RefactoringCoreMessages.IntroduceIndirectionRefactoring_call_warning_static_expression_access);
			}
		}

		for (int i= 0; i < originalInvocationArgs.size(); i++) {
			Expression originalInvocationArg= (Expression) originalInvocationArgs.get(i);
			ASTNode movedArg= unitRewriter.getASTRewrite().createMoveTarget(originalInvocationArg);
			newInvocationArgs.add(movedArg);
		}

		unitRewriter.getASTRewrite().replace(originalInvocation, newInvocation,
				unitRewriter.createGroupDescription(RefactoringCoreMessages.IntroduceIndirectionRefactoring_group_description_replace_call));

		return status;
	}

	/**
	 * Attempts to qualify a "this" expression for a method invocation with an appropriate qualifier. 
	 * The invoked method is analyzed according to the following specs:
	 * 
	 * 'this' must be qualified iff method is declared in an enclosing type or a supertype of an enclosing type
	 * 
	 * 1) The method is declared somewhere outside of the cu of the invocation
	 *      1a) inside a supertype of the current type
	 *      1b) inside a supertype of an enclosing type
	 * 2) The method is declared inside of the cu of the invocation
	 * 		2a) inside the type of the invocation 
	 * 		2b) outside the type of the invocation
	 * 
	 * In case of 1a) and 2b), qualify with the enclosing type. 
	 * @param expr 
	 * @param originalInvocation 
	 * @param enclosing 
	 * @param unitRewriter 
	 * @return resulting status
	 * 
	 */
	private RefactoringStatus qualifyThisExpression(ThisExpression expr, FunctionInvocation originalInvocation, IMember enclosing, CompilationUnitRewrite unitRewriter) {

		RefactoringStatus status= new RefactoringStatus();

		IFunctionBinding methodBinding= originalInvocation.resolveMethodBinding();
		FunctionDeclaration methodDeclaration= (FunctionDeclaration) ASTNodes.findDeclaration(methodBinding, originalInvocation.getRoot());

		ITypeBinding currentTypeBinding= null;
		if (methodDeclaration != null) {
			// Case 1) : Declaring class is inside this cu => use its name if it's declared in an enclosing type
			if (ASTNodes.isParent(originalInvocation, methodDeclaration.getParent()))
				currentTypeBinding= methodBinding.getDeclaringClass();
			else
				currentTypeBinding= ASTNodes.getEnclosingType(originalInvocation);
		} else {
			// Case 2) : Declaring class is outside of this cu => find subclass in this cu
			ASTNode currentTypeDeclaration= getEnclosingTypeDeclaration(originalInvocation);
			currentTypeBinding= ASTNodes.getEnclosingType(currentTypeDeclaration);
			while (currentTypeDeclaration != null && (Bindings.findMethodInHierarchy(currentTypeBinding, methodBinding.getName(), methodBinding.getParameterTypes()) == null)) {
				currentTypeDeclaration= getEnclosingTypeDeclaration(currentTypeDeclaration.getParent());
				currentTypeBinding= ASTNodes.getEnclosingType(currentTypeDeclaration);
			}
		}

		if (currentTypeBinding == null) {
			status.merge(createWarningAboutCall(enclosing, originalInvocation, RefactoringCoreMessages.IntroduceIndirectionRefactoring_call_warning_declaring_type_not_found));
			return status;
		}

		ITypeBinding typeOfCall= ASTNodes.getEnclosingType(originalInvocation);
		if (!typeOfCall.equals(currentTypeBinding)) {
			if (currentTypeBinding.isAnonymous()) {
				// Cannot qualify, see bug 115277
				status.merge(createWarningAboutCall(enclosing, originalInvocation, RefactoringCoreMessages.IntroduceIndirectionRefactoring_call_warning_anonymous_cannot_qualify));
			} else {
				expr.setQualifier(unitRewriter.getAST().newSimpleName(currentTypeBinding.getName()));
			}
		} else {
			// do not qualify, only use "this.".
		}

		return status;
	}

	// ********* SMALL HELPERS ********************

	/*
	 * Helper method for finding an IFunction inside a binding hierarchy
	 */
	private IFunctionBinding findMethodBindingInHierarchy(ITypeBinding currentTypeBinding, IFunction methodDeclaration) {
		IFunctionBinding[] bindings= currentTypeBinding.getDeclaredMethods();
		for (int i= 0; i < bindings.length; i++)
			if (methodDeclaration.equals(bindings[i].getJavaElement()))
				return bindings[i];

		ITypeBinding superClass= currentTypeBinding.getSuperclass();
		if (superClass != null) {
			IFunctionBinding b= findMethodBindingInHierarchy(superClass, methodDeclaration);
			if (b != null)
				return b;
		}
		return null;
	}

	/*
	 * Helper method for retrieving a *bottom-up* list of super type bindings
	 */
	private ITypeBinding[] getTypeAndAllSuperTypes(ITypeBinding type) {
		List result= new ArrayList();
		collectSuperTypes(type, result);
		return (ITypeBinding[]) result.toArray(new ITypeBinding[result.size()]);
	}

	private void collectSuperTypes(ITypeBinding curr, List list) {
		if (list.add(curr.getTypeDeclaration())) {
			ITypeBinding superClass= curr.getSuperclass();
			if (superClass != null) {
				collectSuperTypes(superClass, list);
			}
		}
	}

	private CompilationUnitRewrite getCachedCURewrite(IJavaScriptUnit unit) {
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) fRewrites.get(unit);
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(unit);
			fRewrites.put(unit, rewrite);
		}
		return rewrite;
	}

	private boolean isRewriteKept(IJavaScriptUnit compilationUnit) {
		return fIntermediaryClass.getJavaScriptUnit().equals(compilationUnit);
	}

	private void createChangeAndDiscardRewrite(IJavaScriptUnit compilationUnit) throws CoreException {
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) fRewrites.get(compilationUnit);
		if (rewrite != null) {
			fTextChangeManager.manage(compilationUnit, rewrite.createChange());
			fRewrites.remove(compilationUnit);
		}
	}

	private SearchResultGroup[] getReferences(IFunction[] methods, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		SearchPattern pattern= RefactoringSearchEngine.createOrPattern(methods, IJavaScriptSearchConstants.REFERENCES);
		IJavaScriptSearchScope scope= RefactoringScopeFactory.create(fIntermediaryClass, false);
		return RefactoringSearchEngine.search(pattern, scope, pm, status);
	}

	private ITypeBinding typeToBinding(IType type, JavaScriptUnit root) throws JavaScriptModelException {
		ASTNode typeNode= typeToDeclaration(type, root);
		if (type.isAnonymous()) {
			return ((AnonymousClassDeclaration) typeNode).resolveBinding();
		} else {
			return ((AbstractTypeDeclaration) typeNode).resolveBinding();
		}
	}

	private ASTNode typeToDeclaration(IType type, JavaScriptUnit root) throws JavaScriptModelException {
		Name intermediateName= (Name) NodeFinder.perform(root, type.getNameRange());
		if (type.isAnonymous()) {
			return ASTNodes.getParent(intermediateName, AnonymousClassDeclaration.class);
		} else {
			return ASTNodes.getParent(intermediateName, AbstractTypeDeclaration.class);
		}
	}

	private ASTNode getEnclosingTypeDeclaration(ASTNode node) {
		while (node != null) {
			if (node instanceof AbstractTypeDeclaration) {
				return node;
			} else if (node instanceof AnonymousClassDeclaration) {
				return node;
			}
			node= node.getParent();
		}
		return null;
	}

	private ChildListPropertyDescriptor typeToBodyDeclarationProperty(IType type, JavaScriptUnit root) throws JavaScriptModelException {
		ASTNode typeDeclaration= typeToDeclaration(type, root);
		if (typeDeclaration instanceof AbstractTypeDeclaration)
			return ((AbstractTypeDeclaration) typeDeclaration).getBodyDeclarationsProperty();
		else if (typeDeclaration instanceof AnonymousClassDeclaration)
			return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;

		Assert.isTrue(false);
		return null;
	}

	private RefactoringStatus createWarningAboutCall(IMember enclosing, ASTNode concreteNode, String message) {
		String name= JavaScriptElementLabels.getElementLabel(enclosing, JavaScriptElementLabels.ALL_DEFAULT);
		String container= JavaScriptElementLabels.getElementLabel(enclosing.getDeclaringType(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
		return RefactoringStatus.createWarningStatus(Messages.format(message, new String[] { name, container }), JavaStatusContext.create(enclosing.getJavaScriptUnit(), concreteNode));
	}

	private ITypeBinding getExpressionType(FunctionInvocation invocation) {
		Expression expression= invocation.getExpression();
		ITypeBinding typeBinding= null;
		if (expression == null) {
			typeBinding= invocation.resolveMethodBinding().getDeclaringClass();
		} else {
			typeBinding= expression.resolveTypeBinding();
		}

		Assert.isNotNull(typeBinding, "Type binding of target expression may not be null"); //$NON-NLS-1$
		return typeBinding;
	}

	private IFile[] getAllFilesToModify() {
		List cus= new ArrayList();
		cus.addAll(Arrays.asList(fTextChangeManager.getAllCompilationUnits()));
		return ResourceUtil.getFiles((IJavaScriptUnit[]) cus.toArray(new IJavaScriptUnit[cus.size()]));
	}

	private boolean isStaticTarget() throws JavaScriptModelException {
		return Flags.isStatic(fTargetMethod.getFlags());
	}

	private IMember getEnclosingInitialSelectionMember() throws JavaScriptModelException {
		return (IMember) fSelectionCompilationUnit.getElementAt(fSelectionStart);
	}

	private static ASTNode getSelectedNode(IJavaScriptUnit unit, JavaScriptUnit root, int offset, int length) {
		ASTNode node= null;
		try {
			if (unit != null)
				node= checkNode(NodeFinder.perform(root, offset, length, unit));
			else
				node= checkNode(NodeFinder.perform(root, offset, length));
		} catch (JavaScriptModelException e) {
			// Do nothing
		}
		if (node != null)
			return node;
		return checkNode(NodeFinder.perform(root, offset, length));
	}

	private static ASTNode checkNode(ASTNode node) {
		if (node == null)
			return null;
		if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
			node= node.getParent();
		} else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			node= ((ExpressionStatement) node).getExpression();
		}
		switch (node.getNodeType()) {
			case ASTNode.FUNCTION_INVOCATION:
			case ASTNode.FUNCTION_DECLARATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
				return node;
		}
		return null;
	}

	// ***************** VISIBILITY ********************

	private ModifierKeyword getNeededVisibility(IMember whoToAdjust, IMember fromWhereToLook) throws JavaScriptModelException {
		return fAdjustor.getVisibilityThreshold(fromWhereToLook, whoToAdjust, new NullProgressMonitor());
	}

	private RefactoringStatus adjustVisibility(IMember whoToAdjust, IMember fromWhereToLook, IProgressMonitor monitor) throws CoreException {
		return adjustVisibility(whoToAdjust, getNeededVisibility(whoToAdjust, fromWhereToLook), true, monitor);
	}
	
	private RefactoringStatus adjustVisibility(IMember whoToAdjust, ModifierKeyword neededVisibility, IProgressMonitor monitor) throws CoreException {
		return adjustVisibility(whoToAdjust, neededVisibility, false, monitor);
	}
	
	private RefactoringStatus adjustVisibility(IMember whoToAdjust, ModifierKeyword neededVisibility, boolean alsoIncreaseEnclosing, IProgressMonitor monitor) throws CoreException {
		
		Map adjustments;
		if (isRewriteKept(whoToAdjust.getJavaScriptUnit()))
			adjustments= fIntermediaryAdjustments;
		else
			adjustments= new HashMap();
		
		int existingAdjustments= adjustments.size();
		addAdjustment(whoToAdjust, neededVisibility, adjustments);

		if (alsoIncreaseEnclosing)
			while (whoToAdjust.getDeclaringType() != null) {
				whoToAdjust= whoToAdjust.getDeclaringType();
				addAdjustment(whoToAdjust, neededVisibility, adjustments);
			}

		boolean hasNewAdjustments= (adjustments.size() - existingAdjustments) > 0;
		if (hasNewAdjustments && ( (whoToAdjust.isReadOnly() || whoToAdjust.isBinary())))
			return RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.IntroduceIndirectionRefactoring_cannot_update_binary_target_visibility, new String[] { JavaScriptElementLabels
					.getElementLabel(whoToAdjust, JavaScriptElementLabels.ALL_DEFAULT) }), JavaStatusContext.create(whoToAdjust));

		RefactoringStatus status= new RefactoringStatus();
		
		// Don't create a rewrite if it is not necessary
		if (!hasNewAdjustments)
			return status;

		try {
			monitor.beginTask(RefactoringCoreMessages.MemberVisibilityAdjustor_adjusting, 2);
			Map rewrites;
			if (!isRewriteKept(whoToAdjust.getJavaScriptUnit())) {
				CompilationUnitRewrite rewrite= new CompilationUnitRewrite(whoToAdjust.getJavaScriptUnit());
				rewrite.setResolveBindings(false);
				rewrites= new HashMap();
				rewrites.put(whoToAdjust.getJavaScriptUnit(), rewrite);
				status.merge(rewriteVisibility(adjustments, rewrites, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
				rewrite.attachChange((CompilationUnitChange) fTextChangeManager.get(whoToAdjust.getJavaScriptUnit()), true, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	private RefactoringStatus rewriteVisibility(Map adjustments, Map rewrites, IProgressMonitor monitor) throws JavaScriptModelException {
		RefactoringStatus status= new RefactoringStatus();
		fAdjustor.setRewrites(rewrites);
		fAdjustor.setAdjustments(adjustments);
		fAdjustor.setStatus(status);
		fAdjustor.rewriteVisibility(monitor);
		return status;
	}

	private void addAdjustment(IMember whoToAdjust, ModifierKeyword neededVisibility, Map adjustments) throws JavaScriptModelException {
		ModifierKeyword currentVisibility= ModifierKeyword.fromFlagValue(JdtFlags.getVisibilityCode(whoToAdjust));
		if (MemberVisibilityAdjustor.hasLowerVisibility(currentVisibility, neededVisibility)
				&& MemberVisibilityAdjustor.needsVisibilityAdjustments(whoToAdjust, neededVisibility, adjustments))
			adjustments.put(whoToAdjust, new MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment(whoToAdjust, neededVisibility,
					RefactoringStatus.createWarningStatus(Messages.format(MemberVisibilityAdjustor.getMessage(whoToAdjust), new String[] {
							MemberVisibilityAdjustor.getLabel(whoToAdjust), MemberVisibilityAdjustor.getLabel(neededVisibility) }), JavaStatusContext
							.create(whoToAdjust))));
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.METHOD)
					return createInputFatalStatus(element, IJavaScriptRefactorings.INTRODUCE_INDIRECTION);
				else
					fTargetMethod= (IFunction) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.TYPE)
					return createInputFatalStatus(element, IJavaScriptRefactorings.INTRODUCE_INDIRECTION);
				else
					fIntermediaryClass= (IType) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1));
			final String references= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES);
			if (references != null) {
				fUpdateReferences= Boolean.valueOf(references).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				return setIntermediaryMethodName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
	}
}
