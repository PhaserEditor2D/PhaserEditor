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
package org.eclipse.wst.jsdt.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

/**
 * Dialog to browse for package fragments.
 */
public class PackageSelectionDialog extends ElementListSelectionDialog {
	
	public static final int F_REMOVE_DUPLICATES= 1;
	public static final int F_SHOW_PARENTS= 2;
	public static final int F_HIDE_DEFAULT_PACKAGE= 4;
	public static final int F_HIDE_EMPTY_INNER= 8;
	

	/** The dialog location. */
	private Point fLocation;
	/** The dialog size. */
	private Point fSize;
	
	private IRunnableContext fContext;
	private IJavaScriptSearchScope fScope;
	private int fFlags;

	/**
	 * Creates a package selection dialog.
	 * @param parent the parent shell
	 * @param context the runnable context to run the search in
	 * @param flags a combination of <code>F_REMOVE_DUPLICATES</code>, <code>F_SHOW_PARENTS</code>,
	 *  <code>F_HIDE_DEFAULT_PACKAGE</code> and  <code>F_HIDE_EMPTY_INNER</code>
	 * @param scope the scope defining the available packages.
	 */
	public PackageSelectionDialog(Shell parent, IRunnableContext context, int flags, IJavaScriptSearchScope scope) {
		super(parent, createLabelProvider(flags));
		fFlags= flags;
		fScope= scope;
		fContext= context;
	}
	
	private static ILabelProvider createLabelProvider(int dialogFlags) {
		int flags= JavaScriptElementLabelProvider.SHOW_DEFAULT;
		if ((dialogFlags & F_REMOVE_DUPLICATES) == 0) {
			flags= flags | JavaScriptElementLabelProvider.SHOW_ROOT;
		}
		return new JavaScriptElementLabelProvider(flags);
	}
	
	
	/*
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		final ArrayList packageList= new ArrayList();
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				if (monitor == null) {
					monitor= new NullProgressMonitor();
				}
				boolean hideEmpty= (fFlags & F_HIDE_EMPTY_INNER) != 0;
				monitor.beginTask(JavaUIMessages.PackageSelectionDialog_progress_search, hideEmpty ? 2 : 1);
				try {
					SearchRequestor requestor= new SearchRequestor() {
						private HashSet fSet= new HashSet();
						private final boolean fAddDefault= (fFlags & F_HIDE_DEFAULT_PACKAGE) == 0;
						private final boolean fDuplicates= (fFlags & F_REMOVE_DUPLICATES) == 0;
						private final boolean fIncludeParents= (fFlags & F_SHOW_PARENTS) != 0;

						public void acceptSearchMatch(SearchMatch match) throws CoreException {
							IJavaScriptElement enclosingElement= (IJavaScriptElement) match.getElement();
							String name= enclosingElement.getElementName();
							if (fAddDefault || name.length() > 0) {
								if (fDuplicates || fSet.add(name)) {
									packageList.add(enclosingElement);
									if (fIncludeParents) {
										addParentPackages(enclosingElement, name);
									}
								}
							}
						}
						
						private void addParentPackages(IJavaScriptElement enclosingElement, String name) {
							IPackageFragmentRoot root= (IPackageFragmentRoot) enclosingElement.getParent();
							int idx= name.lastIndexOf('.');
							while (idx != -1) {
								name= name.substring(0, idx);
								if (fDuplicates || fSet.add(name)) {
									packageList.add(root.getPackageFragment(name));
								}
								idx= name.lastIndexOf('.');
							}
						}
					};
					SearchPattern pattern= SearchPattern.createPattern("*", //$NON-NLS-1$
							IJavaScriptSearchConstants.PACKAGE, IJavaScriptSearchConstants.DECLARATIONS,
							SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE);
					new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), fScope, requestor, new SubProgressMonitor(monitor, 1));
					
					if (monitor.isCanceled()) {
						throw new InterruptedException();
					}

					if (hideEmpty) {
						removeEmptyPackages(new SubProgressMonitor(monitor, 1));
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException();
				} finally {
					monitor.done();
				}
			}
			
			private void removeEmptyPackages(IProgressMonitor monitor) throws JavaScriptModelException, InterruptedException {
				monitor.beginTask(JavaUIMessages.PackageSelectionDialog_progress_findEmpty, packageList.size());
				try {
					ArrayList res= new ArrayList(packageList.size());
					for (int i= 0; i < packageList.size(); i++) {
						IPackageFragment pkg= (IPackageFragment) packageList.get(i);
						if (pkg.hasChildren() || !pkg.hasSubpackages()) {
							res.add(pkg);
						}
						monitor.worked(1);
						if (monitor.isCanceled()) {
							throw new InterruptedException();
						}
					}
					packageList.clear();
					packageList.addAll(res);
				} finally{
					monitor.done();
				}
			}
		};

		try {
			fContext.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, JavaUIMessages.PackageSelectionDialog_error_title, JavaUIMessages.PackageSelectionDialog_error3Message); 
			return CANCEL;
		} catch (InterruptedException e) {
			// cancelled by user
			return CANCEL;
		}
		
		if (packageList.isEmpty()) {
			String title= JavaUIMessages.PackageSelectionDialog_nopackages_title; 
			String message= JavaUIMessages.PackageSelectionDialog_nopackages_message; 
			MessageDialog.openInformation(getShell(), title, message);
			return CANCEL;
		}
		
		setElements(packageList.toArray());

		return super.open();
	}
	
	
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.OPEN_PACKAGE_DIALOG);
	}

	/*
	 * @see Window#close()
	 */
	public boolean close() {
		writeSettings();
		return super.close();
	}

