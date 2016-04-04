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
package phasereditor.chains.core;

import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserConstant;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserProperty;
import phasereditor.inspect.core.jsdoc.PhaserType;

public class ChainItem {
	private String _chain;
	private String _returnTypeName;
	private int _depth;
	private IPhaserMember _phaserMember;
	private String _display;

	public ChainItem(IPhaserMember phaserMember, String chain, String returnTypeName, int depth) {
		_phaserMember = phaserMember;
		_chain = chain;
		_returnTypeName = returnTypeName;
		_depth = depth;
		if (isType()) {
			_display = chain;
		} else {
			_display = getChain() + " : " + getReturnTypeName() + ";";
		}
	}

	public String getDisplay() {
		return _display;
	}

	public PhaserType getDeclarationType() {
		return getPhaserMember().getDeclType();
	}

	public String getMemberName() {
		return getPhaserMember().getName();
	}

	public boolean isConst() {
		return getPhaserMember() instanceof PhaserConstant;
	}

	public boolean isProperty() {
		return getPhaserMember() instanceof PhaserProperty;
	}

	public boolean isMethod() {
		return getPhaserMember() instanceof PhaserMethod;
	}

	public boolean isType() {
		return getPhaserMember() instanceof PhaserType;
	}

	public PhaserMethod getMethod() {
		return (PhaserMethod) getPhaserMember();
	}

	public PhaserProperty getProperty() {
		return (PhaserProperty) getPhaserMember();
	}

	public PhaserConstant getConstant() {
		return (PhaserConstant) getPhaserMember();
	}

	@Override
	public String toString() {
		return getDisplay();
	}

	public String getChain() {
		return _chain;
	}

	public String getReturnTypeName() {
		return _returnTypeName;
	}

	public int getDepth() {
		return _depth;
	}

	public IPhaserMember getPhaserMember() {
		return _phaserMember;
	}
}