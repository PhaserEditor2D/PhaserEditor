// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.assetpack.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.project.core.ProjectCore;

/**
 * @author arian
 *
 */
public class AssetPackBuildParticipant implements IProjectBuildParticipant {

	private static final String DATA_KEY = "phasereditor.assetpack.core.buildData";

	@Override
	public void startupOnInitialize(IProject project, Map<String, Object> env) {
		AssetPackCore.discoverAssetPackModels();
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		ProjectCore.deleteResourceMarkers(project, AssetPackCore.ASSET_PACK_PROBLEM_ID);
		AssetPackCore.removeAssetPackModels(project);
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		ProjectCore.deleteResourceMarkers(project, AssetPackCore.ASSET_PACK_PROBLEM_ID);

		AssetPackCore.discoverAssetPackModels(project);

		List<AssetPackModel> list = AssetPackCore.getAssetPackModels(project);

		for (AssetPackModel pack : list) {
			List<IStatus> problems = pack.build();
			for (IStatus problem : problems) {
				createAssetPackMarker(pack.getFile(), problem);
			}
		}
	}

	@Override
	public void build(IProject project, IResourceDelta mainDelta, Map<String, Object> env) {
		try {
			PackDelta packDelta = new PackDelta();
			setData(env, packDelta);

			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

			// check for the packs to delete

			mainDelta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource deltaResource = delta.getResource();

					List<AssetPackModel> packs = AssetPackCore.getAssetPackModels(project);

					if (deltaResource instanceof IFile) {
						IFile deltaFile = (IFile) deltaResource;

						int kind = delta.getKind();

						switch (kind) {
						case IResourceDelta.REMOVED:
							for (AssetPackModel pack : packs) {
								if (deltaFile.equals(pack.getFile())) {
									IPath movedTo = delta.getMovedToPath();
									if (movedTo == null) {
										// removed: delete pack from map
										AssetPackCore.removeAssetPackModel(pack);
									} else {
										// moved: update the pack
										if (movedTo.getFileExtension().equals("json")) {
											AssetPackCore.moveAssetPackModel(root.getFile(movedTo), pack);
										} else {
											AssetPackCore.removeAssetPackModel(pack);
										}
									}

									// add the pack and all assets to the
									// delta
									packDelta.add(pack);
									packDelta.getAssets().addAll(pack.getAssets());
								}
							}
							break;
						case IResourceDelta.CHANGED:
							if (AssetPackCore.isAssetPackFile(deltaFile)) {
								try {
									AssetPackCore.resetAssetPackModel(deltaFile);
								} catch (Exception e) {
									AssetPackCore.logError(e);
								}
							}
							break;
						case IResourceDelta.ADDED:
							// just added:
							IPath movedFrom = delta.getMovedFromPath();
							if (movedFrom == null && AssetPackCore.isAssetPackFile(deltaFile)) {
								AssetPackCore.getAssetPackModel(deltaFile, true);
							}
							break;
						default:
							break;
						}
					}
					return true;
				}
			});

			// compute delta packs affected by the change

			List<AssetPackModel> allPacks = AssetPackCore.getAssetPackModels(project);

			// TODO: probably this is not going to work.
			// delta packs can be computed by comparing the old model with the
			// new model.

			mainDelta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					if (resource instanceof IFile) {

						// TODO: let's compute delta pack in a better way?

						IPath movedPath = delta.getMovedToPath();
						IPath deltaPath = resource.getFullPath();

						for (AssetPackModel pack : allPacks) {
							PackDelta delta1 = pack.computeDelta(movedPath);
							PackDelta delta2 = pack.computeDelta(deltaPath);
							packDelta.add(delta1);
							packDelta.add(delta2);
						}
					}
					return true;
				}
			});

			// build and validate all the affected packs
			{

				// delete all affected files markers

				{
					Set<IFile> toCleanMarks = new HashSet<>();

					for (AssetPackModel pack : packDelta.getPacks()) {
						toCleanMarks.add(pack.getFile());
					}

					for (AssetModel asset : packDelta.getAssets()) {
						toCleanMarks.add(asset.getPack().getFile());
					}

					for (IFile file : toCleanMarks) {
						if (file.exists()) {
							ProjectCore.deleteResourceMarkers(file, AssetPackCore.ASSET_PACK_PROBLEM_ID);
						}
					}
				}

				// build all affected assets

				Set<AssetModel> toBuild = new LinkedHashSet<>();

				for (AssetModel asset : packDelta.getAssets()) {
					toBuild.add(asset);
				}

				for (AssetPackModel pack : packDelta.getPacks()) {
					toBuild.addAll(pack.getAssets());
				}

				for (AssetModel asset : toBuild) {
					IFile file = asset.getPack().getFile();
					if (!file.exists()) {
						continue;
					}
					List<IStatus> problems = new ArrayList<>();
					asset.build(problems);
					for (IStatus problem : problems) {
						createAssetPackMarker(file, problem);
					}
				}
			}

		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private static void createAssetPackMarker(IFile file, IStatus problem) {
		ProjectCore.createErrorMarker(AssetPackCore.ASSET_PACK_PROBLEM_ID, problem, file);
	}

	public static PackDelta getData(Map<String, Object> env) {
		return (PackDelta) env.get(DATA_KEY);
	}

	private static void setData(Map<String, Object> env, PackDelta data) {
		env.put(DATA_KEY, data);
	}
}
