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
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.animation.ui.AnimationActions;
import phasereditor.animation.ui.AnimationCanvas;
import phasereditor.animation.ui.editor.properties.AnimationsPropertyPage;
import phasereditor.animation.ui.editor.wizards.AssetsSplitter;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.project.core.PhaserProjectBuilder;
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

	private static final String ANIMATION_KEY = "animation";
	private static final String OUTLINER_TREE_STATE_KEY = "outliner.tree.state";
	public static final String ID = "phasereditor.animation.ui.editor.AnimationsEditor"; //$NON-NLS-1$
	private AnimationsModel_in_Editor _model;
	private AnimationCanvas _animCanvas;
	Outliner _outliner;
	ISelectionChangedListener _outlinerListener;
	private AnimationTimelineCanvas_in_Editor _timelineCanvas;
	private ImageCanvas_Zoom_1_1_Action _zoom_1_1_action;
	private ImageCanvas_Zoom_FitWindow_Action _zoom_fitWindow_action;
	private boolean _dirty;
	private String _initialAnimtionKey;
	JSONObject _initialOutlinerState;
	private Action _deleteAction;
	private Action _newAction;
	private Action _outlineAction;
	private AnimationActions _animationActions;
	private AnimationModel _initialAnimation;

	public AnimationActions getAnimationActions() {
		return _animationActions;
	}

	public ImageCanvas_Zoom_1_1_Action getZoom_1_1_action() {
		return _zoom_1_1_action;
	}

	public ImageCanvas_Zoom_FitWindow_Action getZoom_fitWindow_action() {
		return _zoom_fitWindow_action;
	}

	public Action getDeleteAction() {
		return _deleteAction;
	}

	public Action getNewAction() {
		return _newAction;
	}

	public Action getOutlineAction() {
		return _outlineAction;
	}

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

		_timelineCanvas = new AnimationTimelineCanvas_in_Editor(sash, SWT.BORDER);
		_timelineCanvas.setEditor(this);

		sash.setWeights(new int[] { 2, 1 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {

		createActions();

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
		_animCanvas.setStepCallback(_timelineCanvas::redraw);
		_animCanvas.setPlaybackCallback(_animationActions::animationStatusChanged);
		_animCanvas.addPaintListener(e -> {
			if (_animCanvas.getModel() != null) {
				e.gc.setAlpha(40);
				e.gc.setForeground(_animCanvas.getForeground());
				e.gc.drawText(_animCanvas.getModel().getKey(), 0, 0, true);
			}
		});
		_animCanvas.setZoomWhenShiftPressed(false);

		_timelineCanvas.setZoomWhenShiftPressed(false);

		disableToolbar();

		if (!_model.getAnimations().isEmpty()) {
			_initialAnimation = _model.getAnimations().get(0);
		}

		if (_initialAnimtionKey != null) {
			var opt = _model.getAnimations().stream().filter(a -> a.getKey().equals(_initialAnimtionKey)).findFirst();
			if (opt.isPresent()) {
				_initialAnimation = opt.get();
			}
		}

		init_DND_Support();

		if (PhaserProjectBuilder.isStartupFinished()) {

			if (_initialAnimation != null) {
				loadAnimation(_initialAnimation);
				_initialAnimation = null;
			}
		}

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

	protected final void openNewAnimationDialog(List<AnimationFrameModel> frames) {

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

			var anim = new AnimationModel(getModel());

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

	private void disableToolbar() {
		_animationActions.setEnabled(false);

		_zoom_1_1_action.setEnabled(false);
		_zoom_fitWindow_action.setEnabled(false);
		_deleteAction.setEnabled(false);
	}

	public void dirtyPropertyChanged() {

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

	private void createActions() {

		_animationActions = new AnimationActions(_animCanvas, _timelineCanvas);

		_zoom_1_1_action = new ImageCanvas_Zoom_1_1_Action(_animCanvas);
		_zoom_fitWindow_action = new ImageCanvas_Zoom_FitWindow_Action(_animCanvas);

		_deleteAction = new Action("Delete",
				Workbench.getInstance().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE)) {
			@Override
			public void run() {
				var anim = getAnimationCanvas().getModel();
				if (anim != null) {
					deleteAnimations(List.of(anim));
				}
			}
		};

		_newAction = new Action("New Animation",
				EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_NEW_FRAME_ANIMATION)) {
			@Override
			public void run() {
				openNewAnimationDialog(null);
			}
		};

		_outlineAction = new Action("Quick Outline",
				EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_OUTLINE)) {
			@Override
			public void run() {
				var model = getModel();

				var dlg = new QuickOutlineDialog(getEditorSite().getShell());
				dlg.setModel(model);
				dlg.setSelected(getAnimationCanvas().getModel());

				if (dlg.open() == Window.OK) {
					var selected = dlg.getSelected();
					if (selected != null) {
						selectAnimation(selected);
					}
				}
			}
		};
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

	public boolean isStopped() {
		return _animCanvas.isStopped();
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
			// return new AnimationsPGridPage(this);
			return new AnimationsPropertyPage(this);
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
		var anim = (AnimationModel) elem;
		loadAnimation(anim);
		getEditorSite().getSelectionProvider().setSelection(event.getStructuredSelection());
	}

	public void selectAnimation(AnimationModel anim) {
		StructuredSelection selection = anim == null ? StructuredSelection.EMPTY : new StructuredSelection(anim);

		if (_outliner == null) {

			loadAnimation(anim);

			getEditorSite().getSelectionProvider().setSelection(selection);

		} else {

			_outliner.setSelection(selection);

		}
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
			_viewer = new AnimationsTreeViewer(_filteredTreeCanvas.getTree());

			AssetPackUI.installAssetTooltips(_filteredTreeCanvas.getTree(), _filteredTreeCanvas.getUtils());

			{
				int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
				DropTarget target = new DropTarget(_filteredTreeCanvas.getTree(), options);
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

			if (PhaserProjectBuilder.isStartedFirstTime()) {
				// if the builders are ready, then do this, else, do it on the first refresh.
				_viewer.setInput(getModel());
			}

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
				_outliner.getFilteredTreeCanvas().getTree().restoreState(_initialOutlinerState);
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
				if (_viewer.getInput() == null) {
					_viewer.setInput(getModel());
				} else {
					_viewer.refresh();
				}
			}
		}
	}

	public void build() {
		_model.build();

		if (_outliner != null) {
			_outliner.refresh();
		}

		AnimationModel model = _animCanvas.getModel();

		if (_initialAnimation != null) {
			model = _initialAnimation;
			_initialAnimation = null;
		}

		loadAnimation(model);
	}

	protected void loadAnimation(AnimationModel anim) {
		_animationActions.setChecked(false);
		_animationActions.setEnabled(false);

		if (anim == null) {

			_zoom_1_1_action.setEnabled(false);
			_zoom_fitWindow_action.setEnabled(false);
			_deleteAction.setEnabled(false);

			_animCanvas.setModel(null);
			_timelineCanvas.setModel(null);

			return;
		}

		_animCanvas.setModel(anim, false);

		_animationActions.getPlayAction().setEnabled(true);

		if (_timelineCanvas.getModel() != anim) {
			_timelineCanvas.setModel(anim);
		}

		_zoom_1_1_action.setEnabled(true);
		_zoom_fitWindow_action.setEnabled(true);
		_deleteAction.setEnabled(true);
	}

	public void deleteAnimations(List<AnimationModel> animations) {
		_model.getAnimations().removeAll(animations);

		if (_outliner != null) {
			_outliner.refresh();
		}

		if (animations.contains(_animCanvas.getModel())) {
			var anim = _model.getAnimations().isEmpty() ? null : _model.getAnimations().get(0);

			selectAnimation(anim);
		}

		setDirty();
	}

	public void deleteFrames(List<AnimationFrameModel> frames) {

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
			_outliner.getFilteredTreeCanvas().getTree().saveState(jsonSate);
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
			var anim = new AnimationModel(_model);

			_model.getAnimation(group.getPrefix());

			anim.setKey(_model.getNewAnimationName(group.getPrefix()));

			_model.getAnimations().add(anim);

			for (var frame : group.getAssets()) {

				var animFrame = new AnimationFrameModel(anim);
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
				var anim = _model.getAnimations().get(0);
				selectAnimation(anim);
			}

		}

		setDirty();
	}
}
