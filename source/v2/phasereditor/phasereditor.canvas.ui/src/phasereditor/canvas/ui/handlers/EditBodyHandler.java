package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.BodyModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.operations.ChangeBodyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.BaseSpriteControl;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class EditBodyHandler extends AbstractHandler {

	public static final String ARCADE_BODY_CIRCULAR = "Arcade Body - Circular";
	public static final String ARCADE_BODY_RECTANGULAR = "Arcade Body - Rectangular";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISpriteNode sprite = (ISpriteNode) ((IStructuredSelection) HandlerUtil.getCurrentSelection(event))
				.getFirstElement();

		if (!sprite.getModel().isOverriding(BaseSpriteModel.PROPSET_PHYSICS)) {
			MessageDialog.openWarning(HandlerUtil.getActiveShell(event), "Physics",
					"Cannot change the physics of this prefab instance.");
			return null;
		}

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ObjectCanvas canvas = editor.getCanvas();

		BodyModel body = sprite.getModel().getBody();

		if (body == null) {
			// create a new body

			MenuManager manager = new MenuManager();

			IEditorSite locator = canvas.getEditor().getEditorSite();
			manager.add(new CommandContributionItem(new CommandContributionItemParameter(locator, "1",
					"phasereditor.canvas.ui.arcadeRectBody", SWT.PUSH)));
			manager.add(new CommandContributionItem(new CommandContributionItemParameter(locator, "2",
					"phasereditor.canvas.ui.arcadeCircleBody", SWT.PUSH)));

			Menu menu = manager.createContextMenu(canvas);

			menu.setVisible(true);

		} else if (body instanceof RectArcadeBodyModel) {
			canvas.getHandlerBehavior().editArcadeRectBody(sprite);
		} else if (body instanceof CircleArcadeBodyModel) {
			canvas.getHandlerBehavior().editArcadeCircleBody(sprite);
		}
		return null;
	}

	public static void setNewBody(ISpriteNode sprite, String result) {

		BaseSpriteControl<?> control = sprite.getControl();
		ObjectCanvas canvas = control.getCanvas();

		BodyModel body;

		CompositeOperation operations = new CompositeOperation();

		SelectOperation select = new SelectOperation();

		if (result == ARCADE_BODY_CIRCULAR) {
			CircleArcadeBodyModel circle = new CircleArcadeBodyModel();
			circle.setRadius(Math.min(control.getTextureWidth() / 2, control.getTextureHeight() / 2));
			body = circle;
		} else {
			body = new RectArcadeBodyModel();
		}

		String id = control.getId();

		operations.add(new ChangeBodyOperation(id, body));

		select.add(id);

		operations.add(select);

		canvas.getUpdateBehavior().executeOperations(operations);

		if (body instanceof RectArcadeBodyModel) {
			canvas.getHandlerBehavior().editArcadeRectBody(sprite);
		} else if (body instanceof CircleArcadeBodyModel) {
			canvas.getHandlerBehavior().editArcadeCircleBody(sprite);
		}
	}

}
