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
package phasereditor.assetpack.ui.editors;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONException;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.ScriptAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

public class ScriptAssetEditorComp extends Composite {
	private DataBindingContext m_bindingContext;

	private Text _text;
	private Text _text_1;
	private ScriptAssetModel _model;
	private ScriptAssetEditorComp _self = this;

	private boolean _firstTime = true;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public ScriptAssetEditorComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		_titleLabel = new Label(this, SWT.WRAP);
		GridData gd_titleLabel = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_titleLabel.widthHint = 200;
		_titleLabel.setLayoutData(gd_titleLabel);
		_titleLabel.setText("Set the properties of the 'script' file. Required fields are denoted by '*'.");

		Composite composite = new Composite(this, SWT.NONE);
		GridData gd_composite = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_composite.widthHint = 200;
		composite.setLayoutData(gd_composite);
		GridLayout gl_composite = new GridLayout(3, false);
		gl_composite.marginWidth = 0;
		composite.setLayout(gl_composite);

		_keyLabel = new Label(composite, SWT.NONE);
		_keyLabel.setText("key*");

		_text = new Text(composite, SWT.BORDER);
		GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text.horizontalIndent = 5;
		_text.setLayoutData(gd_text);
		_text.setText("");

		_urlLabel = new Label(composite, SWT.NONE);
		_urlLabel.setText("url*");

		_text_1 = new Text(composite, SWT.BORDER);
		GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_text_1.horizontalIndent = 5;
		_text_1.setLayoutData(gd_text_1);
		_text_1.setText("");

		Button btnBrowse = new Button(composite, SWT.NONE);
		btnBrowse.setText("Browse...");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseUrl();
			}
		});

		Label lblHelp = new Label(this, SWT.NONE);
		lblHelp.setText("Help");

		_helpText = new Text(this, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		_helpText.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));
		GridData gd_helpText = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd_helpText.heightHint = 200;
		gd_helpText.widthHint = 200;
		_helpText.setLayoutData(gd_helpText);
		_helpText.setText("");
		m_bindingContext = initDataBindings();
	}

	private void decorateControls() {
		IObservableList bindings = m_bindingContext.getBindings();
		for (int i = 0; i < bindings.size(); i++) {
			Binding b = (Binding) bindings.get(i);
			ControlDecorationSupport.create(b, SWT.TOP | SWT.LEFT);
		}
	}

	private void validateModelToTarget() {
		IObservableList bindings = m_bindingContext.getBindings();
		for (int i = 0; i < bindings.size(); i++) {
			Binding b = (Binding) bindings.get(i);
			b.validateTargetToModel();
		}
	}

	protected void browseUrl() {
		try {
			AssetPackModel pack = _model.getPack();
			IFile urlFile = _model.getFileFromUrl(_model.getUrl());
			IContainer folder = pack.getAssetsFolder().getParent();
			List<IFile> files = AssetPackCore.discoverFiles(folder, AssetPackCore.createFileExtFilter("js"));
			AssetPackUI.browseAssetFile(pack, "script", urlFile, files, getShell(), new Consumer<String>() {

				@Override
				public void accept(String url) {
					getModel().setUrl(url);
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public ScriptAssetModel getModel() {
		return _model;
	}

	public void setModel(ScriptAssetModel model) {
		_model = model;
		firePropertyChange("model");

		if (_firstTime) {
			_firstTime = false;
			setHelpMessages(model);
			decorateControls();
		}

		validateModelToTarget();
	}

	private void setHelpMessages(ScriptAssetModel model) {
		_helpText.setText(model.getHelp());
		_helpText.setToolTipText(model.getHelp());

		// params
		try {
			_keyLabel.setToolTipText(model.getHelp("key"));
			_urlLabel.setToolTipText(model.getHelp("url"));
		} catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private transient final PropertyChangeSupport _support = new PropertyChangeSupport(this);
	private Label _titleLabel;
	private Text _helpText;
	private Label _keyLabel;
	private Label _urlLabel;

	public void addPropertyChangeListener(PropertyChangeListener l) {
		_support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		_support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		_support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		_support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		_support.firePropertyChange(property, true, false);
	}

	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue modelkey_selfObserveValue = BeanProperties.value("model.key").observe(_self);
		UpdateValueStrategy strategy = new UpdateValueStrategy();
		strategy.setBeforeSetValidator(new RequiredValidator());
		bindingContext.bindValue(observeText_textObserveWidget, modelkey_selfObserveValue, strategy, null);
		//
		IObservableValue observeText_text_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_1);
		IObservableValue modelurl_selfObserveValue = BeanProperties.value("model.url").observe(_self);
		UpdateValueStrategy strategy_1 = new UpdateValueStrategy();
		strategy_1.setBeforeSetValidator(new RequiredValidator());
		bindingContext.bindValue(observeText_text_1ObserveWidget, modelurl_selfObserveValue, strategy_1, null);
		//
		return bindingContext;
	}
}
