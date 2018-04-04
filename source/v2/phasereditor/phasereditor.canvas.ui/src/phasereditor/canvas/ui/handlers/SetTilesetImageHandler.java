package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.editors.PGridEditingSupport;
import phasereditor.canvas.ui.shapes.TilemapSpriteNode;

public class SetTilesetImageHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);

		TilemapSpriteNode sprite = (TilemapSpriteNode) sel.getFirstElement();

		PGridFrameProperty prop = sprite.getControl().getTilesetImageProperty();

		Object frame = PGridEditingSupport.openSelectFrameDialog(prop, HandlerUtil.getActiveShell(event));

		if (frame != null) {
			PGridEditingSupport.executeChangePropertyValueOperation(frame, prop);
		}

		return null;
	}

}
