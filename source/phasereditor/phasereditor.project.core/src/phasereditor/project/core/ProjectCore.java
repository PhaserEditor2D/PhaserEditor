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
package phasereditor.project.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.jsdt.ui.project.JsNature;

import phasereditor.inspect.core.IPhaserTemplate;

public class ProjectCore {

	protected static final String PLUGIN_ID = Activator.PLUGIN_ID;
	public static final String PHASER_PROJECT_NATURE = PLUGIN_ID + ".nature";
	public static final String GLOBAL_SCOPE_INITIALIZER_ID = PLUGIN_ID + ".globalScope";
	public static final String BROWSER_SCOPE_INITIALIZER_ID = PLUGIN_ID + ".browserscope";
	public static final String ECMA5_SCOPE_INITIALIZER_ID = PLUGIN_ID + ".ecma5scope";
	public static final String PHASER_BUILDER_ID = PLUGIN_ID + ".builder";
	public static final String PHASER_PROBLEM_MARKER_ID = PLUGIN_ID + ".problem";

	public static IPath getDesignPath(IProject project) {
		IPath path = project.getFullPath();

		IContainer folder = project.getFolder("Design");
		if (folder.exists()) {
			path = folder.getFullPath();
		}

		return path;
	}

	/**
	 * Returns the source folder of the specified project
	 * 
	 * @param project
	 *            the project which source path is needed
	 * @return IPath of the source folder
	 */
	public static IPath getWebContentPath(IProject project) {
		IPath path = project.getFullPath();
		IContainer folder = project.getFolder("WebContent");
		if (!folder.exists()) {
			// default to project, but look for index.html
			IContainer[] result = { project };
			try {
				project.accept(new IResourceVisitor() {

					@Override
					public boolean visit(IResource resource) throws CoreException {
						if (result[0] != null) {
							return false;
						}

						if (resource instanceof IFile && resource.getName().equals("index.html")) {
							result[0] = ((IFile) resource).getParent();
							return false;
						}
						return true;
					}
				});
				folder = result[0];
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		}

		path = folder.getFullPath();

		return path;
	}

	/**
	 * Get the source folder of given the Phaser project.
	 * 
	 * @param project
	 *            The project.
	 * @return The source folder or null if no source folder is found.
	 */
	public static IContainer getWebContentFolder(IProject project) {
		IPath path = getWebContentPath(project);

		if (path.equals(project.getFullPath())) {
			return project;
		}

		IWorkspaceRoot root = project.getWorkspace().getRoot();
		IFolder folder = root.getFolder(path);
		return folder;
	}

	public static IPath getAssetsPath(IProject project) {
		IPath webpath = getWebContentPath(project);

		IResource member = ResourcesPlugin.getWorkspace().getRoot().findMember(webpath.append("assets"));

		if (member.exists() && member instanceof IFolder) {
			return member.getFullPath();
		}

		return webpath;
	}

	public static boolean isPhaserProject(IProject project) {
		return PhaserProjectNature.hasNature(project);
	}

	public static List<IProject> getPhaserProjects() {
		List<IProject> list = new ArrayList<>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (isPhaserProject(project)) {
				list.add(project);
			}
		}
		return list;
	}

	public static void configureNewPhaserProject(IProject project, IPhaserTemplate template) {
		WorkspaceJob copyJob = new WorkspaceJob("Copying template content") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				JsNature.addJsNature(project, monitor);
				PhaserProjectNature.addPhaserNature(project, monitor);

				IFolder webContentFolder = project.getFolder("WebContent");
				webContentFolder.create(true, true, monitor);

				IFolder folder = project.getFolder("Design");
				folder.create(true, true, monitor);

				template.copyInto(webContentFolder, monitor);

				return Status.OK_STATUS;
			}

		};

		copyJob.schedule();

		copyJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				IFolder webContentFolder = project.getFolder("WebContent");
				IFile file = template.getOpenFile(webContentFolder);
				if (file != null) {
					Job job = new Job("Opening " + file.getName()) {

						@Override
						protected IStatus run(IProgressMonitor monitor2) {
							Display.getDefault().syncExec(new Runnable() {

								@Override
								public void run() {
									IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
									IWorkbenchPage page = window.getActivePage();
									try {
										IDE.openEditor(page, file);
									} catch (PartInitException e) {
										e.printStackTrace();
										throw new RuntimeException(e);
									}

									String url = template.getInfo().getUrl();
									if (url != null) {
										if (MessageDialog.openQuestion(window.getShell(), "Open URL",
												"Do you want to open the template url?")) {
											try {
												IWorkbenchBrowserSupport support = PlatformUI.getWorkbench()
														.getBrowserSupport();
												IWebBrowser browser = support.getExternalBrowser();
												browser.openURL(new URL(url));
											} catch (Exception e) {
												e.printStackTrace();
												throw new RuntimeException(e);
											}
										}
									}
								}
							});

							return Status.OK_STATUS;
						}
					};
					job.setUser(true);
					job.schedule();
				}
			}
		});
	}

	/**
	 * Returns the project that contains the specified path
	 * 
	 * @param path
	 *            the path which project is needed
	 * @return IProject object. If path is <code>null</code> the return value is
	 *         also <code>null</code>.
	 */
	public static IProject getProjectFromPath(IPath path) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = null;

		if (path != null) {
			if (workspace.validatePath(path.toString(), IResource.PROJECT).isOK()) {
				project = workspace.getRoot().getProject(path.toString());
			} else {
				project = workspace.getRoot().getFile(path).getProject();
			}
		}

		return project;
	}

}
