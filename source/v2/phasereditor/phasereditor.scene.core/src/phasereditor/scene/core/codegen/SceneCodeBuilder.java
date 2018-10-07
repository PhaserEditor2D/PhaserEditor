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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.WorldModel;
import phasereditor.scene.core.codedom.ClassDeclDom;
import phasereditor.scene.core.codedom.MethodDeclDom;
import phasereditor.scene.core.codedom.RawCode;
import phasereditor.scene.core.codedom.UnitDom;

/**
 * @author arian
 *
 */
public class SceneCodeBuilder {

	private IFile _file;

	public SceneCodeBuilder(IFile file) {
		_file = file;
	}

	public UnitDom build(WorldModel model) {

		var unit = new UnitDom();

		var clsName = _file.getFullPath().removeFileExtension().lastSegment();

		var clsDom = new ClassDeclDom(clsName);
		clsDom.setSuperClass("Phaser.Scene");

		var preloadDom = buildPreloadMethod(model);

		clsDom.getMembers().add(preloadDom);

		unit.getElements().add(clsDom);

		return unit;
	}

	@SuppressWarnings("static-method")
	private MethodDeclDom buildPreloadMethod(WorldModel model) {

		var preloadDom = new MethodDeclDom("preload");

		Map<String, String[]> packSectionList = new HashMap<>();

		model.visit(objModel -> {
			if (objModel instanceof TextureComponent) {
				var frame = TextureComponent.get_frame(objModel);
				if (frame != null) {

					var pack =  ProjectCore.getAssetUrl(frame.getAsset().getPack().getFile());
					var section = frame.getAsset().getSection().getKey();

					packSectionList.put(pack + "-" + section, new String[] { pack, section });
				}
			}
		});


		for (var pair : packSectionList.values()) {
			var line = new RawCode("this.load.pack('" + pair[0] + "', '" + pair[1] + "');");
			preloadDom.getInstructions().add(line);
		}

		return preloadDom;
	}

}
