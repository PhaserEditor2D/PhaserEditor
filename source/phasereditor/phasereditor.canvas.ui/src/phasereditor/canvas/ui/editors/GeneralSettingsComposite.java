// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;

import phasereditor.canvas.core.EditorSettings;

import org.eclipse.swt.layout.GridData;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.core.databinding.beans.PojoProperties;

/**
 * @author arian
 *
 */
public class GeneralSettingsComposite extends Composite {
	@SuppressWarnings("unused")
	private DataBindingContext m_bindingContext;
	private Text _text;
	private Text _text_1;

	private EditorSettings _model = new EditorSettings();

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public GeneralSettingsComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Group grpSnapping = new Group(this, SWT.NONE);
		grpSnapping.setText("Snapping");
		grpSnapping.setLayout(new GridLayout(2, false));

		_btnEnableSnapping = new Button(grpSnapping, SWT.CHECK);
		_btnEnableSnapping.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		_btnEnableSnapping.setText("Enable Snapping");

		Label lblStepWidth = new Label(grpSnapping, SWT.NONE);
		lblStepWidth.setText("Step Width");

		_text = new Text(grpSnapping, SWT.BORDER);
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblStepHeight = new Label(grpSnapping, SWT.NONE);
		lblStepHeight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblStepHeight.setText("Step Height");

		_text_1 = new Text(grpSnapping, SWT.BORDER);
		_text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		m_bindingContext = initDataBindings();

	}

	/**
	 * @return the model
	 */
	public EditorSettings getModel() {
		return _model;
	}

	/**
	 * @param model
	 *            the model to set
	 */
	public void setModel(EditorSettings model) {
		_model = model;
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private Button _btnEnableSnapping;

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

	@SuppressWarnings("all")
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeSelection_btnEnableSnappingObserveWidget = WidgetProperties.selection()
				.observe(_btnEnableSnapping);
		IObservableValue enableStepping_modelObserveValue = PojoProperties.value("enableStepping").observe(_model);
		bindingContext.bindValue(observeSelection_btnEnableSnappingObserveWidget, enableStepping_modelObserveValue,
				null, null);
		//
		IObservableValue observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue stepWidth_modelObserveValue = PojoProperties.value("stepWidth").observe(_model);
		bindingContext.bindValue(observeText_textObserveWidget, stepWidth_modelObserveValue, null, null);
		//
		IObservableValue observeText_text_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_1);
		IObservableValue stepHeight_modelObserveValue = PojoProperties.value("stepHeight").observe(_model);
		bindingContext.bindValue(observeText_text_1ObserveWidget, stepHeight_modelObserveValue, null, null);
		//
		return bindingContext;
	}
}
