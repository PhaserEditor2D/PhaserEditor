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

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class ArcadeCircleBodyHandlersGroup extends HandlersGroup {

	private ArcadeCircleBodyResizeHandler _resizeArcadeCircleBody_Radius;
	private Circle _resizeArcadeCircleBody_area;
	private ArcadeBodyMoveHandler _moveArcadeCircleBody;

	public ArcadeCircleBodyHandlersGroup(SelectionNode selnode) {
		super(selnode);

		_resizeArcadeCircleBody_Radius = new ArcadeCircleBodyResizeHandler(selnode);
		_resizeArcadeCircleBody_area = new Circle();
		_resizeArcadeCircleBody_area.setFill(Color.GREENYELLOW);
		_resizeArcadeCircleBody_area.setMouseTransparent(true);
		_resizeArcadeCircleBody_area.setOpacity(0.5);
		_moveArcadeCircleBody = new ArcadeBodyMoveHandler(selnode);

		getChildren().setAll(_resizeArcadeCircleBody_area, _moveArcadeCircleBody, _resizeArcadeCircleBody_Radius);
	}

	public void updateHandlers() {
		IObjectNode node = _selnode.getObjectNode();

		if (!(node instanceof ISpriteNode)) {
			return;
		}

		ISpriteNode sprite = (ISpriteNode) node;

		if (!(sprite.getModel().getBody() instanceof CircleArcadeBodyModel)) {
			return;
		}

		CircleArcadeBodyModel body = (CircleArcadeBodyModel) sprite.getModel().getBody();

		if (body == null) {
			return;
		}

		double bodyX = body.getOffsetX();
		double bodyY = body.getOffsetY();
		double bodyR = body.getRadius();

		{
			double scale = _selnode.getCanvas().getZoomBehavior().getScale();
			double scaleX = sprite.getModel().getScaleX();
			double scaleY = sprite.getModel().getScaleY();
			bodyX *= scale * scaleX;
			bodyY *= scale * scaleY;
			bodyR *= scale * scaleX;
		}

		double hs = SelectionNode.HANDLER_SIZE / 2;
		_moveArcadeCircleBody.relocate(bodyX + bodyR - hs, bodyY + bodyR - hs);
		_resizeArcadeCircleBody_Radius.relocate(bodyX + bodyR * 2 - hs, bodyY + bodyR - hs);
		_resizeArcadeCircleBody_area.setCenterX(bodyX + bodyR);
		_resizeArcadeCircleBody_area.setCenterY(bodyY + bodyR);
		_resizeArcadeCircleBody_area.setRadius(bodyR);
	}

}
