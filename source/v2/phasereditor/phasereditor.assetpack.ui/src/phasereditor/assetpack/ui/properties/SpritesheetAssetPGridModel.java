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
import phasereditor.assetpack.ui.editors.AssetPackEditor;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;
import phasereditor.ui.properties.PGridNumberProperty;
import phasereditor.ui.properties.PGridSection;
import phasereditor.ui.properties.PGridStringProperty;

@SuppressWarnings("boxing")
public class SpritesheetAssetPGridModel extends BaseAssetPGridModel<SpritesheetAssetModel> {

	public SpritesheetAssetPGridModel(SpritesheetAssetModel asset) {
		super(asset);

		PhaserJsdocModel help = InspectCore.getPhaserHelp();

		PGridSection section = new PGridSection("Spritesheet");

		section.add(createKeyProperty());
		
		section.add(new PGridStringProperty("url", "url", getAsset().getHelp("url")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setUrl(value);
				getAsset().build(new ArrayList<>());

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
				return getAsset().getUrl();
			}

			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				return new ImageUrlCellEditor(parent, getAsset(), a -> ((SpritesheetAssetModel) a).getUrl());
			}
		});

		section.add(new PGridStringProperty("normalMap", "normalMap",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFileConfig.normalMap")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setNormalMap(value);

				if (notify) {
					updateEditorFromPropertyChange();
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
				return new ImageUrlCellEditor(parent, getAsset(), a -> ((SpritesheetAssetModel) a).getNormalMap());
			}
		});

		getSections().add(section);
		
		section = new PGridSection("frameConfig");
		
		section.add(new PGridNumberProperty("frameWidth", "frameWidth",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFrameConfig.frameWidth"), true) {

			@Override
			public Double getValue() {
				return (double) getAsset().getFrameWidth();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getAsset().setFrameWidth(value.intValue());
				getAsset().build(new ArrayList<>());

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
				return (double) getAsset().getFrameHeight();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getAsset().setFrameHeight(value.intValue());
				getAsset().build(new ArrayList<>());

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
				return (double) getAsset().getStartFrame();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getAsset().setStartFrame(value.intValue());
				getAsset().build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
				
			}

			@Override
			public boolean isModified() {
				return getAsset().getEndFrame() != -1;
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
				return (double) getAsset().getEndFrame();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getAsset().setEndFrame(value.intValue());
				getAsset().build(new ArrayList<>());
				
				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getAsset().getEndFrame() != -1;
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
				return (double) getAsset().getSpacing();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getAsset().setSpacing(value.intValue());
				getAsset().build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getAsset().getSpacing() != 0;
			}
		});

		section.add(new PGridNumberProperty("margin", "margin",
				help.getMemberHelp("Phaser.Loader.FileTypes.ImageFrameConfig.margin"), true) {

			@Override
			public Double getValue() {
				return (double) getAsset().getMargin();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getAsset().setMargin(value.intValue());
				getAsset().build(new ArrayList<>());

				if (notify) {
					updateEditorFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getAsset().getMargin() != 0;
			}
		});

		getSections().add(section);

	}

	protected static void updateEditorFromPropertyChange() {
		AssetPackEditor editor = (AssetPackEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		editor.refresh();
	}

}