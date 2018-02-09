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

import org.eclipse.swt.graphics.RGB;

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

	public void section(String openTag, String defaultContent) {
		append(openTag);
		append(getSectionContent(openTag, "papa(--o^^o--)pig", defaultContent));
	}

	public void section(String openTag, String closeTag, String defaultContent) {
		String content = getSectionContent(openTag, closeTag, defaultContent);

		if (openTag.equals("/* --- pre-init-begin --- */") || openTag.equals("/* --- post-init-begin --- */")) {
			String content2 = content.trim();
			if (content2.length() == 0 || (content2.startsWith("//") && !content2.contains("\n"))) {
				return;
			}
		}
		
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

	public void line() {
		line("");
	}

	public void line(String line) {
		append(line);
		append("\n");
		append(getIndentTabs());
	}
	
	public static String escapeLines(String text) {		
		return text.replace("\r\n", "\n").replace("\n", "\\n");
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
		return "#" + toHexString(rgb.red) + toHexString(rgb.green) + toHexString(rgb.blue);
	}

	public static String getHexString2(RGB rgb) {
		return "0x" + toHexString(rgb.red) + toHexString(rgb.green) + toHexString(rgb.blue);
	}

	public static String toHexString(int n) {
		String s = Integer.toHexString(n);
		return (s.length() == 1 ? "0" : "") + s;
	}

	public static String getRGBString(RGB rgb) {
		return "rgb(" + rgb.red + "," + rgb.green + "," + rgb.blue + ")";
	}

}
