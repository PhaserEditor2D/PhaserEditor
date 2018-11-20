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

import java.util.HashSet;

/**
 * @author arian
 *
 */
public class NameComputer {
	private ObjectModel _world;
	private HashSet<String> _names;

	public NameComputer(ObjectModel world) {
		super();
		_world = world;

		_names = new HashSet<>();
		_world.visit(model -> {
			_names.add(VariableComponent.get_variableName(model));
		});
	}

	public String newName(String baseName) {
		if (!_names.contains(baseName)) {
			return baseName;
		}

		for (int i = 1; true; i++) {
			var name = baseName + "_" + i;
			if (!_names.contains(name)) {
				return name;
			}
		}
	}
}
