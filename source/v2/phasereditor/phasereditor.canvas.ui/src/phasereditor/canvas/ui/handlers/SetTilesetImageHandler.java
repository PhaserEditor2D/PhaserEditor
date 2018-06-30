package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.editors.CanvasPGridEditingSupport;
import phasereditor.canvas.ui.shapes.TilemapSpriteNode;

public class SetTilesetImageHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);

		TilemapSpriteNode sprite = (TilemapSpriteNode) sel.getFirstElement();

		PGridFrameProperty prop = sprite.getControl().getTilesetImageProperty();

		Object frame = CanvasPGridEditingSupport.openSelectFrameDialog(prop, HandlerUtil.getActiveShell(event));

		if (frame != null) {
			editor.getPropertyGrid().getEditSupport().executeChangePropertyValueOperation(frame, prop);
		}

		return null;
	}

}
