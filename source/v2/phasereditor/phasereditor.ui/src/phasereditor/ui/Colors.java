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
package phasereditor.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;

/**
 * @author arian
 *
 */
@SuppressWarnings("all")
public class Colors {

	public static Color color(String webColor) {
		return SwtRM.getColor(rgb(webColor).rgb);
	}

	public static Color color(int r, int g, int b) {
		return SwtRM.getColor(r, g, b);
	}

	public static Color color(RGB rgb) {
		return SwtRM.getColor(rgb);
	}
	
	public static Color color(RGBA rgba) {
		return color(rgba.rgb);
	}

	public static RGBA rgb(String webColor) {
		return rgb(webColor, 1);
	}

	/**
	 * Creates an RGB color specified with an HTML or CSS attribute string.
	 *
	 * <p>
	 * This method supports the following formats:
	 * <ul>
	 * <li>Any standard HTML color name
	 * <li>An HTML long or short format hex string with an optional hex alpha
	 * channel. Hexadecimal values may be preceded by either {@code "0x"} or
	 * {@code "#"} and can either be 2 digits in the range {@code 00} to
	 * {@code 0xFF} or a single digit in the range {@code 0} to {@code F}.
	 * <li>An {@code rgb(r,g,b)} or {@code rgba(r,g,b,a)} format string. Each of the
	 * {@code r}, {@code g}, or {@code b} values can be an integer from 0 to 255 or
	 * a floating point percentage value from 0.0 to 100.0 followed by the percent
	 * ({@code %}) character. The alpha component, if present, is a floating point
	 * value from 0.0 to 1.0. Spaces are allowed before or after the numbers and
	 * between the percentage number and its percent sign ({@code %}).
	 * <li>An {@code hsl(h,s,l)} or {@code hsla(h,s,l,a)} format string. The
	 * {@code h} value is a floating point number from 0.0 to 360.0 representing the
	 * hue angle on a color wheel in degrees with {@code 0.0} or {@code 360.0}
	 * representing red, {@code 120.0} representing green, and {@code 240.0}
	 * representing blue. The {@code s} value is the saturation of the desired color
	 * represented as a floating point percentage from gray ({@code 0.0}) to the
	 * fully saturated color ({@code 100.0}) and the {@code l} value is the desired
	 * lightness or brightness of the desired color represented as a floating point
	 * percentage from black ({@code 0.0}) to the full brightness of the color
	 * ({@code 100.0}). The alpha component, if present, is a floating point value
	 * from 0.0 to 1.0. Spaces are allowed before or after the numbers and between
	 * the percentage number and its percent sign ({@code %}).
	 * </ul>
	 *
	 * <p>
	 * For formats without an alpha component and for named colors, opacity is set
	 * according to the {@code opacity} argument. For colors specified with an alpha
	 * component, the resulting opacity is a combination of the parsed alpha
	 * component and the {@code opacity} argument, so a transparent color becomes
	 * more transparent by specifying opacity.
	 * </p>
	 *
	 * <p>
	 * Examples:
	 * </p>
	 * <div class="classUseContainer">
	 * <table class="overviewSummary">
	 * <caption>Web Color Format Table</caption>
	 * <tr>
	 * <th scope="col" class="colFirst">Web Format String</th>
	 * <th scope="col" class="colLast">Equivalent constructor or factory call</th>
	 * </tr>
	 * <tr class="rowColor">
	 * <th scope="row" class="colFirst"><code>Color.web("orange", 0.5);</code></th>
	 * <td class="colLast"><code>new Color(1.0, 0xA5/255.0, 0.0, 0.5)</code></td>
	 * </tr>
	 * <tr class="altColor">
	 * <th scope="row" class=
	 * "colFirst"><code>Color.web("0xff66cc33", 0.5);</code></th>
	 * <td class="colLast"><code>new Color(1.0, 0.4, 0.8, 0.1)</code></td>
	 * </tr>
	 * <tr class="rowColor">
	 * <th scope="row" class=
	 * "colFirst"><code>Color.web("0xff66cc", 0.5);</code></th>
	 * <td class="colLast"><code>new Color(1.0, 0.4, 0.8, 0.5)</code></td>
	 * </tr>
	 * <tr class="altColor">
	 * <th scope="row" class="colFirst"><code>Color.web("#ff66cc", 0.5);</code></th>
	 * <td class="colLast"><code>new Color(1.0, 0.4, 0.8, 0.5)</code></td>
	 * </tr>
	 * <tr class="rowColor">
	 * <th scope="row" class="colFirst"><code>Color.web("#f68", 0.5);</code></th>
	 * <td class="colLast"><code>new Color(1.0, 0.4, 0.8, 0.5)</code></td>
	 * </tr>
	 * <tr class="altColor">
	 * <th scope="row" class=
	 * "colFirst"><code>Color.web("rgb(255,102,204)", 0.5);</code></th>
	 * <td class="colLast"><code>new Color(1.0, 0.4, 0.8, 0.5)</code></td>
	 * </tr>
	 * <tr class="rowColor">
	 * <th scope="row" class=
	 * "colFirst"><code>Color.web("rgb(100%,50%,50%)", 0.5);</code></th>
	 * <td class="colLast"><code>new Color(1.0, 0.5, 0.5, 0.5)</code></td>
	 * </tr>
	 * <tr class="altColor">
	 * <th scope="row" class=
	 * "colFirst"><code>Color.web("rgb(255,50%,50%,0.25)", 0.5);</code></th>
	 * <td class="colLast"><code>new Color(1.0, 0.5, 0.5, 0.125)</code></td>
	 * </tr>
	 * <tr class="rowColor">
	 * <th scope="row" class=
	 * "colFirst"><code>Color.web("hsl(240,100%,100%)", 0.5);</code></th>
	 * <td class="colLast"><code>Color.hsb(240.0, 1.0, 1.0, 0.5)</code></td>
	 * </tr>
	 * <tr class="altColor">
	 * <th scope="row" style="border-bottom:1px solid" class="colFirst">
	 * <code>Color.web("hsla(120,0%,0%,0.25)", 0.5);</code></th>
	 * <td style="border-bottom:1px solid" class="colLast">
	 * <code>Color.hsb(120.0, 0.0, 0.0, 0.125)</code></td>
	 * </tr>
	 * </table>
	 * </div>
	 *
	 * @param colorString
	 *            the name or numeric representation of the color in one of the
	 *            supported formats
	 * @param opacity
	 *            the opacity component in range from 0.0 (transparent) to 1.0
	 *            (opaque)
	 * @return the RGB color specified with the colorString
	 * @throws NullPointerException
	 *             if {@code colorString} is {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code colorString} specifies an unsupported color name or
	 *             contains an illegal numeric value
	 */
	public static RGBA rgb(String colorString, double opacity) {
		if (colorString == null) {
			throw new NullPointerException("The color components or name must be specified");
		}
		if (colorString.isEmpty()) {
			throw new IllegalArgumentException("Invalid color specification");
		}

		String color = colorString.toLowerCase(Locale.ROOT);

		if (color.startsWith("#")) {
			color = color.substring(1);
		} else if (color.startsWith("0x")) {
			color = color.substring(2);
		} else if (color.startsWith("rgb")) {
			if (color.startsWith("(", 3)) {
				return parseRGBColor(color, 4, false, opacity);
			} else if (color.startsWith("a(", 3)) {
				return parseRGBColor(color, 5, true, opacity);
			}
		} else if (color.startsWith("hsl")) {
			if (color.startsWith("(", 3)) {
				return parseHSLColor(color, 4, false, opacity);
			} else if (color.startsWith("a(", 3)) {
				return parseHSLColor(color, 5, true, opacity);
			}
		} else {
			RGBA col = NamedColors.get(color);
			if (col != null) {
				if (opacity == 1.0) {
					return col;
				} else {
					return new RGBA(col.rgb.red, col.rgb.green, col.rgb.blue, (int) (opacity * 255));
				}
			}
		}

		int len = color.length();

		try {
			int r;
			int g;
			int b;
			int a;

			if (len == 3) {
				r = Integer.parseInt(color.substring(0, 1), 16);
				g = Integer.parseInt(color.substring(1, 2), 16);
				b = Integer.parseInt(color.substring(2, 3), 16);
				return rgba((int) (r / 15.0), (int) (g / 15.0), (int) (b / 15.0), opacity);
			} else if (len == 4) {
				r = Integer.parseInt(color.substring(0, 1), 16);
				g = Integer.parseInt(color.substring(1, 2), 16);
				b = Integer.parseInt(color.substring(2, 3), 16);
				a = Integer.parseInt(color.substring(3, 4), 16);
				return rgba((int) (r / 15.0), (int) (g / 15.0), (int) (b / 15.0), (int) (opacity * a / 15.0));
			} else if (len == 6) {
				r = Integer.parseInt(color.substring(0, 2), 16);
				g = Integer.parseInt(color.substring(2, 4), 16);
				b = Integer.parseInt(color.substring(4, 6), 16);
				return rgba(r, g, b, opacity);
			} else if (len == 8) {
				r = Integer.parseInt(color.substring(0, 2), 16);
				g = Integer.parseInt(color.substring(2, 4), 16);
				b = Integer.parseInt(color.substring(4, 6), 16);
				a = Integer.parseInt(color.substring(6, 8), 16);
				return rgba(r, g, b, opacity * a / 255.0);
			}
		} catch (NumberFormatException nfe) {
			//
		}

		throw new IllegalArgumentException("Invalid color specification");
	}

