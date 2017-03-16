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
package phasereditor.canvas.ui.editors;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;

import javafx.embed.swt.FXCanvas;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.behaviors.CreateBehavior;
import phasereditor.canvas.ui.editors.behaviors.DragBehavior;
import phasereditor.canvas.ui.editors.behaviors.HandlerBehavior;
import phasereditor.canvas.ui.editors.behaviors.KeyboardBehavior;
import phasereditor.canvas.ui.editors.behaviors.MouseBehavior;
import phasereditor.canvas.ui.editors.behaviors.PaintBehavior;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;
import phasereditor.canvas.ui.editors.behaviors.ZoomBehavior;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.palette.PaletteComp;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;

/**
 * @author arian
 *
 */
public class ObjectCanvas extends FXCanvas {
	private CreateBehavior _createBehavior;
	private Pane _selectionPane;
	private SelectionBehavior _selectionBehavior;
	private DragBehavior _dragBehavior;
	private WorldModel _worldModel;
	private PGrid _pgrid;
	private UpdateBehavior _updateBehavior;
	private GroupControl _worldControl;
	private TreeViewer _outline;
	private StackPane _root;
	private ZoomBehavior _zoomBehavior;
	private Pane _selectionFrontPane;
	private MouseBehavior _mouseBehavior;
	private GridPane _backGridPane;
	private FrontGridPane _frontGridPane;
	private PaintBehavior _paintBehavior;
	private PaletteComp _palette;
	private CanvasEditor _editor;
	private EditorSettings _settingsModel;
	private Pane _handlerPane;
	private HandlerBehavior _handlerBehavior;
	private KeyboardBehavior _keyboardBehavior;

	public ObjectCanvas(Composite parent, int style) {
		super(parent, style);
	}

	public void init(CanvasEditor editor, CanvasModel model, PGrid grid, TreeViewer outline,
			PaletteComp palette) {
		_editor = editor;
		_settingsModel = model.getSettings();
		_worldModel = model.getWorld();
		_pgrid = grid;
		_outline = outline;
		_palette = palette;

		createScene();

		initDrop();

		_createBehavior = new CreateBehavior(this);
		_selectionBehavior = new SelectionBehavior(this);
		_dragBehavior = new DragBehavior(this);
		_updateBehavior = new UpdateBehavior(this, _pgrid, outline);
		_zoomBehavior = new ZoomBehavior(this);
		_mouseBehavior = new MouseBehavior(this);
		_keyboardBehavior = new KeyboardBehavior(this);
		_paintBehavior = new PaintBehavior(this);
		_handlerBehavior = new HandlerBehavior(this);

		_updateBehavior.updateFromSettings();
		_zoomBehavior.updateZoomAndPan();
		
		// just to force to select the default stuff in the property grid
		_selectionBehavior.setSelection(StructuredSelection.EMPTY);
	}

	public CanvasEditor getEditor() {
		return _editor;
	}

	public void selectAll() {
		_selectionBehavior.selectAll();
	}

	public PaintBehavior getPaintBehavior() {
		return _paintBehavior;
	}

	public PaletteComp getPalette() {
		return _palette;
	}

	public TreeViewer getOutline() {
		return _outline;
	}

	public PGrid getPGrid() {
		return _pgrid;
	}

	public EditorSettings getSettingsModel() {
		return _settingsModel;
	}

	public WorldModel getWorldModel() {
		return _worldModel;
	}

	public MouseBehavior getMouseBehavior() {
		return _mouseBehavior;
	}
	
	public KeyboardBehavior getKeyboardBehavior() {
		return _keyboardBehavior;
	}

	public CreateBehavior getCreateBehavior() {
		return _createBehavior;
	}

	public ZoomBehavior getZoomBehavior() {
		return _zoomBehavior;
	}

	public SelectionBehavior getSelectionBehavior() {
		return _selectionBehavior;
	}

	public DragBehavior getDragBehavior() {
		return _dragBehavior;
	}

	public UpdateBehavior getUpdateBehavior() {
		return _updateBehavior;
	}
	
	public HandlerBehavior getHandlerBehavior() {
		return _handlerBehavior;
	}

	private void initDrop() {
		getScene().setOnDragOver(event -> {
			try {
				ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null) {
					event.consume();
				} else {
					event.acceptTransferModes(TransferMode.ANY);
					ObjectCanvas.this.setFocus();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		getScene().setOnDragDropped(event -> {
			try {
				ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				_createBehavior.dropAssets((IStructuredSelection) selection, event);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	private void createScene() {
		_root = new StackPane();
		setScene(new Scene(_root));

		_backGridPane = new GridPane(this);
		_backGridPane.setId("__grid-pane__");

		_worldControl = new GroupControl(this, _worldModel);
		GroupNode world = _worldControl.getNode();

		_frontGridPane = new FrontGridPane(this);
		_frontGridPane.setId("__world-glass-pane__");

		_selectionPane = new Pane();
		_selectionPane.setId("__selection-pane__");

		
		_handlerPane = new Pane();
		_handlerPane.setId("__handler-pane__");
		
		
		_selectionFrontPane = new Pane();
		_selectionFrontPane.setId("__selection-glass-pane__");
		_selectionFrontPane.setMouseTransparent(true);

		_root.setAlignment(Pos.TOP_LEFT);

		_root.getChildren().setAll(_backGridPane, world, _frontGridPane, _selectionPane, _handlerPane, _selectionFrontPane);
	}

	public GridPane getBackGridPane() {
		return _backGridPane;
	}

	public FrontGridPane getFrontGridPane() {
		return _frontGridPane;
	}

	public Pane getRootPane() {
		return _root;
	}

	public GroupNode getWorldNode() {
		return _worldControl.getNode();
	}

	public Pane getSelectionPane() {
		return _selectionPane;
	}
	
	public Pane getHandlerPane() {
		return _handlerPane;
	}

	public Pane getSelectionFrontPane() {
		return _selectionFrontPane;
	}
	
	public void dropToCanvas(CompositeOperation operations, GroupNode parentNode,  BaseObjectControl<?> control, double sceneX, double sceneY) {
		// IObjectNode node = control.getIObjectNode();

		double invScale = 1 / _zoomBehavior.getScale();
		Point2D translate = _zoomBehavior.getTranslate();

		double x = (sceneX - translate.getX()) * invScale;
		double y = (sceneY - translate.getY()) * invScale;

		double w = control.getTextureWidth() / 2;
		double h = control.getTextureHeight() / 2;

		x -= w;
		y -= h;

		{
			// stepping
			EditorSettings settings = getSettingsModel();
			if (settings.isEnableStepping()) {
				x = Math.round(x / settings.getStepWidth()) * settings.getStepWidth();
				y = Math.round(y / settings.getStepHeight()) * settings.getStepHeight();
			}
		}

		BaseObjectModel model = control.getModel();

		operations.add(new AddNodeOperation(model.toJSON(false), -1, x, y, parentNode.getModel().getId()));

	}

	public void dirty() {
		getWorldModel().setDirty(true);
	}
}
