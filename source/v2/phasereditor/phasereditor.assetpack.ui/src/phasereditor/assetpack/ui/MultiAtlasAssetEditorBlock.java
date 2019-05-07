// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.assetpack.ui;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.eclipse.swt.graphics.RGB;

import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.ui.preview.MultiAtlasAssetFrameProvider;
import phasereditor.ui.FrameGridCellRenderer;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.IEditorBlock;

/**
 * @author arian
 *
 */
public class MultiAtlasAssetEditorBlock extends AssetKeyEditorBlock<MultiAtlasAssetModel> {

	private List<IEditorBlock> _children;

	public MultiAtlasAssetEditorBlock(MultiAtlasAssetModel asset) {
		super(asset);

		_children = asset.getSubElements().stream()

				.map(frame -> AssetPackUI.getAssetEditorBlock(frame))

				.collect(toList());
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public List<IEditorBlock> getChildren() {
		return _children;
	}

	@Override
	public ICanvasCellRenderer getRenderer() {
		return new FrameGridCellRenderer(new MultiAtlasAssetFrameProvider(getAssetKey()), 8);
	}

	@Override
	public String getSortName() {
		return "Atlas";
	}

	@Override
	public RGB getColor() {
		return AssetFrameEditorBlock.TEXTURE_COLOR;
	}

}
