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
package phasereditor.atlas.ui.editors;

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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import phasereditor.atlas.core.SettingsBean;

public class AtlasSettingsDialog extends Dialog {
	private static final int DEFAULT_BTN = 1959;
	private DataBindingContext m_bindingContext;
	private Text _text;
	private Text _text_1;
	private Text _text_2;
	private Text _text_3;
	private SettingsBean _settings;
	private Combo _sizeConstraintsCombo;
	private Text _text_4;
	private Text _text_5;
	private Button _btnStripWhitespaceX;
	private Button _btnStripWhitespaceY;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public AtlasSettingsDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Atlas Settings");
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, true));

		Group grpMinSize = new Group(container, SWT.NONE);
		grpMinSize.setLayout(new GridLayout(2, false));
		grpMinSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpMinSize.setText("Min Size");

		Label lblMinWidth = new Label(grpMinSize, SWT.NONE);
		GridData gd_lblMinWidth = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblMinWidth.widthHint = 120;
		lblMinWidth.setLayoutData(gd_lblMinWidth);
		lblMinWidth.setText("Min Width");

		_text_1 = new Text(grpMinSize, SWT.BORDER);
		_text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblMinHeight = new Label(grpMinSize, SWT.NONE);
		lblMinHeight.setText("Min Height");

		_text_2 = new Text(grpMinSize, SWT.BORDER);
		_text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Group grpMaxSize = new Group(container, SWT.NONE);
		grpMaxSize.setLayout(new GridLayout(2, false));
		grpMaxSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpMaxSize.setText("Max Size");

		Label lblSizeMaxWidth = new Label(grpMaxSize, SWT.NONE);
		GridData gd_lblSizeMaxWidth = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblSizeMaxWidth.widthHint = 120;
		lblSizeMaxWidth.setLayoutData(gd_lblSizeMaxWidth);
		lblSizeMaxWidth.setText("Max Width");

		_text = new Text(grpMaxSize, SWT.BORDER);
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSizeMaxHeight = new Label(grpMaxSize, SWT.NONE);
		lblSizeMaxHeight.setText("Max Height");

		_text_3 = new Text(grpMaxSize, SWT.BORDER);
		_text_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Group grpOther = new Group(container, SWT.NONE);
		grpOther.setText("Size Constraints");
		grpOther.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		grpOther.setLayout(new GridLayout(1, false));

		_sizeConstraintsCombo = new Combo(grpOther, SWT.READ_ONLY);
		GridData gd_sizeConstraintsCombo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_sizeConstraintsCombo.widthHint = 220;
		_sizeConstraintsCombo.setLayoutData(gd_sizeConstraintsCombo);
		_sizeConstraintsCombo.setItems(new String[] { "Any Size", "POT (Power of 2)" });

		Group grpPadding = new Group(container, SWT.NONE);
		grpPadding.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpPadding.setText("Padding");
		grpPadding.setLayout(new GridLayout(2, false));

		Label lblPaddingX = new Label(grpPadding, SWT.NONE);
		GridData gd_lblPaddingX = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_lblPaddingX.widthHint = 120;
		lblPaddingX.setLayoutData(gd_lblPaddingX);
		lblPaddingX.setText("Padding X");

		_text_4 = new Text(grpPadding, SWT.BORDER);
		_text_4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblPaddingY = new Label(grpPadding, SWT.NONE);
		lblPaddingY.setText("Padding Y");

		_text_5 = new Text(grpPadding, SWT.BORDER);
		_text_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Group grpStrip = new Group(container, SWT.NONE);
		grpStrip.setLayout(new GridLayout(1, false));
		grpStrip.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		grpStrip.setText("Strip");

		_btnStripWhitespaceX = new Button(grpStrip, SWT.CHECK);
		_btnStripWhitespaceX.setText("Strip Whitespace X");

		_btnStripWhitespaceY = new Button(grpStrip, SWT.CHECK);
		_btnStripWhitespaceY.setText("Strip Whitespace Y");

		afterCreateWidgets();

		return container;
	}

	private void afterCreateWidgets() {
		_sizeConstraintsCombo.select(_settings.pot ? 1 : 0);
	}

	@Override
	protected void okPressed() {
		_settings.pot = _sizeConstraintsCombo.getSelectionIndex() == 1;
		super.okPressed();
	}

	public SettingsBean getSettings() {
		return _settings;
	}

	public void setSettings(SettingsBean settings) {
		_settings = settings;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, DEFAULT_BTN, "Restore Defaults", false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		m_bindingContext = initDataBindings();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == DEFAULT_BTN) {
			restoreDefaults();
		}
	}

	private void restoreDefaults() {
		_settings.update(new SettingsBean());
		m_bindingContext.updateTargets();
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(539, 401);
	}

	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeText_text_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_1);
		IObservableValue minWidth_settingsObserveValue = PojoProperties.value("minWidth").observe(_settings);
		bindingContext.bindValue(observeText_text_1ObserveWidget, minWidth_settingsObserveValue, null, null);
		//
		IObservableValue observeText_text_2ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_2);
		IObservableValue minHeight_settingsObserveValue = PojoProperties.value("minHeight").observe(_settings);
		bindingContext.bindValue(observeText_text_2ObserveWidget, minHeight_settingsObserveValue, null, null);
		//
		IObservableValue observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue maxWidth_settingsObserveValue = PojoProperties.value("maxWidth").observe(_settings);
		bindingContext.bindValue(observeText_textObserveWidget, maxWidth_settingsObserveValue, null, null);
		//
		IObservableValue observeText_text_3ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_3);
		IObservableValue maxHeight_settingsObserveValue = PojoProperties.value("maxHeight").observe(_settings);
		bindingContext.bindValue(observeText_text_3ObserveWidget, maxHeight_settingsObserveValue, null, null);
		//
		IObservableValue observeText_text_4ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_4);
		IObservableValue paddingX_settingsObserveValue = PojoProperties.value("paddingX").observe(_settings);
		bindingContext.bindValue(observeText_text_4ObserveWidget, paddingX_settingsObserveValue, null, null);
		//
		IObservableValue observeText_text_5ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_5);
		IObservableValue paddingY_settingsObserveValue = PojoProperties.value("paddingY").observe(_settings);
		bindingContext.bindValue(observeText_text_5ObserveWidget, paddingY_settingsObserveValue, null, null);
		//
		IObservableValue observeSelection_btnStripWhitespaceXObserveWidget = WidgetProperties.selection()
				.observe(_btnStripWhitespaceX);
		IObservableValue stripWhitespaceX_settingsObserveValue = PojoProperties.value("stripWhitespaceX")
				.observe(_settings);
		bindingContext.bindValue(observeSelection_btnStripWhitespaceXObserveWidget,
				stripWhitespaceX_settingsObserveValue, null, null);
		//
		IObservableValue observeSelection_btnStripWhitespaceYObserveWidget = WidgetProperties.selection()
				.observe(_btnStripWhitespaceY);
		IObservableValue stripWhitespaceY_settingsObserveValue = PojoProperties.value("stripWhitespaceY")
				.observe(_settings);
		bindingContext.bindValue(observeSelection_btnStripWhitespaceYObserveWidget,
				stripWhitespaceY_settingsObserveValue, null, null);
		//
		return bindingContext;
	}
}
