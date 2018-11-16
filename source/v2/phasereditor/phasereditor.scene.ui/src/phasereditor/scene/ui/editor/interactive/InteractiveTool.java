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

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.ui.editor.SceneCanvas;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.SceneObjectRenderer;

/**
 * @author arian
 *
 */
public abstract class InteractiveTool {

	private SceneEditor _editor;
	protected boolean _dragging;

	public InteractiveTool(SceneEditor editor) {
		super();
		_editor = editor;
	}

	protected abstract boolean canEdit(ObjectModel model);

	public boolean isDragging() {
		return _dragging;
	}

	public SceneEditor getEditor() {
		return _editor;
	}

	public SceneCanvas getScene() {
		return _editor.getScene();
	}

	public SceneObjectRenderer getRenderer() {
		return _editor.getScene().getSceneRenderer();
	}

	public List<ObjectModel> getModels() {

		var list = new ArrayList<ObjectModel>();

		for (var obj : getEditor().getScene().getSelection()) {
			if (obj instanceof ObjectModel && canEdit((ObjectModel) obj)) {
				list.add((ObjectModel) obj);
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

	protected static void fillArrow(GC gc, int globalX, int globalY, float globalAngle, int size, Color color) {
		var tx = new Transform(gc.getDevice());

		tx.translate(globalX, globalY);
		tx.rotate(globalAngle);
		tx.translate(0, -size / 2);

		gc.setTransform(tx);

		gc.setBackground(color);

		var points = new int[] { 0, 0, size, size / 2, 0, size };

		gc.fillPolygon(points);

		gc.setTransform(null);

		tx.dispose();

	}

	protected static void fillRect(GC gc, int globalX, int globalY, float globalAngle, int size, Color color) {

		var tx = new Transform(gc.getDevice());

		tx.translate(globalX, globalY);
		tx.rotate(globalAngle);

		gc.setTransform(tx);

		gc.setBackground(color);

		gc.fillRectangle(-size / 2, -size / 2, size, size);

		gc.setTransform(null);

		tx.dispose();
	}

	protected static void fillCircle(GC gc, int globalX, int globalY, int size, Color color) {
		var tx = new Transform(gc.getDevice());
		tx.translate(globalX, globalY);
		gc.setTransform(tx);

		gc.setBackground(color);

		gc.fillOval(-size / 2, -size / 2, size, size);

		gc.setTransform(null);
		tx.dispose();
	}

}
