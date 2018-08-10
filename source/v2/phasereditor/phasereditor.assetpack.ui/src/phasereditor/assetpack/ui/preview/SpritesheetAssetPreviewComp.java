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

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.widgets.SpritesheetPreviewCanvas;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FrameGridCanvas;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.IFrameProvider;
import phasereditor.ui.IZoomable;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;

@SuppressWarnings("synthetic-access")
public class SpritesheetAssetPreviewComp extends Composite {

	SpritesheetPreviewCanvas _sheetCanvas;

	private FrameGridCanvas _gridCanvas;

	private Action _textureAction;

	private Action _tilesAction;

	private Action _listAction;

	public SpritesheetAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		setLayout(new StackLayout());

		_sheetCanvas = new SpritesheetPreviewCanvas(this, SWT.NONE);

		DragSource dragSource = new DragSource(_sheetCanvas, DND.DROP_MOVE | DND.DROP_DEFAULT);
		dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer() });
		dragSource.addDragListener(new DragSourceAdapter() {

			@Override
			public void dragStart(DragSourceEvent event) {
				ISelection sel = getSelection();
				if (sel.isEmpty()) {
					event.doit = false;
					return;
				}

				var frame = (SpritesheetAssetModel.FrameModel) ((StructuredSelection) sel).getFirstElement();

				AssetPackUI.set_DND_Image(event, frame);

				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				transfer.setSelection(sel);
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				if (event.image != null) {
					event.image.dispose();
				}
			}

			private ISelection getSelection() {
				return new StructuredSelection(AssetPackCore.sortAssets(getSelectedFrames()));
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				List<FrameModel> frames = getSelectedFrames();
				if (!frames.isEmpty()) {
					event.data = frames.get(0).getName();
				}
			}
		});

		_gridCanvas = new FrameGridCanvas(this, SWT.NONE, true);

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {
		// nothing
	}

	private SpritesheetAssetModel _model;

	private ImageCanvas_Zoom_1_1_Action _zoom_1_1_action;

	private ImageCanvas_Zoom_FitWindow_Action _zoom_fitWindow_action;

	public void setModel(SpritesheetAssetModel model) {
		_model = model;

		IFile imgFile = model.getUrlFile();

		{
			// sprite canvas
			_sheetCanvas.setSpritesheet(model);

			_sheetCanvas.setImageFile(imgFile);

			String str = "Frames Size: " + model.getFrameWidth() + "x" + model.getFrameHeight();
			if (_sheetCanvas.getImage() != null) {
				str += "\n";
				Rectangle b = _sheetCanvas.getImage().getBounds();
				str += "Image Size: " + b.width + "x" + b.height + "\n";
				str += "Image URL: " + model.getUrl();
			}
			_sheetCanvas.setToolTipText(str);
		}

		{
			// frames
			var provider = _model == null ? IFrameProvider.NULL : new SpritesheetAssetFrameProvider(_model);
			_gridCanvas.loadFrameProvider(provider);
		}

		((StackLayout) getLayout()).topControl = _sheetCanvas;

		layout();

		updateActionsState();
	}

	public SpritesheetAssetModel getModel() {
		return _model;
	}

	private void moveTop(Control control) {
		StackLayout layout = (StackLayout) getLayout();
		layout.topControl = control;
		layout();

		swtRun(this::updateActionsState);

		control.setFocus();
	}

	private void updateActionsState() {
		StackLayout layout = (StackLayout) getLayout();
		Control control = layout.topControl;

		_zoom_1_1_action.setEnabled(control == _sheetCanvas);

		_tilesAction.setChecked(control == _gridCanvas && !_gridCanvas.isListLayout());
		_listAction.setChecked(control == _gridCanvas && _gridCanvas.isListLayout());
		_textureAction.setChecked(control == _sheetCanvas);
	}

	public void createToolBar(IToolBarManager toolbar) {

		_tilesAction = new Action("Tiles", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_APPLICATION_TILE));
			}

			@Override
			public void run() {
				moveTop(_gridCanvas);
				_gridCanvas.setListLayout(false);
			}
		};
		_textureAction = new Action("Texture", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_IMAGES));
			}

			@Override
			public void run() {
				moveTop(_sheetCanvas);
			}
		};
		_listAction = new Action("List", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_APPLICATION_LIST));
			}

			@Override
			public void run() {
				moveTop(_gridCanvas);
				if (_gridCanvas.getFrameSize() < 32) {
					_gridCanvas.setFrameSize(32);
				}
				_gridCanvas.setListLayout(true);
			}
		};

		toolbar.add(_textureAction);
		toolbar.add(_tilesAction);
		toolbar.add(_listAction);

		toolbar.add(new Separator());

		_zoom_1_1_action = new ImageCanvas_Zoom_1_1_Action(_sheetCanvas);
		_zoom_fitWindow_action = new ImageCanvas_Zoom_FitWindow_Action() {
			@Override
			public IZoomable getImageCanvas() {
				var top = ((StackLayout) getLayout()).topControl;
				return (IZoomable) top;
			}
		};

		toolbar.add(_zoom_1_1_action);
		toolbar.add(_zoom_fitWindow_action);

	}

	List<FrameModel> getSelectedFrames() {
		return _sheetCanvas.getSelectedFrames();
	}
}
