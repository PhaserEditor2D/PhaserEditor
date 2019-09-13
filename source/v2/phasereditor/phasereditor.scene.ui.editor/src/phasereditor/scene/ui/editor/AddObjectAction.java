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
package phasereditor.scene.ui.editor;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SvgAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.NameComputer;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.PackReferencesCollector;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.editor.messages.DropObjectsMessage;
import phasereditor.scene.ui.editor.messages.LoadAssetsMessage;
import phasereditor.scene.ui.editor.messages.SelectObjectsMessage;

/**
 * @author arian
 *
 */
public abstract class AddObjectAction<T extends ObjectModel> extends Action {
	private SceneEditor _editor;

	public AddObjectAction(SceneEditor editor, String label) {
		super(label);
		_editor = editor;
	}

	public SceneEditor getEditor() {
		return _editor;
	}

	@Override
	public void run() {
		var model = createModel();

		if (model == null) {
			return;
		}

		var sceneModel = _editor.getSceneModel();
		var displayList = sceneModel.getDisplayList();
		var computer = new NameComputer(displayList);

		var name = createName(model, computer);

		VariableComponent.set_variableName(model, name);

		var pos = _editor.getCameraCenter();

		var models = new ArrayList<ObjectModel>();

		var collector = new PackReferencesCollector(_editor.getSceneModel(), _editor.getAssetFinder());

		var packData = collector.collectNewPack(() -> {
			models.addAll(_editor.selectionDropped(pos[0], pos[1], new Object[] { model }));
		});

		_editor.getBroker().sendAllBatch(

				new LoadAssetsMessage(packData),

				new DropObjectsMessage(models),

				new SelectObjectsMessage(_editor)

		);
	}

	protected static String createNameFromTexture(NameComputer computer, ObjectModel model, String basename) {
		String basename2 = null;
		var key = TextureComponent.get_textureKey(model);
		var frame = TextureComponent.get_textureFrame(model);

		if (frame != null) {
			basename2 = frame;
		}

		if (basename2 == null && key != null) {
			basename2 = key;
		}

		if (basename2 == null) {
			basename2 = basename;
		}

		return computer.newName(basename2);
	}

	protected BitmapFontAssetModel selectAndSetBitmapText(ObjectModel model) {
		var asset = AssetPackUI.openAssetDialog(getEditor().getProject(), key -> key instanceof BitmapFontAssetModel);

		if (asset != null) {
			if (asset instanceof BitmapFontAssetModel) {
				BitmapTextComponent.set_fontAssetKey(model, asset.getKey());
			} else {
				return null;
			}
		}
		return (BitmapFontAssetModel) asset;
	}

	protected IAssetKey selectAndSetTexture(ObjectModel model) {
		var asset = AssetPackUI.openAssetDialog(getEditor().getProject(),
				key -> key instanceof ImageAssetModel || key instanceof ImageAssetModel.Frame
						|| key instanceof AtlasAssetModel.Frame || key instanceof MultiAtlasAssetModel.Frame
						|| key instanceof SvgAssetModel);
		if (asset != null) {
			TextureComponent.set_textureKey(model, asset.getAsset().getKey());
			TextureComponent.set_textureFrame(model, asset.getKey());
		}
		return asset;
	}

	protected abstract T createModel();

	protected abstract String createName(T model, NameComputer computer);
}
