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
package phasereditor.ui.animations;

import static java.lang.System.in;
import static java.lang.System.out;

import java.io.IOException;

import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.TimelineCallback;
import org.pushingpixels.trident.ease.Linear;

/**
 * @author arian
 *
 */
public class TestTrident {

	private float _hello;

	public float getHello() {
		return _hello;
	}

	public void setHello(float hello) {
		_hello = hello;
		out.println(hello);
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) throws IOException {
		Timeline tl = new Timeline(new TestTrident());
		tl.addCallback(new TimelineCallback() {

			@Override
			public void onTimelineStateChanged(TimelineState oldState, TimelineState newState, float durationFraction,
					float timelinePosition) {
				out.println(oldState + " -> " + newState);
			}

			@Override
			public void onTimelinePulse(float durationFraction, float timelinePosition) {
				out.println("durationFrac " + durationFraction + " timeline pos " + timelinePosition);
			}
		});
		tl.setEase(new Linear());
		tl.addPropertyToInterpolate("hello", 0f, 10f);
		tl.setDuration(1000);
		tl.play();
		in.read();
	}
}
