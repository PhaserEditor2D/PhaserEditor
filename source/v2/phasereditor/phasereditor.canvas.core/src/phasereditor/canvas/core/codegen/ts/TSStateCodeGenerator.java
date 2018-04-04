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

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.codegen.BaseStateGenerator;

/**
 * @author arian
 *
 */
public class TSStateCodeGenerator extends BaseStateGenerator implements ITSCodeGeneratorUtils {

	public TSStateCodeGenerator(CanvasModel model) {
		super(model);
	}

	@Override
	public void generateHeader() {
		String classname = _settings.getClassName();
		String baseclass = _settings.getBaseClass();

		line("/**");
		line(" * " + classname + ".");
		line(" */");
		openIndent("class " + classname + " extends " + baseclass + " {");
		line();
		openIndent("constructor() {");
		line();
		line("super();");

		trim(() -> {
			line();
			userCode(_settings.getUserCode().getState_constructor_before());
			userCode(_settings.getUserCode().getState_constructor_after());
		});

		closeIndent("}");

		line();

		generateInitMethod();

		line();

		generatePreloadMethod();

		line();

		openIndent("create() {");

		trim(() -> {
			line();
			userCode(_settings.getUserCode().getCreate_before());
			line();
		});
	}

	private void generatePreloadMethod() {
		openIndent("preload () {");

		generatePreloadMethodBody();

		closeIndent("}");
	}

	private void generateInitMethod() {
		// INIT
		openIndent("init(" + _settings.getUserCode().getState_init_args() + ") {");
		generateInitMethodBody();
		closeIndent("}");
	}

	@Override
	public void generateFooter() {
		userCode(_settings.getUserCode().getCreate_after());

		closeIndent("}");

		line();

		generatePublicFieldDeclarations(this, _model.getWorld());

		line();

		section("/* state-methods-begin */", "/* state-methods-end */", getYouCanInsertCodeHere());
		line();
		closeIndent("}");

		section(END_GENERATED_CODE, getYouCanInsertCodeHere());
	}
}
