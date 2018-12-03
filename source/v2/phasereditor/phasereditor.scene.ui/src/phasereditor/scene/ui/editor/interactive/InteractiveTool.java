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
package phasereditor.scene.ui.editor.interactive;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.ui.SceneObjectRenderer;
import phasereditor.scene.ui.editor.SceneCanvas;
import phasereditor.scene.ui.editor.SceneEditor;

/**
 * @author arian
 *
 */
public abstract class InteractiveTool {

	private SceneEditor _editor;
	protected boolean _dragging;
	protected boolean _hightlights;

	public InteractiveTool(SceneEditor editor) {
		super();
		_editor = editor;
		_hightlights = false;
	}

	protected abstract boolean canEdit(ObjectModel model);

	public boolean isDragging() {
		return _dragging;
	}
	
	public boolean isHightlights() {
		return _hightlights;
	}

	public SceneEditor getEditor() {
		return _editor;
	}
	
	public AssetFinder getAssetFinder() {
		return getEditor().getScene().getAssetFinder();
	}

	public SceneCanvas getScene() {
		return _editor.getScene();
	}

	public SceneObjectRenderer getRenderer() {
		return _editor.getScene().getSceneRenderer();
	}

	public List<ObjectModel> getModels() {

		var list = new ArrayList<ObjectModel>();

		for (var model : getEditor().getSelectionList()) {
			if (canEdit(model)) {
				list.add(model);
			}
		}

		return list;
	}

	public abstract void render(GC gc);

	@SuppressWarnings("unused")
	public void mouseUp(MouseEvent e) {
		//
	}

	@SuppressWarnings("unused")
	public void mouseMove(MouseEvent e) {
		//

	}

	@SuppressWarnings("unused")
	public void mouseDown(MouseEvent e) {
		//
	}

	@SuppressWarnings({ "static-method", "unused" })
	public boolean contains(int sceneX, int sceneY) {
		return false;
	}

	protected boolean doPaint() {
		return !getScene().isInteractiveDragging();
	}

	protected static void drawLine(GC gc, float x1, float y1, float x2, float y2, Color color, Color darkColor) {
		gc.setLineWidth(3);
		gc.setForeground(darkColor);
		gc.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

		gc.setLineWidth(1);
		gc.setForeground(color);
		gc.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
	}

	protected static void drawArrow(GC gc, int x, int y, float globalAngle, int size, Color color, Color darkColor) {
		var tx = new Transform(gc.getDevice());

		tx.translate(x, y);
		tx.rotate(globalAngle);
		tx.translate(0, -size / 2);

		gc.setTransform(tx);

		var points = new int[] { -1, -1, size + 2, size / 2, -1, size + 2 };

		gc.setBackground(darkColor);
		gc.fillPolygon(points);

		points = new int[] { 0, 0, size, size / 2, 0, size };

		gc.setBackground(color);
		gc.fillPolygon(points);

		gc.setTransform(null);

		tx.dispose();

	}

	protected static void drawRect(GC gc, int x, int y, float globalAngle, int size, Color color, Color darkColor) {

		var tx = new Transform(gc.getDevice());

		tx.translate(x, y);
		tx.rotate(globalAngle);

		gc.setTransform(tx);

		gc.setBackground(darkColor);
		gc.fillRectangle(-size / 2 - 1, -size / 2 - 1, size + 2, size + 2);

		gc.setBackground(color);
		gc.fillRectangle(-size / 2, -size / 2, size, size);

		gc.setTransform(null);

		tx.dispose();
	}

	protected static void drawCircle(GC gc, int x, int y, int size, Color color, Color darkColor) {
		var tx = new Transform(gc.getDevice());
		tx.translate(x, y);
		gc.setTransform(tx);

		gc.setBackground(darkColor);
		gc.fillOval(-size / 2 - 1, -size / 2 - 1, size + 2, size + 2);

		gc.setBackground(color);
		gc.fillOval(-size / 2, -size / 2, size, size);

		gc.setTransform(null);
		tx.dispose();
	}

}
