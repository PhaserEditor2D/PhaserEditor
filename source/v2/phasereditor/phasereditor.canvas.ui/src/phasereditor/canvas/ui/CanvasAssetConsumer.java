package phasereditor.canvas.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TreeViewer;

import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetConsumer;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.IAssetReplacer;

public class CanvasAssetConsumer implements IAssetConsumer {

	public CanvasAssetConsumer() {
	}
	
	@Override
	public void installTooltips(TreeViewer viewer) {
		CanvasUI.installCanvasTooltips(viewer);
	}
	
	@Override
	public FindAssetReferencesResult getAssetReferences(IAssetKey assetKey, IProgressMonitor monitor) {
		return CanvasUI.findAllKeyAssetReferences(assetKey, monitor);
	}

	@Override
	public IAssetReplacer getAssetReplacer() {
		return new CanvasAssetReplacer();
	}

}
