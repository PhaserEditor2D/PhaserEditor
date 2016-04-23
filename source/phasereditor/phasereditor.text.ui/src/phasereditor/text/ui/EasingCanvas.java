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
package phasereditor.text.ui;

import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import phasereditor.ui.animations.EasingInterpolator;

/**
 * @author arian
 *
 */
public class EasingCanvas extends FXCanvas {

	private static final int RADIUS = 15;
	private Circle _sprite;
	private TranslateTransition _anim;

	public EasingCanvas(Composite parent, int style) {
		super(parent, style);
		_sprite = new Circle(RADIUS);
		//_sprite.setFill(new Color(137f / 255, 181f / 255, 232f / 255, 1f));
		_sprite.setFill(Color.WHITE);
		_anim = new TranslateTransition(Duration.millis(1500), _sprite);
		_anim.setCycleCount(Animation.INDEFINITE);
		Pane pane = new Pane();
		pane.setStyle("-fx-background-color:#89b5e8");
		pane.getChildren().add(_sprite);
		setScene(new Scene(pane));
	}

	public void setEasing(Function<Double, Double> easing) {
		_anim.stop();
		_anim.setInterpolator(new EasingInterpolator(easing));

		Scene scene = getScene();

		_anim.setFromX(RADIUS + 50);
		_anim.setFromY(scene.getHeight() - RADIUS - 50);
		_anim.setToX(scene.getWidth() - RADIUS - 50);
		_anim.setToY(RADIUS + 50);

		_anim.play();
	}

	public void stop() {
		_anim.stop();
	}

}
