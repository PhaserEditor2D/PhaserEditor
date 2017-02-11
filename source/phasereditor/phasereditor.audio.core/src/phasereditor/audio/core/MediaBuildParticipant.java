package phasereditor.audio.core;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

import phasereditor.project.core.IProjectBuildParticipant;

public class MediaBuildParticipant implements IProjectBuildParticipant {

	public MediaBuildParticipant() {
	}

	@Override
	public void build(IProject project, IResourceDelta resDelta, Map<String, Object> env) {

		try {
			resDelta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;

						if (resource.exists()) {

							boolean changed = delta.getKind() == IResourceDelta.CHANGED;

							if (AudioCore.isSupportedAudio(file)) {

								if (changed) {
									AudioCore.removeSoundProperties(file);
								}

								AudioCore.getSoundWavesFile(file);
								AudioCore.getSoundDuration(file);

							} else if (AudioCore.isSupportedVideo(file)) {

								if (changed) {
									AudioCore.removeVideoProperties(file);
								}

								AudioCore.getVideoSnapshotFile(file);
							}
						}

					}
					return true;
				}
			});

		} catch (CoreException e) {
			AudioCore.logError(e);
		}

	}

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		// nothing
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		try {
			project.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						if (resource.exists()) {
							if (AudioCore.isSupportedVideo(file)) {
								AudioCore.removeVideoProperties(file);
							} else if (AudioCore.isSupportedAudio(file)) {
								AudioCore.removeSoundProperties(file);
							}
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			AudioCore.logError(e);
		}
	}
	
	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		clean(project, env);
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		try {
			project.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						if (resource.exists()) {
							if (AudioCore.isSupportedVideo(file)) {
								AudioCore.getVideoSnapshotFile(file);
							} else if (AudioCore.isSupportedAudio(file)) {
								AudioCore.getSoundWavesFile(file);
								AudioCore.getSoundDuration(file);
							}
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			AudioCore.logError(e);
		}
	}

}
