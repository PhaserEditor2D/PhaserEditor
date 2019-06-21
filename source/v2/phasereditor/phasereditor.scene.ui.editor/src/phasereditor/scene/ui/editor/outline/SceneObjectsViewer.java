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
package phasereditor.scene.ui.editor.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.assetpack.ui.BitmapFontTreeCanvasRenderer;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvasViewer;

public class SceneObjectsViewer extends TreeCanvasViewer {
	private SceneEditor _editor;

	public SceneObjectsViewer(TreeCanvas canvas, SceneEditor editor, ITreeContentProvider contentProvider) {
		super(canvas, contentProvider, new SceneOutlineLabelProvider(editor));

		_editor = editor;

	}

	@Override
	protected void setItemIconProperties(TreeCanvasItem item) {
		super.setItemIconProperties(item);

		var data = item.getData();

		var finder = getFinder();

		if (data instanceof BitmapTextModel) {
			item.setRenderer(new BitmapFontTreeCanvasRenderer(item) {
				@Override
				protected BitmapFontAssetModel getBitmapFontAsset() {
					var model = (BitmapTextModel) _item.getData();
					var fontAsset = BitmapTextComponent.utils_getFont(model, finder);
					return fontAsset;
				}

				@Override
				protected String getPreviewText() {
					var model = (BitmapTextModel) _item.getData();
					var text = TextualComponent.get_text(model);
					return text;
				}
			});

		} else if (data instanceof TileSpriteModel) {
			item.setRenderer(new TileSpriteTreeItemRenderer(item, getFinder()));
		} else if (data instanceof TextureComponent) {
			var frame = TextureComponent.utils_getTexture((ObjectModel) data, finder);
			var renderer = AssetsTreeCanvasViewer.createImageRenderer(item, frame);
			if (renderer != null) {
				item.setRenderer(renderer);
			}
		} else if (data instanceof ObjectModel) {
			var model = (ObjectModel) data;
			var type = model.getType();
			item.setKeywords(type);
		}
	}

	private AssetFinder getFinder() {
		return AssetPackCore.getAssetFinder(_editor.getProject());
	}
}