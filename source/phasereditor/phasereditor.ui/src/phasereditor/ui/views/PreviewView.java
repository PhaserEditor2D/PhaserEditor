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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PhaserEditorUI;

public class PreviewView extends ViewPart implements IShowInTarget {

	private static final String ELEM_FACTORY_KEY = "phasereditor.ui.preview.elemenFactoryId";
	public static final String ID = "phasereditor.ui.preview"; //$NON-NLS-1$
	private Composite _previewContainer;
	private Canvas _noPreviewComp;
	private IPreviewFactory _previewFactory;
	private IAdaptable _initalElement;
	private Object _previewElement;
	private Control _previewControl;

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

		_noPreviewComp = new Canvas(_previewContainer, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);
		_noPreviewComp.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				paintNoPreviewComp(e);
			}
		});
		_noPreviewComp.setLayout(new GridLayout(1, false));

		createActions();
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
					}
				}
			}
		} catch (Exception e) {
			// just keep the calm, should be many reason because the cannot be
			// recovered.
			e.printStackTrace();
		}
	}

	protected void paintNoPreviewComp(PaintEvent e) {
		GC gc = e.gc;
		Rectangle b = _noPreviewComp.getBounds();
		{
			PhaserEditorUI.paintPreviewBackground(gc, b);
			PhaserEditorUI.paintPreviewMessage(gc, b, "Drop it here");
		}
	}

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

		if (_initalElement != null) {
			preview(_initalElement);
		}
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		// nothing
	}

	/**
	 * Initialize the toolbar.
	 */
	@SuppressWarnings("unused")
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
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
		if (_previewFactory != null && _previewControl != null) {
			_previewFactory.hiddenControl(_previewControl);
		}

		boolean availablePreview = false;
		Control preview = null;

		_previewFactory = null;
		_previewControl = null;

		setTitleImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_EYE));

		if (elem == null) {
			preview = _noPreviewComp;
			setPartName("Preview");
		} else {
			IAdapterManager adapterManager = Platform.getAdapterManager();

			IPreviewFactory factory = adapterManager.getAdapter(elem, IPreviewFactory.class);
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

				factory.updateControl(preview, elem);
				setPartName("Preview - " + factory.getTitle(elem));

				Image icon = factory.getIcon(elem);
				if (icon != null) {
					setTitleImage(icon);
				}

				_previewFactory = factory;
				_previewControl = preview;

				availablePreview = true;
			}
		}

		if (preview != null) {
			StackLayout layout = (StackLayout) _previewContainer.getLayout();
			layout.topControl = preview;
			_previewContainer.layout();
		}

		_previewElement = elem;
		return availablePreview;
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
