package phasereditor.ide.intro;

import static java.lang.System.out;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;
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

import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.ui.SceneUI;

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
