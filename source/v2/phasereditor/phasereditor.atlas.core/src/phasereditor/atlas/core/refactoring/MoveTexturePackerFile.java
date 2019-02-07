package phasereditor.atlas.core.refactoring;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.resource.MoveResourceChange;
import org.eclipse.ltk.internal.core.refactoring.resource.MoveResourcesProcessor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class MoveTexturePackerFile extends MoveParticipant {

	private IFile _file;
	private List<IFile> _images;
	private List<MoveParticipant> _participants;
	private IFile _jsonFile;
	private List<MoveResourceChange> _moveChanges;
	private Set<Object> _movingElements;

	@Override
	protected boolean initialize(Object element) {

		_file = (IFile) element;

		_jsonFile = ResourcesPlugin.getWorkspace().getRoot()
				.getFile(_file.getFullPath().removeFileExtension().addFileExtension("json"));

		_images = new ArrayList<>();

		_moveChanges = new ArrayList<>();

		_movingElements = Set.of(getProcessor().getElements());

		try (InputStream contents = _file.getContents()) {
			JSONObject obj = new JSONObject(new JSONTokener(contents));

			// pages
			{
				JSONArray jsonPages = obj.optJSONArray("pages");
				if (jsonPages != null) {
					var pagesCount = jsonPages.length();
					for (int i = 0; i < pagesCount; i++) {
						{
							IPath filename;
							var name = new Path(_file.getName()).removeFileExtension().toString();

							if (pagesCount > 1) {
								name += i + 1;
							}

							filename = new Path(name).addFileExtension("png");

							var imageFile = _file.getParent().getFile(filename);

							_images.add(imageFile);

						}
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		return true;
	}

	@Override
	public String getName() {
		return "Move Texture Packer output files.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		var status = new RefactoringStatus();

		if (canMoveFile(_jsonFile)) {
			status.addInfo("The output Multi-Atlas file '" + _jsonFile.getName() + "' will be moved.");
		}

		for (var file : _images) {
			if (canMoveFile(file)) {
				status.addInfo("The generated texture '" + file.getName() + "' will be moved.");
			}
		}

		_participants = new ArrayList<>();

		var sharable = new SharableParticipants();

		try {

			if (canMoveFile(_jsonFile)) {
				registerMove(_jsonFile, status, sharable, pm, context);
			}

			// move image files participants
			for (var image : _images) {
				if (canMoveFile(image)) {
					registerMove(image, status, sharable, pm, context);
				}
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}

		return status;
	}

	private boolean canMoveFile(IFile file) {
		return file.exists() && !_movingElements.contains(file);
	}

	private void registerMove(IResource resource, RefactoringStatus status, SharableParticipants sharable,
			IProgressMonitor pm, CheckConditionsContext context) throws CoreException {

		var destination = (IContainer) getArguments().getDestination();

		var processor = new MoveResourcesProcessor(new IResource[] { resource });

		{

			processor.setDestination(destination);
			processor.setUpdateReferences(getArguments().getUpdateReferences());

			var status2 = processor.checkInitialConditions(pm);
			status.merge(status2);

			status2 = processor.checkFinalConditions(pm, context);
			status.merge(status2);
		}

		var list = processor.loadParticipants(status, sharable);

		for (var participant : list) {
			if (participant.initialize(processor, resource, getArguments())) {
				var status2 = participant.checkConditions(pm, context);
				status.merge(status2);
				_participants.add((MoveParticipant) participant);
			}
		}

		_moveChanges.add(new MoveResourceChange(resource, destination));
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		var changes = new CompositeChange("Move the Texture Packer output files.");

		for (var change : _moveChanges) {
			changes.add(change);
		}

		for (var participant : _participants) {
			var change = participant.createChange(pm);
			changes.add(change);
		}

		return changes;
	}

}
