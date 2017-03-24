// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.webrun.ui;

import static java.lang.System.out;

import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import phasereditor.project.core.ProjectCore;
import phasereditor.webrun.core.WebRunCore;

public class WebRunUI {
	/**
	 * Open a browser pointing to the WebContent folder of the given project.
	 * 
	 * @param project
	 */
	public static void openBrowser(IProject project) {

		try {
			if (ProjectCore.hasErrors(project)) {
				if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Run",
						"The project '" + project.getName() + "' has errors, do you want to run it?")) {
					return;
				}

			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

		String url = getProjectBrowserURL(project);
		openBrowser(url);

	}

	public static String getProjectBrowserURL(IProject project) {
		IContainer webContent = ProjectCore.getWebContentFolder(project);
		String path = webContent.getFullPath().toPortableString();
		String url = ("http://localhost:" + WebRunCore.getServerPort() + "/" + path).replace("//", "/");
		url = URIUtil.encodePath(url);
		return url;
	}

	public static void openBrowser(ISelection sel) {
		IResource resource = null;

		if (sel != null && sel instanceof IStructuredSelection) {
			Object elem = ((IStructuredSelection) sel).getFirstElement();
			if (elem != null && elem instanceof IResource) {
				resource = (IResource) elem;
			}
		}
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Shell shell = window.getShell();

		if (resource == null) {
			IEditorPart editor = window.getActivePage().getActiveEditor();
			if (editor != null) {
				IEditorInput input = editor.getEditorInput();
				if (input instanceof IFileEditorInput) {
					resource = ((IFileEditorInput) input).getFile();
				}
			}
		}

		if (resource == null) {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			if (projects.length == 1) {
				resource = projects[0];
			}
		}

		if (resource == null) {
			MessageDialog.openInformation(shell, "Run Local",
					"Project not found. Please on the Project Explorer select the projet to run.");
		} else {
			IProject project = resource.getProject();
			openBrowser(project);
		}
	}

	/**
	 * Open the external/default browser to show the given url. The external
	 * browser is that one returned by
	 * {@link IWorkbenchBrowserSupport#getExternalBrowser()}.
	 * 
	 * @see IWorkbenchBrowserSupport#getExternalBrowser()
	 * @param url
	 */
	public static void openBrowser(String url) {
		WebRunCore.startServerIfNotRunning();

		out.println("Open " + url);
		try {
			IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser = support.getExternalBrowser();
			browser.openURL(new URL(url));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
