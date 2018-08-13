// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.assetexplorer.ui.views;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetexplorer.ui.views.newactions.NewAnimationWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewAssetPackWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewAtlasWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewExampleProjectWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewProjectWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewWizardLancher;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.atlas.core.AtlasCore;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasType;

class AssetExplorerContentProvider extends AssetsContentProvider {

	IPartListener _partListener;
	private TreeViewer _viewer;
	private IProject _projectInContent;
	private IProject _forceToFocuseOnProject;

	public AssetExplorerContentProvider() {
		super(true);

		_partListener = new IPartListener() {

			@Override
			public void partOpened(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					refreshViewer();
				}
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					refreshViewer();
				}
			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					refreshViewer();
				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					refreshViewer();
				}
			}

			@Override
			public void partActivated(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					refreshViewer();
				}
			}
		};
		getActivePage().addPartListener(_partListener);
	}

	private static IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, newInput);
		_viewer = (TreeViewer) viewer;
	}

	@Override
	public void dispose() {
		getActivePage().removePartListener(_partListener);

		super.dispose();
	}

	@Override
	public Object[] getChildren(Object parent) {
		IProject activeProjet = getActiveProject();

		if (_forceToFocuseOnProject != null && _forceToFocuseOnProject.exists()) {
			activeProjet = _forceToFocuseOnProject;
		}

		if (activeProjet == null) {
			if (_projectInContent != null && _projectInContent.exists()) {
				activeProjet = _projectInContent;
			}
		} else {
			_projectInContent = activeProjet;
		}

		if (parent == AssetExplorer.ROOT) {

			if (activeProjet == null) {
				List<Object> list = new ArrayList<>();
				list.add(new NewProjectWizardLauncher());
				list.add(new NewExampleProjectWizardLauncher());
				list.addAll(List.of(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
				return list.toArray();
			}

			return new Object[] {

					AssetExplorer.CANVAS_NODE,

					AssetExplorer.ANIMATIONS_NODE,

					AssetExplorer.ATLAS_NODE,

					AssetExplorer.PACK_NODE,

					AssetExplorer.PROJECTS_NODE

			};
		}

		if (parent == AssetExplorer.PROJECTS_NODE) {
			var projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			var current = activeProjet;
			var list = new ArrayList<>();
			list.add(new NewProjectWizardLauncher());
			list.add(new NewExampleProjectWizardLauncher());
			list.addAll(Arrays.stream(projects).filter(p -> current != p).collect(toList()));
			return list.toArray();

		}

		if (parent == AssetExplorer.ANIMATIONS_NODE) {
			return NewWizardLancher.children(new NewAnimationWizardLauncher(),
					AssetPackCore.getAnimationsFileCache().getProjectData(activeProjet));
		}

		if (parent == AssetExplorer.ATLAS_NODE) {
			return NewWizardLancher.children(new NewAtlasWizardLauncher(),
					AtlasCore.getAtlasFileCache().getProjectData(activeProjet));
		}

		if (parent == AssetExplorer.CANVAS_NODE) {
			List<Object> list = new ArrayList<>();

			list.add(CanvasType.SPRITE);
			list.add(CanvasType.GROUP);
			list.add(CanvasType.STATE);

			return list.toArray();
		}

		if (parent == AssetExplorer.PACK_NODE) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();

			List<Object> list = new ArrayList<>();

			for (IProject project : workspace.getRoot().getProjects()) {

				if (activeProjet != project) {
					continue;
				}

				{
					List<AssetPackModel> packs = AssetPackCore.getAssetPackModels(project);
					list.addAll(packs);
				}
			}

			return NewWizardLancher.children(new NewAssetPackWizardLauncher(), list);
		}

		if (parent instanceof CanvasType) {
			List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(activeProjet);

			List<CanvasFile> list = new ArrayList<>();

			for (CanvasFile cfile : cfiles) {
				if (cfile.getType() == parent) {
					list.add(cfile);
				}
			}

			list.sort((a, b) -> {
				return a.getFile().getName().compareTo(b.getFile().getName());
			});

			return list.toArray();
		}

		if (parent instanceof CanvasFile) {
			List<IFile> list = new ArrayList<>();

			CanvasFile canvasFile = (CanvasFile) parent;
			IFile file = canvasFile.getFile();
			String name = file.getName();
			name = name.substring(0, name.length() - ".canvas".length());

			IFile srcFile = file.getParent().getFile(new org.eclipse.core.runtime.Path(name + ".js"));

			if (srcFile.exists()) {
				list.add(srcFile);
			}

			srcFile = file.getParent().getFile(new org.eclipse.core.runtime.Path(name + ".ts"));

			if (srcFile.exists()) {
				list.add(srcFile);
			}

			return list.toArray();
		}

		if (parent instanceof AssetPackModel) {
			return ((AssetPackModel) parent).getSections().toArray();
		}

		if (parent instanceof AssetsContentProvider.Container) {
			return ((Container) parent).children;
		}

		return super.getChildren(parent);
	}

	public static IProject getActiveProject() {
		IProject activeProjet = null;
		IEditorPart editor = getActivePage().getActiveEditor();
		if (editor != null) {
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				activeProjet = file.getProject();
			} else {
				activeProjet = input.getAdapter(IProject.class);
			}
		}
		return activeProjet;
	}

	public IProject getProjectInContent() {
		return _projectInContent;
	}

	public void forceToFocuseOnProject(IProject project) {
		_forceToFocuseOnProject = project;
	}

	Object _lastToken = null;

	void refreshViewer() {
		if (PlatformUI.getWorkbench().isClosing()) {
			return;
		}

		if (_viewer == null) {
			return;
		}

		_viewer.getTree().setRedraw(false);
		try {
			out.println("AssetExplorerContentProvider.refreshViewer()");
			_viewer.refresh();

			IProject project = getActiveProject();

			if (project != _lastToken) {
				_viewer.expandToLevel(4);
				_lastToken = project;
			}
		} finally {
			_viewer.getTree().setRedraw(true);
		}
	}
}