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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.canvas.core.SceneSettings;
import phasereditor.ui.ColorButtonSupport;

/**
 * @author arian
 *
 */
public class GeneralEditorSettingsComp extends Composite {
	private GeneralEditorSettingsComp _self = this;

	@SuppressWarnings("unused")
	private DataBindingContext m_bindingContext;
	private Text _text;
	private Text _text_1;
	private SceneSettings _model = new SceneSettings();
	private Button _btnGenerateOnSave;
	private Button _colorButton;
	private ColorButtonSupport _colorSupport;
	private Text _text_2;
	private Text _text_3;
	private Button _btnEnbaleStepping;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	@SuppressWarnings("unused")
	public GeneralEditorSettingsComp(Composite parent, int style) {
		super(parent, style);

		this.setLayout(new GridLayout(3, false));

		Label lblScene = new Label(this, SWT.NONE);
		lblScene.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		lblScene.setText("Scene");
		lblScene.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));

		Label lblColor = new Label(this, SWT.NONE);
		lblColor.setText("Color");

		_colorButton = new Button(this, SWT.NONE);
		GridData gd_colorButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_colorButton.widthHint = 80;
		_colorButton.setLayoutData(gd_colorButton);
		_colorButton.setAlignment(SWT.LEFT);

		Button btnClear = new Button(this, SWT.NONE);
		btnClear.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnClear.setImage(ResourceManager.getPluginImage("org.eclipse.ui", "/icons/full/etool16/clear.png"));
		btnClear.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void widgetSelected(SelectionEvent e) {
				_colorSupport.clearColor();
				_colorSupport.updateContent();
			}
		});

		Label lblWidth = new Label(this, SWT.NONE);
		lblWidth.setText("Width");

		_text = new Text(this, SWT.BORDER);
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		Label lblHeight = new Label(this, SWT.NONE);
		lblHeight.setText("Height");

		_text_1 = new Text(this, SWT.BORDER);
		_text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);

		Label lblSnapping = new Label(this, SWT.NONE);
		lblSnapping.setFont(SWTResourceManager.getBoldFont(getFont()));
		lblSnapping.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		lblSnapping.setText("Snapping");

		_btnEnbaleStepping = new Button(this, SWT.CHECK);
		_btnEnbaleStepping.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		_btnEnbaleStepping.setText("Enbale Snapping");
		new Label(this, SWT.NONE);

		Label lblStepWidth = new Label(this, SWT.NONE);
		lblStepWidth.setText("Step Width");

		_text_2 = new Text(this, SWT.BORDER);
		_text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label lblStepHeight = new Label(this, SWT.NONE);
		lblStepHeight.setText("Step Height");

		_text_3 = new Text(this, SWT.BORDER);
		_text_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);

		Label lblCodeGeneration = new Label(this, SWT.NONE);
		lblCodeGeneration.setFont(SWTResourceManager.getBoldFont(getFont()));
		lblCodeGeneration.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblCodeGeneration.setText("Code Generation");
		new Label(this, SWT.NONE);

		_btnGenerateOnSave = new Button(this, SWT.CHECK);
		_btnGenerateOnSave.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		_btnGenerateOnSave.setText("Generate on Save");
		new Label(this, SWT.NONE);

		m_bindingContext = initDataBindings();

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		_colorSupport = ColorButtonSupport.createDefault(_colorButton, (color) -> {
			getModel().setSceneColor(color);
		});
		updateColorsButtons();
	}

	private void updateColorsButtons() {
		_colorSupport.setColor(_model.getSceneColor());
		_colorSupport.updateContent();
	}

	public SceneSettings getModel() {
		return _model;
	}

	public void setModel(SceneSettings model) {
		_model = model;
		updateColorsButtons();
		firePropertyChange("model");
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		support.firePropertyChange(property, true, false);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue modelsceneWidth_selfObserveValue = BeanProperties.value("model.sceneWidth").observe(_self);
		bindingContext.bindValue(observeText_textObserveWidget, modelsceneWidth_selfObserveValue, null, null);
		//
		IObservableValue observeText_text_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_1);
		IObservableValue modelsceneHeight_selfObserveValue = BeanProperties.value("model.sceneHeight").observe(_self);
		bindingContext.bindValue(observeText_text_1ObserveWidget, modelsceneHeight_selfObserveValue, null, null);
		//
		IObservableValue observeSelection_btnEnbaleSteppingObserveWidget = WidgetProperties.selection()
				.observe(_btnEnbaleStepping);
		IObservableValue modelenableStepping_selfObserveValue = BeanProperties.value("model.enableStepping")
				.observe(_self);
		bindingContext.bindValue(observeSelection_btnEnbaleSteppingObserveWidget, modelenableStepping_selfObserveValue,
				null, null);
		//
		IObservableValue observeText_text_2ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_2);
		IObservableValue modelstepWidth_selfObserveValue = BeanProperties.value("model.stepWidth").observe(_self);
		bindingContext.bindValue(observeText_text_2ObserveWidget, modelstepWidth_selfObserveValue, null, null);
		//
		IObservableValue observeText_text_3ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_3);
		IObservableValue modelstepHeight_selfObserveValue = BeanProperties.value("model.stepHeight").observe(_self);
		bindingContext.bindValue(observeText_text_3ObserveWidget, modelstepHeight_selfObserveValue, null, null);
		//
		IObservableValue observeSelection_btnGenerateOnSaveObserveWidget = WidgetProperties.selection()
				.observe(_btnGenerateOnSave);
		IObservableValue modelgenerateOnSave_selfObserveValue = BeanProperties.value("model.generateOnSave")
				.observe(_self);
		bindingContext.bindValue(observeSelection_btnGenerateOnSaveObserveWidget, modelgenerateOnSave_selfObserveValue,
				null, null);
		//
		return bindingContext;
	}
}
