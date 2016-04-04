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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import phasereditor.audiosprite.core.AudioSpriteCore;
import phasereditor.audiosprite.core.AudioSpritesModel;

public class UpdateAudioSpritesJob extends Job {

	private AudioSpritesModel _model;
	private List<IFile> _files;

	public UpdateAudioSpritesJob(AudioSpritesModel model, List<IFile> files) {
		super("Update audio sprites");
		_model = model;
		_files = files;
		setUser(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {

			IFile file = AudioSpriteCore.updateAudioSprite(_model, _files, out::println, monitor);

			if (file != null) {
				file.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return Status.OK_STATUS;
	}

}
