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
package phasereditor.canvas.ui.refactoring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.json.JSONObject;
import org.json.JSONTokener;

import javafx.scene.Node;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class UpdatePrefabReferencesChange extends Change {

	private IPath _srcPath;
	private IPath _dstPath;
	private String _name;
	private IProject _project;
	private List<CanvasEditor> _affectedEditors;
	private List<CanvasFile> _affectedFiles;
	private IFile _movingFile;

	public UpdatePrefabReferencesChange(String name, IFile file, IPath srcPath, IPath dstPath) {
		super();
		_srcPath = srcPath;
		_dstPath = dstPath;
		_name = name;
		_project = file.getProject();
		_movingFile = file;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
		_affectedEditors = new ArrayList<>();
		_affectedFiles = new ArrayList<>();

		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(_project);
		Set<CanvasFile> used = new HashSet<>();

		for (CanvasFile cfile : cfiles) {
			IFile file = cfile.getFile();

			if (file.equals(_movingFile)) {
				continue;
			}

			PhaserEditorUI.forEachOpenFileEditor(file, editor -> {
				if (editor instanceof CanvasEditor) {
					_affectedEditors.add((CanvasEditor) editor);
					used.add(cfile);
				}
			});
		}

		for (CanvasFile cfile : cfiles) {
			IFile file = cfile.getFile();

			if (file.equals(_movingFile)) {
				continue;
			}

			_affectedFiles.add(cfile);
		}

	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		IFile dstFile = _project.getFile(_dstPath);

		for (CanvasEditor editor : _affectedEditors) {
			GroupNode world = editor.getCanvas().getWorldNode();
			boolean[] modified = { false };
			walkTree(world, node -> {
				BaseObjectModel model = node.getModel();
				if (model.isPrefabInstance()) {
					IPath path = model.getPrefab().getFile().getProjectRelativePath();
					if (path.equals(_srcPath)) {
						// just change the file of the prefabs, the rebuild
						// should be made by the canvas builder participant.
						model.getPrefab().setFile(dstFile);
						modified[0] = true;
					}
				}
			});
		}

		String srcFileName = _srcPath.toPortableString();
		String dstFileName = _dstPath.toPortableString();

		for (CanvasFile cfile : _affectedFiles) {
			IFile file = cfile.getFile();
			try (InputStream contents = file.getContents()) {
				JSONObject data = new JSONObject(new JSONTokener(contents));

				boolean modified = false;

				if (data.has("prefab-table")) {
					JSONObject table = data.getJSONObject("prefab-table");
					for (String id : table.keySet()) {
						String fname = table.getString(id);

						if (srcFileName.equals(fname)) {
							table.put(id, dstFileName);
							modified = true;
						}
					}
				}

				if (modified) {
					String content = data.toString(2);
					file.setContents(new ByteArrayInputStream(content.getBytes()), true, false, pm);
				}

			} catch (IOException e) {
				CanvasUI.logError(e);
			}
		}

		return new UpdatePrefabReferencesChange(_name, _movingFile, _dstPath, _srcPath);
	}

	@Override
	public Object getModifiedElement() {
		return _movingFile;
	}

	protected static void walkTree(IObjectNode node, Consumer<IObjectNode> visitor) {
		visitor.accept(node);

		if (node instanceof GroupNode) {
			GroupNode group = (GroupNode) node;
			for (Node n : group.getChildren()) {
				walkTree((IObjectNode) n, visitor);
			}
		}
	}
}
