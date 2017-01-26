package phasereditor.assetpack.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.editors.AssetPackEditor;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.ui.views.PreviewView;

public class AssetPartsBuildParticipant implements IProjectBuildParticipant {

	public AssetPartsBuildParticipant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		// nothing
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		refreshParts();
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		refreshParts();
	}

	private static void refreshParts() {
		swtRun(new Runnable() {

			@Override
			public void run() {
				if (PlatformUI.getWorkbench().isClosing()) {
					return;
				}

				// explorer

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewReference[] refs = page.getViewReferences();

				// preview windows

				for (IViewReference ref : refs) {
					if (ref.getId().equals(PreviewView.ID)) {
						PreviewView view = (PreviewView) ref.getView(false);
						Object elem = view.getPreviewElement();

						if (elem != null) {
							if (elem instanceof IAssetKey) {
								view.preview(((IAssetKey) elem).getSharedVersion());
							} else if (elem instanceof IFile) {
								if (!((IFile) elem).exists()) {
									elem = null;
								}
								view.preview(elem);
							}
						}
					}
				}

			}

		});

	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		swtRun(new Runnable() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				if (PlatformUI.getWorkbench().isClosing()) {
					return;
				}

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewReference[] refs = page.getViewReferences();

				buildPreviewViews(delta, refs);

				PackDelta packDelta = AssetPackBuildParticipant.getData(env);
				
				buildAssetPackEditors(delta, packDelta, page);
			}

		});
	}

	private static void buildAssetPackEditors(IResourceDelta delta, PackDelta packDelta, IWorkbenchPage page) {

		// all of this is shit, we should check only for resource delta and
		// renamed pack files.

		IEditorReference[] refs = page.getEditorReferences();
		for (IEditorReference ref : refs) {
			if (ref.getId().equals(AssetPackEditor.ID)) {
				AssetPackEditor editor = (AssetPackEditor) ref.getEditor(false);
				if (editor != null) {
					IFile curFile = editor.getEditorInput().getFile();
					try {

						// handle a rename or deletion

						delta.accept(d -> {
							int kind = d.getKind();
							if (kind == IResourceDelta.REMOVED && d.getResource().equals(curFile)) {
								IPath movedTo = d.getMovedToPath();
								if (movedTo == null) {
									page.closeEditor(editor, true);
								} else {
									IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(movedTo);
									editor.handleFileRename(newFile);
								}
							}
							return true;
						});

						// update content

						boolean refresh = false;
						AssetPackModel pack = editor.getModel();
						for (AssetModel asset : packDelta.getAssets()) {
							if (asset.getPack().getFile().equals(pack.getFile())) {
								refresh = true;
								break;
							}
						}

						if (refresh) {
							editor.refresh();
						}

					} catch (CoreException e) {
						AssetPackUI.logError(e);
					}
				}
			}
		}
	}

	private static void buildPreviewViews(IResourceDelta delta, IViewReference[] refs) {
		for (IViewReference ref : refs) {
			if (ref.getId().equals(PreviewView.ID)) {
				PreviewView view = (PreviewView) ref.getView(false);

				if (view == null) {
					continue;
				}

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

	private static void updatePreview(IResourceDelta resourceDelta, PreviewView view, Object elem) {
		IAssetKey key = (IAssetKey) elem;
		IAssetKey shared = key.getSharedVersion();

		if (shared != key) {
			view.preview(shared);
			return;
		}

		if (shared.getAsset().touched(resourceDelta)) {
			view.preview(elem);
		}
	}

}
