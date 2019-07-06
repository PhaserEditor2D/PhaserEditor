// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.ide.ui.toolbar;

import static phasereditor.ui.PhaserEditorUI.getWorkbenchWindow;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import phasereditor.ui.IEditorHugeToolbar;
import phasereditor.ui.ISuperWorkbenchListener;

public class HugeToolbar extends Composite implements ISuperWorkbenchListener {

	private Composite _centerArea;
	private IEditorPart _activeEditor;
	private IWorkbenchWindow _window;

	@SuppressWarnings("unused")
	public HugeToolbar(Composite parent) {
		super(parent, 0);

		var gridLayout = new GridLayout(3, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		parent.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				updateBounds();
			}

			@Override
			public void controlMoved(ControlEvent e) {
				updateBounds();
			}
		});

		var layout = new RowLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.center = true;

		var leftArea = new Composite(this, 0);
		leftArea.setLayout(layout);
		leftArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		{
			_centerArea = new Composite(this, 0);
			_centerArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_centerArea.setLayout(layout);
		}

		var rightArea = new Composite(this, 0);
		rightArea.setLayout(layout);

		new GoHomeWrapper(leftArea);
		new NewMenuWrapper(leftArea);
		new RunProjectWrapper(leftArea);
		new AlienEditorWrapper(leftArea);
		new QuickAccessWrapper(rightArea);

		{
			var sep = new Label(rightArea, SWT.SEPARATOR);
			sep.setLayoutData(new RowData(SWT.DEFAULT, 15));
		}

		new PerspectiveSwitcherWrapper(rightArea);

		updateBounds();

		// run async because the window is not finished
		swtRun(() -> {
			_window = getWorkbenchWindow(parent);

			if (_window == null) {
				return;
			}

			_window.addPageListener(this);
			pageOpened(_window.getActivePage());
		});

	}

	private void updateBounds() {
		var size = computeSize(SWT.DEFAULT, SWT.DEFAULT);
		setBounds(0, 0, getParent().getClientArea().width, size.y);
	}

	private void updateWithCurrentEditor() {
		var editor = _window.getActivePage().getActiveEditor();
		if (editor == _activeEditor) {
			return;
		}

		for (var c : _centerArea.getChildren()) {
			c.dispose();
		}

		var editorToolbar = editor.getAdapter(IEditorHugeToolbar.class);
		if (editorToolbar != null) {
			editorToolbar.createContent(_centerArea);
		}

		_centerArea.requestLayout();
	}

	@Override
	public void pageOpened(IWorkbenchPage page) {
		page.addPartListener(this);
		updateWithCurrentEditor();
	}

	@Override
	public void pageClosed(IWorkbenchPage page) {
		page.removePartListener(this);
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		updateWithCurrentEditor();
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		updateWithCurrentEditor();
	}
}