package phasereditor.canvas.ui;

import org.eclipse.core.expressions.PropertyTester;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class CanvasPropertyTester extends PropertyTester {

	public CanvasPropertyTester() {
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
		default:
			break;
		}
		return false;
	}

}
