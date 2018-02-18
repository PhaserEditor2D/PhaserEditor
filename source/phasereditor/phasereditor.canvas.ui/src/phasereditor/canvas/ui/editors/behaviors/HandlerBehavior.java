// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors.behaviors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.edithandlers.AnchorHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.AnchorShortcutsPane;
import phasereditor.canvas.ui.editors.edithandlers.AngleHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.AngleShortucsPane;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeHighlightCircleBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeHighlightRectBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeMoveBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeResizeCircleBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeResizeRectBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.Axis;
import phasereditor.canvas.ui.editors.edithandlers.IEditHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.PivotHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.PivotShortcutPane;
import phasereditor.canvas.ui.editors.edithandlers.ScaleHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ScaleShortcutsPane;
import phasereditor.canvas.ui.editors.edithandlers.TileHandlerNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class HandlerBehavior {
	private ObjectCanvas _canvas;
	private Pane _pane;
	private String _lastCmd;
	private Object _lastObjId;

	public HandlerBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;
		_pane = _canvas.getHandlerPane();
	}

	public void editScale(IObjectNode object) {
		if (!prepareCommand("scale", object)) {
			return;
		}

		add(new ScaleShortcutsPane(object));

		Arrays.stream(Axis.values()).filter(a -> a != Axis.CENTER)
				.forEach(axis -> add(new ScaleHandlerNode(object, axis)));

		update();
	}

	public void editTile(IObjectNode object) {
		if (!prepareCommand("tile", object)) {
			return;
		}

		for (Axis axis : Axis.values()) {
			if (axis == Axis.CENTER) {
				continue;
			}
			add(new TileHandlerNode(object, axis));
		}

		update();
	}

	public void editArcadeRectBody(ISpriteNode object) {
		if (!prepareCommand("arcadeRectBody", object)) {
			return;
		}

		add(new ArcadeHighlightRectBodyHandlerNode(object));

		for (Axis axis : Axis.values()) {
			if (axis == Axis.CENTER) {
				continue;
			}

			add(new ArcadeResizeRectBodyHandlerNode(object, axis));
		}

		add(new ArcadeMoveBodyHandlerNode(object));

		update();
	}

	public void editArcadeCircleBody(ISpriteNode sprite) {
		if (!prepareCommand("arcadeCircleBody", sprite)) {
			return;
		}

		add(new ArcadeHighlightCircleBodyHandlerNode(sprite));
		add(new ArcadeMoveBodyHandlerNode(sprite));
		add(new ArcadeResizeCircleBodyHandlerNode(sprite));

		update();
	}

	public void editAngle(IObjectNode object) {
		if (!prepareCommand("angle", object)) {
			return;
		}

		add(new AngleShortucsPane(object));

		add(new AngleHandlerNode(object, 1));
		add(new AngleHandlerNode(object, 2));
		add(new AngleHandlerNode(object, 3));

		update();
	}

	public void editAnchor(ISpriteNode object) {
		if (!prepareCommand("anchor", object)) {
			return;
		}

		add(new AnchorShortcutsPane(object));
		add(new AnchorHandlerNode(object));

		update();
	}

	public void editPivot(IObjectNode object) {
		if (!prepareCommand("pivot", object)) {
			return;
		}

		add(new PivotShortcutPane(object));

		add(new PivotHandlerNode(object));

		update();
	}

	private void add(Node node) {
		_pane.getChildren().add(node);
	}

	public boolean prepareCommand(String cmd, IObjectNode node) {
		_pane.getChildren().clear();

		String id = node.getModel().getId();

		if (!id.equals(_lastObjId)) {
			_lastObjId = id;
			_lastCmd = cmd;
			return true;
		}

		boolean ok = _lastCmd == null || !cmd.equals(_lastCmd);

		_lastCmd = ok ? cmd : null;

		return ok;
	}

	public void clear() {
		_lastCmd = null;
		_lastObjId = null;
		_pane.getChildren().clear();
	}

	public void update() {

		List<Node> del = new ArrayList<>();

		_pane.getChildren().forEach(n -> {
			if (!((IEditHandlerNode) n).isValid()) {
				del.add(n);
			}
		});

		_pane.getChildren().removeAll(del);

		_pane.getChildren().forEach(n -> {
			((IEditHandlerNode) n).updateHandler();
		});
	}

	public void clearNotSelected() {
		@SuppressWarnings("unchecked")
		Set<IObjectNode> set = new HashSet<>(_canvas.getSelectionBehavior().getSelection().toList());
		new ArrayList<>(_pane.getChildren()).forEach(n -> {
			IObjectNode obj = ((IEditHandlerNode) n).getObject();
			if (!set.contains(obj)) {
				_pane.getChildren().remove(n);
				_lastCmd = null;
				_lastObjId = null;
			}
		});
	}

	public boolean isEditing() {
		return !_pane.getChildren().isEmpty();
	}
}
