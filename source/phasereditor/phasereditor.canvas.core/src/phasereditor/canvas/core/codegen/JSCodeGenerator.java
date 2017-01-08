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
package phasereditor.canvas.core.codegen;

/**
 * @author arian
 *
 */
public class JSCodeGenerator extends JSLikeCodeGenerator {

	@Override
	protected void generateHeader(StringBuilder sb, String preInitUserCode, String classname) {
		String tabs1 = tabs(1);
		sb.append("/**\n");
		sb.append(" * " + classname + ".\n");
		sb.append(" * @param {Phaser.Game} aGame The game.\n");
		sb.append(
				" * @param {Phaser.Group} aParent The parent group. If not given the game world will be used instead.\n");
		sb.append(" */\n");
		sb.append("function " + classname + "(aGame, aParent) {\n");
		sb.append(tabs1 + "Phaser.Group.call(this, aGame, aParent);\n\n");

		sb.append(PRE_INIT_CODE_BEGIN);
		sb.append(preInitUserCode);
		sb.append(PRE_INIT_CODE_END + "\n");
		sb.append("\n");
	}

	@Override
	protected void generateFooter(StringBuilder sb, String postInitUserCode, String postGenUserCode, String classname) {
		String tabs1 = tabs(1);

		sb.append(POST_INIT_CODE_BEGIN);
		sb.append(postInitUserCode);
		sb.append(POST_INIT_CODE_END);

		sb.append("\n\n}\n\n");

		sb.append("/** @type Phaser.Group */\n");
		sb.append("var " + classname + "_proto = Object.create(Phaser.Group.prototype);\n");
		sb.append(classname + ".prototype = " + classname + "_proto;\n");
		sb.append(classname + ".prototype.constructor = Phaser.Group;\n");
		sb.append("\n");

		sb.append(END_GENERATED_CODE);
		sb.append(postGenUserCode);
	}

}
