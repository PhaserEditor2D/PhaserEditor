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
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor;
import phasereditor.project.core.PhaserProjectNature;

/**
 * @author arian
 *
 */
public class AssetMoveProcessor extends MoveProcessor {

	protected final AssetModel[] _assets;
	protected final AssetPackEditor _editor;
	private AssetSectionModel _dstSection;

	public AssetMoveProcessor(AssetSectionModel section, AssetModel[] assets, AssetPackEditor editor) {
		super();
		_editor = editor;
		_assets = assets;
		_dstSection = section;
	}

	@Override
	public String getIdentifier() {
		return getClass().getName();
	}

	public boolean isInDirtyEditor() {
		return _editor != null && _editor.isDirty();
	}

	public AssetSectionModel getDstSection() {
		return _dstSection;
	}

	public void setDstSection(AssetSectionModel dstSection) {
		_dstSection = dstSection;
	}

	@Override
	public Object[] getElements() {
		return _assets;
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

		final ArrayList<MoveParticipant> result = new ArrayList<>();

		if (!isApplicable()) {
			return new RefactoringParticipant[0];
		}

		MoveArguments args = new MoveAssetsArguments(_dstSection, isInDirtyEditor());

		result.addAll(Arrays.asList(
				ParticipantManager.loadMoveParticipants(status, this, new AssetMoveList(_assets, _dstSection.getKey()),
						args, PhaserProjectNature.NATURE_IDS, sharedParticipants)));

		return result.toArray(new RefactoringParticipant[result.size()]);

	}

	public RefactoringStatus validateDestiny(AssetSectionModel dstSection) {
		for (AssetModel asset : _assets) {
			if (dstSection == asset.getSection()) {
				return RefactoringStatus.createFatalErrorStatus("Cannot move to the same section.");
			}
		}

		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public String getProcessorName() {
		return "Move Asset";
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		AssetMoveList movelist = new AssetMoveList(_assets, _dstSection.getKey());

		if (isInDirtyEditor()) {
			return new MoveAssetInEditorChange(movelist, _editor);
		}

		return new MoveAssetInFileChange(movelist);
	}
}