	private static RGBA rgba(int r, int g, int b, double a) {
		return new RGBA(r, g, b, (int) (a * 255));
	}

	private static RGBA parseRGBColor(String color, int roff, boolean hasAlpha, double a) {
		try {
			int rend = color.indexOf(',', roff);
			int gend = rend < 0 ? -1 : color.indexOf(',', rend + 1);
			int bend = gend < 0 ? -1 : color.indexOf(hasAlpha ? ',' : ')', gend + 1);
			int aend = hasAlpha ? (bend < 0 ? -1 : color.indexOf(')', bend + 1)) : bend;
			if (aend >= 0) {
				double r = parseComponent(color, roff, rend, PARSE_COMPONENT);
				double g = parseComponent(color, rend + 1, gend, PARSE_COMPONENT);
				double b = parseComponent(color, gend + 1, bend, PARSE_COMPONENT);
				if (hasAlpha) {
					a *= parseComponent(color, bend + 1, aend, PARSE_ALPHA);
				}
				return rgba((int) (r * 255), (int) (g * 255), (int) (b * 255), a);
			}
		} catch (NumberFormatException nfe) {
			//
		}

		throw new IllegalArgumentException("Invalid color specification");
	}

	private static RGBA parseHSLColor(String color, int hoff, boolean hasAlpha, double a) {
		try {
			int hend = color.indexOf(',', hoff);
			int send = hend < 0 ? -1 : color.indexOf(',', hend + 1);
			int lend = send < 0 ? -1 : color.indexOf(hasAlpha ? ',' : ')', send + 1);
			int aend = hasAlpha ? (lend < 0 ? -1 : color.indexOf(')', lend + 1)) : lend;
			if (aend >= 0) {
				double h = parseComponent(color, hoff, hend, PARSE_ANGLE);
				double s = parseComponent(color, hend + 1, send, PARSE_PERCENT);
				double l = parseComponent(color, send + 1, lend, PARSE_PERCENT);
				if (hasAlpha) {
					a *= parseComponent(color, lend + 1, aend, PARSE_ALPHA);
				}
				return hsb(h, s, l, a);
			}
		} catch (NumberFormatException nfe) {
			//
		}

		throw new IllegalArgumentException("Invalid color specification");
	}

