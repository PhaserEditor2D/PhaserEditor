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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.atlas.ui.AtlasCanvas;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredFrameGrid;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.IZoomable;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;

@SuppressWarnings("synthetic-access")
public class AtlasAssetPreviewComp extends Composite {
	static final Object NO_SELECTION = "none";

	private AtlasCanvas _atlasCanvas;
	private AtlasAssetModel _model;

	private Action _textureAction;

	private Action _tilesAction;

	private Action _listAction;

	private ImageCanvas_Zoom_1_1_Action _zoom_1_1_action;

	private ImageCanvas_Zoom_FitWindow_Action _zoom_fitWindow_action;

	private FilteredFrameGrid _filteredGrid;

	public AtlasAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		setLayout(new StackLayout());

		_atlasCanvas = new AtlasCanvas(this, SWT.NONE, true);
		_filteredGrid = new FilteredFrameGrid(this, SWT.NONE, true);

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		moveTop(_filteredGrid);
	}

	private void moveTop(Control control) {
		StackLayout layout = (StackLayout) getLayout();
		layout.topControl = control;
		layout();

		swtRun(this::updateActionsState);

		control.setFocus();
	}

	private void updateActionsState() {
		if (_zoom_1_1_action == null) {
			return;
		}

		StackLayout layout = (StackLayout) getLayout();
		Control control = layout.topControl;
		_zoom_1_1_action.setEnabled(control == _atlasCanvas);
		_zoom_fitWindow_action.setEnabled(control == _atlasCanvas);

		_tilesAction.setChecked(control == _filteredGrid && !_filteredGrid.getCanvas().isListLayout());
		_listAction.setChecked(control == _filteredGrid && _filteredGrid.getCanvas().isListLayout());
		_textureAction.setChecked(control == _atlasCanvas);
	}

	public void setModel(AtlasAssetModel model) {
		_model = model;
		String url = model.getTextureURL();
		IFile file = model.getFileFromUrl(url);
		_atlasCanvas.setImageFile(file);

		List<Frame> frames = model.getAtlasFrames();

		_atlasCanvas.setFrames(frames);
		_atlasCanvas.redraw();

		_filteredGrid.getCanvas().loadFrameProvider(new AtlasAssetFramesProvider(model));
		_filteredGrid.getCanvas().resetZoom();
	}

	public AtlasAssetModel getModel() {
		return _model;
	}

	public AtlasCanvas getAtlasCanvas() {
		return _atlasCanvas;
	}

	public void createToolBar(IToolBarManager toolbar) {

		_tilesAction = new Action("Tiles", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_APPLICATION_TILE));
			}

			@Override
			public void run() {
				moveTop(_filteredGrid);
				_filteredGrid.getCanvas().setListLayout(false);
			}
		};
		_textureAction = new Action("Texture", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_IMAGES));
			}

			@Override
			public void run() {
				moveTop(_atlasCanvas);
			}
		};
		_listAction = new Action("List", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_APPLICATION_LIST));
			}

			@Override
			public void run() {
				moveTop(_filteredGrid);
				var canvas = _filteredGrid.getCanvas();
				if (canvas.getFrameSize() < 32) {
					canvas.setFrameSize(32);
				}
				canvas.setListLayout(true);
			}
		};

		toolbar.add(_tilesAction);
		toolbar.add(_listAction);
		toolbar.add(_textureAction);

		toolbar.add(new Separator());

		_zoom_1_1_action = new ImageCanvas_Zoom_1_1_Action(_atlasCanvas);
		_zoom_fitWindow_action = new ImageCanvas_Zoom_FitWindow_Action() {
			@Override
			public IZoomable getImageCanvas() {
				Control top = ((StackLayout) getLayout()).topControl;
				if (top instanceof IZoomable) {
					return (IZoomable) top;
				}
				return _atlasCanvas;
			}
		};
		toolbar.add(_zoom_1_1_action);
		toolbar.add(_zoom_fitWindow_action);

		updateActionsState();
	}

}
