package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.PGridEnumProperty;
import phasereditor.canvas.ui.editors.grid.editors.CanvasPGridEditingSupport;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.TextControl;
import phasereditor.canvas.ui.shapes.TextNode;
import phasereditor.ui.properties.PGridNumberProperty;

public class ChangeTextFontHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		Object[] selection = HandlerUtil.getCurrentStructuredSelection(event).toArray();

		FontDialog dlg = new FontDialog(HandlerUtil.getActiveShell(event));

		FontData data = new FontData();

		{
			TextNode node = (TextNode) selection[0];
			TextModel model = node.getModel();
			data.setName(model.getStyleFont());
			data.setHeight(model.getStyleFontSize());
			FontPosture fxStyle = model.getStyleFontStyle();
			int swtStyle = 0;
			if (fxStyle == FontPosture.ITALIC) {
				swtStyle |= SWT.ITALIC;
			}
			if (model.getStyleFontWeight() == FontWeight.BOLD) {
				swtStyle |= SWT.BOLD;

			}
			data.setStyle(swtStyle);
		}

		dlg.setFontList(new FontData[] { data });

		data = dlg.open();

		if (data != null) {
			CompositeOperation operations = new CompositeOperation();

			for (Object obj : selection) {
				TextControl control = ((TextNode) obj).getControl();

				PGridEnumProperty<String> propFontName = control.getFontNameProperty();
				PGridNumberProperty propFontSize = control.getFontSizeProperty();
				PGridEnumProperty<FontPosture> propFontStyle = control.getFontStyleProperty();
				Object fxPosture = (data.getStyle() & SWT.ITALIC) == SWT.ITALIC ? FontPosture.ITALIC
						: FontPosture.REGULAR;
				PGridEnumProperty<FontWeight> propFontWeight = control.getFontWeightProperty();
				FontWeight fxWeight = (data.getStyle() & SWT.BOLD) == SWT.BOLD ? FontWeight.BOLD : FontWeight.NORMAL;

				operations.add(CanvasPGridEditingSupport.makeChangePropertyValueOperation(data.getName(), propFontName));
				operations.add(CanvasPGridEditingSupport.makeChangePropertyValueOperation(Double.valueOf(data.getHeight()),
						propFontSize));
				operations.add(CanvasPGridEditingSupport.makeChangePropertyValueOperation(fxPosture, propFontStyle));
				operations.add(CanvasPGridEditingSupport.makeChangePropertyValueOperation(fxWeight, propFontWeight));
			}

			editor.getCanvas().getUpdateBehavior().executeOperations(operations);
		}

		return null;
	}

}
