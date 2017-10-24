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
package phasereditor.canvas.core.codegen.ts;

import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.core.codegen.JSLikeBaseSpriteCodeGenerator;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

/**
 * @author arian
 *
 */
public class TSSpriteCodeGenerator extends JSLikeBaseSpriteCodeGenerator implements ITSCodeGeneratorUtils {

	/**
	 * @param model
	 */
	public TSSpriteCodeGenerator(CanvasModel model) {
		super(model);
	}

	@Override
	public void generateHeader() {
		String classname = _settings.getClassName();
		String baseclass = _settings.getBaseClass();
		PhaserJSDoc help = InspectCore.getPhaserHelp();

		openIndent("class " + classname + " extends " + baseclass + " {");

		BaseSpriteModel sprite = (BaseSpriteModel) _model.getWorld().findFirstSprite();

		String key = "null";
		String frame = "null";

		if (sprite != null && sprite instanceof AssetSpriteModel) {
			TextureArgs info = getTextureArgs(((AssetSpriteModel<?>) sprite).getAssetKey());
			key = info.key;
			frame = info.frame;
		}

		if (sprite instanceof TileSpriteModel) {
			TileSpriteModel tile = (TileSpriteModel) sprite;

			MethodDoc mdoc = new MethodDoc();
			mdoc.comment(classname);
			mdoc.arg("aGame", help.getMethodArgHelp("Phaser.TileSprite", "game"));
			mdoc.arg("aX", help.getMethodArgHelp("Phaser.TileSprite", "x"));
			mdoc.arg("aY", help.getMethodArgHelp("Phaser.TileSprite", "y"));
			mdoc.arg("aWidth", help.getMethodArgHelp("Phaser.TileSprite", "width"));
			mdoc.arg("aHeight", help.getMethodArgHelp("Phaser.TileSprite", "height"));
			mdoc.arg("aKey", help.getMethodArgHelp("Phaser.TileSprite", "key"));
			mdoc.arg("aFrame", help.getMethodArgHelp("Phaser.TileSprite", "frame"));
			mdoc.append();

			openIndent(
					"constructor(aGame : Phaser.Game, aX : number, aY : number, aWidth : number, aHeight : number, aKey : any, aFrame : any) {");
			
			
			trim(() -> {
				line();
				userCode(_settings.getUserCode().getCreate_before());
				line();
			});
			
			openIndent("super(aGame, aX, aY,");
			line("aWidth == undefined || aWidth == null? " + tile.getWidth() + " : aWidth,");
			line("aHeight == undefined || aHeight == null? " + tile.getHeight() + " : aHeight,");
			line("aKey || " + key + ",");
			append("aFrame || " + frame);
			closeIndent(");");

		} else if (sprite instanceof ButtonSpriteModel) {
			ButtonSpriteModel button = (ButtonSpriteModel) sprite;
			MethodDoc mdoc = new MethodDoc();
			mdoc.comment(classname);
			mdoc.arg("aGame", help.getMethodArgHelp("Phaser.Button", "game"));
			mdoc.arg("aX", help.getMethodArgHelp("Phaser.Button", "x"));
			mdoc.arg("aY", help.getMethodArgHelp("Phaser.Button", "y"));
			mdoc.arg("aKey", help.getMethodArgHelp("Phaser.Button", "key"));
			mdoc.arg("aCallback", help.getMethodArgHelp("Phaser.Button", "callback"));
			mdoc.arg("aCallbackContext", help.getMethodArgHelp("Phaser.Button", "callbackContext"));
			mdoc.arg("aOverFrame", help.getMethodArgHelp("Phaser.Button", "overFrame"));
			mdoc.arg("aOutFrame", help.getMethodArgHelp("Phaser.Button", "outFrame"));
			mdoc.arg("aDownFrame", help.getMethodArgHelp("Phaser.Button", "downFrame"));
			mdoc.arg("aUpFrame", help.getMethodArgHelp("Phaser.Button", "upFrame"));
			mdoc.append();

			openIndent(
					"constructor(aGame : Phaser.Game, aX : number, aY : number, aKey : any, aCallback : any, aCallbackContext : any, aOverFrame : any, aOutFrame : any, aDownFrame : any, aUpFrame : any) {");

			trim(() -> {
				line();
				userCode(_settings.getUserCode().getCreate_before());
				line();
			});
			
			openIndent("super(");
			line("aGame, aX, aY,");
			line("aKey || " + key + ",");
			line("aCallback || " + emptyStringToNull(button.getCallback()) + ",");
			line("aCallbackContext /* || " + emptyStringToNull(button.getCallbackContext()) + " */,");
			line("aOverFrame == undefined || aOverFrame == null? " + frameKey(button.getOverFrame()) + " : aOverFrame,");
			line("aOutFrame == undefined || aOutFrame == null? " + frameKey(button.getOutFrame()) + " : aOutFrame,");
			line("aDownFrame == undefined || aDownFrame == null? " + frameKey(button.getDownFrame()) + " : aDownFrame,");
			append("aUpFrame == undefined || aUpFrame == null? " + frameKey(button.getUpFrame()) + " : aUpFrame");
			closeIndent(");");
		} else if (sprite instanceof TextModel) {
			TextModel text = (TextModel) sprite;
			MethodDoc mdoc = new MethodDoc();
			mdoc.comment(classname);
			mdoc.arg("aGame", help.getMethodArgHelp("Phaser.Text", "game"));
			mdoc.arg("aX", help.getMethodArgHelp("Phaser.Text", "x"));
			mdoc.arg("aY", help.getMethodArgHelp("Phaser.Text", "y"));
			mdoc.arg("aText", help.getMethodArgHelp("Phaser.Text", "text"));
			mdoc.arg("aStyle", help.getMethodArgHelp("Phaser.Text", "style"));

			mdoc.append();

			openIndent("constructor(aGame : Phaser.Game, aX : number, aY : number, aText : string, aStyle : any) {");

			trim(() -> {
				line();
				userCode(_settings.getUserCode().getCreate_before());
				line();
			});			
			
			openIndent("super(aGame, aX, aY,");
			line("aText || '" + escapeLines(text.getText()) + "',");
			line("aStyle || ");

			String[] lines = text.getPhaserStyleObject().toString(4).split("\n");
			openIndent();
			for (int i = 0; i < lines.length; i++) {
				if (i == lines.length - 1) {
					append(lines[i]);
				} else {
					line(lines[i]);
				}
			}
			closeIndent(");");
			closeIndent();
		} else {
			MethodDoc mdoc = new MethodDoc();
			mdoc.comment(classname);
			mdoc.arg("aGame", help.getMethodArgHelp("Phaser.Sprite", "game"));
			mdoc.arg("aX", help.getMethodArgHelp("Phaser.Sprite", "x"));
			mdoc.arg("aY", help.getMethodArgHelp("Phaser.Sprite", "y"));
			mdoc.arg("aKey", help.getMethodArgHelp("Phaser.Sprite", "key"));
			mdoc.arg("aFrame", help.getMethodArgHelp("Phaser.Sprite", "frame"));
			mdoc.append();

			openIndent("constructor(aGame : Phaser.Game, aX : number, aY : number, aKey : any, aFrame : any) {");
			
			trim(() -> {
				line();
				userCode(_settings.getUserCode().getCreate_before());
				line();
			});
			
			line("super(aGame, aX, aY, aKey || " + key + ", aFrame == undefined || aFrame == null? " + frame + " : aFrame);");
		}
	}

	@Override
	public void generateFooter() {
		trim(() -> {
			line();
			userCode(_settings.getUserCode().getCreate_after());
		});

		closeIndent("}");

		trim(() -> {
			line();
			generatePublicFieldDeclarations(this, _model.getWorld());
		});

		line();

		section("/* sprite-methods-begin */", "/* sprite-methods-end */", getYouCanInsertCodeHere());

		closeIndent("}");
		section(END_GENERATED_CODE, getYouCanInsertCodeHere());
	}

}
