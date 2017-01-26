package phasereditor.canvas.core.contenttype;

import phasereditor.canvas.core.CanvasType;

public class PrefabGroupContentTypeDescriber extends CanvasContentTypeDescriber{

	@Override
	protected boolean acceptCanvasType(CanvasType type) {
		return type == CanvasType.GROUP;
	}
}
