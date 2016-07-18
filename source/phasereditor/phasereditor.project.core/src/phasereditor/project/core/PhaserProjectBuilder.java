// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.project.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.ide.IDE;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetModel.AssetStatus;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.audio.core.AudioCore;

public class PhaserProjectBuilder extends IncrementalProjectBuilder {

	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();

		fullBuild();
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		fullBuild();
	}

	private void fullBuild() {
		buildPacks(null, new PackDelta());
		AudioCore.makeMediaSnapshots(getProject());
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {

		// detect a delete (rename or move should be covered by refactorings)
		IResourceDelta mainDelta = getDelta(getProject());

		PackDelta packDelta = new PackDelta();

		if (mainDelta != null) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			mainDelta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					// this only check for the packs to delete

					IResource deltaResource = delta.getResource();

					IProject project = getProject();

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

									// add the pack and all assets to the delta
									packDelta.add(pack);
									packDelta.getAssets().addAll(pack.getAssets());
								}
							}
						}
					}
					return true;
				}
			});
		}

		if (mainDelta == null) {
			AudioCore.makeMediaSnapshots(getProject());
		} else {
			AudioCore.makeSoundWavesAndMetadata(mainDelta);
			AudioCore.makeVideoSnapshot(mainDelta);
		}

		// Any change on the project implies to rebuild all the packs of that
		// project.
		buildPacks(mainDelta, packDelta);

		return null;
	}

	private void cleanProblemMarkers() {
		IProject project = getProject();
		try {
			project.deleteMarkers(ProjectCore.PHASER_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private void buildPacks(IResourceDelta buildDelta, PackDelta packDelta) {
		IProject project = getProject();

		cleanProblemMarkers();

		// ensure all models was created

		try {
			project.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						try {
							if (AssetPackCore.isAssetPackFile(file)) {
								AssetPackCore.getAssetPackModel(file);
							}
						} catch (Exception e) {
							e.printStackTrace();
							createAssetPackMarker(file,
									new Status(IStatus.WARNING, ProjectCore.PLUGIN_ID, e.getMessage()));
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

		if (buildDelta == null) {
			// if there is not delta, then let's say all the project's packs are
			// affected.
			packDelta.getPacks().addAll(allPacks);
		} else {
			try {
				buildDelta.accept(new IResourceDeltaVisitor() {

					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						IResource resource = delta.getResource();
						if (resource instanceof IFile) {

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
		}

		// build and validate all the affected packs
		{
//			Collection<AssetPackModel> buildPacks;
//			if (buildDelta == null) {
//				buildPacks = packDelta.getPacks();
//			} else {
//				buildPacks = allPacks;
//			}

			for(AssetModel asset : packDelta.getAssets()) {
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

	protected static void createAssetPackMarker(IResource resource, IStatus problem) {
		if (!resource.exists()) {
			return;
		}

		try {
			int severity;
			switch (problem.getSeverity()) {
			case IStatus.ERROR:
				severity = IMarker.SEVERITY_ERROR;
				break;
			default:
				severity = IMarker.SEVERITY_WARNING;
				break;
			}

			IMarker marker = resource.createMarker(ProjectCore.PHASER_PROBLEM_MARKER_ID);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.MESSAGE, problem.getMessage());
			marker.setAttribute(IMarker.LOCATION, resource.getProject().getName());
			marker.setAttribute(IMarker.TRANSIENT, true);
			marker.setAttribute(IDE.EDITOR_ID_ATTR, AssetPackCore.ASSET_EDITOR_ID);

			if (problem instanceof AssetStatus) {
				AssetModel asset = ((AssetStatus) problem).getAsset();
				String ref = asset.getPack().getStringReference(asset);
				marker.setAttribute(AssetPackCore.ASSET_EDITOR_GOTO_MARKER_ATTR, ref);
			}

		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

}
