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

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import javafx.collections.FXCollections;
import javafx.embed.swt.FXCanvas;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

/**
 * @author arian
 *
 */
public class PGridFrameDialog extends Dialog {
	private FXCanvas _canvas;
	private List<Object> _frames;
	private Object _selectedframe;
	private ListView<Object> _listView;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public PGridFrameDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.SHELL_TRIM);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		_canvas = new FXCanvas(container, SWT.BORDER);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		afterCreateWidgets();

		return container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Frame Selector");
	}

	private void afterCreateWidgets() {
		_listView = new ListView<>(FXCollections.observableList(_frames));
		_listView.setOrientation(Orientation.VERTICAL);
		_listView.setCellFactory(new Callback<ListView<Object>, ListCell<Object>>() {

			@Override
			public ListCell<Object> call(ListView<Object> param) {
				return new FrameCell();
			}
		});
		_listView.getSelectionModel().select(_listView.getItems().indexOf(_selectedframe));
		_canvas.setScene(new Scene(_listView));
		_canvas.setFocus();
	}

	@Override
	protected void okPressed() {
		_selectedframe = _listView.getSelectionModel().getSelectedItem();
		super.okPressed();
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
		return new Point(367, 287);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setFrames(List frames) {
		_frames = frames;
	}

	public Object getSelectedFrame() {
		return _selectedframe;
	}

	public void setSelectedframe(Object selframe) {
		_selectedframe = selframe;
	}
}
