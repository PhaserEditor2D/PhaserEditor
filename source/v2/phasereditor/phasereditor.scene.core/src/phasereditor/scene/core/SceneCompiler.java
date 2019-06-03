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
package phasereditor.scene.core;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import phasereditor.project.core.codegen.BaseCodeGenerator;
import phasereditor.scene.core.codedom.UnitDom;
import phasereditor.scene.core.codegen.JS6_UnitCodeGenerator;
import phasereditor.scene.core.codegen.SceneCodeDomBuilder;
import phasereditor.scene.core.codegen.TS_UnitCodeGenerator;

/**
 * @author arian
 *
 */
public class SceneCompiler {
	private IFile _sceneFile;
	private SceneModel _sceneModel;

	public SceneCompiler(IFile sceneFile, SceneModel sceneModel) {
		super();
		_sceneFile = sceneFile;
		_sceneModel = sceneModel;
	}

	public void compileToFile(IProgressMonitor monitor) throws Exception {

		var result = compile();

		var code = result.code;
		var codeFile = result.codeFile;
		var charset = result.charset;

		ByteArrayInputStream stream = new ByteArrayInputStream(code.getBytes(charset));
		if (codeFile.exists()) {
			codeFile.setContents(stream, IResource.NONE, monitor);
		} else {
			codeFile.create(stream, false, monitor);
			codeFile.setCharset(charset.name(), monitor);
		}
		codeFile.refreshLocal(1, null);

	}

	public static class CompileResult {
		public String code;
		public IFile codeFile;
		public Charset charset;

		public CompileResult(String code, IFile codeFile, Charset charset) {
			super();
			this.code = code;
			this.codeFile = codeFile;
			this.charset = charset;
		}

	}

	public CompileResult compile() throws Exception {
		var codeFile = SceneCore.getSceneSourceCodeFile(_sceneModel, _sceneFile);

		Charset charset;

		if (codeFile.exists()) {
			charset = Charset.forName(codeFile.getCharset());
		} else {
			charset = Charset.forName("UTF-8");
		}

		String replace = null;

		if (codeFile.exists()) {
			byte[] bytes = Files.readAllBytes(codeFile.getLocation().makeAbsolute().toFile().toPath());
			replace = new String(bytes, charset);
		}

		var builder = new SceneCodeDomBuilder(codeFile);
		var unitDom = builder.build(_sceneModel);

		var codeGenerator = getCodeGenerator(unitDom);

		var code = codeGenerator.generate(replace);

		return new CompileResult(code, codeFile, charset);
	}

	private BaseCodeGenerator getCodeGenerator(UnitDom unitDom) {
		var lang = _sceneModel.getCompilerLang();

		switch (lang) {
		case JAVA_SCRIPT_6:
			return new JS6_UnitCodeGenerator(unitDom);
		case TYPE_SCRIPT:
			return new TS_UnitCodeGenerator(unitDom);
		default:
			break;
		}
		
		throw new RuntimeException("Invalid Output Language.");
	}
}
