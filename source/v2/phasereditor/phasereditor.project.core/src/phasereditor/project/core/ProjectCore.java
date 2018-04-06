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

import static java.lang.System.out;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

import phasereditor.inspect.core.IPhaserTemplate;
import phasereditor.project.core.codegen.SourceLang;

public class ProjectCore {

	protected static final String PLUGIN_ID = Activator.PLUGIN_ID;
	public static final String PHASER_PROJECT_NATURE = PLUGIN_ID + ".nature";
	public static final String GLOBAL_SCOPE_INITIALIZER_ID = PLUGIN_ID + ".globalScope";
	public static final String BROWSER_SCOPE_INITIALIZER_ID = PLUGIN_ID + ".browserscope";
	public static final String ECMA5_SCOPE_INITIALIZER_ID = PLUGIN_ID + ".ecma5scope";
	public static final String PHASER_BUILDER_ID = PLUGIN_ID + ".builder";
	public static final String PHASER_PROBLEM_MARKER_ID = PLUGIN_ID + ".problem";
	private static final QualifiedName PROJECT_LANG = new QualifiedName("phasereditor.project.core", "lang");

	public enum OS {
		WINDOWS, LINUX, MAC
	}

	private static OS _os;
	private static java.nio.file.Path _userFolderPath;

	public static OS getOS() {
		if (_os == null) {
			String osname = System.getProperty("os.name").toLowerCase();
			if (osname.contains("windows")) {
				_os = OS.WINDOWS;
			} else if (osname.contains("mac")) {
				_os = OS.MAC;
			} else {
				_os = OS.LINUX;
			}
		}
		return _os;
	}

	public static java.nio.file.Path getUserCacheFolder() {
		if (_userFolderPath == null) {
			String home = System.getProperty("user.home");
			java.nio.file.Path homePath = Paths.get(home);

			java.nio.file.Path dir;
			if (getOS() == OS.MAC) {
				dir = homePath.resolve("Library/Caches/com.boniatillo.phasereditor");
			} else {
				dir = homePath.resolve(".phasereditor");
			}
			_userFolderPath = dir;
		}

		try {

			Files.createDirectories(_userFolderPath);

			return _userFolderPath;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public static List<IProjectBuildParticipant> getBuildParticipants() {
		List<IProjectBuildParticipant> list = new ArrayList<>();
		IExtensionPoint point = Platform.getExtensionRegistry()
				.getExtensionPoint("phasereditor.project.core.buildParticipant");

		Map<IProjectBuildParticipant, String> orderMap = new HashMap<>();

		for (IConfigurationElement element : point.getConfigurationElements()) {
			try {
				IProjectBuildParticipant participant = (IProjectBuildParticipant) element
						.createExecutableExtension("handler");
				list.add(participant);
				String order = element.getAttribute("order");
				orderMap.put(participant, order);
			} catch (Exception e) {
				ProjectCore.logError(e);
			}
		}

		list.sort((a, b) -> {

			try {
				String order1 = orderMap.get(a);
				String order2 = orderMap.get(b);
				int c = new Double(Double.parseDouble(order1)).compareTo(new Double(Double.parseDouble(order2)));
				return c;
			} catch (Exception e) {
				return 0;
			}
		});

		return list;
	}

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

	public static String getAssetUrl(IFile file) {
		IContainer assetsFolder = ProjectCore.getWebContentFolder(file.getProject());
		String relPath = file.getFullPath().makeRelativeTo(assetsFolder.getFullPath()).toPortableString();
		return relPath;
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

	public static void configureNewPhaserProject(IProject project, IPhaserTemplate template,
			Map<String, String> paramValues, SourceLang lang) {
		PhaserProjectBuilder.setActionAfterFirstBuild(project, () -> openTemplateMainFileInEditor(project, template));

		WorkspaceJob copyJob = new WorkspaceJob("Copying template content") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IFolder webContentFolder = project.getFolder("WebContent");
				webContentFolder.create(true, true, monitor);

				IFolder folder = project.getFolder("Design");
				folder.create(true, true, monitor);

				template.copyInto(webContentFolder, paramValues, monitor);

				//TODO: #RemovingWST
//				if (lang == SourceLang.JAVA_SCRIPT) {
//					JsNature.addJsNature(project, monitor);
//				}

				setProjectLanguage(project, lang);
				PhaserProjectNature.addPhaserNature(project, lang, monitor);

				return Status.OK_STATUS;
			}

		};
		copyJob.schedule();
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

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	/**
	 * Test if the file is part of the web content tree.
	 */
	public static boolean isWebContentFile(IFile file) {
		IContainer webContentFolder = getWebContentFolder(file.getProject());
		return webContentFolder.getFullPath().isPrefixOf(file.getFullPath());
	}

	public static void deleteResourceMarkers(IResource resource, String type) {
		try {
			resource.deleteMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			logError(e);
		}
	}

	public static boolean hasProblems(IFile file) {
		try {
			IMarker[] markers = file.findMarkers(PHASER_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
			return markers.length > 0;
		} catch (CoreException e) {
			logError(e);
		}
		return false;
	}

	public static IMarker createErrorMarker(String type, IStatus status, IResource resource) {
		try {
			int severity;
			switch (status.getSeverity()) {
			case IStatus.ERROR:
				severity = IMarker.SEVERITY_ERROR;
				break;
			default:
				severity = IMarker.SEVERITY_WARNING;
				break;
			}

			IMarker marker = resource.createMarker(type);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.MESSAGE, status.getMessage());
			marker.setAttribute(IMarker.LOCATION, resource.getProject().getName());

			return marker;

		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	static void openTemplateMainFileInEditor(IProject project, IPhaserTemplate template) {
		IFolder webContentFolder = project.getFolder("WebContent");
		IFile file = template.getOpenFile(webContentFolder);
		if (file != null) {
			out.println("Opening project main file: " + file);
			Display.getDefault().asyncExec(new Runnable() {

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
								IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
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
		}
	}

	public static boolean hasErrors(IProject project) throws CoreException {
		int severity = project.findMaxProblemSeverity(ProjectCore.PHASER_PROBLEM_MARKER_ID, true,
				IResource.DEPTH_INFINITE);
		return severity == IMarker.SEVERITY_ERROR;
	}

	/**
	 * Let's say that a TypeScript project is that one with a
	 * <code>tsconfig.json</code> file in the WebContent folder.
	 * 
	 * @param project
	 *            The project to test.
	 * @return If the project is a TypeScript one.
	 */
	public static boolean isTypeScriptProject(IProject project) {
		IContainer folder = getWebContentFolder(project);

		IFile tsconfig = folder.getFile(new Path("tsconfig.json"));

		boolean exists = tsconfig.exists();

		return exists;
	}

	public static void setProjectLanguage(IProject project, SourceLang lang) {
		try {
			project.setPersistentProperty(PROJECT_LANG, lang.name());
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public static SourceLang getProjectLanguage(IProject project) {
		try {
			Map<QualifiedName, String> props = project.getPersistentProperties();
			String name = props.getOrDefault(PROJECT_LANG, SourceLang.JAVA_SCRIPT.name());
			SourceLang lang = SourceLang.valueOf(name);
			return lang;
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public static SourceLang getProjectLanguage(IPath path) {
		IResource res = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);

		if (res == null) {
			res = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		}

		if (res == null) {
			return SourceLang.JAVA_SCRIPT;
		}

		IProject project = res.getProject();

		return getProjectLanguage(project);
	}
}
