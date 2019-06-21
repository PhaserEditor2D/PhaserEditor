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
package phasereditor.scene.ui.editor.outline;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.scene.core.TextComponent;
import phasereditor.scene.core.TextModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.ui.BaseImageTreeCanvasItemRenderer;
import phasereditor.ui.Colors;
import phasereditor.ui.FrameData;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class TextTreeItemRenderer extends BaseImageTreeCanvasItemRenderer {

	public TextTreeItemRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	protected void paintScaledInArea(GC gc, Rectangle area) {
		var model = (TextModel) _item.getData();
		var text = TextualComponent.get_text(model);
		var fgStr = TextComponent.get_color(model);
		var bgStr = TextComponent.get_backgroundColor(model);
		var fg = Colors.color(fgStr);
		var bg = Colors.color(bgStr);

		var size = gc.textExtent(text);

		try {
			var img = PhaserEditorUI.image_Swing_To_SWT(new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_ARGB));
			var g2 = new GC(img);
			
			if (fg == null) {
				fg = Colors.color(Colors.WHITE);
			}
			g2.setForeground(fg);
			
			if (bg != null) {
				g2.setBackground(bg);
				g2.fillRectangle(img.getBounds());
			}
			
			g2.drawText(text, 0, 0, true);
			g2.dispose();

			PhaserEditorUI.paintScaledImageInArea(gc, img, FrameData.fromImage(img), area);

			img.dispose();
		} catch (IOException e) {
			SceneUIEditor.logError(e);
		}

	}

	@Override
	public ImageProxy get_DND_Image() {
		// TODO Auto-generated method stub
		return null;
	}

}
