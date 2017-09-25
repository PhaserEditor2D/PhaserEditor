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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ListDialog;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.assetpack.ui.widgets.SpritesheetPreviewCanvas;
import phasereditor.ui.Animation;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

public class SpritesheetAssetPreviewComp extends Composite {
	private int _fps = 5;

	SpritesheetPreviewCanvas _canvas;
	

	protected List<FrameModel> _selectedFrames;

	public static class FpsValidator implements IValidator {

		@Override
		public IStatus validate(Object value) {
			Integer fps = (Integer) value;
			if (fps.intValue() < 1) {
				return ValidationStatus.error("Wrong FPS value");
			} else if (fps.intValue() > 120) {
				return ValidationStatus.error("Is not too fast?");
			}
			return Status.OK_STATUS;
		}

	}

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public SpritesheetAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (_animation != null) {
					_animation.stop();
				}
			}
		});
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		_canvas = new SpritesheetPreviewCanvas(this, SWT.NONE);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {

		// DnD

		class Listener extends MouseAdapter {

			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1) {
					_selectedFrames = _canvas.getSelectedFrames();
				}
			}
		}

		_canvas.addMouseListener(new Listener());

		DragSource dragSource = new DragSource(_canvas, DND.DROP_MOVE | DND.DROP_DEFAULT);
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
				if (_selectedFrames == null) {
					return StructuredSelection.EMPTY;
				}

				return new StructuredSelection(_selectedFrames);
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (_selectedFrames != null && !_selectedFrames.isEmpty()) {
					event.data = _selectedFrames.get(0).getName();
				}
			}
		});
	}

	protected Animation _animation;

	private SpritesheetAssetModel _model;

	private void animate() {
		_animation = new Animation(30) {

			@Override
			public void action() {
				try {
					int n = _canvas.getFrameCount();
					if (n > 0) {
						int f = _canvas.getFrame();
						f = (f + 1) % n;
						_canvas.setFrame(f);
						_canvas.redraw();
					}
				} catch (SWTException e) {
					// invalid access.
					stop();
				}
			}
		};
		_animation.start();
	}

	protected void playButtonPressed() {
		boolean single = _canvas.isSingleFrame();
		single = !single;
		_animation.pause(!single);
		_canvas.setSingleFrame(single);
		_canvas.fitWindow();
		_canvas.redraw();
	}

	public void setModel(SpritesheetAssetModel model) {
		_model = model;

		_selectedFrames = new ArrayList<>();

		_canvas.setSpritesheet(model);

		IFile file = model.getUrlFile();
		_canvas.setImageFile(file);

		{
			String str = "Frames Size: " + model.getFrameWidth() + "x" + model.getFrameHeight();
			if (_canvas.getImage() != null) {
				str += "\n";
				Rectangle b = _canvas.getImage().getBounds();
				str += "Image Size: " + b.width + "x" + b.height;
				str += "Image URL: " + model.getUrl();
			}
			_canvas.setToolTipText(str);
		}

		animate();
		_animation.pause(true);
		setFps(5);
	}

	public SpritesheetAssetModel getModel() {
		return _model;
	}

	public void stopAnimation() {
		if (_animation != null) {
			_animation.pause(true);
		}
	}

	public void setFps(int fps) {
		_fps = fps;
		if (_animation != null) {
			_animation.setFps(fps);
		}
	}

	private Action _playAction;


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

		Object[] fpsList = new Object[6 + 2];
		for (int i = 0; i < 6; i++) {
			fpsList[i + 2] = Integer.valueOf((i + 1) * 10);
		}
		fpsList[0] = Integer.valueOf(1);
		fpsList[1] = Integer.valueOf(5);

		toolbar.add(new Action("Settings") {

			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_SETTINGS));
			}

			@SuppressWarnings({ "boxing", "synthetic-access" })
			@Override
			public void run() {
				ListDialog dlg = new ListDialog(getShell());
				dlg.setContentProvider(new ArrayContentProvider());
				dlg.setLabelProvider(new LabelProvider());
				dlg.setInput(fpsList);
				dlg.setInitialSelections(new Object[] { _fps });
				dlg.setMessage("Select the frames per second:");
				dlg.setTitle("FPS");

				if (dlg.open() == Window.OK) {
					Integer fps = (Integer) dlg.getResult()[0];
					setFps(fps.intValue());
				}
			}
		});
	}
}
