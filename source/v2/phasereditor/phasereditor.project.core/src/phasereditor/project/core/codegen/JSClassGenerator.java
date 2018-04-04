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
package phasereditor.project.core.codegen;

/**
 * @author arian
 *
 */
public class JSClassGenerator extends BaseCodeGenerator {

	private String _clsname;
	private String _baseClass;
	private boolean _hasBaseClass;

	public JSClassGenerator(String clsname, String baseClass) {
		this._clsname = clsname;
		this._baseClass = baseClass;
		_hasBaseClass = baseClass != null && baseClass.trim().length() > 0;
	}

	@Override
	protected void internalGenerate() {
		line("/**");
		line(" *");
		line(" */");
		openIndent("function " + _clsname + " () {");
		if (_hasBaseClass) {
			line(_baseClass + ".call(this);");
		}
		closeIndent("}");
		line();
		if (_hasBaseClass) {
			line("/** @type " + _baseClass + " */");
			line("var " + _clsname + "_proto = Object.create(" + _baseClass + ".prototype);");
			line(_clsname + ".prototype = " + _clsname + "_proto;");
			line(_clsname + ".prototype.constructor = " + _clsname + ";");
			line();
		}
	}
}
