package phasereditor.canvas.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFilesBuildParticipant;
import phasereditor.canvas.core.Prefab;
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
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		// nothing
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
			swtRun(new Runnable() {

				@SuppressWarnings("boxing")
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
							IFile curFile = ((FileEditorInput) editor.getEditorInput()).getFile();

							// update editor name (and image?)
							if (!fullBuild) {
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

							boolean rebuild = false;

							if (fullBuild) {
								rebuild = true;
							} else if (packDelta.inProject(editor.getEditorInputFile().getProject())) {
								rebuild = true;
							} else {
								boolean[] value = { false };
								try {
									delta.accept(d -> {
										IResource resource = d.getResource();
										if (resource instanceof IFile && resource.exists()) {
											IFile file = (IFile) resource;
											if (file.equals(curFile)) {
												return true;
											}
											if (CanvasCore.isCanvasFile(file)) {
												editor.getModel().getWorld().walk(obj -> {
													if (obj.isPrefabInstance()) {
														Prefab prefab = obj.getPrefab();
														if (prefab.getFile().equals(file)) {
															value[0] = true;
															return false;
														}
													}
													return true;
												});
											}
										}
										return true;
									});
									rebuild = value[0];
								} catch (CoreException e) {
									AssetPackUI.logError(e);
								}
							}

							if (rebuild) {
								UpdateBehavior updateBehavior = editor.getCanvas().getUpdateBehavior();
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
