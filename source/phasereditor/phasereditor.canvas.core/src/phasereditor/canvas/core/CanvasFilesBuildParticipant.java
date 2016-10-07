package phasereditor.canvas.core;

import static java.lang.System.out;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.PhaserProjectBuilder;
import phasereditor.project.core.ProjectCore;

public class CanvasFilesBuildParticipant implements IProjectBuildParticipant {

	public CanvasFilesBuildParticipant() {
	}

	@Override
	public void build(BuildArgs args) throws CoreException {
		if (args.getResourceDelta() == null) {
			return;
		}

		Set<IFile> validatedFiles = new HashSet<>();

		validateModifiedCanvasFiles(args.getResourceDelta(), validatedFiles);

		validateCanvasFilesUsingModifiedAssets(args.getAssetDelta(), validatedFiles);
	}

	private static void validateCanvasFilesUsingModifiedAssets(PackDelta delta, Set<IFile> validatedFiles) {
		try {
			Set<IFile> used = new HashSet<>();

			IProject project = null;
			for (AssetPackModel pack : delta.getPacks()) {
				project = pack.getFile().getProject();
				break;
			}

			if (project != null) {
				for (AssetModel asset : delta.getAssets()) {
					AssetPackModel pack = asset.getPack();
					project = pack.getFile().getProject();
					break;
				}
			}

			if (project != null) {
				IContainer webContent = ProjectCore.getWebContentFolder(project);

				webContent.accept(r -> {
					if (r instanceof IFile && !used.contains(r)) {
						IFile file = (IFile) r;
						if (CanvasCore.isCanvasFile(file)) {
							out.println("Building canvas " + file);
							validateCanvasFile(file);
							validatedFiles.add(file);
						}
					}
					return true;
				});

			}
		} catch (Exception e) {
			CanvasCore.logError(e);
		}
	}

	private static void validateModifiedCanvasFiles(IResourceDelta delta, Set<IFile> validatedFiles)
			throws CoreException {
		delta.accept(new IResourceDeltaVisitor() {

			@SuppressWarnings("synthetic-access")
			@Override
			public boolean visit(IResourceDelta delta2) throws CoreException {
				IResource resource = delta2.getResource();

				if (resource == null) {
					return true;
				}

				if (delta2.getKind() == IResourceDelta.REMOVED) {
					return true;
				}

				if (!(resource instanceof IFile)) {
					return true;
				}

				IFile file = (IFile) resource;
				if (ProjectCore.isWebContentFile(file) && CanvasCore.isCanvasFile(file)) {
					out.println("Building canvas " + file);
					validateCanvasFile(file);
					validatedFiles.add(file);
				}

				return true;
			}
		});

	}

	private static void validateCanvasFile(IFile file) {
		// try (InputStream contents = file.getContents();) {
		// JSONObject data = new JSONObject(new JSONTokener(contents));
		// CanvasEditorModel model = new CanvasEditorModel(file);
		// model.read(data, false);
		// // TODO: just make a validation
		// model.getWorld().build();
		// } catch (Exception e) {
		// CanvasCore.logError(e);
		// }

		try {
			CanvasFileValidation validation = new CanvasFileValidation(file);
			List<IStatus> problems = validation.validate();
			for (IStatus problem : problems) {
				PhaserProjectBuilder.createErrorMarker(problem, file);
			}
		} catch (Exception e) {
			CanvasCore.logError(e);
		}
	}

}
