package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.TileSpriteNode;

public class ResizeTileHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		ObjectCanvas canvas = editor.getCanvas();

		IObjectNode sprite = (IObjectNode) getSelected(event);

		BaseObjectModel model = sprite.getModel();
		
		if (sprite instanceof TileSpriteNode) {
			if (model.isOverriding(TileSpriteModel.PROPSET_TILE_SIZE)) {
				canvas.getHandlerBehavior().editTile(sprite);
			} else {
				MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Resize Tile Sprite", "The 'tileSize' of the prefab instance '" + model.getEditorName() + "' is read-only.");
			}
		} else {
			// maybe we want to morph it into a tile sprite
			if (MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Resize Tile Sprite",
					"The object '" + model.getEditorName() + "' is not a tileSprite."
							+ " Do you want to convert it first?")) {

				// morph the selected sprite into a tile sprite
				MorphToTileSpriteHandler morph = new MorphToTileSpriteHandler();
				morph.execute(event);

				// find the new selection node and enable tile resize handlers.
				canvas.getHandlerBehavior().editTile((IObjectNode) getSelected(event));
			}
		}

		return null;
	}

	private static Object getSelected(ExecutionEvent event) {
		return ((IStructuredSelection) HandlerUtil.getCurrentSelection(event)).getFirstElement();
	}
}
