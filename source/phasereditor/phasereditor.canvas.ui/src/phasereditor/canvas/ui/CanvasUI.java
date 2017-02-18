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

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.subshell.snippets.jface.tooltip.tooltipsupport.ICustomInformationControlCreator;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TableViewerInformationProvider;
import com.subshell.snippets.jface.tooltip.tooltipsupport.Tooltips;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TreeViewerInformationProvider;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.preview.ExternalImageFileInformationControl;
import phasereditor.assetpack.ui.widgets.ImagePreviewComposite;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.PrefabReference;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ITextureChangeableControl;

/**
 * @author arian
 *
 */
public class CanvasUI {
	private static final int CANVAS_SCREENSHOT_SIZE = 256;
	public static final String PLUGIN_ID = "phasereditor.canvas.ui";

	public static void logError(Exception e) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	private static final QualifiedName SNAPSHOT_FILENAME_KEY = new QualifiedName("phasereditor.canvas.core",
			"snapshot-file");

	public static Map<IFile, List<PrefabReference>> findPrefabReferences(Prefab prefab) {

		IProject project = prefab.getFile().getProject();

		List<PrefabReference> refs = findPrefabReferencesInEditorsContent(prefab);

		Set<IFile> used = new HashSet<>();

		for (PrefabReference editor : refs) {
			used.add(editor.getFile());
		}

		for (CanvasFile cfile : CanvasCore.getCanvasFileCache().getProjectData(project)) {
			if (!used.contains(cfile.getFile())) {
				List<PrefabReference> thisRefs = CanvasCore.findPrefabReferencesInFileContent(prefab, cfile.getFile());
				refs.addAll(thisRefs);
			}
		}

		Map<IFile, List<PrefabReference>> map = new LinkedHashMap<>();

		for (PrefabReference ref : refs) {
			IFile file = ref.getFile();
			map.putIfAbsent(file, new ArrayList<>());
			map.get(file).add(ref);
		}

		return map;
	}

