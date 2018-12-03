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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import phasereditor.assetexplorer.ui.views.newactions.NewAnimationWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewAssetPackWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewAtlasWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewExampleProjectWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewProjectWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewSceneWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewWizardLancher;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.scene.core.SceneFile;
import phasereditor.scene.ui.SceneUI;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IconTreeCanvasItemRenderer;
import phasereditor.ui.ImageTreeCanvasItemRenderer;
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
		
		SceneUI.installSceneTooltips(this);
	}

	@Override
	protected void setItemIconProperties(TreeCanvasItem item) {
		var elem = item.getData();

		if (elem instanceof SceneFile) {
			var file = ((SceneFile) elem).getFile();
			var imgFile = SceneUI.getSceneScreenshotFile(file, false);

			if (imgFile == null) {
				super.setItemIconProperties(item);
			} else {
				var img = getTree().loadImage(imgFile.toFile());
				item.setRenderer(new ImageTreeCanvasItemRenderer(item, img));
			}

			item.setLabel(file.getName());
		} else {
			super.setItemIconProperties(item);
		}

		if (elem == AssetsView.SCENES_NODE

				|| elem == AssetsView.ANIMATIONS_NODE

				|| elem == AssetsView.ATLAS_NODE

				|| elem == AssetsView.PACK_NODE

				|| elem == AssetsView.PROJECTS_NODE) {
			item.setRenderer(new IconTreeCanvasItemRenderer(item, null));
		}

	}

	@Override
	protected void setItemProperties(TreeCanvasItem item) {
		super.setItemProperties(item);

		var elem = item.getData();

		var actions = new ArrayList<TreeCanvasItemAction>();
		item.setActions(actions);

		if (elem == AssetsView.SCENES_NODE) {
			actions.add(new NewWizardLauncherTreeItemAction(IMG_NEW_CANVAS, new NewSceneWizardLauncher()));
			item.setHeader(true);
		} else if (elem == AssetsView.ANIMATIONS_NODE) {
			actions.add(new NewWizardLauncherTreeItemAction(IMG_NEW_FRAME_ANIMATION, new NewAnimationWizardLauncher()));
			item.setHeader(true);
		} else if (elem == AssetsView.ATLAS_NODE) {
			actions.add(new NewWizardLauncherTreeItemAction(IMG_NEW_ATLAS, new NewAtlasWizardLauncher()));
			item.setHeader(true);
		} else if (elem == AssetsView.PACK_NODE) {
			actions.add(new NewWizardLauncherTreeItemAction(IMG_NEW_BOX, new NewAssetPackWizardLauncher()));
			item.setHeader(true);
		} else if (elem == AssetsView.PROJECTS_NODE) {
			actions.add(new NewWizardLauncher_Menu_TreeItemAction(IMG_NEW_PHASER_PROJECT, "New Project...",
					new NewProjectWizardLauncher(), new NewExampleProjectWizardLauncher()));
			item.setHeader(true);
		}

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

	class NewWizardLauncherTreeItemAction extends TreeCanvasItemAction {
		private NewWizardLancher _launcher;

		public NewWizardLauncherTreeItemAction(String icon, NewWizardLancher launcher) {
			super(EditorSharedImages.getImage(icon), launcher.getLabel());
			_launcher = launcher;
		}

		@Override
		public void run(MouseEvent event) {
			AssetExplorerContentProvider provider = (AssetExplorerContentProvider) getContentProvider();
			_launcher.openWizard(provider.getProjectInContent());
		}

	}

	class NewWizardLauncher_Menu_TreeItemAction extends TreeCanvasItemAction {
		private NewWizardLancher[] _launchers;

		public NewWizardLauncher_Menu_TreeItemAction(String icon, String label, NewWizardLancher... launchers) {
			super(EditorSharedImages.getImage(icon), label);
			_launchers = launchers;
		}

		@Override
		public void run(MouseEvent event) {
			AssetExplorerContentProvider provider = (AssetExplorerContentProvider) getContentProvider();
			IProject project = provider.getProjectInContent();

			MenuManager manager = new MenuManager();

			for (var launcher : _launchers) {
				manager.add(new Action(launcher.getLabel()) {
					@Override
					public void run() {
						launcher.openWizard(project);
					}
				});
			}

			var canvas = getTree();
			var menu = manager.createContextMenu(canvas);
			menu.setVisible(true);
		}

	}

}
