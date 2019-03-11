// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONException;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.PhaserExampleCategoryModel;
import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.inspect.core.examples.PhaserExamplesRepoModel;

/**
 * @author arian
 *
 */
public class BuildExamplesCache {
	private static final int PORT = 1994;

	static Browser _browser;

	static Path _wsPath;
	static Path _examplesProjectPath;
	static Path _metadataProjectPath;
	static LinkedList<PhaserExampleModel> _examples;
	static private Path _cacheFolder;
	static private PhaserExampleModel _currentExample;
	static private PhaserExamplesRepoModel model;
	static private Path _examplesPath;

	static void processNewExample() throws Exception {

		if (_examples.isEmpty()) {
			examplesProcessingDone();
		} else {

			if (_currentExample != null) {
				saveExampleCache(_currentExample);
			}

			_currentExample = _examples.removeFirst();

			Path exampleFile = _currentExample.getFilePath();
			Path path = exampleFile;
			path = _examplesPath.relativize(path);
			_currentExample.addMapping(path.toString(), exampleFile.getFileName().toString());
			String url = "http://127.0.0.1:" + PORT + "/view-iframe.html?src=" + path;

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
				loadUrl(url);
			}
		}
	}

	private static void saveExampleCache(PhaserExampleModel example) throws IOException {
		Path cacheFile = getCacheFile(example);
		List<String> urls = example.getFilesMapping().stream()

				.map(m -> m.getOriginal().toString())

				.filter(url -> !url.startsWith("src/"))

				.collect(toList());
		Files.write(cacheFile, urls);
	}

	static private void examplesProcessingDone() throws Exception {
		out.println("DONE!");

		saveCache();

		exit(0);
	}

	synchronized static void addExampleMapping(String url) throws IOException {
		String url2 = decodeUrl(url);

		out.println("- Catching asset: " + url2);

		_currentExample.addMapping(url2, url2);
	}

	static boolean loadFromCache(PhaserExampleModel example) throws IOException {
		Path cacheFile = getCacheFile(example);

		if (Files.exists(cacheFile)) {
			List<String> urls = Files.readAllLines(cacheFile);

			for (String url : new HashSet<>(urls)) {
				url = decodeUrl(url);
				out.println("* Restore asset: " + url);
				_currentExample.addMapping(url, url);
			}

			return true;
		}

		return false;
	}

	private static String decodeUrl(String url) throws UnsupportedEncodingException {
		return java.net.URLDecoder.decode(url, "UTF-8");
	}

	private static Path getCacheFile(PhaserExampleModel example) throws IOException {
		String id = getExampleId(example);
		Path cacheFile = _cacheFolder.resolve(id);
		return cacheFile;
	}

	private static String getExampleId(PhaserExampleModel example) throws IOException {
		Path exampleFile = example.getFilePath();
		Path relFile = _examplesProjectPath.resolve("phaser3-examples/public/src").relativize(exampleFile);
		String name = relFile.toString().replace(" ", "_").replace(".", "_").replace("/", "_");
		// long t = Files.getLastModifiedTime(exampleFile).toMillis();
		var content = Files.readAllBytes(exampleFile);
		var md5 = DigestUtils.md5Hex(content);
		String id = name + "-" + md5;
		return id;
	}

	private static void saveCache() throws JSONException, IOException {
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

	private static void visitCategory(PhaserExampleCategoryModel category) {
		for (PhaserExampleModel example : category.getTemplates()) {
			visitExample(example);
		}

		for (PhaserExampleCategoryModel subcategory : category.getSubCategories()) {
			visitCategory(subcategory);
		}
	}

	private static void visitExample(PhaserExampleModel example) {
		_examples.add(example);
	}

	public static void main(String[] args) throws Exception {
		// init cache

		_cacheFolder = Paths.get(System.getProperty("user.home") + "/.phasereditor_dev/examples-cache");
		Files.createDirectories(_cacheFolder);

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

		startServer("/home/arian/Documents/Phaser/phaser3-examples/public");

		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());

		_browser = new Browser(shell, 0);

		ExecutorService pool = Executors.newCachedThreadPool();

		_browser.addProgressListener(new ProgressListener() {

			@Override
			public void completed(ProgressEvent event) {
				pool.execute(new Runnable() {
					@Override
					public void run() {

						try {
							Thread.sleep(1 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						try {
							processNewExample();
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				});
			}

			@Override
			public void changed(ProgressEvent event) {
				//
			}
		});

		pool.execute(new Runnable() {

			@Override
			public void run() {
				try {
					processNewExample();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	private static void loadUrl(String url) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				_browser.setUrl(url);
			}
		});

	}

	private static void startServer(String examplesPath) {
		Server _server = new Server(PORT);
		_server.setAttribute("useFileMappedBuffer", "false");
		HandlerList handlerList = new HandlerList();

		ContextHandler context = new ContextHandler("/");

		ResourceHandler resourceHandler = new ResourceHandler() {
			@Override
			public Resource getResource(String path) {
				// out.println("URL: " + path);

				if (path.startsWith("/assets") || path.startsWith("/plugin")) {
					try {
						addExampleMapping(path.substring(1));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				return super.getResource(path);
			}
		};
		resourceHandler.setCacheControl("no-store,no-cache,must-revalidate");
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(new String[] { "index.html" });
		resourceHandler.setResourceBase(examplesPath);
		context.setHandler(resourceHandler);
		handlerList.addHandler(context);

		_server.setHandler(handlerList);

		try {
			_server.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
