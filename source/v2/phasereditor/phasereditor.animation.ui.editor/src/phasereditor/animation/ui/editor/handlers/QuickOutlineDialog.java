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
package phasereditor.animation.ui.editor.handlers;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import phasereditor.animation.ui.editor.AnimationsTreeViewer;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.TreeCanvas;

/**
 * @author arian
 *
 */
public class QuickOutlineDialog extends Dialog implements MouseListener {

	private FilteredTreeCanvas _filteredTree;
	private AnimationsModel _model;
	private AnimationsTreeViewer _viewer;
	private AnimationModel _selected;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public QuickOutlineDialog(Shell parentShell) {
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
		container.setLayout(new FillLayout());

		_filteredTree = new FilteredTreeCanvas(container, SWT.BORDER);
		_viewer = new AnimationsTreeViewer(_filteredTree.getCanvas());

		afterCreateWidgets();

		return container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Animations");
	}

	private void afterCreateWidgets() {
		_viewer.getCanvas().addMouseListener(this);
		_viewer.setInput(getModel());

		if (_selected != null) {
			_viewer.getCanvas().getUtils().setSelectionObject(_selected);
		}
	}

	@Override
	protected void okPressed() {

		TreeCanvas canvas = _viewer.getCanvas();
		List<Object> selection = canvas.getUtils().getSelectedObjects();
		if (selection.isEmpty()) {
			if (canvas.getVisibleItems().size() == 1) {
				_selected = (AnimationModel) canvas.getVisibleItems().get(0).getData();
				return;
			}

			return;
		}

		_selected = (AnimationModel) selection.get(0);

		super.okPressed();
	}

	public AnimationsModel getModel() {
		return _model;
	}

	public void setModel(AnimationsModel model) {
		_model = model;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 600);
	}

	public void setSelected(AnimationModel selected) {
		_selected = selected;
	}

	public AnimationModel getSelected() {
		return _selected;
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		okPressed();
	}

	@Override
	public void mouseDown(MouseEvent e) {
		//
	}

	@Override
	public void mouseUp(MouseEvent e) {
		//
	}
}
