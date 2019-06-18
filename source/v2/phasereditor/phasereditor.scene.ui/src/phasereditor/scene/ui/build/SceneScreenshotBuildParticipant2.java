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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.IResourceDeltaVisitor2;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.PackReferencesCollector;
import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.SceneUI;
import phasereditor.webrun.core.ApiHub;
import phasereditor.webrun.core.ApiMessage;
import phasereditor.webrun.core.WebRunCore;

/**
 * @author arian
 *
 */
public class SceneScreenshotBuildParticipant2 implements IProjectBuildParticipant {

	static final String SOCKET_CHANNEL = "sceneScreenshot";

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		var cache = SceneCore.getSceneFileDataCache();
		var sfiles = cache.getProjectData(project);

		buildAndSendMessage(project, sfiles.stream().map(sfile -> sfile.getFile()).collect(toList()));
	}

	private static void buildAndSendMessage(IProject project, Collection<IFile> files) {
		var msg = new CreateSceneScreenshotMessage(files, project);
		ApiHub.sendMessageAllClients(SOCKET_CHANNEL, msg);
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		var files = SceneCore.getSceneFileDataCache().getProjectData(project);
		for (var sceneFile : files) {
			SceneCore.clearSceneScreenshot(sceneFile.getFile());
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
							SceneCore.clearSceneScreenshot(file);
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

			var files = new HashSet<IFile>();

			delta.accept(new IResourceDeltaVisitor2() {
				@Override
				public void fileAdded(IFile file) {
					if (SceneCore.isSceneFile(file)) {
						// SceneCore.getSceneScreenshotFile(file, true);
						files.add(file);
					}
				}

				@Override
				public void fileRemoved(IFile file) {
					if (SceneCore.isSceneFile(file)) {
						SceneCore.clearSceneScreenshot(file);
					}
				}

				@Override
				public void fileMovedTo(IFile file, IPath movedFromPath, IPath movedToPath) {
					if (SceneCore.isSceneFile(file)) {
						SceneCore.clearSceneScreenshot(file);
						// SceneCore.getSceneScreenshotFile(file, true);
						files.add(file);
					}
				}

				@Override
				public void fileChanged(IFile file) {
					if (SceneCore.isSceneFile(file)) {
						SceneCore.clearSceneScreenshot(file);
						// SceneCore.getSceneScreenshotFile(file, true);
						files.add(file);
					}
				}
			});

			buildAndSendMessage(project, files);

		} catch (CoreException e) {
			SceneUI.logError(e);
		}
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {

		var sfiles = SceneCore.getSceneFileDataCache().getProjectData(project);

		for (var sceneFile : sfiles) {
			SceneCore.clearSceneScreenshot(sceneFile.getFile());
			// SceneCore.getSceneScreenshotFile(sceneFile.getFile(), true);
		}

		var files = sfiles.stream().map(file -> file.getFile()).collect(toList());

		buildAndSendMessage(project, files);
	}

}

class CreateSceneScreenshotMessage extends ApiMessage {

	public CreateSceneScreenshotMessage(Collection<IFile> sceneFiles, IProject project) {
		super();

		_data.put("method", "CreateScreenshot");

		_data.put("projectUrl", WebRunCore.getProjectBrowserURL(project, false));

		var map = new HashMap<IFile, SceneModel>();

		{
			for (var file : sceneFiles) {
				var model = new SceneModel();
				try {
					model.read(file);
					map.put(file, model);
				} catch (Exception e) {
					SceneUI.logError(e);
				}
			}
		}

		{
			// pack data
			var assets = new HashSet<AssetModel>();

			var finder = AssetPackCore.getAssetFinder(project);

			for (var model : map.values()) {
				var collector = new PackReferencesCollector(model, finder);
				var collect = collector.collectAssetKeys();
				assets.addAll(collect.stream().map(k -> k.getAsset()).collect(toList()));
			}

			var packData = PackReferencesCollector.collectNewPack(assets);
			_data.put("pack", packData);
		}

		{
			// scene data
			var sceneArrayData = new JSONArray();
			_data.put("scenes", sceneArrayData);

			for (var entry : map.entrySet()) {
				var file = entry.getKey();
				var model = entry.getValue();

				var sceneData = new JSONObject();

				var filename = file.getFullPath().toPortableString();
				sceneData.put("file", filename);
				sceneData.put("model", model.toJSON());

				sceneArrayData.put(sceneData);
			}
		}
	}

}