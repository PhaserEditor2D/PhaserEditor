package phasereditor.assetpack.ui;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import phasereditor.animation.ui.model.AnimationsModel_Persistable;
import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.views.PreviewView;

public class AssetPartsBuildParticipant implements IProjectBuildParticipant {

	public AssetPartsBuildParticipant() {
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		refresh_at_startup();
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		refresh_because_a_change();
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		refresh_because_a_change();
	}

	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		refresh_because_a_change();
	}

	private static void refresh_at_startup() {
		refresh(view -> {
			var elem = view.getPreviewElement();
			if (elem instanceof AnimationModel) {
				refreshAnimationPreview(view, (AnimationModel) elem);
			}
		});
	}

	private static void refresh_because_a_change() {
		refresh(view -> {
			var elem = view.getPreviewElement();
			if (elem instanceof IAssetKey) {
				view.preview(((IAssetKey) elem).getSharedVersion());
			} else if (elem instanceof IFile) {
				if (!((IFile) elem).exists()) {
					elem = null;
				}
				view.preview(elem);
			} else if (elem instanceof AnimationModel) {
				refreshAnimationPreview(view, (AnimationModel) elem);
			}
		});

	}

	private static void refresh(Consumer<PreviewView> consumer) {
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
						if (view == null) {
							continue;
						}

						if (view.getPreviewElement() != null) {
							consumer.accept(view);
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

				buildPreviewViews(project, delta, refs);

				PackDelta packDelta = AssetPackBuildParticipant.getData(env);

				buildAssetPackEditors(delta, packDelta, page);
			}

		});
	}

	@SuppressWarnings("unused")
	private static void buildAssetPackEditors(IResourceDelta delta, PackDelta packDelta, IWorkbenchPage page) {

		// all of this is shit, we should check only for resource delta and
		// renamed pack files.

//		IEditorReference[] refs = page.getEditorReferences();
//		for (IEditorReference ref : refs) {
//			if (ref.getId().equals(AssetPackEditor.ID)) {
//				AssetPackEditor editor = (AssetPackEditor) ref.getEditor(false);
//				if (editor != null) {
//					IFile curFile = editor.getEditorInput().getFile();
//					try {
//
//						// handle a rename or deletion
//
//						delta.accept(d -> {
//							int kind = d.getKind();
//							if (kind == IResourceDelta.REMOVED && d.getResource().equals(curFile)) {
//								IPath movedTo = d.getMovedToPath();
//								if (movedTo == null) {
//									page.closeEditor(editor, true);
//								} else {
//									IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(movedTo);
//									editor.handleFileRename(newFile);
//								}
//							}
//							return true;
//						});
//
//						// update content
//
//						boolean refresh = false;
//						AssetPackModel pack = editor.getModel();
//						for (AssetModel asset : packDelta.getAssets()) {
//							if (asset.getPack().getFile().equals(pack.getFile())) {
//								refresh = true;
//								break;
//							}
//						}
//
//						if (refresh) {
//							editor.refresh();
//						}
//
//					} catch (CoreException e) {
//						AssetPackUI.logError(e);
//					}
//				}
//			}
//		}
	}

	private static void buildPreviewViews(IProject project, IResourceDelta delta, IViewReference[] refs) {
		
		var finder = new AssetFinder(project);
		finder.build();
		
		for (IViewReference ref : refs) {
			if (ref.getId().equals(PreviewView.ID)) {
				PreviewView view = (PreviewView) ref.getView(false);

				if (view == null) {
					continue;
				}

				Object elem = view.getPreviewElement();

				if (elem != null) {
					if (elem instanceof IAssetKey) {
						updateAssetPreview(delta, view, (IAssetKey) elem);
						continue;
					} else if (elem instanceof AnimationModel) {
						updateAnimationPreview(finder, delta, view, (AnimationModel) elem);
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

	private static void updateAnimationPreview(AssetFinder finder, IResourceDelta delta, PreviewView view, AnimationModel oldAnim) {
		
		var files = oldAnim.computeUsedFiles(finder);

		boolean touched = ProjectCore.areFilesAffectedByDelta(delta, files);

		if (touched) {
			AnimationsModel oldAnims = oldAnim.getAnimations();
			IFile file = oldAnims.getFile();

			if (file.exists()) {
				try {
					var newAnims = new AnimationsModel_Persistable(file, oldAnims.getDataKey());
					var newAnim = newAnims.getAnimation(oldAnim.getKey());
					newAnim.build();
					view.preview(newAnim);
					return;
				} catch (Exception e) {
					// an error trying to recover the updated version
				}
			}

			view.preview(null);
		}
	}

	private static void updateAssetPreview(IResourceDelta delta, PreviewView view, IAssetKey key) {
		IAssetKey shared = key.getSharedVersion();

		if (shared != key) {
			view.preview(shared);
			return;
		}

		if (shared.getAsset().touched(delta)) {
			view.preview(key);
		}
	}

	static void refreshAnimationPreview(PreviewView view, AnimationModel anim) {
		anim.getAnimations().build();
		view.preview(anim);
	}

}