	public static RGBA hsb(double h, double s, double l, double a) {
		return new RGBA((float) h, (float) s, (float) l, (float) a);
	}

	private static final int PARSE_COMPONENT = 0; // percent, or clamped to [0,255] => [0,1]
	private static final int PARSE_PERCENT = 1; // clamped to [0,100]% => [0,1]
	private static final int PARSE_ANGLE = 2; // clamped to [0,360]
	private static final int PARSE_ALPHA = 3; // clamped to [0.0,1.0]

	private static double parseComponent(String color, int off, int end, int type) {
		color = color.substring(off, end).trim();
		if (color.endsWith("%")) {
			if (type > PARSE_PERCENT) {
				throw new IllegalArgumentException("Invalid color specification");
			}
			type = PARSE_PERCENT;
			color = color.substring(0, color.length() - 1).trim();
		} else if (type == PARSE_PERCENT) {
			throw new IllegalArgumentException("Invalid color specification");
		}
		double c = ((type == PARSE_COMPONENT) ? Integer.parseInt(color) : Double.parseDouble(color));
		switch (type) {
		case PARSE_ALPHA:
			return (c < 0.0) ? 0.0 : ((c > 1.0) ? 1.0 : c);
		case PARSE_PERCENT:
			return (c <= 0.0) ? 0.0 : ((c >= 100.0) ? 1.0 : (c / 100.0));
		case PARSE_COMPONENT:
			return (c <= 0.0) ? 0.0 : ((c >= 255.0) ? 1.0 : (c / 255.0));
		case PARSE_ANGLE:
			return ((c < 0.0) ? ((c % 360.0) + 360.0) : ((c > 360.0) ? (c % 360.0) : c));
		}

		throw new IllegalArgumentException("Invalid color specification");
	}

	private static final class NamedColors {
		private static final Map<String, RGBA> namedColors = createNamedColors();

		private NamedColors() {
		}

		private static RGBA get(String name) {
			return namedColors.get(name);
		}

