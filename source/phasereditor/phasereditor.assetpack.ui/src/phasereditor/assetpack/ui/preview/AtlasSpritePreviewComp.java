// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.ui.preview;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.atlas.ui.AtlasCanvas;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;

public class AtlasSpritePreviewComp extends AtlasCanvas {
	static final Object NO_SELECTION = "none";

	private Frame _model;

	public AtlasSpritePreviewComp(Composite parent, int style) {
		super(parent, style);

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		DragSource dragSource = new DragSource(this, DND.DROP_MOVE | DND.DROP_DEFAULT);
		dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer() });
		dragSource.addDragListener(new DragSourceAdapter() {

			@Override
			public void dragStart(DragSourceEvent event) {
				ISelection sel = getSelection();
				if (sel.isEmpty()) {
					event.doit = false;
					return;
				}
				event.image = AssetLabelProvider.GLOBAL_48.getImage(((StructuredSelection) sel).getFirstElement());
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				transfer.setSelection(sel);
			}

			private ISelection getSelection() {
				return new StructuredSelection(getModel());
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = getModel().getKey();
			}
		});
	}

	public void setModel(AtlasAssetModel.Frame model) {
		_model = model;
		AtlasAssetModel atlas = model.getAsset();
		String url = atlas.getTextureURL();
		IFile file = atlas.getFileFromUrl(url);
		setImageFile(file);
		setFrame(model);
		setSingleFrame(true);
		redraw();
		
		Image img = getImage();
		if (img != null) {
			Rectangle b = img.getBounds();
			String str = "Sprite Size: " + _model.getSpriteW() + "x" + _model.getSpriteH() + "\n";
			str += "Image Size: " + b.width + "x" + b.height + "\n";
			str += "Image URL: " + getModel().getAsset().getTextureURL();
			setToolTipText(str);
		}
	}

	public AtlasAssetModel.Frame getModel() {
		return _model;
	}

	public void createToolBar(IToolBarManager toolbar) {
		toolbar.add(new ImageCanvas_Zoom_1_1_Action(this));
		toolbar.add(new ImageCanvas_Zoom_FitWindow_Action(this));
	}

}
