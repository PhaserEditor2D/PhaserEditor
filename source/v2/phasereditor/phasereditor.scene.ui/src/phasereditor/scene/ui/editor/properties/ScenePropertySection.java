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

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.ui.editor.SceneCanvas;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.ObjectSnapshotOperation;

/**
 * @author arian
 *
 */
public abstract class ScenePropertySection extends PropertySection {
	private ScenePropertiesPage _page;

	public ScenePropertySection(String name, ScenePropertiesPage page) {
		super(name);
		_page = page;
	}

	public ScenePropertiesPage getPage() {
		return _page;
	}

	public SceneEditor getEditor() {
		return _page.getEditor();
	}

	public SceneCanvas getCanvas() {
		return getEditor().getCanvas();
	}

	protected void listenFloat(Text text, Consumer<Float> listener, List<Object> models) {
		super.listenFloat(text, value -> {

			var beforeData = ObjectSnapshotOperation.takeSnapshot(models);

			listener.accept(value);

			getCanvas().getSceneRenderer().addPostPaintAction(() -> {
				var afterData = ObjectSnapshotOperation.takeSnapshot(models);
				getEditor()
						.executeOperation(new ObjectSnapshotOperation(beforeData, afterData, "Change object property"));
			});

			getCanvas().redraw();
		});
	}

	protected void listen(Text text, Consumer<String> listener, List<Object> models) {
		super.listen(text, value -> {

			var beforeData = ObjectSnapshotOperation.takeSnapshot(models);

			listener.accept(value);

			getCanvas().getSceneRenderer().addPostPaintAction(() -> {
				var afterData = ObjectSnapshotOperation.takeSnapshot(models);
				getEditor()
						.executeOperation(new ObjectSnapshotOperation(beforeData, afterData, "Change object property"));
			});

			getCanvas().redraw();

		});
	}

	protected void listen(Button check, Consumer<Boolean> listener, List<Object> models) {
		super.listen(check, value -> {

			var beforeData = ObjectSnapshotOperation.takeSnapshot(models);

			listener.accept(value);

			getCanvas().getSceneRenderer().addPostPaintAction(() -> {
				var afterData = ObjectSnapshotOperation.takeSnapshot(models);
				getEditor()
						.executeOperation(new ObjectSnapshotOperation(beforeData, afterData, "Change object property"));
			});

			getCanvas().redraw();
		});
	}

}
