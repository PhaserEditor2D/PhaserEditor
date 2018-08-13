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

import phasereditor.assetexplorer.ui.views.newactions.NewCanvasWizardLauncher;
import phasereditor.assetexplorer.ui.views.newactions.NewWizardLancher;
import phasereditor.assetpack.ui.AssetsJFaceTreeCanvasAdapter;
import phasereditor.canvas.core.CanvasType;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.TreeCanvas.TreeCanvasItemAction;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class AssetExplorerJFaceTreeCanvasAdapter extends AssetsJFaceTreeCanvasAdapter {

	public AssetExplorerJFaceTreeCanvasAdapter(ITreeContentProvider contentProvider, LabelProvider labelProvider) {
		super(contentProvider, labelProvider);
	}

	@Override
	protected void setItemProperties(TreeCanvasItem item, Object elem) {
		super.setItemProperties(item, elem);

		var actions = item.getActions();

		if (elem instanceof CanvasType) {
			actions.add(new NewWizardLauncherTreeItemAction(new NewCanvasWizardLauncher((CanvasType) elem)));
		}

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
