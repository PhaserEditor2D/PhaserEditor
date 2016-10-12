package phasereditor.canvas.ui;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.ui.AssetPackUI;
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
		build(null, null, true);
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		PackDelta packDelta = AssetPackBuildParticipant.getData(env);
		build(delta, packDelta, false);
	}

	private static void build(IResourceDelta delta, PackDelta packDelta, boolean fullBuild) {
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
							// update editor name (and image?)
							if (!fullBuild) {
								IFile curFile = ((FileEditorInput) editor.getEditorInput()).getFile();
								try {
									delta.accept(d -> {
										if (d.getKind() == IResourceDelta.REMOVED && d.getResource().equals(curFile)) {
											IPath movedTo = d.getMovedToPath();
											IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(movedTo);
											editor.handleFileRename(newFile);
										}
										return true;
									});
								} catch (CoreException e) {
									AssetPackUI.logError(e);
								}
							}

							// update editor content
							UpdateBehavior updateBehavior = editor.getCanvas().getUpdateBehavior();
							if (fullBuild || packDelta.inProject(editor.getEditorInputFile().getProject())) {
								updateBehavior.rebuild();
							}

						}
					}
				}
			});
		} catch (Exception e) {
			CanvasUI.logError(e);
		}
	}

}
