package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.SelectionNode;
import phasereditor.canvas.ui.editors.TileHandlerGroup;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.TileSpriteNode;

public class ResizeTileHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		ObjectCanvas canvas = editor.getCanvas();

		SelectionNode selnode = getSelectedNode(canvas);

		IObjectNode sprite = selnode.getObjectNode();

		if (sprite instanceof TileSpriteNode) {
			selnode.showHandlers(TileHandlerGroup.class);
		} else {
			// maybe we want to morph it into a tile sprite
			if (MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Resize Tile Sprite",
					"The object '" + sprite.getModel().getEditorName() + "' is not a tileSprite."
							+ " Do you want to convert it first?")) {

				// morph the selected sprite into a tile sprite
				MorphToTileSpriteHandler morph = new MorphToTileSpriteHandler();
				morph.execute(event);

				// find the new selection node and enable tile resize handlers.
				getSelectedNode(canvas).showHandlers(TileHandlerGroup.class);
			}
		}

		return null;
	}

	private static SelectionNode getSelectedNode(ObjectCanvas canvas) {
		return (SelectionNode) canvas.getSelectionPane().getChildren().get(0);
	}
}