	public static List<PrefabReference> findPrefabReferencesInEditorsContent(Prefab prefab) {
		List<PrefabReference> result = new ArrayList<>();
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorRef : page.getEditorReferences()) {
					IEditorPart editor = editorRef.getEditor(false);
					if (editor != null && editor instanceof CanvasEditor) {
						CanvasEditor canvasEditor = (CanvasEditor) editor;
						List<PrefabReference> refs = CanvasCore.findPrefabReferenceInModelContent(prefab,
								canvasEditor.getModel().getWorld());
						result.addAll(refs);
					}
				}
			}
		}

		return result;
	}

	public static void changeSpriteTexture(IObjectNode sprite, Object texture, CompositeOperation operations) {

		Object frame;

		if (texture instanceof ImageAssetModel) {
			frame = ((ImageAssetModel) texture).getFrame();
		} else if (texture instanceof SpritesheetAssetModel) {
			frame = ((SpritesheetAssetModel) texture).getAllFrames().get(0);
		} else {
			frame = texture;
		}

		BaseSpriteModel oldModel = (BaseSpriteModel) sprite.getModel();
		JSONObject oldData = oldModel.toJSON(false);

		BaseSpriteModel newModel = ((ITextureChangeableControl) sprite.getControl())
				.createModelWithTexture((IAssetFrameModel) frame);

		if (oldModel.isPrefabInstance()) {
			newModel.setPrefab(oldModel.getPrefab());
		}

		newModel.setId(oldModel.getId());
		newModel.readInfo(oldData.getJSONObject("info"));
		JSONObject newData = newModel.toJSON(false);

		operations.add(new DeleteNodeOperation(oldModel.getId()));
		operations.add(new AddNodeOperation(newData, oldModel.getIndex(), oldModel.getX(), oldModel.getY(),
				oldModel.getParent().getId()));
	}

	public static void clearCanvasScreenshot(IFile file) {
		try {
			if (!file.exists()) {
				return;
			}

			String fname = file.getPersistentProperty(SNAPSHOT_FILENAME_KEY);
			if (fname == null) {
				return;
			}

			String home = System.getProperty("user.home");
			Path dir = Paths.get(home).resolve(".phasereditor/snapshots");
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

	public static Path getCanvasScreenshotFile(IFile file, boolean makeIfNotExist) {
		if (file == null) {
			return null;
		}

		try {
			String filename = file.getPersistentProperty(SNAPSHOT_FILENAME_KEY);
			String home = System.getProperty("user.home");
			Path dir = Paths.get(home).resolve(".phasereditor/snapshots");
			Path writeTo;
			if (filename == null) {
				filename = file.getName() + "_" + UUID.randomUUID().toString() + ".png";
			}
			writeTo = dir.resolve(filename);

			if (makeIfNotExist) {
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

	public static javafx.scene.image.Image makeCanvasScreenshot_FXImage(IFile file, int maxSize) {
		javafx.scene.image.Image[] result = new javafx.scene.image.Image[1];

		try (InputStream contents = file.getContents()) {
			CanvasModel model = new CanvasModel(file);
			model.read(new JSONObject(new JSONTokener(contents)));
			GroupControl worldControl = new GroupControl(null, model.getWorld());
			GroupNode node = worldControl.getNode();

			Scene scene = new Scene(node);

			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {

					try {
						Method m = Scene.class.getDeclaredMethod("doCSSLayoutSyncForSnapshot", Node.class);
						m.setAccessible(true);
						m.invoke(scene, node);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					SnapshotParameters params = new SnapshotParameters();
					params.setFill(Color.TRANSPARENT);
					node.setBackground(
							new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(0), new Insets(0))));

					Bounds b = SelectionBehavior.buildSelectionBounds(node.getChildren(), node);

					if (b != null) {
						// out.println("Bounds: " + b);
						double f = 1;
						double x = b.getMinX();
						double y = b.getMinY();
						double w = b.getWidth();
						double h = b.getHeight();

						double max = Math.max(w, h);
						if (max > maxSize) {
							f = maxSize / max;
						}

						params.setTransform(new Scale(f, f, x, y));
						params.setViewport(new Rectangle2D(x, y, w * f, h * f));
					}

					WritableImage image = node.snapshot(params, null);
					result[0] = image;
				}
			});
		} catch (IOException | CoreException e) {
			e.printStackTrace();
		}

		return result[0];
	}

	public static void makeCanvasScreenshot(IFile file, Path writeTo) {
		long t = currentTimeMillis();

		javafx.scene.image.Image fxImage = makeCanvasScreenshot_FXImage(file, CANVAS_SCREENSHOT_SIZE);

		BufferedImage buff = SwingFXUtils.fromFXImage(fxImage, null);

		try {
			ImageIO.write(buff, "png", writeTo.toFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		out.println(
				"Ready canvas snapshot src:" + file + " --> dst:" + writeTo + " " + (currentTimeMillis() - t) + "ms");
	}

	public static void installCanvasTooltips(TreeViewer viewer) {
		Tooltips.install(viewer.getControl(), new TreeViewerInformationProvider(viewer), getCanvasTooltipsCreators(),
				false);
	}

	public static void installCanvasTooltips(TableViewer viewer) {
		Tooltips.install(viewer.getControl(), new TableViewerInformationProvider(viewer), getCanvasTooltipsCreators(),
				false);
	}

	private static List<ICustomInformationControlCreator> getCanvasTooltipsCreators() {
		List<ICustomInformationControlCreator> creators = new ArrayList<>();

		creators.add(new ICustomInformationControlCreator() {

			@Override
			public IInformationControl createInformationControl(Shell parent) {
				ExternalImageFileInformationControl control = new ExternalImageFileInformationControl(parent) {

					@Override
					protected ImagePreviewComposite createContent2(Composite parentComp) {
						ImagePreviewComposite preview = super.createContent2(parentComp);
						preview.destroyResolutionLabel();
						return preview;
					}

					@Override
					public File getFileToDisplay(Object model) {
						IFile file = null;
						if (model instanceof CanvasFile) {
							file = ((CanvasFile) model).getFile();
						} else if (model instanceof IFile) {
							CanvasFile data = CanvasCore.getCanvasFileCache().getFileData((IFile) model);
							file = data.getFile();
						}

						if (file != null) {
							Path path = CanvasUI.getCanvasScreenshotFile(file, false);
							return path.toFile();
						}

						return super.getFileToDisplay(model);
					}
				};
				return control;
			}

			@Override
			public boolean isSupported(Object info) {
				if (info instanceof CanvasFile) {
					return true;
				}

				if (info instanceof IFile) {
					CanvasFile data = CanvasCore.getCanvasFileCache().getFileData((IFile) info);
					if (data != null) {
						return true;
					}
				}
				return false;
			}
		});
		return creators;
	}

	public static Image getCanvasFileIcon(CanvasFile canvasFile, AssetLabelProvider labelProvider) {
		IFile file = canvasFile.getFile();

		if (!file.exists()) {
			return null;
		}

		Path imgfile = CanvasUI.getCanvasScreenshotFile(file, false);
		return labelProvider.getIcon(imgfile.toAbsolutePath().toString());
	}

}
