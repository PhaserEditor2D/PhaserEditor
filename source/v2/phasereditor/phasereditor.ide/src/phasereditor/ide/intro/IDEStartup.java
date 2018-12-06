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
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
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
		private Color _BG;
		private Color _FG;
		private Color _folderSelectionFG;
		private Color _folderFG;
		private Color _folderSelectionBG;
		private Color _buttonFG;
		private Color _separatorColor;
		private Color _boldLabelFG;
		private Color _textFG;
		private Color _folderBG;

		
		private static Color rgb(String hex) {
			return rgb(ColorUtil.web(hex));
		}
		
		private static Color rgb(RGBA rgba) {
			return SWTResourceManager.getColor(rgba.rgb);
		}

		private static Color rgb(int swt) {
			return SWTResourceManager.getColor(swt);
		}

		private static Color rgb(int r, int g, int b) {
			return SWTResourceManager.getColor(r, g, b);
		}

		@SuppressWarnings("unused")
		public PaintControlListener() {
			// spaceship

			var darkGray = rgb(52, 52, 52);
			var darkGray2 = rgb(72, 72, 72);
			var lightGray = rgb(86, 86, 85);
			var darkWhite = rgb(170, 170, 170);
			var black = rgb(0, 0, 0);
			var blue = rgb(89, 128, 183);
			var green = rgb(0, 65, 58);
			var white = rgb(255, 255, 255);

			_BG = lightGray;
			_FG = black;
			_textBG = darkGray;
			_textFG = white;
			_listBG = _BG;
			_folderSelectionFG = white;
			_folderSelectionBG = lightGray;
			_folderFG = _FG;
			_folderBG = _BG;
			_buttonFG = white;
			_separatorColor = _FG;
			_boldLabelFG = black;

			// black

			_BG = rgb(0, 0, 0);
			_FG = rgb(200, 200, 200);
			_textBG = rgb(30, 30, 30);
			_textFG = rgb(SWT.COLOR_GREEN);
			_listBG = rgb(5, 5, 5);
			_folderSelectionFG = rgb(SWT.COLOR_RED);
			_folderSelectionBG = rgb(SWT.COLOR_BLACK);
			_folderFG = rgb(SWT.COLOR_DARK_GRAY);
			_folderBG = _BG;
			_buttonFG = rgb(SWT.COLOR_RED);
			_separatorColor = rgb(SWT.COLOR_DARK_GRAY);
			_boldLabelFG = rgb(ColorUtil.LIGHTBLUE);

			// soyuz

			{
				_BG = rgb(ColorUtil.web("#D5CCC3"));
				_FG = rgb(0, 0, 0);
				_textBG = rgb("#E0D9D1");
				_textFG = _FG;
				_listBG = _BG;
				_folderSelectionFG = _FG;
				_folderSelectionBG = _textBG;
				_folderFG = _FG;
				_folderBG = _BG;
				_buttonFG = _FG;
				_separatorColor =  rgb("#E0D9D1");
				_boldLabelFG = _FG;
			}
			
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

				if (widget instanceof Control) {
					var control = (Control) widget;

					control.setBackground(_BG);
					control.setForeground(_FG);

					if (widget instanceof CTabFolder) {
						var folder = (CTabFolder) widget;
						folder.setBorderVisible(false);
						folder.setSelectionBackground(_folderSelectionBG);
						folder.setSelectionForeground(_folderSelectionFG);
						folder.setBackground(_folderBG);
						folder.setForeground(_folderFG);
						folder.setSimple(true);
						folder.setRenderer(new MyCTabFolderRenderer(folder));
					} else if (widget instanceof Button) {
						var button = (Button) widget;
						button.setForeground(_buttonFG);
					} else if (widget instanceof Label) {
						var label = (Label) widget;
						if ((label.getFont().getFontData()[0].getStyle() & SWT.BOLD) == SWT.BOLD) {
							label.setForeground(_boldLabelFG);
						}

						if ((label.getStyle() & SWT.SEPARATOR) == SWT.SEPARATOR) {
							label.setBackground(_separatorColor);
						}
					} else if (widget instanceof Text) {
						var text = (Text) widget;
						text.setForeground(_textFG);
						text.setBackground(_textBG);
					} else if (widget instanceof TreeCanvas) {
						var tree = (TreeCanvas) widget;
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
