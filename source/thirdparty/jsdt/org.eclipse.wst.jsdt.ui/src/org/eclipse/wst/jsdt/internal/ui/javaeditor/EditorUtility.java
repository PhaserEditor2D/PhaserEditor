/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.javaeditor;


import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Util;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * A number of routines for working with JavaElements in editors.
 *
 * Use 'isOpenInEditor' to test if an element is already open in a editor
 * Use 'openInEditor' to force opening an element in a editor
 * With 'getWorkingCopy' you get the working copy (element in the editor) of an element
 */
public class EditorUtility {


	/**
	 * Tests if a CU is currently shown in an editor
	 * 
	 * @return the IEditorPart if shown, null if element is not open in an editor
	 */
	public static IEditorPart isOpenInEditor(Object inputElement) {
		IEditorInput input= null;

		try {
			input= getEditorInput(inputElement);
		} catch (JavaScriptModelException x) {
			JavaScriptPlugin.log(x.getStatus());
		}

		if (input != null) {
			IWorkbenchPage p= JavaScriptPlugin.getActivePage();
			if (p != null) {
				return p.findEditor(input);
			}
		}

		return null;
	}

	/**
	 * Opens a Java editor for an element such as <code>IJavaScriptElement</code>, <code>IFile</code>, or <code>IStorage</code>.
	 * The editor is activated by default.
	 * 
	 * @return an open editor or <code>null</code> if an external editor was opened
	 * @throws PartInitException if the editor could not be opened or the input element is not valid
	 */
	public static IEditorPart openInEditor(Object inputElement) throws JavaScriptModelException, PartInitException {
		return openInEditor(inputElement, true);
	}

	/**
	 * Opens the editor currently associated with the given element (IJavaScriptElement, IFile, IStorage...)
	 * 
	 * @return an open editor or <code>null</code> if an external editor was opened
	 * @throws PartInitException if the editor could not be opened or the input element is not valid
	 */
	public static IEditorPart openInEditor(Object inputElement, boolean activate) throws JavaScriptModelException, PartInitException {

		if (inputElement instanceof IFile)
			return openInEditor((IFile) inputElement, activate);
		
		if(inputElement instanceof IJavaScriptElement && ((IJavaScriptElement)inputElement).isVirtual()) {
			
			URI hostElementPath = ((IJavaScriptElement)inputElement).getHostPath(); 
			
			if(hostElementPath!=null) {
				/* See if we can resolve the URI on the workspace */
				IResource realFile = ((IJavaScriptElement)inputElement).getJavaScriptProject().getProject().getWorkspace().getRoot().getFileForLocation(new Path(hostElementPath.getPath()));
				if(realFile==null || !realFile.exists()) {
					realFile = ((IJavaScriptElement)inputElement).getJavaScriptProject().getProject().getWorkspace().getRoot().findMember(hostElementPath.getPath());
				}
				if(realFile!=null) return openInEditor((IFile)realFile, activate);
				return openInEditor(hostElementPath, activate);
			}
			
		}

		/*
		 * Support to navigate inside non-primary working copy.
		 * For now we only support to navigate inside the currently
		 * active editor.
		 * 
		 * XXX: once we have FileStoreEditorInput as API,
		 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=111887
		 * we can fix this code by creating the correct editor input
		 * in getEditorInput(Object)  
		 */
		if (inputElement instanceof IJavaScriptElement) {
			IJavaScriptUnit cu= (IJavaScriptUnit)((IJavaScriptElement)inputElement).getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
			if (cu != null && !JavaModelUtil.isPrimary(cu)) {
				IWorkbenchPage page= JavaScriptPlugin.getActivePage();
				if (page != null) {
					IEditorPart editor= page.getActiveEditor();
					if (editor != null) {
						IJavaScriptElement editorCU= EditorUtility.getEditorInputJavaElement(editor, false);
						if (cu.equals(editorCU)) {
							if (activate && page.getActivePart() != editor)
								page.activate(editor);
							return editor;
						}
					}
				}
			}
		}

		IEditorInput input= getEditorInput(inputElement);
		if (input == null)
			throwPartInitException(JavaEditorMessages.EditorUtility_no_editorInput);
		
		return openInEditor(input, getEditorID(input), activate);
	}

	/**
	 * Selects a Java Element in an editor
	 */
	public static void revealInEditor(IEditorPart part, IJavaScriptElement element) {
		if (element == null)
			return;

		// only change selection if the part is not active
		if (part instanceof JavaEditor) {
			((JavaEditor) part).setSelection(element);
			return;
		}

		// Support for non-Java editor
		try {
			ISourceRange range= null;
			if (element instanceof IJavaScriptUnit)
				range= null;
			else if (element instanceof IClassFile)
				range= null;
			else if (element instanceof ILocalVariable)
				range= ((ILocalVariable)element).getNameRange();
			else if (element instanceof IMember)
				range= ((IMember)element).getNameRange();
			else if (element instanceof ISourceReference)
				range= ((ISourceReference)element).getSourceRange();

			if (range != null)
				revealInEditor(part, range.getOffset(), range.getLength());
		} catch (JavaScriptModelException e) {
			// don't reveal
		}
	}

