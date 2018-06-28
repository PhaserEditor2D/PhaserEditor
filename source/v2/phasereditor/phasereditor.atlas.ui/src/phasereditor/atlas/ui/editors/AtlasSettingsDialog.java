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
	private Button _btnAlias;
	private Button _btnUseIndexes;
	private Button _btnGrid;
	private Button _btnDebug;
	private Button _btnMultiatlas;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public AtlasSettingsDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
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
	@SuppressWarnings("unused")
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, true));

		Label lblHoverTheMouse = new Label(container, SWT.NONE);
		lblHoverTheMouse.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblHoverTheMouse.setText("Hover the mouse on the parameter's label to open the tool-tip.");

		Group grpMinSize = new Group(container, SWT.NONE);
		grpMinSize.setLayout(new GridLayout(2, false));
		grpMinSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		grpMinSize.setText("Layout");

		Label lblMinWidth = new Label(grpMinSize, SWT.NONE);
		lblMinWidth.setToolTipText("The minimum width of output pages.");
		lblMinWidth.setText("Min W");

		_text_1 = new Text(grpMinSize, SWT.BORDER);
		_text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label lblMinHeight = new Label(grpMinSize, SWT.NONE);
		lblMinHeight.setToolTipText("The minimum height of output pages.");
		lblMinHeight.setText("Min H");

		_text_2 = new Text(grpMinSize, SWT.BORDER);
		_text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label lblSizeMaxWidth = new Label(grpMinSize, SWT.NONE);
		lblSizeMaxWidth.setToolTipText(
				"The maximum width of output pages.\r\n1024 is safe for all devices.\r\nExtremely old devices may have degraded performance over 512.");
		lblSizeMaxWidth.setText("Max W");

		_text = new Text(grpMinSize, SWT.BORDER);
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label lblSizeMaxHeight = new Label(grpMinSize, SWT.NONE);
		lblSizeMaxHeight.setToolTipText(
				"The maximum height of output pages.\r\n1024 is safe for all devices.\r\nExtremely old devices may have degraded performance over 512.");
		lblSizeMaxHeight.setText("Max H");

		_text_3 = new Text(grpMinSize, SWT.BORDER);
		_text_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label lblConstraints = new Label(grpMinSize, SWT.NONE);
		lblConstraints.setToolTipText("If POT, output pages will have power of two dimensions.");
		lblConstraints.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblConstraints.setText("Constraints:");

		_sizeConstraintsCombo = new Combo(grpMinSize, SWT.READ_ONLY);
		_sizeConstraintsCombo.setToolTipText("");
		_sizeConstraintsCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		_sizeConstraintsCombo.setItems(new String[] { "Any Size", "POT (Power of 2)" });

		Group grpPadding = new Group(container, SWT.NONE);
		grpPadding.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		grpPadding.setText("Sprites");
		grpPadding.setLayout(new GridLayout(2, false));

		Label lblPaddingX = new Label(grpPadding, SWT.NONE);
		lblPaddingX.setToolTipText("The number of pixels between packed images on the x-axis.");
		lblPaddingX.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPaddingX.setText("Padding X");

		_text_4 = new Text(grpPadding, SWT.BORDER);
		_text_4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label lblPaddingY = new Label(grpPadding, SWT.NONE);
		lblPaddingY.setToolTipText("The number of pixels between packed images on the y-axis.");
		lblPaddingY.setText("Padding Y");

		_text_5 = new Text(grpPadding, SWT.BORDER);
		GridData gd_text_5 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_text_5.widthHint = 40;
		_text_5.setLayoutData(gd_text_5);

		_btnStripWhitespaceX = new Button(grpPadding, SWT.CHECK);
		_btnStripWhitespaceX.setToolTipText(
				"If true, blank pixels on the left and right edges of input images\r\nwill be removed. Applications must take special care to draw\r\nthese regions properly.");
		_btnStripWhitespaceX.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		_btnStripWhitespaceX.setText("Strip Whitespace X");

		_btnStripWhitespaceY = new Button(grpPadding, SWT.CHECK);
		_btnStripWhitespaceY.setToolTipText(
				"If true, blank pixels on the top and bottom edges of input images\r\nwill be removed. Applications must take special care to draw\r\nthese regions properly.");
		_btnStripWhitespaceY.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		_btnStripWhitespaceY.setText("Strip Whitespace Y");

		Group grpFlags = new Group(container, SWT.NONE);
		grpFlags.setLayout(new GridLayout(2, false));
		grpFlags.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		grpFlags.setText("Flags");

		_btnAlias = new Button(grpFlags, SWT.CHECK);
		_btnAlias.setToolTipText("If true, two images that are pixel for pixel the same will only be packed once.");
		_btnAlias.setText("Alias");

		_btnUseIndexes = new Button(grpFlags, SWT.CHECK);
		_btnUseIndexes.setToolTipText(
				"If true, images are sorted by parsing the sufix of the file names\r\n(eg. animation_01.png, animation_02.png, ...)");
		_btnUseIndexes.setText("Use Indexes");

		_btnGrid = new Button(grpFlags, SWT.CHECK);
		_btnGrid.setToolTipText("If true, images are packed in a uniform grid, in order.");
		_btnGrid.setText("Grid");

		_btnDebug = new Button(grpFlags, SWT.CHECK);
		_btnDebug.setToolTipText("If true, lines are drawn on the output pages\r\nto show the packed image bounds.");
		_btnDebug.setText("Debug");

		_btnMultiatlas = new Button(grpFlags, SWT.CHECK);
		_btnMultiatlas.setToolTipText("If true, generates the Phaser 3 multi-atlas format.");
		_btnMultiatlas.setText("Multi-atlas");

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
		return new Point(800, 600);
	}

	@SuppressWarnings("unchecked")
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue<?> observeText_text_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_1);
		IObservableValue<?> minWidth_settingsObserveValue = PojoProperties.value("minWidth").observe(_settings);
		bindingContext.bindValue(observeText_text_1ObserveWidget, minWidth_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeText_text_2ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_2);
		IObservableValue<?> minHeight_settingsObserveValue = PojoProperties.value("minHeight").observe(_settings);
		bindingContext.bindValue(observeText_text_2ObserveWidget, minHeight_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue<?> maxWidth_settingsObserveValue = PojoProperties.value("maxWidth").observe(_settings);
		bindingContext.bindValue(observeText_textObserveWidget, maxWidth_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeText_text_3ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_3);
		IObservableValue<?> maxHeight_settingsObserveValue = PojoProperties.value("maxHeight").observe(_settings);
		bindingContext.bindValue(observeText_text_3ObserveWidget, maxHeight_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeSelection_btnStripWhitespaceXObserveWidget = WidgetProperties.selection()
				.observe(_btnStripWhitespaceX);
		IObservableValue<?> stripWhitespaceX_settingsObserveValue = PojoProperties.value("stripWhitespaceX")
				.observe(_settings);
		bindingContext.bindValue(observeSelection_btnStripWhitespaceXObserveWidget,
				stripWhitespaceX_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeSelection_btnStripWhitespaceYObserveWidget = WidgetProperties.selection()
				.observe(_btnStripWhitespaceY);
		IObservableValue<?> stripWhitespaceY_settingsObserveValue = PojoProperties.value("stripWhitespaceY")
				.observe(_settings);
		bindingContext.bindValue(observeSelection_btnStripWhitespaceYObserveWidget,
				stripWhitespaceY_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeSelection_btnAliasObserveWidget = WidgetProperties.selection().observe(_btnAlias);
		IObservableValue<?> alias_settingsObserveValue = PojoProperties.value("alias").observe(_settings);
		bindingContext.bindValue(observeSelection_btnAliasObserveWidget, alias_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeSelection_btnUseIndexesObserveWidget = WidgetProperties.selection()
				.observe(_btnUseIndexes);
		IObservableValue<?> useIndexes_settingsObserveValue = PojoProperties.value("useIndexes").observe(_settings);
		bindingContext.bindValue(observeSelection_btnUseIndexesObserveWidget, useIndexes_settingsObserveValue, null,
				null);
		//
		IObservableValue<?> observeSelection_btnGridObserveWidget = WidgetProperties.selection().observe(_btnGrid);
		IObservableValue<?> grid_settingsObserveValue = PojoProperties.value("grid").observe(_settings);
		bindingContext.bindValue(observeSelection_btnGridObserveWidget, grid_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeSelection_btnDebugObserveWidget = WidgetProperties.selection().observe(_btnDebug);
		IObservableValue<?> debug_settingsObserveValue = PojoProperties.value("debug").observe(_settings);
		bindingContext.bindValue(observeSelection_btnDebugObserveWidget, debug_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeSelection_btnMultiatlasObserveWidget = WidgetProperties.selection()
				.observe(_btnMultiatlas);
		IObservableValue<?> multiatlas_settingsObserveValue = PojoProperties.value("multiatlas").observe(_settings);
		bindingContext.bindValue(observeSelection_btnMultiatlasObserveWidget, multiatlas_settingsObserveValue, null,
				null);
		//
		IObservableValue<?> observeText_text_4ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_4);
		IObservableValue<?> paddingX_settingsObserveValue = PojoProperties.value("paddingX").observe(_settings);
		bindingContext.bindValue(observeText_text_4ObserveWidget, paddingX_settingsObserveValue, null, null);
		//
		IObservableValue<?> observeText_text_5ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_5);
		IObservableValue<?> paddingY_settingsObserveValue = PojoProperties.value("paddingY").observe(_settings);
		bindingContext.bindValue(observeText_text_5ObserveWidget, paddingY_settingsObserveValue, null, null);
		//
		return bindingContext;
	}
}
