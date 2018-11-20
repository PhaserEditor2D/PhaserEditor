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
public interface GameObjectEditorComponent {

	// editorDirty

	static String gameObjecEditortDirty_name = "gameObjectEditorDirty";

	static boolean gameObjectEditorDirty_default = true;

	static boolean get_gameObjectEditorDirty(ObjectModel obj) {
		return (boolean) obj.get("gameObjectEditorDirty");
	}

	static void set_gameObjectEditorDirty(ObjectModel obj, boolean editorDirty) {
		obj.put("gameObjectEditorDirty", editorDirty);
	}

	// editorShow

	static String gameObjectEditorShow_name = "gameObjectEditorShow";

	static boolean gemeObjectEditorShow_default = true;

	static boolean get_gameObjectEditorShow(ObjectModel obj) {
		return (boolean) obj.get("gameObjectEditorShow");
	}

	static void set_gameObjectEditorShow(ObjectModel obj, boolean editorShow) {
		obj.put("gameObjectEditorShow", editorShow);
	}

	// editorClosed

	static String gameObjectEditorClosed_name = "gameObjectEditorClosed";

	static boolean editorClosed_default = false;

	static boolean get_gameObjectEditorClosed(ObjectModel obj) {
		return (boolean) obj.get("gameObjectEditorClosed");
	}

	static void set_gameObjectEditorClosed(ObjectModel obj, boolean editorClosed) {
		obj.put("gameObjectEditorClosed", editorClosed);
	}

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

	// editorTransparency

	static String gameObjectEditorTransparency_name = "gameObjectEditorTransparency";

	static float gameObjectEditorTransparency_default = 1;

	static float get_gameObjectEditorTransparency(ObjectModel obj) {
		return (float) obj.get("gameObjectEditorTransparency");
	}

	static void set_gameObjectEditorTransparency(ObjectModel obj, float gameObjectEditorTransparency) {
		obj.put("gameObjectEditorTransparency", gameObjectEditorTransparency);
	}

	// utils

	static boolean is(Object model) {
		return model instanceof GameObjectEditorComponent;
	}

	static void init(ObjectModel obj) {
		set_gameObjectEditorDirty(obj, gameObjectEditorDirty_default);
		set_gameObjectEditorName(obj, gameObjectEditorName_default);
		set_editorField(obj, gameObjectEditorField_default);
		set_gameObjectEditorShow(obj, gemeObjectEditorShow_default);
		set_gameObjectEditorClosed(obj, editorClosed_default);
		set_gameObjectEditorTransparency(obj, gameObjectEditorTransparency_default);
	}
}
