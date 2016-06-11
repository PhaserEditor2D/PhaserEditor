package phasereditor.canvas.ui.handlers;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class MorphToTileSpriteHandler extends AbstractMorphHandler {

	@Override
	protected TileSpriteModel createMorphModel(ISpriteNode srcNode, IAssetKey assetKey, GroupNode parent) {
		TileSpriteModel model = new TileSpriteModel(parent.getModel(), assetKey);
		model.updateWith(srcNode.getModel());
		model.setWidth(srcNode.getControl().getTextureWidth());
		model.setHeight(srcNode.getControl().getTextureHeight());
		return model;
	}

}
