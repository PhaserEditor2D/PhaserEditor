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
package phasereditor.audiosprite.ui.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import phasereditor.audiosprite.core.AudioSpritesModel;

public class AudioSpriteEditor extends EditorPart implements IResourceChangeListener {

	private AudioSpriteEditorComp _comp;
	private AudioSpritesModel _model;
	protected boolean _dirty;

	public AudioSpriteEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		String content = _model.toJSON().toString(2);
		IFileEditorInput input = (IFileEditorInput) getEditorInput();
		try {
			IFile file = input.getFile();
			ByteArrayInputStream source = new ByteArrayInputStream(content.getBytes());
			file.setContents(source, IResource.FORCE, monitor);
			_dirty = false;
			firePropertyChange(PROP_DIRTY);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doSaveAs() {
		// nothing
	}

	@Override
	public boolean isDirty() {
		return _dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		_comp = new AudioSpriteEditorComp(parent, SWT.NONE);

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		if (_model != null) {
			_comp.setModel(_model);
			_comp.addPropertyChangeListener(new PropertyChangeListener() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					_dirty = true;
					firePropertyChange(PROP_DIRTY);
				}
			});
		}

		getWorkspace().addResourceChangeListener(this);

	}

	@Override
	public void setFocus() {
		_comp.setFocus();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		if (_model == null) {
			_model = new AudioSpritesModel(getEditorInputFile());
			if (_comp != null) {
				_comp.setModel(_model);
			}
		} else {
			_model.setModelFile(getEditorInputFile());
		}
		updateTitle();
	}

	public AudioSpritesModel getModel() {
		return _model;
	}

	public IFile getEditorInputFile() {
		return ((IFileEditorInput) getEditorInput()).getFile();
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IWorkspaceRoot root = getWorkspaceRoot();
					IFile thisFile = getEditorInputFile();
					IResource deltaFile = delta.getResource();
					if (deltaFile.equals(thisFile)) {
						if (delta.getKind() == IResourceDelta.REMOVED) {
							IPath movedTo = delta.getMovedToPath();
							if (movedTo == null) {
								// delete
								closeEditorBecauseFileDeleted();
							} else {
								// rename
								setInput(new FileEditorInput(root.getFile(movedTo)));
								updateTitle();
							}
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	protected void closeEditorBecauseFileDeleted() {
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {

			@Override
			public void run() {
				getSite().getPage().closeEditor(AudioSpriteEditor.this, false);
			}
		});
	}

	@Override
	public void dispose() {
		getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	protected void updateTitle() {
		Display.getDefault().asyncExec(new Runnable() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				setPartName(getEditorInputFile().getName());
				firePropertyChange(PROP_TITLE);
			}
		});
	}

	static IWorkspaceRoot getWorkspaceRoot() {
		return getWorkspace().getRoot();
	}

	private static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IContextProvider.class)) {
			return new IContextProvider() {

				@Override
				public String getSearchExpression(Object target) {
					return null;
				}

				@Override
				public int getContextChangeMask() {
					return NONE;
				}

				@Override
				public IContext getContext(Object target) {
					IContext context = HelpSystem.getContext("phasereditor.help.audiospriteseditor");
					return context;
				}
			};
		}
		return super.getAdapter(adapter);
	}
}
