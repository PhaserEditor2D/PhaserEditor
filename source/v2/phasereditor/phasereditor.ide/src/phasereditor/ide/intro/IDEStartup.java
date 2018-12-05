package phasereditor.ide.intro;

import static java.lang.System.out;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.ui.SceneUI;
import phasereditor.ui.ColorUtil;
import phasereditor.ui.TreeCanvas;

@SuppressWarnings("hiding")
public class IDEStartup implements IStartup {

	@Override
	public void earlyStartup() {
		Display.getDefault().asyncExec(IDEStartup::registerWorkbenchListeners);

		// force to start the project builders.
		ProjectCore.getBuildParticipants();
	}

	private static void registerWorkbenchListeners() {
		IWorkbench workbench = PlatformUI.getWorkbench();

		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		out.println("Registering quick viewers on window " + window);

		if (window != null) {
			processWindow(window);
		}

		workbench.addWindowListener(new IWindowListener() {

			@Override
			public void windowOpened(IWorkbenchWindow window) {
				processWindow(window);
			}

			@Override
			public void windowDeactivated(IWorkbenchWindow window) {
				//
			}

			@Override
			public void windowClosed(IWorkbenchWindow window) {
				//

			}

			@Override
			public void windowActivated(IWorkbenchWindow window) {
				//

			}
		});
	}

	static void processPage(IWorkbenchPage page) {

		out.println("Registering quick viewers in page " + page);

		for (IViewReference ref : page.getViewReferences()) {
			out.println("\t\tPart " + ref.getId());
			if (ref.getId().endsWith(ProjectExplorer.VIEW_ID)) {
				IWorkbenchPart part = ref.getPart(false);
				if (part != null) {
					installTooltips(part);
				}
			}
		}

		page.addPartListener(new IPartListener() {

			@Override
			public void partOpened(IWorkbenchPart part) {
				if (part instanceof ProjectExplorer) {
					installTooltips(part);
				}
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
				// nothing
			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				// nothing
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
				// nothing
			}

			@Override
			public void partActivated(IWorkbenchPart part) {
				// nothing
			}
		});
	}

	static void processWindow(IWorkbenchWindow window) {

		if ("1".equals(System.getProperty("phasereditor.theme"))) {
			addWindowStyles(window);
		}

		if (window.getActivePage() != null) {
			processPage(window.getActivePage());
		}

		window.addPageListener(new IPageListener() {

			@Override
			public void pageOpened(IWorkbenchPage page) {
				processPage(page);
			}

			@Override
			public void pageClosed(IWorkbenchPage page) {
				//

			}

			@Override
			public void pageActivated(IWorkbenchPage page) {
				//
			}
		});
	}

	private static class MyCTabFolderRenderer extends CTabFolderRenderer {

		protected MyCTabFolderRenderer(CTabFolder parent) {
			super(parent);
		}

		@Override
		protected void draw(int part, int state, Rectangle bounds, GC gc) {
			super.draw(part, state, bounds, gc);

			gc.setAlpha(20);
			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
			gc.drawRectangle(bounds);
			gc.setAlpha(255);
		}

	}

	private static class PaintControlListener implements ControlListener, PaintListener {

		private Color _textBG;
		private Color _listBG;

		public PaintControlListener() {
			_textBG = SWTResourceManager.getColor(30, 30, 30);
			_listBG = SWTResourceManager.getColor(5, 5, 5);
		}

		@Override
		public void controlMoved(ControlEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void controlResized(ControlEvent e) {
			processWidget(e.widget);
		}

		void processWidget(Widget widget) {
			if (widget.getData("-colors-set") == null) {
				widget.setData("-colors-set", "on");

				var display = widget.getDisplay();

				if (widget instanceof Control) {
					var control = (Control) widget;

					control.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
					control.setForeground(SWTResourceManager.getColor(200, 200, 200));

					if (widget instanceof CTabFolder) {
						var folder = (CTabFolder) widget;
						folder.setBorderVisible(false);
						folder.setSelectionBackground(display.getSystemColor(SWT.COLOR_BLACK));
						folder.setSelectionForeground(display.getSystemColor(SWT.COLOR_RED));
						folder.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
						folder.setSimple(true);
						folder.setRenderer(new MyCTabFolderRenderer(folder));
					} else if (widget instanceof Button) {
						var button = (Button) widget;
						button.setForeground(display.getSystemColor(SWT.COLOR_RED));
					} else if (widget instanceof Label) {
						var label = (Label) widget;
						if ((label.getFont().getFontData()[0].getStyle() & SWT.BOLD) == SWT.BOLD) {
							label.setForeground(SWTResourceManager.getColor(ColorUtil.DARKORANGE.rgb));
						}

						if ((label.getStyle() & SWT.SEPARATOR) == SWT.SEPARATOR) {
							label.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
						}
					} else if (widget instanceof Text) {
						var text = (Text) widget;
						text.setForeground(SWTResourceManager.getColor(ColorUtil.GREEN.rgb));
						text.setBackground(_textBG);
					} else if (widget instanceof TreeCanvas) {
						var tree = (TreeCanvas)widget;
						tree.setBackground(_listBG);
					}
				}

				if (widget instanceof Composite) {
					for (var c : ((Composite) widget).getChildren()) {
						processWidget(c);
					}
				}
			}

			if (widget instanceof Composite) {
				for (var c : ((Composite) widget).getChildren()) {
					if (c.getData("-listener-set") == null) {
						c.setData("-listener-set", "on");
						c.addControlListener(this);
					}
				}
			}
		}

		@Override
		public void paintControl(PaintEvent e) {
			processWidget(e.widget);
		}

	}

	private static void addWindowStyles(IWorkbenchWindow window) {
		var shell = window.getShell();

		var listener = new PaintControlListener();
		shell.addControlListener(listener);
		listener.processWidget(shell);

	}

	static List<WeakReference<CommonViewer>> _used = new ArrayList<>();

	static void installTooltips(IWorkbenchPart part) {
		out.println("Installing ProjectExplorer quick viewers");
		CommonViewer viewer = ((ProjectExplorer) part).getCommonViewer();

		for (WeakReference<CommonViewer> ref : _used) {
			CommonViewer usedViewer = ref.get();
			if (viewer == usedViewer) {
				return;
			}
		}

		_used.add(new WeakReference<>(viewer));

		AssetPackUI.installAssetTooltips(viewer);
		SceneUI.installSceneTooltips(viewer);
	}

}
