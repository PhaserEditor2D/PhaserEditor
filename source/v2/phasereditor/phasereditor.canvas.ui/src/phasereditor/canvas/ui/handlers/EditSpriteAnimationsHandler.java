package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.ui.editors.grid.PGridAnimationsProperty;
import phasereditor.canvas.ui.editors.grid.editors.AnimationsDialog;
import phasereditor.canvas.ui.editors.grid.editors.PGridEditingSupport;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class EditSpriteAnimationsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		AnimationsDialog dlg = new AnimationsDialog(HandlerUtil.getActiveShell(event));
		ISpriteNode node = (ISpriteNode) HandlerUtil.getCurrentStructuredSelection(event).getFirstElement();

		if (!(node.getModel() instanceof AssetSpriteModel)) {
			MessageDialog.openInformation(null, "Animations", "Only sprite-sheet/atlas based objects are supported.");
			return null;
		}

		PGridAnimationsProperty prop = node.getControl().getAnimationsProperty();

		dlg.setLang(prop.getLang());
		
		List<AnimationModel> list = new ArrayList<>();

		for (AnimationModel model : prop.getValue()) {
			list.add(model.clone());
		}
		IAssetKey key = prop.getAssetKey();
		dlg.setAvailableFrames(key == null ? Collections.emptyList() : key.getAsset().getSubElements());
		dlg.setAnimations(list);

		if (dlg.open() == Window.OK) {
			PGridEditingSupport.executeChangePropertyValueOperation(dlg.getValue(), prop);
		}

		return null;
	}

}
