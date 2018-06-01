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
package phasereditor.chains.core.build;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.chains.core.Line;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.ExampleCategoryModel;
import phasereditor.inspect.core.examples.ExampleModel;
import phasereditor.inspect.core.examples.ExamplesModel;

/**
 * @author arian
 *
 */
public class BuildRayoExamples {
	public static void main(String[] args) throws IOException {
		Path wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		Path projectPath = wsPath.resolve(InspectCore.RESOURCES_EXAMPLES_PLUGIN);

		List<Line> lines = new ArrayList<>();
		List<String> files = new ArrayList<>();

		ExamplesModel examples = new ExamplesModel(projectPath);
		examples.build();

		for (ExampleCategoryModel category : examples.getExamplesCategories()) {
			for (ExampleModel example : category.getTemplates()) {
				String filename = example.getCategory().getName().toLowerCase() + "/" + example.getInfo().getMainFile();
				files.add(filename);
				List<String> contentLines = Files.readAllLines(example.getMainFilePath());
				int linenum = 1;
				for (String text : contentLines) {
					text = text.trim();
					if (text.length() > 0 && !text.startsWith("//")) {
						Line line = new Line();
						line.filename = filename;
						line.linenum = linenum;
						line.text = text;
						lines.add(line);
					}
					linenum++;
				}
			}
		}

		JSONObject doc = new JSONObject();
		JSONArray array = new JSONArray();
		doc.put("lines", array);
		for (Line line : lines) {
			JSONObject obj = new JSONObject();
			obj.put("t", line.text);
			obj.put("l", line.linenum);
			obj.put("f", files.indexOf(line.filename));
			array.put(obj);
		}

		array = new JSONArray();
		doc.put("files", array);
		for (String fname : files) {
			array.put(fname);
		}

		out.println(doc.toString(1));

		Files.write(Paths.get("examples.json"), doc.toString().getBytes());
	}
}
