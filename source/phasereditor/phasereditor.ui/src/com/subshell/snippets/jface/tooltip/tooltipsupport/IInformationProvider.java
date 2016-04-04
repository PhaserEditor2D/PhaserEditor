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
package com.subshell.snippets.jface.tooltip.tooltipsupport;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Information provider interface to get information about elements under the mouse cursor.
 * This interface is analog to {@link org.eclipse.jface.text.information.IInformationProvider} in
 * {@link org.eclipse.jface.text.information.InformationPresenter}.
 */
public interface IInformationProvider {

	/**
	 * Returns information about the element at the specified location.
	 * The information returned is used to display an appropriate tooltip.
	 * @param location the location of the element (the coordinate is in the receiver's coordinate system)
	 * @return information about the element, or <code>null</code> if none is available
	 */
	Object getInformation(Point location);

	/**
	 * Returns the area of the element at the specified location.
	 * The area returned is used to place an appropriate tooltip.
	 * @param location the location of the element (the coordinate is in the receiver's coordinate system)
	 * @return the area of the element, or <code>null</code> if none is available
	 */
	Rectangle getArea(Point location);
}