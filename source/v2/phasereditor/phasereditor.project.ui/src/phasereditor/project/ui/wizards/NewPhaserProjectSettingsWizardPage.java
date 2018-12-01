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
import org.eclipse.jface.viewers.LabelProvider;
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

import phasereditor.project.core.codegen.SourceLang;
import phasereditor.project.ui.ProjectUI;

/**
 * @author arian
 *
 */
public class NewPhaserProjectSettingsWizardPage extends WizardPage {
	private static class ViewerLabelProvider extends LabelProvider {
		public ViewerLabelProvider() {
		}

		@Override
		public String getText(Object element) {
			return ((SourceLang) element).getDisplayName();
		}
	}

	private Text _widthText;
	private Text _heightText;
	private Combo _typeCombo;
	private Button _pixelArtBtn;
	private Button _arcadeBtn;
	private Button _matterBtn;
	private Label _label;
	private ComboViewer _comboLang;

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
		grpGame.setText("Game Parameters");

		Label lblWidth = new Label(grpGame, SWT.NONE);
		lblWidth.setText("width:");

		_widthText = new Text(grpGame, SWT.BORDER);
		_widthText.setText("800");
		_widthText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblHeight = new Label(grpGame, SWT.NONE);
		lblHeight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblHeight.setText("height:");

		_heightText = new Text(grpGame, SWT.BORDER);
		_heightText.setText("600");
		_heightText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblRenderer = new Label(grpGame, SWT.NONE);
		lblRenderer.setText("type:");

		_typeCombo = new Combo(grpGame, SWT.READ_ONLY);
		_typeCombo.setItems(new String[] { "AUTO", "WEBGL", "CANVAS", "HEADLESS" });
		_typeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		_typeCombo.select(0);

		_pixelArtBtn = new Button(grpGame, SWT.CHECK);
		_pixelArtBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		_pixelArtBtn.setSize(79, 20);
		_pixelArtBtn.setText("pixelArt");

		Label lblPhysicsconfig = new Label(grpGame, SWT.NONE);
		lblPhysicsconfig.setText("physicsConfig:");

		var physicsGroup = new Group(grpGame, SWT.NONE);
		physicsGroup.setLayout(new GridLayout(5, false));
		physicsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));


		_arcadeBtn = new Button(physicsGroup, SWT.RADIO);
		_arcadeBtn.setText("arcade");

		_matterBtn = new Button(physicsGroup, SWT.RADIO);
		_matterBtn.setText("matter");

		Group codeGroup = new Group(container, SWT.NONE);
		codeGroup.setLayout(new GridLayout(2, false));
		codeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		codeGroup.setText("Code");

		_label = new Label(codeGroup, SWT.NONE);
		_label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		_label.setText("Language: ");

		_comboLang = new ComboViewer(codeGroup, SWT.READ_ONLY);
		Combo combo = _comboLang.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_comboLang.setContentProvider(new ArrayContentProvider());
		_comboLang.setLabelProvider(new ViewerLabelProvider());

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		IPreferenceStore store = ProjectUI.getPreferenceStore();

		_widthText.setText(store.getString(ProjectUI.PREF_PROP_PROJECT_WIZARD_GAME_WIDTH));
		_heightText.setText(store.getString(ProjectUI.PREF_PROP_PROJECT_WIZARD_GAME_HEIGHT));

		_comboLang.setInput(new Object[] { SourceLang.JAVA_SCRIPT_6 });
		_comboLang.setSelection(new StructuredSelection(
				SourceLang.valueOf(store.getString(ProjectUI.PREF_PROP_PROJECT_WIZARD_LANGUAJE))));
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

	public Button getArcadeBtn() {
		return _arcadeBtn;
	}

	public Button getMatterBtn() {
		return _matterBtn;
	}

	public SourceLang getSourceLang() {
		return (SourceLang) _comboLang.getStructuredSelection().getFirstElement();
	}
}
