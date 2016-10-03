/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.CopyRefactoring;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.IntroduceIndirectionRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.IntroduceParameterRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ReplaceInvocationsRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.JavaRenameRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameResourceProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaCopyProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaCopyRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaDeleteRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.wst.jsdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ExtractSupertypeRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.JavaMoveRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PushDownRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.UseSuperTypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.UseSuperTypeRefactoring;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.fix.CleanUpRefactoringWizard;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.refactoring.ChangeSignatureWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.ChangeTypeWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.ConvertAnonymousToNestedWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.ExtractSupertypeWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.InlineConstantWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.InlineTempWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.IntroduceFactoryWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.IntroduceIndirectionWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.IntroduceParameterWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.MoveInnerToTopWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.MoveInstanceMethodWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.PushDownWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.UseSupertypeWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.UserInterfaceStarter;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.wst.jsdt.internal.ui.refactoring.code.InlineMethodWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.code.ReplaceInvocationsWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.CreateTargetQueries;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.DeleteUserInterfaceManager;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.NewNameQueries;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.RenameUserInterfaceManager;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.ReorgCopyWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.ReorgMoveWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.ReorgQueries;
import org.eclipse.wst.jsdt.internal.ui.refactoring.sef.SelfEncapsulateFieldWizard;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;
import org.eclipse.wst.jsdt.ui.refactoring.RenameSupport;

/**
 * Helper class to run refactorings from action code.
 * <p>
 * This class has been introduced to decouple actions from the refactoring code,
 * in order not to eagerly load refactoring classes during action
 * initialization.
 * </p>
 * 
 * 
 */
public final class RefactoringExecutionStarter {

	private static RenameSupport createRenameSupport(IJavaScriptElement element, String newName, int flags) throws CoreException {
		switch (element.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				return RenameSupport.create((IJavaScriptProject) element, newName, flags);
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				return RenameSupport.create((IPackageFragmentRoot) element, newName);
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				return RenameSupport.create((IPackageFragment) element, newName, flags);
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return RenameSupport.create((IJavaScriptUnit) element, newName, flags);
			case IJavaScriptElement.TYPE:
				return RenameSupport.create((IType) element, newName, flags);
			case IJavaScriptElement.METHOD:
				final IFunction method= (IFunction) element;
				if (method.isConstructor())
					return createRenameSupport(method.getDeclaringType(), newName, flags);
				else
					return RenameSupport.create((IFunction) element, newName, flags);
			case IJavaScriptElement.FIELD:
				return RenameSupport.create((IField) element, newName, flags);
			case IJavaScriptElement.LOCAL_VARIABLE:
				return RenameSupport.create((ILocalVariable) element, newName, flags);
		}
		return null;
	}

