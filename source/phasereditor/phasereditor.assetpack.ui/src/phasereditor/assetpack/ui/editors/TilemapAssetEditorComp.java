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
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONException;

import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

public class TilemapAssetEditorComp extends Composite {
	private DataBindingContext m_bindingContext;

	private Text _text;
	private Text _text_1;
	private Text _text_2;
	private Text _helpLabel;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public TilemapAssetEditorComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Label lblSetTheProperties = new Label(this, SWT.WRAP);
		lblSetTheProperties.setText("Set the properties of the 'tilemap'. Required fields are denoted by '*'.");
		GridData gd_lblSetTheProperties = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_lblSetTheProperties.widthHint = 200;
		lblSetTheProperties.setLayoutData(gd_lblSetTheProperties);

		Composite composite = new Composite(this, SWT.NONE);
		GridLayout gl_composite = new GridLayout(3, false);
		gl_composite.marginWidth = 0;
		composite.setLayout(gl_composite);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		_keyLabel = new Label(composite, SWT.NONE);
		_keyLabel.setText("key*");

		_text = new Text(composite, SWT.BORDER);
		_text.setText("");
		_text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		_urlLabel = new Label(composite, SWT.NONE);
		_urlLabel.setText("url");

		_text_1 = new Text(composite, SWT.BORDER);
		_text_1.setText("");
		_text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button btnBrowse = new Button(composite, SWT.NONE);
		btnBrowse.setText("Browse...");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseTilemap();
			}
		});

		_dataLabel = new Label(composite, SWT.NONE);
		_dataLabel.setText("data");
		_dataLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		_text_2 = new Text(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		_text_2.setText("");
		GridData gd_text_2 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_text_2.heightHint = 100;
		gd_text_2.widthHint = 200;
		_text_2.setLayoutData(gd_text_2);

		_formatLabel = new Label(composite, SWT.NONE);
		_formatLabel.setText("format");

		_formatViewer = new ComboViewer(composite, SWT.READ_ONLY);
		Combo combo = _formatViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		_formatViewer.setContentProvider(new ArrayContentProvider());
		_formatViewer.setLabelProvider(new LabelProvider());

		Label lblHelp = new Label(this, SWT.NONE);
		lblHelp.setText("Help");

		_helpLabel = new Text(this, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		_helpLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));
		GridData gd__helpLabel = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd__helpLabel.widthHint = 200;
		_helpLabel.setLayoutData(gd__helpLabel);
		m_bindingContext = initDataBindings();
		afterCreateWidgets();
	}

	protected void browseTilemap() {
		try {
			AssetPackModel pack = _model.getPack();
			IFile urlFile = _model.getUrlFile();
			List<IFile> tilemapFiles = pack.discoverTilemapFiles();
			AssetPackUI.browseAssetFile(pack, "tilemap JSON/CSV", urlFile, tilemapFiles, getShell(),
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

	private void afterCreateWidgets() {
		_formatViewer.setInput(new Object[] { TilemapAssetModel.TILEMAP_CSV, TilemapAssetModel.TILEMAP_TILED_JSON });
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

	private TilemapAssetEditorComp _self = this;
	private TilemapAssetModel _model;
	private boolean _firstTime = true;

	public TilemapAssetModel getModel() {
		return _model;
	}

	public void setModel(TilemapAssetModel model) {
		_model = model;
		firePropertyChange("model");

		if (_firstTime) {
			_firstTime = false;
			decorateControls();
			setHelpMessages(model);
		}

		validateModelToTarget();
	}

	private void setHelpMessages(TilemapAssetModel model) {
		try {
			_keyLabel.setToolTipText(model.getHelp("key"));
			_urlLabel.setToolTipText(model.getHelp("url"));
			_dataLabel.setToolTipText(model.getHelp("data"));
			_formatLabel.setToolTipText(model.getHelp("format"));
			_helpLabel.setText(model.getHelp());
		} catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private Label _keyLabel;
	private Label _urlLabel;
	private Label _dataLabel;
	private Label _formatLabel;
	private ComboViewer _formatViewer;

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

	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeText_textObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text);
		IObservableValue modelkey_selfObserveValue = BeanProperties.value("model.key").observe(_self);
		bindingContext.bindValue(observeText_textObserveWidget, modelkey_selfObserveValue, null, null);
		//
		IObservableValue observeText_text_1ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_1);
		IObservableValue modelurl_selfObserveValue = BeanProperties.value("model.url").observe(_self);
		bindingContext.bindValue(observeText_text_1ObserveWidget, modelurl_selfObserveValue, null, null);
		//
		IObservableValue observeText_text_2ObserveWidget = WidgetProperties.text(SWT.Modify).observe(_text_2);
		IObservableValue modeldata_selfObserveValue = BeanProperties.value("model.data").observe(_self);
		bindingContext.bindValue(observeText_text_2ObserveWidget, modeldata_selfObserveValue, null, null);
		//
		IObservableValue observeSingleSelection_comboViewer = ViewerProperties.singleSelection().observe(_formatViewer);
		IObservableValue modelformat_selfObserveValue = BeanProperties.value("model.format").observe(_self);
		bindingContext.bindValue(observeSingleSelection_comboViewer, modelformat_selfObserveValue, null, null);
		//
		return bindingContext;
	}
}
