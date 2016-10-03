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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 *
 */
public class BuildPathSupport {
	
	public static final String JRE_PREF_PAGE_ID= "org.eclipse.wst.jsdt.debug.ui.preferences.VMPreferencePage"; //$NON-NLS-1$

	
	private BuildPathSupport() {
		super();
	}
	
	/**
	 * Returns a deprecation message for a classpath variable name.
	 * 
	 * @param variableName classpath variable name
	 * @return the deprecation message, or <code>null</code> iff
	 *         <code>variableName</code> is not a classpath variable or the
	 *         variable is not deprecated
	 */
	public static String getDeprecationMessage(String variableName) {
		String deprecationMessage= JavaScriptCore.getIncludepathVariableDeprecationMessage(variableName);
		if (deprecationMessage == null	)
			return null;
		else
			return Messages.format(NewWizardMessages.BuildPathSupport_deprecated,
					new Object[] {variableName, deprecationMessage});
	}

	/**
	 * Finds a source attachment for a new archive in the existing classpaths.
	 * @param elem The new classpath entry
	 * @return A path to be taken for the source attachment or <code>null</code>
	 */
	public static IPath guessSourceAttachment(CPListElement elem) {
		if (elem.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
			return null;
		}
		IJavaScriptProject currProject= elem.getJavaProject(); // can be null
		try {
			// try if the jar itself contains the source
			IJavaScriptModel jmodel= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
			IJavaScriptProject[] jprojects= jmodel.getJavaScriptProjects();
			for (int i= 0; i < jprojects.length; i++) {
				IJavaScriptProject curr= jprojects[i];
				if (!curr.equals(currProject)) {
					IIncludePathEntry[] entries= curr.getRawIncludepath();
					for (int k= 0; k < entries.length; k++) {
						IIncludePathEntry entry= entries[k];
						if (entry.getEntryKind() == elem.getEntryKind()
							&& entry.getPath().equals(elem.getPath())) {
							IPath attachPath= entry.getSourceAttachmentPath();
							if (attachPath != null && !attachPath.isEmpty()) {
								return attachPath;
							}
						}
					}
				}
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e.getStatus());
		}
		return null;
	}
	
	/**
	 * Finds a javadoc location for a new archive in the existing classpaths.
	 * @param elem The new classpath entry
	 * @return A javadoc location found in a similar classpath entry or <code>null</code>.
	 */
	public static String guessJavadocLocation(CPListElement elem) {
		if (elem.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
			return null;
		}
		IJavaScriptProject currProject= elem.getJavaProject(); // can be null
		try {
			// try if the jar itself contains the source
			IJavaScriptModel jmodel= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
			IJavaScriptProject[] jprojects= jmodel.getJavaScriptProjects();
			for (int i= 0; i < jprojects.length; i++) {
				IJavaScriptProject curr= jprojects[i];
				if (!curr.equals(currProject)) {
					IIncludePathEntry[] entries= curr.getRawIncludepath();
					for (int k= 0; k < entries.length; k++) {
						IIncludePathEntry entry= entries[k];
						if (entry.getEntryKind() == elem.getEntryKind() && entry.getPath().equals(elem.getPath())) {
							IIncludePathAttribute[] attributes= entry.getExtraAttributes();
							for (int n= 0; n < attributes.length; n++) {
								IIncludePathAttribute attrib= attributes[n];
								if (IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME.equals(attrib.getName())) {
									return attrib.getValue();
								}
							}
						}
					}
				}
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e.getStatus());
		}
		return null;
	}
	
	private static class UpdatedJsGlobalScopeContainer implements IJsGlobalScopeContainer {

		private IIncludePathEntry[] fNewEntries;
		private IJsGlobalScopeContainer fOriginal;

		public UpdatedJsGlobalScopeContainer(IJsGlobalScopeContainer original, IIncludePathEntry[] newEntries) {
			fNewEntries= newEntries;
			fOriginal= original;
		}

		/**
		 * @deprecated Use {@link #getIncludepathEntries()} instead
		 */
		public IIncludePathEntry[] getClasspathEntries() {
			return getIncludepathEntries();
		}

		public IIncludePathEntry[] getIncludepathEntries() {
			return fNewEntries;
		}

		public String getDescription() {
			return fOriginal.getDescription();
		}

		public int getKind() {
			return fOriginal.getKind();
		}

		public IPath getPath() {
			return fOriginal.getPath();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#resolvedLibraryImport(java.lang.String)
		 */
		public String[] resolvedLibraryImport(String a) {
				return null;
		}
	}

