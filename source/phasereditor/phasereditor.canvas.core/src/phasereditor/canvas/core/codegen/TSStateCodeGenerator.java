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

/**
 * @author arian
 *
 */
public class TSStateCodeGenerator extends JSLikeCodeGenerator {

	public TSStateCodeGenerator(CanvasModel model) {
		super(model);
	}

	@Override
	protected void generateHeader() {
		String classname = _world.getClassName();
		line("/**");
		line(" * " + classname + ".");
		line(" */");
		openIndent("class " + classname + " extends Phaser.State {");
		openIndent("constructor() {");
		section("/* constructor-begin */", "/* constructor-end */", getYouCanInsertCodeHere());
		closeIndent("}");
		
		line();
		openIndent("create() {");

		section(PRE_INIT_CODE_BEGIN, PRE_INIT_CODE_END, getYouCanInsertCodeHere());
		line();
		line();
	}

	@Override
	protected void generateFooter() {
		section(POST_INIT_CODE_BEGIN, POST_INIT_CODE_END, getYouCanInsertCodeHere());

		closeIndent("}");
		
		line();
		
		section("/* state-methods-begin */", "/* state-methods-end */", getYouCanInsertCodeHere());
		
		closeIndent("}");

		section(END_GENERATED_CODE, getYouCanInsertCodeHere());
	}
}
