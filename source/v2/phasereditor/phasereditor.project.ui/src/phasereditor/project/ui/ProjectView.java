// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.project.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

import phasereditor.project.core.ProjectCore;
import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class ProjectView extends ViewPart implements Consumer<IProject> {

	public static final String ID = "phasereditor.project.ui.projectView";
	private FilteredTreeCanvas _filteredTree;
	private TreeCanvasViewer _viewer;
	private IPartListener _partListener;

	@Override
	public void createPartControl(Composite parent) {
		_filteredTree = new FilteredTreeCanvas(parent, SWT.NONE);
		_viewer = new MyTreeViewer();
		_viewer.setInput(new Object());
		_filteredTree.getTree().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				openEditor();
			}
		});

		createContextMenu();

		getViewSite().setSelectionProvider(_viewer);

		ProjectCore.addActiveProjectListener(this);

		accept(ProjectCore.getActiveProject());

		registerWorkbenchListeners();

		// we need this to show the right icons, because some content types are not
		// resolved at the first time.
		swtRun(4000, this::refresh);
	}

	private void registerWorkbenchListeners() {
		_partListener = new IPartListener() {

			@Override
			public void partOpened(IWorkbenchPart part) {
				//
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
				//

			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				//

			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
				//

			}

			@Override
			public void partActivated(IWorkbenchPart part) {
				try {
					if (part instanceof IEditorPart) {
						var input = ((IEditorPart) part).getEditorInput();
						if (input instanceof IFileEditorInput) {
							var file = ((IFileEditorInput) input).getFile();
							var project = file.getProject();
							if (project.equals(ProjectCore.getActiveProject())) {
								reveal(file);
							}
						}
					}
				} catch (NullPointerException e) {
					// may happen
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	protected void reveal(IFile file) {
		_viewer.setSelection(new StructuredSelection(file), true);
	}

	class MyTreeViewer extends TreeCanvasViewer {

		private List<IFileRendererProvider> _renderProviders;

		public MyTreeViewer() {
			super(_filteredTree.getTree(), new MyContentProvider(),
					WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());

			_renderProviders = new ArrayList<>();
			var elems = Platform.getExtensionRegistry()
					.getConfigurationElementsFor("phasereditor.project.ui.fileRenderer");
			for (var elem : elems) {
				try {
					IFileRendererProvider provider = (IFileRendererProvider) elem.createExecutableExtension("class");
					_renderProviders.add(provider);
				} catch (CoreException e) {
					ProjectUI.logError(e);
				}
			}
		}

		@Override
		protected void setItemIconProperties(TreeCanvasItem item) {
			BaseTreeCanvasItemRenderer renderer = null;

			if (item.getData() instanceof IFile) {
				for (var provider : _renderProviders) {
					var renderer2 = provider.createRenderer(item);
					if (renderer2 != null) {
						renderer = renderer2;
						break;
					}
				}
			}

			if (renderer == null) {

				super.setItemIconProperties(item);
			} else {
				item.setRenderer(renderer);
			}
		}
	}

	private void createContextMenu() {
	}

	protected void openEditor() {
		var sel = (IStructuredSelection) _viewer.getSelection();
		var obj = sel.getFirstElement();
		if (obj != null) {
			if (obj instanceof IFile) {
				try {
					IDE.openEditor(getViewSite().getPage(), (IFile) obj);
				} catch (PartInitException e) {
					ProjectCore.logError(e);
				}
			}
		}
	}

	@Override
	public void dispose() {
		ProjectCore.removeActiveProjectListener(this);
		getViewSite().getPage().removePartListener(_partListener);
		super.dispose();
	}

	@Override
	public void setFocus() {
		_filteredTree.setFocus();
	}

	@Override
	public void accept(IProject project) {
		_viewer.refresh();
	}

	class MyContentProvider implements ITreeContentProvider {

		public MyContentProvider() {
			super();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			var project = ProjectCore.getActiveProject();

			if (project == null) {
				return new Object[0];
			}

			return new Object[] { project };
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IContainer) {
				try {
					var members = ((IContainer) parentElement).members();
					return Arrays.stream(members)

							.filter(m -> !m.getName().startsWith("."))

							.sorted((a, b) -> {

								var a1 = a instanceof IContainer ? 0 : 1;
								var b1 = b instanceof IContainer ? 0 : 1;

								return Integer.compare(a1, b1);

							})

							.toArray();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof IResource) {
				return ((IResource) element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

	}

	public void refresh() {
		_viewer.getTree().setRedraw(false);
		var expanded = _viewer.getExpandedElements();
		_viewer.refresh();
		_viewer.setExpandedElements(expanded);
		_viewer.getTree().setRedraw(true);
		_viewer.getTree().redraw();
	}

}
