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
package phasereditor.canvas.ui.editors.grid;

import static java.lang.String.format;
import static java.lang.System.out;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

import javafx.embed.swt.FXCanvas;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;

/**
 * @author arian
 *
 */
public class PGridFrameDialog extends Dialog {
	/**
	 * 
	 */
	private static final String ITEM_TOKEN = "item";
	FXCanvas _canvas;
	private List<FrameData> _frames;
	private Image _image;
	private int _selectedframe;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public PGridFrameDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.SHELL_TRIM);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		_canvas = new FXCanvas(container, SWT.BORDER);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		afterCreateWidgets();

		return container;
	}

	@SuppressWarnings("boxing")
	private void afterCreateWidgets() {
		RGB rgb = SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION).getRGB();
		String commonStyle = "-fx-padding:5px;";
		String selectedStyle = commonStyle
				+ format("-fx-background-color:rgb(%s,%s,%s);", rgb.red, rgb.green, rgb.blue);

		TilePane tilePane = new TilePane(5, 5);

		_canvas.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Point size = _canvas.getSize();
				tilePane.setPrefSize(size.x - 40, size.y - 40);
			}
		});

		Point size = _canvas.getSize();
		tilePane.setPrefSize(size.x, size.y);

		for (FrameData frame : _frames) {
			Rectangle src = frame.src;

			ImageView imgView = new ImageView(_image);
			imgView.setMouseTransparent(true);
			imgView.setViewport(new Rectangle2D(src.x, src.y, src.width, src.height));

			BorderPane item = new BorderPane(imgView);
			item.setUserData(ITEM_TOKEN);
			item.setStyle(commonStyle);
			item.setPickOnBounds(true);
			item.setMouseTransparent(false);
			tilePane.getChildren().add(item);
		}

		tilePane.getChildren().get(_selectedframe).setStyle(selectedStyle);

		BorderPane bpane = new BorderPane(tilePane);
		ScrollPane pane = new ScrollPane(bpane);
		Scene scene = new Scene(pane);
		_canvas.setScene(scene);

		_canvas.getShell().setText("Sprite Sheet Frame");

		tilePane.setOnMouseClicked(event -> {
			Node picked = event.getPickResult().getIntersectedNode();
			out.println(picked);
			if (picked != null && picked.getUserData() == ITEM_TOKEN) {
				tilePane.getChildren().forEach(node -> {
					Pane item = (Pane) node;
					item.setStyle(commonStyle);
				});

				Pane item = (Pane) picked;
				item.setStyle(selectedStyle);
				_selectedframe = tilePane.getChildren().indexOf(item);
			}
		});
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(367, 287);
	}

	public void setFrames(List<FrameData> frames) {
		_frames = frames;
	}

	public void setImage(Image image) {
		_image = image;
	}

	public int getSelectedFrame() {
		return _selectedframe;
	}

	public void setSelectedframe(int selframe) {
		_selectedframe = selframe;
	}
}
