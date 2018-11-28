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

import java.util.ArrayList;
import java.util.List;

/**
 * @author arian
 *
 */
@SuppressWarnings("unchecked")
public interface ParentComponent {
	// children

	static String children_name = "children";

	static List<ObjectModel> get_children(ObjectModel obj) {
		return (List<ObjectModel>) obj.get("children");
	}

	static void set_children(ObjectModel obj, List<ObjectModel> children) {
		obj.put("children", children);
	}

	// parent

	static String parent_name = "parent";

	static ObjectModel parent_default = null;

	static ObjectModel get_parent(ObjectModel obj) {
		return (ObjectModel) obj.get("parent");
	}

	static void set_parent(ObjectModel child, ObjectModel parent) {
		child.put("parent", parent);
	}

	// utils

	static void utils_removeFromParent(ObjectModel child) {
		var parent = get_parent(child);
		if (parent != null) {
			utils_removeChild(parent, child);
		}
	}

	static void utils_addChild(ObjectModel parent, ObjectModel child) {
		var children = get_children(parent);
		children.add(child);
		set_parent(child, parent);
	}

	static void utils_moveChild(ObjectModel newParent, ObjectModel child) {
		utils_removeFromParent(child);
		utils_addChild(newParent, child);
	}

	static void utils_addChild(ObjectModel parent, int index, ObjectModel child) {
		var children = get_children(parent);
		children.add(index, child);
		set_parent(child, parent);
	}

	static void utils_removeChild(ObjectModel parent, ObjectModel child) {
		var children = get_children(parent);
		children.remove(child);
		set_parent(child, null);
	}

	static boolean utils_isDescendentOf(ObjectModel child, ObjectModel parent) {
		if (child == null) {
			return false;
		}

		if (child == parent) {
			return true;
		}

		return utils_isDescendentOf(get_parent(child), parent);
	}

	static boolean is(Object model) {
		return model instanceof ParentComponent;
	}

	static void init(ObjectModel obj) {
		set_parent(obj, parent_default);
		set_children(obj, new ArrayList<>());
	}
}
