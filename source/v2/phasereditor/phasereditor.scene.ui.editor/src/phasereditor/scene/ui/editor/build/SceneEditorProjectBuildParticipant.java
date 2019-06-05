package phasereditor.scene.ui.editor.build;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

import phasereditor.project.ui.build.BaseEditorBuildParticipant;
import phasereditor.scene.core.PackReferencesCollector;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.SceneUIEditor;

public class SceneEditorProjectBuildParticipant extends BaseEditorBuildParticipant<SceneEditor> {

	public SceneEditorProjectBuildParticipant() {
		super(SceneEditor.ID);
	}

	@Override
	protected void buildEditor(SceneEditor editor) {
		editor.build();
	}

	@Override
	protected IFile getEditorFile(SceneEditor editor) {
		return editor.getEditorInput().getFile();
	}

	@Override
	protected void reloadEditorFile(SceneEditor editor) {
		editor.reloadFile();
	}

	@Override
	protected boolean acceptDelta(SceneEditor editor, IResourceDelta delta) {

		var collector = new PackReferencesCollector(editor.getSceneModel(), editor.getAssetFinder());

		var files = collector.collectAssetKeys()

				.stream()

				.flatMap(key -> Arrays.stream(key.getAsset().computeUsedFiles()))

				.collect(toSet());

		try {
			var found = new boolean[] { false };
			delta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta2) throws CoreException {
					var res = delta2.getResource();
					if (files.contains(res)) {
						found[0] = true;
						return false;
					}
					return true;
				}
			});
			return found[0];
		} catch (CoreException e) {
			SceneUIEditor.logError(e);
		}

		return false;
	}
}
