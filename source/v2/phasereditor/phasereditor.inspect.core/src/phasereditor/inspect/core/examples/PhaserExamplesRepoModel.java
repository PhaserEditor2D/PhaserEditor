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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.inspect.core.examples.PhaserExampleModel.Mapping;

@SuppressWarnings("boxing")
public class PhaserExamplesRepoModel {
	Path _examplesFolderPath;
	List<PhaserExampleCategoryModel> _examplesCategories;
	Map<Object, Object> _lookupTable;
	private int _counter;

	public PhaserExamplesRepoModel(Path reposDir) {
		_examplesFolderPath = reposDir.resolve("phaser3-examples/public");
		_examplesCategories = new ArrayList<>();
	}

	private void buildLookupTable() {
		_counter = 0;
		_lookupTable = new HashMap<>();
		buildLookupTable(_examplesCategories);
	}

	private void buildLookupTable(List<PhaserExampleCategoryModel> categories) {
		for (PhaserExampleCategoryModel category : categories) {
			int id = _counter++;
			_lookupTable.put(id, category);
			_lookupTable.put(category, id);

			for (PhaserExampleModel example : category.getTemplates()) {
				id = _counter++;
				_lookupTable.put(id, example);
				_lookupTable.put(example, id);
			}

			buildLookupTable(category.getSubCategories());
		}

	}

	public Object lookup(Object obj) {
		return _lookupTable.get(obj);
	}

	public void build() throws IOException {
		Path assetsPath = _examplesFolderPath.resolve("assets");
		Path srcPath = _examplesFolderPath.resolve("src");
		List<Path> requiredFiles = new ArrayList<>();
		{
			Path[] inAssets = Files.walk(assetsPath).filter(p -> !Files.isDirectory(p)).toArray(Path[]::new);
			requiredFiles.addAll(Arrays.asList(inAssets));
		}

		Comparator<PhaserExampleCategoryModel> comparator = new Comparator<PhaserExampleCategoryModel>() {

			@Override
			public int compare(PhaserExampleCategoryModel o1, PhaserExampleCategoryModel o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};

		Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {

			Stack<PhaserExampleCategoryModel> _stack = new Stack<>();

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.equals(srcPath)) {
					return FileVisitResult.CONTINUE;
				}

				String filename = dir.getFileName().toString();

				if (filename.startsWith("_") || filename.equals("archived")) {
					out.println("Skip " + dir);
					return FileVisitResult.SKIP_SUBTREE;
				}

				if (Files.exists(dir.resolve("boot.json"))) {
					out.println("Skip boot based examples " + dir);
					return FileVisitResult.SKIP_SIBLINGS;
				}

				PhaserExampleCategoryModel parent = _stack.isEmpty() ? null : _stack.peek();
				PhaserExampleCategoryModel category = new PhaserExampleCategoryModel(parent, dir, getName(dir.getFileName()));
				_stack.push(category);

				if (parent == null) {
					_examplesCategories.add(category);
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path jsFile, BasicFileAttributes attrs) throws IOException {
				PhaserExampleCategoryModel category = _stack.peek();

				if (!isExampleJSFile(jsFile)) {
					out.println("Skip " + jsFile);
					return FileVisitResult.CONTINUE;
				}

				PhaserExampleModel exampleModel = new PhaserExampleModel(category, jsFile);

				// add main example file
				exampleModel.addMapping(_examplesFolderPath.relativize(jsFile), jsFile.getFileName().toString());
				category.addExample(exampleModel);

				// add assets files
				String content = new String(Files.readAllBytes(jsFile));

				for (Path requiredFile : requiredFiles) {
					String assetRelPath = _examplesFolderPath.relativize(requiredFile).toString().replace("\\", "/");
					if (content.contains(assetRelPath) || content.contains("../" + assetRelPath)) {
						exampleModel.addMapping(_examplesFolderPath.relativize(requiredFile), assetRelPath);
					}
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (dir.equals(srcPath) || _stack.isEmpty()) {
					return FileVisitResult.CONTINUE;
				}

				PhaserExampleCategoryModel category = _stack.pop();
				category.getSubCategories().sort(comparator);
				category.getTemplates().sort((t1, t2) -> t1.getName().compareTo(t2.getName()));

				return FileVisitResult.CONTINUE;
			}

		});

		_examplesCategories.sort(comparator);

		for (PhaserExampleCategoryModel c : _examplesCategories) {
			c.printTree(0);
		}

	}

