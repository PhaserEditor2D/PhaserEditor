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
package phasereditor.canvas.ui.wizards;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.SourceLang;
import phasereditor.canvas.ui.editors.LangLabelProvider;

/**
 * @author arian
 *
 */
public class NewPage_GroupSettings extends WizardPage {
	@SuppressWarnings("unused")
	private DataBindingContext m_bindingContext;
	private ComboViewer _langComboViewer;
	private Text _text;
	private EditorSettings _settings;

	public NewPage_GroupSettings() {
		super("group.settings.page");
		setMessage("Customize the new Phaser Group.");
		setTitle("Group Customization");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.
	 * widgets.Composite)
	 */
	@SuppressWarnings("all")
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(2, false));

		Label lblBaseClassName = new Label(container, SWT.NONE);
		lblBaseClassName.setText("Base Class Name");

		_text = new Text(container, SWT.BORDER);
		_text.setText("Phaser.Group");
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblCodeFormat = new Label(container, SWT.NONE);
		lblCodeFormat.setText("Code Format");

		_langComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		Combo combo = _langComboViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_langComboViewer.setContentProvider(new ArrayContentProvider());
		_langComboViewer.setLabelProvider(new LangLabelProvider());

		afterCreateWidgets();

		m_bindingContext = initDataBindings();
	}

	private void afterCreateWidgets() {
		_langComboViewer.setInput(SourceLang.values());
	}

	private NewPage_GroupSettings _self = this;

	/**
	 * @return the settings
	 */
	public EditorSettings getSettings() {
		return _settings;
	}

	/**
	 * @param settings
	 *            the settings to set
	 */
	public void setSettings(EditorSettings settings) {
		_settings = settings;
		firePropertyChange("settings");
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

	@SuppressWarnings("all")
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue settingsbaseClass_selfObserveValue = BeanProperties.value("settings.baseClass").observe(_self);
		bindingContext.bindValue(observeText_textObserveWidget, settingsbaseClass_selfObserveValue, null, null);
		//
		IObservableValue observeSingleSelection_langComboViewer = ViewerProperties.singleSelection()
				.observe(_langComboViewer);
		IObservableValue settingslang_selfObserveValue = BeanProperties.value("settings.lang").observe(_self);
		bindingContext.bindValue(observeSingleSelection_langComboViewer, settingslang_selfObserveValue, null, null);
		//
		return bindingContext;
	}
}
