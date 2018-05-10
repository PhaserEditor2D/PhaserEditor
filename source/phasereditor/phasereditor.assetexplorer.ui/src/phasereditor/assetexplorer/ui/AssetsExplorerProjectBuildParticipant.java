package phasereditor.assetexplorer.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetexplorer.ui.views.AssetExplorer;
import phasereditor.project.core.IProjectBuildParticipant;

public class AssetsExplorerProjectBuildParticipant implements IProjectBuildParticipant {

	public AssetsExplorerProjectBuildParticipant() {
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		// nothing
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		swtRun(() -> refreshExplorer(project));
	}

	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		swtRun(() -> refreshExplorer(project));
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		swtRun(() -> refreshExplorer(project));
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		swtRun(() -> refreshExplorer(project));
	}

	private static void refreshExplorer(IProject project) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IViewReference[] refs = page.getViewReferences();
		for (IViewReference ref : refs) {
			if (ref.getId().equals(AssetExplorer.ID)) {
				AssetExplorer view = (AssetExplorer) ref.getView(false);
				if (view != null) {
					view.refreshContent(project);
				}
			}
		}
	}

}
