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
package phasereditor.ide.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.internal.workbench.swt.AbstractPartRenderer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.renderers.swt.TrimmedPartLayout;
import org.eclipse.e4.ui.workbench.renderers.swt.WBWRenderer;
import org.eclipse.e4.ui.workbench.renderers.swt.WorkbenchRendererFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.statushandlers.StatusManager;

import phasereditor.ide.IDEPlugin;

/**
 * @author arian
 *
 */
public class MyRenderFactory extends WorkbenchRendererFactory {
	private MyWBWRenderer _winRenderer;

	@Override
	public AbstractPartRenderer getRenderer(MUIElement uiElement, Object parent) {
		if (uiElement instanceof MWindow) {
			if (_winRenderer == null) {
				_winRenderer = new MyWBWRenderer();
				initRenderer(_winRenderer);
			}
			return _winRenderer;
		}

		return super.getRenderer(uiElement, parent);
	}

}

class MyWBWRenderer extends WBWRenderer {

	@Override
	public Object createWidget(MUIElement element, Object parent) {
		var widget = super.createWidget(element, parent);

		var shell = (Shell) widget;

		var layout = (TrimmedPartLayout) shell.getLayout();

		var toolbar = new HugeToolbar(shell);

		layout.gutterTop = toolbar.getBounds().height;

		return widget;
	}

}

class HugeToolbar extends Composite {

	@SuppressWarnings("unused")
	public HugeToolbar(Composite parent) {
		super(parent, 0);

		setLayout(new RowLayout());

		parent.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				updateBounds();
			}

			@Override
			public void controlMoved(ControlEvent e) {
				updateBounds();
			}
		});

		new PerspectiveHandler(this);

		updateBounds();

	}

	private void updateBounds() {
		var size = computeSize(SWT.DEFAULT, SWT.DEFAULT);
		setBounds(0, 0, getParent().getBounds().width, size.y);
	}

}

class PerspectiveHandler implements IPerspectiveListener {

	private Button _btn;
	private IPerspectiveDescriptor _currentPersp;

	public PerspectiveHandler(Composite parent) {
		_btn = new Button(parent, SWT.PUSH);
		_btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> populateMenu()));
		_btn.addMouseListener(MouseListener.mouseUpAdapter(this::populatePropsMenu));
		updateButton();

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
	}

	private void populatePropsMenu(MouseEvent e) {
		if (e.button == 3) {
			var manager = new MenuManager();
			manager.add(new Action("Reset") {
				@Override
				public void run() {
					getActivePage().resetPerspective();
				}
			});
			showMenu(manager);
		}
	}

	/**
	 * The action for that allows the user to choose any perspective to open.
	 *
	 * @since 3.1
	 */
	private Action _openOtherAction = new Action(WorkbenchMessages.PerspectiveMenu_otherItem) {
		@Override
		public final void runWithEvent(final Event event) {
			runOther(new SelectionEvent(event));
		}
	};

	/**
	 * Show the "other" dialog, select a perspective, and run it. Pass on the
	 * selection event should the menu need it.
	 *
	 * @param event
	 *            the selection event
	 */
	static void runOther(SelectionEvent event) {
		IHandlerService handlerService = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getService(IHandlerService.class);
		try {
			handlerService.executeCommand(IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE, null);
		} catch (Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.WARNING, WorkbenchPlugin.PI_WORKBENCH,
					"Failed to execute " + IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE, e)); //$NON-NLS-1$
		}
	}

	private static String[] EDITOR_PERSPECTIVES = { StartPerspective.ID, CodePerspective.ID, ScenePerspective.ID,
			LabsPerspectiveFactory.ID, "org.eclipse.egit.ui.GitRepositoryExploring" };

	private void populateMenu() {
		var manager = new MenuManager();

		var reg = PlatformUI.getWorkbench().getPerspectiveRegistry();

		for (var id : EDITOR_PERSPECTIVES) {
			var persp = reg.findPerspectiveWithId(id);

			if (persp == null) {
				continue;
			}

			manager.add(new Action(persp.getLabel(), persp.getImageDescriptor()) {
				{
					setDescription(persp.getDescription());
				}

				@Override
				public void run() {
					getActivePage().setPerspective(persp);
				}
			});
		}

		manager.add(_openOtherAction);

		showMenu(manager);
	}

	private void showMenu(MenuManager manager) {
		var menu = manager.createContextMenu(_btn);
		menu.setVisible(true);
	}

	private void updateButton() {
		var persp = getActivePage().getPerspective();

		if (_currentPersp != persp) {
			_currentPersp = persp;
			_btn.setText(persp.getLabel());

			var imgReg = IDEPlugin.getDefault().getImageRegistry();
			var img = imgReg.get(persp.getId());
			if (img == null) {
				img = persp.getImageDescriptor().createImage();
				imgReg.put(persp.getId(), img);
			}

			_btn.setImage(img);
			_btn.setToolTipText(persp.getDescription());

		}
	}

	public Button getButton() {
		return _btn;
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		updateButton();
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		//
	}

	private static IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
}