	/**
	 * Apply a modified classpath entry to the classpath. The classpath entry can also be from a classpath container.
	 * @param shell If not null and the entry could not be found on the projects classpath, a dialog will ask to put the entry on the classpath
	 * @param newEntry The modified entry. The entry's kind or path must be unchanged.
	 * @param changedAttributes The attibutes that have changed. See {@link CPListElement} for constants values.
	 * @param jproject Project where the entry belongs to
	 * @param containerPath The path of the entry's parent container or <code>null</code> if the entry is not in a container
	 * @param monitor The progress monitor to use
	 * @throws CoreException
	 */
	public static void modifyClasspathEntry(Shell shell, IIncludePathEntry newEntry, String[] changedAttributes, IJavaScriptProject jproject, IPath containerPath, IProgressMonitor monitor) throws CoreException {
		if (containerPath != null) {
			updateContainerClasspath(jproject, containerPath, newEntry, changedAttributes, monitor);
		} else {
			updateProjectClasspath(shell, jproject, newEntry, changedAttributes, monitor);
		}
	}
	
	
	/**
	 * Apply a modified classpath entry to the classpath. The classpath entry can also be from a classpath container.
	 * @param shell If not null and the entry could not be found on the projects classpath, a dialog will ask to put the entry on the classpath
	 * @param newEntry The modified entry. The entry's kind or path must be unchanged.
	 * @param jproject Project where the entry belongs to
	 * @param containerPath The path of the entry's parent container or <code>null</code> if the entry is not in a container
	 * @param monitor The progress monitor to use
	 * @throws CoreException
	 */
	public static void modifyClasspathEntry(Shell shell, IIncludePathEntry newEntry, IJavaScriptProject jproject, IPath containerPath, IProgressMonitor monitor) throws CoreException {
		modifyClasspathEntry(shell, newEntry, null, jproject, containerPath, monitor);
	}

	private static void updateContainerClasspath(IJavaScriptProject jproject, IPath containerPath, IIncludePathEntry newEntry, String[] changedAttributes, IProgressMonitor monitor) throws CoreException {
		IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(containerPath, jproject);
		if (container == null) {
			throw new CoreException(new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, "Container " + containerPath + " cannot be resolved", null));  //$NON-NLS-1$//$NON-NLS-2$
		}
		IIncludePathEntry[] entries= container.getIncludepathEntries();
		IIncludePathEntry[] newEntries= new IIncludePathEntry[entries.length];
		for (int i= 0; i < entries.length; i++) {
			IIncludePathEntry curr= entries[i];
			if (curr.getEntryKind() == newEntry.getEntryKind() && curr.getPath().equals(newEntry.getPath())) {
				newEntries[i]= getUpdatedEntry(curr, newEntry, changedAttributes, jproject);
			} else {
				newEntries[i]= curr;
			}
		}
		requestContainerUpdate(jproject, container, newEntries);
		monitor.worked(1);
	}

	private static IIncludePathEntry getUpdatedEntry(IIncludePathEntry currEntry, IIncludePathEntry updatedEntry, String[] updatedAttributes, IJavaScriptProject jproject) {
		if (updatedAttributes == null) {
			return updatedEntry; // used updated entry 'as is'
		}
		CPListElement currElem= CPListElement.createFromExisting(currEntry, jproject);
		CPListElement newElem= CPListElement.createFromExisting(updatedEntry, jproject);
		for (int i= 0; i < updatedAttributes.length; i++) {
			String attrib= updatedAttributes[i];
			currElem.setAttribute(attrib, newElem.getAttribute(attrib));
		}
		return currElem.getClasspathEntry();
	}

	/**
	 * Request a container update.
	 * @param jproject The project of the container
	 * @param container The container to requesta  change to
	 * @param newEntries The updated entries
	 * @throws CoreException
	 */
	public static void requestContainerUpdate(IJavaScriptProject jproject, IJsGlobalScopeContainer container, IIncludePathEntry[] newEntries) throws CoreException {
		IPath containerPath= container.getPath();
		IJsGlobalScopeContainer updatedContainer= new UpdatedJsGlobalScopeContainer(container, newEntries);
		JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(containerPath.segment(0));
		if (initializer != null) {
			initializer.requestJsGlobalScopeContainerUpdate(containerPath, jproject, updatedContainer);
		}
	}

	private static void updateProjectClasspath(Shell shell, IJavaScriptProject jproject, IIncludePathEntry newEntry, String[] changedAttributes, IProgressMonitor monitor) throws JavaScriptModelException {
		IIncludePathEntry[] oldClasspath= jproject.getRawIncludepath();
		int nEntries= oldClasspath.length;
		ArrayList newEntries= new ArrayList(nEntries + 1);
		int entryKind= newEntry.getEntryKind();
		IPath jarPath= newEntry.getPath();
		boolean found= false;
		for (int i= 0; i < nEntries; i++) {
			IIncludePathEntry curr= oldClasspath[i];
			if (curr.getEntryKind() == entryKind && curr.getPath().equals(jarPath)) {
				// add modified entry
				newEntries.add(getUpdatedEntry(curr, newEntry, changedAttributes, jproject));
				found= true;
			} else {
				newEntries.add(curr);
			}
		}
		if (!found) {
			if (!putJarOnClasspathDialog(shell)) {
				return;
			}
			// add new
			newEntries.add(newEntry);			
		}
		IIncludePathEntry[] newClasspath= (IIncludePathEntry[]) newEntries.toArray(new IIncludePathEntry[newEntries.size()]);
		jproject.setRawIncludepath(newClasspath, monitor);
	}
	
	private static boolean putJarOnClasspathDialog(final Shell shell) {
		if (shell == null) {
			return false;
		}
		
		final boolean[] result= new boolean[1];
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= NewWizardMessages.BuildPathSupport_putoncpdialog_title; 
				String message= NewWizardMessages.BuildPathSupport_putoncpdialog_message; 
				result[0]= MessageDialog.openQuestion(shell, title, message);
			}
		});
		return result[0];
	}
}
