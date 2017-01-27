package phasereditor.assetexplorer.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetexplorer.ui.views.AssetExplorer;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.ProjectCore;

public class AssetsExplorerProjectBuildParticipant implements IProjectBuildParticipant {

	public AssetsExplorerProjectBuildParticipant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		// nothing
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {

		IContainer webFolder = ProjectCore.getWebContentFolder(project);
		try {
			webFolder.accept(r -> {
				if (r instanceof IFile) {
					IFile file = (IFile) r;
					CanvasUI.clearCanvasScreenshot(file);
				}
				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}

		swtRun(AssetsExplorerProjectBuildParticipant::refreshExplorer);
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		try {
			delta.accept(d -> {
				boolean changed = d.getKind() == IResourceDelta.ADDED || d.getKind() == IResourceDelta.CHANGED
						|| d.getKind() == IResourceDelta.REPLACED;

				if (d.getResource() instanceof IFile) {
					IFile f = (IFile) d.getResource();
					CanvasUI.clearCanvasScreenshot(f);
					if (changed && CanvasCore.isPrefabFile(f)) {
						CanvasUI.getCanvasScreenshotFile(f, true);
					}
				}

				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
		swtRun(AssetsExplorerProjectBuildParticipant::refreshExplorer);
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		try {
			IContainer webFolder = ProjectCore.getWebContentFolder(project);
			webFolder.accept(r -> {
				if (r instanceof IFile) {
					IFile f = (IFile) r;
					if (CanvasCore.isPrefabFile(f)) {
						CanvasUI.clearCanvasScreenshot(f);
						CanvasUI.getCanvasScreenshotFile(f, true);
					}
				}
				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
		swtRun(AssetsExplorerProjectBuildParticipant::refreshExplorer);
	}

	private static void refreshExplorer() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IViewReference[] refs = page.getViewReferences();
		for (IViewReference ref : refs) {
			if (ref.getId().equals(AssetExplorer.ID)) {
				AssetExplorer view = (AssetExplorer) ref.getView(false);
				if (view != null) {
					view.refreshContent();
				}
			}
		}
	}

}
