// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.animations.Easing;
import phasereditor.ui.info.GenericInformationControlCreator;

public class EasingProposalComputer extends BaseProposalComputer {

	private static final int RELEVANCE = 500;
	private static Image image;

	static {
		image = EditorSharedImages.getImage(IEditorSharedImages.IMG_EASING_ICON);
	}

	public static Object[][] EASING_TUPLES = {

			{ "Power0", "Phaser.Easing.Power0", (Function<Double, Double>) Easing::Linear },

			{ "Power1", "Phaser.Easing.Power1", (Function<Double, Double>) Easing::QuadraticOut },

			{ "Power2", "Phaser.Easing.Power2", (Function<Double, Double>) Easing::CubicOut },

			{ "Power3", "Phaser.Easing.Power3", (Function<Double, Double>) Easing::QuarticOut },

			{ "Power4", "Phaser.Easing.Power4", (Function<Double, Double>) Easing::QuinticOut },

			{ "Linear", "Phaser.Easing.Linear.None", (Function<Double, Double>) Easing::Linear }

			, { "Quad", "Phaser.Easing.Quadratic.Out", (Function<Double, Double>) Easing::QuadraticOut },

			{ "Cubic", "Phaser.Easing.Cubic.Out", (Function<Double, Double>) Easing::CubicOut },

			{ "Quart", "Phaser.Easing.Quartic.Out", (Function<Double, Double>) Easing::QuarticOut },

			{ "Quint", "Phaser.Easing.Quintic.Out", (Function<Double, Double>) Easing::QuinticOut },

			{ "Sine", "Phaser.Easing.Sinusoidal.Out", (Function<Double, Double>) Easing::SinusoidalOut },

			{ "Expo", "Phaser.Easing.Exponential.Out", (Function<Double, Double>) Easing::ExponentialOut },

			{ "Circ", "Phaser.Easing.Circular.Out", (Function<Double, Double>) Easing::CircularOut },

			{ "Elastic", "Phaser.Easing.Elastic.Out", (Function<Double, Double>) Easing::ElasticOut },

			{ "Back", "Phaser.Easing.Back.Out", (Function<Double, Double>) Easing::BackOut },

			{ "Bounce", "Phaser.Easing.Bounce.Out", (Function<Double, Double>) Easing::BounceOut },

			{ "Quad.easeIn", "Phaser.Easing.Quadratic.In", (Function<Double, Double>) Easing::QuadraticIn },

			{ "Cubic.easeIn", "Phaser.Easing.Cubic.In", (Function<Double, Double>) Easing::CubicIn },

			{ "Quart.easeIn", "Phaser.Easing.Quartic.In", (Function<Double, Double>) Easing::QuarticIn },

			{ "Quint.easeIn", "Phaser.Easing.Quintic.In", (Function<Double, Double>) Easing::QuinticIn },

			{ "Sine.easeIn", "Phaser.Easing.Sinusoidal.In", (Function<Double, Double>) Easing::SinusoidalIn },

			{ "Expo.easeIn", "Phaser.Easing.Exponential.In", (Function<Double, Double>) Easing::ExponentialIn },

			{ "Circ.easeIn", "Phaser.Easing.Circular.In", (Function<Double, Double>) Easing::CircularIn },

			{ "Elastic.easeIn", "Phaser.Easing.Elastic.In", (Function<Double, Double>) Easing::ElasticIn },

			{ "Back.easeIn", "Phaser.Easing.Back.In", (Function<Double, Double>) Easing::BackIn },

			{ "Bounce.easeIn", "Phaser.Easing.Bounce.In", (Function<Double, Double>) Easing::BounceIn },

			{ "Quad.easeOut", "Phaser.Easing.Quadratic.Out", (Function<Double, Double>) Easing::QuadraticOut },

			{ "Cubic.easeOut", "Phaser.Easing.Cubic.Out", (Function<Double, Double>) Easing::CubicOut },

			{ "Quart.easeOut", "Phaser.Easing.Quartic.Out", (Function<Double, Double>) Easing::QuarticOut },

			{ "Quint.easeOut", "Phaser.Easing.Quintic.Out", (Function<Double, Double>) Easing::QuinticOut },

			{ "Sine.easeOut", "Phaser.Easing.Sinusoidal.Out", (Function<Double, Double>) Easing::SinusoidalOut },

			{ "Expo.easeOut", "Phaser.Easing.Exponential.Out", (Function<Double, Double>) Easing::CircularOut },

			{ "Circ.easeOut", "Phaser.Easing.Circular.Out", (Function<Double, Double>) Easing::CircularOut },

			{ "Elastic.easeOut", "Phaser.Easing.Elastic.Out", (Function<Double, Double>) Easing::ElasticOut },

			{ "Back.easeOut", "Phaser.Easing.Back.Out", (Function<Double, Double>) Easing::BackOut },

			{ "Bounce.easeOut", "Phaser.Easing.Bounce.Out", (Function<Double, Double>) Easing::BounceOut },

			{ "Quad.easeInOut", "Phaser.Easing.Quadratic.InOut", (Function<Double, Double>) Easing::QuadraticInOut },

			{ "Cubic.easeInOut", "Phaser.Easing.Cubic.InOut", (Function<Double, Double>) Easing::CubicInOut },

			{ "Quart.easeInOut", "Phaser.Easing.Quartic.InOut", (Function<Double, Double>) Easing::QuadraticInOut },

			{ "Quint.easeInOut", "Phaser.Easing.Quintic.InOut", (Function<Double, Double>) Easing::QuinticInOut },

			{ "Sine.easeInOut", "Phaser.Easing.Sinusoidal.InOut", (Function<Double, Double>) Easing::SinusoidalInOut },

			{ "Expo.easeInOut", "Phaser.Easing.Exponential.InOut",
					(Function<Double, Double>) Easing::ExponentialInOut },

			{ "Circ.easeInOut", "Phaser.Easing.Circular.InOut", (Function<Double, Double>) Easing::CircularInOut },

			{ "Elastic.easeInOut", "Phaser.Easing.Elastic.InOut", (Function<Double, Double>) Easing::ElasticInOut },

			{ "Back.easeInOut", "Phaser.Easing.Back.InOut", (Function<Double, Double>) Easing::BackInOut },

			{ "Bounce.easeInOut", "Phaser.Easing.Bounce.InOut", (Function<Double, Double>) Easing::BounceInOut }

	};

	@Override
	protected List<ProposalData> computeProjectProposals(IProject project) {
		List<ProposalData> list = new ArrayList<>();

		for (Object[] easing : EASING_TUPLES) {
			String value = (String) easing[0];
			ProposalData propData = new ProposalData(easing, value, value, RELEVANCE);
			propData.setControlCreator(new GenericInformationControlCreator(EasingInformationControl.class,
					EasingInformationControl::new));
			propData.setImage(image);
			list.add(propData);
		}

		return list;
	}

}
