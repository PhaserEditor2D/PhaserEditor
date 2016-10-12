package phasereditor.canvas.ui;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.canvas.core.CanvasFilesBuildParticipant;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;
import phasereditor.project.core.IProjectBuildParticipant;

/**
 * Build participant to rebuild the Canvas editors. It does not emit problems.
 * Problems are created in the {@link CanvasFilesBuildParticipant}, when the
 * editor is saved.
 * 
 * @author arian
 *
 */
public class CanvasEditorBuildParticipant implements IProjectBuildParticipant {

	public CanvasEditorBuildParticipant() {
	}
	
	@Override
	public void clean(IProject project, Map<String, Object> env) {
		// nothing
	}
	
	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		build(null, true);
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		PackDelta packDelta = AssetPackBuildParticipant.getData(env);
		build(packDelta, false);
	}

	private static void build(PackDelta packDelta, boolean fullBuild) {
		try {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					IEditorReference[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.getEditorReferences();

					for (IEditorReference ref : editors) {
						if (!ref.getId().equals(CanvasEditor.ID)) {
							continue;
						}

						CanvasEditor editor = (CanvasEditor) ref.getEditor(false);

						if (editor != null) {
							UpdateBehavior updateBehavior = editor.getCanvas().getUpdateBehavior();

							if (fullBuild || packDelta.inProject(editor.getEditorInputFile().getProject())) {
								updateBehavior.rebuild();
							}
						}
					}
				}
			});
		} catch (Exception e) {
			CanvasUI.handleError(e);
		}
	}

}
