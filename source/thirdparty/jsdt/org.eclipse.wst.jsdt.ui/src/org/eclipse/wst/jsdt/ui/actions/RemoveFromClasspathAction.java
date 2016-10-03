/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Action to remove package fragment roots from the classpath of its parent
 * project. Currently, the action is applicable to selections containing
 * non-external archives (JAR or zip).
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class RemoveFromClasspathAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>RemoveFromClasspathAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type
	 * <code> org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public RemoveFromClasspathAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.RemoveFromClasspathAction_Remove); 
		setToolTipText(ActionMessages.RemoveFromClasspathAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.REMOVE_FROM_CLASSPATH_ACTION);
	}
	
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	private static boolean checkEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			if (! canRemove(iter.next()))
				return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	public void run(final IStructuredSelection selection) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, new WorkbenchRunnableAdapter(new IWorkspaceRunnable() {
				public void run(IProgressMonitor pm) throws CoreException {
					try{
						IPackageFragmentRoot[] roots= getRootsToRemove(selection);
						pm.beginTask(ActionMessages.RemoveFromClasspathAction_Removing, roots.length); 
						for (int i= 0; i < roots.length; i++) {
							int jCoreFlags= IPackageFragmentRoot.NO_RESOURCE_MODIFICATION | IPackageFragmentRoot.ORIGINATING_PROJECT_INCLUDEPATH;
							roots[i].delete(IResource.NONE, jCoreFlags, new SubProgressMonitor(pm, 1));
						}
					} finally {
						pm.done();
					}
				}
		}));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), 
					ActionMessages.RemoveFromClasspathAction_exception_dialog_title, 
					ActionMessages.RemoveFromClasspathAction_Problems_occurred); 
		} catch (InterruptedException e) {
			// canceled
		}
	}
	
	private static IPackageFragmentRoot[] getRootsToRemove(IStructuredSelection selection){
		List result= new ArrayList(selection.size()); 
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (canRemove(element))
				result.add(element);
		}
		return (IPackageFragmentRoot[]) result.toArray(new IPackageFragmentRoot[result.size()]);
	}	

	private static boolean canRemove(Object element){
		if (! (element instanceof IPackageFragmentRoot))
			return false;
		IPackageFragmentRoot root= (IPackageFragmentRoot)element;
		try {
			IIncludePathEntry cpe= root.getRawIncludepathEntry();
			if (cpe == null || cpe.getEntryKind() == IIncludePathEntry.CPE_CONTAINER)
				return false; // don't want to remove the container if only a child is selected
			return true;
		} catch (JavaScriptModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
		}
		return false;
	}	
}

