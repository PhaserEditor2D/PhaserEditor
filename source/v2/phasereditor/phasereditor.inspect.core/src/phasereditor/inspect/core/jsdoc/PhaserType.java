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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

public class PhaserType extends PhaserNamespace implements ITypeMember {
	
	private static final long serialVersionUID = 1L;
	
	public static final String PRIVATE = "private";
	public static final String PROTECTED = "protected";
	public static final String PUBLIC = "public";
	public static final String[] VISIBILITY = { PUBLIC, PROTECTED, PRIVATE };

	private List<String> _extends;
	private boolean _constructor;
	private List<PhaserMethodArg> _constructorArgs;
	private boolean _enum;
	private String[] _enumElementsType;
	private Set<PhaserType> _extenders;
	private Set<PhaserType> _extending;
	private boolean _typeDef;

	public PhaserType(JSONObject json) {
		super(json);
		_constructorArgs = new ArrayList<>();
		_extends = Collections.emptyList();
		_constructor = false;
		_enum = false;
		_extending = new LinkedHashSet<>();
		_extenders = new LinkedHashSet<>();
	}

	public Set<PhaserType> getExtending() {
		return _extending;
	}

	public Set<PhaserType> getExtenders() {
		return _extenders;
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
	public PhaserType getDeclType() {
		return this;
	}

	public void setEnum(boolean isEnum) {
		_enum = isEnum;
	}

	public boolean isEnum() {
		return _enum;
	}

	public void setEnumElementsType(String[] enumElementsType) {
		_enumElementsType = enumElementsType;
	}

	public String[] getEnumElementsType() {
		return _enumElementsType;
	}

	public void setTypeDef(boolean typeDef) {
		_typeDef = typeDef;
	}
	
	public boolean isTypeDef() {
		return _typeDef;
	}
}
