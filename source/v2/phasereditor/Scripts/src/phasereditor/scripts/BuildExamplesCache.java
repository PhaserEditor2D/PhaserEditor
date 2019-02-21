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
package phasereditor.scripts;

import static java.lang.System.exit;
import static java.lang.System.out;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.PhaserExampleCategoryModel;
import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.inspect.core.examples.PhaserExamplesRepoModel;

public class BuildExamplesCache extends Application {

	Path _wsPath;
	Path _examplesProjectPath;
	Path _metadataProjectPath;
	WebEngine _engine;
	LinkedList<PhaserExampleModel> _examples;
	private Path _cacheFolder;
	private PhaserExampleModel _currentExample;
	private PhaserExamplesRepoModel model;
	private Path _examplesPath;

	@Override
	public void start(Stage stage) throws Exception {

		// init cache

		_cacheFolder = Paths.get(System.getProperty("user.home") + "/.phasereditor_dev/examples-cache");
		Files.createDirectories(_cacheFolder);

		_engine = new WebEngine();

		Logger logger = Logger.getLogger("com.sun.webkit.WebPage");
		logger.setLevel(Level.FINE);
		logger.addHandler(createLoggerHandler());

		_wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		_examplesProjectPath = _wsPath.resolve(InspectCore.RESOURCES_EXAMPLES_PLUGIN);
		_examplesPath = _examplesProjectPath.resolve("phaser3-examples/public");
		_metadataProjectPath = _wsPath.resolve(InspectCore.RESOURCES_METADATA_PLUGIN);

		out.println("Building model...");
		out.println();

		model = new PhaserExamplesRepoModel(_examplesProjectPath);
		model.build();

		_examples = new LinkedList<>();

		for (PhaserExampleCategoryModel c : model.getExamplesCategories()) {
			visitCategory(c);
		}

		ExecutorService pool = Executors.newCachedThreadPool();

		_engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {

			@Override
			public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
				if (newValue == State.SUCCEEDED) {
					pool.execute(new Runnable() {
						@Override
						public void run() {

							try {
								Thread.sleep(1 * 1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									try {
										processNewExample();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							});

						}
					});
				}
			}
		});

		processNewExample();
	}

	void processNewExample() throws Exception {

		if (_examples.isEmpty()) {
			examplesProcessingDone();
		} else {
			_currentExample = _examples.removeFirst();

			Path exampleFile = _currentExample.getFilePath();

			_currentExample.addMapping(_examplesPath.relativize(exampleFile), exampleFile.getFileName().toString());

			Path path = exampleFile;
			path = _examplesPath.relativize(path);
			String url = "http://127.0.0.1:8080/view.html?src=" + path;

			Path cacheFile = getCacheFile(_currentExample);

			out.println();
			out.println("Processing example: " + _currentExample.getFullName());

			if (loadFromCache(_currentExample)) {
				processNewExample();
			} else {
				if (!Files.exists(cacheFile)) {
					Files.createFile(cacheFile);
				}
				out.println("Loading " + url);
				_engine.load(url);
			}
		}
	}

	private void examplesProcessingDone() throws Exception {
		out.println("DONE!");

		saveCache();

		exit(0);
	}

	void addExampleMapping(String url) throws IOException {
		String url2 = decodeUrl(url);

		out.println("- Catching asset: " + url2);

		_currentExample.addMapping(Paths.get(url2), url2);

		Path cacheFile = getCacheFile(_currentExample);

		List<String> urls = new ArrayList<>(Files.readAllLines(cacheFile));

		urls.add(url2);

		Files.write(cacheFile, urls);
	}

	boolean loadFromCache(PhaserExampleModel example) throws IOException {
		Path cacheFile = getCacheFile(example);

		if (Files.exists(cacheFile)) {
			List<String> urls = Files.readAllLines(cacheFile);

			for (String url : new HashSet<>(urls)) {
				url = decodeUrl(url);
				out.println("* Restore asset: " + url);
				_currentExample.addMapping(Paths.get(url), url);
			}

			return true;
		}

		return false;
	}

	private static String decodeUrl(String url) throws UnsupportedEncodingException {
		return java.net.URLDecoder.decode(url, "UTF-8");
	}

	private Path getCacheFile(PhaserExampleModel example) throws IOException {
		String id = getExampleId(example);
		Path cacheFile = _cacheFolder.resolve(id);
		return cacheFile;
	}

	private String getExampleId(PhaserExampleModel example) throws IOException {
		Path exampleFile = example.getFilePath();
		Path relFile = _examplesProjectPath.resolve("phaser3-examples/public/src").relativize(exampleFile);
		String name = relFile.toString().replace(" ", "_").replace(".", "_").replace("/", "_");
		long t = Files.getLastModifiedTime(exampleFile).toMillis();
		String id = name + "-" + t;
		return id;
	}

	private void saveCache() throws JSONException, IOException {
		Path cache = _metadataProjectPath.resolve("phaser-custom/examples/examples-cache.json");
		model.saveCache(cache);

		// verify
		PhaserExamplesRepoModel newModel = new PhaserExamplesRepoModel(_examplesProjectPath);
		newModel.loadCache(cache);

		out.println("\n\n\n\n\n\n\n\n");

		for (PhaserExampleCategoryModel c : newModel.getExamplesCategories()) {
			c.printTree(0);
		}
	}

	private void visitCategory(PhaserExampleCategoryModel category) {
		for (PhaserExampleModel example : category.getTemplates()) {
			visitExample(example);
		}

		for (PhaserExampleCategoryModel subcategory : category.getSubCategories()) {
			visitCategory(subcategory);
		}
	}

	private void visitExample(PhaserExampleModel example) {
		_examples.add(example);
	}

	private Handler createLoggerHandler() {
		return new Handler() {

			@Override
			public void publish(LogRecord record) {
				String msg = record.getMessage();

				// out.println("URL: " + msg);

				int i = msg.indexOf("http://127.0.0.1:8080/plugins");

				if (i < 0) {
					i = msg.indexOf("http://127.0.0.1:8080/assets");
				}

				if (i > 0) {
					String url = msg.substring(i + "http://127.0.0.1:8080".length() + 1);
					try {
						addExampleMapping(url);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void flush() {
				//
			}

			@Override
			public void close() throws SecurityException {
				//
			}
		};
	}
}
