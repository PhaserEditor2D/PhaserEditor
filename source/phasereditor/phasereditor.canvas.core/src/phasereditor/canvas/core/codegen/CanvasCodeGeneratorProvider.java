// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.core.codegen;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.SourceLang;

/**
 * @author arian
 *
 */
public class CanvasCodeGeneratorProvider {
	private static Map<String, Class<? extends ICodeGenerator>> _map;

	static {
		_map = new HashMap<>();

		_map.put(key(CanvasType.STATE, SourceLang.JAVA_SCRIPT), JSStateCodeGenerator.class);
		_map.put(key(CanvasType.STATE, SourceLang.TYPE_SCRIPT), TSStateCodeGenerator.class);

		_map.put(key(CanvasType.GROUP, SourceLang.JAVA_SCRIPT), JSGroupCodeGenerator.class);
		_map.put(key(CanvasType.GROUP, SourceLang.TYPE_SCRIPT), TSGroupCodeGenerator.class);

		_map.put(key(CanvasType.SPRITE, SourceLang.JAVA_SCRIPT), JSGroupCodeGenerator.class);
		_map.put(key(CanvasType.SPRITE, SourceLang.TYPE_SCRIPT), TSGroupCodeGenerator.class);

	}

	@SuppressWarnings("static-method")
	public ICodeGenerator getCodeGenerator(CanvasModel model) {
		CanvasType type = model.getType();
		SourceLang lang = model.getSettings().getLang();
		Class<? extends ICodeGenerator> cls = _map.get(key(type, lang));
		Constructor<? extends ICodeGenerator> ctr;
		try {
			ctr = cls.getConstructor(CanvasModel.class);
			ICodeGenerator generator = ctr.newInstance(model);
			return generator;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static String key(CanvasType type, SourceLang lang) {
		return type.name() + "$" + lang.name();
	}
}
