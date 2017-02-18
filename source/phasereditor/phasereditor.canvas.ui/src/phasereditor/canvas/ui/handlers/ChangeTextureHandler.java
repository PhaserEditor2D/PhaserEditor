package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.editors.TextureDialog;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class ChangeTextureHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		TextureDialog dlg = new TextureDialog(HandlerUtil.getActiveShell(event));

		dlg.setProject(editor.getEditorInputFile().getProject());

		Object textureModel;

		if (dlg.open() == Window.OK) {
			textureModel = dlg.getResult();
		} else {
			return null;
		}

		editor.getCanvas().getHandlerBehavior().clear();

		CompositeOperation operations = new CompositeOperation();

		for (Object sel : HandlerUtil.getCurrentStructuredSelection(event).toArray()) {

			IObjectNode sprite = (IObjectNode) sel;

			BaseSpriteModel model = (BaseSpriteModel) sprite.getModel();

			if (model.isPrefabInstance() && model.isPrefabReadOnly(BaseSpriteModel.PROPSET_TEXTURE)) {
				MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Change Texture",
						"The texture of the prefab instance '" + model.getEditorName() + "' is read-only.");
				continue;
			}

			operations.add(new SelectOperation(model.getId()));

			CanvasUI.changeSpriteTexture(sprite, textureModel, operations);

			operations.add(new SelectOperation(model.getId()));
		}

		editor.getCanvas().getUpdateBehavior().executeOperations(operations);

		return null;
	}

}
