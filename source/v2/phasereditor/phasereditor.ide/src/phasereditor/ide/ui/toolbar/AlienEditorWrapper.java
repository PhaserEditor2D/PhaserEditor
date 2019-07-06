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
package phasereditor.ide.ui.toolbar;

import static phasereditor.ui.IEditorSharedImages.IMG_ALIEN_EDITOR;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import phasereditor.project.core.ProjectCore;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.PhaserEditorUI;

class AlienEditorWrapper {
	private Button _btn;

	public AlienEditorWrapper(Composite parent) {
		_btn = new Button(parent, SWT.PUSH);
		_btn.setText("Editor");
		_btn.setToolTipText("Open this project in the configured External Editor.");
		_btn.setImage(EditorSharedImages.getImage(IMG_ALIEN_EDITOR));
		_btn.addSelectionListener(SelectionListener
				.widgetSelectedAdapter(e -> PhaserEditorUI.externalEditor_openProject(ProjectCore.getActiveProject())));

		{
			IPropertyChangeListener listener = e -> {
				if (PhaserEditorUI.PREF_PROP_ALIEN_EDITOR_ENABLED.equals(e.getProperty())) {
					updateButton();
				}
			};
			PhaserEditorUI.getPreferenceStore().addPropertyChangeListener(listener);
			_btn.addDisposeListener(e -> PhaserEditorUI.getPreferenceStore().removePropertyChangeListener(listener));
		}

		updateButton();
	}

	private void updateButton() {
		var visible = PhaserEditorUI.externalEditor_enabled();
		_btn.setLayoutData(new RowData(visible ? SWT.DEFAULT : 0, visible ? SWT.DEFAULT : 0));
		_btn.getParent().requestLayout();
	}

	public Button getButton() {
		return _btn;
	}

}