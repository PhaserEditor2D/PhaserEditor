package phasereditor.canvas.ui;

import org.eclipse.core.expressions.PropertyTester;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class CanvasObjectPropertyTester extends PropertyTester {

	public CanvasObjectPropertyTester() {
	}

	@SuppressWarnings("boxing")
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		switch (property) {
		case "isPrefabInstance":
			if (receiver instanceof IObjectNode) {
				BaseObjectModel model = ((IObjectNode) receiver).getModel();
				boolean value = model.isPrefabInstance();
				boolean expected = (boolean) expectedValue;
				return value == expected;
			}
			break;
		case "isGroupPrefabRoot":
			if (receiver instanceof GroupNode) {
				IObjectNode node = (IObjectNode) receiver;
				ObjectCanvas canvas = node.getControl().getCanvas();
				if (canvas.getEditor().getModel().getType() == CanvasType.GROUP) {
					if (node.getGroup() == canvas.getWorldNode()) {
						return true;
					}
				}
			}
			break;
		case "allowPick":
			if (receiver instanceof IObjectNode) {
				boolean editorPick = ((IObjectNode) receiver).getModel().isEditorPick();
				return editorPick == (boolean) expectedValue;
			}
			break;
		case "isClosedGroup":
			if (receiver instanceof GroupNode) {
				return ((GroupNode) receiver).getModel().isEditorClosed() == (boolean) expectedValue;
			}
			break;
		default:
			break;
		}
		return false;
	}

}
