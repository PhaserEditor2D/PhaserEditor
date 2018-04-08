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
package com.phasereditor2d.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtils {
	public static <T> T[] toArray(Class<T> cls, JSONArray jsonArray) {
		T[] array = (T[]) Array.newInstance(cls, jsonArray.length());
		for (int i = 0; i < array.length; i++) {
			array[i] = (T) jsonArray.get(i);
		}
		return array;
	}

	public static <T> List<T> toList(JSONArray array) {
		List<T> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			list.add((T) array.get(i));
		}
		return list;
	}

	public static <T> Iterable<T> iter(JSONArray array, Function<Integer, T> get) {
		return new Iterable<T>() {

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					Integer i = 0;

					@Override
					public T next() {
						T e = get.apply(i);
						return e;
					}

					@Override
					public boolean hasNext() {
						return i < array.length();
					}
				};
			}
		};
	}

	public static Iterable<JSONObject> iterObj(JSONArray array) {
		return new Iterable<JSONObject>() {

			@Override
			public Iterator<JSONObject> iterator() {
				return new Iterator<JSONObject>() {
					int i = 0;

					@Override
					public JSONObject next() {
						return array.getJSONObject(i++);
					}

					@Override
					public boolean hasNext() {
						return i < array.length();
					}
				};
			}
		};
	}
}
