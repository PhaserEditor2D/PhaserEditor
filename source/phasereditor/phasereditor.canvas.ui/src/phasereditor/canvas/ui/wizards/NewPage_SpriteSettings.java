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
import java.util.function.Supplier;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.FlatAssetLabelProvider;
import phasereditor.assetpack.ui.TextureListContentProvider;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.SourceLang;
import phasereditor.canvas.ui.editors.LangLabelProvider;
import phasereditor.ui.FilteredTree2;
import phasereditor.ui.PatternFilter2;
import phasereditor.ui.views.PreviewComp;

/**
 * @author arian
 *
 */
public class NewPage_SpriteSettings extends WizardPage {
	@SuppressWarnings("unused")
	private DataBindingContext m_bindingContext;
	private ComboViewer _langComboViewer;
	private Text _text;
	private EditorSettings _settings;
	private Button _btnGenerateTheCorrspondant;
	private FilteredTree2 _filteredTree;
	private Object _selectedAsset;
	private PreviewComp _previewComp;

	public NewPage_SpriteSettings() {
		super("group.settings.page");
		setMessage("Customize the new Phaser Sprite.");
		setTitle("Sprite Customization");
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
		_text.setText("Phaser.Sprite");
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblCodeFormat = new Label(container, SWT.NONE);
		lblCodeFormat.setText("Code Format");

		_langComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		Combo combo = _langComboViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblSelectTheAsset = new Label(container, SWT.NONE);
		lblSelectTheAsset.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblSelectTheAsset.setText("Select the asset for the sprite:");

		SashForm sashForm = new SashForm(container, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));

		_filteredTree = new FilteredTree2(sashForm, SWT.SINGLE | SWT.BORDER, new PatternFilter2(), 4);
		_assetsViewer = _filteredTree.getViewer();
		_tree = _assetsViewer.getTree();
		_tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		_previewComp = new PreviewComp(sashForm, SWT.BORDER);
		sashForm.setWeights(new int[] { 4, 3 });

		_btnGenerateTheCorrspondant = new Button(container, SWT.CHECK);
		_btnGenerateTheCorrspondant.setSelection(true);
		_btnGenerateTheCorrspondant.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 2, 1));
		_btnGenerateTheCorrspondant.setText("Edit this sprite with the visual editor.");
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
		_assetsViewer.setLabelProvider(new FlatAssetLabelProvider(AssetLabelProvider.GLOBAL_48));
		_assetsViewer.setContentProvider(new TextureListContentProvider());
		AssetPackUI.installAssetTooltips(_assetsViewer);
		_assetsViewer.addSelectionChangedListener(e -> {
			validateErrors();
			previewSelection();
		});
		_btnGenerateTheCorrspondant.addSelectionListener(new SelectionListener() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void widgetSelected(SelectionEvent e) {
				validateErrors();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//
			}
		});
		_previewComp.preview(null);
	}

	private void previewSelection() {
		IStructuredSelection sel = _assetsViewer.getStructuredSelection();
		Object elem = sel.getFirstElement();
		if (!(elem instanceof IAssetFrameModel || elem instanceof ImageAssetModel)) {
			elem = null;
		}
		_previewComp.preview(elem);
	}

	private void validateErrors() {
		setErrorMessage(null);

		if (_btnGenerateTheCorrspondant.getSelection()) {
			IStructuredSelection sel = _assetsViewer.getStructuredSelection();
			Object elem = sel.getFirstElement();
			if (elem instanceof IAssetFrameModel || elem instanceof ImageAssetModel) {
				_selectedAsset = elem;
			} else {
				setErrorMessage("Please select a valid asset/frame.");
			}
		}

		setPageComplete(getErrorMessage() == null);
	}

	public Object getSelectedAsset() {
		return _selectedAsset;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		IProject project = null;
		if (_projectSupplier != null) {
			project = _projectSupplier.get();
		}

		Object lastProject = _assetsViewer.getInput();

		if (lastProject == null || lastProject != project) {
			_assetsViewer.setInput(project);
			_assetsViewer.expandToLevel(4);
		}

		validateErrors();
	}

	private NewPage_SpriteSettings _self = this;
	private Supplier<IProject> _projectSupplier;

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

	public void setProjectProvider(Supplier<IProject> projectSupplier) {
		_projectSupplier = projectSupplier;
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private Tree _tree;
	private TreeViewer _assetsViewer;

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
