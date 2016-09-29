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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
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

import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

public class AudioSpriteAssetEditorComp extends Composite {
	Binding _urlsBinding;
	private DataBindingContext m_bindingContext;

	private AudioSpriteAssetEditorComp _self = this;

	private Text _text;
	private Text _text_1;
	private Text _helpText;
	private Label _urlsLabel;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	@SuppressWarnings("unused")
	public AudioSpriteAssetEditorComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Label lblLaMuela = new Label(this, SWT.WRAP);
		lblLaMuela.setText("Set the properties of the 'audio'. Required fields are denoted by '*'.");
		GridData gd_lblLaMuela = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_lblLaMuela.widthHint = 200;
		lblLaMuela.setLayoutData(gd_lblLaMuela);

		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		GridData gd_composite = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_composite.widthHint = 200;
		composite.setLayoutData(gd_composite);

		_keyLabel = new Label(composite, SWT.NONE);
		_keyLabel.setText("key*");

		_text = new Text(composite, SWT.BORDER);
		_text.setText("");
		GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text.horizontalIndent = 5;
		_text.setLayoutData(gd_text);

		_urlsLabel = new Label(composite, SWT.NONE);
		_urlsLabel.setText("urls*");
		_urlsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		_text_1 = new Text(composite, SWT.BORDER | SWT.MULTI);
		_text_1.setText("");
		GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_text_1.widthHint = 200;
		gd_text_1.heightHint = 80;
		gd_text_1.horizontalIndent = 5;
		_text_1.setLayoutData(gd_text_1);

