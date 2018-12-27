// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.scene.ui;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.statushandlers.StatusManager;

import com.subshell.snippets.jface.tooltip.tooltipsupport.ICustomInformationControlCreator;
import com.subshell.snippets.jface.tooltip.tooltipsupport.Tooltips;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TreeViewerInformationProvider;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.ui.preview.ExternalImageFileInformationControl;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneFile;
import phasereditor.scene.core.SceneModel;
import phasereditor.ui.CanvasUtilsInformationControlProvider;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class SceneUI {

	private static final String PLUGIN_ID = Activator.PLUGIN_ID;
	private static final int SCENE_SCREENSHOT_SIZE = 256;
	private static final QualifiedName SNAPSHOT_FILENAME_KEY = new QualifiedName("phasereditor.scene.core",
			"snapshot-file");

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static void logError(String msg) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, msg, null));
	}

	public static void installSceneTooltips(CommonViewer viewer) {
		Tooltips.install(viewer.getControl(), new TreeViewerInformationProvider(viewer), getSceneTooltipsCreators(),
				false);
	}

	public static void installSceneTooltips(TreeCanvasViewer viewer) {
		Tooltips.install(viewer.getControl(), new CanvasUtilsInformationControlProvider(viewer.getTree().getUtils()),
				getSceneTooltipsCreators(), false);
	}

	private static List<ICustomInformationControlCreator> getSceneTooltipsCreators() {
		List<ICustomInformationControlCreator> creators = new ArrayList<>();

		creators.add(new ICustomInformationControlCreator() {

			@Override
			public IInformationControl createInformationControl(Shell parent) {
				var control = new ExternalImageFileInformationControl(parent) {

					@Override
					public File getFileToDisplay(Object model) {
						IFile file = null;
						if (model instanceof SceneFile) {
							file = ((SceneFile) model).getFile();
						} else if (model instanceof IFile) {
							var data = SceneCore.getSceneFileDataCache().getFileData((IFile) model);
							file = data.getFile();
						}

						if (file != null) {
							Path path = getSceneScreenshotFile(file, false);
							return path.toFile();
						}

						return super.getFileToDisplay(model);
					}
				};
				return control;
			}

			@Override
			public boolean isSupported(Object info) {
				if (info instanceof SceneFile) {
					return true;
				}

				if (info instanceof IFile) {
					var data = SceneCore.getSceneFileDataCache().getFileData((IFile) info);
					if (data != null) {
						return true;
					}
				}
				return false;
			}
		});
		return creators;
	}

	public static void clearSceneScreenshot(IFile file) {
		try {
			if (!file.exists()) {
				return;
			}

			String fname = file.getPersistentProperty(SNAPSHOT_FILENAME_KEY);
			if (fname == null) {
				return;
			}

			Path dir = ProjectCore.getUserCacheFolder().resolve("snapshots");
			Path snapshot = dir.resolve(fname);
			if (Files.exists(snapshot)) {
				out.println("Removing snapshot from " + file);
				Files.delete(snapshot);
			}
			file.setPersistentProperty(SNAPSHOT_FILENAME_KEY, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Path getSceneScreenshotFile(IFile file, boolean makeIfNotExist) {
		if (file == null) {
			return null;
		}

		try {
			String filename = file.getPersistentProperty(SNAPSHOT_FILENAME_KEY);
			Path dir = ProjectCore.getUserCacheFolder().resolve("snapshots");
			Path writeTo;
			if (filename == null) {
				filename = file.getName() + "_" + UUID.randomUUID().toString() + ".png";
			}
			writeTo = dir.resolve(filename);

			if (makeIfNotExist) {
				if (!Files.exists(writeTo)) {
					makeSceneScreenshot(file, writeTo);
				}
			}

			file.setPersistentProperty(SNAPSHOT_FILENAME_KEY, filename);

			return writeTo;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static class OfflineSceneRendererContext implements ISceneObjectRendererContext, Closeable {

		private AssetFinder _finder;
		private Device _device;
		private float _scale;

		public OfflineSceneRendererContext(AssetFinder finder, Device device, float scale) {
			_finder = finder;
			_device = device;
			_scale = scale;
		}

		@Override
		public AssetFinder getAssetFinder() {
			return _finder;
		}

		@Override
		public int getOffsetX() {
			return 0;
		}

		@Override
		public int getOffsetY() {
			return 0;
		}

		@Override
		public float getScale() {
			return _scale;
		}

		public void setScale(float scale) {
			_scale = scale;
		}

		@Override
		public Device getDevice() {
			return _device;
		}

		@Override
		public void close() {
			//
		}
	}

	public static Image makeSceneScreenshot_SWTImage(IFile file, int maxSize) {
		try {
			var display = PlatformUI.getWorkbench().getDisplay();

			var model = new SceneModel();
			model.read(file);

			var finder = AssetPackCore.getAssetFinder(file.getProject());

			Image img = null;

			try (var context = new OfflineSceneRendererContext(finder, display, 1)) {

				var renderer = new SceneObjectRenderer(context);

				// just for now, until we do a better job with position and dimension
				// computations
				var width = model.getBorderWidth();
				var height = model.getBorderHeight();

				var max = Math.max(width, height);
				var scale = 1f;
				if (max > maxSize) {
					scale = (float) maxSize / max;
				}

				context.setScale(scale);

				width *= scale;
				height *= scale;

				img = new Image(context.getDevice(), width, height);

				var gc = new GC(img);
				var tx = new Transform(gc.getDevice());

				// display.syncExec(() -> {
				renderer.renderScene(gc, tx, model);
				// });

			}

			return img;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void makeSceneScreenshot(IFile file, Path writeTo) {
		long t = currentTimeMillis();

		var image = makeSceneScreenshot_SWTImage(file, SCENE_SCREENSHOT_SIZE);

		var loader = new ImageLoader();

		try {
			Files.createDirectories(writeTo.getParent());
			loader.data = new ImageData[] { image.getImageData() };
			loader.save(writeTo.toString(), SWT.IMAGE_PNG);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			image.dispose();
		}

		out.println(
				"Ready scene snapshot src:" + file + " --> dst:" + writeTo + " " + (currentTimeMillis() - t) + "ms");
	}

}
