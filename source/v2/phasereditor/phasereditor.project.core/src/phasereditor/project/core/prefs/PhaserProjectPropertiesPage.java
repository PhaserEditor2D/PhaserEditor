package phasereditor.project.core.prefs;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import phasereditor.project.core.ProjectCore;

public class PhaserProjectPropertiesPage extends PropertyPage {

	private Text _widthText;
	private Text _heightText;

	/**
	 * Constructor for SamplePropertyPage.
	 */
	public PhaserProjectPropertiesPage() {
		super();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		comp.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		comp.setLayoutData(data);

		{
			var label = new Label(comp, 0);
			label.setText("Default Scene Width");
			_widthText = new Text(comp, SWT.BORDER);
			_widthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		{
			var label = new Label(comp, 0);
			label.setText("Default Scene Height");
			_heightText = new Text(comp, SWT.BORDER);
			_heightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		afterCreateWidgets();

		return comp;
	}

	private void afterCreateWidgets() {

		initFromStore(ProjectCore.getProjectPreferenceStore(getElement()));

	}

	@Override
	protected void performDefaults() {

		super.performDefaults();

		initFromStore(ProjectCore.getPreferenceStore());

	}

	private void initFromStore(IPreferenceStore store) {
		var width = store.getInt(ProjectCore.PREF_PROP_PROJECT_GAME_WIDTH);
		var height = store.getInt(ProjectCore.PREF_PROP_PROJECT_GAME_HEIGHT);

		_widthText.setText(Integer.toString(width));
		_heightText.setText(Integer.toString(height));
	}

	@Override
	public IProject getElement() {
		return (IProject) super.getElement();
	}

	@Override
	public boolean performOk() {
		try {
			var width = Integer.parseInt(_widthText.getText());
			var height = Integer.parseInt(_heightText.getText());

			ProjectCore.setProjectSceneSize(getElement(), width, height);

			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}