	/**
	 * Selects and reveals the given region in the given editor part.
	 */
	public static void revealInEditor(IEditorPart part, IRegion region) {
		if (part != null && region != null)
			revealInEditor(part, region.getOffset(), region.getLength());
	}

	/**
	 * Selects and reveals the given offset and length in the given editor part.
	 */
	public static void revealInEditor(IEditorPart editor, final int offset, final int length) {
		if (editor instanceof ITextEditor) {
			((ITextEditor)editor).selectAndReveal(offset, length);
			return;
		}

		// Support for non-text editor - try IGotoMarker interface
		 if (editor instanceof IGotoMarker) {
			final IEditorInput input= editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				final IGotoMarker gotoMarkerTarget= (IGotoMarker)editor;
				WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
					protected void execute(IProgressMonitor monitor) throws CoreException {
						IMarker marker= null;
						try {
							marker= ((IFileEditorInput)input).getFile().createMarker(IMarker.TEXT);
							marker.setAttribute(IMarker.CHAR_START, offset);
							marker.setAttribute(IMarker.CHAR_END, offset + length);

							gotoMarkerTarget.gotoMarker(marker);

						} finally {
							if (marker != null)
								marker.delete();
						}
					}
				};

				try {
					op.run(null);
				} catch (InvocationTargetException ex) {
					// reveal failed
				} catch (InterruptedException e) {
					Assert.isTrue(false, "this operation can not be canceled"); //$NON-NLS-1$
				}
			}
			return;
		}

		/*
		 * Workaround: send out a text selection
		 * XXX: Needs to be improved, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=32214
		 */
		if (editor != null && editor.getEditorSite().getSelectionProvider() != null) {
			IEditorSite site= editor.getEditorSite();
			if (site == null)
				return;

			ISelectionProvider provider= editor.getEditorSite().getSelectionProvider();
			if (provider == null)
				return;

			provider.setSelection(new TextSelection(offset, length));
		}
	}

	private static IEditorPart openInEditor(IFile file, boolean activate) throws PartInitException {
		if (file == null)
			throwPartInitException(JavaEditorMessages.EditorUtility_file_must_not_be_null);
		
		IWorkbenchPage p= JavaScriptPlugin.getActivePage();
		if (p == null)
			throwPartInitException(JavaEditorMessages.EditorUtility_no_active_WorkbenchPage);
		
		IEditorPart editorPart= IDE.openEditor(p, file, activate);
		initializeHighlightRange(editorPart);
		return editorPart;
	}
	
	private static IEditorPart openInEditor(URI file, boolean activate) throws PartInitException{
		if (file == null)
			throwPartInitException(JavaEditorMessages.EditorUtility_file_must_not_be_null);
		
		IWorkbenchPage p= JavaScriptPlugin.getActivePage();
		if (p == null)
			throwPartInitException(JavaEditorMessages.EditorUtility_no_active_WorkbenchPage);
		
		   IEditorDescriptor desc = PlatformUI.getWorkbench().
		      getEditorRegistry().getDefaultEditor(file.getPath());
		if(desc==null) {
			throwPartInitException(JavaEditorMessages.EditorUtility_cantFindEditor + file.toString());
		}
		IEditorPart editorPart= IDE.openEditor(p, file, desc.getId(), activate);
		initializeHighlightRange(editorPart);
		return editorPart;
	}

	private static IEditorPart openInEditor(IEditorInput input, String editorID, boolean activate) throws PartInitException {
		Assert.isNotNull(input);
		Assert.isNotNull(editorID);

		IWorkbenchPage p= JavaScriptPlugin.getActivePage();
		if (p == null)
			throwPartInitException(JavaEditorMessages.EditorUtility_no_active_WorkbenchPage);

		IEditorPart editorPart= p.openEditor(input, editorID, activate);
		initializeHighlightRange(editorPart);
		return editorPart;
	}

	private static void throwPartInitException(String message) throws PartInitException {
		IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.OK, message, null);
		throw new PartInitException(status);
	}

	private static void initializeHighlightRange(IEditorPart editorPart) {
		if (editorPart instanceof ITextEditor) {
			IAction toggleAction= editorPart.getEditorSite().getActionBars().getGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY);
			boolean enable= toggleAction != null; 
			if (enable && editorPart instanceof JavaEditor)
				enable= JavaScriptPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SHOW_SEGMENTS);
			else
				enable= enable && toggleAction.isEnabled() && toggleAction.isChecked();
			if (enable) {
				if (toggleAction instanceof TextEditorAction) {
					// Reset the action
					((TextEditorAction)toggleAction).setEditor(null);
					// Restore the action
					((TextEditorAction)toggleAction).setEditor((ITextEditor)editorPart);
				} else {
					// Uncheck
					toggleAction.run();
					// Check
					toggleAction.run();
				}
			}
		}
	}

	private static String getEditorID(IEditorInput input) throws PartInitException {
		Assert.isNotNull(input);
		IEditorDescriptor editorDescriptor;
		if (input instanceof IFileEditorInput)
			editorDescriptor= IDE.getEditorDescriptor(((IFileEditorInput)input).getFile());
		else if (input instanceof InternalClassFileEditorInput )
			return JavaScriptUI.ID_CF_EDITOR;
		else {
			String name= input.getName();
			if (name == null)
				throwPartInitException(JavaEditorMessages.EditorUtility_could_not_find_editorId);
			editorDescriptor= IDE.getEditorDescriptor(name);
		}
		return editorDescriptor.getId();
	}

	/**
	 * Returns the given editor's input as Java element.
	 *
	 * @param editor the editor
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * @return the given editor's input as Java element or <code>null</code> if none
	 * 
	 */
	public static IJavaScriptElement getEditorInputJavaElement(IEditorPart editor, boolean primaryOnly) {
		Assert.isNotNull(editor);
		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput == null)
			return null;
		
		IJavaScriptElement je= JavaScriptUI.getEditorInputJavaElement(editorInput);
		if (je != null || primaryOnly)
			return je;

		return  JavaScriptPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput, false);
	}

	private static IEditorInput getEditorInput(IJavaScriptElement element) throws JavaScriptModelException {
		while (element != null) {
			if (element instanceof IJavaScriptUnit) {
				IJavaScriptUnit unit= ((IJavaScriptUnit) element).getPrimary();
					IResource resource= unit.getResource();
					if (resource instanceof IFile)
						return new FileEditorInput((IFile) resource);
			}

			if (element instanceof IClassFile)
			{
				String elementName = element.getElementName();
				if (Util.isMetadataFileName(elementName) || Util.isJavaLikeFileName(elementName))
				{
					IResource resource=element.getResource();
					if (resource instanceof IFile && !org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(resource.getName()))
								return new FileEditorInput((IFile) resource);

				}
				return new InternalClassFileEditorInput((IClassFile) element);
			}

			element= element.getParent();
		}

		return null;
	}

	public static IEditorInput getEditorInput(Object input) throws JavaScriptModelException {
		if (input instanceof IJavaScriptElement)
			return getEditorInput((IJavaScriptElement) input);

		if (input instanceof IFile)
			return new FileEditorInput((IFile) input);

		if (JavaModelUtil.isOpenableStorage(input))
			return new JarEntryEditorInput((IStorage)input);

		return null;
	}

	/**
	 * If the current active editor edits a java element return it, else
	 * return null
	 */
	public static IJavaScriptElement getActiveEditorJavaInput() {
		IWorkbenchPage page= JavaScriptPlugin.getActivePage();
		if (page != null) {
			IEditorPart part= page.getActiveEditor();
			if (part != null) {
				IEditorInput editorInput= part.getEditorInput();
				if (editorInput != null) {
					return JavaScriptUI.getEditorInputJavaElement(editorInput);
				}
			}
		}
		return null;
	}

	/**
	 * Maps the localized modifier name to a code in the same
	 * manner as #findModifier.
	 *
	 * @param modifierName the modifier name
	 * @return the SWT modifier bit, or <code>0</code> if no match was found
	 * 
	 */
	public static int findLocalizedModifier(String modifierName) {
		if (modifierName == null)
			return 0;

		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.CTRL)))
			return SWT.CTRL;
		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.SHIFT)))
			return SWT.SHIFT;
		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.ALT)))
			return SWT.ALT;
		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.COMMAND)))
			return SWT.COMMAND;

		return 0;
	}

	/**
	 * Returns the modifier string for the given SWT modifier
	 * modifier bits.
	 *
	 * @param stateMask	the SWT modifier bits
	 * @return the modifier string
	 * 
	 */
	public static String getModifierString(int stateMask) {
		String modifierString= ""; //$NON-NLS-1$
		if ((stateMask & SWT.CTRL) == SWT.CTRL)
			modifierString= appendModifierString(modifierString, SWT.CTRL);
		if ((stateMask & SWT.ALT) == SWT.ALT)
			modifierString= appendModifierString(modifierString, SWT.ALT);
		if ((stateMask & SWT.SHIFT) == SWT.SHIFT)
			modifierString= appendModifierString(modifierString, SWT.SHIFT);
		if ((stateMask & SWT.COMMAND) == SWT.COMMAND)
			modifierString= appendModifierString(modifierString,  SWT.COMMAND);

		return modifierString;
	}

	/**
	 * Appends to modifier string of the given SWT modifier bit
	 * to the given modifierString.
	 *
	 * @param modifierString	the modifier string
	 * @param modifier			an int with SWT modifier bit
	 * @return the concatenated modifier string
	 * 
	 */
	private static String appendModifierString(String modifierString, int modifier) {
		if (modifierString == null)
			modifierString= ""; //$NON-NLS-1$
		String newModifierString= Action.findModifierString(modifier);
		if (modifierString.length() == 0)
			return newModifierString;
		return Messages.format(JavaEditorMessages.EditorUtility_concatModifierStrings, new String[] {modifierString, newModifierString});
	}

	/**
	 * Returns the Java project for a given editor input or <code>null</code> if no corresponding
	 * Java project exists.
	 *
	 * @param input the editor input
	 * @return the corresponding Java project
	 *
	 * 
	 */
	public static IJavaScriptProject getJavaProject(IEditorInput input) {
		IJavaScriptProject jProject= null;
		if (input instanceof IFileEditorInput) {
			IProject project= ((IFileEditorInput)input).getFile().getProject();
			if (project != null) {
				jProject= JavaScriptCore.create(project);
				if (!jProject.exists())
					jProject= null;
			}
		} else if (input instanceof IClassFileEditorInput) {
			jProject= ((IClassFileEditorInput)input).getClassFile().getJavaScriptProject();
		}
		return jProject;
	}
	
	/**
	 * Returns an array of all editors that have an unsaved content. If the identical content is 
	 * presented in more than one editor, only one of those editor parts is part of the result.
	 * 
	 * @return an array of all dirty editor parts.
	 * 
	 */
	public static IEditorPart[] getDirtyEditors() {
		Set inputs= new HashSet();
		List result= new ArrayList(0);
		IWorkbench workbench= PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorPart[] editors= pages[x].getDirtyEditors();
				for (int z= 0; z < editors.length; z++) {
					IEditorPart ep= editors[z];
					IEditorInput input= ep.getEditorInput();
					if (inputs.add(input))
						result.add(ep);
				}
			}
		}
		return (IEditorPart[])result.toArray(new IEditorPart[result.size()]);
	}
	
	/**
	 * Returns the editors to save before performing global Java-related
	 * operations.
	 * 
	 * @param saveUnknownEditors <code>true</code> iff editors with unknown buffer management should also be saved
	 * @return the editors to save
	 * 
	 */
	public static IEditorPart[] getDirtyEditorsToSave(boolean saveUnknownEditors) {
		Set inputs= new HashSet();
		List result= new ArrayList(0);
		IWorkbench workbench= PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorPart[] editors= pages[x].getDirtyEditors();
				for (int z= 0; z < editors.length; z++) {
					IEditorPart ep= editors[z];
					IEditorInput input= ep.getEditorInput();
					if (!mustSaveDirtyEditor(ep, input, saveUnknownEditors))
						continue;
					
					if (inputs.add(input))
						result.add(ep);
				}
			}
		}
		return (IEditorPart[])result.toArray(new IEditorPart[result.size()]);
	}

	/**
	 * 
	 */
	private static boolean mustSaveDirtyEditor(IEditorPart ep, IEditorInput input, boolean saveUnknownEditors) {
		/*
		 * Goal: save all editors that could interfere with refactoring operations.
		 * 
		 * If <code>saveUnknownEditors</code> is <code>false</code>, save all editors
		 * for compilation units that are not working copies.
		 * 
		 * If <code>saveUnknownEditors</code> is <code>true</code>, save all editors
		 * whose implementation is probably not based on file buffers.
		 */
		IResource resource= (IResource) input.getAdapter(IResource.class);
		if (resource == null)
			return saveUnknownEditors;

		IJavaScriptElement javaElement= JavaScriptCore.create(resource);
		if (javaElement instanceof IJavaScriptUnit) {
			IJavaScriptUnit cu= (IJavaScriptUnit) javaElement;
			if (!cu.isWorkingCopy()) {
				return true;
			}
		}
		
		if (! (ep instanceof ITextEditor))
			return saveUnknownEditors;
		
		ITextEditor textEditor= (ITextEditor) ep;
		IDocumentProvider documentProvider= textEditor.getDocumentProvider();
		if (! (documentProvider instanceof TextFileDocumentProvider))
			return saveUnknownEditors;
		
		return false;
	}

}
