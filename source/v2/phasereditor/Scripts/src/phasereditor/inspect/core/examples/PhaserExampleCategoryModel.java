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
package phasereditor.inspect.core.examples;

import static java.lang.System.out;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PhaserExampleCategoryModel {
	private String _name;
	private List<PhaserExampleModel> _examples;
	private List<PhaserExampleCategoryModel> _subCategories;
	private PhaserExampleCategoryModel _parentCategory;
	private Path _path;
	private String _fullname;

	public PhaserExampleCategoryModel(PhaserExampleCategoryModel parent, Path path, String name) {
		super();
		_name = name;
		_examples = new ArrayList<>();
		_subCategories = new ArrayList<>();
		_parentCategory = parent;
		_path = path;

		if (parent != null) {
			parent._subCategories.add(this);
		}
		_fullname = (parent == null ? "" : parent.getFullName() + " / ") + _name;
	}

	public String getFullName() {
		return _fullname;
	}

	public Path getPath() {
		return _path;
	}

	public PhaserExampleCategoryModel getParentCategory() {
		return _parentCategory;
	}

	public List<PhaserExampleCategoryModel> getSubCategories() {
		return _subCategories;
	}

	public String getName() {
		return _name;
	}

	public List<PhaserExampleModel> getTemplates() {
		return _examples;
	}

	public void addExample(PhaserExampleModel exampleModel) {
		_examples.add(exampleModel);
	}

	public void printTree(int depth) {
		for (int i = 0; i < depth; i++) {
			out.print("\t");
		}
		out.println(getName());

		for (PhaserExampleCategoryModel c : _subCategories) {
			c.printTree(depth + 1);
		}

		for (PhaserExampleModel e : _examples) {
			for (int i = 0; i < depth + 1; i++) {
				out.print("\t");
			}
			out.println(e.getName());
		}
	}

}
