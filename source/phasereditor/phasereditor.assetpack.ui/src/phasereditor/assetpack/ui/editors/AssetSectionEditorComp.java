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
package phasereditor.assetpack.ui.editors;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.assetpack.core.AssetSectionModel;

public class AssetSectionEditorComp extends Composite {
	private DataBindingContext m_bindingContext;

	private Text _text;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public AssetSectionEditorComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Label lblSetTheProperties = new Label(this, SWT.NONE);
		lblSetTheProperties.setText("Set the properties of the 'section'. Required fields are denoted by '*'.");
		lblSetTheProperties.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Composite composite = new Composite(this, SWT.NONE);
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.marginWidth = 0;
		composite.setLayout(gl_composite);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		Label lblKey = new Label(composite, SWT.NONE);
		lblKey.setText("key*");
		lblKey.setToolTipText(
				"The key you use for the sections is entirely up to you and is passed to the Phaser.Loader.pack call.");

		_text = new Text(composite, SWT.BORDER);
		_text.setText("");
		GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_text.horizontalIndent = 5;
		gd_text.widthHint = 200;
		_text.setLayoutData(gd_text);

		Label lblHelp = new Label(this, SWT.NONE);
		lblHelp.setText("Help");

		Label lblSectionsAreA = new Label(this, SWT.WRAP);
		lblSectionsAreA.setText(
				"Sections are a way for you to control the splitting-up of asset loading, so you don't have to load everything at once.");
		GridData gd_lblSectionsAreA = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_lblSectionsAreA.widthHint = 200;
		lblSectionsAreA.setLayoutData(gd_lblSectionsAreA);
		m_bindingContext = initDataBindings();

	}

	private AssetSectionEditorComp _self = this;
	private AssetSectionModel _model;
	private boolean _firstTime = true;

	private void decorateControls() {
		IObservableList bindings = m_bindingContext.getBindings();
		for (int i = 0; i < bindings.size(); i++) {
			Binding b = (Binding) bindings.get(i);
			ControlDecorationSupport.create(b, SWT.TOP | SWT.LEFT);
		}
	}

	private void validateModelToTarget() {
		IObservableList bindings = m_bindingContext.getBindings();
		for (int i = 0; i < bindings.size(); i++) {
			Binding b = (Binding) bindings.get(i);
			b.validateTargetToModel();
		}
	}

	public AssetSectionModel getModel() {
		return _model;
	}

	public void setModel(AssetSectionModel model) {
		_model = model;
		firePropertyChange("model");

		if (_firstTime) {
			_firstTime = false;
			decorateControls();
		}

		validateModelToTarget();
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

	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue modelkey_selfObserveValue = BeanProperties.value("model.key").observe(_self);
		UpdateValueStrategy strategy = new UpdateValueStrategy();
		strategy.setBeforeSetValidator(new RequiredValidator());
		bindingContext.bindValue(observeText_textObserveWidget, modelkey_selfObserveValue, strategy, null);
		//
		return bindingContext;
	}
}
