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
package phasereditor.assetexplorer.ui.views;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import phasereditor.assetpack.core.SceneFileAssetModel;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneFile;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IconTreeCanvasItemRenderer;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.ImageProxyTreeCanvasItemRenderer;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvas.TreeCanvasItemAction;

/**
 * @author arian
 *
 */
public class AssetExplorerTreeCanvasViewer extends AssetsTreeCanvasViewer {

	public AssetExplorerTreeCanvasViewer(TreeCanvas canvas, ITreeContentProvider contentProvider,
			LabelProvider labelProvider) {
		super(canvas, contentProvider, labelProvider);
	}

	@Override
	protected void setItemIconProperties(TreeCanvasItem item) {
		var elem = item.getData();

		if (elem instanceof SceneFileAssetModel) {

			var asset = (SceneFileAssetModel) elem;

			var imgFile = SceneCore.getSceneScreenshotFile(asset);

			if (imgFile == null) {
				super.setItemIconProperties(item);
			} else {
				item.setRenderer(new ImageProxyTreeCanvasItemRenderer(item, ImageProxy.get(imgFile, null)));
			}

			item.setLabel(asset.getKey());

		} else if (elem instanceof SceneFile) {

			var file = ((SceneFile) elem).getFile();
			var imgFile = SceneCore.getSceneScreenshotFile(file);

			if (imgFile == null) {
				super.setItemIconProperties(item);
			} else {
				item.setRenderer(new ImageProxyTreeCanvasItemRenderer(item, ImageProxy.get(imgFile.toFile(), null)));
			}

			item.setLabel(file.getName());

		} else {
			super.setItemIconProperties(item);
		}

		if (elem == AssetsView.SCENES_NODE

				|| elem == AssetsView.ANIMATIONS_NODE

				|| elem == AssetsView.ATLAS_NODE

				|| elem == AssetsView.PACK_NODE

		) {
			item.setRenderer(new IconTreeCanvasItemRenderer(item));
		}

	}

	@Override
	protected void setItemProperties(TreeCanvasItem item) {
		super.setItemProperties(item);

		var elem = item.getData();

		var actions = new ArrayList<TreeCanvasItemAction>();
		item.setActions(actions);

		if (elem instanceof SceneFile) {
			var sceneFile = (SceneFile) elem;
			actions.add(new TreeCanvasItemAction(EditorSharedImages.getImage(IMG_GENERIC_EDITOR), "Open source file.") {
				@Override
				public void run(MouseEvent event) {
					var file = sceneFile.getFile();
					var openFile = file.getProject()
							.getFile(file.getProjectRelativePath().removeFileExtension().addFileExtension("ts"));

					if (!openFile.exists()) {
						openFile = file.getProject()
								.getFile(file.getProjectRelativePath().removeFileExtension().addFileExtension("js"));
						if (openFile.exists()) {
							try {
								IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
										openFile);
							} catch (PartInitException e) {
								throw new RuntimeException(e);
							}
						}
					}

				}
			});
		}

		item.setParentByNature(item.isHeader());

	}

}
