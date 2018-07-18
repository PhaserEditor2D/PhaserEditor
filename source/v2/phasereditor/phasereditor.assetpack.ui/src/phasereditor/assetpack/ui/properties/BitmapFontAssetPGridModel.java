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
package phasereditor.assetpack.ui.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.properties.PGridSection;
import phasereditor.ui.properties.PGridStringProperty;

public class BitmapFontAssetPGridModel extends BaseAssetPGridModel<BitmapFontAssetModel> {

	public BitmapFontAssetPGridModel(BitmapFontAssetModel asset) {
		super(asset);

		PGridSection section = new PGridSection("Bitmap Font");

		section.add(createKeyProperty());

		section.add(new PGridStringProperty("fontDataURL", "fontDataURL", getAsset().getHelp("fontDataURL")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setFontDataURL(value);
			}

			@Override
			public boolean isModified() {
				return true;
			}

			@Override
			public String getValue() {
				return getAsset().getFontDataURL();
			}

			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				return new BitmapFontDataUrlCellEditor(parent, getAsset());
			}
		});

		section.add(new PGridStringProperty("textureURL", "textureURL", getAsset().getHelp("textureURL")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setTextureURL(value);
			}

			@Override
			public boolean isModified() {
				return true;
			}

			@Override
			public String getValue() {
				return getAsset().getTextureURL();
			}

			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				return new ImageUrlCellEditor(parent, getAsset(), a -> ((BitmapFontAssetModel) a).getTextureURL());
			}
		});

		section.add(new PGridStringProperty("normalMap", "normalMap",
				InspectCore.getPhaserHelp().getMemberHelp("Phaser.Loader.FileTypes.BitmapFontFileConfig.normalMap")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setNormalMap(value);
			}

			@Override
			public boolean isModified() {
				return getAsset().getNormalMap() != null && getAsset().getNormalMap().length() > 0;
			}

			@Override
			public String getValue() {
				return getAsset().getNormalMap();
			}

			@Override
			public String getDefaultValue() {
				return null;
			}

			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				return new ImageUrlCellEditor(parent, getAsset(), a -> ((BitmapFontAssetModel) a).getNormalMap());
			}
		});

		getSections().add(section);

	}

}