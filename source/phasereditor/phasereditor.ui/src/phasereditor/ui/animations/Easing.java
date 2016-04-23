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
package phasereditor.ui.animations;

/**
 * @author arian
 *
 */
@SuppressWarnings("all")
public class Easing {

	public static double Linear(double k) {
		return k;
	}

	public static double QuadraticIn(double k) {
		return k * k;
	}

	public static double QuadraticOut(double k) {
		return k * (2 - k);
	}

	public static double QuadraticInOut(double k) {
		if ((k *= 2) < 1)
			return 0.5 * k * k;
		return -0.5 * (--k * (k - 2) - 1);
	}

	public static double CubicIn(double k) {
		return k * k * k;
	}

	public static double CubicOut(double k) {
		return --k * k * k + 1;
	}

	public static double CubicInOut(double k) {
		if ((k *= 2) < 1)
			return 0.5 * k * k * k;
		return 0.5 * ((k -= 2) * k * k + 2);
	}

	public static double QuarticIn(double k) {
		return k * k * k * k;
	}
	
	public static double QuarticOut(double k) {
		return 1 - ( --k * k * k * k );
	}

	public static double QuarticInOut(double k) {
		if ((k *= 2) < 1)
			return 0.5 * k * k * k * k;
		return -0.5 * ((k -= 2) * k * k * k - 2);
	}

	public static double QuinticIn(double k) {
		return k * k * k * k * k;
	}

	public static double QuinticOut(double k) {
		return --k * k * k * k * k + 1;
	}

	public static double QuinticInOut(double k) {
		if ((k *= 2) < 1)
			return 0.5 * k * k * k * k * k;
		return 0.5 * ((k -= 2) * k * k * k * k + 2);
	}

	public static double SinusoidalIn(double k) {
		if (k == 0)
			return 0;
		if (k == 1)
			return 1;
		return 1 - Math.cos(k * Math.PI / 2);
	}

	public static double SinusoidalOut(double k) {
		if (k == 0)
			return 0;
		if (k == 1)
			return 1;
		return Math.sin(k * Math.PI / 2);
	}

	public static double SinusoidalInOut(double k) {
		if (k == 0)
			return 0;
		if (k == 1)
			return 1;
		return 0.5 * (1 - Math.cos(Math.PI * k));
	}

	public static double ExponentialIn(double k) {
		return k == 0 ? 0 : Math.pow(1024, k - 1);
	}

	public static double ExponentialOut(double k) {
		return k == 1 ? 1 : 1 - Math.pow(2, -10 * k);
	}

	public static double ExponentialInOut(double k) {
		if (k == 0)
			return 0;
		if (k == 1)
			return 1;
		if ((k *= 2) < 1)
			return 0.5 * Math.pow(1024, k - 1);
		return 0.5 * (-Math.pow(2, -10 * (k - 1)) + 2);
	}

	public static double CircularIn(double k) {
		return 1 - Math.sqrt(1 - k * k);
	}

	public static double CircularOut(double k) {
		return Math.sqrt(1 - (--k * k));
	}

	public static double CircularInOut(double k) {
		if ((k *= 2) < 1)
			return -0.5 * (Math.sqrt(1 - k * k) - 1);
		return 0.5 * (Math.sqrt(1 - (k -= 2) * k) + 1);
	}

	public static double ElasticIn(double k) {
		double s, a = 0.1, p = 0.4;
		if (k == 0)
			return 0;
		if (k == 1)
			return 1;
		if (a != 0 || a < 1) {
			a = 1;
			s = p / 4;
		} else
			s = p * Math.asin(1 / a) / (2 * Math.PI);
		return -(a * Math.pow(2, 10 * (k -= 1)) * Math.sin((k - s) * (2 * Math.PI) / p));
	}

	public static double ElasticOut(double k) {
		double s, a = 0.1, p = 0.4;
		if (k == 0)
			return 0;
		if (k == 1)
			return 1;
		if (a != 0 || a < 1) {
			a = 1;
			s = p / 4;
		} else
			s = p * Math.asin(1 / a) / (2 * Math.PI);
		return (a * Math.pow(2, -10 * k) * Math.sin((k - s) * (2 * Math.PI) / p) + 1);
	}

	public static double ElasticInOut(double k) {
		double s, a = 0.1, p = 0.4;
		if (k == 0)
			return 0;
		if (k == 1)
			return 1;
		if (a != 0 || a < 1) {
			a = 1;
			s = p / 4;
		} else
			s = p * Math.asin(1 / a) / (2 * Math.PI);
		if ((k *= 2) < 1)
			return -0.5 * (a * Math.pow(2, 10 * (k -= 1)) * Math.sin((k - s) * (2 * Math.PI) / p));
		return a * Math.pow(2, -10 * (k -= 1)) * Math.sin((k - s) * (2 * Math.PI) / p) * 0.5 + 1;
	}

	public static double BackIn(double k) {
		double s = 1.70158;
		return k * k * ((s + 1) * k - s);
	}

	public static double BackOut(double k) {
		double s = 1.70158;
		return --k * k * ((s + 1) * k + s) + 1;
	}

	public static double BackInOut(double k) {
		double s = 1.70158 * 1.525;
		if ((k *= 2) < 1)
			return 0.5 * (k * k * ((s + 1) * k - s));
		return 0.5 * ((k -= 2) * k * ((s + 1) * k + s) + 2);
	}

	public static double BounceIn(double k) {
		return 1 - BounceOut(1 - k);
	}

	public static double BounceOut(double k) {
		if (k < (1 / 2.75)) {

			return 7.5625 * k * k;

		} else if (k < (2 / 2.75)) {

			return 7.5625 * (k -= (1.5 / 2.75)) * k + 0.75;

		} else if (k < (2.5 / 2.75)) {

			return 7.5625 * (k -= (2.25 / 2.75)) * k + 0.9375;

		} else {

			return 7.5625 * (k -= (2.625 / 2.75)) * k + 0.984375;

		}
	}
	
	public static double BounceInOut(double k) {
		if ( k < 0.5 ) return BounceIn( k * 2 ) * 0.5;
        return BounceOut( k * 2 - 1 ) * 0.5 + 0.5;
	}
}
