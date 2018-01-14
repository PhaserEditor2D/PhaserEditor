package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.canvas.core.ITextSpriteModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.SelectTextureDialog;
import phasereditor.canvas.ui.editors.grid.editors.BitmapTextFontDialog;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * 
 * @author arian
 *
 */
public abstract class AbstractMorphHandler<T extends BaseObjectModel> extends AbstractHandler {

	private Class<T> _morphToType;

	public AbstractMorphHandler(Class<T> morphToType) {
		_morphToType = morphToType;
	}

	@Override
	public final Object execute(ExecutionEvent event) throws ExecutionException {
		Object[] sel = ((IStructuredSelection) HandlerUtil.getCurrentSelection(event)).toArray();

		CompositeOperation operations = new CompositeOperation();

		{
			List<String> beforeSelection = new ArrayList<>();

			for (Object elem : sel) {
				beforeSelection.add(((IObjectNode) elem).getModel().getId());
			}
			operations.add(new SelectOperation(beforeSelection));
		}

		{
			List<String> afterSelection = new ArrayList<>();

			for (Object elem : sel) {
				BaseObjectControl<?> control = ((IObjectNode) elem).getControl();
				BaseObjectModel model = control.getModel();
				IAssetKey source = null;

				boolean doMorph = true;
				if (_morphToType == BitmapTextModel.class) {
					BitmapTextFontDialog dlg = new BitmapTextFontDialog(HandlerUtil.getActiveShell(event));
					CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
					dlg.setProject(editor.getEditorInputFile().getProject());
					if (dlg.open() == Window.OK) {
						source = dlg.getSelectedFont();
					} else {
						continue;
					}
				} else if (model instanceof TextModel || model instanceof BitmapTextModel) {
					boolean morphingToOtherText = ITextSpriteModel.class.isAssignableFrom(_morphToType);
					if (!morphingToOtherText) {
						SelectTextureDialog dlg = new SelectTextureDialog(HandlerUtil.getActiveShell(event), "Select Texture");
						CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
						dlg.setProject(editor.getEditorInputFile().getProject());
						if (dlg.open() == Window.OK) {
							source = (IAssetKey) dlg.getSelection().getFirstElement();
						} else {
							continue;
						}
					}
				} else if (model instanceof AssetSpriteModel<?>) {
					source = ((AssetSpriteModel<?>) model).getAssetKey();
					doMorph = source != null;
				}

				if (doMorph) {
					String id = addMorph(operations, (ISpriteNode) elem, source);
					afterSelection.add(id);
				}
			}

			operations.add(new SelectOperation(afterSelection));
		}

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ObjectCanvas canvas = editor.getCanvas();

		canvas.getUpdateBehavior().executeOperations(operations);

		if (_morphToType == TextModel.class) {
			canvas.getSelectionBehavior().updateSelectedNodes_async();
		}

		editor.getSettingsPage().refresh();

		return null;
	}

	public Class<T> getMorphToType() {
		return _morphToType;
	}

	protected final String addMorph(CompositeOperation operations, ISpriteNode srcNode, Object source) {
		// delete source
		operations.add(new DeleteNodeOperation(srcNode.getModel().getId()));

		// create morph
		GroupNode parent = srcNode.getGroup();
		BaseObjectModel dstModel = createMorphModel(srcNode, source, parent);

		@SuppressWarnings("unlikely-arg-type")
		int i = parent.getNode().getChildren().indexOf(srcNode);
		operations.add(new AddNodeOperation(dstModel.toJSON(false), i, dstModel.getX(), dstModel.getY(),
				parent.getModel().getId()));
		return dstModel.getId();
	}

	protected abstract T createMorphModel(ISpriteNode srcNode, Object source, GroupNode parent);

}
