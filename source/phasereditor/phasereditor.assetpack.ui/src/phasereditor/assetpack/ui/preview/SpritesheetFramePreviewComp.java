// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.assetpack.ui.widgets.SpritesheetPreviewCanvas;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;

/**
 * @author arian
 *
 */
public class SpritesheetFramePreviewComp extends SpritesheetPreviewCanvas {

	private FrameModel _model;

	public SpritesheetFramePreviewComp(Composite parent, int style) {
		super(parent, style);

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
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				transfer.setSelection(sel);
			}

			private ISelection getSelection() {
				return new StructuredSelection(getModel());
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = getSpritesheet().getKey() + " - " + getModel().getKey();
			}
		});

	}

	public void setModel(SpritesheetAssetModel.FrameModel model) {
		_model = model;
		setSpritesheet(model.getAsset());
		IFile file = model.getAsset().getUrlFile();
		setImageFile(file);
		setFrame(model.getIndex());
		setSingleFrame(true);

		if (getImage() != null) {
			SpritesheetAssetModel sheet = model.getAsset();
			String str = "Frames Size: " + sheet.getFrameWidth() + "x" + sheet.getFrameHeight() + "\n";
			Rectangle b = getImage().getBounds();
			str += "Image Size: " + b.width + "x" + b.height + "\n";
			str += "Image URL: " + sheet.getUrl();
			setToolTipText(str);
		}

		getDisplay().asyncExec(() -> {
			fitWindow();
			redraw();
		});
	}

	public FrameModel getModel() {
		return _model;
	}

	public void createToolBar(IToolBarManager toolbar) {
		toolbar.add(new ImageCanvas_Zoom_1_1_Action(this));
		toolbar.add(new ImageCanvas_Zoom_FitWindow_Action(this));
	}
}
