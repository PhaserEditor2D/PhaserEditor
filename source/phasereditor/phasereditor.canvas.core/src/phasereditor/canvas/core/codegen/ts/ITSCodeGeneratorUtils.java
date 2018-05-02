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

import java.util.List;

import phasereditor.canvas.core.AnimationModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.core.TilemapSpriteModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.core.codegen.JSLikeCanvasCodeGenerator;

/**
 * @author arian
 *
 */
public interface ITSCodeGeneratorUtils {

	public default void generatePublicFieldDeclarations(JSLikeCanvasCodeGenerator generator, WorldModel worldModel) {
		worldModel.walk_skipGroupIfFalse(model -> {

			this.generatePublicFieldDeclaration(generator, model);

			return !model.isPrefabInstance();
		});
	}

	default public void generatePublicFieldDeclaration(JSLikeCanvasCodeGenerator generator, BaseObjectModel obj) {
		if (!(obj instanceof WorldModel) && obj.isEditorGenerate()) {
			if (obj.isEditorField()) {
				String name = generator.getVarName(obj);
				String camel = JSLikeCanvasCodeGenerator.getPublicFieldName(name);
				String visibility = obj.isEditorPublic() ? "public" : "private";
				generator.line(visibility + " " + camel + " : " + getObjectType(obj) + ";");

				if (obj instanceof TilemapSpriteModel) {
					generator.line("public " + camel + "_layer : Phaser.TilemapLayer;");
				}
			}

			if (obj instanceof BaseSpriteModel && obj.isOverriding(BaseSpriteModel.PROPSET_ANIMATIONS)) {
				List<AnimationModel> anims = ((BaseSpriteModel) obj).getAnimations();
				for (AnimationModel anim : anims) {
					if (anim.isPublic()) {
						String name = JSLikeCanvasCodeGenerator
								.getPublicFieldName(generator.getAnimationVarName(obj, anim));
						generator.line("public " + name + " : Phaser.Animation;");
					}
				}
			}
		}
	}

	public default String getObjectType(BaseObjectModel obj) {
		if (obj.isPrefabInstance()) {
			return obj.getPrefab().getClassName();
		}

		if (obj instanceof ButtonSpriteModel) {
			return "Phaser.Button";
		}

		if (obj instanceof TileSpriteModel) {
			return "Phaser.TileSprite";
		}

		if (obj instanceof TextModel) {
			return "Phaser.Text";
		}

		if (obj instanceof BitmapTextModel) {
			return "Phaser.BitmapText";
		}

		if (obj instanceof TilemapSpriteModel) {
			return "Phaser.Tilemap";
		}

		if (obj instanceof GroupModel) {
			return "Phaser.Group";
		}

		return "Phaser.Sprite";
	}

}
