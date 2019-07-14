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

import static java.lang.System.out;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import phasereditor.assetpack.core.SceneFileAssetModel;
import phasereditor.lic.LicCore;
import phasereditor.project.core.FileDataCache;
import phasereditor.project.core.ProjectCore;

/**
 * @author arian
 *
 */
public class SceneCore {

	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	public static final String EDITOR_SCENE_FILE_CONTENT_TYPE = SceneContentTypeDescriber.CONTENT_TYPE_ID;

	private static SceneFileDataCache _fileDataCache;

	public static IFile getSceneSourceCodeFile(SceneModel sceneModel, IFile sceneFile) {
		var path = sceneFile.getProjectRelativePath()

				.removeFileExtension()

				.addFileExtension(sceneModel.getCompilerLang().getExtension());

		return sceneFile.getProject().getFile(path);
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

		compiler.compileToFile(monitor);

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

			if (!"scene".equals(file.getFileExtension())) {
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

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static void logError(String msg) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, msg, null));
	}

	private static final QualifiedName SNAPSHOT_FILENAME_KEY = new QualifiedName("phasereditor.scene.core",
			"snapshot-file");

	public static Path getSceneScreenshotFile(IFile file) {
		if (file == null) {
			return null;
		}

		try {
			String filename = file.getPersistentProperty(SNAPSHOT_FILENAME_KEY);

			if (filename == null) {
				filename = file.getName() + "_" + UUID.randomUUID().toString() + ".png";
			}

			Path dir = ProjectCore.getUserCacheFolder().resolve("snapshots");

			Path writeTo;

			writeTo = dir.resolve(filename);

			return writeTo;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void saveScreenshotPath(IFile file, Path writeTo) {
		var filename = writeTo.getFileName().toString();

		try {
			file.setPersistentProperty(SceneCore.SNAPSHOT_FILENAME_KEY, filename);
		} catch (CoreException e) {
			logError(e);
		}
	}

	public static void clearSceneScreenshot(IFile file) {
		try {
			if (!file.exists()) {
				return;
			}

			String fname = file.getPersistentProperty(SceneCore.SNAPSHOT_FILENAME_KEY);
			if (fname == null) {
				return;
			}

			Path dir = ProjectCore.getUserCacheFolder().resolve("snapshots");
			Path snapshot = dir.resolve(fname);
			if (Files.exists(snapshot)) {
				out.println("Removing snapshot from " + file);
				Files.delete(snapshot);
			}
			file.setPersistentProperty(SceneCore.SNAPSHOT_FILENAME_KEY, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static File getSceneScreenshotFile(SceneFileAssetModel asset) {
		var file = asset.getUrlFile();
		if (file != null) {
			var file2 = file.getProject()
					.getFile(file.getProjectRelativePath().removeFileExtension().addFileExtension("scene"));
			if (file2.exists()) {
				var screenPath = getSceneScreenshotFile(file2);
				if (screenPath != null) {
					var screenFile = screenPath.toFile();
					if (screenFile.exists()) {
						return screenFile;
					}
				}
			}
		}
		return null;
	}

}
