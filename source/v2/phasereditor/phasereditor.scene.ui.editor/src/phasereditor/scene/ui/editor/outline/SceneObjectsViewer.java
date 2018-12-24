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

import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.ObjectModel;
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

		if (!_editor.getScene().isRendered()) {
			return;
		}

		var data = item.getData();

		var sceneRenderer = _editor.getScene().getSceneRenderer();
		var finder = _editor.getScene().getAssetFinder();

		if (data instanceof BitmapTextModel) {
			var model = (BitmapTextModel) data;

			var image = sceneRenderer.getBitmapTextImage(model);

			if (image != null) {
				// TODO: 99866554
				// item.setRenderer(new ImageTreeCanvasItemRenderer(item, image, FrameData.fromImage(image)));
			}
		}

		if (data instanceof TileSpriteModel) {
			var model = (TileSpriteModel) data;

			var image = sceneRenderer.getTileSpriteTextImage(model);

			if (image != null) {
				//TODO: 8883665366
//				item.setRenderer(new ImageTreeCanvasItemRenderer(item, image, FrameData.fromImage(image)));
			}
		}

		if (data instanceof TextureComponent) {
			var frame = TextureComponent.utils_getTexture((ObjectModel) data, finder);
			var renderer = AssetsTreeCanvasViewer.createImageRenderer(item, frame);
			if (renderer != null) {
				item.setRenderer(renderer);
			}
		}

		if (data instanceof ObjectModel) {
			var model = (ObjectModel) data;
			var type = model.getType();
			item.setKeywords(type);
		}
	}
}