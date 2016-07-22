// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors;

import java.util.Arrays;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.ui.FilteredTree2;

/**
 * @author arian
 *
 */
public class SelectGroupDialog extends Dialog {

	private FilteredTree _filteredTree;
	private ObjectCanvas _canvas;
	private Object _result;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public SelectGroupDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		FillLayout fl_container = new FillLayout(SWT.HORIZONTAL);
		fl_container.marginWidth = 5;
		fl_container.marginHeight = 5;
		container.setLayout(fl_container);

		PatternFilter filter = new PatternFilter();
		filter.setIncludeLeadingWildcard(true);

		_filteredTree = new FilteredTree2(container, SWT.BORDER | SWT.SINGLE, filter, 3);

		TreeViewer viewer = _filteredTree.getViewer();
		viewer.setLabelProvider(new OutlineLabelProvider());
		viewer.setContentProvider(new OutlineContentProvider(true) {
			@Override
			public Object[] getChildren(@SuppressWarnings("hiding") Object parent) {
				Object[] children = super.getChildren(parent);
				return Arrays.stream(children).filter(e -> e instanceof GroupNode).toArray();
			}
		});
		viewer.setInput(_canvas);
		viewer.expandToLevel(3);
		viewer.addDoubleClickListener(e -> {
			okPressed();
		});

		return container;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		_result = _filteredTree.getViewer().getStructuredSelection().getFirstElement();
		super.okPressed();
	}

	public GroupNode getResult() {
		return (GroupNode) _result;
	}

	public void setCanvas(ObjectCanvas canvas) {
		_canvas = canvas;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select Target Group");
	}
}
