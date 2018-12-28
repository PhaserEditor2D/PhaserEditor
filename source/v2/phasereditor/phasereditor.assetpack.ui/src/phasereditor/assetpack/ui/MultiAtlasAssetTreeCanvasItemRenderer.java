package phasereditor.assetpack.ui;

import static java.util.stream.Collectors.toList;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

public class MultiAtlasAssetTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	public MultiAtlasAssetTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {
		var asset = (MultiAtlasAssetModel) _item.getData();
		var frames = asset.getSubElements();
		var proxies = frames.stream()

				.map(f -> f.getImageFile())

				.distinct()

				.map(f -> ImageProxy.get(f, null))

				.filter(proxy -> proxy != null && proxy.getImage() != null)

				.collect(toList());

		var imageSize = _item.getCanvas().getImageSize();

		var i = 0;
		var x1 = x;

		while (i < proxies.size()) {
			var proxy = proxies.get(i);

			var rect = proxy.paintScaledInArea(e.gc, new Rectangle(x1, y, imageSize, imageSize), false);

			if (rect == null) {
				x1 += 2;
			} else {
				x1 += rect.width + 10;
			}

			i++;
		}

	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {
		return canvas.getImageSize();
	}

	@Override
	public ImageProxy get_DND_Image() {
		return null;
	}

}
