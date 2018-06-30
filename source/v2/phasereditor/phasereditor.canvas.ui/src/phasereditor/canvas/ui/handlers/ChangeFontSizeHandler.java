package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.editors.CanvasPGridEditingSupport;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.BitmapTextNode;
import phasereditor.canvas.ui.shapes.ITextSpriteNode;
import phasereditor.canvas.ui.shapes.TextNode;
import phasereditor.ui.properties.PGridProperty;

public class ChangeFontSizeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String amount = event.getParameter("phasereditor.canvas.ui.fontSizeAmount");
		Object[] selection = HandlerUtil.getCurrentStructuredSelection(event).toArray();
		CompositeOperation operations = new CompositeOperation();
		for (Object obj : selection) {
			ITextSpriteNode node = (ITextSpriteNode) obj;
			int fontSize = node.getModel().getFontSize();
			PGridProperty<?> prop;
			if (node instanceof BitmapTextNode) {
				prop = ((BitmapTextNode) node).getControl().getSizeProperty();
			} else {
				prop = ((TextNode) node).getControl().getFontSizeProperty();
			}

			int sign = amount.equals("up") ? 1 : -1;
			int delta = 1;

			if (fontSize > 72) {
				delta = 16;
			} else if (fontSize >= 40) {
				delta = 8;
			} else if (fontSize >= 28) {
				delta = 4;
			} else if (fontSize >= 18) {
				delta = 2;
			}

			delta *= sign;

			operations
					.add(CanvasPGridEditingSupport.makeChangePropertyValueOperation(Double.valueOf(fontSize + delta), prop));
		}

		((CanvasEditor) HandlerUtil.getActiveEditor(event)).getCanvas().getUpdateBehavior()
				.executeOperations(operations);

		return null;
	}

}
