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
package phasereditor.scene.ui.editor.properties;

import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvasDialog;
import phasereditor.ui.TreeCanvasViewer;

public class QuickSelectAssetDialog extends TreeCanvasDialog {

	public QuickSelectAssetDialog(Shell shell) {
		super(shell);
	}

	@Override
	protected TreeCanvasViewer createViewer(TreeCanvas tree) {
		var viewer = new AssetsTreeCanvasViewer(tree, new AssetsContentProvider(), AssetLabelProvider.GLOBAL_16);

		tree.addMouseListener(MouseListener.mouseDoubleClickAdapter(e -> {
			setResult(getViewer().getStructuredSelection().getFirstElement());
			close();
		}));

		AssetPackUI.installAssetTooltips(tree, tree.getUtils());
		
		return viewer;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// no buttons
	}

}