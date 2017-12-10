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
package phasereditor.canvas.ui.editors.grid.editors;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;

/**
 * @author arian
 *
 */
public class FrameDialog extends Dialog {

	private Composite _container;
	private TreeViewer _viewer;
	private List<?> _frames;
	private Object _result;
	private Object _selection;
	private boolean _allowNull;
	private boolean _allowMultipleSelection;
	private List<?> _multipleResult = Collections.emptyList();

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public FrameDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.RESIZE);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		_container = (Composite) super.createDialogArea(parent);
		FillLayout fl_container = new FillLayout(SWT.HORIZONTAL);
		fl_container.marginWidth = 5;
		fl_container.marginHeight = 5;
		_container.setLayout(fl_container);

		afterCreateWidgets();

		return _container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Frame Selector");
	}

	private void afterCreateWidgets() {
		PatternFilter filter = new PatternFilter();
		filter.setIncludeLeadingWildcard(true);
		FilteredTree tree = new FilteredTree(_container, getTreeStyle(), filter, true);
		tree.setQuickSelectionMode(true);
		_viewer = tree.getViewer();
		_viewer.setContentProvider(new ITreeContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				//
			}

			@Override
			public void dispose() {
				//
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@SuppressWarnings("synthetic-access")
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement == _frames) {
					return _frames.toArray();
				}
				return new Object[0];
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return getElements(parentElement);
			}
		});
		_viewer.setLabelProvider(AssetLabelProvider.GLOBAL_48);
		_viewer.setInput(_frames);
		_viewer.setSelection(_selection == null ? StructuredSelection.EMPTY : new StructuredSelection(_selection),
				true);
		_viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
	}

	private int getTreeStyle() {
		if (_allowMultipleSelection) {
			return SWT.BORDER | SWT.MULTI;
		}
		return SWT.SINGLE | SWT.BORDER;
	}

	public void setFrames(List<?> frames) {
		_frames = frames;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CLIENT_ID) {
			_result = PGridFrameProperty.NULL_FRAME;
			setReturnCode(OK);
			close();
		}
		super.buttonPressed(buttonId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		IStructuredSelection sel = (IStructuredSelection) _viewer.getSelection();
		_result = sel.getFirstElement();
		_multipleResult = sel.toList();
		super.okPressed();
	}

	public Object getResult() {
		return _result;
	}

	public List<?> getMultipleResult() {
		return _multipleResult;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (_allowNull) {
			createButton(parent, IDialogConstants.CLIENT_ID, "Set Null", false);
		}
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	public boolean isAllowNull() {
		return _allowNull;
	}

	public void setAllowNull(boolean allowNull) {
		_allowNull = allowNull;
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	public void setSelectedItem(Object selection) {
		_selection = selection;
	}

	public void setAllowMultipleSelection(boolean allow) {
		_allowMultipleSelection = allow;
	}

}
