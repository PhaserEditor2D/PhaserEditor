// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.core.codegen;

import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

/**
 * @author arian
 *
 */
public class TSSpriteCodeGenerator extends JSLikeBaseSpriteCodeGenerator {

	/**
	 * @param model
	 */
	public TSSpriteCodeGenerator(CanvasModel model) {
		super(model);
	}

	@Override
	protected void generateHeader() {
		String classname = _world.getClassName();
		String baseclass = _settings.getBaseClass();
		PhaserJSDoc help = InspectCore.getPhaserHelp();

		line("/**");
		line(" * " + classname + ".");
		line(" * @param {Phaser.Game} aGame " + help.getMethodArgHelp("Phaser.Sprite", "game"));
		line(" * @param {number} aX " + help.getMethodArgHelp("Phaser.Sprite", "x"));
		line(" * @param {number} aY " + help.getMethodArgHelp("Phaser.Sprite", "y"));
		line(" */");
		openIndent("class " + classname + " extends " + baseclass + " {");
		openIndent("constructor(aGame : Phaser.Game, aX : number, aY : number) {");
		AssetSpriteModel<?> sprite = findSprite();
		String key = "null";
		String frame = "null";
		if (sprite != null) {
			TextureArgs info = ICodeGenerator.getTextureArgs(sprite.getAssetKey());
			key = info.key;
			frame = info.frame;
		}
		line("super(aGame, aX, aY, " + key + ", " + frame + ");");

		section(PRE_INIT_CODE_BEGIN, PRE_INIT_CODE_END, getYouCanInsertCodeHere());

		line("");
		line("");
	}

	@Override
	protected void generateFooter() {
		section(POST_INIT_CODE_BEGIN, POST_INIT_CODE_END, getYouCanInsertCodeHere());
		closeIndent();
		line("}");
		closeIndent();
		section(END_GENERATED_CODE, getYouCanInsertCodeHere());
	}

}
