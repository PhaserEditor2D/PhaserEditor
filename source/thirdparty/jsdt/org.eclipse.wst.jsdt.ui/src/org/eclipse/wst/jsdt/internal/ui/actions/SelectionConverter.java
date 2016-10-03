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

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.ICodeAssist;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

public class SelectionConverter {

	private static final IJavaScriptElement[] EMPTY_RESULT= new IJavaScriptElement[0];
	
	private SelectionConverter() {
		// no instance
	}

	/**
	 * Converts the selection provided by the given part into a structured selection.
	 * The following conversion rules are used:
	 * <ul>
	 *	<li><code>part instanceof JavaEditor</code>: returns a structured selection
	 * 	using code resolve to convert the editor's text selection.</li>
	 * <li><code>part instanceof IWorkbenchPart</code>: returns the part's selection
	 * 	if it is a structured selection.</li>
	 * <li><code>default</code>: returns an empty structured selection.</li>
	 * </ul>
	 */
	public static IStructuredSelection getStructuredSelection(IWorkbenchPart part) throws JavaScriptModelException {
		if (part instanceof JavaEditor)
			return new StructuredSelection(codeResolve((JavaEditor)part));
		ISelectionProvider provider= part.getSite().getSelectionProvider();
		if (provider != null) {
			ISelection selection= provider.getSelection();
			if (selection instanceof IStructuredSelection)
				return (IStructuredSelection)selection;
		}
		return StructuredSelection.EMPTY;
	}

	
	/**
	 * Converts the given structured selection into an array of Java elements.
	 * An empty array is returned if one of the elements stored in the structured
	 * selection is not of type <code>IJavaScriptElement</code>
	 */
	public static IJavaScriptElement[] getElements(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			IJavaScriptElement[] result= new IJavaScriptElement[selection.size()];
			int i= 0;
			for (Iterator iter= selection.iterator(); iter.hasNext(); i++) {
				Object element= iter.next();
				if (!(element instanceof IJavaScriptElement))
					return EMPTY_RESULT;
				result[i]= (IJavaScriptElement)element;
			}
			return result;
		}
		return EMPTY_RESULT;
	}

	public static boolean canOperateOn(JavaEditor editor) {
		if (editor == null)
			return false;
		return getInput(editor) != null;
		
	}
		
	public static IJavaScriptElement[] codeResolveOrInputForked(JavaEditor editor) throws InvocationTargetException, InterruptedException {
		IJavaScriptElement input= getInput(editor);
		ITextSelection selection= (ITextSelection)editor.getSelectionProvider().getSelection();
		IJavaScriptElement[] result= performForkedCodeResolve(input, selection);
		if (result.length == 0) {
			result= new IJavaScriptElement[] {input};
		}
		return result;
	}
				
	public static IJavaScriptElement[] codeResolve(JavaEditor editor) throws JavaScriptModelException {
		return codeResolve(editor, true);
	}
		
	/**
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * 
	 */
	public static IJavaScriptElement[] codeResolve(JavaEditor editor, boolean primaryOnly) throws JavaScriptModelException {
		return codeResolve(getInput(editor, primaryOnly), (ITextSelection)editor.getSelectionProvider().getSelection());
	}
	
	/**
	 * Perform a code resolve in a separate thread.
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 * 
	 */
	public static IJavaScriptElement[] codeResolveForked(JavaEditor editor, boolean primaryOnly) throws InvocationTargetException, InterruptedException {
		return performForkedCodeResolve(getInput(editor, primaryOnly), (ITextSelection)editor.getSelectionProvider().getSelection());
	}
			
	public static IJavaScriptElement getElementAtOffset(JavaEditor editor) throws JavaScriptModelException {
		return getElementAtOffset(editor, true);
	}
	
	/**
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * 
	 */
	private static IJavaScriptElement getElementAtOffset(JavaEditor editor, boolean primaryOnly) throws JavaScriptModelException {
		return getElementAtOffset(getInput(editor, primaryOnly), (ITextSelection)editor.getSelectionProvider().getSelection());
	}
	
	public static IType getTypeAtOffset(JavaEditor editor) throws JavaScriptModelException {
		IJavaScriptElement element= SelectionConverter.getElementAtOffset(editor);
		IType type= (IType)element.getAncestor(IJavaScriptElement.TYPE);
		if (type == null) {
			IJavaScriptUnit unit= SelectionConverter.getInputAsCompilationUnit(editor);
			if (unit != null)
				type= unit.findPrimaryType();
		}
		return type;
	}
	
	public static IJavaScriptElement getInput(JavaEditor editor) {
		return getInput(editor, true);
	}
	
	/**
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * 
	 */
	private static IJavaScriptElement getInput(JavaEditor editor, boolean primaryOnly) {
		if (editor == null)
			return null;
		return EditorUtility.getEditorInputJavaElement(editor, primaryOnly);
	}
	
	public static ITypeRoot getInputAsTypeRoot(JavaEditor editor) {
		Object editorInput= SelectionConverter.getInput(editor);
		if (editorInput instanceof ITypeRoot)
			return (ITypeRoot)editorInput;
		return null;
	}
	
	public static IJavaScriptUnit getInputAsCompilationUnit(JavaEditor editor) {
		Object editorInput= SelectionConverter.getInput(editor);
		if (editorInput instanceof IJavaScriptUnit)
			return (IJavaScriptUnit)editorInput;
		return null;
	}

	public static IClassFile getInputAsClassFile(JavaEditor editor) {
		Object editorInput= SelectionConverter.getInput(editor);
		if (editorInput instanceof IClassFile)
			return (IClassFile)editorInput;
		return null;
	}

	private static IJavaScriptElement[] performForkedCodeResolve(final IJavaScriptElement input, final ITextSelection selection) throws InvocationTargetException, InterruptedException {
		final class CodeResolveRunnable implements IRunnableWithProgress {
			IJavaScriptElement[] result;
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					result= codeResolve(input, selection);
				} catch (JavaScriptModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		}
		CodeResolveRunnable runnable= new CodeResolveRunnable();
		PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		return runnable.result;
	}

	public static IJavaScriptElement[] codeResolve(IJavaScriptElement input, ITextSelection selection) throws JavaScriptModelException {
			if (input instanceof ICodeAssist) {
				if (input instanceof IJavaScriptUnit) {
					JavaModelUtil.reconcile((IJavaScriptUnit) input);
				}
				IJavaScriptElement[] elements= ((ICodeAssist)input).codeSelect(selection.getOffset() + selection.getLength(), 0);
				if (elements.length > 0) {
					return elements;
				}
			}
			return EMPTY_RESULT;
	}
	
	public static IJavaScriptElement getElementAtOffset(IJavaScriptElement input, ITextSelection selection) throws JavaScriptModelException {
		if (input instanceof IJavaScriptUnit) {
			IJavaScriptUnit cunit= (IJavaScriptUnit) input;
			JavaModelUtil.reconcile(cunit);
			IJavaScriptElement ref= cunit.getElementAt(selection.getOffset());
			if (ref == null)
				return input;
			else
				return ref;
		} else if (input instanceof IClassFile) {
			IJavaScriptElement ref= ((IClassFile)input).getElementAt(selection.getOffset());
			if (ref == null)
				return input;
			else
				return ref;
		}
		return null;
	}
	
