/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.wst.jsdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaStatusConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class CallHierarchyUI {
    private static final int DEFAULT_MAX_CALL_DEPTH= 10;    
    private static final String PREF_MAX_CALL_DEPTH = "PREF_MAX_CALL_DEPTH"; //$NON-NLS-1$

    private static CallHierarchyUI fgInstance;

    private CallHierarchyUI() {
        // Do nothing
    }

    public static CallHierarchyUI getDefault() {
        if (fgInstance == null) {
            fgInstance = new CallHierarchyUI();
        }

        return fgInstance;
    }

    /**
     * Returns the maximum tree level allowed
     * @return int
     */
    public int getMaxCallDepth() {
        int maxCallDepth;
        
        IPreferenceStore settings = JavaScriptPlugin.getDefault().getPreferenceStore();
        maxCallDepth = settings.getInt(PREF_MAX_CALL_DEPTH);
        if (maxCallDepth < 1 || maxCallDepth > 99) {
            maxCallDepth= DEFAULT_MAX_CALL_DEPTH;
        }

        return maxCallDepth;
    }

    public void setMaxCallDepth(int maxCallDepth) {
        IPreferenceStore settings = JavaScriptPlugin.getDefault().getPreferenceStore();
        settings.setValue(PREF_MAX_CALL_DEPTH, maxCallDepth);
    }
    
    public static void jumpToMember(IJavaScriptElement element) {
        if (element != null) {
            try {
                JavaScriptUI.openInEditor(element, true, true);
            } catch (JavaScriptModelException e) {
                JavaScriptPlugin.log(e);
            } catch (PartInitException e) {
                JavaScriptPlugin.log(e);
            }
        }
    }

    public static void jumpToLocation(CallLocation callLocation) {
        try {
            IEditorPart methodEditor = JavaScriptUI.openInEditor(callLocation.getMember(), false, false);
            if (methodEditor instanceof ITextEditor) {
                ITextEditor editor = (ITextEditor) methodEditor;
                editor.selectAndReveal(callLocation.getStart(),
                    (callLocation.getEnd() - callLocation.getStart()));
            }
        } catch (JavaScriptModelException e) {
            JavaScriptPlugin.log(e);
        } catch (PartInitException e) {
            JavaScriptPlugin.log(e);
        }
    }

    /**
     * @return <code>true</code> iff no error occurred while trying to
     *  open the editor, <code>false</code> iff an error dialog was raised.
     */
    public static boolean openInEditor(Object element, Shell shell, String title) {
        CallLocation callLocation= CallHierarchy.getCallLocation(element);
        
        try {
	        IMember enclosingMember;
	        int selectionStart;
			int selectionLength;
			
	        if (callLocation != null) {
				enclosingMember= callLocation.getMember();
				selectionStart= callLocation.getStart();
				selectionLength= callLocation.getEnd() - selectionStart;
	        } else if (element instanceof MethodWrapper) {
	        	enclosingMember= ((MethodWrapper) element).getMember();
	        	if(enclosingMember.isVirtual()) {
	        		selectionStart = 0;
	        		selectionLength = 0;
	        		
	        	}else {
		        	ISourceRange selectionRange= enclosingMember.getNameRange();
		        	if (selectionRange == null)
		        		selectionRange= enclosingMember.getSourceRange();
		        	if (selectionRange == null)
		        		return true;
		        	selectionStart= selectionRange.getOffset();
		        	selectionLength= selectionRange.getLength();
	        	}
	        } else {
	            return true;
	        }
	
            boolean activateOnOpen = OpenStrategy.activateOnOpen();

			IEditorPart methodEditor = JavaScriptUI.openInEditor(enclosingMember, activateOnOpen, false);
            if (methodEditor instanceof ITextEditor) {
                ITextEditor editor = (ITextEditor) methodEditor;
				editor.selectAndReveal(selectionStart, selectionLength);
            }
            return true;
        } catch (JavaScriptModelException e) {
            JavaScriptPlugin.log(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(),
                    IJavaStatusConstants.INTERNAL_ERROR,
                    CallHierarchyMessages.CallHierarchyUI_open_in_editor_error_message, e)); 

            ErrorDialog.openError(shell, title,
                CallHierarchyMessages.CallHierarchyUI_open_in_editor_error_message, 
                e.getStatus());
            return false;
        } catch (PartInitException x) {
            String name;
        	if (callLocation != null)
        		name= callLocation.getCalledMember().getElementName();
        	else if (element instanceof MethodWrapper)
        		name= ((MethodWrapper) element).getName();
        	else
        		name= "";  //$NON-NLS-1$
            MessageDialog.openError(shell, title,
                Messages.format(
                    CallHierarchyMessages.CallHierarchyUI_open_in_editor_error_messageArgs, 
                    new String[] { name, x.getMessage() }));
            return false;
        }
    }

    public static IEditorPart isOpenInEditor(Object elem) {
        IJavaScriptElement javaElement= null;
        if (elem instanceof MethodWrapper) {
            javaElement= ((MethodWrapper) elem).getMember();
        } else if (elem instanceof CallLocation) {
            javaElement= ((CallLocation) elem).getCalledMember();
        }
        if (javaElement != null) {
            return EditorUtility.isOpenInEditor(javaElement);
        }
        return null;
    }

    /**
     * Converts the input to a possible input candidates
     */ 
    public static IJavaScriptElement[] getCandidates(Object input) {
        if (!(input instanceof IJavaScriptElement)) {
            return null;
        }
        IJavaScriptElement elem= (IJavaScriptElement) input;
        if (elem.getElementType() == IJavaScriptElement.METHOD) {
            return new IJavaScriptElement[] { elem };
        }
        return null;    
    }
    
    public static CallHierarchyViewPart open(IJavaScriptElement[] candidates, IWorkbenchWindow window) {
        Assert.isTrue(candidates != null && candidates.length != 0);
            
        IJavaScriptElement input= null;
        if (candidates.length > 1) {
            String title= CallHierarchyMessages.CallHierarchyUI_selectionDialog_title;  
            String message= CallHierarchyMessages.CallHierarchyUI_selectionDialog_message; 
            input= SelectionConverter.selectJavaElement(candidates, window.getShell(), title, message);         
        } else {
            input= candidates[0];
        }
        if (input == null)
            return null;
            
        return openInViewPart(window, input);
    }
    
    private static CallHierarchyViewPart openInViewPart(IWorkbenchWindow window, IJavaScriptElement input) {
        IWorkbenchPage page= window.getActivePage();
        try {
            CallHierarchyViewPart result= (CallHierarchyViewPart)page.showView(CallHierarchyViewPart.ID_CALL_HIERARCHY);
            result.setMethod((IFunction)input);
            return result;
        } catch (CoreException e) {
            ExceptionHandler.handle(e, window.getShell(), 
                CallHierarchyMessages.CallHierarchyUI_error_open_view, e.getMessage()); 
        }
        return null;        
    }
    
    /**
     * Converts an ISelection (containing MethodWrapper instances) to an ISelection
     * with the MethodWrapper's replaced by their corresponding IMembers. If the selection
     * contains elements which are not MethodWrapper instances or not already IMember instances
     * they are discarded.  
     * @param selection The selection to convert.
     * @return An ISelection containing IMember's in place of MethodWrapper instances.
     */
    static ISelection convertSelection(ISelection selection) {
        if (selection.isEmpty()) {
            return selection;   
        }
        
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection= (IStructuredSelection) selection;
            List javaElements= new ArrayList();
            for (Iterator iter= structuredSelection.iterator(); iter.hasNext();) {
                Object element= iter.next();
                if (element instanceof MethodWrapper) {
                    IMember member= ((MethodWrapper)element).getMember();
                    if (member != null) {
                        javaElements.add(member);
                    }
                } else if (element instanceof IMember) {
                    javaElements.add(element);
                } else if (element instanceof CallLocation) {
                    IMember member = ((CallLocation) element).getMember();
                    javaElements.add(member);
                }
            }
            return new StructuredSelection(javaElements);
        }
        return StructuredSelection.EMPTY; 
    }
}
