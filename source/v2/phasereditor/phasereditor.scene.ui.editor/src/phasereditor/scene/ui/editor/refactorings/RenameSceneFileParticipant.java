package phasereditor.scene.ui.editor.refactorings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
import org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor;

import phasereditor.scene.core.SceneCore;

public class RenameSceneFileParticipant extends RenameParticipant {

	private IFile _sceneFile;
	private IFile _jsFile;
	private List<RenameParticipant> _participants;
	private String _jsFileNewName;

	public RenameSceneFileParticipant() {
	}

	@Override
	protected boolean initialize(Object element) {

		if (!(element instanceof IFile)) {
			return false;
		}

		var file = (IFile) element;

		if (!SceneCore.isSceneFile(file)) {
			return false;
		}

		_sceneFile = file;
		_jsFile = _sceneFile.getProject()
				.getFile(_sceneFile.getProjectRelativePath().removeFileExtension().addFileExtension("js"));

		if (!_jsFile.exists()) {
			return false;
		}

		var newSceneFileName = getArguments().getNewName();
		_jsFileNewName = new Path(newSceneFileName).removeFileExtension().addFileExtension("js").toString();

		return true;
	}

	@Override
	public String getName() {
		return "Rename scene file.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		var status = new RefactoringStatus();

		status.addInfo("The Scene compiled file '" + _jsFile.getName() + "' will be renamed.");

		_participants = new ArrayList<>();

		var sharable = new SharableParticipants();

		try {

			var renameJsFileProcessor = new RenameResourceProcessor(_jsFile);
			{
				renameJsFileProcessor.setNewResourceName(_jsFileNewName);

				var status2 = renameJsFileProcessor.checkInitialConditions(pm);
				status.merge(status2);

				status2 = renameJsFileProcessor.checkFinalConditions(pm, context);
				status.merge(status2);
			}

			var list = renameJsFileProcessor.loadParticipants(status, sharable);

			for (var participant : list) {
				if (participant.initialize(renameJsFileProcessor, _jsFile, new RenameArguments(_jsFileNewName, true))) {
					var status2 = participant.checkConditions(pm, context);
					status.merge(status2);
					_participants.add((RenameParticipant) participant);
				}
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		var changelist = new CompositeChange("Rename Scene compiled file '" + _jsFile.getName() + "'");
		changelist.add(new RenameResourceChange(_jsFile.getFullPath(), _jsFileNewName));

		for (var participant : _participants) {
			var change = participant.createChange(pm);
			changelist.add(change);
		}

		return changelist;
	}

}
