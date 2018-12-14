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
package phasereditor.assetpack.ui.editor.refactorings;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.ltk.core.refactoring.participants.DeleteProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.project.core.PhaserProjectNature;

/**
 * @author arian
 *
 */
public class AssetDeleteProcessor extends DeleteProcessor {

	private Object[] _elements;
	private AssetPackEditor _editor;

	public AssetDeleteProcessor(Object[] elements, AssetPackEditor editor) {
		super();

		_editor = editor;

		List<Object> list = new ArrayList<>();

		for (Object elem : elements) {
			if (elem instanceof AssetModel) {
				list.add(elem);
			} else if (elem instanceof AssetPackModel) {
				list.addAll(((AssetPackModel) elem).getAssets());
			} else if (elem instanceof AssetSectionModel) {
				list.addAll(((AssetSectionModel) elem).getAssets());
			} else if (elem instanceof AssetGroupModel) {
				list.addAll(((AssetGroupModel) elem).getAssets());
			}
		}

		for (Object elem : elements) {
			if (elem instanceof AssetSectionModel) {
				list.add(elem);
			}
		}

		_elements = list.toArray();
	}

	@Override
	public Object[] getElements() {
		return _elements;
	}

	@Override
	public String getIdentifier() {
		return "phasereditor.assetpack.ui.refactorings.deleteAssetProcessor";
	}

	@Override
	public String getProcessorName() {
		return "Delete Asset Processor";
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {

		if (_elements.length == 0) {
			RefactoringStatus.create(Status.CANCEL_STATUS);
		}

		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws CoreException, OperationCanceledException {

		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange change = new CompositeChange("Delete assets") {

			@SuppressWarnings("hiding")
			@Override
			public Change perform(IProgressMonitor pm) throws CoreException {
				Change c = super.perform(pm);

				swtRun(() -> _editor.build());

				return c;
			}
		};

		for (Object elem : _elements) {

			if (_editor == null) {
				if (elem instanceof AssetModel) {
					change.add(new DeleteAssetChange((AssetModel) elem));
				} else {
					AssetSectionModel section = (AssetSectionModel) elem;
					change.add(new DeleteAssetSectionChange(section.getPack().getFile(), section.getKey()));
				}
			} else {
				if (elem instanceof AssetModel) {
					change.add(new DeleteAssetInEditorChange((AssetModel) elem, _editor));
				} else {
					change.add(new DeleteAssetSectionInEditorChange(((AssetSectionModel) elem).getKey(), _editor));
				}
			}

		}

		return change;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
			throws CoreException {

		final ArrayList<DeleteParticipant> result = new ArrayList<>();

		if (!isApplicable()) {
			return new RefactoringParticipant[0];
		}

		final DeleteArguments deleteArguments = new DeleteArguments();

		for (Object elem : _elements) {
			if (elem instanceof AssetModel) {
				result.addAll(Arrays.asList(ParticipantManager.loadDeleteParticipants(status, this, elem,
						deleteArguments, PhaserProjectNature.NATURE_IDS, sharedParticipants)));
			}
		}

		return result.toArray(new RefactoringParticipant[result.size()]);

	}

}
