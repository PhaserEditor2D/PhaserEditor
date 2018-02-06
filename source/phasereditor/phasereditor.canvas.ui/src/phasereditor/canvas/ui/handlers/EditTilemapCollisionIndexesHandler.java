package phasereditor.canvas.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.grid.PGridTilemapIndexesProperty;
import phasereditor.canvas.ui.editors.grid.editors.PGridEditingSupport;
import phasereditor.canvas.ui.shapes.TilemapSpriteNode;

public class EditTilemapCollisionIndexesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);
		TilemapSpriteNode sprite = (TilemapSpriteNode) sel.getFirstElement();
		PGridTilemapIndexesProperty prop = sprite.getControl().getCollisionIndexesProperty();
		List<Integer> indexes = PGridEditingSupport.openTilemapIndexesDialog(prop, HandlerUtil.getActiveShell(event));
		if (indexes != null) {
			PGridEditingSupport.executeChangePropertyValueOperation(indexes, prop);
		}
		return null;
	}

}
