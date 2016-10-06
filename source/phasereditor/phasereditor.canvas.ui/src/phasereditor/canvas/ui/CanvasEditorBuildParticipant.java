package phasereditor.canvas.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;
import phasereditor.project.core.IProjectBuildParticipant;

public class CanvasEditorBuildParticipant implements IProjectBuildParticipant {

	public CanvasEditorBuildParticipant() {
	}

	@Override
	public void build(BuildArgs args) throws CoreException {
		try {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					IEditorReference[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.getEditorReferences();
					for (IEditorReference ref : editors) {
						if (ref.getId().equals(CanvasEditor.ID)) {
							CanvasEditor editor = (CanvasEditor) ref.getEditor(false);
							if (editor != null) {
								UpdateBehavior updateBehavior = editor.getCanvas().getUpdateBehavior();
								updateBehavior.rebuild(args.getAssetDelta());
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
