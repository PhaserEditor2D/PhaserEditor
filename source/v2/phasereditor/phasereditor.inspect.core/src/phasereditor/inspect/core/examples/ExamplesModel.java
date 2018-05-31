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
package phasereditor.inspect.core.examples;

import static java.lang.System.out;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.inspect.core.examples.ExampleModel.Mapping;

public class ExamplesModel {
	private Path _examplesFolderPath;
	private List<ExampleCategoryModel> _examplesCategories;

	public ExamplesModel(Path reposDir) {
		_examplesFolderPath = reposDir.resolve("phaser3-examples/public");
		_examplesCategories = new ArrayList<>();
	}

	public void build(IProgressMonitor monitor) throws IOException {
		buildExamples(monitor);
	}

	private void buildExamples(IProgressMonitor monitor) throws IOException {
		Path assetsPath = _examplesFolderPath.resolve("assets");
		Path srcPath = _examplesFolderPath.resolve("src");
		List<Path> requiredFiles = new ArrayList<>();
		{
			Path[] inAssets = Files.walk(assetsPath).filter(p -> !Files.isDirectory(p)).toArray(Path[]::new);
			requiredFiles.addAll(Arrays.asList(inAssets));
		}

		Path[] jsFiles = Files.walk(srcPath).filter(this::isExampleJSFile).toArray(Path[]::new);

		out.println("Examples: " + jsFiles.length);

		Map<Path, ExampleCategoryModel> catMap = new HashMap<>();

		monitor.beginTask("Building examples", jsFiles.length);

		for (Path jsFile : jsFiles) {
			
			//TODO: for the moment ignore this kind of examples.
			if (Files.exists(jsFile.resolveSibling("boot.json"))) {
				continue;
			}

			monitor.subTask(jsFile.getFileName().toString());

			Path catPath = jsFile.getParent();
			ExampleCategoryModel catModel = catMap.get(catPath);
			if (catModel == null) {
				Path relPath = _examplesFolderPath.resolve("src").relativize(catPath);
				catModel = new ExampleCategoryModel(getName(relPath));
				catMap.put(catPath, catModel);
				_examplesCategories.add(catModel);
			}

			String mainFile = jsFile.getFileName().toString().replace("\\", "/");
			ExampleModel exampleModel = new ExampleModel(this, catModel, getName(jsFile.getFileName()), mainFile);

			// add main example file
			exampleModel.addMapping(_examplesFolderPath.relativize(jsFile), jsFile.getFileName().toString());
			catModel.addExample(exampleModel);

			// add assets files
			String content = new String(Files.readAllBytes(jsFile));
			for (Path file : requiredFiles) {
				String assetRelPath = _examplesFolderPath.relativize(file).toString().replace("\\", "/");
				if (content.contains(assetRelPath) || content.contains("../" + assetRelPath)) {
					exampleModel.addMapping(_examplesFolderPath.relativize(file), assetRelPath);
				}
			}

			monitor.worked(1);
		}
		monitor.done();

		_examplesCategories.sort(new Comparator<ExampleCategoryModel>() {

			@Override
			public int compare(ExampleCategoryModel o1, ExampleCategoryModel o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		AtomicInteger i = new AtomicInteger(1);
		_examplesCategories.stream().forEach(m -> {
			out.println(i + ": " + m.getName());
			i.incrementAndGet();
		});

	}

	private static String getName(Path path) {
		String name = path.toString().replace("\\", "/").replace("/", " - ");
		if (name.endsWith(".js")) {
			name = name.substring(0, name.length() - 3);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (i == 0) {
				c = Character.toUpperCase(c);
			} else {
				char prev = name.charAt(i - 1);
				if (prev == ' ') {
					c = Character.toUpperCase(c);
				}
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public List<ExampleCategoryModel> getExamplesCategories() {
		return _examplesCategories;
	}

	private boolean isExampleJSFile(Path p) {
		String str = p.toString();
		return str.endsWith(".js");
	}

	public Path getExamplesRepoPath() {
		return _examplesFolderPath;
	}

	public void loadCache(Path cache) throws IOException {
		_examplesCategories = new ArrayList<>();

		JSONObject jsonDoc;
		try (InputStream input = Files.newInputStream(cache)) {
			jsonDoc = new JSONObject(new JSONTokener(input));
		}

		loadCategories(jsonDoc.getJSONArray("examplesCategories"), _examplesCategories);
	}

	private void loadCategories(JSONArray jsonCategories, List<ExampleCategoryModel> categories) {
		for (int i = 0; i < jsonCategories.length(); i++) {
			JSONObject jsonCategory = jsonCategories.getJSONObject(i);
			ExampleCategoryModel category = new ExampleCategoryModel(jsonCategory.getString("name"));
			categories.add(category);

			JSONArray jsonExamples = jsonCategory.getJSONArray("examples");
			for (int j = 0; j < jsonExamples.length(); j++) {
				JSONObject jsonExample = jsonExamples.getJSONObject(j);
				ExampleModel example = new ExampleModel(this, category, jsonExample.getString("name"),
						jsonExample.getString("mainFile"));
				category.addExample(example);

				JSONArray jsonMaps = jsonExample.getJSONArray("map");
				for (int k = 0; k < jsonMaps.length(); k++) {
					JSONObject jsonMap = jsonMaps.getJSONObject(k);
					String orig = jsonMap.getString("orig");
					example.addMapping(_examplesFolderPath.resolve(orig), jsonMap.getString("dst"));
				}
			}
		}
	}

	public void saveCache(Path cache) throws JSONException, IOException {
		JSONObject jsonDoc = new JSONObject();

		JSONArray jsonExamplesCategories = new JSONArray();
		jsonDoc.put("examplesCategories", jsonExamplesCategories);
		saveCategories(jsonExamplesCategories, _examplesCategories);

		Files.write(cache, jsonDoc.toString(2).getBytes());
	}

	private static void saveCategories(JSONArray jsonExamplesCategories, List<ExampleCategoryModel> categories) {
		for (ExampleCategoryModel category : categories) {
			JSONObject jsonCategory = new JSONObject();
			jsonCategory.put("name", category.getName());
			jsonExamplesCategories.put(jsonCategory);

			JSONArray jsonExamples = new JSONArray();
			jsonCategory.put("examples", jsonExamples);
			for (ExampleModel example : category.getTemplates()) {
				JSONObject jsonExample = new JSONObject();
				jsonExamples.put(jsonExample);
				jsonExample.put("name", example.getName());
				jsonExample.put("mainFile", example.getInfo().getMainFile());

				JSONArray jsonMaps = new JSONArray();
				jsonExample.put("map", jsonMaps);
				for (Mapping map : example.getFilesMapping()) {
					JSONObject jsonMap = new JSONObject();
					jsonMaps.put(jsonMap);
					jsonMap.put("orig", map.getOriginal().toString().replace("\\", "/"));
					jsonMap.put("dst", map.getDestiny());
				}
			}
		}
	}
}
