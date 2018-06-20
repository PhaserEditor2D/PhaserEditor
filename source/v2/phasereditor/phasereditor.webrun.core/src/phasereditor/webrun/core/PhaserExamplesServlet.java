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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.ExampleCategoryModel;
import phasereditor.inspect.core.examples.ExampleModel;
import phasereditor.inspect.core.examples.ExamplesRepoModel;

/**
 * @author arian
 *
 */
public class PhaserExamplesServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PrintStream out = new PrintStream(resp.getOutputStream());

		resp.setContentType("text/html");

		out.println("<!DOCTYPE html>");
		out.println("<html>");

		out.println("<body style='font-family:arial;margin:1em'>");

		out.println("<h1>Phaser Examples</h1>");
		out.println("<small><i>Hosted locally by Phaser Editor</i></small><br><br>");

		ExamplesRepoModel examples = InspectCore.getExamplesRepoModel();

		out.println("<h2>Chapters</h2>");
		out.println("<ul>");
		for (ExampleCategoryModel cat : examples.getExamplesCategories()) {
			out.println("<li><a href='#" + examples.lookup(cat) + "'>" + cat.getName() + "</li></a>");
		}
		out.println("</ul>");

		out.println("<h2>Table of Contents</h2>");

		out.println("<ul>");
		printTableOfContents(out, examples, examples.getExamplesCategories());
		out.println("</ul>");

		out.println("</body>");

		out.println("</html>");

	}

	/**
	 * @param out
	 * @param examples
	 * @param examplesCategories
	 */
	private void printTableOfContents(PrintStream out, ExamplesRepoModel examples, List<ExampleCategoryModel> categories) {

		for (ExampleCategoryModel category : categories) {
			out.println("<li>");
			out.println("<h3 id='" + examples.lookup(category) + "'>" + category.getName() + "</h3>");

			out.println("<ul>");

			for (ExampleModel example : category.getTemplates()) {
				out.println("<li><a href='/phaser-example?n=" + examples.lookup(example) + "'>" + example.getName() + "</a></li>");
			}

			List<ExampleCategoryModel> subCategories = category.getSubCategories();
			printTableOfContents(out, examples, subCategories);

			out.println("</ul>");
			out.println("</li>");
		}
	}

}
