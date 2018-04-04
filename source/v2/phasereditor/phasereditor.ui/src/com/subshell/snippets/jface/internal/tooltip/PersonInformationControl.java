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
package com.subshell.snippets.jface.internal.tooltip;

import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * An information control for {@link Person}s. Expects a {@link Person} to be passed as the input.
 */
class PersonInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {
	private Label label;

	PersonInformationControl(Shell parent) {
		super(parent, false);
		create();
	}

	@Override
	protected void createContent(Composite parent) {
		label = new Label(parent, SWT.NONE);
		Display display = parent.getDisplay();
		label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	}

	@Override
	public void setInput(Object input) {
		setInput((Person) input);
	}

	private void setInput(Person person) {
		String labelText = "";
		if (person != null) {
			labelText = "Prename: " + person.getPrename() + "\nLastname: " + person.getLastname();
		}
		label.setText(labelText);
	}

	@Override
	public boolean hasContents() {
		return (label != null) && !label.getText().equals("");
	}

	@Override
	public Point computeSizeHint() {
		Rectangle trim = super.computeTrim();
		Point size = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		size.x += trim.width * 2;
		size.y += trim.height * 2;
		return size;
	}

}
