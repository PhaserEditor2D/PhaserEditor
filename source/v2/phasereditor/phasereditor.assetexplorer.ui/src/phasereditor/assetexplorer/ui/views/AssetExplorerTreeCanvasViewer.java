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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import phasereditor.assetexplorer.ui.views.newactions.NewAnimationWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewAssetPackWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewAtlasWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewCanvasWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewWizardLancher;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FrameData;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.IconType;
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
	protected void setItemIconProperties(TreeCanvasItem item, Object elem) {

		if (elem instanceof CanvasFile) {
			var file = ((CanvasFile) elem).getFile();
			var imgFile = CanvasUI.getCanvasScreenshotFile(file, false);

			if (imgFile != null) {
				item.setImageFile(imgFile.toFile());
				var img = getCanvas().loadImage(item.getImageFile());
				item.setFrameData(FrameData.fromImage(img));
			}

			item.setLabel(file.getName());
			item.setIconType(IconType.IMAGE_FRAME);

		} else {
			super.setItemIconProperties(item, elem);
		}

		if (elem == AssetExplorer.CANVAS_NODE

				|| elem instanceof CanvasType

				|| elem == AssetExplorer.ANIMATIONS_NODE

				|| elem == AssetExplorer.ATLAS_NODE

				|| elem == AssetExplorer.PACK_NODE

				|| elem == AssetExplorer.PROJECTS_NODE) {
			item.setIcon(null);
		}

	}

	@Override
	protected void setItemProperties(TreeCanvasItem item, Object elem) {
		super.setItemProperties(item, elem);

		var actions = item.getActions();

		if (elem == AssetExplorer.CANVAS_NODE) {
			item.setHeader(true);
		}

		if (elem instanceof CanvasType) {
			actions.add(new NewWizardLauncherTreeItemAction(new NewCanvasWizardLauncher((CanvasType) elem)));
			item.setHeader(true);
		}

		if (elem == AssetExplorer.ANIMATIONS_NODE) {
			actions.add(new NewWizardLauncherTreeItemAction(new NewAnimationWizardLauncher()));
			item.setHeader(true);
		}

		if (elem == AssetExplorer.ATLAS_NODE) {
			actions.add(new NewWizardLauncherTreeItemAction(new NewAtlasWizardLauncher()));
			item.setHeader(true);
		}

		if (elem == AssetExplorer.PACK_NODE) {
			actions.add(new NewWizardLauncherTreeItemAction(new NewAssetPackWizardLauncher()));
			item.setHeader(true);
		}

		if (elem == AssetExplorer.PROJECTS_NODE) {
			// actions.add(new NewWizardLauncherTreeItemAction(new
			// NewAssetPackWizardLauncher()));
			item.setHeader(true);
		}

		if (elem instanceof CanvasFile) {
			var canvasFile = (CanvasFile) elem;
			actions.add(new TreeCanvasItemAction(EditorSharedImages.getImage(IMG_GENERIC_EDITOR), "Open source file.") {
				@Override
				public void run(MouseEvent event) {
					var file = canvasFile.getFile();
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

	class NewWizardLauncherTreeItemAction extends TreeCanvasItemAction {
		private NewWizardLancher _launcher;

		public NewWizardLauncherTreeItemAction(NewWizardLancher launcher) {
			super(EditorSharedImages.getImage(IMG_ADD), launcher.getLabel());
			_launcher = launcher;
		}

		@Override
		public void run(MouseEvent event) {
			AssetExplorerContentProvider provider = (AssetExplorerContentProvider) getContentProvider();
			_launcher.openWizard(provider.getProjectInContent());
		}

	}

}
