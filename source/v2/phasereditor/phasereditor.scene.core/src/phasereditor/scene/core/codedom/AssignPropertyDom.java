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

import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class AssignPropertyDom extends CodeDom {
	private String _propertyName;
	private String _propertyValueExpr;
	private String _contextExpr;
	private String _propertyType;

	public AssignPropertyDom(String propertyName, String contextExpr) {
		_propertyName = propertyName;
		_contextExpr = contextExpr;
	}

	public void value(String expr) {
		_propertyValueExpr = expr;
	}

	public void valueLiteral(String expr) {
		_propertyValueExpr = JSONObject.quote(expr);
	}

	public void value(float n) {
		_propertyValueExpr = Float.toString(n);
	}

	public void value(int n) {
		_propertyValueExpr = Integer.toString(n);
	}
	
	public void value(boolean expr) {
		value(Boolean.toString(expr));
	}

	public String getPropertyName() {
		return _propertyName;
	}

	public String getContextExpr() {
		return _contextExpr;
	}

	public String getPropertyValueExpr() {
		return _propertyValueExpr;
	}

	public String getPropertyType() {
		return _propertyType;
	}

	public void setPropertyType(String propertyType) {
		_propertyType = propertyType;
	}

}
