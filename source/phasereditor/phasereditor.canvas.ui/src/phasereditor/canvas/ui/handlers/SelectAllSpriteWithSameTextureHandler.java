package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.json.JSONObject;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.MissingAssetSpriteModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;
import phasereditor.canvas.ui.shapes.MissingAssetNode;

public class SelectAllSpriteWithSameTextureHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		GroupNode world = editor.getCanvas().getWorldNode();

		Object sel = HandlerUtil.getCurrentStructuredSelection(event).getFirstElement();

		List<Object> selection = new ArrayList<>();

		if (sel instanceof ISpriteNode) {
			ISpriteNode node = (ISpriteNode) sel;
			BaseSpriteModel model = node.getModel();
			if (model instanceof AssetSpriteModel) {
				IAssetKey assetKey = ((AssetSpriteModel<?>) model).getAssetKey();
				world.walkTree(node2 -> {
					BaseObjectModel model2 = node2.getModel();
					if (model2 instanceof AssetSpriteModel<?>) {
						IAssetKey assetKey2 = ((AssetSpriteModel<?>) model2).getAssetKey();
						if (assetKey.getSharedVersion().equals(assetKey2.getSharedVersion())) {
							selection.add(node2);
						}
					}
				}, true);
			}
		} else if (sel instanceof MissingAssetNode) {
			MissingAssetSpriteModel model = (MissingAssetSpriteModel) ((MissingAssetNode) sel).getModel();
			JSONObject assetRef = model.getSrcData().getJSONObject("asset-ref");
			world.walkTree(node2 -> {
				if (node2 instanceof MissingAssetNode) {
					MissingAssetSpriteModel model2 = (MissingAssetSpriteModel) node2.getModel();
					JSONObject assetRef2 = model2.getSrcData().getJSONObject("asset-ref");
					if (assetRef.toString().equals(assetRef2.toString())) {
						selection.add(node2);
					}
				}
			}, true);
		}

		editor.getCanvas().getSelectionBehavior().setSelection(new StructuredSelection(selection));

		return null;
	}

}
