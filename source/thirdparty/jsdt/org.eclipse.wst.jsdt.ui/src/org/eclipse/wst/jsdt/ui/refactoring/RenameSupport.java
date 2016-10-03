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
package org.eclipse.wst.jsdt.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.JavaRenameProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.JavaRenameRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameJavaProjectProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameLocalVariableProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameNonVirtualMethodProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameSourceFolderProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.UserInterfaceStarter;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.RenameRefactoringWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.RenameSelectionState;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.RenameUserInterfaceManager;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.RenameUserInterfaceStarter;

/**
 * Central access point to execute rename refactorings.
 * <p>
 * Note: this class is not intended to be subclassed.
 * </p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 *
 */
public class RenameSupport {

	private RenameRefactoring fRefactoring;
	private RefactoringStatus fPreCheckStatus;
	
	/**
	 * Executes some light weight precondition checking. If the returned status
	 * is an error then the refactoring can't be executed at all. However,
	 * returning an OK status doesn't guarantee that the refactoring can be
	 * executed. It may still fail while performing the exhaustive precondition
	 * checking done inside the methods <code>openDialog</code> or
	 * <code>perform</code>.
	 * 
	 * The method is mainly used to determine enable/disablement of actions.
	 * 
	 * @return the result of the light weight precondition checking.
	 * 
	 * @throws CoreException if an unexpected exception occurs while performing the checking.
	 * 
	 * @see #openDialog(Shell)
	 * @see #perform(Shell, IRunnableContext)
	 */
	public IStatus preCheck() throws CoreException {
		ensureChecked();
		if (fPreCheckStatus.hasFatalError())
			return fPreCheckStatus.getEntryMatchingSeverity(RefactoringStatus.FATAL).toStatus();
		else
			return new Status(IStatus.OK, JavaScriptPlugin.getPluginId(), 0, "", null); //$NON-NLS-1$
	}

	/**
	 * Opens the refactoring dialog for this rename support. 
	 * 
	 * @param parent a shell used as a parent for the refactoring dialog.
	 * @throws CoreException if an unexpected exception occurs while opening the
	 * dialog.
	 * 
	 * @see #openDialog(Shell, boolean)
	 */
	public void openDialog(Shell parent) throws CoreException {
		openDialog(parent, false);
	}
	
	/**
	 * Opens the refactoring dialog for this rename support. 
	 * 
	 * <p>
	 * This method has to be called from within the UI thread. 
	 * </p>
	 * 
	 * @param parent a shell used as a parent for the refactoring, preview, or error dialog
	 * @param showPreviewOnly if <code>true</code>, the dialog skips all user input pages and
	 * directly shows the preview or error page. Otherwise, shows all pages.
	 * @return <code>true</code> if the refactoring has been executed successfully,
	 * <code>false</code> if it has been canceled or if an error has happened during
	 * initial conditions checking.
	 * 
	 * @throws CoreException if an error occurred while executing the
	 * operation.
	 * 
	 * @see #openDialog(Shell)
	 * 
	 */
	public boolean openDialog(Shell parent, boolean showPreviewOnly) throws CoreException {
		ensureChecked();
		if (fPreCheckStatus.hasFatalError()) {
			showInformation(parent, fPreCheckStatus);
			return false; 
		}
		
		UserInterfaceStarter starter;
		if (! showPreviewOnly) {
			starter= RenameUserInterfaceManager.getDefault().getStarter(fRefactoring);
		} else {
			starter= new RenameUserInterfaceStarter();
			RenameRefactoringWizard wizard= new RenameRefactoringWizard(fRefactoring, fRefactoring.getName(), null, null, null) {
				protected void addUserInputPages() {
					// nothing to add
				}
			};
			wizard.setForcePreviewReview(showPreviewOnly);
			starter.initialize(wizard);
		}
		return starter.activate(fRefactoring, parent, getJavaRenameProcessor().getSaveMode());
	}

