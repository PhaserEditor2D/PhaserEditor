package phasereditor.canvas.ui;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.IResourceDeltaVisitor2;
import phasereditor.project.core.ProjectCore;

public class CanvasScreenshotProjectBuildParticipant implements IProjectBuildParticipant {

	public CanvasScreenshotProjectBuildParticipant() {
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(project);
		for (CanvasFile cfile : cfiles) {
			CanvasUI.getCanvasScreenshotFile(cfile.getFile(), true);
		}
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(project);
		for (CanvasFile cfile : cfiles) {
			CanvasUI.clearCanvasScreenshot(cfile.getFile());
		}
	}
	
	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		try {
			IContainer webFolder = ProjectCore.getWebContentFolder(project);
			webFolder.accept(new IResourceVisitor() {
				
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						if (CanvasCore.isCanvasFile(file)) {
							CanvasUI.clearCanvasScreenshot(file);
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			CanvasUI.logError(e);
		}
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		try {
			delta.accept(new IResourceDeltaVisitor2() {
				@Override
				public void fileAdded(IFile file) {
					if (CanvasCore.isCanvasFile(file)) {
						CanvasUI.getCanvasScreenshotFile(file, true);
					}
				}

				@Override
				public void fileRemoved(IFile file) {
					if (CanvasCore.isCanvasFile(file)) {
						CanvasUI.clearCanvasScreenshot(file);
					}
				}

				@Override
				public void fileMovedTo(IFile file, IPath movedFromPath, IPath movedToPath) {
					if (CanvasCore.isCanvasFile(file)) {
						CanvasUI.clearCanvasScreenshot(file);
						CanvasUI.getCanvasScreenshotFile(file, true);
					}
				}

				@Override
				public void fileChanged(IFile file) {
					if (CanvasCore.isCanvasFile(file)) {
						CanvasUI.clearCanvasScreenshot(file);
						CanvasUI.getCanvasScreenshotFile(file, true);
					}
				}
			});
		} catch (CoreException e) {
			CanvasUI.logError(e);
		}
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(project);
		for (CanvasFile cfile : cfiles) {
			CanvasUI.clearCanvasScreenshot(cfile.getFile());
			CanvasUI.getCanvasScreenshotFile(cfile.getFile(), true);
		}
	}
}
