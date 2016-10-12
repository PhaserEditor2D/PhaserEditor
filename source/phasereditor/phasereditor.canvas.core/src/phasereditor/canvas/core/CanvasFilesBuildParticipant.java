package phasereditor.canvas.core;

import static java.lang.System.out;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.ProjectCore;

public class CanvasFilesBuildParticipant implements IProjectBuildParticipant {

	public CanvasFilesBuildParticipant() {
	}
	
	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		// nothing
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		ProjectCore.deleteResourceMarkers(project, CanvasCore.CANVAS_PROBLEM_MARKER_ID);
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		ProjectCore.deleteResourceMarkers(project, CanvasCore.CANVAS_PROBLEM_MARKER_ID);
		IContainer webFolder = ProjectCore.getWebContentFolder(project);
		try {
			webFolder.accept(r -> {
				if (r instanceof IFile && CanvasCore.isCanvasFile((IFile) r)) {
					validateCanvasFile((IFile) r);
				}
				return true;
			});
		} catch (CoreException e) {
			CanvasCore.logError(e);
		}
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		PackDelta packDelta = AssetPackBuildParticipant.getData(env);

		Set<IFile> validatedFiles = new HashSet<>();

		try {
			validateModifiedCanvasFiles(delta, validatedFiles);
		} catch (CoreException e) {
			CanvasCore.logError(e);
		}

		validateCanvasFilesUsingModifiedAssets(packDelta, validatedFiles);
	}

	private static void validateCanvasFilesUsingModifiedAssets(PackDelta delta, Set<IFile> validatedFiles) {

		// This method validate all the canvas files of the project containing
		// the asset changes.
		//
		// The question is that to know if a canvas file is affected, it should
		// be inspected, and that inspection is as expensive as validate it, so
		// at the end we just validate everybody.

		try {
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
					if (r instanceof IFile) {
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
		try {
			ProjectCore.deleteResourceMarkers(file, CanvasCore.CANVAS_PROBLEM_MARKER_ID);
			CanvasFileValidation validation = new CanvasFileValidation(file);
			List<IStatus> problems = validation.validate();
			for (IStatus problem : problems) {
				ProjectCore.createErrorMarker(CanvasCore.CANVAS_PROBLEM_MARKER_ID, problem, file);
			}
		} catch (Exception e) {
			CanvasCore.logError(e);
		}
	}

}
