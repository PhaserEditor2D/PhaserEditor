package phasereditor.canvas.ui.refactoring;

import static java.lang.System.out;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.resource.MoveResourceChange;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.PrefabReference;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.ui.PhaserEditorUI;

public class CanvasFileMoveParticipant extends MoveParticipant {

	private CanvasType _canvasType;
	private IFile _file;

	public CanvasFileMoveParticipant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean initialize(Object element) {
		if (!(element instanceof IFile)) {
			return false;
		}

		IFile file = (IFile) element;

		if (!CanvasCore.isCanvasFile(file)) {
			return false;
		}

		_file = file;

		_canvasType = CanvasCore.getCanvasType(file);

		return true;
	}

	@Override
	public String getName() {
		return "Move Canvas";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		RefactoringStatus status = new RefactoringStatus();

		if (!_canvasType.isPrefab()) {
			return status;
		}

		Map<IFile, List<PrefabReference>> refMap = CanvasUI.findPrefabReferences(new Prefab(_file, _canvasType));

		for (Entry<IFile, List<PrefabReference>> entry : refMap.entrySet()) {
			IFile file = entry.getKey();
			List<PrefabReference> refs = entry.getValue();

			String filepath = file.getProjectRelativePath().toString();

			status.addWarning("The canvas file '" + filepath + "' has " + refs.size() + " prefab '" + _file.getName()
					+ "' instances.", new CanvasFileRefactoringStatusContext(file));
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		List<IFile> files = CanvasCore.getCanvasDereivedFiles(_file);

		if (files.isEmpty()) {
			return null;
		}

		MoveArguments args = getArguments();

		IContainer dst = (IContainer) args.getDestination();

		if (files.size() == 1) {
			return new MoveResourceChange(files.get(0), dst);
		}

		CompositeChange change = new CompositeChange(
				"Delete " + files.size() + " files derived from " + _file.getFullPath().toPortableString());

		for (IFile file : files) {
			change.add(new MoveResourceChange(file, dst));
		}

		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(_file.getProject());
		
		for (CanvasFile cfile : cfiles) {
			IFile file = cfile.getFile();

			if (file.equals(_file)) {
				continue;
			}

			boolean inEditor = PhaserEditorUI.forEachOpenFileEditor(file, editor -> {
				out.println("Missing to check " + editor);
			});

			if (!inEditor) {
				
				IFile dstFile = dst.getFile(new Path(_file.getName()));
				String dstPath = dstFile.getProjectRelativePath().toPortableString();
				
				try (InputStream contents = file.getContents()) {
					JSONObject obj = new JSONObject(new JSONTokener(contents));
//					replaceGropup(world,)
				} catch (IOException e) {
					CanvasUI.logError(e);
				}
			}
		}

		// TextFileChange change2 = new TextFileChange("pepe", _file);
		// change2.setEdit(new ReplaceEdit(0, 0, getName()));

		return change;
	}

}
