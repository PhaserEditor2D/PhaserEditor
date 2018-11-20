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

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public interface VariableComponent {

	// editorName

	static String gameObjectEditorName_name = "gameObjectEditorName";

	static String gameObjectEditorName_default = "unnamed";

	static String get_gameObjectEditorName(ObjectModel obj) {
		return (String) obj.get("gameObjectEditorName");
	}

	static void set_gameObjectEditorName(ObjectModel obj, String editorName) {
		obj.put("gameObjectEditorName", editorName);
	}

	// editorField

	static String gameObjectEditorField_name = "gameObjectEditorField";

	static boolean gameObjectEditorField_default = false;

	static boolean get_gameObjectEditorField(ObjectModel obj) {
		return (boolean) obj.get("gameObjectEditorField");
	}

	static void set_editorField(ObjectModel obj, boolean editorField) {
		obj.put("gameObjectEditorField", editorField);
	}

	// utils

	static boolean is(Object obj) {
		return obj instanceof VariableComponent;
	}

	static void init(ObjectModel obj) {
		set_gameObjectEditorName(obj, gameObjectEditorName_default);
		set_editorField(obj, gameObjectEditorField_default);
	}
}
