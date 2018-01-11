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
package phasereditor.bmpfont.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class BitmapFontModel {

	public static class InfoTag {
		private String _face;
		private int _size;

		public String getFace() {
			return _face;
		}

		public void setFace(String face) {
			_face = face;
		}

		public int getSize() {
			return _size;
		}

		public void setSize(int size) {
			_size = size;
		}
	}

	/**
	 * The common tag.
	 * 
	 * @author arian
	 *
	 */
	public static class CommonTag {
		private int _lineHeight;
		private int _base;
		private int _scaleW;
		private int _scaleH;

		public int getLineHeight() {
			return _lineHeight;
		}

		public void setLineHeight(int lineHeight) {
			_lineHeight = lineHeight;
		}

		public int getBase() {
			return _base;
		}

		public void setBase(int base) {
			_base = base;
		}

		public int getScaleW() {
			return _scaleW;
		}

		public void setScaleW(int scaleW) {
			_scaleW = scaleW;
		}

		public int getScaleH() {
			return _scaleH;
		}

		public void setScaleH(int scaleH) {
			_scaleH = scaleH;
		}

	}

	/*
	 * The char tag.
	 */
	public static class CharTag {
		private int _id;
		private int _x;
		private int _y;
		private int _width;
		private int _height;
		private int _xoffset;
		private int _yoffset;
		private int _xadvance;

		public int getId() {
			return _id;
		}

		public void setId(int id) {
			_id = id;
		}

		public int getX() {
			return _x;
		}

		public void setX(int x) {
			_x = x;
		}

		public int getY() {
			return _y;
		}

		public void setY(int y) {
			_y = y;
		}

		public int getWidth() {
			return _width;
		}

		public void setWidth(int width) {
			_width = width;
		}

		public int getHeight() {
			return _height;
		}

		public void setHeight(int height) {
			_height = height;
		}

		public int getXoffset() {
			return _xoffset;
		}

		public void setXoffset(int xoffset) {
			_xoffset = xoffset;
		}

		public int getYoffset() {
			return _yoffset;
		}

		public void setYoffset(int yoffset) {
			_yoffset = yoffset;
		}

		public int getXadvance() {
			return _xadvance;
		}

		public void setXadvance(int xadvance) {
			_xadvance = xadvance;
		}

	}

	public static class KerningTag {
		private int _first;
		private int _second;
		private int _amount;

		public int getFirst() {
			return _first;
		}

		public void setFirst(int first) {
			_first = first;
		}

		public int getSecond() {
			return _second;
		}

		public void setSecond(int second) {
			_second = second;
		}

		public int getAmount() {
			return _amount;
		}

		public void setAmount(int amount) {
			_amount = amount;
		}

	}

	private Map<Integer, CharTag> _chars;
	private CommonTag _commonTag;
	private InfoTag _infoTag;
	private Map<String, KerningTag> _kernings;

	public BitmapFontModel(InputStream input) throws Exception {
		_chars = new HashMap<>();
		_kernings = new HashMap<>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(input);
		doc.getDocumentElement().normalize();

		{
			_infoTag = new InfoTag();
			Node elem = doc.getElementsByTagName("info").item(0);
			NamedNodeMap attrs = elem.getAttributes();
			_infoTag.setFace(attrs.getNamedItem("face").getNodeValue());
			_infoTag.setSize(Integer.parseInt(attrs.getNamedItem("size").getNodeValue()));
		}

		{
			NodeList chars = doc.getElementsByTagName("char");

			for (int i = 0; i < chars.getLength(); i++) {

				Node elem = chars.item(i);

				if (elem.getNodeType() == Node.ELEMENT_NODE) {

					NamedNodeMap attrs = elem.getAttributes();

					CharTag charTag = new CharTag();

					charTag.setId(Integer.parseInt(attrs.getNamedItem("id").getNodeValue()));
					charTag.setX(Integer.parseInt(attrs.getNamedItem("x").getNodeValue()));
					charTag.setY(Integer.parseInt(attrs.getNamedItem("y").getNodeValue()));
					charTag.setWidth(Integer.parseInt(attrs.getNamedItem("width").getNodeValue()));
					charTag.setHeight(Integer.parseInt(attrs.getNamedItem("height").getNodeValue()));
					charTag.setXoffset(Integer.parseInt(attrs.getNamedItem("xoffset").getNodeValue()));
					charTag.setYoffset(Integer.parseInt(attrs.getNamedItem("yoffset").getNodeValue()));
					charTag.setXadvance(Integer.parseInt(attrs.getNamedItem("xadvance").getNodeValue()));

					_chars.put(charTag.getId(), charTag);
				}
			}
		}

		{
			_commonTag = new CommonTag();
			Node elem = doc.getElementsByTagName("common").item(0);
			NamedNodeMap attrs = elem.getAttributes();
			_commonTag.setLineHeight(Integer.parseInt(attrs.getNamedItem("lineHeight").getNodeValue()));
			_commonTag.setBase(Integer.parseInt(attrs.getNamedItem("base").getNodeValue()));
			_commonTag.setScaleW(Integer.parseInt(attrs.getNamedItem("scaleW").getNodeValue()));
			_commonTag.setScaleH(Integer.parseInt(attrs.getNamedItem("scaleH").getNodeValue()));
		}

		{
			NodeList kernings = doc.getElementsByTagName("kerning");

			for (int i = 0; i < kernings.getLength(); i++) {

				Node elem = kernings.item(i);

				if (elem.getNodeType() == Node.ELEMENT_NODE) {

					NamedNodeMap attrs = elem.getAttributes();

					KerningTag kerning = new KerningTag();

					kerning.setFirst(Integer.parseInt(attrs.getNamedItem("first").getNodeValue()));
					kerning.setSecond(Integer.parseInt(attrs.getNamedItem("second").getNodeValue()));
					kerning.setAmount(Integer.parseInt(attrs.getNamedItem("amount").getNodeValue()));

					_kernings.put(kerning.getFirst() + "-" + kerning.getSecond(), kerning);
				}
			}
		}

	}

	public String getInfoFace() {
		return _infoTag.getFace();
	}

	public int getInfoSize() {
		return _infoTag.getSize();
	}

	public enum Align {
		left, center, right
	}

	public static class RenderArgs {
		private String _text;
		private int _fontSize;
		private int _maxWidth;
		private Align _align;

		/**
		 * 
		 * @param text
		 *            The text.
		 * @param maxWidth
		 *            The max width to wrap the text. Set <code>0</code> to ignore it.
		 * @param align
		 */
		public RenderArgs(String text, int fontSize, int maxWidth, Align align) {
			super();
			_text = text;
			_fontSize = fontSize;
			_maxWidth = maxWidth;
			_align = align;
		}

		public RenderArgs(String text) {
			this(text, 0, 32, Align.left);
		}

		public String getText() {
			return _text;
		}

		public int getFontSize() {
			return _fontSize;
		}

		public int getMaxWidth() {
			return _maxWidth;
		}

		public Align getAlign() {
			return _align;
		}

	}

	public void render(RenderArgs args, BitmapFontRenderer renderer) {

		class CharRenderInfo {
			public char c;
			public int x;
			public int y;
			public int srcX;
			public int srcY;
			public int srcW;
			public int srcH;
			public int x2;

			public CharRenderInfo(char c, int x, int y, int srcX, int srcY, int srcW, int srcH) {
				super();
				this.c = c;
				this.x = x;
				this.y = y;
				this.srcX = srcX;
				this.srcY = srcY;
				this.srcW = srcW;
				this.srcH = srcH;
			}

		}

		class LineRenderInfo extends ArrayList<CharRenderInfo> {

			private static final long serialVersionUID = 1L;

			public int getPixelLength() {
				return isEmpty() ? 0 : get(size() - 1).x2;
			}
		}

		String text = args.getText();
		text = text.replace("\r\n", "\n").replace("\r", "\n");

		String normalText = "";

		{
			StringBuilder sb = new StringBuilder();
			for (int c : text.toCharArray()) {
				sb.append(_chars.containsKey(c) || c == '\n' ? (char) c : ' ');
			}
			normalText = sb.toString();
		}

		List<LineRenderInfo> lines;

		{
			int maxWidth = (int) (args.getMaxWidth() * (double) _infoTag.getSize() / args.getFontSize());

			boolean wrap = maxWidth > 0;

			int x = 0;
			int y = 0;

			lines = new ArrayList<>();
			LineRenderInfo line = new LineRenderInfo();

			int lastSpaceIndex = -1;
			int lineStart = 0;

			int len = normalText.length();

			for (int i = 0; i < len; i++) {
				int c = normalText.charAt(i);
				int first = c;
				int second = i == len - 1 ? -1 : text.charAt(i + 1);

				if (c == '\n') {
					x = 0;
					lastSpaceIndex = -1;
					lineStart = i;
					lines.add(line);
					line = new LineRenderInfo();
					y += _commonTag.getLineHeight();
					continue;
				}

				CharTag charTag = _chars.get(c);

				if (c == ' ') {
					lastSpaceIndex = i;
				}

				CharRenderInfo charRenderInfo = new CharRenderInfo((char) c, x + charTag.getXoffset(),
						y + charTag.getYoffset(), charTag.getX(), charTag.getY(), charTag.getWidth(),
						charTag.getHeight());

				line.add(charRenderInfo);

				int k = 0;

				if (second != -1) {
					KerningTag kerning = _kernings.get(first + "-" + second);
					if (kerning != null) {
						k = kerning.getAmount();
					}
				}

				x += charTag.getXadvance() + k;

				// this is the right code, but we have to deal with the Phaser 2.6.2 bug
				charRenderInfo.x2 = x;

				if (wrap && x > maxWidth && lastSpaceIndex != -1) {
					// get the new line to add, from the line start to the current position
					// String newLine = normalText.substring(i + 1 - line.size(), lastSpaceIndex);

					// remove the line chars from the last space
					line.removeAll(line.subList(lastSpaceIndex - lineStart, line.size()));

					// remove trailing spaces
					while (true) {
						if (!line.isEmpty()) {
							int last = line.size() - 1;
							if (line.get(last).c == ' ') {
								line.remove(last);
							} else {
								break;
							}
						}
					}

					// move the cursor to the last space position
					i = lastSpaceIndex;
					lineStart = lastSpaceIndex;

					// reset the last space position to empty
					lastSpaceIndex = -1;

					// add the new line
					lines.add(line);
					line = new LineRenderInfo();

					// reset x, y
					x = 0;
					y += _commonTag.getLineHeight();
				}
			}

			if (line.size() > 0) {
				lines.add(line);
			}
		}

		// align

		if (args.getAlign() != Align.left) {
			int maxLen = 0;

			for (LineRenderInfo line : lines) {
				int lineLen = line.getPixelLength();
				maxLen = Math.max(maxLen, lineLen);
			}

			for (LineRenderInfo line : lines) {
				int offset = maxLen - line.getPixelLength();

				if (args.getAlign() == Align.center) {
					offset = offset / 2;
				}

				for (CharRenderInfo c : line) {
					c.x += offset;
					c.x2 += offset;
				}
			}

		}

		// render lines

		lines.forEach(line -> line.forEach(c -> renderer.render(c.c, c.x, c.y, c.srcX, c.srcY, c.srcW, c.srcH)));
	}

	public static class MetricsRenderer implements BitmapFontRenderer {

		private int _width = 0;
		private int _height = 0;

		@Override
		public void render(char c, int x, int y, int srcX, int srcY, int srcW, int srcH) {
			if (x + srcW > _width) {
				_width = x + srcW;
			}

			if (y + srcH > _height) {
				_height = y + srcH;
			}
		}

		public int getWidth() {
			return _width;
		}

		public int getHeight() {
			return _height;
		}

	}

	public MetricsRenderer metrics(String text) {
		MetricsRenderer result = new MetricsRenderer();
		render(new RenderArgs(text), result);
		return result;
	}
}
