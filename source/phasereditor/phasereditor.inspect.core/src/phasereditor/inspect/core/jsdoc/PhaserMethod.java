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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhaserMethod extends PhaserMember {
	private List<PhaserMethodArg> _args;
	private Map<String, PhaserVariable> _argsMap;
	private String[] _returnTypes;
	private String _returnHelp;
	private PhaserType _declType;

	public PhaserMethod() {
		_args = new ArrayList<>();
		_argsMap = new HashMap<>();
		_returnTypes = new String[0];
	}

	@Override
	public PhaserType getDeclType() {
		return _declType;
	}

	public void setDeclType(PhaserType declType) {
		_declType = declType;
	}

	public String getReturnHelp() {
		return _returnHelp;
	}

	public void setReturnHelp(String returnHelp) {
		_returnHelp = returnHelp;
	}

	public String[] getReturnTypes() {
		return _returnTypes;
	}

	public void setReturnTypes(String[] returnTypes) {
		_returnTypes = returnTypes;
	}

	public List<PhaserMethodArg> getArgs() {
		return _args;
	}

	public Map<String, PhaserVariable> getArgsMap() {
		return _argsMap;
	}
}
