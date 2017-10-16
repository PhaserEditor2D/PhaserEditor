// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.core.codegen.js5;

import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.core.codegen.JSLikeBaseSpriteCodeGenerator;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

/**
 * @author arian
 *
 */
public class JSSpriteCodeGenerator extends JSLikeBaseSpriteCodeGenerator {

	public JSSpriteCodeGenerator(CanvasModel model) {
		super(model);
	}

	@Override
	public void generateHeader() {
		String classname = _settings.getClassName();
		String baseclass = _settings.getBaseClass();

		PhaserJSDoc help = InspectCore.getPhaserHelp();

		AssetSpriteModel<?> sprite = (AssetSpriteModel<?>) _model.getWorld().findFirstSprite();

		String key = "null";
		String frame = "null";
		if (sprite != null) {
			TextureArgs info = getTextureArgs(sprite.getAssetKey());
			key = info.key;
			frame = info.frame;
		}

		if (sprite instanceof TileSpriteModel) {
			MethodDoc mdoc = new MethodDoc();
			mdoc.comment(classname);
			mdoc.arg("aGame", "Phaser.Game", help.getMethodArgHelp("Phaser.TileSprite", "game"));
			mdoc.arg("aX", "Number", help.getMethodArgHelp("Phaser.TileSprite", "x"));
			mdoc.arg("aY", "Number", help.getMethodArgHelp("Phaser.TileSprite", "y"));
			mdoc.arg("aWidth", "Number", help.getMethodArgHelp("Phaser.TileSprite", "width"));
			mdoc.arg("aHeight", "Number", help.getMethodArgHelp("Phaser.TileSprite", "height"));
			mdoc.arg("aKey", "any", help.getMethodArgHelp("Phaser.TileSprite", "key"));
			mdoc.arg("aFrame", "any", help.getMethodArgHelp("Phaser.TileSprite", "frame"));
			mdoc.append();

			openIndent("function " + classname + "(aGame, aX, aY, aWidth, aHeight, aKey, aFrame) {");

			line();
			line("var pKey = aKey === undefined? " + key + " : aKey;");
			line("var pFrame = aFrame === undefined? " + frame + " : aFrame;");
			line();
			line(baseclass + ".call(this, aGame, aX, aY, aWidth, aHeight, pKey, pFrame);");

		} else if (sprite instanceof ButtonSpriteModel) {
			ButtonSpriteModel button = (ButtonSpriteModel) sprite;
			MethodDoc mdoc = new MethodDoc();
			mdoc.comment(classname);
			mdoc.arg("aGame", "Phaser.Game", help.getMethodArgHelp("Phaser.Button", "game"));
			mdoc.arg("aX", "Number", help.getMethodArgHelp("Phaser.Button", "x"));
			mdoc.arg("aY", "Number", help.getMethodArgHelp("Phaser.Button", "y"));
			mdoc.arg("aKey", "any", help.getMethodArgHelp("Phaser.Button", "key"));
			mdoc.arg("aCallback", "any", help.getMethodArgHelp("Phaser.Button", "callback"));
			mdoc.arg("aCallbackContext", "any", help.getMethodArgHelp("Phaser.Button", "callbackContext"));
			mdoc.arg("aOverFrame", "any", help.getMethodArgHelp("Phaser.Button", "overFrame"));
			mdoc.arg("aOutFrame", "any", help.getMethodArgHelp("Phaser.Button", "outFrame"));
			mdoc.arg("aDownFrame", "any", help.getMethodArgHelp("Phaser.Button", "downFrame"));
			mdoc.arg("aUpFrame", "any", help.getMethodArgHelp("Phaser.Button", "upFrame"));
			mdoc.append();

			openIndent("function " + classname
					+ "(aGame, aX, aY, aKey, aCallback, aCallbackContext, aOverFrame, aOutFrame, aDownFrame, aUpFrame) {");

			openIndent(baseclass + ".call(");
			line("this, aGame, aX, aY,");
			line("aKey || " + key + ",");
			line("aCallback || " + emptyStringToNull(button.getCallback()) + ",");
			line("aCallbackContext || " + emptyStringToNull(button.getCallbackContext()) + ",");
			line("aOverFrame || " + frameKey(button.getOverFrame()) + ",");
			line("aOutFrame || " + frameKey(button.getOutFrame()) + ",");
			line("aDownFrame || " + frameKey(button.getDownFrame()) + ",");
			append("aUpFrame || " + frameKey(button.getUpFrame()));
			closeIndent(");");
		} else {
			MethodDoc mdoc = new MethodDoc();
			mdoc.comment(classname);
			mdoc.arg("aGame", "Phaser.Game", help.getMethodArgHelp("Phaser.Sprite", "game"));
			mdoc.arg("aX", "Number", help.getMethodArgHelp("Phaser.Sprite", "x"));
			mdoc.arg("aY", "Number", help.getMethodArgHelp("Phaser.Sprite", "y"));
			mdoc.arg("aKey", "any", help.getMethodArgHelp("Phaser.Sprite", "key"));
			mdoc.arg("aFrame", "any", help.getMethodArgHelp("Phaser.Sprite", "frame"));
			mdoc.append();

			openIndent("function " + classname + "(aGame, aX, aY, aKey, aFrame) {");
			line();
			line("var pKey = aKey === undefined? " + key + " : aKey;");
			line("var pFrame = aFrame === undefined? " + frame + " : aFrame;");
			line();
			line(baseclass + ".call(this, aGame, aX, aY, pKey, pFrame);");
		}

		trim(() -> {
			line();
			userCode(_settings.getUserCode().getCreate_before());
		});

	}

	@Override
	public void generateFooter() {
		String classname = _settings.getClassName();
		String baseclass = _settings.getBaseClass();

		trim(() -> {
			line();
			userCode(_settings.getUserCode().getCreate_after());
		});

		closeIndent("}");
		line();

		line("/** @type " + baseclass + " */");
		line("var " + classname + "_proto = Object.create(" + baseclass + ".prototype);");
		line(classname + ".prototype = " + classname + "_proto;");
		line(classname + ".prototype.constructor = " + classname + ";");
		line();

		section(END_GENERATED_CODE, getYouCanInsertCodeHere());
	}
}
