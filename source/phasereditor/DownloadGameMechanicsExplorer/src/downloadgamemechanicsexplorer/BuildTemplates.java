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
package downloadgamemechanicsexplorer;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author arian
 *
 */
public class BuildTemplates {
	public static void main(String[] args) throws JSONException, IOException {
		JSONArray slugArray = new JSONArray(new JSONTokener(Files.newInputStream(Paths.get("data/examples.json"))));

		List<String> assetnames = new ArrayList<>();

		Path assetsPath = Paths.get("data/assets");
		Files.walk(assetsPath).forEach(p -> {
			Path asset = assetsPath.relativize(p);
			if (!Files.isDirectory(p)) {
				assetnames.add(asset.toString().replace("\\", "/"));
			}
		});

		assetnames.stream().forEach(n -> {
			System.out.println(n);
		});

		String index_html = new String(Files.readAllBytes(Paths.get("data/examples-index.html")));

		int n = 0;
		for (int i = 0; i < slugArray.length(); i++) {
			JSONObject slug = slugArray.getJSONObject(i);
			String slugname = slug.getString("name");
			String slugCat = slug.getString("category");
			slugCat = slugCat.substring(0, 1).toUpperCase() + slugCat.substring(1);
			JSONArray examples = slug.getJSONArray("examples");
			for (int j = 0; j < examples.length(); j++) {
				n++;
				JSONObject example = examples.getJSONObject(j);
				String name = example.getString("name");
				String script = example.getString("script");
				String desc = example.getString("description");
				Path catPath = Paths.get("data/templates/" + (n < 10 ? "0" : "") + n + " - " + slugCat + " - "
						+ slugname + " - " + name);
				Files.createDirectories(catPath);
				Path examplePath = catPath; // do not nest the example into the
											// category
				Path webContent = examplePath.resolve("WebContent");
				Files.createDirectories(examplePath.resolve("Design"));
				Files.createDirectories(webContent);

				// assets
				Path scriptPath = Paths.get("data/source").resolve(script);
				List<String> content = Files.readAllLines(scriptPath);
				for (String line : content) {
					for (String assetname : assetnames) {
						if (line.contains(assetname)) {
							Path src = Paths.get("data/assets").resolve(assetname);
							out.println("copy " + src + " to " + webContent);
							Path dst = webContent.resolve(assetname);
							Files.createDirectories(dst.getParent());
							Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}

				// script.js
				{
					String js = new String(Files.readAllBytes(scriptPath));
					js = js.replace("/assets", "assets");
					js = js.replace("_EXAMPLEWIDTH", "640");
					js = js.replace("_EXAMPLEHEIGHT", "320");
					Files.write(webContent.resolve(scriptPath.getFileName()), js.getBytes(), StandardOpenOption.CREATE);
				}

				// index.html
				{
					String html = index_html.replace("{{title}}", "Phaser Editor - " + name);
					html = html.replace("{{description}}", desc);
					html = html.replace("{{include-js}}", "<script src='" + script + "'></script>");

					Files.write(webContent.resolve("index.html"), html.getBytes(), StandardOpenOption.CREATE);
				}

				// template.js
				{
					JSONObject obj = new JSONObject();
					JSONObject author = new JSONObject();
					author.put("name", "John Watson");
					author.put("email", "john@watson-net.com");
					author.put("website", "http://gamemechanicexplorer.com");
					obj.put("author", author);
					obj.put("mainFile", script);
					obj.put("description", desc);
					Files.write(examplePath.resolve("template.json"), obj.toString(4).getBytes(),
							StandardOpenOption.CREATE);
				}
			}
		}
	}
}
