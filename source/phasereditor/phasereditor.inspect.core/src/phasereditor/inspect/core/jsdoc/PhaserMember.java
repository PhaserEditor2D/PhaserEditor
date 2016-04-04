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
package phasereditor.inspect.core.jsdoc;

import java.nio.file.Path;

public abstract class PhaserMember implements IPhaserMember {
	private String _name;
	private String _help;
	private int _line;
	private int _offset;
	private Path _file;
	private boolean _static;

	public PhaserMember() {
	}

	/**
	 * @return the static
	 */
	@Override
	public boolean isStatic() {
		return _static;
	}

	/**
	 * @param static1
	 *            the static to set
	 */
	public void setStatic(boolean static1) {
		_static = static1;
	}

	@Override
	public Path getFile() {
		return _file;
	}

	@Override
	public void setFile(Path file) {
		_file = file;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public int getLine() {
		return _line;
	}

	@Override
	public void setLine(int line) {
		_line = line;
	}

	@Override
	public int getOffset() {
		return _offset;
	}

	@Override
	public void setOffset(int offset) {
		_offset = offset;
	}

	public void setName(String name) {
		_name = name;
	}

	@Override
	public String getHelp() {
		return _help;
	}

	public void setHelp(String help) {
		_help = help;
	}
}
