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
import org.eclipse.jface.action.IMenuCreator;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.widgets.SpritesheetPreviewCanvas;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCanvas;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;
import phasereditor.ui.animations.FrameAnimationCanvas;

public class SpritesheetAssetPreviewComp extends Composite {

	SpritesheetPreviewCanvas _sheetCanvas;

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
				event.image = AssetLabelProvider.GLOBAL_48.getImage(((StructuredSelection) sel).getFirstElement());
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				transfer.setSelection(sel);
			}

			private ISelection getSelection() {
				return new StructuredSelection(getSelectedFrames());
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				List<FrameModel> frames = getSelectedFrames();
				if (!frames.isEmpty()) {
					event.data = frames.get(0).getName();
				}
			}
		});

		_animCanvas = new FrameAnimationCanvas(this, SWT.NONE);

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {
		// nothing
	}

	private SpritesheetAssetModel _model;

	private SpritesheetAnimationModel _canvasAnimModel;

	private AnimationModel _animModel;

	protected void playButtonPressed() {
		StackLayout layout = (StackLayout) getLayout();

		if (isSheetInTheTop()) {
			layout.topControl = _animCanvas;
			_animCanvas.stop();
			updateAnimationModels();
			_animCanvas.play();
		} else {
			layout.topControl = _sheetCanvas;
			_animCanvas.stop();
		}

		layout();
	}

	private boolean isSheetInTheTop() {
		StackLayout layout = (StackLayout) getLayout();
		return layout.topControl == _sheetCanvas;
	}

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
			updateAnimationModels();
			_animCanvas.stop();
		}

		((StackLayout) getLayout()).topControl = _sheetCanvas;
		layout();
	}

	private void updateAnimationModels() {
		List<FrameModel> frames;

		List<FrameModel> selection = getSelectedFrames();

		if (!selection.isEmpty()) {
			frames = selection;
		} else {
			frames = _model.getFrames();
		}

		int fps = 5;
		if (_animModel != null) {
			fps = _animModel.getFrameRate();
		}
		_animModel = new AnimationModel("noname");
		_animModel.getFrames().addAll(frames);
		_animModel.setFrameRate(fps);
		_animModel.setLoop(true);
		_canvasAnimModel = new SpritesheetAnimationModel(_animModel);
		_animCanvas.setModel(_canvasAnimModel);
	}

	public SpritesheetAssetModel getModel() {
		return _model;
	}

	public void setFps(int fps) {
		_canvasAnimModel.setFrameRates(fps);
		if (!isSheetInTheTop()) {
			_animCanvas.play();
		}
	}

	private Action _playAction;
	private FrameAnimationCanvas _animCanvas;

	private Action _setFpsAction;

	public Menu _menu;

	public void createToolBar(IToolBarManager toolbar) {

		// play buttons

		_playAction = new Action("Play") {
			@Override
			public void run() {
				boolean b = getText().equals("Play");
				setText(b ? "Stop" : "Play");
				setImageDescriptor(b ? EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_STOP)
						: EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PLAY));
				playButtonPressed();
			}
		};
		_playAction.setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PLAY));
		toolbar.add(_playAction);

		// settings

		class FpsAction extends Action implements IMenuCreator {
			FpsAction() {
				super("Set Frames Per Second (FPS)", AS_DROP_DOWN_MENU);
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_CONTROL_EQUALIZER));
				setMenuCreator(this);
			}

			@Override
			public void dispose() {
				if (_menu != null) {
					_menu.dispose();
				}
			}

			@SuppressWarnings("synthetic-access")
			@Override
			public Menu getMenu(Control parent) {
				if (_menu != null) {
					_menu.dispose();
				}
				int[] fpsList = new int[6 + 2];
				for (int i = 0; i < 6; i++) {
					fpsList[i + 2] = (i + 1) * 10;
				}
				fpsList[0] = 1;
				fpsList[1] = 5;

				_menu = new Menu(parent);
				for (int i : fpsList) {
					final int fps = i;
					MenuItem item = new MenuItem(_menu, SWT.CHECK);
					item.setText(Integer.toString(fps));
					item.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							setFps(fps);
						}
					});
					if (i == _animModel.getFrameRate()) {
						item.setSelection(true);
					}
				}

				return _menu;
			}

			@Override
			public Menu getMenu(Menu parent) {
				return null;
			}

			@Override
			public void runWithEvent(Event event) {
				ToolItem item = (ToolItem) event.widget;
				Rectangle rect = item.getBounds();
				Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
				Menu menu = getMenu(item.getParent());
				menu.setLocation(pt.x, pt.y + rect.height);
				menu.setVisible(true);
			}

		}

		_setFpsAction = new FpsAction();
		toolbar.add(_setFpsAction);

		toolbar.add(new Separator());
		toolbar.add(new ImageCanvas_Zoom_1_1_Action() {

			@Override
			public ImageCanvas getImageCanvas() {
				return (ImageCanvas) ((StackLayout) getLayout()).topControl;
			}
		});
		toolbar.add(new ImageCanvas_Zoom_FitWindow_Action() {

			@Override
			public ImageCanvas getImageCanvas() {
				return (ImageCanvas) ((StackLayout) getLayout()).topControl;
			}
		});

	}

	public void stopAnimation() {
		_animCanvas.stop();
	}

	List<FrameModel> getSelectedFrames() {
		return _sheetCanvas.getSelectedFrames();
	}
}
