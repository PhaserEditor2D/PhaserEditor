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
package phasereditor.ui.info;

import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import phasereditor.ui.PhaserEditorUI;

public abstract class BaseInformationControl extends AbstractInformationControl
		implements IInformationControlExtension2 {

	private Control _contentControl;

	/**
	 * This is used because there are scenarios where the
	 * {@link #setInput(Object)} method is not called. It is the case of the
	 * "hover" in the javascript editor, cause it uses a {@link BestMatchHover}
	 * wrapper that does not implements {@link IInformationControlExtension2}.
	 */
	public BaseInformationControl(Shell parentShell) {
		super(parentShell, false);
		create();
	}

	@Override
	public Point computeSizeHint() {
		return new Point(255, 200);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible) {
			handleHidden(_contentControl);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.AbstractInformationControl#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		disposeControl(_contentControl);
	}

	@SuppressWarnings("unused")
	protected void disposeControl(Control control) {
		// nothing
	}

	@SuppressWarnings("unused")
	protected void handleHidden(Control control) {
		// nothing
	}

	@Override
	public boolean hasContents() {
		return true;
	}

	@Override
	protected final void createContent(Composite parentComp) {
		FillLayout layout = new FillLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		parentComp.setLayout(layout);
		_contentControl = createContent2(parentComp);
		Shell shell = _contentControl.getShell();
		PhaserEditorUI.applyThemeStyle(shell);
	}

	protected abstract Control createContent2(Composite parentComp);

	protected abstract void updateContent(Control control, Object model);

	@Override
	public void setInput(Object object) {
		updateContent(_contentControl, object);
	}
}