		Button btnBrowse = new Button(composite, SWT.NONE);
		btnBrowse.setText("Browse...");
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseUrls();
			}
		});

		_jsonURLLabel = new Label(composite, SWT.NONE);
		_jsonURLLabel.setText("jsonURL");

		_jsonURLText = new Text(composite, SWT.BORDER);
		_jsonURLText.setText("");
		GridData gd_jsonURLText = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_jsonURLText.widthHint = 200;
		gd_jsonURLText.horizontalIndent = 5;
		_jsonURLText.setLayoutData(gd_jsonURLText);

		Button _browseJsonURLButton = new Button(composite, SWT.NONE);
		_browseJsonURLButton.setText("Browse...");
		_browseJsonURLButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseJsonUrl();
			}
		});

		_jsonDataLabel = new Label(composite, SWT.NONE);
		_jsonDataLabel.setText("jsonData");
		_jsonDataLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		_jsonDataText = new Text(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		_jsonDataText.setText("");
		GridData gd_jsonDataText = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		gd_jsonDataText.widthHint = 200;
		gd_jsonDataText.heightHint = 100;
		gd_jsonDataText.horizontalIndent = 5;
		_jsonDataText.setLayoutData(gd_jsonDataText);

		_autoDecodeButton = new Button(composite, SWT.CHECK);
		_autoDecodeButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

		_autoDecodeLabel = new Label(composite, SWT.NONE);
		_autoDecodeLabel.setText("autoDecode");
		new Label(composite, SWT.NONE);

		_label = new Label(this, SWT.NONE);
		_label.setText("Help");

		_helpText = new Text(this, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		_helpText.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));
		_helpText.setText("");
		GridData gd_helpText = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd_helpText.widthHint = 200;
		_helpText.setLayoutData(gd_helpText);
		m_bindingContext = initDataBindings();

	}

	protected void browseJsonUrl() {
		AudioSpriteAssetModel asset = getModel();
		AssetPackModel packModel = asset.getPack();
		try {
			IFile currentFile = asset.getJsonURLFile();

			AssetPackUI.browseAssetFile(packModel, "audio sprites JSON", currentFile,
					packModel.discoverAudioSpriteFiles(), getShell(), new Consumer<String>() {

						@Override
						public void accept(String t) {
							try {
								asset.setJsonURL(t);
								if (asset.setUrlsFromJsonResources()) {
									_urlsBinding.updateModelToTarget();
								}
							} catch (JSONException e) {
								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
					});
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	void browseUrls() {
		AudioSpriteAssetModel asset = getModel();
		AssetPackModel packModel = asset.getPack();
		try {
			List<IFile> currentFiles = new ArrayList<>();
			for (String url : asset.getUrls()) {
				IFile file = asset.getFileFromUrl(url);
				if (file != null) {
					currentFiles.add(file);
				}
			}

			AssetPackUI.browseAudioUrl(packModel, currentFiles, packModel.discoverAudioFiles(), getShell(),
					new Consumer<String>() {

						@Override
						public void accept(String t) {
							try {
								asset.setUrlsJSONString(t);
							} catch (JSONException e) {
								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
					});
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void decorateControls() {
		IObservableList<?> bindings = m_bindingContext.getBindings();
		for (int i = 0; i < bindings.size(); i++) {
			Binding b = (Binding) bindings.get(i);
			ControlDecorationSupport.create(b, SWT.TOP | SWT.LEFT);
		}
	}

	private void validateModelToTarget() {
		IObservableList<?> bindings = m_bindingContext.getBindings();
		for (int i = 0; i < bindings.size(); i++) {
			Binding b = (Binding) bindings.get(i);
			b.validateTargetToModel();
		}
	}

	private AudioSpriteAssetModel _model;
	private boolean _firstTime = true;

	public AudioSpriteAssetModel getModel() {
		return _model;
	}

	public void setModel(AudioSpriteAssetModel model) {
		_model = model;
		firePropertyChange("model");

		if (_firstTime) {
			_firstTime = false;
			decorateControls();
			setHelpMessages(model);
		}

		validateModelToTarget();
	}

	private void setHelpMessages(AudioSpriteAssetModel model) {
		try {
			_keyLabel.setToolTipText(model.getHelp("key"));
			_urlsLabel.setToolTipText(model.getHelp("urls"));
			_jsonURLLabel.setToolTipText(model.getHelp("jsonURL"));
			_jsonDataLabel.setToolTipText(model.getHelp("jsonData"));
			_autoDecodeButton.setToolTipText(model.getHelp("autoDecode"));
			_autoDecodeLabel.setToolTipText(model.getHelp("autoDecode"));
			_helpText.setText(model.getHelp());
			_helpText.setToolTipText(model.getHelp());
		} catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private transient final PropertyChangeSupport _support = new PropertyChangeSupport(this);
	private Label _keyLabel;
	private Button _autoDecodeButton;
	private Label _jsonURLLabel;
	private Text _jsonURLText;
	private Label _jsonDataLabel;
	private Text _jsonDataText;
	private Label _autoDecodeLabel;
	private Label _label;

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

	@SuppressWarnings("unchecked")
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue<?> observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue<?> modelkey_selfObserveValue = BeanProperties.value("model.key").observe(_self);
		UpdateValueStrategy strategy = new UpdateValueStrategy();
		strategy.setBeforeSetValidator(new RequiredValidator());
		bindingContext.bindValue(observeText_textObserveWidget, modelkey_selfObserveValue, strategy, null);
		//
		IObservableValue<?> observeText_text_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_1);
		IObservableValue<?> modelurlsJSONString_selfObserveValue = BeanProperties.value("model.urlsJSONString")
				.observe(_self);
		UpdateValueStrategy strategy_1 = new UpdateValueStrategy();
		strategy_1.setBeforeSetValidator(new UrlsValidator());
		_urlsBinding = bindingContext.bindValue(observeText_text_1ObserveWidget, modelurlsJSONString_selfObserveValue,
				strategy_1, null);
		//
		IObservableValue<?> observeSelection_autoDecodeButtonObserveWidget = WidgetProperties.selection()
				.observe(_autoDecodeButton);
		IObservableValue<?> modelautoDecode_selfObserveValue = BeanProperties.value("model.autoDecode").observe(_self);
		bindingContext.bindValue(observeSelection_autoDecodeButtonObserveWidget, modelautoDecode_selfObserveValue, null,
				null);
		//
		IObservableValue<?> observeText_jsonURLTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(_jsonURLText);
		IObservableValue<?> modeljsonURL_selfObserveValue = BeanProperties.value("model.jsonURL").observe(_self);
		bindingContext.bindValue(observeText_jsonURLTextObserveWidget, modeljsonURL_selfObserveValue, null, null);
		//
		IObservableValue<?> observeText_jsonDataTextObserveWidget = WidgetProperties.text(SWT.Modify)
				.observe(_jsonDataText);
		IObservableValue<?> modeljsonData_selfObserveValue = BeanProperties.value("model.jsonData").observe(_self);
		UpdateValueStrategy strategy_2 = new UpdateValueStrategy();
		strategy_2.setBeforeSetValidator(new AudioSpriteJSONValidator());
		bindingContext.bindValue(observeText_jsonDataTextObserveWidget, modeljsonData_selfObserveValue, strategy_2,
				null);
		//
		return bindingContext;
	}
}
