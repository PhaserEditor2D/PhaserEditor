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
package phasereditor.animation.ui;

import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.json.JSONObject;

import javafx.animation.Animation.Status;
import phasereditor.animation.ui.properties.AnimationsPGridPage;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.animations.AnimationCanvas;
import phasereditor.assetpack.ui.animations.AnimationCanvas.IndexTransition;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredContentOutlinePage;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class AnimationsEditor extends EditorPart {

	public static final String ID = "phasereditor.animation.ui.AnimationsEditor"; //$NON-NLS-1$
	private AnimationsModel_in_Editor _model;
	private AnimationCanvas _animCanvas;
	Outliner _outliner;
	ISelectionChangedListener _outlinerListener;
	private AnimationTimelineCanvas _timelineCanvas;
	private Action _playAction;
	private Action _pauseAction;
	private Action _stopAction;
	private Action[] _playbackActions = { _playAction, _pauseAction, _stopAction };

	public AnimationsEditor() {
		// force the start the project builders
		ProjectCore.getBuildParticipants();
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

		_timelineCanvas = new AnimationTimelineCanvas(sash, SWT.BORDER);
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
				var anim = getTimelineCanvas().getAnimation();

				if (anim != null) {
					getTimelineCanvas().clearSelection();
					getEditorSite().getSelectionProvider().setSelection(new StructuredSelection(anim));
				}
			}
		});
		_animCanvas.setNoImageMessage("");
		_animCanvas.setStepCallback(_timelineCanvas::redraw);
		_animCanvas.setPlaybackCallback(this::animationStatusChanged);

		for (var btn : _playbackActions) {
			btn.setEnabled(false);
		}
	}

	private void animationStatusChanged(Status status) {

		AnimationCanvas animCanvas = getAnimationCanvas();
		var anim = animCanvas.getModel();
		var frames = anim.getFrames();

		switch (status) {
		case RUNNING:
			_playAction.setChecked(true);
			_pauseAction.setChecked(false);
			break;
		case STOPPED:
			if (!frames.isEmpty()) {
				animCanvas.showFrame(0);
			}
			_playAction.setChecked(false);
			_pauseAction.setEnabled(false);
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
				if (transition != null) {
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
					transition.play();
				} else {
					canvas.play();
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

		manager.add(_playAction);
		manager.add(_pauseAction);
		manager.add(_stopAction);

		_playbackActions = new Action[] { _playAction, _pauseAction, _stopAction };

		return manager.createControl(parent);
	}

	@Override
	public void setFocus() {
		_animCanvas.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Do the Save operation
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

		try (InputStream contents = file.getContents()) {
			JSONObject jsonData = JSONObject.read(contents);
			_model = new AnimationsModel_in_Editor(this, jsonData);
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
		return false;
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

	public AnimationTimelineCanvas getTimelineCanvas() {
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
		}
		return _outliner;
	}

	public Outliner getOutliner() {
		return _outliner;
	}

	protected void outliner_selectionChanged(SelectionChangedEvent event) {
		var elem = event.getStructuredSelection().getFirstElement();
		if (elem != null) {
			var anim = (AnimationModel_in_Editor) elem;
			loadAnimation(anim);
		}
	}

	private void loadAnimation(AnimationModel_in_Editor anim) {
		_animCanvas.setModel(anim, false);

		for (var btn : _playbackActions) {
			btn.setChecked(false);
			btn.setEnabled(btn == _playAction);
		}

		if (_timelineCanvas.getAnimation() != anim) {
			_timelineCanvas.setAnimation(anim);
		}
	}

	class Outliner extends FilteredContentOutlinePage {
		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);

			TreeViewer viewer = getTreeViewer();
			viewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
			viewer.setContentProvider(new OutlinerContentProvider());
			viewer.setInput(getModel());

			// viewer.getControl().setMenu(getMenuManager().createContextMenu(viewer.getControl()));
		}

		@Override
		public void dispose() {
			removeSelectionChangedListener(_outlinerListener);
			AnimationsEditor.this._outliner = null;
			super.dispose();
		}
	}

	private static class OutlinerContentProvider implements ITreeContentProvider {

		public OutlinerContentProvider() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof AnimationsModel_in_Editor) {
				return ((AnimationsModel_in_Editor) parentElement).getAnimations().toArray();
			}
			return new Object[] {};
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

	}

	public void build() {
		_model.build();
		if (_outliner != null) {
			_outliner.refresh();
		}
	}
}
