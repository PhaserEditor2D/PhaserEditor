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
package phasereditor.canvas.ui;

import static java.lang.System.out;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONObject;
import org.json.JSONTokener;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;

/**
 * @author arian
 *
 */
public class CanvasUI {
	public static final String PLUGIN_ID = "phasereditor.canvas.ui";

	public static void logError(Exception e) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	private static final QualifiedName SNAPSHOT_FILENAME_KEY = new QualifiedName("phasereditor.canvas.core",
			"snapshot-file");

	public synchronized static Path getCanvasScreenshotFile(IFile file, boolean forceMake) {
		if (file == null) {
			return null;
		}

		try {
			String filename = file.getPersistentProperty(SNAPSHOT_FILENAME_KEY);
			String home = System.getProperty("user.home");
			Path dir = Paths.get(home).resolve(".phasereditor/snapshots");
			Path writeTo;
			if (filename == null) {
				filename = UUID.randomUUID().toString() + ".jpg";
			}
			writeTo = dir.resolve(filename);

			if (forceMake) {
				if (!Files.exists(writeTo)) {
					makeCanvasScreenshot(file, writeTo);
				}
			}

			file.setPersistentProperty(SNAPSHOT_FILENAME_KEY, filename);

			return writeTo;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void makeCanvasScreenshot(IFile file, @SuppressWarnings("unused") Path writeTo) {
		CanvasModel model = new CanvasModel(file);
		try (InputStream contents = file.getContents()) {
			model.read(new JSONObject(new JSONTokener(contents)));
			GroupControl worldControl = new GroupControl(null, model.getWorld());
			GroupNode node = worldControl.getNode();
			WritableImage image = new WritableImage(1000, 1000);
			SnapshotParameters params = new SnapshotParameters();
			// TODO: set the transform to scale the image.
			// params.setTransform(null);
			node.snapshot(params, image);
			out.println(image);
		} catch (IOException | CoreException e) {
			e.printStackTrace();
		}
	}
}
