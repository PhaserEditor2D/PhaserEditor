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
package phasereditor.project.ui.wizards;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

/**
 * @author arian
 *
 */
public class NewPhaserProjectSettingsWizardPage extends WizardPage {
	private Text _widthText;
	private Text _heightText;
	private Combo _typeCombo;
	private Button _pixelArtBtn;
	private Label _label;
	private ComboViewer _comboLang;
	private Combo _physicsCombo;
	private Combo _scaleModeCombo;
	private Combo _scaleAutoCenterCombo;

	/**
	 * Create the wizard.
	 */
	public NewPhaserProjectSettingsWizardPage() {
		super("wizardPage");
		setTitle("New Phaser Project");
		setDescription("Set the project parameters");
	}

	/**
	 * Create contents of the wizard.
	 * 
	 * @param parent
	 */
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);

		setControl(container);
		container.setLayout(new GridLayout(1, false));

		Group grpGame = new Group(container, SWT.NONE);
		grpGame.setLayout(new GridLayout(4, false));
		grpGame.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		grpGame.setText("Game Config");

		Label lblWidth = new Label(grpGame, SWT.NONE);
		lblWidth.setText("Width");

		_widthText = new Text(grpGame, SWT.BORDER);
		_widthText.setText("800");
		_widthText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblHeight = new Label(grpGame, SWT.NONE);
		lblHeight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblHeight.setText("Height");

		_heightText = new Text(grpGame, SWT.BORDER);
		_heightText.setText("600");
		_heightText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblRenderer = new Label(grpGame, SWT.NONE);
		lblRenderer.setText("Type");

		_typeCombo = new Combo(grpGame, SWT.READ_ONLY);
		_typeCombo.setItems(new String[] { "Phaser.AUTO", "Phaser.WEBGL", "Phaser.CANVAS", "Phaser.HEADLESS" });
		_typeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		_typeCombo.select(0);

		{
			var label = new Label(grpGame, 0);
			label.setText("Pixel Art");
			_pixelArtBtn = new Button(grpGame, SWT.CHECK);
			_pixelArtBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
			_pixelArtBtn.setSize(79, 20);
		}

		{
			var label = new Label(grpGame, SWT.NONE);
			label.setText("Physics");

			var combo = new Combo(grpGame, SWT.READ_ONLY);
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
			combo.setItems(new String[] {

					"Arcade",

					"Matter",

					"None"

			});
			combo.select(2);
			_physicsCombo = combo;
		}

		{
			var label = new Label(grpGame, SWT.NONE);
			label.setText("Scale Mode");

			var combo = new Combo(grpGame, SWT.READ_ONLY);
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
			combo.setItems(new String[] {

					"Phaser.Scale.FIT",

					"Phaser.Scale.ENVELOP",

					"Phaser.Scale.HEIGHT_CONTROLS_WIDTH",

					"Phaser.Scale.WIDTH_CONTROLS_HEIGHT",

					"Phaser.Scale.RESIZE",

					"Phaser.Scale.NONE"

			});
			combo.select(0);
			_scaleModeCombo = combo;
		}

		{
			var label = new Label(grpGame, SWT.NONE);
			label.setText("Scale Auto Center");

			var combo = new Combo(grpGame, SWT.READ_ONLY);
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
			combo.setItems(new String[] {

					"Phaser.Scale.CENTER_BOTH",

					"Phaser.Scale.CENTER_HORIZONTALLY",

					"Phaser.Scale.CENTER_VERTICALLY",

					"Phaser.Scale.NO_CENTER"

			});
			combo.select(0);
			_scaleAutoCenterCombo = combo;
		}

		Group codeGroup = new Group(container, SWT.NONE);
		codeGroup.setLayout(new GridLayout(2, false));
		codeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		codeGroup.setText("Code");

		_label = new Label(codeGroup, SWT.NONE);
		_label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		_label.setText("Language");

		_comboLang = new ComboViewer(codeGroup, SWT.READ_ONLY);
		Combo combo = _comboLang.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_comboLang.setContentProvider(new ArrayContentProvider());
		_comboLang.setLabelProvider(new SourceLangLabelProvider());

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		IPreferenceStore store = ProjectCore.getPreferenceStore();

		_widthText.setText(store.getString(ProjectCore.PREF_PROP_PROJECT_GAME_WIDTH));
		_heightText.setText(store.getString(ProjectCore.PREF_PROP_PROJECT_GAME_HEIGHT));

		_comboLang.setInput(new Object[] { SourceLang.JAVA_SCRIPT_6, SourceLang.TYPE_SCRIPT });
		_comboLang.setSelection(new StructuredSelection(ProjectCore.getDefaultProjectLanguage()));
	}

	public void setFocus() {
		_widthText.setFocus();
	}

	public Text getWidthText() {
		return _widthText;
	}

	public Text getHeightText() {
		return _heightText;
	}

	public Combo getTypeCombo() {
		return _typeCombo;
	}

	public Button getPixelArtBtn() {
		return _pixelArtBtn;
	}

	public String getPhysics() {
		var text = _physicsCombo.getText();
		if ("None".equals(text)) {
			return null;
		}
		return text.toLowerCase();
	}

	public String getScaleMode() {
		var text = _scaleModeCombo.getText();
		if (text.contains("NONE")) {
			return null;
		}
		return text;
	}

	public String getScaleAutoCenter() {
		var text = _scaleAutoCenterCombo.getText();
		if (text.contains("NO_CENTER")) {
			return null;
		}
		return text;
	}

	public SourceLang getSourceLang() {
		return (SourceLang) _comboLang.getStructuredSelection().getFirstElement();
	}
}
