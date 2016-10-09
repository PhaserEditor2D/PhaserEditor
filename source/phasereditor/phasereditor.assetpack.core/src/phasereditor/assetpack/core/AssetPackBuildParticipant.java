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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
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
		fullBuild(false, project);
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		fullBuild(true, project);
	}

	private void fullBuild(boolean clean, IProject project) {
		buildPacks(Optional.empty(), new PackDelta(), project);
	}

	@Override
	public void build(IProject project, IResourceDelta mainDelta, Map<String, Object> env) {
		PackDelta packDelta = new PackDelta();
		setData(env, packDelta);

		if (mainDelta != null) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			try {
				mainDelta.accept(new IResourceDeltaVisitor() {

					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						// this only check for the packs to delete

						IResource deltaResource = delta.getResource();

						List<AssetPackModel> packs = AssetPackCore.getAssetPackModels(project);

						if (deltaResource instanceof IFile) {
							IFile deltaFile = (IFile) deltaResource;
							int deltakind = delta.getKind();
							if (deltakind == IResourceDelta.REMOVED) {
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
							}
						}
						return true;
					}
				});
			} catch (CoreException e) {
				AssetPackCore.logError(e);
			}
		}

		// Any change on the project implies to rebuild all the packs of that
		// project.
		buildPacks(Optional.ofNullable(mainDelta), packDelta, project);
	}

	private static void buildPacks(Optional<IResourceDelta> resourceDelta, PackDelta packDelta, IProject project) {
		try {

			if (resourceDelta.isPresent()) {
				// reset modified models

				resourceDelta.get().accept(new IResourceDeltaVisitor() {

					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						try {
							IResource resource = delta.getResource();

							if (!(resource instanceof IFile)) {
								return true;
							}

							IFile file = (IFile) resource;

							if (!AssetPackCore.isAssetPackFile(file)) {
								return true;
							}

							if (delta.getKind() == IResourceDelta.CHANGED
									|| delta.getKind() == IResourceDelta.REMOVED) {
								AssetPackCore.resetAssetPackModel(file);
							}

						} catch (Exception e) {
							AssetPackCore.logError(e);
						}
						return true;
					}
				});
			}

			// ensure all models was created

			IContainer webFolder = ProjectCore.getWebContentFolder(project);
			webFolder.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						try {
							if (AssetPackCore.isAssetPackFile(file)) {
								AssetPackCore.getAssetPackModel(file);
							}
						} catch (Exception e) {
							AssetPackCore.logError(e);
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}

		// compute delta packs affected by the change

		List<AssetPackModel> allPacks = AssetPackCore.getAssetPackModels(project);

		if (resourceDelta.isPresent()) {

			// TODO: probably this is not going to work.
			// delta packs can be computed by comparing the old model with the
			// new model.

			try {
				resourceDelta.get().accept(new IResourceDeltaVisitor() {

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
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		} else {
			// if there is not delta, then let's say all the project's packs are
			// affected.
			packDelta.getPacks().addAll(allPacks);
		}

		// build and validate all the affected packs
		{
			for (AssetModel asset : packDelta.getAssets()) {
				List<IStatus> problems = new ArrayList<>();
				asset.build(problems);
				for (IStatus problem : problems) {
					createAssetPackMarker(asset.getPack().getFile(), problem);
				}
			}

			for (AssetPackModel pack : packDelta.getPacks()) {
				List<IStatus> problems = pack.build();
				for (IStatus problem : problems) {
					createAssetPackMarker(pack.getFile(), problem);
				}
			}
		}

		// always call this to refresh asset viewers.

		AssetPackCore.firePacksChanged(packDelta);
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
