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
package phasereditor.ide.intro;

import static java.lang.System.out;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.application.DelayedEventsProcessor;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchAdvisor;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.canvas.ui.CanvasUI;

/*
 * 
 * To be removed!!!
 * 
 */
@Deprecated
@SuppressWarnings("restriction")
public class PhaserWorkbenchAdvisor extends IDEWorkbenchAdvisor {

	public PhaserWorkbenchAdvisor() {
		super();
	}

	public PhaserWorkbenchAdvisor(DelayedEventsProcessor processor) {
		super(processor);
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return "phasereditor.ide.ui.perspective";
	}

	@Override
	public void preStartup() {
		super.preStartup();

		registerWorkbenchListeners();
	}

	static List<WeakReference<CommonViewer>> _used = new ArrayList<>();

	static void installTooltips(IWorkbenchPart part) {
		out.println("Installing ProjectExplorer tooltips");
		CommonViewer viewer = ((ProjectExplorer) part).getCommonViewer();

		for (WeakReference<CommonViewer> ref : _used) {
			CommonViewer usedViewer = ref.get();
			if (viewer == usedViewer) {
				return;
			}
		}

		_used.add(new WeakReference<>(viewer));

		AssetPackUI.installAssetTooltips(viewer);
		CanvasUI.installCanvasTooltips(viewer);
	}

	private static void registerWorkbenchListeners() {
		IWorkbench workbench = PlatformUI.getWorkbench();

		if (workbench.getActiveWorkbenchWindow() != null) {
			processWindow(workbench.getActiveWorkbenchWindow());
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
		for (IViewReference ref : page.getViewReferences()) {
			if (ref.getId().endsWith(ProjectExplorer.VIEW_ID)) {
				IWorkbenchPart part = ref.getPart(false);
				if (part != null) {
					AssetPackUI.installAssetTooltips(((ProjectExplorer) part).getCommonViewer());
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

//@formatter:off
// XXX: just do not load the JS editor at startup, it creates a strange
// behavior!
	
//	@Override
//	public void postStartup() {
//		super.postStartup();
//
//		// an ugly work around to ensure all the JS stuff is loaded at the
//		// startup
//
//		IEditorPart editor;
//
//		long t = currentTimeMillis();
//
//		try {
//			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//			editor = IDE.openEditor(page, new StringEditorInput("closing", ""),
//					"org.eclipse.wst.jsdt.ui.CompilationUnitEditor");
//
//			out.println("Loaded " + editor + " " + (currentTimeMillis() - t));
//
//			page.closeEditor(editor, false);
//
//		} catch (PartInitException e) {
//			e.printStackTrace();
//		}
//	}
//@formatter:on
}
