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

import static java.lang.System.out;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class PhaserProjectBuilder extends IncrementalProjectBuilder {

	private static HashMap<IProject, Runnable> _actions = new HashMap<>();
	private static boolean _registeredProjectDeleteListener = false;

	public PhaserProjectBuilder() {
		if (!_registeredProjectDeleteListener) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {

				@Override
				public void resourceChanged(IResourceChangeEvent event) {
					IProject project = (IProject) event.getResource();
					projectDeleted(project);
				}
			}, IResourceChangeEvent.PRE_DELETE);

			ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {

				@Override
				public void resourceChanged(IResourceChangeEvent event) {
					try {
						event.getDelta().accept(new IResourceDeltaVisitor() {

							@Override
							public boolean visit(IResourceDelta delta) throws CoreException {

								IResource resource = delta.getResource();
								if (resource instanceof IProject) {
									if ((delta.getFlags() & IResourceDelta.OPEN) == IResourceDelta.OPEN) {
										out.println("Oepened project " + resource.getName());
									}
								}
								return true;
							}
						});
					} catch (CoreException e) {
						ProjectCore.logError(e);
					}
				}
			}, IResourceChangeEvent.POST_CHANGE);

			_registeredProjectDeleteListener = true;
		}
	}

	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();		

		IProject project = getProject();

		Map<String, Object> env = new HashMap<>();

		out.println("PhaserProjectBuilder.startupOnInitialize (start)");

		List<IProjectBuildParticipant> list = ProjectCore.getBuildParticipants();

		for (IProjectBuildParticipant participant : list) {
			try {
				out.println("\t" + participant + " (building)");
				participant.startupOnInitialize(project, env);
			} catch (Exception e) {
				ProjectCore.logError(e);
			}
		}

		out.println("PhaserProjectBuilder.startupOnInitialize (done)");

	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		out.println("PhaserProjectBuilder.clean (start)");

		IProject project = getProject();
		Map<String, Object> env = new HashMap<>();
		List<IProjectBuildParticipant> list = ProjectCore.getBuildParticipants();
		for (IProjectBuildParticipant participant : list) {
			try {
				out.println("\t" + participant + " (building)");
				participant.clean(project, env);
			} catch (Exception e) {
				ProjectCore.logError(e);
			}
		}

		out.println("PhaserProjectBuilder.clean (done)");
	}

	protected static void projectDeleted(IProject project) {
		Map<String, Object> env = new HashMap<>();

		out.println("PhaserProjectBuilder.projectDeleted (start)");

		List<IProjectBuildParticipant> list = ProjectCore.getBuildParticipants();

		for (IProjectBuildParticipant participant : list) {
			try {
				out.println("\t" + participant + " (building)");
				participant.projectDeleted(project, env);
			} catch (Exception e) {
				ProjectCore.logError(e);
			}
		}

		out.println("PhaserProjectBuilder.projectDeleted (done)");
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		boolean fullBuild = kind == FULL_BUILD;
		if (fullBuild) {
			out.println("PhaserProjectBuilder.fullBuild (start)");
		} else {
			out.println("PhaserProjectBuilder.build (start)");
		}

		// call all build participant!!!

		Map<String, Object> env = new HashMap<>();
		List<IProjectBuildParticipant> list = ProjectCore.getBuildParticipants();

		monitor.beginTask("Building Phaser elements", list.size());

		IProject project = getProject();

		for (IProjectBuildParticipant participant : list) {
			try {
				monitor.subTask("Building " + participant.getClass().getSimpleName());
				out.println("\t" + participant + " (building)");

				if (fullBuild) {
					participant.fullBuild(project, env);
				} else {
					participant.build(project, getDelta(project), env);
				}
				monitor.worked(1);
			} catch (Exception e) {
				ProjectCore.logError(e);
			}
		}

		monitor.done();

		if (fullBuild) {
			out.println("PhaserProjectBuilder.fullBuild (done)");
		} else {
			out.println("PhaserProjectBuilder.build (done)");
		}

		runAfterBuildActions(project);

		return null;
	}

	public static void setActionAfterFirstBuild(IProject project, Runnable runnable) {
		_actions.put(project, runnable);
	}

	private static void runAfterBuildActions(IProject project) {
		Runnable action = _actions.get(project);
		if (action != null) {
			_actions.remove(project);
			action.run();
		}
	}
}
