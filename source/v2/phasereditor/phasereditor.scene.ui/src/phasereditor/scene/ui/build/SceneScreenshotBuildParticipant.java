// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.scene.ui.build;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.IResourceDeltaVisitor2;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.SceneCore;
import phasereditor.scene.ui.SceneUI;

/**
 * @author arian
 *
 */
public class SceneScreenshotBuildParticipant implements IProjectBuildParticipant {

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		var cache = SceneCore.getSceneFileDataCache();
		var data = cache.getProjectData(project);
		for (var sceneFile : data) {
			SceneUI.getSceneScreenshotFile(sceneFile.getFile(), true);
		}
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		var files = SceneCore.getSceneFileDataCache().getProjectData(project);
		for (var sceneFile : files) {
			SceneUI.clearSceneScreenshot(sceneFile.getFile());
		}
	}

	@Override
	public void projectDeleted(IProject project, Map<String, Object> env) {
		try {
			var webFolder = ProjectCore.getWebContentFolder(project);
			webFolder.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						if (SceneCore.isSceneFile(file)) {
							SceneUI.clearSceneScreenshot(file);
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			SceneUI.logError(e);
		}
	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		try {
			delta.accept(new IResourceDeltaVisitor2() {
				@Override
				public void fileAdded(IFile file) {
					if (SceneCore.isSceneFile(file)) {
						SceneUI.getSceneScreenshotFile(file, true);
					}
				}

				@Override
				public void fileRemoved(IFile file) {
					if (SceneCore.isSceneFile(file)) {
						SceneUI.clearSceneScreenshot(file);
					}
				}

				@Override
				public void fileMovedTo(IFile file, IPath movedFromPath, IPath movedToPath) {
					if (SceneCore.isSceneFile(file)) {
						SceneUI.clearSceneScreenshot(file);
						SceneUI.getSceneScreenshotFile(file, true);
					}
				}

				@Override
				public void fileChanged(IFile file) {
					if (SceneCore.isSceneFile(file)) {
						SceneUI.clearSceneScreenshot(file);
						SceneUI.getSceneScreenshotFile(file, true);
					}
				}
			});
		} catch (CoreException e) {
			SceneUI.logError(e);
		}
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {

		var files = SceneCore.getSceneFileDataCache().getProjectData(project);

		for (var sceneFile : files) {
			SceneUI.clearSceneScreenshot(sceneFile.getFile());
			SceneUI.getSceneScreenshotFile(sceneFile.getFile(), true);
		}
	}

}
