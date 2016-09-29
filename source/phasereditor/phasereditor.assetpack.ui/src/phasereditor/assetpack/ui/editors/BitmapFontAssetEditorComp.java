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
import java.util.function.Function;

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

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

public class BitmapFontAssetEditorComp extends Composite {
	private DataBindingContext m_bindingContext;

	private Text _text;
	private Text _text_1;
	private Text _text_2;
	private Text _text_3;
	private Text _text_4;
	private Text _text_5;
	private Label _xSpacingLabel;
	private Label _ySpacingLabel;
	private Label _textureURLLabel;
	private Label _atlasURLLabel;
	private Label _atlasDataLabel;
	private Label _keyLabel;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public BitmapFontAssetEditorComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Label lblSetTheProperties = new Label(this, SWT.WRAP);
		lblSetTheProperties.setText("Set the properties of the 'bitmapFont'. Required fields are denoted by '*'.");
		GridData gd_lblSetTheProperties = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblSetTheProperties.widthHint = 100;
		lblSetTheProperties.setLayoutData(gd_lblSetTheProperties);

		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		composite.setLayout(new GridLayout(3, false));

		_keyLabel = new Label(composite, SWT.NONE);
		_keyLabel.setText("key*");

		_text = new Text(composite, SWT.BORDER);
		_text.setText("");
		GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text.horizontalIndent = 5;
		gd_text.widthHint = 100;
		_text.setLayoutData(gd_text);

		_textureURLLabel = new Label(composite, SWT.NONE);
		_textureURLLabel.setText("textureURL");

		_text_1 = new Text(composite, SWT.BORDER);
		_text_1.setText("");
		GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_text_1.horizontalIndent = 5;
		_text_1.setLayoutData(gd_text_1);