	static String getName(Path path) {
		String name = path.toString().replace("\\", "/");
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

	public List<PhaserExampleCategoryModel> getExamplesCategories() {
		return _examplesCategories;
	}

	static boolean isExampleJSFile(Path p) {
		String filename = p.getFileName().toString();

		return !filename.startsWith("_") && filename.endsWith(".js");
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

		loadCategories(jsonDoc.getJSONArray("examplesCategories"), null);

		buildLookupTable();
	}

	private void loadCategories(JSONArray jsonCategories, PhaserExampleCategoryModel parent) {
		for (int i = 0; i < jsonCategories.length(); i++) {
			JSONObject jsonCategory = jsonCategories.getJSONObject(i);
			String relPath = jsonCategory.getString("relPath");
			Path path = _examplesFolderPath.resolve(relPath);
			PhaserExampleCategoryModel category = new PhaserExampleCategoryModel(parent, path, jsonCategory.getString("name"));

			if (parent == null) {
				_examplesCategories.add(category);
			}

			JSONArray jsonSubCategories = jsonCategory.getJSONArray("subCategories");
			loadCategories(jsonSubCategories, category);

			JSONArray jsonExamples = jsonCategory.getJSONArray("examples");
			for (int j = 0; j < jsonExamples.length(); j++) {
				JSONObject jsonExample = jsonExamples.getJSONObject(j);
				String mainFilePathStr = jsonExample.getString("mainFile");
				Path mainFile = _examplesFolderPath.resolve(mainFilePathStr);
				PhaserExampleModel example = new PhaserExampleModel(category, mainFile);
				category.addExample(example);

				JSONArray jsonMaps = jsonExample.getJSONArray("map");
				for (int k = 0; k < jsonMaps.length(); k++) {
					JSONObject jsonMap = jsonMaps.getJSONObject(k);
					String orig = jsonMap.getString("orig");
					String dst = jsonMap.optString("dst", orig);
					example.addMapping(_examplesFolderPath.resolve(orig), dst);
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

	private void saveCategories(JSONArray jsonExamplesCategories, List<PhaserExampleCategoryModel> categories) {
		for (PhaserExampleCategoryModel category : categories) {
			JSONObject jsonCategory = new JSONObject();
			String relPath = _examplesFolderPath.relativize(category.getPath()).toString().replace("\\", "/");
			jsonCategory.put("name", category.getName());
			jsonCategory.put("relPath", relPath);
			jsonExamplesCategories.put(jsonCategory);

			JSONArray jsonSubCategories = new JSONArray();
			jsonCategory.put("subCategories", jsonSubCategories);
			saveCategories(jsonSubCategories, category.getSubCategories());

			JSONArray jsonExamples = new JSONArray();
			jsonCategory.put("examples", jsonExamples);
			for (PhaserExampleModel example : category.getTemplates()) {
				JSONObject jsonExample = new JSONObject();
				jsonExamples.put(jsonExample);
				jsonExample.put("mainFile", _examplesFolderPath.relativize(example.getMainFilePath()));

				JSONArray jsonMaps = new JSONArray();
				jsonExample.put("map", jsonMaps);
				for (Mapping map : example.getFilesMapping()) {
					JSONObject jsonMap = new JSONObject();
					jsonMaps.put(jsonMap);
					String orig = map.getOriginal().toString().replace("\\", "/");
					String dst = map.getDestiny();
					jsonMap.put("orig", orig);
					if (!orig.equals(dst)) {
						jsonMap.put("dst", dst);
					}
				}
			}
		}
	}
}
