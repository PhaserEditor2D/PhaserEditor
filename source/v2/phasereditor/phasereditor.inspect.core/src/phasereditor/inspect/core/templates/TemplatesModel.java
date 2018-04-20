// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.inspect.core.templates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import phasereditor.inspect.core.InspectCore;

public class TemplatesModel {
	private List<TemplateCategoryModel> _categories;
	private Path _templatesFolder;
	private Path _phaserJs;
	private Path[] _tsFiles;

	public TemplatesModel(Path templatesFolder) throws IOException {
		_categories = new ArrayList<>();
		_templatesFolder = templatesFolder;
		_phaserJs = InspectCore.getBundleFile(InspectCore.RESOURCES_PHASER_CODE_PLUGIN,
				"phaser-master/dist/phaser.js");

		_tsFiles = new Path[] {

				InspectCore.getBundleFile(InspectCore.RESOURCES_PHASER_CODE_PLUGIN,
						"phaser-master/typescript/phaser.d.ts")

		};

		load();
	}

	public List<TemplateCategoryModel> getCategories() {
		return _categories;
	}

	public Path getTemplatesFolder() {
		return _templatesFolder;
	}

	public Path getPhaserJs() {
		return _phaserJs;
	}

	public Path[] getTypeScriptFiles() {
		return _tsFiles;
	}

	private void load() throws IOException {
		_categories = new ArrayList<>();

		Files.walk(_templatesFolder, 1).filter(file -> file != _templatesFolder && Files.isDirectory(file))
				.forEach(file -> {
					TemplateCategoryModel category = new TemplateCategoryModel(this, file.getFileName().toString());
					_categories.add(category);
					Path infoFile = file.resolve("info.txt");
					if (Files.exists(infoFile)) {
						try {
							String info = new String(Files.readAllBytes(infoFile));
							category.setDescription(info);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					try {
						Files.walk(file, 1).forEach(file2 -> {
							String filename = file2.getFileName().toString();
							if (!file2.equals(file) && !filename.equals("info.txt")) {
								category.addTemplate(file2);
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				});
	}

	public TemplateModel findById(String id) {
		for (TemplateCategoryModel c : _categories) {
			for (TemplateModel t : c.getTemplates()) {
				if (id.equals(t.getInfo().getId())) {
					return t;
				}
			}
		}
		return null;
	}
}
