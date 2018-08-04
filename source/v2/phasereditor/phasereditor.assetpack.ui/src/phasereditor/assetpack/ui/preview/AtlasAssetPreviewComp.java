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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.FilteredTree;
import org.json.JSONArray;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.atlas.ui.AtlasCanvas;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FrameGridCanvas;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.IZoomable;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;
import phasereditor.ui.PatternFilter2;

@SuppressWarnings("synthetic-access")
public class AtlasAssetPreviewComp extends Composite {
	static final Object NO_SELECTION = "none";

	private FilteredTree _spritesList;
	private AtlasCanvas _canvas;
	private AtlasAssetModel _model;
	private FrameGridCanvas _gridCanvas;

	private Action _textureAction;

	private Action _tilesAction;

	private Action _listAction;

	private ImageCanvas_Zoom_1_1_Action _zoom_1_1_action;

	private ImageCanvas_Zoom_FitWindow_Action _zoom_fitWindow_action;

	public AtlasAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		setLayout(new StackLayout());

		_canvas = new AtlasCanvas(this, SWT.NONE);
		_spritesList = new FilteredTree(this, SWT.MULTI, new PatternFilter2(), true);
		_gridCanvas = new FrameGridCanvas(this, SWT.NONE);

		{
			Transfer[] types = { LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance() };
			_spritesList.getViewer().addDragSupport(DND.DROP_MOVE | DND.DROP_DEFAULT, types, new DragSourceAdapter() {

				private Object[] _data;

				@Override
				public void dragStart(DragSourceEvent event) {
					LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
					_data = ((IStructuredSelection) _spritesList.getViewer().getSelection()).toArray();
					transfer.setSelection(new StructuredSelection(_data));
				}

				@Override
				public void dragSetData(DragSourceEvent event) {
					JSONArray array = new JSONArray();
					for (Object elem : _data) {
						if (elem instanceof IAssetKey) {
							array.put(AssetPackCore.getAssetJSONReference((IAssetKey) elem));
						}
					}
					event.data = array.toString();
				}
			});
		}

		afterCreateWidgets();
	}

	static class AtlasContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof AtlasAssetModel) {
				return ((AtlasAssetModel) parentElement).getAtlasFrames().toArray();
			}

			return new Object[] {};
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	private void afterCreateWidgets() {
		TreeViewer viewer = _spritesList.getViewer();
		viewer.setContentProvider(new AtlasContentProvider());
		viewer.setLabelProvider(AssetLabelProvider.GLOBAL_64);

		moveTop(_gridCanvas);
	}

	private void moveTop(Control control) {
		StackLayout layout = (StackLayout) getLayout();
		layout.topControl = control;
		layout();

		updateActionsState();

		control.setFocus();
	}

	private void updateActionsState() {
		if (_zoom_1_1_action == null) {
			return;
		}

		StackLayout layout = (StackLayout) getLayout();
		Control control = layout.topControl;
		_zoom_1_1_action.setEnabled(control == _canvas);
		_zoom_fitWindow_action.setEnabled(control != _spritesList);

		_tilesAction.setChecked(control == _gridCanvas);
		_textureAction.setChecked(control == _canvas);
		_listAction.setChecked(control == _spritesList);
	}

	public void setModel(AtlasAssetModel model) {
		_model = model;
		String url = model.getTextureURL();
		IFile file = model.getFileFromUrl(url);
		_canvas.setImageFile(file);

		List<Frame> frames = model.getAtlasFrames();

		_canvas.setFrames(frames);
		_canvas.redraw();

		_spritesList.getViewer().setInput(model);
		// sort frames by name
		_spritesList.getViewer().setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((AtlasAssetModel.Frame) e1).getKey().toLowerCase()
						.compareTo(((AtlasAssetModel.Frame) e2).getKey().toLowerCase());
			}
		});

		_gridCanvas.loadFrameProvider(new AtlasAssetFramesProvider(model));
		_gridCanvas.resetZoom();
	}

	public AtlasAssetModel getModel() {
		return _model;
	}

	public AtlasCanvas getAtlasCanvas() {
		return _canvas;
	}

	public void createToolBar(IToolBarManager toolbar) {

		_tilesAction = new Action("Tiles", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_APPLICATION_TILE));
			}

			@Override
			public void run() {
				moveTop(_gridCanvas);
			}
		};
		_textureAction = new Action("Texture", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_IMAGES));
			}

			@Override
			public void run() {
				moveTop(_canvas);
			}
		};
		_listAction = new Action("List", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_APPLICATION_LIST));
			}

			@Override
			public void run() {
				moveTop(_spritesList);
			}
		};

		toolbar.add(_tilesAction);
		toolbar.add(_listAction);
		toolbar.add(_textureAction);

		toolbar.add(new Separator());

		_zoom_1_1_action = new ImageCanvas_Zoom_1_1_Action(_canvas);
		_zoom_fitWindow_action = new ImageCanvas_Zoom_FitWindow_Action() {
			@Override
			public IZoomable getImageCanvas() {
				Control top = ((StackLayout) getLayout()).topControl;
				if (top instanceof IZoomable) {
					return (IZoomable) top;
				}
				return _canvas;
			}
		};
		toolbar.add(_zoom_1_1_action);
		toolbar.add(_zoom_fitWindow_action);

		updateActionsState();
	}

}
