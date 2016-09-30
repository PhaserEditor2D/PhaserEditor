package phasereditor.canvas.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
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

		List<IObjectNode> nodes = SelectionBehavior.filterSelection((IStructuredSelection) content);

		cb.dispose();

		if (nodes != null) {
			CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

			for (IObjectNode node : nodes) {
				CanvasEditor srcEditor = node.getControl().getCanvas().getEditor();
				if (!srcEditor.getEditorInputFile().getProject().equals(editor.getEditorInputFile().getProject())) {
					MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Canvas",
							"Cannot paste objects from other projects.");
					return null;
				}
			}

			editor.getCanvas().getCreateBehavior().paste(nodes.toArray());
		}

		return null;
	}

}
