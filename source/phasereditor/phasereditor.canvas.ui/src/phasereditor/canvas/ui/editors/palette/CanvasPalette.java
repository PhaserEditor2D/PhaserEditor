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
package phasereditor.canvas.ui.editors.palette;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;

import javafx.embed.swt.FXCanvas;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.TilePane;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.ImageAssetModel;

/**
 * @author arian
 *
 */
public class CanvasPalette extends FXCanvas {

	private TilePane _tilePane;

	private double _tileSize;

	public CanvasPalette(Composite parent, int style) {
		super(parent, style);

		_tileSize = 64;

		_tilePane = new TilePane(Orientation.HORIZONTAL);
		_tilePane.setHgap(5);
		_tilePane.setVgap(5);
		_tilePane.setPadding(new Insets(5));

		ScrollPane scroll = new ScrollPane(_tilePane);
		scroll.setFitToWidth(true);

		setScene(new Scene(scroll));

		initDrop();
	}

	private void initDrop() {
		getScene().setOnDragOver(event -> {
			try {
				ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null) {
					event.consume();
				} else {
					event.acceptTransferModes(TransferMode.ANY);
					CanvasPalette.this.setFocus();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		getScene().setOnDragDropped(event -> {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			dropAssets((IStructuredSelection) selection);
		});
	}

	private void dropAssets(IStructuredSelection selection) {
		for (Object elem : selection.toArray()) {
			addNode(elem);
		}
	}

	private void addNode(Object source) {
		IPaletteNode node = null;

		if (source instanceof ImageAssetModel) {
			node = new ImagePaletteNode((ImageAssetModel) source);
		} else if (source instanceof AtlasAssetModel.FrameItem) {
			node = new AtlasPaletteNode((FrameItem) source);
		}
		if (source instanceof AtlasAssetModel) {
			for (IAssetElementModel elem : ((AtlasAssetModel) source).getSubElements()) {
				addNode(elem);
			}
		}

		if (node != null) {
			node.configure(_tileSize);
			_tilePane.getChildren().add((Node) node);
		}
	}
}