		private static Map<String, RGBA> createNamedColors() {
			Map<String, RGBA> colors = new HashMap<>(256);

			colors.put("aliceblue", ALICEBLUE);
			colors.put("antiquewhite", ANTIQUEWHITE);
			colors.put("aqua", AQUA);
			colors.put("aquamarine", AQUAMARINE);
			colors.put("azure", AZURE);
			colors.put("beige", BEIGE);
			colors.put("bisque", BISQUE);
			colors.put("black", BLACK);
			colors.put("blanchedalmond", BLANCHEDALMOND);
			colors.put("blue", BLUE);
			colors.put("blueviolet", BLUEVIOLET);
			colors.put("brown", BROWN);
			colors.put("burlywood", BURLYWOOD);
			colors.put("cadetblue", CADETBLUE);
			colors.put("chartreuse", CHARTREUSE);
			colors.put("chocolate", CHOCOLATE);
			colors.put("coral", CORAL);
			colors.put("cornflowerblue", CORNFLOWERBLUE);
			colors.put("cornsilk", CORNSILK);
			colors.put("crimson", CRIMSON);
			colors.put("cyan", CYAN);
			colors.put("darkblue", DARKBLUE);
			colors.put("darkcyan", DARKCYAN);
			colors.put("darkgoldenrod", DARKGOLDENROD);
			colors.put("darkgray", DARKGRAY);
			colors.put("darkgreen", DARKGREEN);
			colors.put("darkgrey", DARKGREY);
			colors.put("darkkhaki", DARKKHAKI);
			colors.put("darkmagenta", DARKMAGENTA);
			colors.put("darkolivegreen", DARKOLIVEGREEN);
			colors.put("darkorange", DARKORANGE);
			colors.put("darkorchid", DARKORCHID);
			colors.put("darkred", DARKRED);
			colors.put("darksalmon", DARKSALMON);
			colors.put("darkseagreen", DARKSEAGREEN);
			colors.put("darkslateblue", DARKSLATEBLUE);
			colors.put("darkslategray", DARKSLATEGRAY);
			colors.put("darkslategrey", DARKSLATEGREY);
			colors.put("darkturquoise", DARKTURQUOISE);
			colors.put("darkviolet", DARKVIOLET);
			colors.put("deeppink", DEEPPINK);
			colors.put("deepskyblue", DEEPSKYBLUE);
			colors.put("dimgray", DIMGRAY);
			colors.put("dimgrey", DIMGREY);
			colors.put("dodgerblue", DODGERBLUE);
			colors.put("firebrick", FIREBRICK);
			colors.put("floralwhite", FLORALWHITE);
			colors.put("forestgreen", FORESTGREEN);
			colors.put("fuchsia", FUCHSIA);
			colors.put("gainsboro", GAINSBORO);
			colors.put("ghostwhite", GHOSTWHITE);
			colors.put("gold", GOLD);
			colors.put("goldenrod", GOLDENROD);
			colors.put("gray", GRAY);
			colors.put("green", GREEN);
			colors.put("greenyellow", GREENYELLOW);
			colors.put("grey", GREY);
			colors.put("honeydew", HONEYDEW);
			colors.put("hotpink", HOTPINK);
			colors.put("indianred", INDIANRED);
			colors.put("indigo", INDIGO);
			colors.put("ivory", IVORY);
			colors.put("khaki", KHAKI);
			colors.put("lavender", LAVENDER);
			colors.put("lavenderblush", LAVENDERBLUSH);
			colors.put("lawngreen", LAWNGREEN);
			colors.put("lemonchiffon", LEMONCHIFFON);
			colors.put("lightblue", LIGHTBLUE);
			colors.put("lightcoral", LIGHTCORAL);
			colors.put("lightcyan", LIGHTCYAN);
			colors.put("lightgoldenrodyellow", LIGHTGOLDENRODYELLOW);
			colors.put("lightgray", LIGHTGRAY);
			colors.put("lightgreen", LIGHTGREEN);
			colors.put("lightgrey", LIGHTGREY);
			colors.put("lightpink", LIGHTPINK);
			colors.put("lightsalmon", LIGHTSALMON);
			colors.put("lightseagreen", LIGHTSEAGREEN);
			colors.put("lightskyblue", LIGHTSKYBLUE);
			colors.put("lightslategray", LIGHTSLATEGRAY);
			colors.put("lightslategrey", LIGHTSLATEGREY);
			colors.put("lightsteelblue", LIGHTSTEELBLUE);
			colors.put("lightyellow", LIGHTYELLOW);
			colors.put("lime", LIME);
			colors.put("limegreen", LIMEGREEN);
			colors.put("linen", LINEN);
			colors.put("magenta", MAGENTA);
			colors.put("maroon", MAROON);
			colors.put("mediumaquamarine", MEDIUMAQUAMARINE);
			colors.put("mediumblue", MEDIUMBLUE);
			colors.put("mediumorchid", MEDIUMORCHID);
			colors.put("mediumpurple", MEDIUMPURPLE);
			colors.put("mediumseagreen", MEDIUMSEAGREEN);
			colors.put("mediumslateblue", MEDIUMSLATEBLUE);
			colors.put("mediumspringgreen", MEDIUMSPRINGGREEN);
			colors.put("mediumturquoise", MEDIUMTURQUOISE);
			colors.put("mediumvioletred", MEDIUMVIOLETRED);
			colors.put("midnightblue", MIDNIGHTBLUE);
			colors.put("mintcream", MINTCREAM);
			colors.put("mistyrose", MISTYROSE);
			colors.put("moccasin", MOCCASIN);
			colors.put("navajowhite", NAVAJOWHITE);
			colors.put("navy", NAVY);
			colors.put("oldlace", OLDLACE);
			colors.put("olive", OLIVE);
			colors.put("olivedrab", OLIVEDRAB);
			colors.put("orange", ORANGE);
			colors.put("orangered", ORANGERED);
			colors.put("orchid", ORCHID);
			colors.put("palegoldenrod", PALEGOLDENROD);
			colors.put("palegreen", PALEGREEN);
			colors.put("paleturquoise", PALETURQUOISE);
			colors.put("palevioletred", PALEVIOLETRED);
			colors.put("papayawhip", PAPAYAWHIP);
			colors.put("peachpuff", PEACHPUFF);
			colors.put("peru", PERU);
			colors.put("pink", PINK);
			colors.put("plum", PLUM);
			colors.put("powderblue", POWDERBLUE);
			colors.put("purple", PURPLE);
			colors.put("red", RED);
			colors.put("rosybrown", ROSYBROWN);
			colors.put("royalblue", ROYALBLUE);
			colors.put("saddlebrown", SADDLEBROWN);
			colors.put("salmon", SALMON);
			colors.put("sandybrown", SANDYBROWN);
			colors.put("seagreen", SEAGREEN);
			colors.put("seashell", SEASHELL);
			colors.put("sienna", SIENNA);
			colors.put("silver", SILVER);
			colors.put("skyblue", SKYBLUE);
			colors.put("slateblue", SLATEBLUE);
			colors.put("slategray", SLATEGRAY);
			colors.put("slategrey", SLATEGREY);
			colors.put("snow", SNOW);
			colors.put("springgreen", SPRINGGREEN);
			colors.put("steelblue", STEELBLUE);
			colors.put("tan", TAN);
			colors.put("teal", TEAL);
			colors.put("thistle", THISTLE);
			colors.put("tomato", TOMATO);
			colors.put("transparent", TRANSPARENT);
			colors.put("turquoise", TURQUOISE);
			colors.put("violet", VIOLET);
			colors.put("wheat", WHEAT);
			colors.put("white", WHITE);
			colors.put("whitesmoke", WHITESMOKE);
			colors.put("yellow", YELLOW);
			colors.put("yellowgreen", YELLOWGREEN);

			return colors;
		}
	}

	private static RGBA rgb(float r, float g, float b) {
		return new RGBA((int) (r * 255), (int) (g * 255), (int) (b * 255), 255);
	}

