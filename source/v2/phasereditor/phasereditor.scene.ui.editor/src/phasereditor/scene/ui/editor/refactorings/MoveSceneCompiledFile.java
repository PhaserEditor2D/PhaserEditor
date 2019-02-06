// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.scene.ui.editor.refactorings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.resource.MoveResourceChange;
import org.eclipse.ltk.internal.core.refactoring.resource.MoveResourcesProcessor;

/**
 * @author arian
 *
 */
public class MoveSceneCompiledFile extends MoveParticipant {
	private IFile _sceneFile;
	private IFile _jsFile;
	private List<MoveParticipant> _participants;

	@Override
	protected boolean initialize(Object element) {
		var file = (IFile) element;

		_sceneFile = file;
		_jsFile = _sceneFile.getProject()
				.getFile(_sceneFile.getProjectRelativePath().removeFileExtension().addFileExtension("js"));

		if (!_jsFile.exists()) {
			return false;
		}

		var elements = getProcessor().getElements();

		if (Set.of(elements).contains(_jsFile)) {
			// the js file is included in the move
			return false;
		}

		return true;
	}

	@Override
	public String getName() {
		return "Move Scene compiled files.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		var status = new RefactoringStatus();

		status.addInfo("The Scene compiled file '" + _jsFile.getName() + "' will be moved.");

		_participants = new ArrayList<>();

		var sharable = new SharableParticipants();

		try {

			var moveJsFileProcessor = new MoveResourcesProcessor(new IResource[] { _jsFile });
			{
				moveJsFileProcessor.setDestination((IContainer) getArguments().getDestination());
				moveJsFileProcessor.setUpdateReferences(getArguments().getUpdateReferences());

				var status2 = moveJsFileProcessor.checkInitialConditions(pm);
				status.merge(status2);

				status2 = moveJsFileProcessor.checkFinalConditions(pm, context);
				status.merge(status2);
			}

			var list = moveJsFileProcessor.loadParticipants(status, sharable);

			for (var participant : list) {
				if (participant.initialize(moveJsFileProcessor, _jsFile, getArguments())) {
					var status2 = participant.checkConditions(pm, context);
					status.merge(status2);
					_participants.add((MoveParticipant) participant);
				}
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		var changelist = new CompositeChange("Update stuff related to file '" + _sceneFile.getName() + "'");

		IContainer folder = (IContainer) getArguments().getDestination();

		changelist.add(new MoveResourceChange(_jsFile, folder));

		for (var participant : _participants) {
			var change = participant.createChange(pm);
			changelist.add(change);
		}

		return changelist;
	}
}
