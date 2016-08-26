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
package buildphaserdocs;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author arian
 *
 */
public class BuildPhaserReferenceGuide {
	public static void main(String[] args) throws IOException {
		StringBuilder toc = new StringBuilder();
		toc.append("<toc label='Phaser Documentation 2.5.0' link_to='toc.xml#phaser-doc'>\n");

		Files.walk(Paths.get("input-docs")).forEach(p -> {
			String name = p.getFileName().toString();
			if (!Files.isDirectory(p) && (name.startsWith("Phaser.") || name.startsWith("PIXI."))) {
				try {

					//out.println("Processing " + p);
					String topic = "<topic href='html/phaser-doc/" + name + "' label='"
							+ name.substring(0, name.length() - 5) + "'> ";
					toc.append(topic);
					String s = getJSDoc(p, toc);
					toc.append("</topic>\n");

					
					Files.write(Paths.get("output-docs").resolve(name), s.getBytes(), StandardOpenOption.CREATE);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		toc.append("</toc>\n");

		String str = toc.toString();
		Files.write(Paths.get("phaser-toc.xml"), str.getBytes(), StandardOpenOption.CREATE);
		out.println(str);
	}

	public static String getJSDoc(Path file, StringBuilder topics) throws IOException {
		String input = new String(Files.readAllBytes(file));
		Document doc = Jsoup.parse(input);
		String body = doc.select("#main").html();

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<head>");
		sb.append("<link type='text/css' rel='stylesheet' href='styles/javadoc.css'>");
		sb.append("<link type='text/css' rel='stylesheet' href='styles/default.css'>");
		sb.append(body);
		sb.append("</head>");
		sb.append("</html>");

		{
			Elements list = doc.select("h4");
			for (Element e : list) {
				String name = e.text();
				String signature = xmlEscapeText(name);
				for (String c : new String[] { "(", ":" }) {
					int i = name.indexOf(c);
					if (i != -1) {
						name = name.substring(0, i);
					}
				}
				int i = name.indexOf(">");
				if (i != -1) {
					name = name.substring(i + 1).trim();
				}

				topics.append("  <topic label='" + signature + "' href='html/phaser-doc/" + file.getFileName() + "#"
						+ name + "'></topic>\n");
			}
		}

		return sb.toString();
	}

	static String xmlEscapeText(String t) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			switch (c) {
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '\"':
				sb.append("&quot;");
				break;
			case '&':
				sb.append("&amp;");
				break;
			case '\'':
				sb.append("&apos;");
				break;
			default:
				if (c > 0x7e) {
					sb.append("&#" + ((int) c) + ";");
				} else
					sb.append(c);
			}
		}
		return sb.toString();
	}
}
