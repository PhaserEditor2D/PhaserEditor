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
package phasereditor.ui.editors;

import static java.lang.System.out;

import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.EditorPart;

/**
 * @author arian
 *
 */
public class EditorFileStampHelper {
	private EditorPart _editor;
	private long _lastFileStamp;
	private String _editorName;
	private Runnable _reloadMethod;
	private Consumer<IProgressMonitor> _saveMethod;

	public EditorFileStampHelper(EditorPart editor, Runnable reloadMethod, Consumer<IProgressMonitor> saveMethod) {
		_editor = editor;
		_editorName = _editor.getClass().getSimpleName();
		_reloadMethod = reloadMethod;
		_saveMethod = saveMethod;
	}

	public void helpReloadFile() {
		var file = getEditorInput().getFile();
		var stamp = file.getModificationStamp();

		if (_lastFileStamp == stamp) {
			// nothing changed, return.
			// this happen when the modification is perfomed by the editor itself
			out.println(_editorName + ": abort reloadFile() " + file);
			return;
		}

		_lastFileStamp = stamp;

		out.println(_editorName + ": reloadFile() " + file);

		_reloadMethod.run();
	}

	private IFileEditorInput getEditorInput() {
		return (IFileEditorInput) _editor.getEditorInput();
	}

	public void helpDoSave(IProgressMonitor monitor) {
		var file = getEditorInput().getFile();
		var stamp = file.getModificationStamp();
		if (stamp != _lastFileStamp) {
			// the file was modified by other editor
			var confirmSave = MessageDialog.openConfirm(_editor.getEditorSite().getShell(), "Save",
					"The editor's content was modified by an external editor. Do you want to continue saving?\n\n"
							+ "WARNING: If you continue the saving, the external modifications will be lost!\n");
			if (!confirmSave) {
				var confirmReload = MessageDialog.openConfirm(_editor.getEditorSite().getShell(), "Reload",
						"Do you want to load the external changes?");

				if (confirmReload) {
					helpReloadFile();
				}

				return;
			}
		}

		_saveMethod.accept(monitor);

		_lastFileStamp = file.getModificationStamp();

	}

}