	/**
	 * Executes the rename refactoring without showing a dialog to gather
	 * additional user input (for example the new name of the <tt>IJavaScriptElement</tt>).
	 * Only an error dialog is shown (if necessary) to present the result
	 * of the refactoring's full precondition checking.
	 * <p>
	 * The method has to be called from within the UI thread. 
	 * </p>
	 * 
	 * @param parent a shell used as a parent for the error dialog.
	 * @param context a {@link IRunnableContext} to execute the operation.
	 * 
	 * @throws InterruptedException if the operation has been canceled by the
	 * user.
	 * @throws InvocationTargetException if an error occurred while executing the
	 * operation.
	 * 
	 * @see #openDialog(Shell)
	 * @see IRunnableContext#run(boolean, boolean, org.eclipse.jface.operation.IRunnableWithProgress)
	 */
	public void perform(Shell parent, IRunnableContext context) throws InterruptedException, InvocationTargetException {
		try {
			ensureChecked();
			if (fPreCheckStatus.hasFatalError()) {
				showInformation(parent, fPreCheckStatus);
				return;
			}
			
			RenameSelectionState state= createSelectionState();
		
			RefactoringExecutionHelper helper= new RefactoringExecutionHelper(fRefactoring,
					RefactoringCore.getConditionCheckingFailedSeverity(),
					getJavaRenameProcessor().getSaveMode(),
					parent,
					context);
			helper.perform(true, true);
		
			restoreSelectionState(state);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	/** Flag indication that no additional update is to be performed. */
	public static final int NONE= 0;
	
	/** Flag indicating that references are to be updated as well. */
	public static final int UPDATE_REFERENCES= 1 << 0;
	
	/**
	 * Flag indicating that textual matches in comments and in string literals
	 * are to be updated as well.
	 * 
	 */
	public static final int UPDATE_TEXTUAL_MATCHES= 1 << 6;

	/** Flag indicating that the getter method is to be updated as well. */
	public static final int UPDATE_GETTER_METHOD= 1 << 4;

	/** Flag indicating that the setter method is to be updated as well. */
	public static final int UPDATE_SETTER_METHOD= 1 << 5;


	private RenameSupport(RenameJavaScriptElementDescriptor descriptor) throws CoreException {
		RefactoringStatus refactoringStatus= new RefactoringStatus();
		fRefactoring= (RenameRefactoring) descriptor.createRefactoring(refactoringStatus);
		if (refactoringStatus.hasFatalError()) {
			fPreCheckStatus= refactoringStatus;
		} else {
			preCheck();
			refactoringStatus.merge(fPreCheckStatus);
			fPreCheckStatus= refactoringStatus;
		}
	}
	
	/**
	 * Creates a new rename support for the given
	 * {@link RenameJavaScriptElementDescriptor}.
	 * 
	 * @param descriptor the {@link RenameJavaScriptElementDescriptor} to create a
	 *        {@link RenameSupport} for. The caller is responsible for
	 *        configuring the descriptor before it is passed.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating the
	 *         {@link RenameSupport}.
	 * 
	 */
	public static RenameSupport create(RenameJavaScriptElementDescriptor descriptor) throws CoreException {
		return new RenameSupport(descriptor);
	}
	
	private RenameSupport(JavaRenameProcessor processor, String newName, int flags) throws CoreException {
		fRefactoring= new JavaRenameRefactoring(processor);
		initialize(fRefactoring, newName, flags);
	}

	private JavaRenameProcessor getJavaRenameProcessor() {
		return (JavaRenameProcessor) fRefactoring.getProcessor();
	}

	/**
	 * Creates a new rename support for the given {@link IJavaScriptProject}.
	 * 
	 * @param project the {@link IJavaScriptProject} to be renamed.
	 * @param newName the project's new name. <code>null</code> is a valid
	 * value indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code> or <code>NONE</code>.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating
	 * the {@link RenameSupport}.
	 */
	public static RenameSupport create(IJavaScriptProject project, String newName, int flags) throws CoreException {
		JavaRenameProcessor processor= new RenameJavaProjectProcessor(project);
		return new RenameSupport(processor, newName, flags);
	}
	
	/**
	 * Creates a new rename support for the given {@link IPackageFragmentRoot}.
	 * 
	 * @param root the {@link IPackageFragmentRoot} to be renamed.
	 * @param newName the package fragment root's new name. <code>null</code> is
	 * a valid value indicating that no new name is provided.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating
	 * the {@link RenameSupport}.
	 */
	public static RenameSupport create(IPackageFragmentRoot root, String newName) throws CoreException {
		JavaRenameProcessor processor= new RenameSourceFolderProcessor(root);
		return new RenameSupport(processor, newName, 0);
	}
	
	/**
	 * Creates a new rename support for the given {@link IPackageFragment}.
	 * 
	 * @param fragment the {@link IPackageFragment} to be renamed.
	 * @param newName the package fragment's new name. <code>null</code> is a
	 * valid value indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, and <code>UPDATE_TEXTUAL_MATCHES</code>,
	 * or their bitwise OR, or <code>NONE</code>.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating
	 * the {@link RenameSupport}.
	 */
	public static RenameSupport create(IPackageFragment fragment, String newName, int flags) throws CoreException {
		JavaRenameProcessor processor= new RenamePackageProcessor(fragment);
		return new RenameSupport(processor, newName, flags);
	}
	
	/**
	 * Creates a new rename support for the given {@link IJavaScriptUnit}.
	 * 
	 * @param unit the {@link IJavaScriptUnit} to be renamed.
	 * @param newName the compilation unit's new name. <code>null</code> is a
	 * valid value indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, and <code>UPDATE_TEXTUAL_MATCHES</code>,
	 * or their bitwise OR, or <code>NONE</code>.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating
	 * the {@link RenameSupport}.
	 */
	public static RenameSupport create(IJavaScriptUnit unit, String newName, int flags) throws CoreException {
		JavaRenameProcessor processor= new RenameCompilationUnitProcessor(unit);
		return new RenameSupport(processor, newName, flags);
	}
	
	/**
	 * Creates a new rename support for the given {@link IType}.
	 * 
	 * @param type the {@link IType} to be renamed.
	 * @param newName the type's new name. <code>null</code> is a valid value
	 * indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, and <code>UPDATE_TEXTUAL_MATCHES</code>,
	 * or their bitwise OR, or <code>NONE</code>.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating
	 * the {@link RenameSupport}.
	 */
	public static RenameSupport create(IType type, String newName, int flags) throws CoreException {
		JavaRenameProcessor processor= new RenameTypeProcessor(type);
		return new RenameSupport(processor, newName, flags);
	}
	
	/**
	 * Creates a new rename support for the given {@link IFunction}.
	 * 
	 * @param method the {@link IFunction} to be renamed.
	 * @param newName the method's new name. <code>null</code> is a valid value
	 * indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code> or <code>NONE</code>.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating
	 * the {@link RenameSupport}.
	 */
	public static RenameSupport create(IFunction method, String newName, int flags) throws CoreException {
		JavaRenameProcessor processor;
		if (MethodChecks.isVirtual(method)) {
			processor= new RenameVirtualMethodProcessor(method);
		} else {
			processor= new RenameNonVirtualMethodProcessor(method);
		}
		return new RenameSupport(processor, newName, flags);
	}
	
	/**
	 * Creates a new rename support for the given {@link IField}.
	 * 
	 * @param field the {@link IField} to be renamed.
	 * @param newName the field's new name. <code>null</code> is a valid value
	 * indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, <code>UPDATE_TEXTUAL_MATCHES</code>,
	 * <code>UPDATE_GETTER_METHOD</code>, and <code>UPDATE_SETTER_METHOD</code>,
	 * or their bitwise OR, or <code>NONE</code>.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating
	 * the {@link RenameSupport}.
	 */
	public static RenameSupport create(IField field, String newName, int flags) throws CoreException {
			final RenameFieldProcessor processor= new RenameFieldProcessor(field);
			processor.setRenameGetter(updateGetterMethod(flags));
			processor.setRenameSetter(updateSetterMethod(flags));
			return new RenameSupport(processor, newName, flags);
	}

	/**
	 * Creates a new rename support for the given {@link ILocalVariable}.
	 * 
	 * @param variable the {@link ILocalVariable} to be renamed.
	 * @param newName the variable's new name. <code>null</code> is a valid value
	 * indicating that no new name is provided.
	 * @param flags flags controlling additional parameters. Valid flags are
	 * <code>UPDATE_REFERENCES</code>, or <code>NONE</code>.
	 * @return the {@link RenameSupport}.
	 * @throws CoreException if an unexpected error occurred while creating
	 * the {@link RenameSupport}.
	 * 
	 */
	public static RenameSupport create(ILocalVariable variable, String newName, int flags) throws CoreException {
		RenameLocalVariableProcessor processor= new RenameLocalVariableProcessor(variable);
		processor.setUpdateReferences(updateReferences(flags));
		return new RenameSupport(processor, newName, flags);
	}

	private static void initialize(RenameRefactoring refactoring, String newName, int flags) {
		if (refactoring.getProcessor() == null)
			return;
		setNewName((INameUpdating)refactoring.getAdapter(INameUpdating.class), newName);
		IReferenceUpdating reference= (IReferenceUpdating)refactoring.getAdapter(IReferenceUpdating.class);
		if (reference != null) {
			reference.setUpdateReferences(updateReferences(flags));
		}
		ITextUpdating text= (ITextUpdating)refactoring.getAdapter(ITextUpdating.class);
		if (text != null) {
			text.setUpdateTextualMatches(updateTextualMatches(flags));
		}
	}
	
	private static void setNewName(INameUpdating refactoring, String newName) {
		if (newName != null)
			refactoring.setNewElementName(newName);
	}
	
	private static boolean updateReferences(int flags) {
		return (flags & UPDATE_REFERENCES) != 0;
	}
	
	private static boolean updateTextualMatches(int flags) {
		int TEXT_UPDATES= UPDATE_TEXTUAL_MATCHES;
		return (flags & TEXT_UPDATES) != 0;
	}
	
	private static boolean updateGetterMethod(int flags) {
		return (flags & UPDATE_GETTER_METHOD) != 0;
	}
	
	private static boolean updateSetterMethod(int flags) {
		return (flags & UPDATE_SETTER_METHOD) != 0;
	}
	
	private void ensureChecked() throws CoreException {
		if (fPreCheckStatus == null) {
			if (!fRefactoring.isApplicable()) {
				fPreCheckStatus= RefactoringStatus.createFatalErrorStatus(JavaUIMessages.RenameSupport_not_available); 
			} else {
				fPreCheckStatus= new RefactoringStatus();
			}
		}
	}
	
	private void showInformation(Shell parent, RefactoringStatus status) {
		String message= status.getMessageMatchingSeverity(RefactoringStatus.FATAL);
		MessageDialog.openInformation(parent, JavaUIMessages.RenameSupport_dialog_title, message); 
	}
	
	private RenameSelectionState createSelectionState() {
		RenameProcessor processor= (RenameProcessor) fRefactoring.getAdapter(RenameProcessor.class);
		Object[] elements= processor.getElements();
		RenameSelectionState state= elements.length == 1 ? new RenameSelectionState(elements[0]) : null;
		return state;
	}
	
	private void restoreSelectionState(RenameSelectionState state) throws CoreException {
		INameUpdating nameUpdating= (INameUpdating) fRefactoring.getAdapter(INameUpdating.class);
		if (nameUpdating != null && state != null) {
			Object newElement= nameUpdating.getNewElement();
			if (newElement != null) {
				state.restore(newElement);
			}			
		}
	}
}
