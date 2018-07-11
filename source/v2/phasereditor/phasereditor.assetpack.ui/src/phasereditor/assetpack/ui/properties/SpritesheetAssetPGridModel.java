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

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor2;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;
import phasereditor.ui.properties.PGridModel;
import phasereditor.ui.properties.PGridNumberProperty;
import phasereditor.ui.properties.PGridSection;
import phasereditor.ui.properties.PGridStringProperty;

@SuppressWarnings("boxing")
public class SpritesheetAssetPGridModel extends PGridModel {
	SpritesheetAssetModel _asset;

	public SpritesheetAssetPGridModel(SpritesheetAssetModel asset) {
		super();
		_asset = asset;

		PhaserJsdocModel help = InspectCore.getPhaserHelp();

		PGridSection section = new PGridSection("Spritesheet");

		section.add(new PGridStringProperty("key", "key", _asset.getHelp("key")) {

			@Override
			public String getValue() {
				return _asset.getKey();
			}

			@Override
			public void setValue(String value, boolean notify) {
				_asset.setKey(value);

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return true;
			}

		});
		section.add(new PGridStringProperty("url", "url", _asset.getHelp("url")) {

			@Override
			public void setValue(String value, boolean notify) {
				_asset.setUrl(value);
				_asset.build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return true;
			}

			@Override
			public String getValue() {
				return _asset.getUrl();
			}

			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				return new ImageUrlCellEditor(parent, _asset, a -> ((SpritesheetAssetModel) a).getUrlFile());
			}
		});

		section.add(new PGridStringProperty("normalMap", "normalMap",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFileConfig.normalMap")) {

			@Override
			public void setValue(String value, boolean notify) {
				_asset.setNormalMap(value);

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return _asset.getNormalMap() != null && _asset.getNormalMap().length() > 0;
			}

			@Override
			public String getValue() {
				return _asset.getNormalMap();
			}

			@Override
			public String getDefaultValue() {
				return null;
			}

			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				return new ImageUrlCellEditor(parent, _asset, a -> ((SpritesheetAssetModel) a).getNormalMapFile());
			}
		});

		getSections().add(section);
		
		section = new PGridSection("frameConfig");
		
		section.add(new PGridNumberProperty("frameWidth", "frameWidth",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFrameConfig.frameWidth"), true) {

			@Override
			public Double getValue() {
				return (double) _asset.getFrameWidth();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				_asset.setFrameWidth(value.intValue());
				_asset.build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return true;
			}
		});

		section.add(new PGridNumberProperty("frameHeight", "frameHeight",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFrameConfig.frameHeight"), true) {

			@Override
			public Double getValue() {
				return (double) _asset.getFrameHeight();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				_asset.setFrameHeight(value.intValue());
				_asset.build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return true;
			}
		});

		section.add(new PGridNumberProperty("startFrame", "startFrame",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFrameConfig.startFrame"), true) {

			@Override
			public Double getValue() {
				return (double) _asset.getStartFrame();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				_asset.setStartFrame(value.intValue());
				_asset.build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
				
			}

			@Override
			public boolean isModified() {
				return _asset.getEndFrame() != -1;
			}

			@Override
			public Double getDefaultValue() {
				return (double) -1;
			}
		});

		section.add(new PGridNumberProperty("endFrame", "endFrame",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFrameConfig.endFrame"), true) {

			@Override
			public Double getValue() {
				return (double) _asset.getEndFrame();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				_asset.setEndFrame(value.intValue());
				_asset.build(new ArrayList<>());
				
				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return _asset.getEndFrame() != -1;
			}

			@Override
			public Double getDefaultValue() {
				return (double) -1;
			}
		});

		section.add(new PGridNumberProperty("spacing", "spacing",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFrameConfig.spacing"), true) {

			@Override
			public Double getValue() {
				return (double) _asset.getSpacing();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				_asset.setSpacing(value.intValue());
				_asset.build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return _asset.getSpacing() != 0;
			}
		});

		section.add(new PGridNumberProperty("margin", "margin",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFrameConfig.margin"), true) {

			@Override
			public Double getValue() {
				return (double) _asset.getMargin();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				_asset.setMargin(value.intValue());
				_asset.build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return _asset.getMargin() != 0;
			}
		});

		getSections().add(section);

	}

	protected static void updateEditorFromPropertyChange() {
		AssetPackEditor2 editor = (AssetPackEditor2) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		editor.refresh();
	}

}