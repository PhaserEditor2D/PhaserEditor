// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.webrun.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.ExampleCategoryModel;
import phasereditor.inspect.core.examples.ExampleModel;
import phasereditor.inspect.core.examples.ExamplesModel;

/**
 * @author arian
 *
 */
public class PhaserExampleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("boxing")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		PrintStream out = new PrintStream(resp.getOutputStream());

		String id = req.getParameter("n");

		ExamplesModel examples = InspectCore.getExamplesModel();
		ExampleModel example = (ExampleModel) examples.lookup(Integer.parseInt(id));

		resp.setContentType("text/html");

		if (example == null) {
			out.println("Example not found: " + id);
			return;
		}

		out.println("<!DOCTYPE html>");
		out.println("<html>");

		out.println("<head>");
		out.println("<script src='/phaser-code/dist/phaser.js'></script>");
		out.println("<script src='/jslibs/highlight.pack.js'></script>");
		out.println("<script src='/jslibs/highlightjs-line-numbers.min.js'></script>");
		out.println("<link rel='stylesheet' href='jslibs/default.css'>");
		out.println("<title>" + example.getName() + "/" + example.getCategory().getName() + "</title>");
		out.println("</head>");

		out.println("<body style='font-family:arial;margin:1em'>");

		out.println("<h1>" + example.getName() + "</h1>");
		out.println("<small><i>Hosted locally by Phaser Editor</i></small><br><br>");

		out.println("<a href='/phaser-examples'>Examples</a> / ");

		{
			StringBuilder sb = new StringBuilder();
			ExampleCategoryModel cat = example.getCategory();
			do {
				sb.insert(0, "<a href='/phaser-examples#" + examples.lookup(cat) + "'>" + cat.getName() + "</a> / ");
				cat = cat.getParentCategory();
			} while (cat != null);
			
			sb.append(example.getName());
			out.println(sb.toString());
		}
		

		out.println("<br><br>");

		out.println("<div style='float:left'>");
		out.println("<ul>");
		for (ExampleModel example2 : example.getCategory().getTemplates()) {
			out.println("<li><a href='/phaser-example?n=" + examples.lookup(example2) + "'>" + example2.getName()
					+ "</a></li>");
		}
		out.println("</ul>");
		out.println("</div>");

		out.println("<div style='float:left; margin-left:2em' id='phaser-example'></div>");
		out.println("<div style='clear:both	'></div>");

		{
			Path path = examples.getExamplesRepoPath().relativize(example.getMainFilePath());
			out.println("<script src='/examples-files/" + path.toString().replace("\\", "/") + "'></script>");
		}

		out.println("<pre id='text'>");

		Path file = example.getMainFilePath();

		byte[] bytes = Files.readAllBytes(file);
		out.println(new String(bytes));

		out.println("</pre>");

		out.println("<script>");
		out.println("var dom = document.getElementById('text');");
		out.println("hljs.highlightBlock(dom);");
		out.println("hljs.lineNumbersBlock(dom);");
		out.println("var list = document.getElementsByTagName('tr');");

		String line = req.getParameter("l");
		if (line != null) {
			line = Integer.parseInt(line) - 1 + "";
			out.println("console.log('scrolling to " + line + "')");
			out.println("setTimeout(function () { list[" + line + "].scrollIntoViewIfNeeded();list[" + line
					+ "].setAttribute('style', 'background:lightgray');}, 1000);");
		}

		out.println("</script>");

		out.println("</body>");

		out.println("</html>");

	}

}
