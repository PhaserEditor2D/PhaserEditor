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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PhaserExampleModel  {
	public static class Mapping {
		private Path _original;
		private String destiny;

		public Mapping(Path original, String destiny) {
			super();
			_original = original;
			this.destiny = destiny;
		}

		public Path getOriginal() {
			return _original;
		}

		public String getDestiny() {
			return destiny;
		}
	}

	private String _name;
	private List<Mapping> _filesMapping;
	private PhaserExampleCategoryModel _category;
	private Path _mainFilePath;
	private String _fullname;

	public PhaserExampleModel(PhaserExampleCategoryModel category, Path mainFilePath) {
		_name = PhaserExamplesRepoModel.getName(mainFilePath.getFileName());
		_category = category;
		_filesMapping = new ArrayList<>();
		_mainFilePath = mainFilePath;
		_fullname = category.getFullName() + " / " + _name;
	}
	
	public String getFullName() {
		return _fullname;
	}

	public String getName() {
		return _name;
	}

	public List<Mapping> getFilesMapping() {
		return _filesMapping;
	}

	public void addMapping(Path orig, String dest) {
		_filesMapping.add(new Mapping(orig, dest));
	}

	

	public Path getMainFilePath() {
		return _mainFilePath;
	}

	public String toStringTree() {
		StringBuilder sb = new StringBuilder();
		sb.append(_name + "\n");
		for (Mapping m : _filesMapping) {
			sb.append(m.getOriginal() + " --> " + m.getDestiny() + "\n");
		}
		return sb.toString();
	}

	public PhaserExampleCategoryModel getCategory() {
		return _category;
	}
	
	public Path getFilePath() {
		return getMainFilePath();
	}
}
