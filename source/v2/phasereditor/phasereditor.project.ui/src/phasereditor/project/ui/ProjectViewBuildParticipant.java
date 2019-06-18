package phasereditor.project.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.ui.PlatformUI;

import phasereditor.project.core.IProjectBuildParticipant;

public class ProjectViewBuildParticipant implements IProjectBuildParticipant {

	public ProjectViewBuildParticipant() {
	}

	private static List<ProjectView> getOpenViews() {
		var list = new ArrayList<ProjectView>();
		for (var win : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (var page : win.getPages()) {
				for (var ref : page.getViewReferences()) {
					if (ref.getId().equals(ProjectView.ID)) {
						var view = ref.getPart(false);
						if (view != null) {
							list.add((ProjectView) view);
						}
					}
				}
			}
		}
		return list;
	}

	private static void refreshViews() {
		swtRun(ProjectViewBuildParticipant::realRefreshViews);

		// lets do this because there are screenshot builders that run async.
		swtRun(500, ProjectViewBuildParticipant::realRefreshViews);
	}

	private static void realRefreshViews() {
		for (var view : getOpenViews()) {
			view.refresh();
		}
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		//
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		refreshViews();
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		refreshViews();
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		refreshViews();
	}

	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		refreshViews();
	}

}
