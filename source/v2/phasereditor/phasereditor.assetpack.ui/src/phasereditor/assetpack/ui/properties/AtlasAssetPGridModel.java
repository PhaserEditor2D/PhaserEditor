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

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.atlas.core.AtlasCore;
import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.properties.PGridSection;
import phasereditor.ui.properties.PGridStringProperty;

/**
 * @author arian
 *
 */
public class AtlasAssetPGridModel extends BaseAssetPGridModel<AtlasAssetModel> {

	public AtlasAssetPGridModel(AtlasAssetModel asset) {
		super(asset);

		PGridSection section = new PGridSection("Atlas JSON");

		section.add(new PGridStringProperty("key", "key", getAsset().getHelp("key")) {

			@Override
			public String getValue() {
				return getAsset().getKey();
			}

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setKey(value);

				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return true;
			}
		});

		section.add(new PGridStringProperty("textureURL", "textureURL", getAsset().getHelp("textureURL")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setTextureURL(value);

				if (notify) {
					updateFromPropertyChange();
				}
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
				return new ImageUrlCellEditor(parent, getAsset(), a -> ((ImageAssetModel) a).getUrlFile());
			}
		});

		section.add(new PGridStringProperty("atlasURL", "atlasURL", getAsset().getHelp("atlasURL")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setAtlasURL(value);

				IFile file = getAsset().getFileFromUrl(value);
				if (file != null) {
					String format;
					try {
						format = AtlasCore.getAtlasFormat(file);
						if (format != null) {
							asset.setFormat(format);
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}

				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return true;
			}

			@Override
			public String getValue() {
				return getAsset().getAtlasURL();
			}

			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				AssetType type = getAsset().getType();

				Function<AssetModel, IFile> getFile = a -> a.getFileFromUrl(((AtlasAssetModel) a).getAtlasURL());

				Supplier<List<IFile>> discoverFiles = () -> {
					try {
						return getAsset().getPack().discoverAtlasFiles(type);
					} catch (CoreException e) {
						throw new RuntimeException(e);
					}
				};

				String title = type == AssetType.atlasXML ? "atlas XML" : "atlas JSON";

				return new FileUrlCellEditor(parent, getAsset(), getFile, discoverFiles, title);
			}
		});

		section.add(new PGridStringProperty("normalMap", "normalMap",
				InspectCore.getPhaserHelp().getMemberHelp("Phaser.Loader.FileTypes.AtlasJSONFileConfig.normalMap")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setNormalMap(value);

				if (notify) {
					updateFromPropertyChange();
				}
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
				return new ImageUrlCellEditor(parent, getAsset(), a -> ((AtlasAssetModel) a).getNormalMapFile());
			}
		});

		getSections().add(section);
	}
}
