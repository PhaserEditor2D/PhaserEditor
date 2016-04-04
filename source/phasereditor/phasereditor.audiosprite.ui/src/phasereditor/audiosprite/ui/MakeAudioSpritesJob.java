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
package phasereditor.audiosprite.ui;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import phasereditor.audio.core.AudioCore;
import phasereditor.audiosprite.core.AudioSpriteCore;

public class MakeAudioSpritesJob extends Job {
	private List<IFile> _files;
	private String _audiospritesName;
	private IContainer _dstFolder;

	public MakeAudioSpritesJob(List<IFile> files, IContainer destFolder, String audiospritesName) {
		super("Create audio sprite");
		setUser(true);
		_files = files;
		_audiospritesName = audiospritesName;
		_dstFolder = destFolder;
	}

	public String getAudiospritesName() {
		return _audiospritesName;
	}

	public List<IFile> getFiles() {
		return _files;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			IFile audiospritesFile = AudioSpriteCore.makeAudioSprite(_files, _dstFolder, _audiospritesName,
					out::println, monitor);

			_dstFolder.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());

			// open the file in the editor
			swtRun(new Runnable() {

				@Override
				public void run() {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

					boolean confirm = getFiles().isEmpty() || MessageDialog.openConfirm(window.getShell(),
							"Audio Sprite",
							"The audio sprite '" + getAudiospritesName() + "' is done. Do you want to edit it?");
					if (confirm) {
						try {
							IWorkbenchPage page = window.getActivePage();
							IDE.openEditor(page, audiospritesFile);
						} catch (PartInitException e) {
							throw new RuntimeException(e);
						}
					}
				}
			});

			monitor.done();

		} catch (Exception e) {
			e.printStackTrace();
			return new Status(IStatus.ERROR, AudioCore.PLUGIN_ID, e.getMessage(), e);
		}

		return Status.OK_STATUS;
	}

}