	/*
	 * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Control control= super.createContents(parent);
		readSettings();
		return control;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#getInitialSize()
	 */
	protected Point getInitialSize() {
		Point result= super.getInitialSize();
		if (fSize != null) {
			result.x= Math.max(result.x, fSize.x);
			result.y= Math.max(result.y, fSize.y);
			Rectangle display= getShell().getDisplay().getClientArea();
			result.x= Math.min(result.x, display.width);
			result.y= Math.min(result.y, display.height);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#getInitialLocation(org.eclipse.swt.graphics.Point)
	 */
	protected Point getInitialLocation(Point initialSize) {
		Point result= super.getInitialLocation(initialSize);
		if (fLocation != null) {
			result.x= fLocation.x;
			result.y= fLocation.y;
			Rectangle display= getShell().getDisplay().getClientArea();
			int xe= result.x + initialSize.x;
			if (xe > display.width) {
				result.x-= xe - display.width; 
			}
			int ye= result.y + initialSize.y;
			if (ye > display.height) {
				result.y-= ye - display.height; 
			}
		}
		return result;
	}



	/**
	 * Initializes itself from the dialog settings with the same state
	 * as at the previous invocation.
	 */
	private void readSettings() {
		IDialogSettings s= getDialogSettings();
		try {
			int x= s.getInt("x"); //$NON-NLS-1$
			int y= s.getInt("y"); //$NON-NLS-1$
			fLocation= new Point(x, y);
			int width= s.getInt("width"); //$NON-NLS-1$
			int height= s.getInt("height"); //$NON-NLS-1$
			fSize= new Point(width, height);

		} catch (NumberFormatException e) {
			fLocation= null;
			fSize= null;
		}
	}

	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeSettings() {
		IDialogSettings s= getDialogSettings();

		Point location= getShell().getLocation();
		s.put("x", location.x); //$NON-NLS-1$
		s.put("y", location.y); //$NON-NLS-1$

		Point size= getShell().getSize();
		s.put("width", size.x); //$NON-NLS-1$
		s.put("height", size.y); //$NON-NLS-1$
	}

	/**
	 * Returns the dialog settings object used to share state
	 * between several find/replace dialogs.
	 *
	 * @return the dialog settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaScriptPlugin.getDefault().getDialogSettings();
		String sectionName= getClass().getName();
		IDialogSettings subSettings= settings.getSection(sectionName);
		if (subSettings == null)
			subSettings= settings.addNewSection(sectionName);
		return subSettings;
	}



}
