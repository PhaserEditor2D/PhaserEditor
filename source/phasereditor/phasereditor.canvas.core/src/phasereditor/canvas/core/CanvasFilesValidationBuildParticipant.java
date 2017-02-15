package phasereditor.canvas.core;

import static java.lang.System.out;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.IResourceDeltaVisitor2;
import phasereditor.project.core.ProjectCore;

public class CanvasFilesValidationBuildParticipant implements IProjectBuildParticipant {

	public CanvasFilesValidationBuildParticipant() {
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		// nothing, the markers are persisted across sessions
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		ProjectCore.deleteResourceMarkers(project, CanvasCore.CANVAS_PROBLEM_MARKER_ID);
	}

	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		// nothing
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		ProjectCore.deleteResourceMarkers(project, CanvasCore.CANVAS_PROBLEM_MARKER_ID);

		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(project);
		cfiles.forEach(cfile -> {
			validateCanvasFile(cfile.getFile());
		});
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		if (isModifiedAPrefab(delta)) {
			fullBuild(project, env);
			return;
		}

		PackDelta packDelta = AssetPackBuildParticipant.getData(env);
		if (packDelta.isEmpty()) {
			// if no assets was modified, then validate only the modified canvas
			// files.
			try {
				validateModifiedCanvasFiles(delta);
			} catch (CoreException e) {
				CanvasCore.logError(e);
			}
		} else {
			// if any asset was modified then we prefer to do a full validation
			fullBuild(project, env);
		}

	}

	private static boolean isModifiedAPrefab(IResourceDelta delta) {
		try {
			boolean[] found = { false };
			delta.accept(new IResourceDeltaVisitor2() {
				@Override
				public boolean fileVisited(IFile file) {
					CanvasFile data = CanvasCore.getCanvasFileCache().getFileData(file);

					if (data != null && data.getType().isPrefab()) {
						found[0] = true;
					}

					return !found[0];
				}
			});
			return found[0];
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static void validateModifiedCanvasFiles(IResourceDelta delta) throws CoreException {

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
					validateCanvasFile(file);
				}

				return true;
			}
		});

	}

	private static void validateCanvasFile(IFile file) {
		out.println("Validate canvas file " + file);

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
