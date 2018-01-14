package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.PGridBitmapTextFontProperty;
import phasereditor.canvas.ui.editors.grid.editors.BitmapTextFontDialog;
import phasereditor.canvas.ui.editors.grid.editors.PGridEditingSupport;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.BitmapTextNode;

public class ChangeBitmapFontHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		Object[] selection = HandlerUtil.getCurrentStructuredSelection(event).toArray();

		BitmapTextFontDialog dlg = new BitmapTextFontDialog(HandlerUtil.getActiveShell(event));

		dlg.setProject(editor.getEditorInputFile().getProject());
		dlg.setSelectedFont(((BitmapTextNode) selection[0]).getModel().getAssetKey());

		if (dlg.open() == Window.OK) {
			CompositeOperation operations = new CompositeOperation();

			BitmapFontAssetModel font = dlg.getSelectedFont();

			for (Object obj : selection) {
				PGridBitmapTextFontProperty prop = ((BitmapTextNode) obj).getControl().getFontProperty();
				operations.add(PGridEditingSupport.makeChangePropertyValueOperation(font, prop));
			}

			editor.getCanvas().getUpdateBehavior().executeOperations(operations);
		}

		return null;
	}

}
