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
package phasereditor.ui.views;

import static java.lang.System.currentTimeMillis;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.SwtRM;

public class PreviewView extends ViewPart implements IShowInTarget {

	private static final String ELEM_FACTORY_KEY = "phasereditor.ui.preview.elemenFactoryId";
	public static final String ID = "phasereditor.ui.preview"; //$NON-NLS-1$
	private Composite _previewContainer;
	// private Canvas _noPreviewComp;
	private Composite _noPreviewComp;
	private IPreviewFactory _previewFactory;
	private IAdaptable _initalElement;
	private Object _previewElement;
	private Control _previewControl;
	private IMemento _initialMemento;

	public PreviewView() {
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		_previewContainer = new Composite(parent, SWT.NONE);
		StackLayout sl_previewContainer = new StackLayout();
		_previewContainer.setLayout(sl_previewContainer);

		_noPreviewComp = new Composite(_previewContainer, SWT.NONE);
		_noPreviewComp.setBackground(SwtRM.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		// _noPreviewComp = new Canvas(_previewContainer, SWT.NO_BACKGROUND |
		// SWT.DOUBLE_BUFFERED);
		// _noPreviewComp.addPaintListener(new PaintListener() {
		// @Override
		// public void paintControl(PaintEvent e) {
		// paintNoPreviewComp(e);
		// }
		// });
		_noPreviewComp.setLayout(new GridLayout(1, false));
		Label lblDropItHere = new Label(_noPreviewComp, SWT.NONE);
		lblDropItHere.setText("Drop it here");
		lblDropItHere.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));

