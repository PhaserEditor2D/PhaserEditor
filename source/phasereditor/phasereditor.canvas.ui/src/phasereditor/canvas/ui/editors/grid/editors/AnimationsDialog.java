// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors.grid.editors;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.ResourceManager;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.ui.animations.FrameAnimationCanvas;
import phasereditor.ui.animations.IFramesAnimationModel;

/**
 * @author arian
 *
 */
@SuppressWarnings({ "synthetic-access", "unchecked" })
public class AnimationsDialog extends Dialog {
	private DataBindingContext m_bindingContext;
	private Text _frameRateText;
	private Table _table;
	private ComboViewer _animationsViewer;
	private TableViewer _framesViewer;
	private List<AnimationModel> _animList;
	private Button _loopButton;
	private AnimationModel _anim;
	private FrameAnimationCanvas _canvas;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public AnimationsDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE | SWT.TITLE);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Animations Editor");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#getShellStyle()
	 */
	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(4, false);
		gl_composite.marginWidth = 0;
		gl_composite.marginHeight = 0;
		composite.setLayout(gl_composite);

		_lblName = new Label(composite, SWT.NONE);
		_lblName.setText("Name");

		_animationsViewer = new ComboViewer(composite, SWT.READ_ONLY);
		_animationsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = _animationsViewer.getStructuredSelection();
				_anim = sel.isEmpty() ? null : (AnimationModel) sel.getFirstElement();
				updateFromAnimation();
			}
		});
		Combo combo = _animationsViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button button = new Button(composite, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAnimation();
			}
		});
		button.setImage(ResourceManager.getPluginImage("org.eclipse.ui", "/icons/full/obj16/add_obj.png"));

		_deleteAnimButton = new Button(composite, SWT.NONE);
		_deleteAnimButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteAnimation();
			}
		});
		_deleteAnimButton
				.setImage(ResourceManager.getPluginImage("org.eclipse.ui", "/icons/full/obj16/delete_obj.png"));

		_lblFrameRate = new Label(composite, SWT.NONE);
		_lblFrameRate.setText("Frame Rate");

		_frameRateText = new Text(composite, SWT.BORDER);
		_frameRateText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		Composite composite_4 = new Composite(composite, SWT.NONE);
		composite_4.setLayout(new GridLayout(7, false));
		composite_4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1));

		_loopButton = new Button(composite_4, SWT.CHECK);
		_loopButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		_loopButton.setText("Loop");

		_btnKilloncomplete = new Button(composite_4, SWT.CHECK);
		_btnKilloncomplete.setText("Kill On Complete");

		_button = new Button(composite_4, SWT.CHECK);
		_button.setToolTipText("Phaser Editor: play this animation after created.");
		_button.setText("Auto Play");

		_btnPublic = new Button(composite_4, SWT.CHECK);
		_btnPublic.setText("Public");

		SashForm sashForm = new SashForm(container, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Composite composite_2 = new Composite(sashForm, SWT.NONE);
		GridLayout gl_composite_2 = new GridLayout(1, false);
		gl_composite_2.marginWidth = 0;
		gl_composite_2.marginHeight = 0;
		composite_2.setLayout(gl_composite_2);

		_framesViewer = new TableViewer(composite_2, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		_table = _framesViewer.getTable();
		_table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_framesToolbar = new Composite(composite_2, SWT.NONE);
		GridLayout gl_framesToolbar = new GridLayout(2, false);
		gl_framesToolbar.marginWidth = 0;
		gl_framesToolbar.marginHeight = 0;
		_framesToolbar.setLayout(gl_framesToolbar);

		Button btnAdd = new Button(_framesToolbar, SWT.NONE);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addFrame();
			}
		});
		btnAdd.setImage(ResourceManager.getPluginImage("org.eclipse.ui", "/icons/full/obj16/add_obj.png"));

		Button btnRemove = new Button(_framesToolbar, SWT.NONE);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteFrame();
			}
		});
		btnRemove.setImage(ResourceManager.getPluginImage("org.eclipse.ui", "/icons/full/obj16/delete_obj.png"));

		Composite composite_3 = new Composite(sashForm, SWT.NONE);
		GridLayout gl_composite_3 = new GridLayout(1, false);
		gl_composite_3.marginHeight = 0;
		gl_composite_3.marginWidth = 0;
		composite_3.setLayout(gl_composite_3);

		_canvas = new FrameAnimationCanvas(composite_3, SWT.BORDER);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Composite composite_1 = new Composite(composite_3, SWT.NONE);
		GridLayout gl_composite_1 = new GridLayout(1, false);
		gl_composite_1.marginHeight = 0;
		gl_composite_1.marginWidth = 0;
		composite_1.setLayout(gl_composite_1);

		_playButton = new Button(composite_1, SWT.NONE);
		_playButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				playAnimation();
			}
		});
		_playButton.setImage(ResourceManager.getPluginImage("phasereditor.ui", "icons/control_play.png"));
		sashForm.setWeights(new int[] { 2, 3 });

		return container;
	}

	protected void deleteAnimation() {
		Object anim = _animationsViewer.getStructuredSelection().getFirstElement();

		_animList.remove(anim);

		_animationsViewer.refresh();

		if (_animList.isEmpty()) {
			setAnimation(null);
		} else {
			setAnimation(_animList.get(0));
		}
		updateFromAnimation();
	}

	protected void deleteFrame() {
		int[] sel = _framesViewer.getTable().getSelectionIndices();
		for (int i : sel) {
			_anim.getFrames().remove(i);
		}
		_framesViewer.refresh();
		playAnimation();
	}

	protected void addFrame() {
		FrameDialog dlg = new FrameDialog(getParentShell());
		dlg.setAllowMultipleSelection(true);
		dlg.setAllowNull(false);
		dlg.setFrames(_allFrames);

		if (dlg.open() == Window.OK) {
			List<IAssetFrameModel> list = new ArrayList<>();
			for (Object obj : dlg.getMultipleResult()) {
				list.add((IAssetFrameModel) obj);
			}
			_anim.getFrames().addAll(list);
			_framesViewer.refresh();
			playAnimation();
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		// stop the animations
		_canvas.setModel(null);

		super.buttonPressed(buttonId);
	}

	private void afterCreateWidgets() {
		_framesViewer.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return AssetLabelProvider.GLOBAL_64.getImage(element);
			}

			@Override
			public String getText(Object element) {
				if (element instanceof SpritesheetAssetModel.FrameModel) {
					int index = ((SpritesheetAssetModel.FrameModel) element).getIndex();
					return Integer.toString(index);
				}

				return AssetLabelProvider.GLOBAL_64.getText(element);
			}
		});
		_framesViewer.setContentProvider(ArrayContentProvider.getInstance());
		_animationsViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				AnimationModel anim = (AnimationModel) element;
				return anim.getName() + (anim.isAutoPlay() ? " (auto)" : "");
			}
		});

		_animationsViewer.setContentProvider(ArrayContentProvider.getInstance());
		_animationsViewer.setInput(_animList);

		AssetPackUI.installAssetTooltips(_framesViewer);

		if (_animList.isEmpty()) {
			setAnimation(null);
		} else {
			setAnimation(_animList.get(0));
		}

		PhaserJSDoc help = InspectCore.getPhaserHelp();
		_lblName.setToolTipText(help.getMethodArgHelp("Phaser.AnimationManager.add", "name"));
		_lblFrameRate.setToolTipText(help.getMethodArgHelp("Phaser.AnimationManager.add", "frameRate"));
		_loopButton.setToolTipText(help.getMemberHelp("Phaser.Animation.loop"));
		_btnKilloncomplete.setToolTipText(help.getMemberHelp("Phaser.Animation.killOnComplete"));
		_btnPublic.setToolTipText("If generate a public field to reference this animation (Phaser Editor).");

		initFramesDrop();

		updateFromAnimation();
	}

	private void initFramesDrop() {
		int operations = DND.DROP_DEFAULT | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		_framesViewer.addDragSupport(operations, transfers, new DragSourceListener() {

			private int _data;

			@Override
			public void dragStart(DragSourceEvent event) {
				_data = _framesViewer.getTable().getSelectionIndex();
			}

			@SuppressWarnings("boxing")
			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = _data;
				LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(_data));
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				// finished
			}
		});

		_framesViewer.addDropSupport(operations, transfers, new ViewerDropAdapter(_framesViewer) {

			private int _location;
			private int _target;

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				return true;
			}

			@SuppressWarnings("boxing")
			@Override
			public boolean performDrop(Object data) {
				if (_target == -1) {
					return false;
				}

				List<IAssetFrameModel> frames = _anim.getFrames();

				Object[] elems = ((IStructuredSelection) data).toArray();
				int target = _target;

				int i = (int) elems[0];

				if (i == _target) {
					return false;
				}

				IAssetFrameModel toMove = frames.get(i);

				if (i < _target) {
					target--;
				}

				frames.remove(i);

				switch (_location) {
				case LOCATION_ON:
				case LOCATION_BEFORE:
					frames.add(target, toMove);
					break;
				case LOCATION_AFTER:
					frames.add(target + 1, toMove);
					break;
				default:
					break;
				}

				_framesViewer.refresh();

				return true;
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				_location = determineLocation(event);
				// _target = (int) determineTarget(event);
				TableItem item = (TableItem) event.item;
				if (item == null) {
					_target = -1;
				} else {
					_target = _framesViewer.getTable().indexOf(item);
				}
				super.dragOver(event);
			}
		});
	}

	protected void addAnimation() {
		InputDialog dlg = new InputDialog(getParentShell(), "Animation", "Enter the animation name.", "walk",
				new IInputValidator() {

					@Override
					public String isValid(String newText) {
						if (newText.isEmpty()) {
							return "It is not a valid name";
						}

						for (AnimationModel anim : getAnimations()) {
							if (newText.equals(anim.getName())) {
								return "That name exists";
							}
						}
						return null;
					}
				});

		if (dlg.open() == Window.OK) {
			String name = dlg.getValue();
			AnimationModel anim = new AnimationModel(name);
			_animList.add(anim);
			_animationsViewer.refresh();
			setAnimation(anim);
			updateFromAnimation();
		}
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
		m_bindingContext = initDataBindings();

		afterCreateWidgets();
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(457, 424);
	}

	public void setAnimations(List<AnimationModel> animations) {
		_animList = animations;
	}

	private AnimationsDialog _self = this;
	private List<?> _allFrames;

	private void updateFromAnimation() {
		if (_anim == null) {
			_framesViewer.setInput(Collections.emptyList());
		} else {
			_framesViewer.setInput(_anim.getFrames());
		}
		if (m_bindingContext != null) {
			m_bindingContext.updateTargets();
		}

		enableAll(_framesToolbar, _anim != null);
		_deleteAnimButton.setEnabled(_anim != null);
		_playButton.setEnabled(_anim != null);

		playAnimation();
	}

	static class SpritesheetAnimationModel implements IFramesAnimationModel {
		private AnimationModel _base;
		private ArrayList<Rectangle> _frames;

		public SpritesheetAnimationModel(AnimationModel base) {
			super();
			_base = base;
			_frames = new ArrayList<>();
			for (IAssetFrameModel assetFrame : _base.getFrames()) {
				_frames.add(assetFrame.getFrameData().src);
			}
		}

		@Override
		public List<Rectangle> getFrames() {
			return _frames;
		}

		@Override
		public boolean isLoop() {
			return _base.isLoop();
		}

		@Override
		public int getFrameRate() {
			return _base.getFrameRate();
		}

		@Override
		public IFile getImageFile() {
			return _base.getFrames().get(0).getImageFile();
		}

	}

	void playAnimation() {
		_canvas.setModel(_anim == null ? null : new SpritesheetAnimationModel(_anim));
	}

	public void setAvailableFrames(List<?> allFrames) {
		_allFrames = allFrames;
	}

	public void setAnimation(AnimationModel animation) {
		_anim = animation;
		_animationsViewer
				.setSelection(animation == null ? StructuredSelection.EMPTY : new StructuredSelection(animation));
	}

	private static void enableAll(Composite comp, boolean enable) {
		for (Control c : comp.getChildren()) {
			c.setEnabled(enable);
		}
	}

	public AnimationModel getAnimation() {
		return _anim;
	}

	public List<AnimationModel> getAnimations() {
		return _animList;
	}

	public List<AnimationModel> getValue() {
		return _animList;
	}

	public int getFrameRate() {
		if (_anim == null) {
			return 0;
		}

		return _anim.getFrameRate();
	}

	public void setFrameRate(int frameRate) {
		if (_anim == null) {
			return;
		}

		_anim.setFrameRate(frameRate);
		firePropertyChange("frameRate");

		playAnimation();
	}

	public boolean isLoop() {
		if (_anim == null) {
			return false;
		}
		return _anim.isLoop();
	}

	public void setLoop(boolean loop) {
		if (_anim == null) {
			return;
		}

		_anim.setLoop(loop);
		playAnimation();
	}

	public void setKillOnComplete(boolean killOnComplete) {
		if (_anim == null) {
			return;
		}

		_anim.setKillOnComplete(killOnComplete);
		firePropertyChange("killOnComplete");
	}

	public boolean getKillOnComplete() {
		if (_anim == null) {
			return false;
		}

		return _anim.isKillOnComplete();
	}

	public void setAutoPlay(boolean autoPlay) {
		if (_anim == null) {
			return;
		}

		if (autoPlay) {
			for (AnimationModel a : _animList) {
				a.setAutoPlay(false);
			}
		}

		_anim.setAutoPlay(autoPlay);
		_animationsViewer.refresh();
		firePropertyChange("autoPlay");
	}

	public boolean isAutoPlay() {
		if (_anim == null) {
			return false;
		}

		return _anim.isAutoPlay();
	}

	public void setPublic(boolean aPublic) {
		if (_anim == null) {
			return;
		}

		_anim.setPublic(aPublic);
		firePropertyChange("public");
	}

	public boolean getPublic() {
		if (_anim == null) {
			return false;
		}

		return _anim.isPublic();
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private Composite _framesToolbar;
	private Button _deleteAnimButton;
	private Button _playButton;
	private Button _btnKilloncomplete;
	private Button _btnPublic;
	private Label _lblFrameRate;
	private Label _lblName;
	private Button _button;

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		support.firePropertyChange(property, true, false);
	}

	@SuppressWarnings("rawtypes")
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeText_frameRateTextObserveWidget = WidgetProperties.text(SWT.Modify)
				.observe(_frameRateText);
		IObservableValue frameRate_selfObserveValue = BeanProperties.value("frameRate").observe(_self);
		bindingContext.bindValue(observeText_frameRateTextObserveWidget, frameRate_selfObserveValue, null, null);
		//
		IObservableValue observeSelection_loopButtonObserveWidget = WidgetProperties.selection().observe(_loopButton);
		IObservableValue loop_selfObserveValue = BeanProperties.value("loop").observe(_self);
		bindingContext.bindValue(observeSelection_loopButtonObserveWidget, loop_selfObserveValue, null, null);
		//
		IObservableValue observeSelection_btnKilloncompleteObserveWidget = WidgetProperties.selection()
				.observe(_btnKilloncomplete);
		IObservableValue killOnComplete_selfObserveValue = BeanProperties.value("killOnComplete").observe(_self);
		bindingContext.bindValue(observeSelection_btnKilloncompleteObserveWidget, killOnComplete_selfObserveValue, null,
				null);
		//
		IObservableValue observeSelection_btnPublicObserveWidget = WidgetProperties.selection().observe(_btnPublic);
		IObservableValue public_selfObserveValue = BeanProperties.value("public").observe(_self);
		bindingContext.bindValue(observeSelection_btnPublicObserveWidget, public_selfObserveValue, null, null);
		//
		IObservableValue observeSelection_buttonObserveWidget = WidgetProperties.selection().observe(_button);
		IObservableValue autoPlay_selfObserveValue = BeanProperties.value("autoPlay").observe(_self);
		bindingContext.bindValue(observeSelection_buttonObserveWidget, autoPlay_selfObserveValue, null, null);
		//
		return bindingContext;
	}
}
