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
package phasereditor.project.ui.build;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import phasereditor.project.core.IProjectBuildParticipant;

/**
 * @author arian
 *
 */
public abstract class BaseEditorBuildParticipant<T extends EditorPart> implements IProjectBuildParticipant{

	protected final HashSet<String> _editorIdSet;

	public BaseEditorBuildParticipant(String... editorId) {
		_editorIdSet = new HashSet<>(List.of(editorId));
	}
	
	protected abstract void buildEditor(T editor);
	
	protected abstract void reloadEditorFile(T editor);
	
	protected abstract IFile getEditorFile(T editor);
	
	
	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		rebuildEditors();
	}

	private void rebuildEditors() {
		rebuildEditors(null);
	}

	private  void rebuildEditors(IResourceDelta delta) {
		swtRun(new Runnable() {

			@Override
			public void run() {
				var refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for (var ref : refs) {
					if (_editorIdSet.contains(ref.getId())) {
						@SuppressWarnings("unchecked")
						var editor = (T) ref.getEditor(false);
						if (editor != null) {
							if (delta == null) {
								buildEditor(editor);
							} else {
								var file = getEditorFile(editor);
								var editorFileChanged = new boolean[] { false };
								try {
									delta.accept(d -> {
										IResource resource = d.getResource();
										if (file.equals(resource)) {
											editorFileChanged[0] = true;
											return false;
										}
										return true;
									});
								} catch (CoreException e) {
									e.printStackTrace();
								}

								if (editorFileChanged[0]) {
									if (!editor.isDirty()) {
										reloadEditorFile(editor);
									}
								
								} else {
									buildEditor(editor);
								}
							}
						}
					}
				}
			}
		});
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		rebuildEditors();
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		rebuildEditors(delta);
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		rebuildEditors();
	}

	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		rebuildEditors();
	}
	
}