		initializeToolBar();
		initializeMenu();
		afterCreateWidgets();
	}

	@Override
	public void saveState(IMemento memento) {
		if (_previewFactory != null && _previewElement != null) {
			IPersistableElement persistable = _previewFactory.getPersistable(_previewElement);
			if (persistable != null) {
				String id = persistable.getFactoryId();
				memento.putString(ELEM_FACTORY_KEY, id);
				persistable.saveState(memento);
			}

			if (_previewControl != null) {
				_previewFactory.savePreviewControl(_previewControl, memento);
			}
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		try {
			if (memento != null) {
				String id = memento.getString(ELEM_FACTORY_KEY);
				if (id != null) {
					IElementFactory elemFactory = PlatformUI.getWorkbench().getElementFactory(id);
					if (elemFactory != null) {
						_initalElement = elemFactory.createElement(memento);
						_initialMemento = memento;
					}
				}
			}
		} catch (Exception e) {
			// just keep the calm, should be many reason because the cannot be
			// recovered.
			e.printStackTrace();
		}
	}

	// protected void paintNoPreviewComp(PaintEvent e) {
	// GC gc = e.gc;
	// Rectangle b = _noPreviewComp.getBounds();
	// {
	// PhaserEditorUI.paintPreviewBackground(gc, b);
	// PhaserEditorUI.paintPreviewMessage(gc, b, "Drop it here");
	// }
	// }

	private void afterCreateWidgets() {
		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(_previewContainer, options);
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			target.setTransfer(types);
			target.addDropListener(new DropTargetAdapter() {
				@Override
				public void drop(DropTargetEvent event) {
					if (event.data instanceof Object[]) {
						selectionDropped((Object[]) event.data);
					}
					if (event.data instanceof IStructuredSelection) {
						selectionDropped(((IStructuredSelection) event.data).toArray());
					}
				}
			});
		}

		{
			StackLayout layout = (StackLayout) _previewContainer.getLayout();
			layout.topControl = _noPreviewComp;
			_previewContainer.layout();
		}

		if (_initalElement == null) {
			fillToolbarWithCommonActions();
		} else {
			preview(_initalElement);
			if (_previewFactory != null && _initialMemento != null) {
				_previewFactory.initPreviewControl(_previewControl, _initialMemento);
			}
		}

	}

	private void initializeToolBar() {
		@SuppressWarnings("unused")
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
	}

	@SuppressWarnings("unused")
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		_previewContainer.setFocus();
	}

	public void selectionDropped(Object[] data) {
		for (Object elem : data) {
			preview(elem);
			break; // TODO:it could open new preview windows.
		}
	}

	public void refresh() {
		if (_previewElement != null) {
			preview(_previewElement);
		}
	}

	public boolean preview(Object elem) {
		boolean availablePreview = false;
		Control preview = null;

		IPreviewFactory previewFactory = null;
		Control previewControl = null;

		boolean updateTitleImage = false;

		if (elem == null) {
			preview = _noPreviewComp;
			setPartName("Preview");
			updateTitleImage = true;
		} else {
			IPreviewFactory factory = Adapters.adapt(elem, IPreviewFactory.class);
			if (factory != null) {
				// try to reuse some of the already created controls
				for (Control c : _previewContainer.getChildren()) {
					if (factory.canReusePreviewControl(c, elem)) {
						preview = c;
						break;
					}
				}

				// if there are not reusable controls, then create one
				if (preview == null) {
					preview = factory.createControl(_previewContainer);
				}

				previewFactory = factory;
				previewControl = preview;

				availablePreview = true;
			}
		}

		// update layout

		if (preview != null) {

			if (_previewFactory != null && _previewControl != null) {
				_previewFactory.hiddenControl(_previewControl);
			}

			IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();
			toolbar.removeAll();

			// do the factory stuff
			if (previewFactory != null) {

				previewFactory.updateToolBar(toolbar, preview);

				previewFactory.updateControl(preview, elem);

				setPartName("Preview - " + previewFactory.getTitle(elem));

				Image icon = previewFactory.getIcon(elem);
				if (icon != null) {
					setTitleImage(icon);
					updateTitleImage = false;
				}
			}

			_previewFactory = previewFactory;
			_previewControl = previewControl;

			StackLayout layout = (StackLayout) _previewContainer.getLayout();
			layout.topControl = preview;
			_previewContainer.layout();

			_previewElement = elem;

			// fill toolbar common actions

			if (toolbar.getItems().length > 0) {
				toolbar.add(new Separator());
			}

			fillToolbarWithCommonActions();
			toolbar.update(true);

			if (updateTitleImage) {
				setTitleImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_MONITOR));
			}
		}

		return availablePreview;
	}

	private void fillToolbarWithCommonActions() {
		IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();

		if (_previewElement != null) {
			toolbar.add(new Action("Clear") {
				{
					setImageDescriptor(EditorSharedImages
							.getImageDescriptor("platform:/plugin/org.eclipse.ui/icons/full/etool16/clear.png"));
				}

				@Override
				public void run() {
					preview(null);
				}
			});

			toolbar.add(new Action("Refresh", EditorSharedImages
					.getImageDescriptor("platform:/plugin/org.eclipse.ui/icons/full/elcl16/refresh_nav.png")) {
				@Override
				public void run() {
					refresh();
				}
			});
		}

		toolbar.add(new Action("Open New Preview Window") {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_NEW_VIEW));
			}

			@Override
			public void run() {
				try {
					IWorkbenchWindow window = getSite().getWorkbenchWindow();
					IWorkbenchPage page = window.getActivePage();
					PreviewView view = (PreviewView) page.showView(PreviewView.ID,
							PreviewView.ID + "@" + currentTimeMillis(), IWorkbenchPage.VIEW_CREATE);
					page.activate(view);
					if (getPreviewElement() != null) {
						view.preview(getPreviewElement());
					}
				} catch (PartInitException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		});

		toolbar.update(true);
	}

	public Object getPreviewElement() {
		return _previewElement;
	}

	@Override
	public boolean show(ShowInContext context) {
		ISelection sel = context.getSelection();
		if (sel instanceof IStructuredSelection) {
			Object elem = ((IStructuredSelection) sel).getFirstElement();
			if (elem == null) {
				return false;
			}
			return preview(elem);
		}
		return false;
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
					IContext context = HelpSystem.getContext("phasereditor.help.preview");
					return context;
				}
			};
		}
		return super.getAdapter(adapter);
	}
}
