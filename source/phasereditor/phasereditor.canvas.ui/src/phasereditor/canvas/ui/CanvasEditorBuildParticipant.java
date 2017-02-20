package phasereditor.canvas.ui;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFilesValidationBuildParticipant;
import phasereditor.canvas.core.MissingPrefabModel;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;
import phasereditor.project.core.IProjectBuildParticipant;

/**
 * Build participant to rebuild the Canvas editors. It does not emit problems.
 * Problems are created in the {@link CanvasFilesValidationBuildParticipant}, when the
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
	public void projectDeleted(IProject project, Map<String, Object> env) {
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

					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

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
											if (movedTo == null) {
												editor.handleFileDelete();
											} else {
												IFile newFile = root.getFile(movedTo);
												editor.handleFileRename(newFile);
											}
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
							} else if (!packDelta.isEmpty() && packDelta.inProject(editor.getEditorInputFile().getProject())) {
								// rebuild if the asset pack delta affects the
								// project
								rebuild = true;
							} else {
								// rebuild if a prefab file was modified
								boolean[] value = { false };
								try {
									delta.accept(new IResourceDeltaVisitor() {

										@Override
										public boolean visit(IResourceDelta d) throws CoreException {
											IResource resource = d.getResource();
											if (resource instanceof IFile) {

												if (!CanvasCore.isPrefabFile((IFile) resource)) {
													return true;
												}

												// the list of all the files
												// affected by the operation
												Set<IFile> files = new HashSet<>();

												// add the main file
												files.add((IFile) resource);

												// add the file from a move
												{
													IPath path = d.getMovedFromPath();
													if (path != null) {
														files.add(root.getFile(path));
													}
												}

												// add the files from a move
												{
													IPath path = d.getMovedToPath();
													if (path != null) {
														files.add(root.getFile(path));
													}
												}

												for (IFile file : files) {
													// abort the search if the
													// file
													// is the same editor file
													if (file.equals(curFile)) {
														return true;
													}

													editor.getModel().getWorld().walk(obj -> {
														if (obj.isPrefabInstance()) {
															Prefab prefab = obj.getPrefab();
															if (prefab.getFile().equals(file)) {
																value[0] = true;
																return false;
															}
														} else if (obj instanceof MissingPrefabModel) {
															JSONObject data = ((MissingPrefabModel) obj).getSrcData();
															String filepath = data.getString("prefabFile");
															IFile prefabFile = curFile.getProject().getFile(filepath);
															if (prefabFile.equals(file)) {
																value[0] = true;
																return false;
															}
														}
														return true;
													});
												}
											}
											return true;
										}
									});
									rebuild = value[0];
								} catch (CoreException e) {
									AssetPackUI.logError(e);
								}
							}

							// rebuild the editor if it is the case
							if (rebuild) {
								out.println("Rebuild canvas editor: " + curFile.getFullPath());
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