		Button btnBrowse = new Button(composite, SWT.NONE);
		btnBrowse.setText("Browse...");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseTextureURL();
			}
		});

		_atlasURLLabel = new Label(composite, SWT.NONE);
		_atlasURLLabel.setText("atlasURL");

		_text_2 = new Text(composite, SWT.BORDER);
		_text_2.setText("");
		GridData gd_text_2 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_text_2.horizontalIndent = 5;
		_text_2.setLayoutData(gd_text_2);

		Button btnBrowse_1 = new Button(composite, SWT.NONE);
		btnBrowse_1.setText("Browse...");
		btnBrowse_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseAtlasURL();
			}
		});

		_atlasDataLabel = new Label(composite, SWT.NONE);
		_atlasDataLabel.setText("atlasData");
		_atlasDataLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		_text_3 = new Text(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		_text_3.setText("");
		GridData gd_text_3 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_3.horizontalIndent = 5;
		gd_text_3.heightHint = 100;
		gd_text_3.widthHint = 100;
		_text_3.setLayoutData(gd_text_3);

		_xSpacingLabel = new Label(composite, SWT.NONE);
		_xSpacingLabel.setText("xSpacing");

		_text_4 = new Text(composite, SWT.BORDER);
		_text_4.setText("");
		GridData gd_text_4 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_4.horizontalIndent = 5;
		gd_text_4.widthHint = 100;
		_text_4.setLayoutData(gd_text_4);

		_ySpacingLabel = new Label(composite, SWT.NONE);
		_ySpacingLabel.setText("ySpacing");

		_text_5 = new Text(composite, SWT.BORDER);
		_text_5.setText("");
		GridData gd_text_5 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_5.horizontalIndent = 5;
		gd_text_5.widthHint = 100;
		_text_5.setLayoutData(gd_text_5);

		Label lblHelp = new Label(this, SWT.NONE);
		lblHelp.setText("Help");

		_helpLabel = new Text(this, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		_helpLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));
		GridData gd__helpLabel = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd__helpLabel.widthHint = 200;
		_helpLabel.setLayoutData(gd__helpLabel);
		m_bindingContext = initDataBindings();
	}

	protected void browseAtlasURL() {
		try {
			AssetPackModel pack = _model.getPack();
			IFile urlFile = _model.getFileFromUrl(_model.getAtlasURL());
			Function<IFile, Boolean> isFontFile = new Function<IFile, Boolean>() {

				@Override
				public Boolean apply(IFile f) {
					String ext = f.getFileExtension();
					if (ext != null && (ext.equals("xml") || ext.equals("fnt"))) {
						return Boolean.TRUE;
					}
					return Boolean.FALSE;
				}
			};
			List<IFile> xmlFiles = AssetPackCore.discoverFiles(pack.getAssetsFolder(), isFontFile);

			AssetPackUI.browseAssetFile(pack, "bitmap font XML/FNT", urlFile, xmlFiles, getShell(),
					new Consumer<String>() {

						@Override
						public void accept(String t) {
							getModel().setAtlasURL(t);
						}
					});
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected void browseTextureURL() {
		try {
			AssetPackModel pack = _model.getPack();
			IFile urlFile = _model.getFileFromUrl(_model.getTextureURL());
			List<IFile> imageFiles = pack.discoverImageFiles();
			AssetPackUI.browseImageUrl(pack, "texture", urlFile, imageFiles, getShell(), new Consumer<String>() {

				@Override
				public void accept(String t) {
					getModel().setTextureURL(t);
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

	private BitmapFontAssetEditorComp _self = this;
	private BitmapFontAssetModel _model;
	private boolean _firstTime = true;

	public BitmapFontAssetModel getModel() {
		return _model;
	}

	public void setModel(BitmapFontAssetModel model) {
		_model = model;
		firePropertyChange("model");

		if (_firstTime) {
			_firstTime = false;
			setHelpMessages(model);
			decorateControls();
		}

		validateModelToTarget();
	}

	private void setHelpMessages(BitmapFontAssetModel model) {
		_helpLabel.setText(model.getHelp());
		_helpLabel.setToolTipText(model.getHelp());

		// params
		try {
			_keyLabel.setToolTipText(model.getHelp("key"));
			_textureURLLabel.setToolTipText(model.getHelp("textureURL"));
			_atlasURLLabel.setToolTipText(model.getHelp("atlasURL"));
			_atlasDataLabel.setToolTipText(model.getHelp("atlasData"));
			_xSpacingLabel.setToolTipText(model.getHelp("xSpacing"));
			_ySpacingLabel.setToolTipText(model.getHelp("ySpacing"));
		} catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private Text _helpLabel;

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
		IObservableValue<?> modeltextureURL_selfObserveValue = BeanProperties.value("model.textureURL").observe(_self);
		UpdateValueStrategy strategy_1 = new UpdateValueStrategy();
		strategy_1.setBeforeSetValidator(new RequiredValidator());
		bindingContext.bindValue(observeText_text_1ObserveWidget, modeltextureURL_selfObserveValue, strategy_1, null);
		//
		IObservableValue<?> observeText_text_2ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_2);
		IObservableValue<?> modelxmlURL_selfObserveValue = BeanProperties.value("model.atlasURL").observe(_self);
		bindingContext.bindValue(observeText_text_2ObserveWidget, modelxmlURL_selfObserveValue, null, null);
		//
		IObservableValue<?> observeText_text_3ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_3);
		IObservableValue<?> modelxmlData_selfObserveValue = BeanProperties.value("model.atlasData").observe(_self);
		bindingContext.bindValue(observeText_text_3ObserveWidget, modelxmlData_selfObserveValue, null, null);
		//
		IObservableValue<?> observeText_text_4ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_4);
		IObservableValue<?> modelxSpacing_selfObserveValue = BeanProperties.value("model.xSpacing").observe(_self);
		bindingContext.bindValue(observeText_text_4ObserveWidget, modelxSpacing_selfObserveValue, null, null);
		//
		IObservableValue<?> observeText_text_5ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_5);
		IObservableValue<?> modelySpacing_selfObserveValue = BeanProperties.value("model.ySpacing").observe(_self);
		bindingContext.bindValue(observeText_text_5ObserveWidget, modelySpacing_selfObserveValue, null, null);
		//
		return bindingContext;
	}
}