	private static RGBA rgb(float r, float g, float b, float a) {
		return new RGBA((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
	}

	/**
	 * A fully transparent color with an ARGB value of #00000000.
	 */
	public static final RGBA TRANSPARENT = new RGBA(0, 0, 0, 0);

	/**
	 * The color alice blue with an RGB value of #F0F8FF
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F0F8FF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA ALICEBLUE = rgb(0.9411765f, 0.972549f, 1.0f);

	/**
	 * The color antique white with an RGB value of #FAEBD7
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FAEBD7;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA ANTIQUEWHITE = rgb(0.98039216f, 0.92156863f, 0.84313726f);

	/**
	 * The color aqua with an RGB value of #00FFFF <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA AQUA = rgb(0.0f, 1.0f, 1.0f);

	/**
	 * The color aquamarine with an RGB value of #7FFFD4
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#7FFFD4;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA AQUAMARINE = rgb(0.49803922f, 1.0f, 0.83137256f);

	/**
	 * The color azure with an RGB value of #F0FFFF <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F0FFFF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA AZURE = rgb(0.9411765f, 1.0f, 1.0f);

	/**
	 * The color beige with an RGB value of #F5F5DC <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F5F5DC;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA BEIGE = rgb(0.9607843f, 0.9607843f, 0.8627451f);

	/**
	 * The color bisque with an RGB value of #FFE4C4 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFE4C4;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA BISQUE = rgb(1.0f, 0.89411765f, 0.76862746f);

	/**
	 * The color black with an RGB value of #000000 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#000000;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA BLACK = rgb(0.0f, 0.0f, 0.0f);

	/**
	 * The color blanched almond with an RGB value of #FFEBCD
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFEBCD;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA BLANCHEDALMOND = rgb(1.0f, 0.92156863f, 0.8039216f);

	/**
	 * The color blue with an RGB value of #0000FF <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#0000FF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA BLUE = rgb(0.0f, 0.0f, 1.0f);

	/**
	 * The color blue violet with an RGB value of #8A2BE2
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#8A2BE2;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA BLUEVIOLET = rgb(0.5411765f, 0.16862746f, 0.8862745f);

	/**
	 * The color brown with an RGB value of #A52A2A <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#A52A2A;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA BROWN = rgb(0.64705884f, 0.16470589f, 0.16470589f);

	/**
	 * The color burly wood with an RGB value of #DEB887
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#DEB887;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA BURLYWOOD = rgb(0.87058824f, 0.72156864f, 0.5294118f);

	/**
	 * The color cadet blue with an RGB value of #5F9EA0
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#5F9EA0;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA CADETBLUE = rgb(0.37254903f, 0.61960787f, 0.627451f);

	/**
	 * The color chartreuse with an RGB value of #7FFF00
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#7FFF00;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA CHARTREUSE = rgb(0.49803922f, 1.0f, 0.0f);

	/**
	 * The color chocolate with an RGB value of #D2691E <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#D2691E;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA CHOCOLATE = rgb(0.8235294f, 0.4117647f, 0.11764706f);

	/**
	 * The color coral with an RGB value of #FF7F50 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF7F50;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA CORAL = rgb(1.0f, 0.49803922f, 0.3137255f);

	/**
	 * The color cornflower blue with an RGB value of #6495ED
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#6495ED;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA CORNFLOWERBLUE = rgb(0.39215687f, 0.58431375f, 0.92941177f);

	/**
	 * The color cornsilk with an RGB value of #FFF8DC <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFF8DC;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA CORNSILK = rgb(1.0f, 0.972549f, 0.8627451f);

	/**
	 * The color crimson with an RGB value of #DC143C <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#DC143C;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA CRIMSON = rgb(0.8627451f, 0.078431375f, 0.23529412f);

	/**
	 * The color cyan with an RGB value of #00FFFF <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA CYAN = rgb(0.0f, 1.0f, 1.0f);

	/**
	 * The color dark blue with an RGB value of #00008B <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#00008B;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKBLUE = rgb(0.0f, 0.0f, 0.54509807f);

	/**
	 * The color dark cyan with an RGB value of #008B8B <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#008B8B;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKCYAN = rgb(0.0f, 0.54509807f, 0.54509807f);

	/**
	 * The color dark goldenrod with an RGB value of #B8860B
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#B8860B;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKGOLDENROD = rgb(0.72156864f, 0.5254902f, 0.043137256f);

	/**
	 * The color dark gray with an RGB value of #A9A9A9 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKGRAY = rgb(0.6627451f, 0.6627451f, 0.6627451f);

	/**
	 * The color dark green with an RGB value of #006400
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#006400;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKGREEN = rgb(0.0f, 0.39215687f, 0.0f);

	/**
	 * The color dark grey with an RGB value of #A9A9A9 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKGREY = DARKGRAY;

	/**
	 * The color dark khaki with an RGB value of #BDB76B
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#BDB76B;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKKHAKI = rgb(0.7411765f, 0.7176471f, 0.41960785f);

	/**
	 * The color dark magenta with an RGB value of #8B008B
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#8B008B;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKMAGENTA = rgb(0.54509807f, 0.0f, 0.54509807f);

	/**
	 * The color dark olive green with an RGB value of #556B2F
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#556B2F;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKOLIVEGREEN = rgb(0.33333334f, 0.41960785f, 0.18431373f);

	/**
	 * The color dark orange with an RGB value of #FF8C00
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF8C00;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKORANGE = rgb(1.0f, 0.54901963f, 0.0f);

	/**
	 * The color dark orchid with an RGB value of #9932CC
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#9932CC;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKORCHID = rgb(0.6f, 0.19607843f, 0.8f);

	/**
	 * The color dark red with an RGB value of #8B0000 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#8B0000;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKRED = rgb(0.54509807f, 0.0f, 0.0f);

	/**
	 * The color dark salmon with an RGB value of #E9967A
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#E9967A;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKSALMON = rgb(0.9137255f, 0.5882353f, 0.47843137f);

	/**
	 * The color dark sea green with an RGB value of #8FBC8F
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#8FBC8F;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKSEAGREEN = rgb(0.56078434f, 0.7372549f, 0.56078434f);

	/**
	 * The color dark slate blue with an RGB value of #483D8B
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#483D8B;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKSLATEBLUE = rgb(0.28235295f, 0.23921569f, 0.54509807f);

	/**
	 * The color dark slate gray with an RGB value of #2F4F4F
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKSLATEGRAY = rgb(0.18431373f, 0.30980393f, 0.30980393f);

	/**
	 * The color dark slate grey with an RGB value of #2F4F4F
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKSLATEGREY = DARKSLATEGRAY;

	/**
	 * The color dark turquoise with an RGB value of #00CED1
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#00CED1;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKTURQUOISE = rgb(0.0f, 0.80784315f, 0.81960785f);

	/**
	 * The color dark violet with an RGB value of #9400D3
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#9400D3;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DARKVIOLET = rgb(0.5803922f, 0.0f, 0.827451f);

	/**
	 * The color deep pink with an RGB value of #FF1493 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF1493;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DEEPPINK = rgb(1.0f, 0.078431375f, 0.5764706f);

	/**
	 * The color deep sky blue with an RGB value of #00BFFF
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#00BFFF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DEEPSKYBLUE = rgb(0.0f, 0.7490196f, 1.0f);

	/**
	 * The color dim gray with an RGB value of #696969 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#696969;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DIMGRAY = rgb(0.4117647f, 0.4117647f, 0.4117647f);

	/**
	 * The color dim grey with an RGB value of #696969 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#696969;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DIMGREY = DIMGRAY;

	/**
	 * The color dodger blue with an RGB value of #1E90FF
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#1E90FF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA DODGERBLUE = rgb(0.11764706f, 0.5647059f, 1.0f);

	/**
	 * The color firebrick with an RGB value of #B22222 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#B22222;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA FIREBRICK = rgb(0.69803923f, 0.13333334f, 0.13333334f);

	/**
	 * The color floral white with an RGB value of #FFFAF0
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFFAF0;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA FLORALWHITE = rgb(1.0f, 0.98039216f, 0.9411765f);

	/**
	 * The color forest green with an RGB value of #228B22
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#228B22;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA FORESTGREEN = rgb(0.13333334f, 0.54509807f, 0.13333334f);

	/**
	 * The color fuchsia with an RGB value of #FF00FF <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA FUCHSIA = rgb(1.0f, 0.0f, 1.0f);

	/**
	 * The color gainsboro with an RGB value of #DCDCDC <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#DCDCDC;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA GAINSBORO = rgb(0.8627451f, 0.8627451f, 0.8627451f);

	/**
	 * The color ghost white with an RGB value of #F8F8FF
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F8F8FF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA GHOSTWHITE = rgb(0.972549f, 0.972549f, 1.0f);

	/**
	 * The color gold with an RGB value of #FFD700 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFD700;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA GOLD = rgb(1.0f, 0.84313726f, 0.0f);

	/**
	 * The color goldenrod with an RGB value of #DAA520 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#DAA520;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA GOLDENROD = rgb(0.85490197f, 0.64705884f, 0.1254902f);

	/**
	 * The color gray with an RGB value of #808080 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#808080;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA GRAY = rgb(0.5019608f, 0.5019608f, 0.5019608f);

	/**
	 * The color green with an RGB value of #008000 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#008000;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA GREEN = rgb(0.0f, 0.5019608f, 0.0f);

	/**
	 * The color green yellow with an RGB value of #ADFF2F
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#ADFF2F;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA GREENYELLOW = rgb(0.6784314f, 1.0f, 0.18431373f);

	/**
	 * The color grey with an RGB value of #808080 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#808080;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA GREY = GRAY;

	/**
	 * The color honeydew with an RGB value of #F0FFF0 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F0FFF0;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA HONEYDEW = rgb(0.9411765f, 1.0f, 0.9411765f);

	/**
	 * The color hot pink with an RGB value of #FF69B4 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF69B4;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA HOTPINK = rgb(1.0f, 0.4117647f, 0.7058824f);

	/**
	 * The color indian red with an RGB value of #CD5C5C
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#CD5C5C;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA INDIANRED = rgb(0.8039216f, 0.36078432f, 0.36078432f);

	/**
	 * The color indigo with an RGB value of #4B0082 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#4B0082;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA INDIGO = rgb(0.29411766f, 0.0f, 0.50980395f);

	/**
	 * The color ivory with an RGB value of #FFFFF0 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFFFF0;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA IVORY = rgb(1.0f, 1.0f, 0.9411765f);

	/**
	 * The color khaki with an RGB value of #F0E68C <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F0E68C;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA KHAKI = rgb(0.9411765f, 0.9019608f, 0.54901963f);

	/**
	 * The color lavender with an RGB value of #E6E6FA <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#E6E6FA;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LAVENDER = rgb(0.9019608f, 0.9019608f, 0.98039216f);

	/**
	 * The color lavender blush with an RGB value of #FFF0F5
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFF0F5;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LAVENDERBLUSH = rgb(1.0f, 0.9411765f, 0.9607843f);

	/**
	 * The color lawn green with an RGB value of #7CFC00
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#7CFC00;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LAWNGREEN = rgb(0.4862745f, 0.9882353f, 0.0f);

	/**
	 * The color lemon chiffon with an RGB value of #FFFACD
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFFACD;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LEMONCHIFFON = rgb(1.0f, 0.98039216f, 0.8039216f);

	/**
	 * The color light blue with an RGB value of #ADD8E6
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#ADD8E6;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTBLUE = rgb(0.6784314f, 0.84705883f, 0.9019608f);

	/**
	 * The color light coral with an RGB value of #F08080
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F08080;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTCORAL = rgb(0.9411765f, 0.5019608f, 0.5019608f);

	/**
	 * The color light cyan with an RGB value of #E0FFFF
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#E0FFFF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTCYAN = rgb(0.8784314f, 1.0f, 1.0f);

	/**
	 * The color light goldenrod yellow with an RGB value of #FAFAD2
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FAFAD2;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTGOLDENRODYELLOW = rgb(0.98039216f, 0.98039216f, 0.8235294f);

	/**
	 * The color light gray with an RGB value of #D3D3D3
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTGRAY = rgb(0.827451f, 0.827451f, 0.827451f);

	/**
	 * The color light green with an RGB value of #90EE90
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#90EE90;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTGREEN = rgb(0.5647059f, 0.93333334f, 0.5647059f);

	/**
	 * The color light grey with an RGB value of #D3D3D3
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTGREY = LIGHTGRAY;

	/**
	 * The color light pink with an RGB value of #FFB6C1
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFB6C1;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTPINK = rgb(1.0f, 0.7137255f, 0.75686276f);

	/**
	 * The color light salmon with an RGB value of #FFA07A
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFA07A;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTSALMON = rgb(1.0f, 0.627451f, 0.47843137f);

	/**
	 * The color light sea green with an RGB value of #20B2AA
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#20B2AA;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTSEAGREEN = rgb(0.1254902f, 0.69803923f, 0.6666667f);

	/**
	 * The color light sky blue with an RGB value of #87CEFA
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#87CEFA;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTSKYBLUE = rgb(0.5294118f, 0.80784315f, 0.98039216f);

	/**
	 * The color light slate gray with an RGB value of #778899
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#778899;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTSLATEGRAY = rgb(0.46666667f, 0.53333336f, 0.6f);

	/**
	 * The color light slate grey with an RGB value of #778899
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#778899;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTSLATEGREY = LIGHTSLATEGRAY;

	/**
	 * The color light steel blue with an RGB value of #B0C4DE
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#B0C4DE;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTSTEELBLUE = rgb(0.6901961f, 0.76862746f, 0.87058824f);

	/**
	 * The color light yellow with an RGB value of #FFFFE0
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFFFE0;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIGHTYELLOW = rgb(1.0f, 1.0f, 0.8784314f);

	/**
	 * The color lime with an RGB value of #00FF00 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#00FF00;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIME = rgb(0.0f, 1.0f, 0.0f);

	/**
	 * The color lime green with an RGB value of #32CD32
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#32CD32;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LIMEGREEN = rgb(0.19607843f, 0.8039216f, 0.19607843f);

	/**
	 * The color linen with an RGB value of #FAF0E6 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FAF0E6;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA LINEN = rgb(0.98039216f, 0.9411765f, 0.9019608f);

	/**
	 * The color magenta with an RGB value of #FF00FF <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MAGENTA = rgb(1.0f, 0.0f, 1.0f);

	/**
	 * The color maroon with an RGB value of #800000 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#800000;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MAROON = rgb(0.5019608f, 0.0f, 0.0f);

	/**
	 * The color medium aquamarine with an RGB value of #66CDAA
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#66CDAA;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMAQUAMARINE = rgb(0.4f, 0.8039216f, 0.6666667f);

	/**
	 * The color medium blue with an RGB value of #0000CD
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#0000CD;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMBLUE = rgb(0.0f, 0.0f, 0.8039216f);

	/**
	 * The color medium orchid with an RGB value of #BA55D3
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#BA55D3;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMORCHID = rgb(0.7294118f, 0.33333334f, 0.827451f);

	/**
	 * The color medium purple with an RGB value of #9370DB
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#9370DB;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMPURPLE = rgb(0.5764706f, 0.4392157f, 0.85882354f);

	/**
	 * The color medium sea green with an RGB value of #3CB371
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#3CB371;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMSEAGREEN = rgb(0.23529412f, 0.7019608f, 0.44313726f);

	/**
	 * The color medium slate blue with an RGB value of #7B68EE
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#7B68EE;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMSLATEBLUE = rgb(0.48235294f, 0.40784314f, 0.93333334f);

	/**
	 * The color medium spring green with an RGB value of #00FA9A
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#00FA9A;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMSPRINGGREEN = rgb(0.0f, 0.98039216f, 0.6039216f);

	/**
	 * The color medium turquoise with an RGB value of #48D1CC
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#48D1CC;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMTURQUOISE = rgb(0.28235295f, 0.81960785f, 0.8f);

	/**
	 * The color medium violet red with an RGB value of #C71585
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#C71585;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MEDIUMVIOLETRED = rgb(0.78039217f, 0.08235294f, 0.52156866f);

	/**
	 * The color midnight blue with an RGB value of #191970
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#191970;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MIDNIGHTBLUE = rgb(0.09803922f, 0.09803922f, 0.4392157f);

	/**
	 * The color mint cream with an RGB value of #F5FFFA
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F5FFFA;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MINTCREAM = rgb(0.9607843f, 1.0f, 0.98039216f);

	/**
	 * The color misty rose with an RGB value of #FFE4E1
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFE4E1;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MISTYROSE = rgb(1.0f, 0.89411765f, 0.88235295f);

	/**
	 * The color moccasin with an RGB value of #FFE4B5 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFE4B5;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA MOCCASIN = rgb(1.0f, 0.89411765f, 0.70980394f);

	/**
	 * The color navajo white with an RGB value of #FFDEAD
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFDEAD;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA NAVAJOWHITE = rgb(1.0f, 0.87058824f, 0.6784314f);

	/**
	 * The color navy with an RGB value of #000080 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#000080;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA NAVY = rgb(0.0f, 0.0f, 0.5019608f);

	/**
	 * The color old lace with an RGB value of #FDF5E6 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FDF5E6;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA OLDLACE = rgb(0.99215686f, 0.9607843f, 0.9019608f);

	/**
	 * The color olive with an RGB value of #808000 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#808000;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA OLIVE = rgb(0.5019608f, 0.5019608f, 0.0f);

	/**
	 * The color olive drab with an RGB value of #6B8E23
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#6B8E23;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA OLIVEDRAB = rgb(0.41960785f, 0.5568628f, 0.13725491f);

	/**
	 * The color orange with an RGB value of #FFA500 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFA500;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA ORANGE = rgb(1.0f, 0.64705884f, 0.0f);

	/**
	 * The color orange red with an RGB value of #FF4500
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF4500;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA ORANGERED = rgb(1.0f, 0.27058825f, 0.0f);

	/**
	 * The color orchid with an RGB value of #DA70D6 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#DA70D6;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA ORCHID = rgb(0.85490197f, 0.4392157f, 0.8392157f);

	/**
	 * The color pale goldenrod with an RGB value of #EEE8AA
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#EEE8AA;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PALEGOLDENROD = rgb(0.93333334f, 0.9098039f, 0.6666667f);

	/**
	 * The color pale green with an RGB value of #98FB98
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#98FB98;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PALEGREEN = rgb(0.59607846f, 0.9843137f, 0.59607846f);

	/**
	 * The color pale turquoise with an RGB value of #AFEEEE
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#AFEEEE;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PALETURQUOISE = rgb(0.6862745f, 0.93333334f, 0.93333334f);

	/**
	 * The color pale violet red with an RGB value of #DB7093
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#DB7093;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PALEVIOLETRED = rgb(0.85882354f, 0.4392157f, 0.5764706f);

	/**
	 * The color papaya whip with an RGB value of #FFEFD5
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFEFD5;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PAPAYAWHIP = rgb(1.0f, 0.9372549f, 0.8352941f);

	/**
	 * The color peach puff with an RGB value of #FFDAB9
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFDAB9;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PEACHPUFF = rgb(1.0f, 0.85490197f, 0.7254902f);

	/**
	 * The color peru with an RGB value of #CD853F <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#CD853F;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PERU = rgb(0.8039216f, 0.52156866f, 0.24705882f);

	/**
	 * The color pink with an RGB value of #FFC0CB <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFC0CB;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PINK = rgb(1.0f, 0.7529412f, 0.79607844f);

	/**
	 * The color plum with an RGB value of #DDA0DD <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#DDA0DD;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PLUM = rgb(0.8666667f, 0.627451f, 0.8666667f);

	/**
	 * The color powder blue with an RGB value of #B0E0E6
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#B0E0E6;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA POWDERBLUE = rgb(0.6901961f, 0.8784314f, 0.9019608f);

	/**
	 * The color purple with an RGB value of #800080 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#800080;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA PURPLE = rgb(0.5019608f, 0.0f, 0.5019608f);

	/**
	 * The color red with an RGB value of #FF0000 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF0000;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA RED = rgb(1.0f, 0.0f, 0.0f);

	/**
	 * The color rosy brown with an RGB value of #BC8F8F
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#BC8F8F;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA ROSYBROWN = rgb(0.7372549f, 0.56078434f, 0.56078434f);

	/**
	 * The color royal blue with an RGB value of #4169E1
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#4169E1;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA ROYALBLUE = rgb(0.25490198f, 0.4117647f, 0.88235295f);

	/**
	 * The color saddle brown with an RGB value of #8B4513
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#8B4513;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SADDLEBROWN = rgb(0.54509807f, 0.27058825f, 0.07450981f);

	/**
	 * The color salmon with an RGB value of #FA8072 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FA8072;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SALMON = rgb(0.98039216f, 0.5019608f, 0.44705883f);

	/**
	 * The color sandy brown with an RGB value of #F4A460
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F4A460;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SANDYBROWN = rgb(0.95686275f, 0.6431373f, 0.3764706f);

	/**
	 * The color sea green with an RGB value of #2E8B57 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#2E8B57;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SEAGREEN = rgb(0.18039216f, 0.54509807f, 0.34117648f);

	/**
	 * The color sea shell with an RGB value of #FFF5EE <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFF5EE;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SEASHELL = rgb(1.0f, 0.9607843f, 0.93333334f);

	/**
	 * The color sienna with an RGB value of #A0522D <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#A0522D;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SIENNA = rgb(0.627451f, 0.32156864f, 0.1764706f);

	/**
	 * The color silver with an RGB value of #C0C0C0 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#C0C0C0;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SILVER = rgb(0.7529412f, 0.7529412f, 0.7529412f);

	/**
	 * The color sky blue with an RGB value of #87CEEB <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#87CEEB;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SKYBLUE = rgb(0.5294118f, 0.80784315f, 0.92156863f);

	/**
	 * The color slate blue with an RGB value of #6A5ACD
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#6A5ACD;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SLATEBLUE = rgb(0.41568628f, 0.3529412f, 0.8039216f);

	/**
	 * The color slate gray with an RGB value of #708090
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#708090;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SLATEGRAY = rgb(0.4392157f, 0.5019608f, 0.5647059f);

	/**
	 * The color slate grey with an RGB value of #708090
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#708090;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SLATEGREY = SLATEGRAY;

	/**
	 * The color snow with an RGB value of #FFFAFA <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFFAFA;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SNOW = rgb(1.0f, 0.98039216f, 0.98039216f);

	/**
	 * The color spring green with an RGB value of #00FF7F
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#00FF7F;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA SPRINGGREEN = rgb(0.0f, 1.0f, 0.49803922f);

	/**
	 * The color steel blue with an RGB value of #4682B4
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#4682B4;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA STEELBLUE = rgb(0.27450982f, 0.50980395f, 0.7058824f);

	/**
	 * The color tan with an RGB value of #D2B48C <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#D2B48C;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA TAN = rgb(0.8235294f, 0.7058824f, 0.54901963f);

	/**
	 * The color teal with an RGB value of #008080 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#008080;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA TEAL = rgb(0.0f, 0.5019608f, 0.5019608f);

	/**
	 * The color thistle with an RGB value of #D8BFD8 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#D8BFD8;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA THISTLE = rgb(0.84705883f, 0.7490196f, 0.84705883f);

	/**
	 * The color tomato with an RGB value of #FF6347 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FF6347;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA TOMATO = rgb(1.0f, 0.3882353f, 0.2784314f);

	/**
	 * The color turquoise with an RGB value of #40E0D0 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#40E0D0;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA TURQUOISE = rgb(0.2509804f, 0.8784314f, 0.8156863f);

	/**
	 * The color violet with an RGB value of #EE82EE <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#EE82EE;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA VIOLET = rgb(0.93333334f, 0.50980395f, 0.93333334f);

	/**
	 * The color wheat with an RGB value of #F5DEB3 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F5DEB3;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA WHEAT = rgb(0.9607843f, 0.87058824f, 0.7019608f);

	/**
	 * The color white with an RGB value of #FFFFFF <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFFFFF;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA WHITE = rgb(1.0f, 1.0f, 1.0f);

	/**
	 * The color white smoke with an RGB value of #F5F5F5
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#F5F5F5;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA WHITESMOKE = rgb(0.9607843f, 0.9607843f, 0.9607843f);

	/**
	 * The color yellow with an RGB value of #FFFF00 <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#FFFF00;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA YELLOW = rgb(1.0f, 1.0f, 0.0f);

	/**
	 * The color yellow green with an RGB value of #9ACD32
	 * <div style="border:1px solid
	 * black;width:40px;height:20px;background-color:#9ACD32;float:right;margin: 0
	 * 10px 0 0"></div>
	 */
	public static final RGBA YELLOWGREEN = rgb(0.6039216f, 0.8039216f, 0.19607843f);

}
