package phasereditor.canvas.ui.handlers;

import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class MorphToSpriteHandler extends AbstractMorphHandler<BaseSpriteModel> {

	public MorphToSpriteHandler() {
		super(BaseSpriteModel.class);
	}
	
	@Override
	protected BaseSpriteModel createMorphModel(ISpriteNode srcNode, Object source, GroupNode parent) {
		BaseSpriteModel model = (BaseSpriteModel) CanvasModelFactory.createModel(parent.getModel(), source);
		model.updateWith(srcNode.getModel());
		
		
		ObjectCanvas canvas = srcNode.getControl().getCanvas();
		if (canvas.getEditor().getModel().getType() == CanvasType.SPRITE) {
			EditorSettings settings = canvas.getSettingsModel();
			settings.setBaseClass("Phaser.Sprite");
		}

		return model;
	}
}
