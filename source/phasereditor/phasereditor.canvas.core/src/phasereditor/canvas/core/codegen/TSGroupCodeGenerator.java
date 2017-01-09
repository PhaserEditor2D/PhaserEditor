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

import phasereditor.canvas.core.BaseObjectModel;

/**
 * @author arian
 *
 */
public class TSGroupCodeGenerator extends JSLikeCodeGenerator {
	@Override
	protected void generateHeader(StringBuilder sb, String preInitUserCode, String classname) {
		String tabs1 = tabs(1);
		String tabs2 = tabs(2);
		sb.append("/**\n");
		sb.append(" * " + classname + ".\n");
		sb.append(" * @param aGame The game.\n");
		sb.append(" * @param aParent The parent group. If not given the game world will be used instead.\n");
		sb.append(" */\n");
		sb.append("class " + classname + " extends Phaser.Group {\n");
		sb.append(tabs1 + "constructor(aGame : Phaser.Game, aParent : Phaser.Group) {\n");
		sb.append(tabs2 + "super(aGame, aParent);\n\n");

		sb.append(PRE_INIT_CODE_BEGIN);
		sb.append(preInitUserCode);
		sb.append(PRE_INIT_CODE_END + "\n");
		sb.append("\n");
	}

	@Override
	protected void generateObjectCreate(int indent, StringBuilder sb, BaseObjectModel model) {
		super.generateObjectCreate(indent + 1, sb, model);
	}

	@Override
	protected void generateFooter(StringBuilder sb, String postInit, String postGen, String classname) {
		sb.append(POST_INIT_CODE_BEGIN);
		sb.append(postInit);
		sb.append(POST_INIT_CODE_END + "\n\n");

		sb.append("}\n\n");

		sb.append(END_GENERATED_CODE );
		sb.append(postGen);
	}
}
