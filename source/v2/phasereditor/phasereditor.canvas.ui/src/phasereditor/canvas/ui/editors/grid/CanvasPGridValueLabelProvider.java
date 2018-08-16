// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors.grid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.PhysicsSortDirection;
import phasereditor.canvas.core.PhysicsType;
import phasereditor.canvas.core.StateSettings.LoadPack;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.project.core.codegen.SourceLang;
import phasereditor.ui.ColorButtonSupport;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.properties.PGrid;
import phasereditor.ui.properties.PGridColorProperty;
import phasereditor.ui.properties.PGridEnumProperty;
import phasereditor.ui.properties.PGridProperty;
import phasereditor.ui.properties.PGridValueLabelProvider;

/**
 * @author arian
 *
 */
public class CanvasPGridValueLabelProvider extends PGridValueLabelProvider {
	private ObjectCanvas _canvas;

	public CanvasPGridValueLabelProvider(PGrid grid) {
		super(grid);
	}

	public void setCanvas(ObjectCanvas canvas) {
		_canvas = canvas;
	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected String getPropertyValueLabel(PGridProperty element) {

		if (element instanceof PGridLoadPackProperty) {
			Set<LoadPack> value = (Set<LoadPack>) getPropertyValue(element);
			return LoadPack.toString(value);
		}

		if (element instanceof PGridColorProperty) {
			PGridColorProperty prop = (PGridColorProperty) element;
			RGB rgb = (RGB) getPropertyValue(prop);

			if (rgb == null) {
				return "";
			}

			if (prop.getDefaultRGB() != null && prop.getDefaultRGB().equals(rgb)) {
				return "(default)";
			}

			return ColorButtonSupport.getHexString(rgb);
		}

		if (element instanceof PGridFrameProperty) {
			String label = ((PGridFrameProperty) element).getLabel();
			return label == null ? "" : label;
		}

		if (element instanceof PGridAnimationsProperty) {
			List<AnimationModel> value = (List<AnimationModel>) getPropertyValue(element);
			return PGridAnimationsProperty.getLabel(value);
		}

		if (element instanceof PGridEnumProperty) {
			Object value = getPropertyValue(element);

			if (value instanceof PhysicsType) {
				return ((PhysicsType) value).getPhaserName();
			}

			if (value instanceof SourceLang) {
				return ((SourceLang) value).getDisplayName();
			}

			if (value instanceof PhysicsSortDirection) {
				return ((PhysicsSortDirection) value).getPhaserName();
			}
		}

		if (element instanceof PGridSpriteProperty) {
			PGridSpriteProperty prop = (PGridSpriteProperty) element;
			String id = (String) getPropertyValue(prop);
			BaseObjectModel model = _canvas.getWorldModel().findById(id);

			if (model == null) {
				return id;
			}

			return model.getEditorName();
		}

		if (element instanceof PGridBitmapTextFontProperty) {
			BitmapFontAssetModel asset = (BitmapFontAssetModel) getPropertyValue(element);
			if (asset == null) {
				return "null";
			}

			return asset.getKey();
		}

		Object value = super.getPropertyValueLabel(element);
		return value == null ? "" : value.toString().replace("\n\r", "").replace("\n", "");
	}

	private Map<Object, Image> _images = new HashMap<>();

	@Override
	public Image getImage(Object element) {
		if (element instanceof PGridColorProperty) {
			PGridColorProperty prop = (PGridColorProperty) element;

			if (!prop.isModified()) {
				return null;
			}

			RGB value = (RGB) getPropertyValue(prop);
			if (value == null) {
				return null;
			}
			if (_images.containsKey(value)) {
				return _images.get(value);
			}
			Image image = PhaserEditorUI.makeColorIcon(value);
			_images.put(value, image);
			return image;
		}

		if (element instanceof PGridFrameProperty) {
			return AssetLabelProvider.GLOBAL_16.getImage(getPropertyValue((PGridFrameProperty) element));
		}

		return super.getImage(element);
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Image img : _images.values()) {
			img.dispose();
		}
		_images.clear();
	}
}
