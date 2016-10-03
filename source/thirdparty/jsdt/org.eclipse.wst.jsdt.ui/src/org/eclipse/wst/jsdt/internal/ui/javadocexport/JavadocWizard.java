/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> bug 38692
 *     Mickael Istria (Red Hat Inc.) - 426209 Java 6 + Warnings cleanup
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.wst.jsdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fJTWPage;
	private JavadocSpecificsWizardPage fJSWPage;
	private JavadocStandardWizardPage fJSpWPage;

	private IPath fDestination;

	private boolean fWriteCustom;
	private boolean fOpenInBrowser;

	private final String TREE_PAGE_DESC= "JavadocTreePage"; //$NON-NLS-1$
	private final String SPECIFICS_PAGE_DESC= "JavadocSpecificsPage"; //$NON-NLS-1$
	private final String STANDARD_PAGE_DESC= "JavadocStandardPage"; //$NON-NLS-1$

	private final int YES= 0;
	private final int YES_TO_ALL= 1;
	private final int NO= 2;
	private final int NO_TO_ALL= 3;
	private final String JAVADOC_ANT_INFORMATION_DIALOG= "javadocAntInformationDialog";//$NON-NLS-1$


	private JavadocOptionsManager fStore;
	private IWorkspaceRoot fRoot;

	private IFile fXmlJavadocFile;
	
	private static final String ID_JAVADOC_PROCESS_TYPE= "org.eclipse.wst.jsdt.ui.javadocProcess"; //$NON-NLS-1$

	public static void openJavadocWizard(JavadocWizard wizard, Shell shell, IStructuredSelection selection ) {
		wizard.init(PlatformUI.getWorkbench(), selection);

		WizardDialog dialog= new WizardDialog(shell, wizard);
		PixelConverter converter= new PixelConverter(JFaceResources.getDialogFont());
		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(100), converter.convertHeightInCharsToPixels(20));
		dialog.open();
	}
	
	
	public JavadocWizard() {
		this(null);
	}

	public JavadocWizard(IFile xmlJavadocFile) {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_EXPORT_JAVADOC);
		setWindowTitle(JavadocExportMessages.JavadocWizard_javadocwizard_title); 

		setDialogSettings(JavaScriptPlugin.getDefault().getDialogSettings());

		fRoot= ResourcesPlugin.getWorkspace().getRoot();
		fXmlJavadocFile= xmlJavadocFile;

		fWriteCustom= false;
	}
		
	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {

		IJavaScriptProject[] checkedProjects= fJTWPage.getCheckedProjects();
		updateStore(checkedProjects);
		
		fStore.updateDialogSettings(getDialogSettings(), checkedProjects);

		// Wizard should not run with dirty editors
		if (!new RefactoringSaveHelper(RefactoringSaveHelper.SAVE_ALL_ALWAYS_ASK).saveEditors(getShell())) {
			return false;
		}

		fDestination= Path.fromOSString(fStore.getDestination());
		fDestination.toFile().mkdirs();

		fOpenInBrowser= fStore.doOpenInBrowser();

		//Ask if you wish to set the javadoc location for the projects (all) to 
		//the location of the newly generated javadoc 
		if (fStore.isFromStandard()) {
			URI newURI= fDestination.toFile().toURI();
			List<IJavaScriptProject> projs= new ArrayList<IJavaScriptProject>();
			//get javadoc locations for all projects
			for (int i= 0; i < checkedProjects.length; i++) {
				IJavaScriptProject curr= checkedProjects[i];
				try {
					URI currURI = JavaScriptUI.getProjectJSdocLocation(curr).toURI();
					if (!newURI.equals(currURI)) { // currURL can be null
						//if not all projects have the same javadoc location ask if you want to change
						//them to have the same javadoc location
						projs.add(curr);
					}
				} catch (URISyntaxException ex) {
					JavaScriptPlugin.log(ex);
				}
			}
			if (!projs.isEmpty()) {
				try {
					setAllJavadocLocations(projs.toArray(new IJavaScriptProject[projs.size()]), newURI.toURL());
				} catch (MalformedURLException e) {
					JavaScriptPlugin.log(e);
				}
			}
		}

		if (fJSWPage.generateAnt()) {
			//@Improve: make a better message
			OptionalMessageDialog.open(JAVADOC_ANT_INFORMATION_DIALOG, getShell(), JavadocExportMessages.JavadocWizard_antInformationDialog_title, null, JavadocExportMessages.JavadocWizard_antInformationDialog_message, MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0); 
			try {
				File file= fStore.createXML(checkedProjects);
				if (file != null) {
					IFile[] files= fRoot.findFilesForLocation(Path.fromOSString(file.getPath()));
					if (files != null) {
						for (int i= 0; i < files.length; i++) {
							files[i].refreshLocal(IResource.DEPTH_ONE, null);
						}
					}
				}
				
			} catch (CoreException e) {
				ExceptionHandler.handle(e, getShell(),JavadocExportMessages.JavadocWizard_error_writeANT_title, JavadocExportMessages.JavadocWizard_error_writeANT_message); 
			}
		}

		if (!executeJavadocGeneration())
			return false;

		return true;
	}
	
	private void updateStore(IJavaScriptProject[] checkedProjects) {
		//writes the new settings to store
		fJTWPage.updateStore(checkedProjects);
		if (!fJTWPage.getCustom())
			fJSpWPage.updateStore();
		fJSWPage.updateStore();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performCancel()
	 */
	public boolean performCancel() {
		
		IJavaScriptProject[] checkedProjects= fJTWPage.getCheckedProjects();
		updateStore(checkedProjects);
		
		//If the wizard was not launched from an ant file store the settings 
		if (fXmlJavadocFile == null) {
			fStore.updateDialogSettings(getDialogSettings(), checkedProjects);
		}
		return super.performCancel();
	}

	private void setAllJavadocLocations(IJavaScriptProject[] projects, URL newURL) {
		Shell shell= getShell();
		String[] buttonlabels= new String[] { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL };

		for (int j= 0; j < projects.length; j++) {
			IJavaScriptProject iJavaProject= projects[j];
			String message= Messages.format(JavadocExportMessages.JavadocWizard_updatejavadoclocation_message, new String[] { iJavaProject.getElementName(), fDestination.toOSString()}); 
			MessageDialog dialog= new MessageDialog(shell, JavadocExportMessages.JavadocWizard_updatejavadocdialog_label, null, message, MessageDialog.QUESTION, buttonlabels, 1);

			switch (dialog.open()) {
				case YES :
					JavaScriptUI.setProjectJSdocLocation(iJavaProject, newURL);
					break;
				case YES_TO_ALL :
					for (int i= j; i < projects.length; i++) {
						iJavaProject= projects[i];
						JavaScriptUI.setProjectJSdocLocation(iJavaProject, newURL);
						j++;
					}
					break;
				case NO_TO_ALL :
					j= projects.length;
					break;
				case NO :
				default :
					break;
			}
		}
	}

	private boolean executeJavadocGeneration() {
		Process process= null;
		try {
			ArrayList vmArgs= new ArrayList();
			ArrayList progArgs= new ArrayList();
			
			IStatus status= fStore.getArgumentArray(vmArgs, progArgs);
			if (!status.isOK()) {
				ErrorDialog.openError(getShell(), JavadocExportMessages.JavadocWizard_error_title, JavadocExportMessages.JavadocWizard_warning_starting_message, status);
			}
			
			File file= File.createTempFile("javadoc-arguments", ".tmp");  //$NON-NLS-1$//$NON-NLS-2$
			vmArgs.add('@' + file.getAbsolutePath());

			FileWriter writer= new FileWriter(file);
			try {
				for (int i= 0; i < progArgs.size(); i++) {
					String curr= (String) progArgs.get(i);
					curr= checkForSpaces(curr);
					
					writer.write(curr);
					writer.write(' ');
				}
			} finally {
				writer.close();
			}
			
			String[] args= (String[]) vmArgs.toArray(new String[vmArgs.size()]);
			process= Runtime.getRuntime().exec(args);
			if (process != null) {
				// construct a formatted command line for the process properties
				StringBuffer buf= new StringBuffer();
				for (int i= 0; i < args.length; i++) {
					buf.append(args[i]);
					buf.append(' ');
				}

				IDebugEventSetListener listener= new JavadocDebugEventListener(getShell().getDisplay(), file);
				DebugPlugin.getDefault().addDebugEventListener(listener);

				ILaunchConfigurationWorkingCopy wc= null;
				try {
					ILaunchConfigurationType lcType= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
					String name= JavadocExportMessages.JavadocWizard_launchconfig_name; 
					wc= lcType.newInstance(null, name);
					wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);

					ILaunch newLaunch= new Launch(wc, ILaunchManager.RUN_MODE, null);
					IProcess iprocess= DebugPlugin.newProcess(newLaunch, process, JavadocExportMessages.JavadocWizard_javadocprocess_label); 
					iprocess.setAttribute(IProcess.ATTR_CMDLINE, buf.toString());
					iprocess.setAttribute(IProcess.ATTR_PROCESS_TYPE, ID_JAVADOC_PROCESS_TYPE);

					DebugPlugin.getDefault().getLaunchManager().addLaunch(newLaunch);

				} catch (CoreException e) {
					String title= JavadocExportMessages.JavadocWizard_error_title; 
					String message= JavadocExportMessages.JavadocWizard_launch_error_message; 
					ExceptionHandler.handle(e, getShell(), title, message);
				}

				return true;

			}
		} catch (IOException e) {
			String title= JavadocExportMessages.JavadocWizard_error_title; 
			String message= JavadocExportMessages.JavadocWizard_exec_error_message; 

			IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e);
			ExceptionHandler.handle(new CoreException(status), getShell(), title, message);
			return false;
		}
		return false;

	}

	private String checkForSpaces(String curr) {
		if (curr.indexOf(' ') == -1) {
			return curr;
		}	
		StringBuffer buf= new StringBuffer();
		buf.append('\'');
		for (int i= 0; i < curr.length(); i++) {
			char ch= curr.charAt(i);
			if (ch == '\\' || ch == '\'') {
				buf.append('\\');				
			}
			buf.append(ch);
		}
		buf.append('\'');
		return buf.toString();
	}

	/*
	 * @see IWizard#addPages()
	 */
	public void addPages() {
		
		fJTWPage= new JavadocTreeWizardPage(TREE_PAGE_DESC, fStore);
		fJSWPage= new JavadocSpecificsWizardPage(SPECIFICS_PAGE_DESC, fJTWPage, fStore);
		fJSpWPage= new JavadocStandardWizardPage(STANDARD_PAGE_DESC, fJTWPage, fStore);

		super.addPage(fJTWPage);
		super.addPage(fJSpWPage);
		super.addPage(fJSWPage);

		fJTWPage.init();
		fJSpWPage.init();
		fJSWPage.init();

	}

	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
		IWorkbenchWindow window= workbench.getActiveWorkbenchWindow();
		List selected= Collections.EMPTY_LIST;
		if (window != null) {
			ISelection selection= window.getSelectionService().getSelection();
			if (selection instanceof IStructuredSelection) {
				selected= ((IStructuredSelection) selection).toList();
			} else {
				IJavaScriptElement element= EditorUtility.getActiveEditorJavaInput();
				if (element != null) {
					selected= new ArrayList();
					selected.add(element);
				}
			}
		}
		fStore= new JavadocOptionsManager(fXmlJavadocFile, getDialogSettings(), selected);
	}

	private void refresh(IPath path) {
		if (fRoot.findContainersForLocation(path).length > 0) {
			try {
				fRoot.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
			}
		}
	}

	private void spawnInBrowser(Display display) {
		if (fOpenInBrowser) {
			try {
				IPath indexFile= fDestination.append("index.html"); //$NON-NLS-1$
				URL url= indexFile.toFile().toURL();
				OpenBrowserUtil.open(url, display, getWindowTitle());
			} catch (MalformedURLException e) {
				JavaScriptPlugin.log(e);
			}
		}
	}

	private class JavadocDebugEventListener implements IDebugEventSetListener {
		private Display fDisplay;
		private File fFile;

		public JavadocDebugEventListener(Display display, File file) {
			fDisplay= display;
			fFile= file;
		}
		
		public void handleDebugEvents(DebugEvent[] events) {
			for (int i= 0; i < events.length; i++) {
				if (events[i].getKind() == DebugEvent.TERMINATE) {
					try {
						if (!fWriteCustom) {
							fFile.delete();
							refresh(fDestination); //If destination of javadoc is in workspace then refresh workspace
							spawnInBrowser(fDisplay);
						}
					} finally {
						DebugPlugin.getDefault().removeDebugEventListener(this);
					}
					return;
				}
			}
		}
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof JavadocTreeWizardPage) {
			if (!fJTWPage.getCustom()) {
				return fJSpWPage;
			}
			return fJSWPage;
		} else if (page instanceof JavadocSpecificsWizardPage) {
			return null;
		} else if (page instanceof JavadocStandardWizardPage)
			return fJSWPage;
		else
			return null;
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page instanceof JavadocSpecificsWizardPage) {
			if (!fJTWPage.getCustom()) {
				return fJSpWPage;
			}
			return fJSWPage;
		} else if (page instanceof JavadocTreeWizardPage) {
			return null;
		} else if (page instanceof JavadocStandardWizardPage)
			return fJTWPage;
		else
			return null;
	}
	
}
