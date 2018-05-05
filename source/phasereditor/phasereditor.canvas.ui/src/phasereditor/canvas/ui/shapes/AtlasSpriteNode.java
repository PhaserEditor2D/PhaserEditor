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
package phasereditor.canvas.ui.shapes;

import javafx.scene.Node;
import phasereditor.canvas.core.AtlasSpriteModel;

/**
 * @author arian
 *
 */
public class AtlasSpriteNode extends FrameNode implements ISpriteNode {

	private AtlasSpriteControl _control;

	public AtlasSpriteNode(AtlasSpriteControl control) {
		super(control.getModel().getAssetKey(), control.isHeadless());
		_control = control;
	}

	@Override
	public AtlasSpriteModel getModel() {
		return _control.getModel();
	}

	@Override
	public AtlasSpriteControl getControl() {
		return _control;
	}

	@Override
	public Node getNode() {
		return this;
	}
}
