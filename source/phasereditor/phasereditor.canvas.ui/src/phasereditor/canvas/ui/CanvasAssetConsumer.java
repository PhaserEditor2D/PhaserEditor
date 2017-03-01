package phasereditor.canvas.ui;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetConsumer;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.IAssetReplacer;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;

public class CanvasAssetConsumer implements IAssetConsumer {

	public CanvasAssetConsumer() {
	}
	
	@Override
	public void installTooltips(TreeViewer viewer) {
		CanvasUI.installCanvasTooltips(viewer);
	}
	
	@Override
	public FindAssetReferencesResult getAssetReferences(IAssetKey assetKey, IProgressMonitor monitor) {
		return CanvasUI.findAllAssetReferences(assetKey, monitor);
	}

	@Override
	public Collection<IFile> getFilesUsingAsset(AssetModel asset) {
		Set<IFile> files = new HashSet<>();
		addUsedInEditors(asset, files);
		addUsedInFiles(asset, files);

		return files;
	}

	private static void addUsedInFiles(AssetModel asset, Set<IFile> files) {
		IProject assetProject = asset.getPack().getFile().getProject();

		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(assetProject);

		for (CanvasFile cfile : cfiles) {
			IFile file = cfile.getFile();

			if (files.contains(file)) {
				continue;
			}

			try (InputStream contents = file.getContents();) {
				JSONObject data = new JSONObject(new JSONTokener(contents));
				CanvasModel model = new CanvasModel(file);
				model.read(data);
				addUsedInModel(asset, files, model.getWorld());
			} catch (Exception e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, e.getMessage(), e));
			}
		}

	}

	private static void addUsedInEditors(AssetModel asset, Set<IFile> files) {
		IProject assetProject = asset.getPack().getFile().getProject();

		IEditorReference[] list = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getEditorReferences();

		for (IEditorReference ref : list) {

			if (ref.getId().equals(CanvasEditor.ID)) {
				CanvasEditor editor = (CanvasEditor) ref.getEditor(false);
				if (editor != null) {
					IFile editorFile = editor.getEditorInputFile();

					if (editorFile.getProject() != assetProject) {
						continue;
					}

					WorldModel model = editor.getModel().getWorld();
					addUsedInModel(asset, files, model);
				}
			}
		}
	}

	private static void addUsedInModel(AssetModel asset, Set<IFile> files, WorldModel model) {
		model.walk_stopIfFalse(m -> {
			if (m instanceof AssetSpriteModel) {
				IAssetKey key = ((AssetSpriteModel<?>) m).getAssetKey();

				if (key == null) {
					return Boolean.TRUE;
				}

				key = key.getSharedVersion();

				if (key == null) {
					return Boolean.TRUE;
				}

				AssetModel objAsset = key.getAsset();
				if (objAsset == asset) {
					files.add(model.getFile());
					return Boolean.FALSE;
				}
			}
			return Boolean.TRUE;
		});
	}

	/* (non-Javadoc)
	 * @see phasereditor.assetpack.core.IAssetConsumer#getAssetReplacer()
	 */
	@Override
	public IAssetReplacer getAssetReplacer() {
		return new CanvasAssetReplacer();
	}

}
