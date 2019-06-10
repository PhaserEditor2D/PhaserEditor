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
package phasereditor.project.core.codegen;

import static phasereditor.ui.Colors.hexColor;

import java.util.List;

import org.eclipse.swt.graphics.RGB;

import phasereditor.ui.Colors;

/**
 * @author arian
 *
 */
public abstract class BaseCodeGenerator implements ICodeGenerator {
	private final StringBuilder _sb;
	private String _replace;
	private int _indent;

	public BaseCodeGenerator() {
		_sb = new StringBuilder();
	}

	public int getOffset() {
		return _sb.length();
	}

	@Override
	public final String generate(String replace) {
		_replace = replace == null ? "" : replace;

		internalGenerate();

		return _sb.toString();
	}

	protected abstract void internalGenerate();

	public int length() {
		return _sb.length();
	}

	public String getSectionContent(String closeTag, String defaultContent) {
		var j = _replace.indexOf(closeTag);

		var size = _replace.length();

		if (size > 0 && j != -1) {
			String section = _replace.substring(0, j);
			return section;
		}

		return defaultContent;
	}

	public String getSectionContent(String openTag, String closeTag, String defaultContent) {
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

	public String getReplaceContent() {
		return _replace;
	}

	public void userCode(String text) {
		String[] lines = text.split("\n");
		for (String line : lines) {
			line(line);
		}
	}

	public void sectionStart(String endTag, String defaultContent) {
		append(getSectionContent(endTag, defaultContent));
		append(endTag);
	}

	public void section(String openTag, String defaultContent) {
		append(openTag);
		append(getSectionContent(openTag, "papa(--o^^o--)pig", defaultContent));
	}

	public void section(String openTag, String closeTag, String defaultContent) {
		String content = getSectionContent(openTag, closeTag, defaultContent);

		append(openTag);
		append(content);
		append(closeTag);
	}

	public String cut(int start, int end) {
		String str = _sb.substring(start, end);
		_sb.delete(start, end);
		return str;
	}

	public void trim(Runnable run) {
		int a = length();
		run.run();
		int b = length();

		String str = _sb.substring(a, b);

		if (str.trim().length() == 0) {
			_sb.delete(a, b);
		}
	}

	public void append(String str) {
		_sb.append(str);
	}

	public void join(List<String> elems) {
		for (var i = 0; i < elems.size(); i++) {
			if (i > 0) {
				append(", ");
			}
			append(elems.get(i));
		}
	}

	public void line() {
		line("");
	}

	public void line(String line) {
		append(line);
		append("\n");
		append(getIndentTabs());
	}

	public static String escapeStringLiterals(String text) {
		return text.replace("\\", "\\\\").replace("\\R", "\n").replace("'", "\\'").replace("\"", "\\\"");
	}

	public void openIndent(String line) {
		_indent++;
		line(line);
	}

	public void openIndent() {
		openIndent("");
	}

	public void closeIndent(String str) {
		_indent--;
		line();
		line(str);
	}

	public void closeIndent() {
		closeIndent("");
	}

	public String getIndentTabs() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < _indent; i++) {
			sb.append("\t");
		}
		return sb.toString();
	}

	public static String emptyStringToNull(String str) {
		return str == null ? null : (str.trim().length() == 0 ? null : str);
	}

	public static String getHexString(RGB rgb) {
		return "#" + hexColor(rgb);
	}

	public static String getHexString2(RGB rgb) {
		return "0x" + Colors.hexColor(rgb);
	}


	public static String getRGBString(RGB rgb) {
		return "rgb(" + rgb.red + "," + rgb.green + "," + rgb.blue + ")";
	}

}
