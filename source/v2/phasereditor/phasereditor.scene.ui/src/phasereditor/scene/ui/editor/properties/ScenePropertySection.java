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
import java.util.function.Function;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;

import phasereditor.inspect.core.InspectCore;
import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.ui.editor.SceneCanvas;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.interactive.InteractiveTool;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public abstract class ScenePropertySection extends FormPropertySection<ObjectModel> {
	private FormPropertyPage _page;

	public ScenePropertySection(String name, FormPropertyPage page) {
		super(name);
		_page = page;
	}

	public FormPropertyPage getPage() {
		return _page;
	}

	public SceneEditor getEditor() {
		return ((ScenePropertyPage) _page).getEditor();
	}

	public SceneCanvas getScene() {
		return getEditor().getScene();
	}

	protected void listenFloat(Text text, Consumer<Float> listener, List<ObjectModel> models) {
		listenFloat(text, listener, models, false);
	}

	protected void listenFloat(Text text, Consumer<Float> listener, List<ObjectModel> models, boolean dirtyModels) {
		listenFloat(text, listener, models, dirtyModels, null);
	}

	protected void listenFloat(Text text, Consumer<Float> listener, List<ObjectModel> models, boolean dirtyModels,
			Function<ObjectModel, Boolean> filterDirtyModels) {

		super.listenFloat(text, value -> {

			var beforeData = SingleObjectSnapshotOperation.takeSnapshot(models);

			listener.accept(value);

			var afterData = SingleObjectSnapshotOperation.takeSnapshot(models);

			getEditor().executeOperation(new SingleObjectSnapshotOperation(beforeData, afterData,
					"Change object property", dirtyModels, filterDirtyModels));

			dirtyModels(models, dirtyModels, filterDirtyModels);

			getScene().redraw();

		});
	}

	private void dirtyModels(List<ObjectModel> models, boolean dirtyModels,
			Function<ObjectModel, Boolean> filterDirtyModels) {

		if (dirtyModels) {
			models.forEach(model -> {
				if (filterDirtyModels == null || filterDirtyModels.apply(model).booleanValue()) {
					GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);
				}
			});

			var editor = getEditor();
			
			if (editor.getOutline() != null) {
				editor.refreshOutline_basedOnId();
			}
		}
	}

	protected void listenInt(Text text, Consumer<Integer> listener, List<ObjectModel> models) {
		listenInt(text, listener, models, false);
	}

	protected void listenInt(Text text, Consumer<Integer> listener, List<ObjectModel> models, boolean dirtyModels) {
		super.listenInt(text, value -> {

			var beforeData = SingleObjectSnapshotOperation.takeSnapshot(models);

			listener.accept(value);

			var afterData = SingleObjectSnapshotOperation.takeSnapshot(models);

			getEditor().executeOperation(
					new SingleObjectSnapshotOperation(beforeData, afterData, "Change object property", dirtyModels));

			dirtyModels(models, dirtyModels, null);

			getScene().redraw();
		});
	}

	protected void listen(Text text, Consumer<String> listener, List<ObjectModel> models) {
		listen(text, listener, models, false);
	}

	protected void listen(Text text, Consumer<String> listener, List<ObjectModel> models, boolean dirtyModels) {
		super.listen(text, value -> {

			var beforeData = SingleObjectSnapshotOperation.takeSnapshot(models);

			listener.accept(value);

			var afterData = SingleObjectSnapshotOperation.takeSnapshot(models);

			getEditor().executeOperation(
					new SingleObjectSnapshotOperation(beforeData, afterData, "Change object property", dirtyModels));

			dirtyModels(models, dirtyModels, null);

			getScene().redraw();

		});
	}

	protected void wrapOperation(Runnable run, List<ObjectModel> models) {
		wrapOperation(run, models, false, null);
	}
	
	protected void wrapOperation(Runnable run, List<ObjectModel> models, boolean dirtyModels) {
		wrapOperation(run, models, dirtyModels, null);
	}
	
	protected void wrapOperation(Runnable run, List<ObjectModel> models, boolean dirtyModels, Function<ObjectModel, Boolean> filterDirtyModels) {
		var beforeData = SingleObjectSnapshotOperation.takeSnapshot(models);

		run.run();

		var afterData = SingleObjectSnapshotOperation.takeSnapshot(models);

		getEditor().executeOperation(
				new SingleObjectSnapshotOperation(beforeData, afterData, "Change object property", dirtyModels, filterDirtyModels));

		dirtyModels(models, dirtyModels, null);

		getScene().redraw();

	}

	protected void listen(Button check, Consumer<Boolean> listener, List<ObjectModel> models) {
		super.listen(check, value -> {

			var beforeData = SingleObjectSnapshotOperation.takeSnapshot(models);

			listener.accept(value);

			var afterData = SingleObjectSnapshotOperation.takeSnapshot(models);

			getEditor().executeOperation(
					new SingleObjectSnapshotOperation(beforeData, afterData, "Change object property"));

			getScene().redraw();
		});
	}

	protected void listenFloat(Scale scale, Consumer<Float> listener, List<ObjectModel> models) {
		super.listenFloat(scale, value -> {

			var beforeData = SingleObjectSnapshotOperation.takeSnapshot(models);

			listener.accept(value);

			var afterData = SingleObjectSnapshotOperation.takeSnapshot(models);

			getEditor().executeOperation(
					new SingleObjectSnapshotOperation(beforeData, afterData, "Change object property"));

			getScene().redraw();

		});
	}

	@Override
	protected String getHelp(String helpHint) {

		if (helpHint.startsWith("*")) {
			return helpHint.substring(1);
		}

		return InspectCore.getPhaserHelp().getMemberHelp(helpHint);
	}
	
	protected void setInteractiveTools(InteractiveTool... tools) {
		getEditor().getScene().setInteractiveTools(tools);
	}
}
