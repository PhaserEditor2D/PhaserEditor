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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhaserType implements IPhaserMember {
	public static final String PRIVATE = "private";
	public static final String PROTECTED = "protected";
	public static final String PUBLIC = "public";
	public static final String[] VISIBILITY = { PUBLIC, PROTECTED, PRIVATE };

	private List<PhaserConstant> _consts;
	private List<PhaserProperty> _properties;
	private List<PhaserMethod> _methods;
	private Map<String, PhaserMember> _memberMap;

	private String _name;
	private String _help;
	private List<String> _extends;
	private boolean _constructor;
	private List<PhaserMethodArg> _constructorArgs;
	private int _line;
	private Path _file;
	private int _offset;
	private boolean _static;

	public PhaserType() {
		_properties = new ArrayList<>();
		_methods = new ArrayList<>();
		_consts = new ArrayList<>();
		_constructorArgs = new ArrayList<>();
		_memberMap = new HashMap<>();

		_name = null;
		_help = null;
		_extends = Collections.emptyList();
		_constructor = false;
		_static = false;
	}

	public void setStatic(boolean static1) {
		_static = static1;
	}

	@Override
	public boolean isStatic() {
		return _static;
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
	public int getLine() {
		return _line;
	}

	@Override
	public void setLine(int definitionLine) {
		_line = definitionLine;
	}

	@Override
	public int getOffset() {
		return _offset;
	}

	@Override
	public void setOffset(int offset) {
		_offset = offset;
	}

	public List<String> getExtends() {
		return _extends;
	}

	public void setExtends(List<String> extends1) {
		_extends = extends1;
	}

	public boolean isConstructor() {
		return _constructor;
	}

	public void setConstructor(boolean constructor) {
		_constructor = constructor;
	}

	public List<PhaserMethodArg> getConstructorArgs() {
		return _constructorArgs;
	}

	@Override
	public String getName() {
		return _name;
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

	public List<PhaserProperty> getProperties() {
		return _properties;
	}

	public List<PhaserMethod> getMethods() {
		return _methods;
	}

	public List<PhaserConstant> getConstants() {
		return _consts;
	}

	public Map<String, PhaserMember> getMemberMap() {
		return _memberMap;
	}

	@Override
	public PhaserType getDeclType() {
		return this;
	}
}
