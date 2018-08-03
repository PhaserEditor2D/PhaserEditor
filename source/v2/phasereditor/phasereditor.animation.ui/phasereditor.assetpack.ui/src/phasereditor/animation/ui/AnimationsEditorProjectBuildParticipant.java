package phasereditor.animation.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.PlatformUI;

import phasereditor.project.core.IProjectBuildParticipant;

public class AnimationsEditorProjectBuildParticipant implements IProjectBuildParticipant {

	public AnimationsEditorProjectBuildParticipant() {
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		rebuildEditors();
	}

	private static void rebuildEditors() {
		rebuildEditors(null);
	}

	private static void rebuildEditors(IResourceDelta delta) {
		swtRun(new Runnable() {

			@Override
			public void run() {
				var refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for (var ref : refs) {
					if (ref.getId().equals(AnimationsEditor.ID)) {
						var editor = (AnimationsEditor) ref.getEditor(false);
						if (editor != null) {
							if (delta == null) {
								editor.build();
							} else {
								var file = editor.getEditorInput().getFile();
								var doit = new boolean[] { true };
								try {
									delta.accept(d -> {
										IResource resource = d.getResource();
										if (file.equals(resource)) {
											doit[0] = false;
											return false;
										}
										return true;
									});
								} catch (CoreException e) {
									e.printStackTrace();
								}

								if (doit[0]) {
									editor.build();
								}
							}
						}
					}
				}
			}
		});
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		rebuildEditors();
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		rebuildEditors(delta);
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		rebuildEditors();
	}

	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		rebuildEditors();
	}

}
