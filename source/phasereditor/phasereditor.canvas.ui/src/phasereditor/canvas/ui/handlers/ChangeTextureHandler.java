package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.ui.TextureDialog;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;
import phasereditor.canvas.ui.editors.grid.editors.PGridEditingSupport;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.BitmapTextControl;
import phasereditor.canvas.ui.shapes.BitmapTextNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.TextControl;
import phasereditor.canvas.ui.shapes.TextNode;

public class ChangeTextureHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		Shell shell = HandlerUtil.getActiveShell(event);
		Object[] selection = HandlerUtil.getCurrentStructuredSelection(event).toArray();

		for (Object obj : selection) {
			if (obj instanceof TextNode) {
				changeText(shell, (TextNode) obj);
				return null;
			} else if (obj instanceof BitmapTextNode) {
				changeText(shell, (BitmapTextNode) obj);
				return null;
			}
		}

		changeTexture(shell, editor, selection);

		return null;
	}

	private static void changeText(Shell shell, BitmapTextNode text) {
		if (text.getModel().isOverriding(BitmapTextModel.PROPSET_TEXT)) {
			BitmapTextControl control = (BitmapTextControl) text.getControl();
			PGridStringProperty prop = control.getTextProperty();
			String result = PGridEditingSupport.openLongStringDialog(prop, shell);
			if (result != null) {
				PGridEditingSupport.changeUndoablePropertyValue(result, prop);
			}
		} else {
			MessageDialog.openInformation(shell, "Change Text", "The 'text' property is read-only.");
		}
	}

	private static void changeText(Shell shell, TextNode text) {

		if (text.getModel().isOverriding(TextModel.PROPSET_TEXT)) {
			TextControl control = (TextControl) text.getControl();
			PGridStringProperty prop = control.getTextProperty();
			String result = PGridEditingSupport.openLongStringDialog(prop, shell);
			if (result != null) {
				PGridEditingSupport.changeUndoablePropertyValue(result, prop);
			}
		} else {
			MessageDialog.openInformation(shell, "Change Text", "The 'text' property is read-only.");
		}
	}

	private static void changeTexture(Shell shell, CanvasEditor editor, Object[] selection) {
		TextureDialog dlg = new TextureDialog(shell) {

			@Override
			protected Object getNoTextureValue() {
				return PGridFrameProperty.NULL_FRAME;
			}
		};

		dlg.setProject(editor.getEditorInputFile().getProject());

		Object textureModel;

		if (dlg.open() != Window.OK || (textureModel = dlg.getResult()) == null) {
			return;
		}

		editor.getCanvas().getHandlerBehavior().clear();

		CompositeOperation operations = new CompositeOperation();

		for (Object sel : selection) {

			IObjectNode sprite = (IObjectNode) sel;

			BaseSpriteModel model = (BaseSpriteModel) sprite.getModel();

			if (model.isPrefabInstance() && model.isPrefabReadOnly(BaseSpriteModel.PROPSET_TEXTURE)) {
				MessageDialog.openInformation(shell, "Change Texture",
						"The texture of the prefab instance '" + model.getEditorName() + "' is read-only.");
				continue;
			}

			operations.add(new SelectOperation(model.getId()));

			CanvasUI.changeSpriteTexture(sprite, textureModel, operations);

			operations.add(new SelectOperation(model.getId()));
		}

		editor.getCanvas().getUpdateBehavior().executeOperations(operations);
	}

}
