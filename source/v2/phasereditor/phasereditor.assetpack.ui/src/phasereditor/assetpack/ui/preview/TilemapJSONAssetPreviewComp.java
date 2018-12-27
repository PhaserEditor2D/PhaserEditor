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
package phasereditor.assetpack.ui.preview;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel.Layer;
import phasereditor.assetpack.core.TilemapAssetModel.TilemapJSON;
import phasereditor.assetpack.core.TilemapAssetModel.Tileset;
import phasereditor.ui.ImageProxyCanvas;

public class TilemapJSONAssetPreviewComp extends Composite {
	static class TilemapLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Layer) {
				return ((Layer) element).getName();
			}
			if (element instanceof Tileset) {
				Tileset tileset = (Tileset) element;
				return tileset.getName() + " (" + tileset.getImage() + ")";
			}
			return super.getText(element);
		}
	}

	private ComboViewer _layersViewer;
	private ComboViewer _tilesetsViewer;
	private Composite _composite;
	private ImageProxyCanvas _imageCanvas;
	private TilemapAssetModel _model;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public TilemapJSONAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		_composite = new Composite(this, SWT.NONE);
		GridData gd_composite = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_composite.widthHint = 200;
		_composite.setLayoutData(gd_composite);
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.marginWidth = 0;
		gl_composite.marginHeight = 0;
		_composite.setLayout(gl_composite);

		Label lblLayers = new Label(_composite, SWT.NONE);
		lblLayers.setText("layers");

		_layersViewer = new ComboViewer(_composite, SWT.READ_ONLY);
		Combo _table = _layersViewer.getCombo();
		_table.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_layersViewer.setLabelProvider(new TilemapLabelProvider());
		_layersViewer.setContentProvider(new ArrayContentProvider());

		Label lblTilesets = new Label(_composite, SWT.NONE);
		lblTilesets.setText("tilesets");

		_tilesetsViewer = new ComboViewer(_composite, SWT.READ_ONLY);
		_tilesetsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				tilesetSelected();
			}
		});
		Combo _table_1 = _tilesetsViewer.getCombo();
		_table_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_tilesetsViewer.setLabelProvider(new TilemapLabelProvider());
		_tilesetsViewer.setContentProvider(new ArrayContentProvider());

		_imageCanvas = new ImageProxyCanvas(this, SWT.NONE);
		_imageCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {
		//
	}

	protected void tilesetSelected() {
		Tileset tileset = (Tileset) ((IStructuredSelection) _tilesetsViewer.getSelection()).getFirstElement();
		IFile file = null;
		if (tileset != null) {
//			file = tileset.getImageFile();
		}
		_imageCanvas.setImageInfo(file, null);
	}

	public TilemapAssetModel getModel() {
		return _model;
	}

	public void setModel(TilemapAssetModel model) {
		_model = model;

		TilemapJSON tilemap = model.getTilemapJSON();
		List<Layer> layers = tilemap.getLayers();
		_layersViewer.setInput(layers);
		if (layers.isEmpty()) {
			_layersViewer.setSelection(new StructuredSelection());
		} else {
			_layersViewer.setSelection(new StructuredSelection(layers.get(0)));
		}

		List<Tileset> tilesets = tilemap.getTilesets();
		_tilesetsViewer.setInput(tilesets);
		ISelection sel = StructuredSelection.EMPTY;

		if (!tilesets.isEmpty()) {
			sel = new StructuredSelection(tilesets.get(0));
		}
		_tilesetsViewer.setSelection(sel);
	}

	public void selectElement(Object element) {
		if (element instanceof Tileset) {
			_tilesetsViewer.setSelection(new StructuredSelection(element));
		} else {
			_layersViewer.setSelection(new StructuredSelection(element));
		}
	}
}
