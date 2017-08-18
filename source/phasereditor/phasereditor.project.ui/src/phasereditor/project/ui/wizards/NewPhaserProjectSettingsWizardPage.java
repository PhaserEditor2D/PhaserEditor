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
			return ((SourceLang)element).getDisplayName();
		}
	}

	private Text _widthText;
	private Text _heightText;
	private Button _transparentBtn;
	private Combo _rendererCombo;
	private Button _antialiasBtn;
	private Button _arcadeBtn;
	private Button _ninjaBtn;
	private Button _p2Btn;
	private Button _box2dBtn;
	private Button _matterBtn;
	private Button _simplestBtn;
	private Button _singleStateBtn;
	private Button _multipleStatesBtn;
	private Button _includeAssets;
	private Group _group;
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
		lblRenderer.setText("renderer:");

		_rendererCombo = new Combo(grpGame, SWT.READ_ONLY);
		_rendererCombo.setItems(new String[] { "Phaser.AUTO", "Phaser.WEBGL", "Phaser.CANVAS", "Phaser.HEADLESS" });
		_rendererCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		_rendererCombo.select(0);

		_transparentBtn = new Button(grpGame, SWT.CHECK);
		_transparentBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		_transparentBtn.setText("transparent");

		_antialiasBtn = new Button(grpGame, SWT.CHECK);
		_antialiasBtn.setSelection(true);
		_antialiasBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		_antialiasBtn.setSize(79, 20);
		_antialiasBtn.setText("antialias");

		Label lblPhysicsconfig = new Label(grpGame, SWT.NONE);
		lblPhysicsconfig.setText("physicsConfig:");

		Composite composite = new Composite(grpGame, SWT.NONE);
		composite.setLayout(new GridLayout(5, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

		_arcadeBtn = new Button(composite, SWT.CHECK);
		_arcadeBtn.setText("arcade");

		_ninjaBtn = new Button(composite, SWT.CHECK);
		_ninjaBtn.setText("ninja");

		_p2Btn = new Button(composite, SWT.CHECK);
		_p2Btn.setText("p2");

		_box2dBtn = new Button(composite, SWT.CHECK);
		_box2dBtn.setText("box2d");

		_matterBtn = new Button(composite, SWT.CHECK);
		_matterBtn.setText("matter");

		Group grpProjectStructure = new Group(container, SWT.NONE);
		grpProjectStructure.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		grpProjectStructure.setText("Project Structure");
		grpProjectStructure.setLayout(new GridLayout(1, false));

		_simplestBtn = new Button(grpProjectStructure, SWT.RADIO);
		_simplestBtn.setText("Simplest (global preload, update, create functions).");

		_singleStateBtn = new Button(grpProjectStructure, SWT.RADIO);
		_singleStateBtn.setSelection(true);
		_singleStateBtn.setText("Single state (easy to add more states).");

		_multipleStatesBtn = new Button(grpProjectStructure, SWT.RADIO);
		_multipleStatesBtn.setText("Multiple states with Preloader (for larger games).");

		Group grpAssets = new Group(container, SWT.NONE);
		grpAssets.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpAssets.setText("Assets");
		grpAssets.setLayout(new GridLayout(1, false));

		_includeAssets = new Button(grpAssets, SWT.CHECK);
		_includeAssets.setText("Include demo assets.");
		
		_group = new Group(container, SWT.NONE);
		_group.setLayout(new GridLayout(2, false));
		_group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_group.setText("Code");
		
		_label = new Label(_group, SWT.NONE);
		_label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		_label.setText("Language: ");
		
		_comboLang = new ComboViewer(_group, SWT.READ_ONLY);		
		Combo combo = _comboLang.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_comboLang.setContentProvider(new ArrayContentProvider());
		_comboLang.setLabelProvider(new ViewerLabelProvider());
		
		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		_comboLang.setInput(SourceLang.values());
		_comboLang.setSelection(new StructuredSelection(SourceLang.JAVA_SCRIPT));
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
	
	public Button getTransparentBtn() {
		return _transparentBtn;
	}

	public Combo getRendererCombo() {
		return _rendererCombo;
	}

	public Button getAntialiasBtn() {
		return _antialiasBtn;
	}

	public Button getArcadeBtn() {
		return _arcadeBtn;
	}

	public Button getNinjaBtn() {
		return _ninjaBtn;
	}

	public Button getP2Btn() {
		return _p2Btn;
	}

	public Button getBox2dBtn() {
		return _box2dBtn;
	}

	public Button getMatterBtn() {
		return _matterBtn;
	}

	public Button getSimplestBtn() {
		return _simplestBtn;
	}

	public Button getSingleStateBtn() {
		return _singleStateBtn;
	}

	public Button getMultipleStatesBtn() {
		return _multipleStatesBtn;
	}

	public Button getIncludeAssets() {
		return _includeAssets;
	}
	
	public SourceLang getSourceLang() {
		return (SourceLang) _comboLang.getStructuredSelection().getFirstElement();	
	}
}
