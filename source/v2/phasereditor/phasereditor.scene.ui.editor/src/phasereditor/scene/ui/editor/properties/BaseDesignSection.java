// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.scene.ui.editor.properties;

import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.editor.messages.UpdateScenePropertiesMessage;
import phasereditor.scene.ui.editor.undo.ScenePropertiesSnapshotOperation;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public abstract class BaseDesignSection extends ScenePropertySection {

	public BaseDesignSection(String name, FormPropertyPage page) {
		super(name, page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof SceneModel;
	}
	
	@Override
	protected void wrapOperation(Runnable run) {
		var editor = getEditor();

		var before = ScenePropertiesSnapshotOperation.takeSnapshot(editor);

		run.run();

		var after = ScenePropertiesSnapshotOperation.takeSnapshot(editor);

		editor.executeOperation(new ScenePropertiesSnapshotOperation(before, after, "Change display property."));

		editor.setDirty(true);
		
		getEditor().getBroker().sendAll(new UpdateScenePropertiesMessage(after));
	}
	
}