	public static void startChangeSignatureRefactoring(final IFunction method, final SelectionDispatchAction action, final Shell shell) throws JavaScriptModelException {
		if (!RefactoringAvailabilityTester.isChangeSignatureAvailable(method))
			return;
		final ChangeSignatureRefactoring refactoring= new ChangeSignatureRefactoring(method);
		final UserInterfaceStarter starter= new UserInterfaceStarter() {

			public final boolean activate(final Refactoring ref, final Shell parent, final int saveMode) throws CoreException {
				final RefactoringStatus status= ref.checkInitialConditions(new NullProgressMonitor());
				if (status.hasFatalError()) {
					final RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
					if (entry.getCode() == RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD || entry.getCode() == RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {

						String message= entry.getMessage();
						final Object element= entry.getData();
						message= message + RefactoringMessages.RefactoringErrorDialogUtil_okToPerformQuestion;
						if (element != null && MessageDialog.openQuestion(shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, message)) {

							final IStructuredSelection selection= new StructuredSelection(element);
							// TODO: should not hijack this
							// ModifiyParametersAction.
							// The action is set up on an editor, but we use it
							// as if it were set up on a ViewPart.
							boolean wasEnabled= action.isEnabled();
							action.selectionChanged(selection);
							if (action.isEnabled()) {
								action.run(selection);
							} else {
								MessageDialog.openInformation(shell, ActionMessages.ModifyParameterAction_problem_title, ActionMessages.ModifyParameterAction_problem_message);
							}
							action.setEnabled(wasEnabled);
						}
						return false;
					}
				}
				return super.activate(ref, parent, saveMode);
			}
		};
		starter.initialize(new ChangeSignatureWizard(refactoring));
		try {
			starter.activate(refactoring, shell, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.RefactoringStarter_unexpected_exception);
		}
	}

	public static void startChangeTypeRefactoring(final IJavaScriptUnit unit, final Shell shell, final int offset, final int length) throws JavaScriptModelException {
		final ChangeTypeRefactoring refactoring= new ChangeTypeRefactoring(unit, offset, length);
		new RefactoringStarter().activate(refactoring, new ChangeTypeWizard(refactoring), shell, RefactoringMessages.ChangeTypeAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}
	
	public static void startCleanupRefactoring(IJavaScriptUnit[] cus, ICleanUp[] cleanUps, Shell shell, boolean showWizard, String actionName) throws InvocationTargetException, JavaScriptModelException {
		final CleanUpRefactoring refactoring= new CleanUpRefactoring(actionName);
		for (int i= 0; i < cus.length; i++) {
			refactoring.addCompilationUnit(cus[i]);
		}
		
		if (!showWizard) {
			for (int i= 0; i < cleanUps.length; i++) {
				refactoring.addCleanUp(cleanUps[i]);
			}
			
			IRunnableContext context;
			if (refactoring.getCompilationUnits().length > 1) {
				context= new ProgressMonitorDialog(shell);
			} else {
				context= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			}
			
			RefactoringExecutionHelper helper= new RefactoringExecutionHelper(refactoring, IStatus.INFO, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES, shell, context);
			try {
				helper.perform(true, true);
			} catch (InterruptedException e) {
			}
		} else {
			CleanUpRefactoringWizard refactoringWizard= new CleanUpRefactoringWizard(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
			RefactoringStarter starter= new RefactoringStarter();
			starter.activate(refactoring, refactoringWizard, shell, actionName, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);			
		}
	}

	public static void startConvertAnonymousRefactoring(final IJavaScriptUnit unit, final int offset, final int length, final Shell shell) throws JavaScriptModelException {
		final ConvertAnonymousToNestedRefactoring refactoring= new ConvertAnonymousToNestedRefactoring(unit, offset, length);
		new RefactoringStarter().activate(refactoring, new ConvertAnonymousToNestedWizard(refactoring), shell, RefactoringMessages.ConvertAnonymousToNestedAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startCopyRefactoring(IResource[] resources, IJavaScriptElement[] javaElements, Shell shell) throws JavaScriptModelException {
		ICopyPolicy copyPolicy= ReorgPolicyFactory.createCopyPolicy(resources, javaElements);
		if (copyPolicy.canEnable()) {
			JavaCopyProcessor processor= new JavaCopyProcessor(copyPolicy);
			CopyRefactoring refactoring= new JavaCopyRefactoring(processor);
			RefactoringWizard wizard= new ReorgCopyWizard(refactoring);
			processor.setNewNameQueries(new NewNameQueries(wizard));
			processor.setReorgQueries(new ReorgQueries(wizard));
			new RefactoringStarter().activate(refactoring, wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_NOTHING);
		}
	}

	public static void startCutRefactoring(final Object[] elements, final Shell shell) throws CoreException, InterruptedException, InvocationTargetException {
		final JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		processor.setSuggestGetterSetterDeletion(false);
		processor.setQueries(new ReorgQueries(shell));
		new RefactoringExecutionHelper(new JavaDeleteRefactoring(processor), RefactoringCore.getConditionCheckingFailedSeverity(), RefactoringSaveHelper.SAVE_NOTHING, shell, new ProgressMonitorDialog(shell)).perform(false, false);
	}

	public static void startDeleteRefactoring(final Object[] elements, final Shell shell) throws CoreException {
		final DeleteRefactoring refactoring= new JavaDeleteRefactoring(new JavaDeleteProcessor(elements));
		DeleteUserInterfaceManager.getDefault().getStarter(refactoring).activate(refactoring, shell, RefactoringSaveHelper.SAVE_NOTHING);
	}

	public static void startExtractSupertypeRefactoring(final IMember[] members, final Shell shell) throws JavaScriptModelException {
		if (!RefactoringAvailabilityTester.isExtractSupertypeAvailable(members))
			return;
		IJavaScriptProject project= null;
		if (members != null && members.length > 0)
			project= members[0].getJavaScriptProject();
		final ExtractSupertypeRefactoring refactoring= new ExtractSupertypeRefactoring(new ExtractSupertypeProcessor(members, JavaPreferencesSettings.getCodeGenerationSettings(project)));
		new RefactoringStarter().activate(refactoring, new ExtractSupertypeWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static boolean startInlineConstantRefactoring(final IJavaScriptUnit unit, final JavaScriptUnit node, final int offset, final int length, final Shell shell) throws JavaScriptModelException {
		final InlineConstantRefactoring refactoring= new InlineConstantRefactoring(unit, node, offset, length);
		if (! refactoring.checkStaticFinalConstantNameSelected().hasFatalError()) {
			new RefactoringStarter().activate(refactoring, new InlineConstantWizard(refactoring), shell, RefactoringMessages.InlineConstantAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
			return true;
		}
		return false;
	}

	public static boolean startInlineMethodRefactoring(final ITypeRoot typeRoot, final JavaScriptUnit node, final int offset, final int length, final Shell shell) throws JavaScriptModelException {
		final InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(typeRoot, node, offset, length);
		if (refactoring != null) {
			new RefactoringStarter().activate(refactoring, new InlineMethodWizard(refactoring), shell, RefactoringMessages.InlineMethodAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
			return true;
		}
		return false;
	}

	public static boolean startInlineTempRefactoring(final IJavaScriptUnit unit, JavaScriptUnit node, final ITextSelection selection, final Shell shell) throws JavaScriptModelException {
		final InlineTempRefactoring refactoring= new InlineTempRefactoring(unit, node, selection.getOffset(), selection.getLength());
		if (! refactoring.checkIfTempSelected().hasFatalError()) {
			new RefactoringStarter().activate(refactoring, new InlineTempWizard(refactoring), shell, RefactoringMessages.InlineTempAction_inline_temp, RefactoringSaveHelper.SAVE_NOTHING);
			return true;
		}
		return false;
	}

	public static void startIntroduceFactoryRefactoring(final IJavaScriptUnit unit, final ITextSelection selection, final Shell shell) throws JavaScriptModelException {
		final IntroduceFactoryRefactoring refactoring= new IntroduceFactoryRefactoring(unit, selection.getOffset(), selection.getLength());
		new RefactoringStarter().activate(refactoring, new IntroduceFactoryWizard(refactoring, RefactoringMessages.IntroduceFactoryAction_use_factory), shell, RefactoringMessages.IntroduceFactoryAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startIntroduceIndirectionRefactoring(final IClassFile file, final int offset, final int length, final Shell shell) throws JavaScriptModelException {
		final IntroduceIndirectionRefactoring refactoring= new IntroduceIndirectionRefactoring(file, offset, length);
		new RefactoringStarter().activate(refactoring, new IntroduceIndirectionWizard(refactoring, RefactoringMessages.IntroduceIndirectionAction_dialog_title), shell, RefactoringMessages.IntroduceIndirectionAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startIntroduceIndirectionRefactoring(final IJavaScriptUnit unit, final int offset, final int length, final Shell shell) throws JavaScriptModelException {
		final IntroduceIndirectionRefactoring refactoring= new IntroduceIndirectionRefactoring(unit, offset, length);
		new RefactoringStarter().activate(refactoring, new IntroduceIndirectionWizard(refactoring, RefactoringMessages.IntroduceIndirectionAction_dialog_title), shell, RefactoringMessages.IntroduceIndirectionAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startIntroduceIndirectionRefactoring(final IFunction method, final Shell shell) throws JavaScriptModelException {
		final IntroduceIndirectionRefactoring refactoring= new IntroduceIndirectionRefactoring(method);
		new RefactoringStarter().activate(refactoring, new IntroduceIndirectionWizard(refactoring, RefactoringMessages.IntroduceIndirectionAction_dialog_title), shell, RefactoringMessages.IntroduceIndirectionAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startIntroduceParameter(IJavaScriptUnit unit, int offset, int length, Shell shell) throws JavaScriptModelException {
		final IntroduceParameterRefactoring refactoring= new IntroduceParameterRefactoring(unit, offset, length);
		new RefactoringStarter().activate(refactoring, new IntroduceParameterWizard(refactoring), shell, RefactoringMessages.IntroduceParameterAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startMoveInnerRefactoring(final IType type, final Shell shell) throws JavaScriptModelException {
		if (!RefactoringAvailabilityTester.isMoveInnerAvailable(type))
			return;
		final MoveInnerToTopRefactoring refactoring= new MoveInnerToTopRefactoring(type, JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaScriptProject()));
		new RefactoringStarter().activate(refactoring, new MoveInnerToTopWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startMoveMethodRefactoring(final IFunction method, final Shell shell) throws JavaScriptModelException {
		final MoveInstanceMethodRefactoring refactoring= new MoveInstanceMethodRefactoring(new MoveInstanceMethodProcessor(method, JavaPreferencesSettings.getCodeGenerationSettings(method.getJavaScriptProject())));
		new RefactoringStarter().activate(refactoring, new MoveInstanceMethodWizard(refactoring), shell, RefactoringMessages.MoveInstanceMethodAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startMoveRefactoring(final IResource[] resources, final IJavaScriptElement[] elements, final Shell shell) throws JavaScriptModelException {
		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, elements);
		if (policy.canEnable()) {
			final JavaMoveProcessor processor= new JavaMoveProcessor(policy);
			final JavaMoveRefactoring refactoring= new JavaMoveRefactoring(processor);
			final RefactoringWizard wizard= new ReorgMoveWizard(refactoring);
			processor.setCreateTargetQueries(new CreateTargetQueries(wizard));
			processor.setReorgQueries(new ReorgQueries(wizard));
			new RefactoringStarter().activate(refactoring, wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_ALL);
		}
	}

	public static void startMoveStaticMembersRefactoring(final IMember[] members, final Shell shell) throws JavaScriptModelException {
		if (!RefactoringAvailabilityTester.isMoveStaticAvailable(members))
			return;
		final Set set= new HashSet();
		set.addAll(Arrays.asList(members));
		final IMember[] elements= (IMember[]) set.toArray(new IMember[set.size()]);
		IJavaScriptProject project= null;
		if (elements.length > 0)
			project= elements[0].getJavaScriptProject();
		final JavaMoveRefactoring refactoring= new JavaMoveRefactoring(new MoveStaticMembersProcessor(elements, JavaPreferencesSettings.getCodeGenerationSettings(project)));
		new RefactoringStarter().activate(refactoring, new MoveMembersWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_NON_JAVA_UPDATES);
	}

	public static void startPullUpRefactoring(final IMember[] members, final Shell shell) throws JavaScriptModelException {
		if (!RefactoringAvailabilityTester.isPullUpAvailable(members))
			return;
		IJavaScriptProject project= null;
		if (members != null && members.length > 0)
			project= members[0].getJavaScriptProject();
		final PullUpRefactoring refactoring= new PullUpRefactoring(new PullUpRefactoringProcessor(members, JavaPreferencesSettings.getCodeGenerationSettings(project)));
		new RefactoringStarter().activate(refactoring, new PullUpWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startPushDownRefactoring(final IMember[] members, final Shell shell) throws JavaScriptModelException {
		if (!RefactoringAvailabilityTester.isPushDownAvailable(members))
			return;
		final PushDownRefactoring refactoring= new PushDownRefactoring(new PushDownRefactoringProcessor(members));
		new RefactoringStarter().activate(refactoring, new PushDownWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startRenameRefactoring(final IJavaScriptElement element, final Shell shell) throws CoreException {
		final RenameSupport support= createRenameSupport(element, null, RenameSupport.UPDATE_REFERENCES);
		if (support != null && support.preCheck().isOK())
			support.openDialog(shell);
	}

	public static void startRenameResourceRefactoring(final IResource resource, final Shell shell) throws CoreException {
		final JavaRenameRefactoring refactoring= new JavaRenameRefactoring(new RenameResourceProcessor(resource));
		RenameUserInterfaceManager.getDefault().getStarter(refactoring).activate(refactoring, shell, RefactoringSaveHelper.SAVE_ALL);
	}

	public static void startReplaceInvocationsRefactoring(final ITypeRoot typeRoot, final int offset, final int length, final Shell shell) throws JavaScriptModelException {
		final ReplaceInvocationsRefactoring refactoring= new ReplaceInvocationsRefactoring(typeRoot, offset, length);
		new RefactoringStarter().activate(refactoring, new ReplaceInvocationsWizard(refactoring), shell, RefactoringMessages.ReplaceInvocationsAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startReplaceInvocationsRefactoring(final IFunction method, final Shell shell) throws JavaScriptModelException {
		final ReplaceInvocationsRefactoring refactoring= new ReplaceInvocationsRefactoring(method);
		new RefactoringStarter().activate(refactoring, new ReplaceInvocationsWizard(refactoring), shell, RefactoringMessages.ReplaceInvocationsAction_dialog_title, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	public static void startSelfEncapsulateRefactoring(final IField field, final Shell shell) {
		try {
			if (!RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field))
				return;
			final SelfEncapsulateFieldRefactoring refactoring= new SelfEncapsulateFieldRefactoring(field);
			new RefactoringStarter().activate(refactoring, new SelfEncapsulateFieldWizard(refactoring), shell, "", RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES); //$NON-NLS-1$
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, ActionMessages.SelfEncapsulateFieldAction_dialog_title, ActionMessages.SelfEncapsulateFieldAction_dialog_cannot_perform);
		}
	}

	public static void startUseSupertypeRefactoring(final IType type, final Shell shell) throws JavaScriptModelException {
		final UseSuperTypeRefactoring refactoring= new UseSuperTypeRefactoring(new UseSuperTypeProcessor(type));
		new RefactoringStarter().activate(refactoring, new UseSupertypeWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
	}

	private RefactoringExecutionStarter() {
		// Not for instantiation
	}
	
//	public static void startIntroduceParameterObject(IJavaScriptUnit unit, int offset, int length, Shell shell) throws CoreException {
//		IJavaScriptElement javaElement= unit.getElementAt(offset);
//		if (javaElement instanceof IFunction) {
//			IFunction method= (IFunction) javaElement;
//			startIntroduceParameterObject(method, shell);
//		}
//	}
//
//	public static void startIntroduceParameterObject(IFunction method, Shell shell) throws CoreException {
//		RefactoringStatus availability= Checks.checkAvailability(method);
//		if (availability.hasError()){
//			MessageDialog.openError(shell, RefactoringMessages.RefactoringExecutionStarter_IntroduceParameterObject_problem_title, RefactoringMessages.RefactoringExecutionStarter_IntroduceParameterObject_problem_description);
//			return;
//		}
//		IntroduceParameterObjectRefactoring refactoring= new IntroduceParameterObjectRefactoring(method);
//		final RefactoringStatus status= refactoring.checkInitialConditions(new NullProgressMonitor());
//		if (status.hasFatalError()) {
//			final RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
//			if (entry.getCode() == RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD || entry.getCode() == RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {
//
//				String message= entry.getMessage();
//				final Object element= entry.getData();
//				IFunction superMethod= (IFunction) element;
//				availability= Checks.checkAvailability(superMethod);
//				if (availability.hasError()){
//					MessageDialog.openError(shell, RefactoringMessages.RefactoringExecutionStarter_IntroduceParameterObject_problem_title, RefactoringMessages.RefactoringExecutionStarter_IntroduceParameterObject_problem_description);
//					return;
//				}
//				message= message + RefactoringMessages.RefactoringErrorDialogUtil_okToPerformQuestion;
//				if (element != null && MessageDialog.openQuestion(shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, message)) {
//					refactoring=new IntroduceParameterObjectRefactoring(superMethod);
//				}
//				else refactoring=null;
//			}
//		}
//		if (refactoring!=null)
//			new RefactoringStarter().activate(refactoring, new IntroduceParameterObjectWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES);
//	}
}
