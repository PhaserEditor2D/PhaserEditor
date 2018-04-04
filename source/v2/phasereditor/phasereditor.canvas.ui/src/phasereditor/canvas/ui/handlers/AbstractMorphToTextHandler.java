// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.canvas.ui.handlers;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.ITextSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public abstract class AbstractMorphToTextHandler<T extends BaseSpriteModel> extends AbstractMorphHandler<T> {

	private String _morphToPhaserName;

	public AbstractMorphToTextHandler(Class<T> morphToType, String morphToPhaserName) {
		super(morphToType);
		_morphToPhaserName = morphToPhaserName;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T createMorphModel(ISpriteNode srcNode, MorphToArgs args, GroupNode parent) {
		MorphToTextArgs textArgs = (MorphToTextArgs) args;
		
		ITextSpriteModel dstModel = createTextModel(srcNode, args, parent);

		((BaseObjectModel) dstModel).updateWith(srcNode.getModel());

		if (srcNode.getModel() instanceof ITextSpriteModel) {
			ITextSpriteModel srcModel = (ITextSpriteModel) srcNode.getModel();
			dstModel.setText(srcModel.getText());
			dstModel.setFontSize(srcModel.getFontSize());
		} else {
			dstModel.setFontSize(textArgs.size);
			dstModel.setText(textArgs.text);
		}

		ObjectCanvas canvas = srcNode.getControl().getCanvas();

		if (canvas.getEditor().getModel().getType() == CanvasType.SPRITE) {
			EditorSettings settings = canvas.getSettingsModel();
			settings.setBaseClass(_morphToPhaserName);
		}

		return (T) dstModel;
	}

	protected abstract ITextSpriteModel createTextModel(ISpriteNode srcNode, Object source, GroupNode parent);
}
