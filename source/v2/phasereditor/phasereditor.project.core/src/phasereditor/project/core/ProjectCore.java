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

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;

import phasereditor.inspect.core.IProjectTemplate;
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

	public static final String PREF_PROP_PROJECT_GAME_WIDTH = "phasereditor.project.ui.gameWidth";
	public static final String PREF_PROP_PROJECT_GAME_HEIGHT = "phasereditor.project.ui.gameHeight";
	public static final String PREF_PROP_PROJECT_WIZARD_LANGUAJE = "phasereditor.project.ui.projectWizardLang";

	private static IProject _activeProject;
	private static final ListenerList<Consumer<IProject>> _activeProjectListeners = new ListenerList<>();
	private static final QualifiedName ACTIVE_PROJECT = new QualifiedName("phasereditor.project.core", "activeProject");
	private static final QualifiedName OPEN_TIME_PROJECT = new QualifiedName("phasereditor.project.core",
			"activeProject");

	public static IProject getActiveProject() {
		if (_activeProject == null) {
			restoreActiveProject();
		}
		return _activeProject;
	}

	private static void restoreActiveProject() {
		try {
			var root = ResourcesPlugin.getWorkspace().getRoot();
			var name = root.getPersistentProperty(ACTIVE_PROJECT);
			if (name != null) {
				var project = root.getProject(name);
				if (project.exists()) {
					_activeProject = project;
				}
			}
		} catch (CoreException e) {
			logError(e);
		}
	}

	public static void setActiveProject(IProject activeProject) {
		_activeProject = activeProject;

		try {
			var root = ResourcesPlugin.getWorkspace().getRoot();
			if (activeProject != null) {
				root.setPersistentProperty(ACTIVE_PROJECT, activeProject.getName());
				activeProject.setPersistentProperty(OPEN_TIME_PROJECT, Long.toString(currentTimeMillis()));
			}
		} catch (CoreException e) {
			logError(e);
		}

		for (var l : _activeProjectListeners) {
			l.accept(activeProject);
		}
	}

	public static Comparator<IProject> getProjectOpenTimeComparator() {
		return new Comparator<>() {

			@Override
			public int compare(IProject o1, IProject o2) {
				try {
					var t1 = o1.getPersistentProperty(OPEN_TIME_PROJECT);
					if (t1 == null) {
						t1 = "0";
					}

					var t2 = o2.getPersistentProperty(OPEN_TIME_PROJECT);
					if (t2 == null) {
						t2 = "0";
					}

					return -t1.compareTo(t2);
				} catch (CoreException e) {
					logError(e);
					return 0;
				}
			}
		};
	}

	public static void addActiveProjectListener(Consumer<IProject> listener) {
		_activeProjectListeners.add(listener);
	}

	public static void removeActiveProjectListener(Consumer<IProject> listener) {
		_activeProjectListeners.remove(listener);
	}

	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
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
				int c = Double.valueOf(Double.parseDouble(order1))
						.compareTo(Double.valueOf(Double.parseDouble(order2)));
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
		IProject project = file.getProject();
		IPath fullPath = file.getFullPath();

		return getAssetUrl(project, fullPath);
	}

	public static String getAssetUrl(IProject project, IPath assetFullPath) {
		IContainer assetsFolder = ProjectCore.getWebContentFolder(project);
		String relPath = assetFullPath.makeRelativeTo(assetsFolder.getFullPath()).toPortableString();
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

	public static void configureNewPhaserProject(IProject project, IProjectTemplate template,
			Map<String, String> paramValues, SourceLang lang, IProgressMonitor monitor) throws CoreException {

		ProjectCore.setActiveProject(project);

		var nullMonitor = new NullProgressMonitor();

		monitor.beginTask("Copying template content.", 5);

		PhaserProjectBuilder.setActionAfterFirstBuild(project, () -> {
			openTemplateMainFileInEditor(project, template);
		});

		IFolder webContentFolder = project.getFolder("WebContent");
		webContentFolder.create(true, true, nullMonitor);
		monitor.worked(1);

		IFolder folder = project.getFolder("Design");
		folder.create(true, true, nullMonitor);
		monitor.worked(1);

		template.copyInto(webContentFolder, paramValues, nullMonitor);
		monitor.worked(1);

		setProjectLanguage(project, lang);
		{
			ProjectCore.getProjectSceneSize(project);
		}

		PhaserProjectNature.addPhaserNature(project, nullMonitor);
		monitor.worked(1);

		project.build(IncrementalProjectBuilder.CLEAN_BUILD, nullMonitor);
		monitor.worked(1);

		{
			var perspId = "phasereditor.ide.code";
			var file = template.getOpenFile(webContentFolder);
			if (file != null && file.getName().endsWith(".scene")) {
				perspId = "phasereditor.ide.ui.perspective";
			}

			var finalPerspId = perspId;
			swtRun(() -> {
				var workbench = PlatformUI.getWorkbench();
				var page = workbench.getActiveWorkbenchWindow().getActivePage();
				page.setPerspective(workbench.getPerspectiveRegistry().findPerspectiveWithId(finalPerspId));
			});
		}

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

	static void openTemplateMainFileInEditor(IProject project, IProjectTemplate template) {
		IFolder webContentFolder = project.getFolder("WebContent");
		IFile file = template.getOpenFile(webContentFolder);
		if (file != null) {
			out.println("Opening project main file: " + file);
			new UIJob(PlatformUI.getWorkbench().getDisplay(), "Opening project main file") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {

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

					return Status.OK_STATUS;
				}
			}.schedule(500);
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
			String name = props.getOrDefault(PROJECT_LANG, SourceLang.JAVA_SCRIPT_6.name());
			SourceLang lang = SourceLang.valueOf(name);
			return lang;
		} catch (Exception e) {
			logError(e);
			return SourceLang.JAVA_SCRIPT_6;
		}
	}

	public static SourceLang getProjectLanguage(IPath path) {
		IResource res = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);

		if (res == null) {
			res = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		}

		if (res == null) {
			return SourceLang.JAVA_SCRIPT_6;
		}

		IProject project = res.getProject();

		return getProjectLanguage(project);
	}

	public static Point getProjectSceneSize(IPath path) {
		var res = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);

		IProject project = res == null ? null : res.getProject();

		return getProjectSceneSize(project);

	}

	private static Map<IProject, IPreferenceStore> _projectPrefMap = new HashMap<>();

	public static IPreferenceStore getProjectPreferenceStore(IProject project) {
		if (_projectPrefMap.containsKey(project)) {
			return _projectPrefMap.get(project);
		}

		var store = new ScopedPreferenceStore(new ProjectScope(project), "phasereditor.project.core");
		_projectPrefMap.put(project, store);

		return store;
	}

	public static Point getProjectSceneSize(IProject project) {

		var width = getPreferenceStore().getInt(PREF_PROP_PROJECT_GAME_WIDTH);
		var height = getPreferenceStore().getInt(PREF_PROP_PROJECT_GAME_HEIGHT);

		if (project != null) {

			var projectStore = getProjectPreferenceStore(project);

			if (projectStore.contains(PREF_PROP_PROJECT_GAME_WIDTH)) {
				width = projectStore.getInt(PREF_PROP_PROJECT_GAME_WIDTH);
				height = projectStore.getInt(PREF_PROP_PROJECT_GAME_HEIGHT);
			}

		}

		return new Point(width, height);

	}

	public static SourceLang getDefaultProjectLanguage() {
		var str = getPreferenceStore().getString(PREF_PROP_PROJECT_WIZARD_LANGUAJE);
		return SourceLang.valueOf(str);
	}

	public static void setProjectSceneSize(IProject project, int width, int height) {
		var store = getProjectPreferenceStore(project);
		store.putValue(PREF_PROP_PROJECT_GAME_WIDTH, Integer.toString(width));
		store.putValue(PREF_PROP_PROJECT_GAME_HEIGHT, Integer.toString(height));
	}

	public static boolean areFilesAffectedByDelta(IResourceDelta delta, Collection<IFile> files) {
		boolean[] touched = { false };

		for (IFile used : files) {
			if (used == null) {
				continue;
			}

			try {
				delta.accept(d -> {
					IResource resource = d.getResource();

					if (used.equals(resource)) {
						touched[0] = true;
						return false;
					}

					IPath movedTo = d.getMovedToPath();
					IPath movedFrom = d.getMovedFromPath();

					if (movedTo != null) {
						if (used.getFullPath().equals(movedTo)) {
							touched[0] = true;
							return false;
						}
					}

					if (movedFrom != null) {
						if (used.getFullPath().equals(movedFrom)) {
							touched[0] = true;
							return false;
						}
					}

					return true;
				});
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (touched[0]) {
				return true;
			}
		}
		return touched[0];
	}

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
				dir = homePath.resolve("Library/Caches/com.phasereditor2d");
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

	public static void setDefaultPreferences() {
		var store = getPreferenceStore();

		store.setDefault(ProjectCore.PREF_PROP_PROJECT_GAME_WIDTH, 800);
		store.setDefault(ProjectCore.PREF_PROP_PROJECT_GAME_HEIGHT, 450);
		store.setDefault(ProjectCore.PREF_PROP_PROJECT_WIZARD_LANGUAJE, SourceLang.JAVA_SCRIPT_6.name());
	}
}
