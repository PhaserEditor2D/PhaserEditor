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
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import phasereditor.audio.core.AudioCore;

public class PhaserProjectBuilder extends IncrementalProjectBuilder {

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

		fullBuild(false);

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

		fullBuild(true);

		out.println("PhaserProjectBuilder.clean (done)");
	}

	private void fullBuild(boolean clean) {
		AudioCore.makeMediaSnapshots(getProject(), clean);
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		boolean fullBuild = kind == FULL_BUILD;
		if (fullBuild) {
			out.println("PhaserProjectBuilder.fullBuild (start)");
		} else {
			out.println("PhaserProjectBuilder.build (start)");
		}

		// detect a delete (rename or move should be covered by refactorings)
		IResourceDelta mainDelta = getDelta(getProject());

		// TODO: move this to a build participant
		if (mainDelta == null) {
			AudioCore.makeMediaSnapshots(getProject(), false);
		} else {
			AudioCore.makeSoundWavesAndMetadata(mainDelta);
			AudioCore.makeVideoSnapshot(mainDelta);
		}

		// call all build participant!!!

		Map<String, Object> env = new HashMap<>();
		List<IProjectBuildParticipant> list = ProjectCore.getBuildParticipants();

		monitor.beginTask("Building Phaser elements", list.size());

		for (IProjectBuildParticipant participant : list) {
			try {
				monitor.subTask("Building " + participant.getClass().getSimpleName());
				out.println("\t" + participant + " (building)");
				if (fullBuild) {
					participant.fullBuild(getProject(), env);
				} else {
					participant.build(getProject(), getDelta(getProject()), env);
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

		return null;
	}
}
