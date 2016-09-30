package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.AtlasSpriteControl;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;
import phasereditor.canvas.ui.shapes.ImageSpriteControl;
import phasereditor.canvas.ui.shapes.SpritesheetSpriteControl;

/**
 * 
 * @author arian
 *
 */
public abstract class AbstractMorphHandler extends AbstractHandler {

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
				IAssetKey source;

				if (control instanceof ImageSpriteControl) {
					source = ((ImageSpriteControl) control).getModel().getAssetKey();
				} else if (control instanceof AtlasSpriteControl) {
					source = ((AtlasSpriteControl) control).getModel().getAssetKey();
				} else if (control instanceof SpritesheetSpriteControl) {
					source = ((SpritesheetSpriteControl) control).getModel().getAssetKey();
				} else {
					source = null;
				}

				if (source != null) {
					String id = addMorph(operations, (ISpriteNode) elem, source);
					afterSelection.add(id);
				}
			}

			operations.add(new SelectOperation(afterSelection));
		}

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		editor.getCanvas().getUpdateBehavior().executeOperations(operations);

		return null;
	}

	protected final String addMorph(CompositeOperation operations, ISpriteNode srcNode, IAssetKey assetKey) {
		// delete source
		operations.add(new DeleteNodeOperation(srcNode.getModel().getId()));

		// create morph
		GroupNode parent = srcNode.getGroup();
		BaseObjectModel dstModel = createMorphModel(srcNode, assetKey, parent);

		int i = parent.getNode().getChildren().indexOf(srcNode);
		operations.add(new AddNodeOperation(dstModel.toJSON(true), i, dstModel.getX(), dstModel.getY(),
				parent.getModel().getId()));
		return dstModel.getId();
	}

	protected abstract BaseObjectModel createMorphModel(ISpriteNode srcNode, IAssetKey assetKey, GroupNode parent);

}
