package phasereditor.canvas.ui;

import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.ProjectCore;

public class CanvasScreenshotProjectBuildParticipant implements IProjectBuildParticipant {

	public CanvasScreenshotProjectBuildParticipant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		// nothing
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {

		IContainer webFolder = ProjectCore.getWebContentFolder(project);
		try {
			webFolder.accept(r -> {
				if (r instanceof IFile) {
					IFile file = (IFile) r;
					CanvasUI.clearCanvasScreenshot(file);
				}
				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		try {
			delta.accept(d -> {
				boolean changed = d.getKind() == IResourceDelta.ADDED || d.getKind() == IResourceDelta.CHANGED
						|| d.getKind() == IResourceDelta.REPLACED;

				if (d.getResource() instanceof IFile) {
					IFile f = (IFile) d.getResource();
					CanvasUI.clearCanvasScreenshot(f);
					if (changed && CanvasCore.isCanvasFile(f)) {
						CanvasUI.getCanvasScreenshotFile(f, true);
					}
				}

				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		try {
			IContainer webFolder = ProjectCore.getWebContentFolder(project);
			webFolder.accept(r -> {
				if (r instanceof IFile) {
					IFile f = (IFile) r;
					if (CanvasCore.isCanvasFile(f)) {
						CanvasUI.clearCanvasScreenshot(f);
						CanvasUI.getCanvasScreenshotFile(f, true);
					}
				}
				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
