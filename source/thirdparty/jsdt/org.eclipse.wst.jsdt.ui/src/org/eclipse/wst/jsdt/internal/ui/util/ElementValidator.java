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
package org.eclipse.wst.jsdt.internal.ui.util;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;

/**
 * Helper class to check if a set of <tt>IJavaScriptElement</tt> objects can be
 * modified by an operation.
 * 
 * 
 */
public class ElementValidator {

	private ElementValidator() {
		// no instance
	}

	/**
	 * Checks if the given element is in sync with the underlying file system.
	 * 
	 * @param element the element to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * element is not in sync
	 * @param title a dialog's title used to present a dialog to the user if the
	 * element is not in sync
	 * @return boolean <code>true</code> if the element is in sync with the file
	 * system. Otherwise <code>false</code> is returned
	 */
	public static boolean checkInSync(IAdaptable element, Shell parent,String title) {
		return checkInSync(new IAdaptable[] {element}, parent, title);
	}
	
	/**
	 * Checks if the given array of elements is in sync with the underlying file
	 * system.
	 * 
	 * @param elements the array of elements to be checked
	 * @param parent a parent shell used to present a dialog to the user if
	 * one of the elements is not in sync
	 * @param title a dialog's title used to present a dialog to the user if
	 * one of the elements is not in sync
	 * @return boolean <code>true</code> if the all elements are in sync with
	 * the file system. Otherwise <code>false</code> is returned
	 */
	public static boolean checkInSync(IAdaptable[] elements, Shell parent, String title) {
		return checkInSync(getResources(elements), parent, title);
	}
	
	/**
	 * Checks if the given element is read-only and if so the methods tries
	 * to make the element writable by calling validate edit. If
	 * <code>validateEdit</code> was able to make the file writable the method
	 * additionally checks if the file has been changed by calling
	 * <code>validateEdit</code>.
	 * 
	 * @param element the element to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * check fails
	 * @param title a dialog's title used to present a dialog to the user if the
	 * check fails
	 * @return boolean <code>true</code> if the element is writable and its
	 * content didn't change by calling <code>validateEdit</code>. Otherwise
	 * <code>false</code> is returned
	 * 
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean checkValidateEdit(IJavaScriptElement element, Shell parent, String title) {
		return checkValidateEdit(new IJavaScriptElement[] {element}, parent, title);
	}
	
	/**
	 * Checks if the given elements are read-only and if so the methods tries to
	 * make the element writable by calling <code>validateEdit</code>. If
	 * <code>validateEdit</code> was able to make the file writable the method
	 * additionally checks if the file has been changed by calling
	 * <code>validateEdit</code>.
	 * 
	 * @param elements the elements to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * check fails
	 * @param title a dialog's title used to present a dialog to the user if the
	 * check fails
	 * @return boolean <code>true</code> if all elements are writable and their
	 * content didn't change by calling <code>validateEdit</code>. Otherwise
	 * <code>false</code> is returned
	 * 
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean checkValidateEdit(IJavaScriptElement[] elements, Shell parent, String title) {
		return checkValidateEdit(getResources(elements), parent, title);
	}
	
	/**
	 * Checks a combination of <code>checkInSync</code> and
	 * <code>checkValidateEdit</code> depending of the value of
	 * <code>editor</code>. If <code>editor</code> is <code>true</code> only
	 * <code>checkValidateEdit</code> is performed since the editor does a in
	 * sync check on focus change. If <code>editor</code> is <code>false</code>
	 * both checks are performed.
	 * 
	 * @param element the element to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * check fails
	 * @param title a dialog's title used to present a dialog to the user if the
	 * check fails
	 * @return boolean <code>true</code> if the element passed the checks.
	 * Otherwise <code>false</code> is returned
	 * 
	 * @see #checkInSync(IAdaptable, Shell, String)
	 * @see #checkValidateEdit(IJavaScriptElement, Shell, String)
	 */
	public static boolean check(IJavaScriptElement element, Shell parent, String title, boolean editor) {
		return check(new IJavaScriptElement[] {element}, parent, title, editor);
	}
	
	/**
	 * Checks a combination of <code>checkInSync</code> and
	 * <code>checkValidateEdit</code> depending of the value of
	 * <code>editor</code>. If <code>editor</code> is <code>true</code> only
	 * <code>checkValidateEdit</code> is performed since the editor does a in
	 * sync check on focus change. If <code>editor</code> is <code>false</code>
	 * both checks are performed.
	 * 
	 * @param elements the elements to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * check fails
	 * @param title a dialog's title used to present a dialog to the user if the
	 * check fails
	 * @return boolean <code>true</code> if all elements pass the checks.
	 * Otherwise <code>false</code> is returned
	 * 
	 * @see #checkInSync(IAdaptable[], Shell, String)
	 * @see #checkValidateEdit(IJavaScriptElement[], Shell, String)
	 */
	public static boolean check(IJavaScriptElement[] elements, Shell parent,String title, boolean editor) {
		IResource[] resources= getResources(elements);
		if (!editor && !checkInSync(resources, parent, title))
			return false;
		return checkValidateEdit(resources, parent, title);
	}

	private static boolean checkInSync(IResource[] resources, Shell parent, String title) {
		IStatus status= Resources.checkInSync(resources);
		if (status.isOK())
			return true;
		ErrorDialog.openError(parent, title, 
			JavaUIMessages.ElementValidator_cannotPerform, 
			status);
		return false;
	}

	private static boolean checkValidateEdit(IResource[] resources, Shell parent, String title) {
		IStatus status= Resources.makeCommittable(resources, parent);
		if (!status.isOK()) {
			ErrorDialog.openError(parent, title, 
				JavaUIMessages.ElementValidator_cannotPerform, 
				status);
			return false;			
		}
		return true;
	}
	
	private static IResource[] getResources(IAdaptable[] elements) {
		Set result= new HashSet();
		for (int i= 0; i < elements.length; i++) {
			IAdaptable element= elements[i];
			IResource resource= null;
			if (element instanceof IJavaScriptElement) {
				IJavaScriptElement je= (IJavaScriptElement)element;
				IJavaScriptUnit cu= (IJavaScriptUnit)je.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
				if (cu != null) {
					je= cu.getPrimary();
				}
				resource= je.getResource();
			} else {
				resource= (IResource)element.getAdapter(IResource.class);
			}
			if (resource != null)
				result.add(resource);
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}	
}
