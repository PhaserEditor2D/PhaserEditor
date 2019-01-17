// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.scene.ui.editor.properties;

import static phasereditor.ui.IEditorSharedImages.IMG_DEPTH_BOTTOM;
import static phasereditor.ui.IEditorSharedImages.IMG_DEPTH_DOWN;
import static phasereditor.ui.IEditorSharedImages.IMG_DEPTH_TOP;
import static phasereditor.ui.IEditorSharedImages.IMG_DEPTH_UP;

import org.eclipse.jface.action.Action;

import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.properties.OrderAction.OrderActionValue;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class JFaceOrderAction extends Action {
	private OrderAction _action;
	
	private static String[] ICON_MAP = {
			IMG_DEPTH_UP, IMG_DEPTH_DOWN, IMG_DEPTH_TOP, IMG_DEPTH_BOTTOM
	};

	public JFaceOrderAction(SceneEditor editor, OrderActionValue order) {
		setImageDescriptor(EditorSharedImages.getImageDescriptor(ICON_MAP[order.ordinal()]));
		_action = new OrderAction(editor, order);
		setText("Move objects to '" + order.name().toLowerCase() + "' in the parent's list.");
	}
	
	@Override
	public void run() {
		_action.run();
	}
}
