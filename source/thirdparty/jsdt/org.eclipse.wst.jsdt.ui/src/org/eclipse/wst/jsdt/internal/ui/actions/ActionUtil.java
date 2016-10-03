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
package org.eclipse.wst.jsdt.internal.ui.actions;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

/*
 * http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
 */
public class ActionUtil {
	
	private ActionUtil(){
	}

	//bug 31998	we will have to disable renaming of linked packages (and cus)
	public static boolean mustDisableJavaModelAction(Shell shell, Object element) {
		if (!(element instanceof IPackageFragment) && !(element instanceof IPackageFragmentRoot))
			return false;
		
		IResource resource= ResourceUtil.getResource(element);
		if ((resource == null) || (! (resource instanceof IFolder)) || (! resource.isLinked()))
			return false;
			
		MessageDialog.openInformation(shell, ActionMessages.ActionUtil_not_possible, ActionMessages.ActionUtil_no_linked); 
		return true;
	}
	
	public static boolean isProcessable(JavaEditor editor) {
		if (editor == null)
			return true;
		Shell shell= editor.getSite().getShell();
		IJavaScriptElement input= SelectionConverter.getInput(editor);
		// if a Java editor doesn't have an input of type Java element
		// then it is for sure not on the build path
		if (input == null) {
			MessageDialog.openInformation(shell, 
				ActionMessages.ActionUtil_notOnBuildPath_title,  
				ActionMessages.ActionUtil_notOnBuildPath_message); 
			return false;
		}
		return isProcessable(shell, input);
	}
	
	public static boolean isProcessable(Shell shell, IJavaScriptElement element) {
		if (element == null)
			return true;
		if (isOnBuildPath(element))
			return true;
		MessageDialog.openInformation(shell, 
			ActionMessages.ActionUtil_notOnBuildPath_title,  
			ActionMessages.ActionUtil_notOnBuildPath_message); 
		return false;
	}

	public static boolean isOnBuildPath(IJavaScriptElement element) {	
        //fix for bug http://dev.eclipse.org/bugs/show_bug.cgi?id=20051
        if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT)
            return true;
		IJavaScriptProject project= element.getJavaScriptProject();
		try {
			if (!project.isOnIncludepath(element))
				return false;
			IProject resourceProject= project.getProject();
			if (resourceProject == null)
				return false;
			IProjectNature nature= resourceProject.getNature(JavaScriptCore.NATURE_ID);
			// We have a Java project
			if (nature != null)
				return true;
		} catch (CoreException e) {
		}
		return false;
	}

	public static boolean areProcessable(Shell shell, IJavaScriptElement[] elements) {
		for (int i= 0; i < elements.length; i++) {
			if (! isOnBuildPath(elements[i])) {
				MessageDialog.openInformation(shell, 
						ActionMessages.ActionUtil_notOnBuildPath_title,  
						Messages.format(ActionMessages.ActionUtil_notOnBuildPath_resource_message, new Object[] {elements[i].getPath()}));
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Check whether <code>editor</code> and <code>element</code> are
	 * processable and editable. If the editor edits the element, the validation
	 * is only performed once. If necessary, ask the user whether the file(s)
	 * should be edited.
	 * 
	 * @param editor an editor, or <code>null</code> iff the action was not
	 *        executed from an editor
	 * @param shell a shell to serve as parent for a dialog
	 * @param element the element to check, cannot be <code>null</code>
	 * @return <code>true</code> if the element can be edited,
	 *         <code>false</code> otherwise
	 */
	public static boolean isEditable(JavaEditor editor, Shell shell, IJavaScriptElement element) {
		if (editor != null) {
			IJavaScriptElement input= SelectionConverter.getInput(editor);
			if (input != null && input.equals(element.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT)))
				return isEditable(editor);
			else
				return isEditable(editor) && isEditable(shell, element);
		}
		return isEditable(shell, element);
	}
	
	public static boolean isEditable(JavaEditor editor) {
		if (! isProcessable(editor))
			return false;
		
		return editor.validateEditorInputState();
	}
			
	public static boolean isEditable(Shell shell, IJavaScriptElement element) {
		if (! isProcessable(shell, element))
			return false;
		
		IJavaScriptElement cu= element.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		if (cu != null) {
			IResource resource= cu.getResource();
			if (resource != null && resource.isDerived()) {
				
				// see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#validateEditorInputState()
				final String warnKey= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WARN_IF_INPUT_DERIVED;
				IPreferenceStore store= EditorsUI.getPreferenceStore();
				if (!store.getBoolean(warnKey))
					return true;
				
				MessageDialogWithToggle toggleDialog= MessageDialogWithToggle.openYesNoQuestion(
						shell,
						ActionMessages.ActionUtil_warning_derived_title,
						Messages.format(ActionMessages.ActionUtil_warning_derived_message, resource.getFullPath().toString()),
						ActionMessages.ActionUtil_warning_derived_dontShowAgain,
						false,
						null,
						null);
				
				EditorsUI.getPreferenceStore().setValue(warnKey, !toggleDialog.getToggleState());
				
				return toggleDialog.getReturnCode() == IDialogConstants.YES_ID;
			}
		}
		return true;
	}

}

