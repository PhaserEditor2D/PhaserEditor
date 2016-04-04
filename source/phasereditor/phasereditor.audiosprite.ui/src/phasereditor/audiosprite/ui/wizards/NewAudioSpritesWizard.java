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
package phasereditor.audiosprite.ui.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;

import phasereditor.audio.core.AudioCore;
import phasereditor.audiosprite.ui.MakeAudioSpritesJob;

public class NewAudioSpritesWizard extends Wizard implements INewWizard {

	private AudioSpritesWizardPage _filenamePage;
	private IStructuredSelection _selection;
	protected IWorkbenchPage _windowPage;

	public NewAudioSpritesWizard() {
		setWindowTitle("New Audio Sprites File");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		_filenamePage = new AudioSpritesWizardPage(_selection);
		addPage(_filenamePage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		_selection = selection;
		_windowPage = workbench.getActiveWorkbenchWindow().getActivePage();
	}

	public IStructuredSelection getSelection() {
		return _selection;
	}

	public List<IFile> getInitialAudioFiles() {
		Object[] elems = _selection.toArray();
		List<IFile> files = new ArrayList<>();
		for (Object elem : elems) {
			if (elem instanceof IFile) {
				IFile file = (IFile) elem;
				if (AudioCore.isSupportedAudio(file)) {
					files.add(file);
				}
			}
		}
		return files;
	}

	@Override
	public boolean performFinish() {
		// no file extension specified so add default extension
		String fileName = _filenamePage.getFileName();
		String spritesName;
		if (fileName.lastIndexOf('.') == -1) {
			spritesName = fileName;
			String newFileName = fileName + ".json";
			_filenamePage.setFileName(newFileName);
		} else {
			spritesName = fileName.substring(0, fileName.length() - 5);
		}

		IContainer dstDir = (IContainer) ResourcesPlugin.getWorkspace().getRoot()
				.findMember(_filenamePage.getContainerFullPath());

		List<IFile> audioFiles = Collections.emptyList();
		MakeAudioSpritesJob job = new MakeAudioSpritesJob(audioFiles, dstDir, spritesName);
		job.schedule();

		return true;
	}

}
