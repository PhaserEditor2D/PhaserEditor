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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.ui.Colors;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.IEditorBlock;
import phasereditor.ui.ImageProxy;

/**
 * @author arian
 *
 */
public class SpritesheetAssetEditorBlock extends AssetKeyEditorBlock<SpritesheetAssetModel> {

	private List<IEditorBlock> _children;

	public SpritesheetAssetEditorBlock(SpritesheetAssetModel assetKey) {
		super(assetKey);
		_children = assetKey.getFrames().stream()

				.map(frame -> AssetPackUI.getAssetEditorBlock(frame))

				.collect(toList());
		
		if (_children.size() > 50) {
			_children = _children.subList(0, 50);
		}
	}
	
	@Override
	public String getKeywords() {
		return "spritesheet texture";
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
		return new ICanvasCellRenderer() {

			@Override
			public void render(Canvas canvas, GC gc, int x, int y, int width, int height) {
				var file = getAssetKey().getUrlFile();
				var proxy = ImageProxy.get(file, null);
				if (proxy != null) {
					proxy.paintScaledInArea(gc, new Rectangle(x, y, width, height));
				}
			}
		};
	}

	@Override
	public String getSortName() {
		return "003";
	}

	@Override
	public RGB getColor() {
		return Colors.YELLOWGREEN.rgb;
	}

}
