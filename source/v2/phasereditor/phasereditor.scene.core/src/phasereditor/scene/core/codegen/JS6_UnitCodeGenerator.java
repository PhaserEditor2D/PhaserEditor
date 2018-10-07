// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.scene.core.codegen;

import phasereditor.project.core.codegen.BaseCodeGenerator;
import phasereditor.scene.core.codedom.ClassDeclDom;
import phasereditor.scene.core.codedom.MemberDeclDom;
import phasereditor.scene.core.codedom.MethodDeclDom;
import phasereditor.scene.core.codedom.RawCode;
import phasereditor.scene.core.codedom.UnitDom;

/**
 * @author arian
 *
 */
public class JS6_UnitCodeGenerator extends BaseCodeGenerator {

	private UnitDom _unit;

	public JS6_UnitCodeGenerator(UnitDom unit) {
		_unit = unit;
	}

	@Override
	protected void internalGenerate() {

		for (var elem : _unit.getElements()) {

			generateUnitElement(elem);

		}

	}

	private void generateUnitElement(Object elem) {

		if (elem instanceof ClassDeclDom) {

			generateClass((ClassDeclDom) elem);

		}
	}

	private void generateClass(ClassDeclDom clsDecl) {

		append("class " + clsDecl.getName() + " ");

		if (clsDecl.getSuperClass() != null) {
			append("extends " + clsDecl.getSuperClass() + " ");
		}

		openIndent("{");

		line("");

		for (var memberDecl : clsDecl.getMembers()) {
			generateMemberDecl(memberDecl);
		}
		
		closeIndent("}");
	}

	private void generateMemberDecl(MemberDeclDom memberDecl) {

		if (memberDecl instanceof MethodDeclDom) {
			generateMethodDecl((MethodDeclDom) memberDecl);
		}

	}

	private void generateMethodDecl(MethodDeclDom methodDecl) {

		append(methodDecl.getName() + "() ");

		line("{");
		openIndent();
		

		for (var instr : methodDecl.getInstructions()) {
			generateInstr(instr);
		}

		closeIndent("}");
	}

	private void generateInstr(Object instr) {

		if (instr instanceof RawCode) {

			generateRawCode(((RawCode) instr));

		}
	}

	private void generateRawCode(RawCode raw) {

		var code = raw.getCode();

		var lines = code.split("\\R");

		for (var line : lines) {
			line(line);
		}		
	}
}
