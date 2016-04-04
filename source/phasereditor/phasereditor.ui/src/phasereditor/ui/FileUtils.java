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
package phasereditor.ui;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

/**
 * Utility class to execute third-party programs.
 * 
 * @author arian
 *
 */
public class FileUtils {

	private static final String OS_EXEC_EXT = SWT.getPlatform().startsWith("win") ? ".exe" : "";

	public static String readStream(InputStream stream) {
		try {
			StringBuilder sb = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "/n");
			}
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void readStream(InputStream stream, Consumer<String> logger) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = reader.readLine()) != null) {
				logger.accept(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static ProcessBuilder createProcessBuilder(String pluginId, String exePath, String... args) {
		try {
			Path execPath;
			URL url = new URL("platform:/plugin/" + pluginId + "/" + exePath + OS_EXEC_EXT);
			URL fileUrl = FileLocator.toFileURL(url);
			File file = new File(fileUrl.getFile());
			execPath = file.toPath().normalize();
			List<String> list = new ArrayList<>();
			list.add(execPath.toAbsolutePath().toString());
			list.addAll(Arrays.asList(args));
			ProcessBuilder pb = new ProcessBuilder(list);

			return pb;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Uncompress the given zip into the EPT install folder
	 * ({user.home}/.ept/pacman/{component}). It forks if the given "done"
	 * callback is not null.
	 * 
	 * @param component
	 *            A symbolic name of the component to install. It is used to
	 *            create the folder.
	 * @param zipUrl
	 * @param done
	 *            A callback with the installation path. May be null, in that
	 *            case it does not fork.
	 * @throws MalformedURLException
	 */
	public static Path installZip(String component, String zipUrl, Consumer<Path> done) throws Exception {
		Path eptHome = getEPTHome();
		Path compDir = eptHome.resolve("pacman/" + component);
		if (Files.exists(compDir)) {
			if (done != null) {
				done.accept(compDir);
			}
		} else {
			ProgressMonitorDialog dlg = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
			boolean fork = done != null;
			dlg.run(fork, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						long t = currentTimeMillis();
						URL url = new URL(zipUrl);
						int total;
						try (InputStream input = url.openConnection().getInputStream()) {
							total = countUnzipSteps(input);
						}
						out.println("Counting total delay " + (currentTimeMillis() - t));

						monitor.beginTask("Installing '" + component + "' at '" + compDir + "'", total);
						// install only once :)

						Files.createDirectories(compDir);
						try (InputStream input = url.openConnection().getInputStream()) {
							unzip(input, compDir, true, monitor);
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					} finally {
						monitor.done();
					}
					if (fork) {
						done.accept(compDir);
					}
				}
			});
		}

		return compDir;
	}

	private static Path getEPTHome() {
		return Paths.get(System.getProperty("user.home"), ".ept");
	}

	static void unzip(InputStream zipInput, Path dstDir, boolean ignoreRootFolder, IProgressMonitor monitor)
			throws IOException {
		out.println("Unzipping " + zipInput + "... ");
		long t = System.currentTimeMillis();
		byte[] buffer = new byte[1024];
		Files.createDirectories(dstDir);

		try (ZipInputStream zis = new ZipInputStream(zipInput)) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				if (!ze.isDirectory()) {
					Path fileName = Paths.get(ze.getName());
					if (ignoreRootFolder) {
						fileName = fileName.getName(0).relativize(fileName);
					}
					Path newFile = dstDir.resolve(fileName);

					Files.createDirectories(newFile.getParent());

					try (FileOutputStream fos = new FileOutputStream(newFile.toFile())) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
				}
				monitor.worked(1);
				ze = zis.getNextEntry();
			}
			zis.closeEntry();
		}
		t = (System.currentTimeMillis() - t) / 1000;
		out.println(t / 60 + " min, " + (t % 60) + " sec/n");
	}

	static int countUnzipSteps(InputStream zipInput) throws IOException {
		int total = 0;
		try (ZipInputStream zis = new ZipInputStream(zipInput)) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				ze = zis.getNextEntry();
				total++;
			}
			zis.closeEntry();
		}
		return total;
	}
}
