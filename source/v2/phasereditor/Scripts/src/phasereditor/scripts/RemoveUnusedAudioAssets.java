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

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import phasereditor.inspect.core.InspectCore;

/**
 * @author arian
 *
 */
public class RemoveUnusedAudioAssets {
	public static void main(String[] args) throws Exception {

		// collect all examples code

		StringBuilder sb = new StringBuilder();

		Path wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		Path examplesPath = wsPath.resolve(InspectCore.RESOURCES_EXAMPLES_PLUGIN).resolve("phaser3-examples/public/");
		Path examplesAudioPath = wsPath.resolve(InspectCore.RESOURCES_EXAMPLES_AUDIO_PLUGIN)
				.resolve("phaser3-examples/public/assets/");

		Files.walk(examplesPath)

				.filter(path -> !Files.isDirectory(path)

						&& (path.toString().endsWith(".js") || path.toString().endsWith(".json")))

				.forEach(path -> {

					try {
						List<String> lines = Files.readAllLines(path);
						for (String line : lines) {
							sb.append(line);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				});

		String allCode = sb.toString();

		int[] total = { 0 };
		int[] toDelete = { 0 };
		long[] toDeleteSize = { 0 };

		Files.walk(examplesAudioPath).forEach(path -> {
			try {

				String name = path.getFileName().toString();
				total[0]++;
				int last = name.lastIndexOf(".");
				if (last >= 0) {
					name = name.substring(0, last);

					var contains = false;

					for (var str : new String[] { "'" + name, "\"" + name, "/" + name, name + "'", name + "\"" }) {
						if (allCode.contains(str)) {
							contains = true;
							break;
						}
					}

					if (contains) {
						out.println("Keep " + path.getFileName());
					} else {
						out.println("Delete " + path.getFileName());
						toDeleteSize[0] += Files.size(path);
						 Files.delete(path);
						toDelete[0]++;
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		out.println();
		out.println("Deleted " + toDelete[0] + " of " + total[0] + " = " + ((int) ((double) toDelete[0] / 1024 * 1024))
				+ "MB released");

	}
}
