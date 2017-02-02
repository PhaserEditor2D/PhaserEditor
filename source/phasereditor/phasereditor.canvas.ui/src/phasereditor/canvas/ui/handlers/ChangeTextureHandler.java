package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.editors.TextureDialog;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class ChangeTextureHandler extends AbstractHandler {

	@SuppressWarnings("rawtypes")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		Object sel = HandlerUtil.getCurrentStructuredSelection(event).getFirstElement();

		TextureDialog dlg = new TextureDialog(HandlerUtil.getActiveShell(event));

		dlg.setProject(editor.getEditorInputFile().getProject());

		ISpriteNode sprite = (ISpriteNode) sel;
		BaseSpriteModel model = sprite.getModel();

		if (model.isPrefabInstance() && model.isPrefabReadOnly(BaseSpriteModel.PROPSET_TEXTURE)) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Change Texture",
					"The texture of this prefab instance is read-only.");
			return null;
		}

		if (model instanceof AssetSpriteModel) {
			dlg.setSelectedItem(((AssetSpriteModel) model).getAssetKey().getSharedVersion());
		}

		if (dlg.open() == Window.OK) {
			Object textureModel = dlg.getResult();

			editor.getCanvas().getHandlerBehavior().clear();
			
			CompositeOperation operations = new CompositeOperation();

			operations.add(new SelectOperation(model.getId()));

			CanvasUI.changeSpriteTexture(sprite, textureModel, operations);

			operations.add(new SelectOperation(model.getId()));

			editor.getCanvas().getUpdateBehavior().executeOperations(operations);
		}

		return null;
	}

}
