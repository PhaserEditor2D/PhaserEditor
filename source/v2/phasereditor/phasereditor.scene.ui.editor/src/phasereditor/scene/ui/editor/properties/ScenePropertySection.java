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
import java.util.function.Function;

import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.inspect.core.InspectCore;
import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.PackReferencesCollector;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.messages.LoadAssetsMessage;
import phasereditor.scene.ui.editor.messages.ResetSceneMessage;
import phasereditor.scene.ui.editor.messages.SelectObjectsMessage;
import phasereditor.scene.ui.editor.messages.UpdateObjectsMessage;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;
import phasereditor.ui.properties.CheckListener;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.FormPropertySection;
import phasereditor.ui.properties.ScaleListener;

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

	public AssetFinder getAssetFinder() {
		return getEditor().getAssetFinder();
	}

	public SceneModel getSceneModel() {
		return getEditor().getSceneModel();
	}

	protected abstract class SceneText extends phasereditor.ui.properties.TextListener {

		protected boolean dirtyModels;
		protected Function<ObjectModel, Boolean> filterDirtyModels;

		public SceneText(Text widget) {
			super(widget);

			dirtyModels = false;
			filterDirtyModels = null;
		}

		@Override
		protected void accept(String value) {
			wrapOperation(() -> accept2(value), dirtyModels, filterDirtyModels);
		}

		protected abstract void accept2(String value);

	}

	protected abstract class SceneTextToFloat extends phasereditor.ui.properties.TextToFloatListener {

		protected boolean dirtyModels;
		protected Function<ObjectModel, Boolean> filterDirtyModels;

		public SceneTextToFloat(Text widget) {
			super(widget);

			dirtyModels = false;
			filterDirtyModels = null;
		}

		@Override
		protected void accept(float value) {
			wrapOperation(() -> accept2(value), dirtyModels, filterDirtyModels);
		}

		protected abstract void accept2(float value);

	}

	protected abstract class SceneTextToInt extends phasereditor.ui.properties.TextToIntListener {

		protected boolean dirtyModels;
		protected Function<ObjectModel, Boolean> filterDirtyModels;

		public SceneTextToInt(Text widget) {
			super(widget);

			dirtyModels = false;
			filterDirtyModels = null;
		}

		@Override
		protected void accept(int value) {
			wrapOperation(() -> accept2(value), dirtyModels, filterDirtyModels);
		}

		protected abstract void accept2(int value);

	}

	protected static void dirtyModels(ScenePropertySection section, List<ObjectModel> models, boolean dirtyModels,
			Function<ObjectModel, Boolean> filterDirtyModels) {

		if (dirtyModels || filterDirtyModels != null) {
			models.forEach(model -> {
				if (filterDirtyModels == null || filterDirtyModels.apply(model).booleanValue()) {
					GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);
				}
			});

			var editor = section.getEditor();

			if (editor.getOutline() != null) {
				editor.refreshOutline_basedOnId();
			}
		}
	}

	protected void wrapOperation(Runnable run) {
		wrapOperation(run, false, null);
	}

	protected void wrapOperation(Runnable run, boolean dirtyModels) {
		wrapOperation(run, dirtyModels, null);
	}

	protected void wrapOperation(Runnable run, boolean dirtyModels, Function<ObjectModel, Boolean> filterDirtyModels) {

		var models = getModels();

		var beforeData = SingleObjectSnapshotOperation.takeSnapshot(models);

		run.run();

		var afterData = SingleObjectSnapshotOperation.takeSnapshot(models);

		getEditor().executeOperation(new SingleObjectSnapshotOperation(beforeData, afterData, "Change object property",
				dirtyModels, filterDirtyModels));

		dirtyModels(this, models, dirtyModels, filterDirtyModels);

		getEditor().getBroker().sendAll(UpdateObjectsMessage.createFromSnapshot(afterData));
	}

	protected void wrapWorldOperation(Runnable run) {
		var editor = getEditor();

		var collector = new PackReferencesCollector(editor.getSceneModel(), editor.getAssetFinder());

		var packData = collector.collectNewPack(() -> {
			var beforeData = WorldSnapshotOperation.takeSnapshot(getEditor());

			run.run();

			var afterData = WorldSnapshotOperation.takeSnapshot(getEditor());

			IUndoableOperation op = new WorldSnapshotOperation(beforeData, afterData, "Change object property");

			getEditor().executeOperation(op);
		});

		editor.getBroker().sendAllBatch(

				new LoadAssetsMessage(packData),

				new ResetSceneMessage(editor),

				new SelectObjectsMessage(editor)

		);
	}

	protected abstract class SceneCheckListener extends CheckListener {
		protected boolean dirtyModels;
		protected Function<ObjectModel, Boolean> filterDirtyModels;

		public SceneCheckListener(Button button) {
			super(button);
		}

		@Override
		protected void accept(boolean value) {
			wrapOperation(() -> accept2(value), dirtyModels, filterDirtyModels);
		}

		protected abstract void accept2(boolean value);

	}

	protected abstract class SceneScaleListener extends ScaleListener {
		protected boolean dirtyModels;
		protected Function<ObjectModel, Boolean> filterDirtyModels;

		public SceneScaleListener(Scale scale) {
			super(scale);
		}

		@Override
		protected void accept(float value) {
			wrapOperation(() -> accept2(value), dirtyModels, filterDirtyModels);
		}

		protected abstract void accept2(float value);
	}

	@Override
	protected String getHelp(String helpHint) {

		if (helpHint.startsWith("*")) {
			return helpHint.substring(1);
		}

		return InspectCore.getPhaserHelp().getMemberHelp(helpHint);
	}

}
