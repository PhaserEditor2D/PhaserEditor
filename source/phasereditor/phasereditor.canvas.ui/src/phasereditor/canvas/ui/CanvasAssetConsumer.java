package phasereditor.canvas.ui;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.IAssetConsumer;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.project.core.ProjectCore;

public class CanvasAssetConsumer implements IAssetConsumer {

	public CanvasAssetConsumer() {
	}

	@Override
	public Collection<IFile> getFilesUsingAsset(AssetModel asset) {
		Set<IFile> files = new HashSet<>();
		addUsedInEditors(asset, files);
		// TODO: we should keep all models in memory and build them, in the
		// project builder
		addUsedInFiles(asset, files);

		return files;
	}

	private static void addUsedInFiles(AssetModel asset, Set<IFile> files) {
		IProject assetProject = asset.getPack().getFile().getProject();

		try {

			IContainer webContent = ProjectCore.getWebContentFolder(assetProject);
			webContent.accept(r -> {
				if (files.contains(r)) {
					return true;
				}

				if (r instanceof IFile) {
					IFile file = (IFile) r;
					if (CanvasCore.isCanvasFile(file)) {
						try (InputStream contents = file.getContents();) {
							JSONObject data = new JSONObject(new JSONTokener(contents));
							CanvasModel model = new CanvasModel(file);
							model.read(data);
							addUsedInModel(asset, files, model.getWorld());
						} catch (Exception e) {
							StatusManager.getManager()
									.handle(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, e.getMessage(), e));
						}
					}
				}

				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
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
		model.walk(m -> {
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

}
