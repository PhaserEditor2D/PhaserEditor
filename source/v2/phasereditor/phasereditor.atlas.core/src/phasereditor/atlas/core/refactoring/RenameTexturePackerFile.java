package phasereditor.atlas.core.refactoring;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
import org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class RenameTexturePackerFile extends RenameParticipant {

	private IFile _file;
	private List<IFile> _images;
	private List<RenameParticipant> _participants;
	private IFile _jsonFile;
	private List<RenameResourceChange> _renameChanges;

	@Override
	protected boolean initialize(Object element) {

		_file = (IFile) element;

		_jsonFile = ResourcesPlugin.getWorkspace().getRoot()
				.getFile(_file.getFullPath().removeFileExtension().addFileExtension("json"));

		_images = new ArrayList<>();

		_renameChanges = new ArrayList<>();

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
		return "Rename Texture Packer output images.";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		var status = new RefactoringStatus();

		if (_jsonFile.exists()) {
			status.addInfo("The output Multi-Atlas file '" + _jsonFile.getName() + "' will be renamed.");
		}

		for (var file : _images) {
			if (file.exists()) {
				status.addInfo("The generated texture '" + file.getName() + "' will be renamed.");
			}
		}

		_participants = new ArrayList<>();

		var sharable = new SharableParticipants();

		try {

			if (_jsonFile.exists()) {
				// rename json file participants
				var newJsonFileName = new Path(getArguments().getNewName()).removeFileExtension()
						.addFileExtension("json").lastSegment();

				registerRename(_jsonFile, newJsonFileName, status, sharable, pm, context);
			}

			// rename image files participants
			var i = 0;
			for (var image : _images) {

				if (!image.exists()) {
					continue;
				}

				String newImageName;

				{
					String basename = new Path(getArguments().getNewName()).removeFileExtension().toString();
					if (_images.size() > 1) {
						newImageName = new Path(basename + (i + 1)).addFileExtension("png").toString();
					} else {
						newImageName = new Path(basename).addFileExtension("png").toString();
					}
				}

				registerRename(image, newImageName, status, sharable, pm, context);

				i++;
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}

		return status;
	}

	private void registerRename(IResource resource, String newName, RefactoringStatus status,
			SharableParticipants sharable, IProgressMonitor pm, CheckConditionsContext context) throws CoreException {

		var renameProcessor = new RenameResourceProcessor(resource);

		{
			renameProcessor.setNewResourceName(newName);

			var status2 = renameProcessor.checkInitialConditions(pm);
			status.merge(status2);

			status2 = renameProcessor.checkFinalConditions(pm, context);
			status.merge(status2);
		}

		var list = renameProcessor.loadParticipants(status, sharable);

		for (var participant : list) {
			if (participant.initialize(renameProcessor, resource, new RenameArguments(newName, true))) {
				var status2 = participant.checkConditions(pm, context);
				status.merge(status2);
				_participants.add((RenameParticipant) participant);
			}
		}

		_renameChanges.add(new RenameResourceChange(resource.getFullPath(), newName));
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		var changes = new CompositeChange("Rename the Texture Packer output images.");

		for (var change : _renameChanges) {
			changes.add(change);
		}

		for (var participant : _participants) {
			var change = participant.createChange(pm);
			changes.add(change);
		}

		return changes;
	}

}
