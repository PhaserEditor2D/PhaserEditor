package phasereditor.canvas.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class PasteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

		Clipboard cb = new Clipboard(HandlerUtil.getActiveShell(event).getDisplay());
		Object content = cb.getContents(transfer);
		if (content == null) {
			return null;
		}

		Object[] data = ((IStructuredSelection) content).toArray();
		cb.dispose();

		if (data != null) {
			CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
			List<IObjectNode> newnodes = editor.getCanvas().getCreateBehavior().paste(data);

			editor.getCanvas().getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
			editor.getCanvas().getSelectionBehavior().setSelection(new StructuredSelection(newnodes));
		}

		return null;
	}

}
