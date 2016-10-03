/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.util.CoreUtility;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.wizards.JavaCapabilityConfigurationPage;

/**
 * As addition to the JavaCapabilityConfigurationPage, the wizard does an
 * early project creation (so that linked folders can be defined) and, if an
 * existing external location was specified, offers to do a classpath detection
 */
public class JavaProjectWizardSecondPage extends JavaCapabilityConfigurationPage {

	private static final String FILENAME_PROJECT= ".project"; //$NON-NLS-1$

	private final JavaProjectWizardFirstPage fFirstPage;

	private URI fCurrProjectLocation; // null if location is platform location
	private IProject fCurrProject;
	
	private boolean fKeepContent;

	private File fDotProjectBackup;
	private File fDotClasspathBackup;
	private Boolean fIsAutobuild;
	private static final String SUPER_TYPE_NAME = "Window"; //$NON-NLS-1$
	/**
	 * Constructor for JavaProjectWizardSecondPage.
	 * @param mainPage the first page of the wizard
	 */
	public JavaProjectWizardSecondPage(JavaProjectWizardFirstPage mainPage) {
		fFirstPage= mainPage;
		fCurrProjectLocation= null;
		fCurrProject= null;
		fKeepContent= false;
		
		fDotProjectBackup= null;
		fDotClasspathBackup= null;
		fIsAutobuild= null;
	}
	
