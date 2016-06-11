package phasereditor.canvas.ui.handlers;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class MorphToButtonSpriteHandler extends AbstractMorphHandler {

	@Override
	protected BaseObjectModel createMorphModel(ISpriteNode srcNode, IAssetKey assetKey, GroupNode parent) {
		ButtonSpriteModel dstModel = new ButtonSpriteModel(parent.getModel(), (IAssetFrameModel) assetKey);
		dstModel.updateWith(srcNode.getModel());
		return dstModel;
	}
}
