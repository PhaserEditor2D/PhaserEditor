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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

import phasereditor.project.core.codegen.SourceLang;

public class PhaserProjectNature implements IProjectNature {

	public static final String NATURE_IDS[] = { ProjectCore.PHASER_PROJECT_NATURE };

	public static void addPhaserNature(IProject project, SourceLang lang, IProgressMonitor monitor)
			throws CoreException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (!hasNature(project)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + NATURE_IDS.length];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			for (int i = 0; i < NATURE_IDS.length; i++) {
				newNatures[prevNatures.length + i] = NATURE_IDS[i];
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);

			resetProjectLibraries(project, lang, monitor);

		} else {
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	}

	public static void resetProjectLibraries(IProject project, SourceLang lang, IProgressMonitor monitor)
			throws JavaScriptModelException {

		if (lang != SourceLang.JAVA_SCRIPT) {
			return;
		}

		IJavaScriptProject jsProject = JavaScriptCore.create(project);

		// add libs

		List<IIncludePathEntry> list = new ArrayList<>();
		for (String id : new String[] { ProjectCore.ECMA5_SCOPE_INITIALIZER_ID,
				ProjectCore.BROWSER_SCOPE_INITIALIZER_ID, ProjectCore.GLOBAL_SCOPE_INITIALIZER_ID }) {
			list.add(JavaScriptCore.newContainerEntry(new Path(id)));
		}

		// add source

		IPath[] exclude = { new Path("**/lib"), new Path("**/phaser.js"), new Path("**/*.min.js"),
				new Path("**/node_modules") };
		IIncludePathEntry srcEntry = JavaScriptCore.newSourceEntry(project.getFullPath(), exclude);
		list.add(srcEntry);

		IIncludePathEntry[] newIncludeEntries = new IIncludePathEntry[list.size()];
		list.toArray(newIncludeEntries);

		out.println("Include entries: " + Arrays.toString(newIncludeEntries));

		jsProject.setRawIncludepath(newIncludeEntries, monitor);
	}

	public static void removePhaserNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (hasNature(project)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length - NATURE_IDS.length];
			int k = 0;
			head: for (int i = 0; i < prevNatures.length; i++) {
				for (int j = 0; j < NATURE_IDS.length; j++) {
					if (prevNatures[i].equals(NATURE_IDS[j])) {
						continue head;
					}
				}
				newNatures[k++] = prevNatures[i];
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		} else {
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	}

	public static boolean hasNature(IProject project) {
		try {
			for (int i = 0; i < NATURE_IDS.length; i++) {
				if (!project.hasNature(NATURE_IDS[i])) {
					return false;
				}
			}
		} catch (CoreException ex) {
			return false;
		}
		return true;
	}

	private IProgressMonitor _monitor;
	private IProject _project;

	public PhaserProjectNature() {
		_monitor = new NullProgressMonitor();
	}

	@Override
	public void configure() throws CoreException {
		// add the builder
		IProjectDescription desc = _project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		boolean found = false;

		String builderId = ProjectCore.PHASER_BUILDER_ID;

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(builderId)) {
				found = true;
				break;
			}
		}
		if (!found) {

			SourceLang lang = ProjectCore.getProjectLanguage(_project);

			// Phaser builder command
			ICommand phaserBuilderCommand = desc.newCommand();
			phaserBuilderCommand.setBuilderName(builderId);
			ICommand[] newCommands;

			if (lang == SourceLang.JAVA_SCRIPT) {
				newCommands = new ICommand[commands.length + 1];
				// Add it before other builders.
				System.arraycopy(commands, 0, newCommands, 1, commands.length);
				newCommands[0] = phaserBuilderCommand;

				// sort commands, move the javascript project to the end
				Arrays.sort(newCommands, new Comparator<ICommand>() {

					@Override
					public int compare(ICommand o1, ICommand o2) {
						String id1 = o1.getBuilderName();
						String id2 = o2.getBuilderName();

						int v1 = (id1 == JavaScriptCore.BUILDER_ID ? 1 : 0);
						int v2 = (id2 == JavaScriptCore.BUILDER_ID ? 1 : 0);

						return v1 - v2;
					}
				});
			} else {
				newCommands = new ICommand[commands.length + 2];
				// Add it before other builders.
				System.arraycopy(commands, 0, newCommands, 2, commands.length);

				ICommand tsBuilderCommand = desc.newCommand();
				tsBuilderCommand.setBuilderName("ts.eclipse.ide.core.typeScriptBuilder");

				newCommands[0] = phaserBuilderCommand;
				newCommands[1] = tsBuilderCommand;
			}

			desc.setBuildSpec(newCommands);
			_project.setDescription(desc, null);
		}

		// configure the JavaScript project, this
		// JavaProject javaProv = (JavaProject) JavaScriptCore.create(_project);
		// javaProv.configure();

		_project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	@Override
	public void deconfigure() throws CoreException {
		removePhaserNature(getProject(), _monitor);
	}

	@Override
	public IProject getProject() {
		return _project;
	}

	@Override
	public void setProject(IProject project) {
		_project = project;
	}

}
