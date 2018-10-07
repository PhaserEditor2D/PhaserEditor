// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.scene.core.codedom;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arian
 *
 */
public class MethodCallDom {

	private String _methodName;
	private String _contextExpr;
	private List<String> _args;
	private String _returnToVar;

	public MethodCallDom(String methodName, String contextExpr) {
		super();
		_methodName = methodName;
		_contextExpr = contextExpr;
		_args = new ArrayList<>();
	}
	
	public String getReturnToVar() {
		return _returnToVar;
	}
	
	public void setReturnToVar(String returnToVar) {
		_returnToVar = returnToVar;
	}

	public void arg(String expr) {
		_args.add(expr);
	}

	public void argLiteral(String expr) {
		_args.add("'" + expr.replace("'", "\\\\'") + "'");
	}

	public void arg(float n) {
		_args.add(Float.toString(n));
	}

	public void arg(int n) {
		_args.add(Integer.toString(n));
	}

	public String getMethodName() {
		return _methodName;
	}

	public String getContextExpr() {
		return _contextExpr;
	}

	public List<String> getArgs() {
		return _args;
	}
}
