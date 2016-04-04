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
package phasereditor.text.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Image;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.preview.AudioFileInformationControl;
import phasereditor.assetpack.ui.preview.ImageFileInformationControl;
import phasereditor.audio.core.AudioCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.info.GenericInformationControlCreator;
import phasereditor.ui.info.TextInformationControlCreator;

public class FilesProposalComputer extends BaseProposalComputer {
	protected static final int RELEVANCE = 0;
	protected static Image _filePropImage;

	static {
		_filePropImage = AssetLabelProvider.getFileImage();
	}

	public FilesProposalComputer() {
	}

	@Override
	protected List<ProposalData> computeProjectProposals(IProject project) {
		List<ProposalData> list = new ArrayList<>();
		try {
			IContainer webContentFolder = ProjectCore.getWebContentFolder(project);
			IResource rootFolder = webContentFolder == null || !webContentFolder.exists() ? project : webContentFolder;

			IPath rootPath = rootFolder.getFullPath();
			TextInformationControlCreator controlCreator = new TextInformationControlCreator(
					"Relative path to a web file.");
			rootFolder.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFolder && resource.getName().startsWith(".")) {
						return false;
					}

					if (resource instanceof IFile) {
						IPath filePath = resource.getFullPath().makeRelativeTo(rootPath);
						String filename = filePath.toPortableString();
						String display = "\"" + filename + "\"";

						Object obj = null;
						if (AssetPackCore.isAudio(resource) || AssetPackCore.isImage(resource)) {
							obj = resource;
						}

						ProposalData proposal = new ProposalData(obj, filename, display, RELEVANCE);
						proposal.setImage(_filePropImage);

						if (AssetPackCore.isImage(resource)) {
							proposal.setControlCreator(new GenericInformationControlCreator(
									ImageFileInformationControl.class, ImageFileInformationControl::new));
						} else if (AudioCore.isSupportedAudio((IFile) resource)) {
							proposal.setControlCreator(new GenericInformationControlCreator(
									AudioFileInformationControl.class, AudioFileInformationControl::new));
						} else {
							proposal.setControlCreator(controlCreator);
						}

						list.add(proposal);
					}
					return true;
				}
			});
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		return list;
	}
}
