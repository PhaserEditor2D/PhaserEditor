package phasereditor.canvas.ui.handlers;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class MorphToSpriteHandler extends AbstractMorphHandler {

	@Override
	protected BaseObjectModel createMorphModel(ISpriteNode srcNode, IAssetKey assetKey, GroupNode parent) {
		BaseSpriteModel model = CanvasModelFactory.createModel(parent.getModel(), assetKey);
		model.updateWith(srcNode.getModel());
		return model;
	}
}
