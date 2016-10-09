package phasereditor.assetpack.ui;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.ui.views.PreviewView;

public class AssetPreviewBuildParticipant implements IProjectBuildParticipant {

	public AssetPreviewBuildParticipant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		Display.getDefault().asyncExec(new Runnable() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				if (PlatformUI.getWorkbench().isClosing()) {
					return;
				}

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewReference[] refs = page.getViewReferences();

				for (IViewReference ref : refs) {
					if (ref.getId().equals(PreviewView.ID)) {
						PreviewView view = (PreviewView) ref.getView(true);
						Object elem = view.getPreviewElement();

						if (elem != null) {
							if (elem instanceof IAssetKey) {
								updatePreview(delta, view, elem);
								continue;
							}
						}

						try {
							delta.accept(r -> {
								IResource resource = r.getResource();

								if (resource.equals(elem)) {

									if (r.getKind() == IResourceDelta.REMOVED) {
										view.preview(null);
										return false;
									}

									view.preview(elem);

									return false;
								}

								return true;
							});
						} catch (CoreException e) {
							e.printStackTrace();
						}

					}
				}
			}
		});

	}

	private static void updatePreview(IResourceDelta resourceDelta, PreviewView view, Object elem) {
		IAssetKey key = (IAssetKey) elem;
		IAssetKey shared = key.getSharedVersion();

		if (shared != key) {
			view.preview(shared);
			return;
		}

		if (shared.touched(resourceDelta)) {
			view.preview(elem);
		}
	}

}
