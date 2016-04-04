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
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

public class SpritesheetAssetEditorComp extends Composite {
	private DataBindingContext m_bindingContext;

	private Text _text;
	private Text _text_1;
	private SpritesheetAssetModel _model;
	private SpritesheetAssetEditorComp _self = this;

	private boolean _firstTime = true;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public SpritesheetAssetEditorComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		_titleLabel = new Label(this, SWT.WRAP);
		GridData gd_titleLabel = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_titleLabel.widthHint = 200;
		_titleLabel.setLayoutData(gd_titleLabel);
		_titleLabel.setText("Set the properties....");

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

		_frameWidthLabel = new Label(composite, SWT.NONE);
		_frameWidthLabel.setText("frameWidth*");

		_text_2 = new Text(composite, SWT.BORDER);
		_text_2.setText("");
		GridData gd_text_2 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_2.horizontalIndent = 5;
		_text_2.setLayoutData(gd_text_2);

		_frameHeightLabel = new Label(composite, SWT.NONE);
		_frameHeightLabel.setText("frameHeight*");

		_text_3 = new Text(composite, SWT.BORDER);
		_text_3.setText("");
		GridData gd_text_3 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_3.horizontalIndent = 5;
		_text_3.setLayoutData(gd_text_3);

		_frameMaxLabel = new Label(composite, SWT.NONE);
		_frameMaxLabel.setText("frameMax");

		_text_4 = new Text(composite, SWT.BORDER);
		_text_4.setText("");
		GridData gd_text_4 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_4.horizontalIndent = 5;
		_text_4.setLayoutData(gd_text_4);

		_marginLabel = new Label(composite, SWT.NONE);
		_marginLabel.setText("margin");

		_text_5 = new Text(composite, SWT.BORDER);
		_text_5.setText("");
		GridData gd_text_5 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_5.horizontalIndent = 5;
		_text_5.setLayoutData(gd_text_5);

		_spacingLabel = new Label(composite, SWT.NONE);
		_spacingLabel.setText("spacing");

		_text_6 = new Text(composite, SWT.BORDER);
		_text_6.setText("");
		GridData gd_text_6 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_6.horizontalIndent = 5;
		_text_6.setLayoutData(gd_text_6);
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
			AssetPackModel packModel = _model.getPack();
			IFile urlFile = _model.getUrlFile();
			List<IFile> imageFiles = packModel.discoverImageFiles();
			AssetPackUI.browseImageUrl(packModel, "spritesheet", urlFile, imageFiles, getShell(),
					new Consumer<String>() {

						@Override
						public void accept(String t) {
							getModel().setUrl(t);
						}
					});
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public SpritesheetAssetModel getModel() {
		return _model;
	}

	public void setModel(SpritesheetAssetModel model) {
		_model = model;
		firePropertyChange("model");

		if (_firstTime) {
			_firstTime = false;
			setHelpMessages(model);
			decorateControls();
		}

		validateModelToTarget();
	}

	private void setHelpMessages(SpritesheetAssetModel model) {
		_titleLabel.setText("Set the properties of the '" + model.getType() + "'. Required fields are denoted by '*'.");
		_helpText.setText(model.getHelp());
		_helpText.setToolTipText(model.getHelp());

		// params
		try {
			_keyLabel.setToolTipText(model.getHelp("key"));
			_urlLabel.setToolTipText(model.getHelp("url"));
			_frameWidthLabel.setToolTipText(model.getHelp("frameWidth"));
			_frameHeightLabel.setToolTipText(model.getHelp("frameHeight"));
			_frameMaxLabel.setToolTipText(model.getHelp("frameMax"));
			_marginLabel.setToolTipText(model.getHelp("margin"));
			_spacingLabel.setToolTipText(model.getHelp("spacing"));
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
	private Label _frameWidthLabel;
	private Text _text_2;
	private Label _frameHeightLabel;
	private Text _text_3;
	private Label _frameMaxLabel;
	private Text _text_4;
	private Label _marginLabel;
	private Text _text_5;
	private Label _spacingLabel;
	private Text _text_6;

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
		IObservableValue observeText_text_2ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_2);
		IObservableValue modelframeWidth_selfObserveValue = BeanProperties.value("model.frameWidth").observe(_self);
		bindingContext.bindValue(observeText_text_2ObserveWidget, modelframeWidth_selfObserveValue, null, null);
		//
		IObservableValue observeText_text_3ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_3);
		IObservableValue modelframeHeight_selfObserveValue = BeanProperties.value("model.frameHeight").observe(_self);
		bindingContext.bindValue(observeText_text_3ObserveWidget, modelframeHeight_selfObserveValue, null, null);
		//
		IObservableValue observeText_text_4ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_4);
		IObservableValue modelframeMax_selfObserveValue = BeanProperties.value("model.frameMax").observe(_self);
		bindingContext.bindValue(observeText_text_4ObserveWidget, modelframeMax_selfObserveValue, null, null);
		//
		IObservableValue observeText_text_5ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_5);
		IObservableValue modelmargin_selfObserveValue = BeanProperties.value("model.margin").observe(_self);
		bindingContext.bindValue(observeText_text_5ObserveWidget, modelmargin_selfObserveValue, null, null);
		//
		IObservableValue observeText_text_6ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_6);
		IObservableValue modelspacing_selfObserveValue = BeanProperties.value("model.spacing").observe(_self);
		bindingContext.bindValue(observeText_text_6ObserveWidget, modelspacing_selfObserveValue, null, null);
		//
		return bindingContext;
	}
}
