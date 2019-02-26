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
package phasereditor.scene.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import phasereditor.lic.LicCore;
import phasereditor.project.core.FileDataCache;

/**
 * @author arian
 *
 */
public class SceneCore {

	private static SceneFileDataCache _fileDataCache;

	public static IFile getSceneSourceCodeFile(IFile sceneFile) {
		var path = sceneFile.getProjectRelativePath();

		// for now it only compiles to JavaScript.

		return sceneFile.getProject().getFile(path.removeFileExtension().addFileExtension("js"));
	}

	public static void compileScene(SceneModel model, IFile sceneFile, IProgressMonitor monitor) throws Exception {

		{
			if (LicCore.isEvaluationProduct()) {
				var cause = SceneCore.isFreeVersionAllowed(sceneFile.getProject());
				if (cause != null) {
					LicCore.launchGoPremiumDialogs(cause);
					return;
				}
			}
		}

		var compiler = new SceneCompiler(sceneFile, model);

		compiler.compile(monitor);

	}

	public static String isFreeVersionAllowed(IProject project) {
		var projectData = getSceneFileDataCache().getProjectData(project);
		var count = projectData.size();

		if (count > LicCore.getFreeNumberSceneFiles()) {
			return count + " scene files";
		}

		return null;
	}

	public static FileDataCache<SceneFile> getSceneFileDataCache() {
		if (_fileDataCache == null) {
			_fileDataCache = new SceneFileDataCache();
		}

		return _fileDataCache;
	}

	public static boolean isSceneFile(IFile file) {

		try {

			if (!file.getFileExtension().equals("scene")) {
				return false;
			}

			var desc = file.getContentDescription();
			if (desc != null) {
				var contentType = desc.getContentType();
				if (contentType != null) {
					return contentType.getId().equals(SceneContentTypeDescriber.CONTENT_TYPE_ID);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}
