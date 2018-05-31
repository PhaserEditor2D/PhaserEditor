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
package phasereditor.inspect.core.build;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.NullProgressMonitor;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.ExamplesModel;

class BuildExamplesCache {
	public static void main(String[] args) throws IOException {
		Path wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		Path examplesProjectPath = wsPath.resolve(InspectCore.RESOURCES_EXAMPLES_PLUGIN);
		Path metadataProjectPath = wsPath.resolve(InspectCore.RESOURCES_METADATA_PLUGIN);

		{
			Path archivedFolder = examplesProjectPath.resolve("phaser3-examples/public/src/archived");
			if (Files.exists(archivedFolder)) {
				out.println("Error: the 'archived' folder is not going to be included.");
				System.exit(0);
			}
			
			archivedFolder = examplesProjectPath.resolve("phaser3-examples/public/src/transform/archived");
			if (Files.exists(archivedFolder)) {
				out.println("Error: the 'transform/archived' folder is not going to be included.");
				System.exit(0);
			}
		}

		ExamplesModel model = new ExamplesModel(examplesProjectPath);
		model.build(new NullProgressMonitor());
		Path cache = metadataProjectPath.resolve("phaser-custom/examples/examples-cache.json");
		model.saveCache(cache);

		// verify
		model = new ExamplesModel(examplesProjectPath);
		model.loadCache(cache);

		out.println("Finished!");
	}
}
