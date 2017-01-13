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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.PhysicsType;
import phasereditor.canvas.core.SourceLang;
import phasereditor.canvas.core.StateSettings;
import phasereditor.canvas.ui.editors.LangLabelProvider;
import phasereditor.ui.ColorButtonSupport;

/**
 * @author arian
 *
 */
public class NewPage_StateSettings extends WizardPage {
	@SuppressWarnings("unused")
	private DataBindingContext m_bindingContext;
	private ComboViewer _langComboViewer;
	private Text _text;
	private EditorSettings _settings;
	private StateSettings _stateSettings;
	private ColorButtonSupport _bgColorSupport;

	public NewPage_StateSettings() {
		super("group.settings.page");
		setMessage("Customize the new Phaser State.");
		setTitle("State Customization");
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
		_text.setText("Phaser.State");
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblCodeFormat = new Label(container, SWT.NONE);
		lblCodeFormat.setText("Code Format");

		_langComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		Combo combo = _langComboViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblScaleMode = new Label(container, SWT.NONE);
		lblScaleMode.setText("Scale Mode");

		_scaleModeViewer = new ComboViewer(container, SWT.READ_ONLY);
		Combo combo_1 = _scaleModeViewer.getCombo();
		combo_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_scaleModeViewer.setContentProvider(new ArrayContentProvider());
		_scaleModeViewer.setLabelProvider(new LabelProvider());
		new Label(container, SWT.NONE);

		_btnPageAlignHorizontally = new Button(container, SWT.CHECK);
		_btnPageAlignHorizontally.setText("Page Align Horizontally");
		new Label(container, SWT.NONE);

		_btnPageAlignVertically = new Button(container, SWT.CHECK);
		_btnPageAlignVertically.setText("Page Align Vertically");

		Label lblBackgroundColor = new Label(container, SWT.NONE);
		lblBackgroundColor.setText("Background Color");

		Composite composite_1 = new Composite(container, SWT.NONE);
		GridLayout gl_composite_1 = new GridLayout(1, false);
		gl_composite_1.marginWidth = 0;
		gl_composite_1.marginHeight = 0;
		composite_1.setLayout(gl_composite_1);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		_btnColor = new Button(composite_1, SWT.NONE);
		_btnColor.setText("--- #888888");

		Label lblPhysicsSystem = new Label(container, SWT.NONE);
		lblPhysicsSystem.setText("Physics System");

		_physicsSystemViewer = new ComboViewer(container, SWT.READ_ONLY);
		Combo combo_2 = _physicsSystemViewer.getCombo();
		combo_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_physicsSystemViewer.setLabelProvider(new LabelProvider());
		_physicsSystemViewer.setContentProvider(new ArrayContentProvider());

		_btnRendererRoundPixels = new Button(container, SWT.CHECK);
		_btnRendererRoundPixels.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		_btnRendererRoundPixels.setText("Renderer Round Pixels");

		_btnGenerateTheCorrspondant = new Button(container, SWT.CHECK);
		_btnGenerateTheCorrspondant.setSelection(true);
		_btnGenerateTheCorrspondant.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, true, 2, 1));
		_btnGenerateTheCorrspondant.setText("Edit this state with the visual editor.");
		_langComboViewer.setContentProvider(new ArrayContentProvider());
		_langComboViewer.setLabelProvider(new LangLabelProvider());

		afterCreateWidgets();

		m_bindingContext = initDataBindings();
	}

	public boolean isGenerateCanvasFile() {
		return _btnGenerateTheCorrspondant.getSelection();
	}

	private void afterCreateWidgets() {
		_langComboViewer.setInput(SourceLang.values());
		_scaleModeViewer.setInput(StateSettings.SCALE_MODES);

		_physicsSystemViewer.setInput(PhysicsType.values());

		_bgColorSupport = ColorButtonSupport.createDefault(_btnColor, color -> {
			// nothing for now
		});
		_bgColorSupport.setColor(_stateSettings.getStageBackgroundColor());
		_bgColorSupport.updateContent();
	}

	private NewPage_StateSettings _self = this;

	public EditorSettings getSettings() {
		return _settings;
	}

	public void setSettings(EditorSettings settings) {
		_settings = settings;
		firePropertyChange("settings");
	}

	public StateSettings getStateSettings() {
		return _stateSettings;
	}

	public void setStateSettings(StateSettings stateSettings) {
		_stateSettings = stateSettings;
		firePropertyChange("stateSettings");
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private ComboViewer _scaleModeViewer;
	private Button _btnColor;
	private ComboViewer _physicsSystemViewer;
	private Button _btnPageAlignHorizontally;
	private Button _btnPageAlignVertically;
	private Button _btnRendererRoundPixels;
	private Button _btnGenerateTheCorrspondant;

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
		IObservableValue observeSingleSelection_scaleModeViewer = ViewerProperties.singleSelection()
				.observe(_scaleModeViewer);
		IObservableValue stateSettingsscaleMode_selfObserveValue = BeanProperties.value("stateSettings.scaleMode")
				.observe(_self);
		bindingContext.bindValue(observeSingleSelection_scaleModeViewer, stateSettingsscaleMode_selfObserveValue, null,
				null);
		//
		IObservableValue observeSelection_btnPageAlignHorizontallyObserveWidget = WidgetProperties.selection()
				.observe(_btnPageAlignHorizontally);
		IObservableValue stateSettingspageAlignHorizontally_selfObserveValue = BeanProperties
				.value("stateSettings.pageAlignHorizontally").observe(_self);
		bindingContext.bindValue(observeSelection_btnPageAlignHorizontallyObserveWidget,
				stateSettingspageAlignHorizontally_selfObserveValue, null, null);
		//
		IObservableValue observeSelection_btnPageAlignVerticallyObserveWidget = WidgetProperties.selection()
				.observe(_btnPageAlignVertically);
		IObservableValue stateSettingspageAlignVertically_selfObserveValue = BeanProperties
				.value("stateSettings.pageAlignVertically").observe(_self);
		bindingContext.bindValue(observeSelection_btnPageAlignVerticallyObserveWidget,
				stateSettingspageAlignVertically_selfObserveValue, null, null);
		//
		IObservableValue observeSingleSelection_physicsSystemViewer = ViewerProperties.singleSelection()
				.observe(_physicsSystemViewer);
		IObservableValue stateSettingsphysicsSystem_selfObserveValue = BeanProperties
				.value("stateSettings.physicsSystem").observe(_self);
		bindingContext.bindValue(observeSingleSelection_physicsSystemViewer,
				stateSettingsphysicsSystem_selfObserveValue, null, null);
		//
		IObservableValue observeSelection_btnRendererRoundPixelsObserveWidget = WidgetProperties.selection()
				.observe(_btnRendererRoundPixels);
		IObservableValue stateSettingsrendererRoundPixels_selfObserveValue = BeanProperties
				.value("stateSettings.rendererRoundPixels").observe(_self);
		bindingContext.bindValue(observeSelection_btnRendererRoundPixelsObserveWidget,
				stateSettingsrendererRoundPixels_selfObserveValue, null, null);
		//
		return bindingContext;
	}
}
