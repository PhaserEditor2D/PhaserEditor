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
package phasereditor.canvas.ui.editors.outline;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.TilemapSpriteModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.ImageTreeCanvasItemRenderer;
import phasereditor.ui.TreeCanvasViewer;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class CanvasOutline extends Page implements IContentOutlinePage {

	private CanvasEditor _editor;
	private FilteredTreeCanvas _filterTree;
	private TreeCanvasViewer _viewer;

	public CanvasOutline(CanvasEditor editor) {
		_editor = editor;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		// TODO Auto-generated method stub
	}

	@Override
	public ISelection getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setSelection(ISelection selection) {
		// TODO Auto-generated method stub

	}

	public CanvasEditor getEditor() {
		return _editor;
	}

	@Override
	public void createControl(Composite parent) {
		_filterTree = new FilteredTreeCanvas(parent, SWT.NONE);
		_viewer = new TreeCanvasViewer(_filterTree.getTree(), new OutlineContentProvider2(),
				new OutlineLabelProvider2(_editor)) {

			@Override
			protected void setItemIconProperties(TreeCanvasItem item) {
				super.setItemIconProperties(item);

				var data = item.getData();

				if (data instanceof TilemapSpriteModel) {
					var img = getEditor().getCanvas2().getWorldRenderer().getTilemapImage((TilemapSpriteModel) data);
					if (img != null) {
						item.setRenderer(new ImageTreeCanvasItemRenderer(item, img));
					}
				} else if (data instanceof BaseObjectModel) {

					if (data instanceof AssetSpriteModel<?>) {
						var asset = ((AssetSpriteModel<?>) data).getAssetKey();

						if (asset instanceof IAssetFrameModel) {
							var renderer = AssetsTreeCanvasViewer.createImageRenderer(item, asset);
							if (renderer != null) {
								item.setRenderer(renderer);
							}
						}
					}

				}

			}

		};
		_viewer.setInput(_editor.getCanvas2());
	}

	@Override
	public Control getControl() {
		return _filterTree;
	}

	@Override
	public void setFocus() {
		_filterTree.getTree().setFocus();
	}

	@Override
	public void dispose() {

		_editor.setOutline2(null);

		super.dispose();
	}

}