	protected boolean useNewSourcePage() {
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			IStatus status= changeToNewProject();
			if (status != null && !status.isOK()) {
				ErrorDialog.openError(getShell(), NewWizardMessages.JavaProjectWizardSecondPage_error_title, null, status);
			}
		} else {
			removeProject();
		}
		super.setVisible(visible);
		if (visible) {

			setFocus();
		}
	}

	private IStatus changeToNewProject() {
		fKeepContent= fFirstPage.getDetect();

		class UpdateRunnable implements IRunnableWithProgress {
			public IStatus infoStatus= Status.OK_STATUS;
			
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (fIsAutobuild == null) {
						fIsAutobuild= Boolean.valueOf(CoreUtility.enableAutoBuild(false));
					}
					infoStatus= updateProject(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException();
				} finally {
                    monitor.done();
                }
			}
		}
		UpdateRunnable op= new UpdateRunnable();
		try {
			getContainer().run(true, false, new WorkspaceModifyDelegatingOperation(op));
			return op.infoStatus;
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.JavaProjectWizardSecondPage_error_title; 
			final String message= NewWizardMessages.JavaProjectWizardSecondPage_error_message; 
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
		return null;
	}
	
	final IStatus updateProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		
		IStatus result= StatusInfo.OK_STATUS;
		
		fCurrProject= fFirstPage.getProjectHandle();
		fCurrProjectLocation= getProjectLocationURI(); 
		
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(NewWizardMessages.JavaProjectWizardSecondPage_operation_initialize, 7); 
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			
			URI realLocation= fCurrProjectLocation;
			if (fCurrProjectLocation == null) {  // inside workspace
				try {
					URI rootLocation= ResourcesPlugin.getWorkspace().getRoot().getLocationURI();
					realLocation= new URI(rootLocation.getScheme(), null,
						Path.fromPortableString(rootLocation.getPath()).append(fCurrProject.getName()).toString(),
						null);
				} catch (URISyntaxException e) {
					Assert.isTrue(false, "Can't happen"); //$NON-NLS-1$
				}
			}

			rememberExistingFiles(realLocation);
            
			try {
				createProject(fCurrProject, fCurrProjectLocation, new SubProgressMonitor(monitor, 2));
			} catch (CoreException e) {
				if (e.getStatus().getCode() == IResourceStatus.FAILED_READ_METADATA) {					
					result= new StatusInfo(IStatus.INFO, Messages.format(NewWizardMessages.JavaProjectWizardSecondPage_DeleteCorruptProjectFile_message, e.getLocalizedMessage()));
					
					deleteProjectFile(realLocation);
					if (fCurrProject.exists())
						fCurrProject.delete(true, null);
					
					createProject(fCurrProject, fCurrProjectLocation, null);					
				} else {
					throw e;
				}	
			}
				
			IIncludePathEntry[] entries= null;
	
			if (fFirstPage.getDetect()) {
				if (!fCurrProject.getFolder(JavaProject.SHARED_PROPERTIES_DIRECTORY).getFile(JavaProject.CLASSPATH_FILENAME).exists()) { 
					final ClassPathDetector detector= 
								new ClassPathDetector(fCurrProject, new SubProgressMonitor(monitor, 2),
											JavaScriptCore.getJavaScriptCore().getDefaultClasspathExclusionPatterns());
					entries= detector.getClasspath();
				} else {
					monitor.worked(2);
				}
			} else if (fFirstPage.isSrcBin()) {
				IPreferenceStore store= PreferenceConstants.getPreferenceStore();
				IPath srcPath= new Path(store.getString(PreferenceConstants.SRCBIN_SRCNAME));
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=272495
				// IPath binPath= new Path(store.getString(PreferenceConstants.SRCBIN_BINNAME));
				
				if (srcPath.segmentCount() > 0) {
					IFolder folder= fCurrProject.getFolder(srcPath);
					CoreUtility.createFolder(folder, true, true, new SubProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
				
//				if (binPath.segmentCount() > 0 && !binPath.equals(srcPath)) {
//					IFolder folder= fCurrProject.getFolder(binPath);
//					CoreUtility.createDerivedFolder(folder, true, true, new SubProgressMonitor(monitor, 1));
//				} else {
					monitor.worked(1);
//				}
				
				final IPath projectPath= fCurrProject.getFullPath();

				// configure the classpath entries, including the default jre library.
				List<IIncludePathEntry> cpEntries= new ArrayList<IIncludePathEntry>();
				cpEntries.add(JavaScriptCore.newSourceEntry(projectPath.append(srcPath), 
							JavaScriptCore.getJavaScriptCore().getDefaultClasspathExclusionPatterns()));
				cpEntries.addAll(Arrays.asList(getDefaultClasspathEntry()));
				entries= cpEntries.toArray(new IIncludePathEntry[cpEntries.size()]);
				
				// configure the output location
//				outputLocation= projectPath.append(binPath);
			} else {
				IPath projectPath= fCurrProject.getFullPath();
				List<IIncludePathEntry> cpEntries = new ArrayList<IIncludePathEntry>();
				cpEntries.add(JavaScriptCore.newSourceEntry(projectPath, 
							JavaScriptCore.getJavaScriptCore().getDefaultClasspathExclusionPatterns()));
				cpEntries.addAll(Arrays.asList(getDefaultClasspathEntry()));
				entries= cpEntries.toArray(new IIncludePathEntry[cpEntries.size()]);

//				outputLocation= projectPath;
				monitor.worked(2);
			}
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			IJavaScriptProject javaProject = JavaScriptCore.create(fCurrProject);
            init(javaProject,  entries, false);
			if(fFirstPage.isWebDefault()) {
				LibrarySuperType superType = new LibrarySuperType(new Path( JavaRuntime.BASE_BROWSER_LIB),  getJavaProject(), SUPER_TYPE_NAME);
				getBuildPathsBlock().setSuperType(superType);
				configureJavaProject(new SubProgressMonitor(monitor, 3)); // create the Java project to allow the use of the new source folder page
			}else {
				configureJavaProject(new SubProgressMonitor(monitor, 3)); // create the Java project to allow the use of the new source folder page
			}
		
		} finally {
			monitor.done();
		}
		return result;
	}
	
	private URI getProjectLocationURI() throws CoreException {
		if (fFirstPage.isInWorkspace()) {
			return null;
		}
		return URIUtil.toURI(fFirstPage.getLocationPath());
	}
	
	private IIncludePathEntry[] getDefaultClasspathEntry() {
		IPath jreContainerPath= new Path(JavaRuntime.JRE_CONTAINER);
		IPath BROWSER_LIB = new Path(JavaRuntime.BASE_BROWSER_LIB);
		 
		if (fFirstPage.isWebEnabled()) {
			// use default
			return new IIncludePathEntry[] { JavaScriptCore.newContainerEntry(jreContainerPath),
																	   JavaScriptCore.newContainerEntry(BROWSER_LIB)  };
		}else {
			return new IIncludePathEntry[] { JavaScriptCore.newContainerEntry(jreContainerPath)};
		}
	}

	private void deleteProjectFile(URI projectLocation) throws CoreException {
		IFileStore file= EFS.getStore(projectLocation);
		if (file.fetchInfo().exists()) {
			IFileStore projectFile= file.getChild(FILENAME_PROJECT);
			if (projectFile.fetchInfo().exists()) {
				projectFile.delete(EFS.NONE, null);
			}
		}
	}
	
	private void rememberExistingFiles(URI projectLocation) throws CoreException {
		fDotProjectBackup= null;
		fDotClasspathBackup= null;
		
		IFileStore file= EFS.getStore(projectLocation);
		if (file.fetchInfo().exists()) {
			IFileStore projectFile= file.getChild(FILENAME_PROJECT);
			if (projectFile.fetchInfo().exists()) {
				fDotProjectBackup= createBackup(projectFile, "project-desc"); //$NON-NLS-1$ 
			}
			IFileStore classpathFile= file.getChild(JavaProject.SHARED_PROPERTIES_DIRECTORY).getChild(JavaProject.CLASSPATH_FILENAME);
			if (classpathFile.fetchInfo().exists()) {
				fDotClasspathBackup= createBackup(classpathFile, "classpath-desc"); //$NON-NLS-1$ 
			}
		}
	}
	
	private void restoreExistingFiles(URI projectLocation, IProgressMonitor monitor) throws CoreException {
		int ticks= ((fDotProjectBackup != null ? 1 : 0) + (fDotClasspathBackup != null ? 1 : 0)) * 2;
		monitor.beginTask("", ticks); //$NON-NLS-1$
		try {
			IFileStore projectFile= EFS.getStore(projectLocation).getChild(FILENAME_PROJECT);
			projectFile.delete(EFS.NONE, new SubProgressMonitor(monitor, 1));
			if (fDotProjectBackup != null) {
				copyFile(fDotProjectBackup, projectFile, new SubProgressMonitor(monitor, 1));
			}
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, NewWizardMessages.JavaProjectWizardSecondPage_problem_restore_project, e); 
			throw new CoreException(status);
		}
		try {
			IFileStore classpathFile= EFS.getStore(projectLocation).getChild(JavaProject.SHARED_PROPERTIES_DIRECTORY).getChild(JavaProject.CLASSPATH_FILENAME);
				classpathFile.delete(EFS.NONE, new SubProgressMonitor(monitor, 1));
			if (fDotClasspathBackup != null) {
				copyFile(fDotClasspathBackup, classpathFile, new SubProgressMonitor(monitor, 1));
			}
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, NewWizardMessages.JavaProjectWizardSecondPage_problem_restore_classpath, e); 
			throw new CoreException(status);
		}
	}
	
	private File createBackup(IFileStore source, String name) throws CoreException {
		try {
			File bak= File.createTempFile("eclipse-" + name, ".bak");  //$NON-NLS-1$//$NON-NLS-2$
			copyFile(source, bak);
			return bak;
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, Messages.format(NewWizardMessages.JavaProjectWizardSecondPage_problem_backup, name), e); 
			throw new CoreException(status);
		} 
	}
	
	private void copyFile(IFileStore source, File target) throws IOException, CoreException {
		InputStream is= source.openInputStream(EFS.NONE, null);
		FileOutputStream os= new FileOutputStream(target);
		copyFile(is, os);
	}
	
	private void copyFile(File source, IFileStore target, IProgressMonitor monitor) throws IOException, CoreException {
		FileInputStream is= new FileInputStream(source);
		OutputStream os= target.openOutputStream(EFS.NONE, monitor);
		copyFile(is, os);
	}
	
	private void copyFile(InputStream is, OutputStream os) throws IOException {		
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead= is.read(buffer);
				if (bytesRead == -1)
					break;
				
				os.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				is.close();
			} finally {
				os.close();
			}
		}
	}
	
	/**
	 * Called from the wizard on finish.
	 * @param monitor the progress monitor
	 * @throws CoreException thrown when the project creation or configuration failed
	 * @throws InterruptedException thrown when the user cancelled the project creation
	 */
	public void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			monitor.beginTask(NewWizardMessages.JavaProjectWizardSecondPage_operation_create, 3); 
			if (fCurrProject == null) {
				updateProject(new SubProgressMonitor(monitor, 1));
			}
			configureJavaProject(new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
			fCurrProject= null;
			if (fIsAutobuild != null) {
				CoreUtility.enableAutoBuild(fIsAutobuild.booleanValue());
				fIsAutobuild= null;
			}
		}
	}

	private void removeProject() { 
		if (fCurrProject == null || !fCurrProject.exists()) {
			return;
		}
		
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				doRemoveProject(monitor);
			}
		};
	
		try {
			getContainer().run(true, true, new WorkspaceModifyDelegatingOperation(op));
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.JavaProjectWizardSecondPage_error_remove_title; 
			final String message= NewWizardMessages.JavaProjectWizardSecondPage_error_remove_message; 
			ExceptionHandler.handle(e, getShell(), title, message);		
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}
	
	final void doRemoveProject(IProgressMonitor monitor) throws InvocationTargetException {
		final boolean noProgressMonitor= (fCurrProjectLocation == null); // inside workspace
		if (monitor == null || noProgressMonitor) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask(NewWizardMessages.JavaProjectWizardSecondPage_operation_remove, 3); 
		try {
			try {
				URI projLoc= fCurrProject.getLocationURI();
				
			    boolean removeContent= !fKeepContent && fCurrProject.isSynchronized(IResource.DEPTH_INFINITE);
			    fCurrProject.delete(removeContent, false, new SubProgressMonitor(monitor, 2));
				
				restoreExistingFiles(projLoc, new SubProgressMonitor(monitor, 1));
			} finally {
				CoreUtility.enableAutoBuild(fIsAutobuild.booleanValue()); // fIsAutobuild must be set
				fIsAutobuild= null;
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
			fCurrProject= null;
			fKeepContent= false;
		}
	}

	/**
	 * Called from the wizard on cancel.
	 */
	public void performCancel() {
		removeProject();
	}      
 }
