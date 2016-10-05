package phasereditor.canvas.ui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.IAssetConsumer;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.ui.editors.CanvasEditor;

public class CanvasAssetConsumer implements IAssetConsumer {

	public CanvasAssetConsumer() {
	}

	@Override
	public Collection<IFile> getFilesUsingAsset(AssetModel asset) {
		Set<IFile> files = new HashSet<>();
		// to verify that this work lets inspect in the canvas editors first.

		IEditorReference[] list = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getEditorReferences();
		for (IEditorReference ref : list) {

			if (ref.getId().equals(CanvasEditor.ID)) {
				CanvasEditor editor = (CanvasEditor) ref.getEditor(false);
				if (editor != null) {

					editor.getModel().getWorld().walk(m -> {
						if (m instanceof AssetSpriteModel) {
							IAssetKey key = ((AssetSpriteModel<?>) m).getAssetKey();
							if (key != null) {
								key = key.findFreshVersion();
								AssetModel objAsset = key.getAsset();
								if (objAsset == asset) {
									files.add(editor.getEditorInputFile());
								}
							}
						}
					});
				}
			}
		}

		return files;
	}

}
