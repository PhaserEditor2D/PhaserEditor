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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.wst.jsdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.wst.jsdt.internal.corext.buildpath.IBuildpathModifierListener;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public abstract class BuildpathModifierAction extends Action implements ISelectionChangedListener {
	
	public static final int ADD_SEL_SF_TO_BP= 0;
	public static final int REMOVE_FROM_BP= 1;
	public static final int EXCLUDE= 2;
	public static final int UNEXCLUDE= 3;
	public static final int EDIT_FILTERS= 4;
	public static final int CREATE_LINK= 5;
	public static final int RESET_ALL= 6;
	public static final int RESET= 9;
	public static final int INCLUDE= 10;
	public static final int UNINCLUDE= 11;
	public static final int CREATE_FOLDER= 12;
	public static final int ADD_JAR_TO_BP= 13;
	public static final int ADD_LIB_TO_BP= 14;
	public static final int ADD_SEL_LIB_TO_BP= 15;
	public static final int CONFIGURE_BUILD_PATH= 16;
	public static final int DROP_DOWN_ACTION= 18;

	private final IWorkbenchSite fSite;
	private final List fSelectedElements;
	private final ISetSelectionTarget fSelectionTarget;
	private final List fListeners;

	public BuildpathModifierAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, int id) {
		this(site, selectionTarget, id, IAction.AS_PUSH_BUTTON);
    }
	
	public BuildpathModifierAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, int id, int style) {
		super("", style); //$NON-NLS-1$
		
		fSite= site;
		fSelectionTarget= selectionTarget;
		fSelectedElements= new ArrayList();
		fListeners= new ArrayList();
		
		setId(Integer.toString(id));
    }

	/**
	 * A detailed description usable for a {@link org.eclipse.ui.forms.widgets.FormText} 
	 * depending on the current selection, or <code>null</code>
	 * if <code>!enabled()</code>
	 * 
	 * @return A detailed description or null if <code>!enabled()</code>
	 */
	public abstract String getDetailedDescription();
	
	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(final SelectionChangedEvent event) {
		final ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			setEnabled(canHandle((IStructuredSelection) selection));
			fSelectedElements.clear();
			fSelectedElements.addAll(((IStructuredSelection)selection).toList());
		} else {
			setEnabled(canHandle(StructuredSelection.EMPTY));
			fSelectedElements.clear();
		}
	}

	protected abstract boolean canHandle(IStructuredSelection elements);
	
	protected List getSelectedElements() {
		return fSelectedElements;
	}
	
	public void addBuildpathModifierListener(IBuildpathModifierListener listener) {
		fListeners.add(listener);
	}
	
	public void removeBuildpathModifierListener(IBuildpathModifierListener listener) {
		fListeners.remove(listener);
	}

	protected void informListeners(BuildpathDelta delta) {
		for (Iterator iterator= fListeners.iterator(); iterator.hasNext();) {
	        ((IBuildpathModifierListener)iterator.next()).buildpathChanged(delta);
        }	
	}
	
	protected Shell getShell() {
		if (fSite == null)
			return JavaScriptPlugin.getActiveWorkbenchShell();
		
	    return fSite.getShell() != null ? fSite.getShell() : JavaScriptPlugin.getActiveWorkbenchShell();
    }
	
	protected void showExceptionDialog(CoreException exception, String title) {
		showError(exception, getShell(), title, exception.getMessage());
	}
	
	protected void showError(CoreException e, Shell shell, String title, String message) {
		IStatus status= e.getStatus();
		if (status != null) {
			ErrorDialog.openError(shell, message, title, status);
		} else {
			MessageDialog.openError(shell, title, message);
		}
	}
	
	protected void selectAndReveal(final ISelection selection) {
		if (fSelectionTarget != null)
			fSelectionTarget.selectReveal(selection);
		
		if (fSite == null)
			return;
			
		// validate the input
		IWorkbenchPage page= fSite.getPage();
		if (page == null)
			return;

		// get all the view and editor parts
		List parts= new ArrayList();
		IWorkbenchPartReference refs[]= page.getViewReferences();
		for (int i= 0; i < refs.length; i++) {
			IWorkbenchPart part= refs[i].getPart(false);
			if (part != null)
				parts.add(part);
		}
		refs= page.getEditorReferences();
		for (int i= 0; i < refs.length; i++) {
			if (refs[i].getPart(false) != null)
				parts.add(refs[i].getPart(false));
		}

		Iterator itr= parts.iterator();
		while (itr.hasNext()) {
			IWorkbenchPart part= (IWorkbenchPart) itr.next();

			// get the part's ISetSelectionTarget implementation
			ISetSelectionTarget target= null;
			if (part instanceof ISetSelectionTarget)
				target= (ISetSelectionTarget) part;
			else
				target= (ISetSelectionTarget) part.getAdapter(ISetSelectionTarget.class);

			if (target != null) {
				// select and reveal resource
				final ISetSelectionTarget finalTarget= target;
				page.getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						finalTarget.selectReveal(selection);
					}
				});
			}
		}
	}
}
