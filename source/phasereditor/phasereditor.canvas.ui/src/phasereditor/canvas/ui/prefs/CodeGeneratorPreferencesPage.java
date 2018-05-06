// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.canvas.ui.prefs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.EditorSettings_UserCode;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.editors.grid.editors.UserCodeDialog;

/**
 * @author arian
 *
 */
public class CodeGeneratorPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

	private HashMap<CanvasType, EditorSettings_UserCode> _settingsMap;

	public CodeGeneratorPreferencesPage() {
		super("Code Generation");
	}

	@Override
	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		Group userCodeGroup = new Group(container, SWT.NONE);
		userCodeGroup.setText("User Code");
		userCodeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		userCodeGroup.setLayout(new GridLayout(2, false));

		{
			Label label = new Label(userCodeGroup, SWT.NONE);
			label.setText(
					"Set the default User Code to be generated.\nFor example, to append 'onCreated();' at the end of each 'create' method.");
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			label.setLayoutData(gd);

		}

		createUserCodeWidgets(userCodeGroup, CanvasType.STATE);
		createUserCodeWidgets(userCodeGroup, CanvasType.SPRITE);
		createUserCodeWidgets(userCodeGroup, CanvasType.GROUP);

		afterCreateWidgets();

		return container;
	}

	private Map<CanvasType, Label> _labelMap = new HashMap<>();

	private void createUserCodeWidgets(Group userCodeGroup, CanvasType type) {

		Label label = new Label(userCodeGroup, SWT.NONE);
		String name = type.name().toLowerCase();
		_labelMap.put(type, label);

		label.setText(name.substring(0, 1).toUpperCase() + name.substring(1) + " user code");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button button = new Button(userCodeGroup, SWT.NONE);
		button.setText("Change");
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> handleChangeButton(type)));

	}

	private void updateLabels() {
		for (CanvasType type : CanvasType.values()) {
			Label label = _labelMap.get(type);
			String name = type.name().toLowerCase();
			String str = _settingsMap.get(type).toString();
			str = str.length() == 2 ? "" : str;
			label.setText(name.substring(0, 1).toUpperCase() + name.substring(1) + " User Code " + str);
		}
	}

	private void afterCreateWidgets() {
		_settingsMap = new HashMap<>();

		for (CanvasType type : CanvasType.values()) {
			CanvasModel canvasModel = new CanvasModel(null);
			canvasModel.setType(type);
			EditorSettings_UserCode settings = new EditorSettings_UserCode(canvasModel);
			update_Settings_from_Store(settings, type);
			_settingsMap.put(type, settings);
		}

		updateLabels();
	}

	private void handleChangeButton(CanvasType type) {
		UserCodeDialog dlg = new UserCodeDialog(getShell());
		CanvasModel canvasModel = new CanvasModel(null);
		canvasModel.setType(type);
		EditorSettings_UserCode settings = _settingsMap.get(type);
		EditorSettings_UserCode copy = settings.copy();
		dlg.setUserCode(copy);
		if (dlg.open() == Window.OK) {
			_settingsMap.put(type, copy);
		}
		updateLabels();
	}

	@Override
	public boolean performOk() {

		for (CanvasType type : CanvasType.values()) {
			update_Store_from_Settings(_settingsMap.get(type), type);
		}

		return true;
	}

	@Override
	protected void performDefaults() {

		for (CanvasType type : CanvasType.values()) {
			EditorSettings_UserCode settings = new EditorSettings_UserCode(new CanvasModel(null));
			settings.getCanvasModel().setType(type);
			_settingsMap.put(type, settings);
		}

		updateLabels();

		super.performDefaults();
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CanvasUI.getPreferenceStore());
	}

	private void update_Settings_from_Store(EditorSettings_UserCode settings, CanvasType type) {

		settings.setCreate_before(getCode(type, "Create_before"));
		settings.setCreate_after(getCode(type, "Create_after"));

		if (type == CanvasType.STATE) {
			settings.setState_constructor_before(getCode(type, "State_constructor_before"));
			settings.setState_constructor_after(getCode(type, "State_constructor_after"));
			settings.setState_init_before(getCode(type, "State_init_before"));
			settings.setState_init_after(getCode(type, "State_init_after"));
			settings.setState_init_args(getCode(type, "State_init_args"));
			settings.setState_preload_before(getCode(type, "State_preload_before"));
			settings.setState_preload_after(getCode(type, "State_preload_after"));
		}
	}

	private void update_Store_from_Settings(EditorSettings_UserCode settings, CanvasType type) {

		setCode(type, "Create_before", settings.getCreate_before());
		setCode(type, "Create_after", settings.getCreate_after());

		if (type == CanvasType.STATE) {
			setCode(type, "State_constructor_before", settings.getState_constructor_before());
			setCode(type, "State_constructor_after", settings.getState_constructor_after());
			setCode(type, "State_init_before", settings.getState_init_before());
			setCode(type, "State_init_after", settings.getState_init_after());
			setCode(type, "State_init_args", settings.getState_init_args());
			setCode(type, "State_preload_before", settings.getState_preload_before());
			setCode(type, "State_preload_after", settings.getState_preload_after());
		}
	}

	private String getCode(CanvasType type, String key) {
		return getPreferenceStore().getString("phasereditor.canvas.ui.codegen." + type + "." + key);
	}

	private void setCode(CanvasType type, String key, String value) {
		getPreferenceStore().setValue("phasereditor.canvas.ui.codegen." + type + "." + key, value);
	}
}
