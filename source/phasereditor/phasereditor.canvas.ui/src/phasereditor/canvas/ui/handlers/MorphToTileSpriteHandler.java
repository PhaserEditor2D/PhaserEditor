package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.shapes.AtlasSpriteControl;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;
import phasereditor.canvas.ui.shapes.ImageSpriteControl;
import phasereditor.canvas.ui.shapes.TileSpriteControl;

public class MorphToTileSpriteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object[] sel = ((IStructuredSelection) HandlerUtil.getCurrentSelection(event)).toArray();

		List<Object> newSelection = new ArrayList<>();

		for (Object elem : sel) {
			BaseObjectControl<?> control = ((IObjectNode) elem).getControl();

			if (control instanceof ImageSpriteControl) {
				ImageSpriteControl srcControl = (ImageSpriteControl) control;
				ImageAssetModel source = srcControl.getModel().getAssetKey();

				morph(newSelection, srcControl.getNode(), source);

			} else if (control instanceof AtlasSpriteControl) {
				AtlasSpriteControl srcControl = (AtlasSpriteControl) control;
				FrameItem source = srcControl.getModel().getFrame();

				morph(newSelection, srcControl.getNode(), source);

			}
		}

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		editor.getCanvas().getWorldModel().firePropertyChange(WorldModel.PROP_DIRTY);
		editor.getCanvas().getSelectionBehavior().setSelection(new StructuredSelection(newSelection));

		return null;
	}

	private static void morph(List<Object> newSelection, ISpriteNode srcNode, IAssetKey assetKey) {
		BaseObjectControl<?> srcControl = srcNode.getControl();
		GroupControl parent = srcControl.getIObjectNode().getGroup().getControl();
		BaseSpriteModel srcModel = (BaseSpriteModel) srcControl.getModel();

		TileSpriteModel model = new TileSpriteModel(parent.getModel(), assetKey);

		model.updateWith(srcModel);
		model.setWidth(srcControl.getTextureWidth());
		model.setHeight(srcControl.getTextureHeight());

		TileSpriteControl tileControl = new TileSpriteControl(srcControl.getCanvas(), model);

		int i = parent.getNode().getChildren().indexOf(srcNode);
		parent.removeChild(srcControl.getIObjectNode());
		parent.addChild(i, tileControl.getIObjectNode());

		newSelection.add(tileControl.getNode());
	}

}
