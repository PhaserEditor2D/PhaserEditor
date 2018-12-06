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
package phasereditor.assetpack.ui.editors;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.preview.AssetFramesProvider;
import phasereditor.ui.FrameGridCanvas;

/**
 * @author arian
 *
 */
public class ManyTexturesSection extends BaseAssetPackEditorSection<IAssetKey> {

	public ManyTexturesSection(AssetPackEditorPropertyPage page) {
		super(page, "Textures Preview");
		setFillSpace(true);
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number > 1;
	}

	@Override
	public boolean canEdit(Object obj) {
		return canEdit2(obj);
	}

	public static boolean canEdit2(Object obj) {
		return obj instanceof IAssetFrameModel || obj instanceof AtlasAssetModel || obj instanceof MultiAtlasAssetModel
				|| obj instanceof SpritesheetAssetModel || obj instanceof ImageAssetModel;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new FrameGridCanvas(parent, 0, true);
		addUpdate(() -> {
			List<? extends IAssetFrameModel> frames = getModels().stream().flatMap(obj -> {
				if (obj instanceof IAssetFrameModel) {
					return List.of(obj).stream();
				}
				return obj.getAsset().getSubElements().stream();
			}).map(o -> (IAssetFrameModel) o).collect(toList());
			comp.loadFrameProvider(new AssetFramesProvider(frames));
		});

		return comp;
	}
}
