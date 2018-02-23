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

import org.eclipse.jface.util.IPropertyChangeListener;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.edithandlers.AnchorHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.AnchorShortcutsPane;
import phasereditor.canvas.ui.editors.edithandlers.AngleHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.AngleShortucsPane;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeBodyCircularShortcutsPane;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeBodyRectShortcutsPane;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeHighlightCircleBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeHighlightRectBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeMoveBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeResizeCircleBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ArcadeResizeRectBodyHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.Axis;
import phasereditor.canvas.ui.editors.edithandlers.IEditHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.MoveHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.MoveShortcutsPane;
import phasereditor.canvas.ui.editors.edithandlers.PivotHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.PivotShortcutPane;
import phasereditor.canvas.ui.editors.edithandlers.ScaleHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.ScaleShortcutsPane;
import phasereditor.canvas.ui.editors.edithandlers.ShortcutPane;
import phasereditor.canvas.ui.editors.edithandlers.TileHandlerNode;
import phasereditor.canvas.ui.editors.edithandlers.TileShortcutsPane;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class HandlerBehavior {
	private ObjectCanvas _canvas;
	private Pane _pane;
	private IPropertyChangeListener _prefsListener;

	public HandlerBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;
		_pane = _canvas.getHandlerPane();

		listenPreferences();
	}

	private void listenPreferences() {

		_prefsListener = e -> {
			switch (e.getProperty()) {

			case CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_POSITION:
			case CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_BG_COLOR:
			case CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_FG_COLOR:

				for (Node n : _pane.getChildren()) {
					if (n instanceof ShortcutPane) {
						ShortcutPane pane = (ShortcutPane) n;
						pane.updateFromPreferences();
					}
				}

				break;
			default:
				break;
			}
		};

		CanvasUI.getPreferenceStore().addPropertyChangeListener(_prefsListener);
		_canvas.addDisposeListener(e -> {
			CanvasUI.getPreferenceStore().removePropertyChangeListener(_prefsListener);
		});
	}

	public void editPosition(IObjectNode object) {
		clear();

		add(new MoveShortcutsPane(object));

		// add(new MoveAxisHandlerNode(object));

		add(new MoveHandlerNode(Axis.BOTTOM, object));
		add(new MoveHandlerNode(Axis.RIGHT, object));
		add(new MoveHandlerNode(Axis.CENTER, object));

		update();
	}

	public void editScale(IObjectNode object) {
		clear();

		add(new ScaleShortcutsPane(object));

		Arrays.stream(Axis.values()).filter(a -> a != Axis.CENTER)
				.forEach(axis -> add(new ScaleHandlerNode(object, axis)));

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

		add(new TileShortcutsPane(object));

		update();
	}

	public void editArcadeRectBody(ISpriteNode object) {
		clear();

		add(new ArcadeBodyRectShortcutsPane(object));

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

		add(new ArcadeBodyCircularShortcutsPane(sprite));

		add(new ArcadeHighlightCircleBodyHandlerNode(sprite));
		add(new ArcadeMoveBodyHandlerNode(sprite));
		add(new ArcadeResizeCircleBodyHandlerNode(sprite));

		update();
	}

	public void editAngle(IObjectNode object) {
		clear();

		add(new AngleShortucsPane(object));

		add(new AngleHandlerNode(object, 1));
		add(new AngleHandlerNode(object, 2));
		add(new AngleHandlerNode(object, 3));

		update();
	}

	public void editAnchor(ISpriteNode object) {
		clear();

		add(new AnchorShortcutsPane(object));
		add(new AnchorHandlerNode(object));

		update();
	}

	public void editPivot(IObjectNode object) {
		clear();

		add(new PivotShortcutPane(object));

		add(new PivotHandlerNode(object));

		update();
	}

	public void add(IEditHandlerNode node) {
		_pane.getChildren().add((Node) node);
	}

	public void remove(IEditHandlerNode node) {
		_pane.getChildren().remove((Node) node);
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

	public List<ShortcutPane> findShortcutPanes() {
		List<ShortcutPane> list = new ArrayList<>();

		for (Object n : _pane.getChildren()) {
			if (n instanceof ShortcutPane) {
				list.add((ShortcutPane) n);
			}
		}

		return list;
	}
}
