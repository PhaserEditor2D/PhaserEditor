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
public interface EditorComponent {
	// editorShow

	static String editorShow_name = "editorShow";

	static boolean editorShow_default = true;

	static boolean get_editorShow(ObjectModel obj) {
		return (boolean) obj.get("editorShow");
	}

	static void set_editorShow(ObjectModel obj, boolean editorShow) {
		obj.put("editorShow", editorShow);
	}

	// editorClosed

	static String editorClosed_name = "editorClosed";

	static boolean editorClosed_default = false;

	static boolean get_editorClosed(ObjectModel obj) {
		return (boolean) obj.get("editorClosed");
	}

	static void set_editorClosed(ObjectModel obj, boolean editorClosed) {
		obj.put("editorClosed", editorClosed);
	}

	// editorName

	static String editorName_name = "editorName";
	
	static String editorName_default = "unnamed";

	static String get_editorName(ObjectModel obj) {
		return (String) obj.get("editorName");
	}

	static void set_editorName(ObjectModel obj, String editorName) {
		obj.put("editorName", editorName);
	}

	static void init(ObjectModel obj) {
		set_editorName(obj, editorName_default);
		set_editorShow(obj, editorShow_default);
		set_editorClosed(obj, editorClosed_default);
	}
}
