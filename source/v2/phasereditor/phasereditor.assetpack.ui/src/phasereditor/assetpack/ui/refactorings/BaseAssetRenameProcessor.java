// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.assetpack.ui.refactorings;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import phasereditor.assetpack.ui.editors.AssetPackEditor2;
import phasereditor.project.core.PhaserProjectNature;

/**
 * @author arian
 *
 */
public abstract class BaseAssetRenameProcessor extends RenameProcessor {

	protected final Object _element;
	protected final AssetPackEditor2 _editor;
	protected String _newName;
	protected final String _initialName;

	public BaseAssetRenameProcessor(Object element, String initialName, AssetPackEditor2 editor) {
		super();
		_editor = editor;
		_element = element;
		_initialName = initialName;
		_newName = _initialName;
	}
	
	@Override
	public String getIdentifier() {
		return getClass().getName();
	}


	public boolean isInDirtyEditor() {
		return _editor != null && _editor.isDirty();
	}

	public String getNewName() {
		return _newName;
	}

	public void setNewName(String newName) {
		_newName = newName;
	}

	@Override
	public Object[] getElements() {
		return new Object[] { _element };
	}

	
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {

		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws CoreException, OperationCanceledException {

		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
			throws CoreException {

		final ArrayList<RenameParticipant> result = new ArrayList<>();

		if (!isApplicable()) {
			return new RefactoringParticipant[0];
		}

		RenameArguments args = new RenameAssetArguments(_newName, true, isInDirtyEditor());

		result.addAll(Arrays.asList(ParticipantManager.loadRenameParticipants(status, this, _element, args,
				PhaserProjectNature.NATURE_IDS, sharedParticipants)));

		return result.toArray(new RefactoringParticipant[result.size()]);

	}

	public RefactoringStatus validateNewName(String text) {
		if (text.trim().length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Invalid name.");
		}

		if (_initialName.equals(text)) {
			return RefactoringStatus.createFatalErrorStatus("Cannot use the same name.");
		}

		return RefactoringStatus.create(Status.OK_STATUS);
	}
}
