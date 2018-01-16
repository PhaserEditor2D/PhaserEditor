package phasereditor.canvas.ui.handlers;

import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class MorphToTileSpriteHandler extends AbstractMorphHandler<TileSpriteModel> {

	public MorphToTileSpriteHandler() {
		super(TileSpriteModel.class);
	}

	@Override
	protected TileSpriteModel createMorphModel(ISpriteNode srcNode, MorphToArgs args, GroupNode parent) {
		MorphToSpriteArgs sprirteArgs = (MorphToSpriteArgs) args;

		TileSpriteModel model = new TileSpriteModel(parent.getModel(), sprirteArgs.asset);

		model.updateWith(srcNode.getModel());

		model.setWidth(srcNode.getControl().getTextureWidth());
		model.setHeight(srcNode.getControl().getTextureHeight());

		ObjectCanvas canvas = srcNode.getControl().getCanvas();
		if (canvas.getEditor().getModel().getType() == CanvasType.SPRITE) {
			EditorSettings settings = canvas.getSettingsModel();
			settings.setBaseClass("Phaser.TileSprite");
		}

		return model;
	}

}
