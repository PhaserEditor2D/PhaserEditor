package phasereditor.project.core.prefs;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

public class PhaserProjectPropertiesPage extends PropertyPage {

	private Text _widthText;
	private Text _heightText;
	private ComboViewer _comboLang;

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

		{
			var label = new Label(comp, 0);
			label.setText("Default Scene Source Language");
			_comboLang = new ComboViewer(comp, SWT.READ_ONLY);
			Combo combo = _comboLang.getCombo();
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			_comboLang.setContentProvider(new ArrayContentProvider());
			_comboLang.setLabelProvider(new ViewerLabelProvider());
			_comboLang.setInput(new Object[] { SourceLang.JAVA_SCRIPT_6, SourceLang.TYPE_SCRIPT });
			_comboLang.setSelection(new StructuredSelection(ProjectCore.getProjectLanguage(getElement())));
		}

		afterCreateWidgets();

		return comp;
	}

	private static class ViewerLabelProvider extends LabelProvider {
		public ViewerLabelProvider() {
		}

		@Override
		public String getText(Object element) {
			return ((SourceLang) element).getDisplayName();
		}
	}

	private void afterCreateWidgets() {
		initFromStore();
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();

		initFromStore();

	}

	private void initFromStore() {
		var store = ProjectCore.getProjectPreferenceStore(getElement());
		var width = store.getInt(ProjectCore.PREF_PROP_PROJECT_GAME_WIDTH);
		var height = store.getInt(ProjectCore.PREF_PROP_PROJECT_GAME_HEIGHT);

		_widthText.setText(Integer.toString(width));
		_heightText.setText(Integer.toString(height));
		_comboLang.setSelection(new StructuredSelection(ProjectCore.getProjectLanguage(getElement())));
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
			ProjectCore.setProjectLanguage(getElement(),
					(SourceLang) _comboLang.getStructuredSelection().getFirstElement());

			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}