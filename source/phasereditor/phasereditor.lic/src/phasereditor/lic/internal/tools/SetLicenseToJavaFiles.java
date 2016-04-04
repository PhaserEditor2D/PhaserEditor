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

package phasereditor.lic.internal.tools;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class SetLicenseToJavaFiles {
	public static void main(String[] args) throws IOException {
		String lic = new String(Files.readAllBytes(Paths.get("LICENSE_COMMENT.TXT")));
		out.println(lic);

		Path root = Paths.get("../");
		root = root.toAbsolutePath().normalize();
		out.println("Visiting " + root);

		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				String name = dir.getFileName().toString();
				if (name.startsWith(".") || name.equals("bin") || name.equals("Documents") || name.equals("Export")
						|| name.equals("org.json")) {
					// out.println("Skip " + dir);
					return FileVisitResult.SKIP_SUBTREE;
				}
				// out.println("Enter " + dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String name = file.getFileName().toString();
				if (name.endsWith(".java")) {
					String content = new String(Files.readAllBytes(file));
					if (content.startsWith("// The MIT License (MIT)")) {
						out.println("Ready  " + file);
					} else {
						out.println("Process " + file);
						content = lic + content;
						Files.write(file, content.getBytes());
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
