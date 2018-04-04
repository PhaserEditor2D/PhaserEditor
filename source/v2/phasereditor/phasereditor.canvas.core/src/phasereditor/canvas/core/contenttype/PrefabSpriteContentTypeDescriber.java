package phasereditor.canvas.core.contenttype;

import phasereditor.canvas.core.CanvasType;

public class PrefabSpriteContentTypeDescriber extends CanvasContentTypeDescriber{

	@Override
	protected boolean acceptCanvasType(CanvasType type) {
		return type == CanvasType.SPRITE;
	}
}
