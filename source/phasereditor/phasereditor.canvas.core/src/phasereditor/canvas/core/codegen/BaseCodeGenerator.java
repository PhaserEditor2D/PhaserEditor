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

import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.WorldModel;

/**
 * @author arian
 *
 */
public abstract class BaseCodeGenerator implements ICodeGenerator {
	private final StringBuilder _sb;
	private String _replace;
	private int _indent;
	protected final WorldModel _world;
	protected final CanvasModel _model;
	protected final EditorSettings _settings;

	public BaseCodeGenerator(CanvasModel model) {
		_sb = new StringBuilder();
		_world = model.getWorld();
		_settings = model.getSettings();
		_model = model;
	}

	@Override
	public final String generate(String replace) {
		_replace = replace == null ? "" : replace;

		internalGenerate();

		return _sb.toString();
	}

	protected abstract void internalGenerate();

	protected int length() {
		return _sb.length();
	}

	protected String getSectionContent(String openTag, String closeTag, String defaultContent) {
		int i = _replace.indexOf(openTag);
		int j = _replace.indexOf(closeTag);
		if (j == -1) {
			j = _replace.length();
		}
		if (i != -1 && j != -1) {
			String section = _replace.substring(i + openTag.length(), j);
			return section;
		}

		return defaultContent;
	}

	protected void section(String openTag, String defaultContent) {
		append(openTag);
		append(getSectionContent(openTag, "papa(--o^^o--)pig", defaultContent));
	}

	protected void section(String openTag, String closeTag, String defaultContent) {
		append(openTag);
		append(getSectionContent(openTag, closeTag, defaultContent));
		append(closeTag);
	}

	protected String cut(int start, int end) {
		String str = _sb.substring(start, end);
		_sb.delete(start, end);
		return str;
	}

	protected void append(String str) {
		_sb.append(str);
	}

	protected void line() {
		line("");
	}

	protected void line(String line) {
		append(line);
		append("\n");
		append(getIndentTabs());
	}

	protected void openIndent(String line) {
		_indent++;
		line(line);
	}

	protected void openIndent() {
		openIndent("");
	}

	protected void closeIndent(String str) {
		_indent--;
		line();
		line(str);
	}
	
	protected void closeIndent() {
		closeIndent("");
	}

	protected String getIndentTabs() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < _indent; i++) {
			sb.append("\t");
		}
		return sb.toString();
	}

	protected static String emptyStringToNull(String str) {
		return str == null ? null : (str.trim().length() == 0 ? null : str);
	}

	protected static String round(double x) {
		return Integer.toString((int) Math.round(x));
	}
}