//	public static IJavaScriptElement[] resolveSelectedElements(IJavaScriptElement input, ITextSelection selection) throws JavaScriptModelException {
//		IJavaScriptElement enclosing= resolveEnclosingElement(input, selection);
//		if (enclosing == null)
//			return EMPTY_RESULT;
//		if (!(enclosing instanceof ISourceReference))
//			return EMPTY_RESULT;
//		ISourceRange sr= ((ISourceReference)enclosing).getSourceRange();
//		if (selection.getOffset() == sr.getOffset() && selection.getLength() == sr.getLength())
//			return new IJavaScriptElement[] {enclosing};
//	}
	
	public static IJavaScriptElement resolveEnclosingElement(JavaEditor editor, ITextSelection selection) throws JavaScriptModelException {
		return resolveEnclosingElement(getInput(editor), selection);
	}
	
	public static IJavaScriptElement resolveEnclosingElement(IJavaScriptElement input, ITextSelection selection) throws JavaScriptModelException {
		IJavaScriptElement atOffset= null;
		if (input instanceof IJavaScriptUnit) {
			IJavaScriptUnit cunit= (IJavaScriptUnit)input;
			JavaModelUtil.reconcile(cunit);
			atOffset= cunit.getElementAt(selection.getOffset());
		} else if (input instanceof IClassFile) {
			IClassFile cfile= (IClassFile)input;
			atOffset= cfile.getElementAt(selection.getOffset());
		} else {
			return null;
		}
		if (atOffset == null) {
			return input;
		} else {
			int selectionEnd= selection.getOffset() + selection.getLength();
			IJavaScriptElement result= atOffset;
			if (atOffset instanceof ISourceReference) {
				ISourceRange range= ((ISourceReference)atOffset).getSourceRange();
				while (range.getOffset() + range.getLength() < selectionEnd) {
					result= result.getParent();
					if (! (result instanceof ISourceReference)) {
						result= input;
						break;
					}
					range= ((ISourceReference)result).getSourceRange();
				}
			}
			return result;
		}
	}

	/**
	 * Shows a dialog for resolving an ambiguous java element.
	 * Utility method that can be called by subclasses.
	 */
	public static IJavaScriptElement selectJavaElement(IJavaScriptElement[] elements, Shell shell, String title, String message) {
		int nResults= elements.length;
		if (nResults == 0)
			return null;
		if (nResults == 1)
			return elements[0];
		
		int flags= JavaScriptElementLabelProvider.SHOW_DEFAULT | JavaScriptElementLabelProvider.SHOW_POST_QUALIFIED | JavaScriptElementLabelProvider.SHOW_ROOT;
						
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaScriptElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(elements);
		
		if (dialog.open() == Window.OK) {
			return (IJavaScriptElement) dialog.getFirstResult();
		}		
		return null;
	}
}
