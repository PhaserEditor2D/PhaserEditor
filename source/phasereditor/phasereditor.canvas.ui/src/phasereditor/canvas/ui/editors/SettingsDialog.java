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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author arian
 *
 */
public class SettingsDialog extends Dialog {
	@SuppressWarnings("unused")
	private DataBindingContext m_bindingContext;
	private Text _text;
	private Text _text_1;
	private SceneSettings _model;
	private Button _btnGenerateOnSave;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public SettingsDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@SuppressWarnings("unused")
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));

		Group grpWorld = new Group(container, SWT.NONE);
		grpWorld.setLayout(new GridLayout(4, false));
		grpWorld.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpWorld.setText("Scene");

		Label lblWidth = new Label(grpWorld, SWT.NONE);
		lblWidth.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblWidth.setText("Width");

		_text = new Text(grpWorld, SWT.BORDER);
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblHeight = new Label(grpWorld, SWT.NONE);
		lblHeight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblHeight.setText("Height");

		_text_1 = new Text(grpWorld, SWT.BORDER);
		_text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblColor = new Label(grpWorld, SWT.NONE);
		lblColor.setText("Color");

		Button btnNewButton = new Button(grpWorld, SWT.NONE);
		btnNewButton.setText("New Button");
		new Label(grpWorld, SWT.NONE);
		new Label(grpWorld, SWT.NONE);

		Group grpCodeGeneration = new Group(container, SWT.NONE);
		grpCodeGeneration.setLayout(new GridLayout(1, false));
		grpCodeGeneration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		grpCodeGeneration.setText("Code Generation");

		_btnGenerateOnSave = new Button(grpCodeGeneration, SWT.CHECK);
		_btnGenerateOnSave.setText("Generate on Save");

		return container;
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
		m_bindingContext = initDataBindings();
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	public SceneSettings getModel() {
		return _model;
	}

	public void setModel(SceneSettings model) {
		_model = model;
	}

	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue sceneWidth_modelObserveValue = PojoProperties.value("sceneWidth").observe(_model);
		bindingContext.bindValue(observeText_textObserveWidget, sceneWidth_modelObserveValue, null, null);
		//
		IObservableValue observeText_text_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_1);
		IObservableValue sceneHeightGetModelObserveValue = PojoProperties.value("sceneHeight").observe(getModel());
		bindingContext.bindValue(observeText_text_1ObserveWidget, sceneHeightGetModelObserveValue, null, null);
		//
		IObservableValue observeSelection_btnGenerateOnSaveObserveWidget = WidgetProperties.selection()
				.observe(_btnGenerateOnSave);
		IObservableValue generateOnSaveGetModelObserveValue = PojoProperties.value("generateOnSave")
				.observe(getModel());
		bindingContext.bindValue(observeSelection_btnGenerateOnSaveObserveWidget, generateOnSaveGetModelObserveValue,
				null, null);
		//
		return bindingContext;
	}
}
