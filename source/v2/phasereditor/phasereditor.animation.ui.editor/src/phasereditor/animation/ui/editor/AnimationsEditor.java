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
package phasereditor.animation.ui.editor;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.animation.Animation.Status;
import phasereditor.animation.ui.AnimationCanvas;
import phasereditor.animation.ui.AnimationCanvas.IndexTransition;
import phasereditor.animation.ui.editor.properties.AnimationsPGridPage;
import phasereditor.animation.ui.editor.wizards.AssetsSplitter;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;
import phasereditor.ui.SelectionProviderImpl;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class AnimationsEditor extends EditorPart implements IPersistableEditor {

	/**
	 * 
	 */
	private static final String ANIMATION_KEY = "animation";
	/**
	 * 
	 */
	private static final String OUTLINER_TREE_STATE_KEY = "outliner.tree.state";
	public static final String ID = "phasereditor.animation.ui.AnimationsEditor"; //$NON-NLS-1$
	private AnimationsModel_in_Editor _model;
	private AnimationCanvas _animCanvas;
	Outliner _outliner;
	ISelectionChangedListener _outlinerListener;
	private AnimationTimelineCanvas_in_Editor _timelineCanvas;
	private Action _playAction;
	private Action _pauseAction;
	private Action _stopAction;
	private Action[] _playbackActions = { _playAction, _pauseAction, _stopAction };
	private ImageCanvas_Zoom_1_1_Action _zoom_1_1_action;
	private ImageCanvas_Zoom_FitWindow_Action _zoom_fitWindow_action;
	private boolean _dirty;
	private String _initialAnimtionKey;
	JSONObject _initialOutlinerState;

	public AnimationsEditor() {
	}

	/**
	 * Create contents of the editor part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		SashForm sash = new SashForm(parent, SWT.VERTICAL);

		Composite topComp = new Composite(sash, SWT.BORDER);
		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		topComp.setLayout(layout);

		_animCanvas = new AnimationCanvas(topComp, SWT.NONE);
		_animCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createToolbar(topComp);

		_timelineCanvas = new AnimationTimelineCanvas_in_Editor(sash, SWT.BORDER);
		_timelineCanvas.setEditor(this);

		sash.setWeights(new int[] { 2, 1 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		getEditorSite().setSelectionProvider(new ISelectionProvider() {

			private ISelection _selection;
			private ListenerList<ISelectionChangedListener> _listeners = new ListenerList<>();

			@Override
			public void setSelection(ISelection selection) {
				_selection = selection;
				var event = new SelectionChangedEvent(this, selection);
				for (var l : _listeners) {
					l.selectionChanged(event);
				}
			}

			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				_listeners.remove(listener);
			}

			@Override
			public ISelection getSelection() {
				return _selection;
			}

			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				_listeners.add(listener);
			}
		});

		_animCanvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				var anim = getTimelineCanvas().getModel();

				if (anim != null) {
					getTimelineCanvas().clearSelection();
				}
			}
		});
		_animCanvas.setNoImageMessage("");
		_animCanvas.setStepCallback(_timelineCanvas::redraw);
		_animCanvas.setPlaybackCallback(this::animationStatusChanged);
		_animCanvas.addPaintListener(e -> {
			if (_animCanvas.getModel() != null) {
				e.gc.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.DECORATIONS_COLOR));
				e.gc.drawText(_animCanvas.getModel().getKey(), 0, 0, true);
			}
		});

		disableToolbar();

		createContextMenu();

		AnimationModel_in_Editor anim = null;

		if (!_model.getAnimations().isEmpty()) {
			anim = (AnimationModel_in_Editor) _model.getAnimations().get(0);
		}

		if (_initialAnimtionKey != null) {
			var opt = _model.getAnimations().stream().filter(a -> a.getKey().equals(_initialAnimtionKey)).findFirst();
			if (opt.isPresent()) {
				anim = (AnimationModel_in_Editor) opt.get();
			}
		}

		if (anim != null) {
			loadAnimation(anim);
		}

		init_DND_Support();
	}

	private void init_DND_Support() {
		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(_animCanvas, options);
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			target.setTransfer(types);
			target.addDropListener(new DropTargetAdapter() {

				@Override
				public void drop(DropTargetEvent event) {
					if (event.data instanceof Object[]) {
						createAnimationsWithDrop((Object[]) event.data);
					}

					if (event.data instanceof IStructuredSelection) {
						createAnimationsWithDrop(((IStructuredSelection) event.data).toArray());
					}
				}
			});
		}
	}

	protected final void openNewAnimationDialog(List<AnimationFrameModel_in_Editor> frames) {

		String initialName = "untitled";

		InputDialog dlg = new InputDialog(getAnimationCanvas().getShell(), "New Animation",
				"Enter the name of the new animation.", initialName, new IInputValidator() {

					@Override
					public String isValid(String newText) {
						if (getModel().getAnimations().stream().filter(a -> a.getKey().equals(newText)).findFirst()
								.isPresent()) {
							return "That name is used by other animation.";
						}
						return null;
					}
				});

		if (dlg.open() == Window.OK) {

			var anim = new AnimationModel_in_Editor(getModel());

			anim.setKey(dlg.getValue());
			if (frames != null) {
				anim.getFrames().addAll(frames);
			}

			getModel().getAnimations().add(anim);

			anim.buildTimeline();

			if (_outliner != null) {
				_outliner.refresh();
			}

			selectAnimation(anim);

			setDirty();
		}
	}

	private void createContextMenu() {
		MenuManager menuManager = new MenuManager();
		menuManager.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.removeAll();
				manager.add(new Action("New Animation",
						EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_NEW_FRAME_ANIMATION)) {
					@Override
					public void run() {
						openNewAnimationDialog(null);
					}
				});
				manager.add(new CommandContributionItem(new CommandContributionItemParameter(getEditorSite(), "outline",
						"phasereditor.ui.quickOutline", SWT.PUSH)));

				AnimationModel currentAnim = getAnimationCanvas().getModel();

				if (currentAnim != null) {
					manager.add(new Separator());
					manager.add(new Action("Delete", Workbench.getInstance().getSharedImages()
							.getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE)) {
						@Override
						public void run() {
							deleteAnimations(List.of((AnimationModel_in_Editor) currentAnim));
						}
					});
				}
			}
		});

		var menu = menuManager.createContextMenu(_animCanvas);
		_animCanvas.setMenu(menu);
	}

	private void disableToolbar() {
		for (var btn : _playbackActions) {
			btn.setEnabled(false);
		}

		_zoom_1_1_action.setEnabled(false);
		_zoom_fitWindow_action.setEnabled(false);
	}

	private void animationStatusChanged(Status status) {

		out.println("status: " + status);

		switch (status) {
		case RUNNING:
			_playAction.setChecked(true);
			_pauseAction.setChecked(false);
			break;
		case STOPPED:
			// TODO: do we really want to do this? it breaks the animation, it looks like
			// the first frame is actually the last frame of the animation.
			//
			// AnimationCanvas animCanvas = getAnimationCanvas();
			// var anim = animCanvas.getModel();
			// var frames = anim.getFrames();
			//
			// if (!frames.isEmpty()) {
			// animCanvas.showFrame(0);
			// }
			_playAction.setChecked(false);
			_pauseAction.setChecked(false);
			break;
		case PAUSED:
			_playAction.setChecked(false);
			_pauseAction.setChecked(true);
			break;
		default:
			break;
		}

		_playAction.setEnabled(!_playAction.isChecked());
		_pauseAction.setEnabled(_playAction.isChecked());
		_stopAction.setEnabled(_playAction.isChecked() || _pauseAction.isChecked());
	}

	public void gridPropertyChanged() {

		setDirty();

		var running = !_animCanvas.isStopped();

		_animCanvas.stop();

		var anim = _timelineCanvas.getModel();

		anim.buildTimeline();

		_timelineCanvas.redraw();

		if (running) {
			_animCanvas.play();
		}
	}

	private ToolBar createToolbar(Composite parent) {
		ToolBarManager manager = new ToolBarManager(SWT.BORDER);

		_playAction = new Action("Play", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PLAY));
			}

			@Override
			public void run() {
				AnimationCanvas canvas = getAnimationCanvas();
				IndexTransition transition = canvas.getTransition();
				if (transition == null) {
					canvas.play();
				} else {
					switch (transition.getStatus()) {
					case PAUSED:
						transition.play();
						break;
					case STOPPED:
						canvas.play();
						break;
					default:
						break;
					}
				}

				getTimelineCanvas().redraw();
				canvas.redraw();
			}
		};

		_pauseAction = new Action("Pause", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PAUSE));
			}

			@Override
			public void run() {
				getAnimationCanvas().pause();
				getTimelineCanvas().redraw();
				getAnimationCanvas().redraw();
			}

		};

		_stopAction = new Action("Stop", EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_STOP)) {

			@Override
			public void run() {
				getAnimationCanvas().stop();
				getTimelineCanvas().redraw();
				getAnimationCanvas().redraw();
			}

		};

		_zoom_1_1_action = new ImageCanvas_Zoom_1_1_Action(_animCanvas);
		_zoom_fitWindow_action = new ImageCanvas_Zoom_FitWindow_Action(_animCanvas);

		manager.add(_playAction);
		manager.add(_pauseAction);
		manager.add(_stopAction);
		manager.add(new Separator());
		manager.add(_zoom_1_1_action);
		manager.add(_zoom_fitWindow_action);

		_playbackActions = new Action[] { _playAction, _pauseAction, _stopAction };

		return manager.createControl(parent);
	}

	@Override
	public void setFocus() {
		_animCanvas.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		var file = getEditorInput().getFile();

		try (ByteArrayInputStream source = new ByteArrayInputStream(_model.toJSON().toString(2).getBytes())) {

			file.setContents(source, true, false, monitor);

			_dirty = false;

			firePropertyChange(PROP_DIRTY);

		} catch (JSONException | CoreException | IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doSaveAs() {
		// Do the Save As operation
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);

		var file = ((IFileEditorInput) input).getFile();

		setPartName(file.getName());

		try {
			_model = new AnimationsModel_in_Editor(this);
			_model.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public IFileEditorInput getEditorInput() {
		return (IFileEditorInput) super.getEditorInput();
	}

	public AnimationsModel_in_Editor getModel() {
		return _model;
	}

	@Override
	public boolean isDirty() {
		return _dirty;
	}

	public void setDirty() {
		_dirty = true;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {

		if (adapter == IContentOutlinePage.class) {
			return createOutliner();
		}

		if (adapter == IPropertySheetPage.class) {
			return new AnimationsPGridPage(this);
		}

		return super.getAdapter(adapter);
	}

	public AnimationCanvas getAnimationCanvas() {
		return _animCanvas;
	}

	public AnimationTimelineCanvas_in_Editor getTimelineCanvas() {
		return _timelineCanvas;
	}

	private Object createOutliner() {
		if (_outliner == null) {
			_outliner = new Outliner();

			_outlinerListener = new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					outliner_selectionChanged(event);
				}
			};
			_outliner.addSelectionChangedListener(_outlinerListener);
		} else {
			_outliner.refresh();
		}

		if (_timelineCanvas.getModel() != null) {
			swtRun(() -> {
				if (_outliner != null) {
					_outliner.setSelection(new StructuredSelection(_timelineCanvas.getModel()));
				}
			});
		}

		return _outliner;
	}

	public Outliner getOutliner() {
		return _outliner;
	}

	protected void outliner_selectionChanged(SelectionChangedEvent event) {
		var elem = event.getStructuredSelection().getFirstElement();
		var anim = (AnimationModel_in_Editor) elem;
		loadAnimation(anim);
		getEditorSite().getSelectionProvider().setSelection(event.getStructuredSelection());
	}

	public void selectAnimation(AnimationModel_in_Editor anim) {
		StructuredSelection selection = anim == null ? StructuredSelection.EMPTY : new StructuredSelection(anim);

		if (_outliner == null) {

			loadAnimation(anim);

			getEditorSite().getSelectionProvider().setSelection(selection);

		} else {

			_outliner.setSelection(selection);

		}
	}

	protected void loadAnimation(AnimationModel_in_Editor anim) {
		if (anim == null) {
			for (var btn : _playbackActions) {
				btn.setChecked(false);
				btn.setEnabled(false);
			}

			_zoom_1_1_action.setEnabled(false);
			_zoom_fitWindow_action.setEnabled(false);

			_animCanvas.setModel(null);
			_timelineCanvas.setModel(null);

			return;
		}

		_animCanvas.setModel(anim, false);

		for (var btn : _playbackActions) {
			btn.setChecked(false);
			btn.setEnabled(btn == _playAction);
		}

		if (_timelineCanvas.getModel() != anim) {
			_timelineCanvas.setModel(anim);
		}

		_zoom_1_1_action.setEnabled(true);
		_zoom_fitWindow_action.setEnabled(true);
	}

	class Outliner extends Page implements IContentOutlinePage, ISelectionChangedListener {
		private FilteredTreeCanvas _filteredTreeCanvas;
		private SelectionProviderImpl _selProvider;
		private TreeCanvasViewer _viewer;

		public Outliner() {
		}

		public FilteredTreeCanvas getFilteredTreeCanvas() {
			return _filteredTreeCanvas;
		}

		@Override
		public void createControl(Composite parent) {
			_filteredTreeCanvas = new FilteredTreeCanvas(parent, SWT.NONE);
			_viewer = new AnimationsTreeViewer(_filteredTreeCanvas.getCanvas());

			AssetPackUI.installAssetTooltips(_filteredTreeCanvas.getCanvas(), _filteredTreeCanvas.getUtils());

			{
				int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
				DropTarget target = new DropTarget(_filteredTreeCanvas.getCanvas(), options);
				Transfer[] types = { LocalSelectionTransfer.getTransfer() };
				target.setTransfer(types);
				target.addDropListener(new DropTargetAdapter() {

					@Override
					public void drop(DropTargetEvent event) {
						if (event.data instanceof Object[]) {
							createAnimationsWithDrop((Object[]) event.data);
						}

						if (event.data instanceof IStructuredSelection) {
							createAnimationsWithDrop(((IStructuredSelection) event.data).toArray());
						}
					}
				});
			}

			_viewer.setInput(getModel());

			for (var l : _initialListeners) {
				_filteredTreeCanvas.getUtils().addSelectionChangedListener(l);
			}

			if (_initialSelection != null) {
				_filteredTreeCanvas.getUtils().setSelection(_initialSelection);
				_filteredTreeCanvas.redraw();
			}

			_initialListeners.clear();
			_initialSelection = null;
			
			if (_initialOutlinerState != null) {
				_outliner.getFilteredTreeCanvas().getCanvas().restoreState(_initialOutlinerState);
				_initialOutlinerState = null;
			}

		}

		@Override
		public void dispose() {
			removeSelectionChangedListener(_outlinerListener);
			AnimationsEditor.this._outliner = null;
			super.dispose();
		}

		private ListenerList<ISelectionChangedListener> _initialListeners = new ListenerList<>();
		private ISelection _initialSelection;

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			if (_filteredTreeCanvas == null) {
				_initialListeners.add(listener);
				return;
			}

			_filteredTreeCanvas.getUtils().addSelectionChangedListener(listener);
		}

		@Override
		public ISelection getSelection() {
			return _filteredTreeCanvas.getUtils().getSelection();
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			if (_filteredTreeCanvas == null) {
				_initialListeners.remove(listener);
				return;
			}

			_filteredTreeCanvas.getUtils().removeSelectionChangedListener(listener);
		}

		@Override
		public void setSelection(ISelection selection) {
			if (_filteredTreeCanvas == null) {
				_initialSelection = selection;
				return;
			}

			_filteredTreeCanvas.getUtils().setSelection(selection);
			_filteredTreeCanvas.redraw();
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			_selProvider.fireSelectionChanged();
		}

		@Override
		public Control getControl() {
			return _filteredTreeCanvas;
		}

		@Override
		public void setFocus() {
			_filteredTreeCanvas.setFocus();
		}

		public void refresh() {
			if (_filteredTreeCanvas != null) {
				_viewer.refreshContent();
			}
		}
	}

	public void build() {
		_animCanvas.stop();

		_model.build();

		if (_outliner != null) {
			_outliner.refresh();
		}

		AnimationModel model = _animCanvas.getModel();
		if (model != null) {
			_animCanvas.setModel(model, false);
		}
		_timelineCanvas.setModel(_timelineCanvas.getModel());
	}

	public void deleteAnimations(List<AnimationModel_in_Editor> animations) {
		_model.getAnimations().removeAll(animations);

		if (_outliner != null) {
			_outliner.refresh();
		}

		if (animations.contains(_animCanvas.getModel())) {
			var anim = _model.getAnimations().isEmpty() ? null : _model.getAnimations().get(0);

			selectAnimation((AnimationModel_in_Editor) anim);
		}

		setDirty();
	}

	public void deleteFrames(List<AnimationFrameModel_in_Editor> frames) {

		boolean running = !_animCanvas.isStopped();

		_animCanvas.stop();

		var animation = _animCanvas.getModel();
		animation.getFrames().removeAll(frames);
		animation.buildTimeline();

		if (running) {
			_animCanvas.play();
		} else {
			if (!animation.getFrames().isEmpty()) {
				_animCanvas.showFrame(0);
			}
		}

		_timelineCanvas.getSelectedFrames().clear();
		_timelineCanvas.redraw();

		setDirty();
	}

	public void playOrPause() {
		_animCanvas.playOrPause();
	}

	@Override
	public void saveState(IMemento memento) {
		var anim = getAnimationCanvas().getModel();

		if (anim != null) {
			memento.putString(ANIMATION_KEY, anim.getKey());
		}

		if (_outliner != null) {
			var jsonSate = new JSONObject();
			_outliner.getFilteredTreeCanvas().getCanvas().saveState(jsonSate);
			memento.putString(OUTLINER_TREE_STATE_KEY, jsonSate.toString());
		}
	}

	@Override
	public void restoreState(IMemento memento) {
		_initialAnimtionKey = memento.getString(ANIMATION_KEY);
		{
			var str = memento.getString(OUTLINER_TREE_STATE_KEY);
			if (str != null) {
				_initialOutlinerState = new JSONObject(str);
			}
		}
	}

	public void refreshOutline() {
		if (_outliner != null) {
			_outliner.refresh();
		}
	}

	@SuppressWarnings("boxing")
	public void createAnimationsWithDrop(Object[] data) {

		var openFirstAnim = _model.getAnimations().isEmpty();

		var splitter = new AssetsSplitter();

		for (var obj : data) {
			if (obj instanceof AtlasAssetModel) {
				splitter.addAll(((AtlasAssetModel) obj).getSubElements());
			} else if (obj instanceof MultiAtlasAssetModel) {
				splitter.addAll(((MultiAtlasAssetModel) obj).getSubElements());
			} else if (obj instanceof SpritesheetAssetModel) {
				splitter.addAll(((SpritesheetAssetModel) obj).getFrames());
			} else if (obj instanceof IAssetFrameModel) {
				splitter.add((IAssetKey) obj);
			} else if (obj instanceof ImageAssetModel) {
				splitter.add(((ImageAssetModel) obj).getFrame());
			}
		}

		var result = splitter.split();

		for (var group : result) {
			out.println(group.getPrefix());
			for (var asset : group.getAssets()) {
				out.println("  " + asset.getKey());
			}
		}

		for (var group : result) {
			var anim = new AnimationModel_in_Editor(_model);

			_model.getAnimation(group.getPrefix());

			anim.setKey(_model.getNewAnimationName(group.getPrefix()));

			_model.getAnimations().add(anim);

			for (var frame : group.getAssets()) {

				var animFrame = new AnimationFrameModel_in_Editor(anim);
				animFrame.setFrameAsset((IAssetFrameModel) frame);
				animFrame.setTextureKey(frame.getAsset().getKey());

				if (frame.getAsset() instanceof ImageAssetModel) {
					// nothing
				} else if (frame instanceof SpritesheetAssetModel.FrameModel) {
					animFrame.setFrameName(((SpritesheetAssetModel.FrameModel) frame).getIndex());
				} else {
					animFrame.setFrameName(frame.getKey());
				}

				anim.getFrames().add(animFrame);
			}

			anim.buildTimeline();
		}

		// sort animations

		_model.getAnimations().sort((a, b) -> a.getKey().compareTo(b.getKey()));

		if (_outliner != null) {
			_outliner.refresh();
		}

		if (openFirstAnim) {

			if (!_model.getAnimations().isEmpty()) {
				var anim = (AnimationModel_in_Editor) _model.getAnimations().get(0);
				selectAnimation(anim);
			}

		}

		setDirty();
	}
}
