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

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.info.TextInformationControlCreator;

public class EasingProposalComputer extends BaseProposalComputer {

	private static final int RELEVANCE = 500;
	private static Image image;

	static {
		image = EditorSharedImages.getImage(IEditorSharedImages.IMG_CHART_CURVE);
	}

	private static String[][] EASING_MAP = { { "Power0", "Phaser.Easing.Power0" }, { "Power1", "Phaser.Easing.Power1" },
			{ "Power2", "Phaser.Easing.Power2" }, { "Power3", "Phaser.Easing.Power3" },
			{ "Power4", "Phaser.Easing.Power4" },

			{ "Linear", "Phaser.Easing.Linear.None" }, { "Quad", "Phaser.Easing.Quadratic.Out" },
			{ "Cubic", "Phaser.Easing.Cubic.Out" }, { "Quart", "Phaser.Easing.Quartic.Out" },
			{ "Quint", "Phaser.Easing.Quintic.Out" }, { "Sine", "Phaser.Easing.Sinusoidal.Out" },
			{ "Expo", "Phaser.Easing.Exponential.Out" }, { "Circ", "Phaser.Easing.Circular.Out" },
			{ "Elastic", "Phaser.Easing.Elastic.Out" }, { "Back", "Phaser.Easing.Back.Out" },
			{ "Bounce", "Phaser.Easing.Bounce.Out" },

			{ "Quad.easeIn", "Phaser.Easing.Quadratic.In" }, { "Cubic.easeIn", "Phaser.Easing.Cubic.In" },
			{ "Quart.easeIn", "Phaser.Easing.Quartic.In" }, { "Quint.easeIn", "Phaser.Easing.Quintic.In" },
			{ "Sine.easeIn", "Phaser.Easing.Sinusoidal.In" }, { "Expo.easeIn", "Phaser.Easing.Exponential.In" },
			{ "Circ.easeIn", "Phaser.Easing.Circular.In" }, { "Elastic.easeIn", "Phaser.Easing.Elastic.In" },
			{ "Back.easeIn", "Phaser.Easing.Back.In" }, { "Bounce.easeIn", "Phaser.Easing.Bounce.In" },

			{ "Quad.easeOut", "Phaser.Easing.Quadratic.Out" }, { "Cubic.easeOut", "Phaser.Easing.Cubic.Out" },
			{ "Quart.easeOut", "Phaser.Easing.Quartic.Out" }, { "Quint.easeOut", "Phaser.Easing.Quintic.Out" },
			{ "Sine.easeOut", "Phaser.Easing.Sinusoidal.Out" }, { "Expo.easeOut", "Phaser.Easing.Exponential.Out" },
			{ "Circ.easeOut", "Phaser.Easing.Circular.Out" }, { "Elastic.easeOut", "Phaser.Easing.Elastic.Out" },
			{ "Back.easeOut", "Phaser.Easing.Back.Out" }, { "Bounce.easeOut", "Phaser.Easing.Bounce.Out" },

			{ "Quad.easeInOut", "Phaser.Easing.Quadratic.InOut" }, { "Cubic.easeInOut", "Phaser.Easing.Cubic.InOut" },
			{ "Quart.easeInOut", "Phaser.Easing.Quartic.InOut" }, { "Quint.easeInOut", "Phaser.Easing.Quintic.InOut" },
			{ "Sine.easeInOut", "Phaser.Easing.Sinusoidal.InOut" },
			{ "Expo.easeInOut", "Phaser.Easing.Exponential.InOut" },
			{ "Circ.easeInOut", "Phaser.Easing.Circular.InOut" },
			{ "Elastic.easeInOut", "Phaser.Easing.Elastic.InOut" }, { "Back.easeInOut", "Phaser.Easing.Back.InOut" },
			{ "Bounce.easeInOut", "Phaser.Easing.Bounce.InOut" } };

	@Override
	protected List<ProposalData> computeProjectProposals(IProject project) {
		List<ProposalData> list = new ArrayList<>();

		for (String[] easing : EASING_MAP) {
			String value = easing[0];
			String info = "Shortcut for " + easing[1];
			ProposalData propData = new ProposalData(easing, value, value, RELEVANCE);
			propData.setControlCreator(new TextInformationControlCreator(info));
			propData.setImage(image);
			list.add(propData);
		}

		return list;
	}

}
