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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.json.JSONObject;

import phasereditor.animation.ui.properties.AnimationsPGridPage;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.animations.AnimationCanvas;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.FilteredContentOutlinePage;

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
		_animCanvas = new AnimationCanvas(sash, SWT.BORDER);
		_timelineCanvas = new AnimationTimelineCanvas(sash, SWT.BORDER);
		_timelineCanvas.setEditor(this);
		_animCanvas.setStepCallback(_timelineCanvas::redraw);

		sash.setWeights(new int[] { 2, 1 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// nothing for now
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

			if (_animCanvas.getModel() == elem) {
				_animCanvas.play();
			} else {
				_animCanvas.setModel(anim);
			}

			if (_timelineCanvas.getAnimation() != anim) {
				_timelineCanvas.setAnimation(anim);
			}

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
