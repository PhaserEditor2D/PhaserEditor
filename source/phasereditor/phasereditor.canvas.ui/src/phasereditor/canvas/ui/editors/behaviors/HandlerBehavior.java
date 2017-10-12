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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.edithandlers.AnchorHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.AngleHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeHighlightCircleBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeHighlightRectBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeMoveBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeResizeCircleBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeResizeRectBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.Axis;
import phasereditor.canvas.ui.editors.edithandlers.IEditHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.PivotHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ScaleHandlerNode;
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

	public HandlerBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;
		_pane = _canvas.getHandlerPane();
	}

	public void editScale(IObjectNode object) {
		clear();

		for (Axis axis : new Axis[] { Axis.RIG, Axis.BOT, Axis.BOT_RIG }) {
			add(new ScaleHandlerNode(object, axis));
		}

		update();
	}

	public void editTile(IObjectNode object) {
		clear();

		for (Axis axis : Axis.values()) {
			if (axis == Axis.CENTER) {
				continue;
			}
			add(new TileHandlerNode(object, axis));
		}

		update();
	}

	public void editArcadeRectBody(ISpriteNode object) {
		clear();

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
		clear();

		add(new ArcadeHighlightCircleBodyHandlerNode(sprite));
		add(new ArcadeMoveBodyHandlerNode(sprite));
		add(new ArcadeResizeCircleBodyHandlerNode(sprite));

		update();
	}

	public void editAngle(IObjectNode object) {
		clear();

		add(new AngleHandlerNode(object, Axis.TOP_LEF));
		add(new AngleHandlerNode(object, Axis.TOP_RIG));
		add(new AngleHandlerNode(object, Axis.BOT_LEF));
		add(new AngleHandlerNode(object, Axis.BOT_RIG));

		if (object instanceof ISpriteNode) {
			ISpriteNode sprite = (ISpriteNode) object;
			add(new AnchorHandlerNode(sprite));
		} else {
			add(new PivotHandlerNode(object));
		}

		update();
	}

	public void editAnchor(ISpriteNode object) {
		clear();

		add(new AnchorHandlerNode(object));

		update();
	}

	public void editPivot(IObjectNode object) {
		clear();

		add(new PivotHandlerNode(object));

		update();
	}

	private void add(Node node) {
		_pane.getChildren().add(node);
	}

	public void clear() {
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
			}
		});
	}

	public boolean isEditing() {
		return !_pane.getChildren().isEmpty();
	}
}
