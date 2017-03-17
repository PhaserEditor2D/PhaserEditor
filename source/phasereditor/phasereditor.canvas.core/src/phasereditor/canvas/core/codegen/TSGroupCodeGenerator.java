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

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

/**
 * @author arian
 *
 */
public class TSGroupCodeGenerator extends JSLikeGroupCodeGenerator implements ITSCodeGeneratorUtils {

	/**
	 * @param model
	 */
	public TSGroupCodeGenerator(CanvasModel model) {
		super(model);
	}

	@Override
	protected void generateHeader() {
		String classname = _settings.getClassName();
		String baseclass = _settings.getBaseClass();

		PhaserJSDoc help = InspectCore.getPhaserHelp();

		line("/**");
		line(" * " + classname + ".");
		line(" * @param aGame " + help.getMethodArgHelp("Phaser.Group", "game"));
		line(" * @param aParent " + help.getMethodArgHelp("Phaser.Group", "parent"));
		line(" * @param aName " + help.getMethodArgHelp("Phaser.Group", "name"));
		line(" * @param aAddToStage " + help.getMethodArgHelp("Phaser.Group", "addToStage"));
		line(" * @param aEnableBody " + help.getMethodArgHelp("Phaser.Group", "enableBody"));
		line(" * @param aPhysicsBodyType " + help.getMethodArgHelp("Phaser.Group", "physicsBodyType"));
		line(" */");
		openIndent("class " + classname + " extends " + baseclass + " {");
		openIndent(
				"constructor(aGame : Phaser.Game, aParent : Phaser.Group, aName : string, aAddToStage : boolean, aEnableBody : boolean, aPhysicsBodyType : number) {");
		line("super(aGame, aParent, aName, aAddToStage, aEnableBody, aPhysicsBodyType);");
		line();
		userCode(_settings.getUserCode().getCreate_before());
		line();
		section(PRE_INIT_CODE_BEGIN, PRE_INIT_CODE_END, getYouCanInsertCodeHere());
	}

	@Override
	protected void generateFooter() {
		
		section(POST_INIT_CODE_BEGIN, POST_INIT_CODE_END, getYouCanInsertCodeHere());
		userCode(_settings.getUserCode().getCreate_after());
		
		closeIndent("}");

		line();

		generatePublicFieldDeclarations(this, _model.getWorld());

		line();
		
		section("/* group-methods-begin */", "/* group-methods-end */", getYouCanInsertCodeHere());

		closeIndent("}");
		section(END_GENERATED_CODE, getYouCanInsertCodeHere());
	}

	@Override
	protected GroupModel getRootObjectsContainer() {
		return _world.findGroupPrefabRoot();
	}
}
