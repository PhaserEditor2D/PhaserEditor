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
package phasereditor.atlas.core;

import static java.lang.System.out;

import java.lang.reflect.Field;

import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;

class SettingsBeab_JSONSerialization_Generator {
	public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException {
		Settings bean = new Settings();
		Field[] fields = bean.getClass().getDeclaredFields();

		for (Field f : fields) {
			Class<?> cls = f.getType();

			if (cls.isArray()) {
				out.println("// avoid " + f.getName());
				continue;
			}

			if (cls.isEnum()) {
				out.println(f.getName() + " = " + cls.getSimpleName() + ".valueOf(obj.optString(\"" + f.getName()
						+ "\", \"" + f.get(bean) + "\"));");
			} else {
				out.println(f.getName() + " = " + getOptMethodCall(f) + "(\"" + f.getName() + "\", "
						+ getDefault(f, bean) + ");");
			}
		}

		out.println("\n\n------------\n\n");

		for (Field f : fields) {
			Class<?> cls = f.getType();

			if (cls.isArray()) {
				out.println("// avoid " + f.getName());
				continue;
			}

			if (cls.isEnum()) {
				out.println("obj.put(\"" + f.getName() + "\", " + f.getName() + ".name());");
			} else {
				out.println("obj.put(\"" + f.getName() + "\", " + f.getName() + ");");
			}
		}
	}

	private static String getDefault(Field f, Settings bean) throws IllegalArgumentException, IllegalAccessException {
		Object value = f.get(bean);
		if (value instanceof String) {
			return "\"" + value.toString() + "\"";
		}
		return value.toString();
	}

	private static String getOptMethodCall(Field f) {
		Class<?> type = f.getType();

		if (type == float.class) {
			return "(float) obj.optDouble";
		}

		String name = type.getSimpleName();
		name = name.substring(0, 1).toUpperCase() + name.substring(1);
		return "obj.opt" + name;
	}
}
