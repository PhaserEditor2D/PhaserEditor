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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import javax.imageio.ImageIO;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
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
import javafx.embed.swt.FXCanvas;
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
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.IAssetReference;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.preview.ExternalImageFileInformationControl;
import phasereditor.assetpack.ui.widgets.ImagePreviewComp;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.PrefabReference;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ITextureChangeableControl;
import phasereditor.project.core.ProjectCore;

/**
 * @author arian
 *
 */
public class CanvasUI {
	private static final int CANVAS_SCREENSHOT_SIZE = 256;
	public static final String PLUGIN_ID = "phasereditor.canvas.ui";
	private static boolean _fxStarted = false;
	
	public static final String PREF_PROP_CANVAS_SHORTCUT_PANE_POSITION = "phasereditor.canvas.ui.shortcuts.position";
	public static final String PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_TOP_LEFT = "Top-Left";
	public static final String PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_TOP_RIGHT = "Top-Right";
	public static final String PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_BOTTOM_LEFT = "Bottom-Left";
	public static final String PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_BOTTOM_RIGHT = "Bottom-Right";
	public static final String PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_NEXT_TO_OBJECT = "Next-To-Object";
	
	
	public static final String PREF_PROP_CANVAS_SHORTCUT_PANE_BG_COLOR = "phasereditor.canvas.ui.shortcuts.bgColor";
	
	public static String get_pref_Canvas_Shortcuts_position() {
		return getPreferenceStore().getString(PREF_PROP_CANVAS_SHORTCUT_PANE_POSITION);
	}
	
	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public static void logError(Exception e) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	private static final QualifiedName SNAPSHOT_FILENAME_KEY = new QualifiedName("phasereditor.canvas.core",
			"snapshot-file");

	public static class FindPrefabReferencesResult {

		private Map<IFile, List<PrefabReference>> _mapFileList;
		private Map<IFile, Set<String>> _mapFileSet;
		private int _total;

		public FindPrefabReferencesResult() {
			_mapFileList = new LinkedHashMap<>();
			_mapFileSet = new HashMap<>();
			_total = 0;
		}

		public void add(PrefabReference ref) {
			IFile file = ref.getFile();

			_mapFileList.putIfAbsent(file, new ArrayList<>());
			_mapFileSet.putIfAbsent(file, new HashSet<>());

			Set<String> set = _mapFileSet.get(file);

			String id = ref.getFile().getFullPath().toPortableString() + "@" + ref.getObjectId();

			if (!set.contains(id)) {
				set.add(id);
				List<PrefabReference> list = _mapFileList.get(file);
				list.add(ref);
				_total++;
			}
		}

		public int getTotalReferences() {
			return _total;
		}

		public int getTotalFiles() {
			return _mapFileList.size();
		}

		public List<PrefabReference> getReferencesOf(IFile file) {
			return _mapFileList.get(file);
		}

		public Set<IFile> getFiles() {
			return _mapFileList.keySet();
		}

		public void addAll(List<PrefabReference> refs) {
			for (PrefabReference ref : refs) {
				add(ref);
			}
		}

		public void merge(FindPrefabReferencesResult result) {
			for (List<PrefabReference> refs : result._mapFileList.values()) {
				addAll(refs);
			}
		}

		public PrefabReference getFirstReference() {
			for (IFile file : _mapFileList.keySet()) {
				List<PrefabReference> list = _mapFileList.get(file);
				if (!list.isEmpty()) {
					return list.get(0);
				}
			}
			return null;
		}
	}

	public static FindAssetReferencesResult findAllKeyAssetReferences(IAssetKey assetKey, IProgressMonitor monitor) {
		return findAllAssetElementReferences(assetKey, CanvasUI::findAssetKeyReferencesInEditorsContent,
				CanvasCore::findAssetKeyReferencesInFileContent, monitor);
	}

	public static FindAssetReferencesResult findAllAssetReferences(AssetModel asset, IProgressMonitor monitor) {
		return findAllAssetElementReferences(asset,
				(key, pm) -> CanvasUI.findAssetReferencesInEditorsContent((AssetModel) key, pm),
				(key, file) -> CanvasCore.findAssetReferencesInFileContent(asset, file), monitor);
	}

	private static FindAssetReferencesResult findAllAssetElementReferences(

			IAssetKey assetKey,

			BiFunction<IAssetKey, IProgressMonitor, List<IAssetReference>> findInEditorMethod,

			BiFunction<IAssetKey, IFile, List<IAssetReference>> findInFileMethod,

			IProgressMonitor monitor) {
		FindAssetReferencesResult result = new FindAssetReferencesResult();

		monitor.beginTask("Finding prefab references", CANVAS_SCREENSHOT_SIZE);

		IProject project = assetKey.getAsset().getPack().getFile().getProject();

		List<IAssetReference> refs = findInEditorMethod.apply(assetKey, monitor); // findAssetKeyReferencesInEditorsContent(assetKey,
																					// monitor);
		result.addAll(refs);

		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(project);

		monitor.beginTask("Find prefab references in files", cfiles.size());

		for (CanvasFile cfile : cfiles) {
			List<IAssetReference> fileRefs = findInFileMethod.apply(assetKey, cfile.getFile()); // CanvasCore.findAssetKeyReferencesInFileContent(assetKey,
																								// cfile.getFile());
			result.addAll(fileRefs);
			monitor.worked(1);
		}

		return result;
	}

	public static List<PrefabReference> findPrefabReferencesInEditorsContent(Prefab prefab, IProgressMonitor monitor) {
		List<IEditorReference> editors = new ArrayList<>();

		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorRef : page.getEditorReferences()) {
					editors.add(editorRef);
				}
			}
		}

		monitor.beginTask("Find prefab refrencesin editors", editors.size());

		List<PrefabReference> result = new ArrayList<>();

		for (IEditorReference editorRef : editors) {
			IEditorPart editor = editorRef.getEditor(false);
			if (editor != null && editor instanceof CanvasEditor) {
				CanvasEditor canvasEditor = (CanvasEditor) editor;
				List<PrefabReference> refs = CanvasCore.findPrefabReferenceInModelContent(prefab,
						canvasEditor.getModel().getWorld());
				result.addAll(refs);
			}
			monitor.worked(1);
		}

		return result;
	}

	public static FindPrefabReferencesResult findAllPrefabReferences(Prefab prefab, IProgressMonitor monitor) {
		FindPrefabReferencesResult result = new FindPrefabReferencesResult();

		monitor.beginTask("Finding prefab references", CANVAS_SCREENSHOT_SIZE);

		IProject project = prefab.getFile().getProject();

		List<PrefabReference> refs = findPrefabReferencesInEditorsContent(prefab, monitor);
		result.addAll(refs);

		List<CanvasFile> cfiles = CanvasCore.getCanvasFileCache().getProjectData(project);

		monitor.beginTask("Find prefab references in files", cfiles.size());

		for (CanvasFile cfile : cfiles) {
			List<PrefabReference> fileRefs = CanvasCore.findPrefabReferencesInFileContent(prefab, cfile.getFile());
			result.addAll(fileRefs);
			monitor.worked(1);
		}

		return result;
	}

	public static List<IAssetReference> findAssetKeyReferencesInEditorsContent(IAssetKey assetKey,
			IProgressMonitor monitor) {
		return findAssetElementReferencesInEditorsContent(assetKey, CanvasCore::findAssetKeyReferenceInModelContent,
				monitor);
	}

	public static List<IAssetReference> findAssetReferencesInEditorsContent(AssetModel asset,
			IProgressMonitor monitor) {
		return findAssetElementReferencesInEditorsContent(asset, (assetKey, model) -> {
			return CanvasCore.findAssetReferenceInModelContent((AssetModel) assetKey, model);
		}, monitor);
	}

	private static <T extends IAssetKey> List<IAssetReference> findAssetElementReferencesInEditorsContent(T assetKey,
			BiFunction<IAssetKey, WorldModel, List<IAssetReference>> findInModelMethod, IProgressMonitor monitor) {
		List<IEditorReference> editors = new ArrayList<>();

		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorRef : page.getEditorReferences()) {
					editors.add(editorRef);
				}
			}
		}

		monitor.beginTask("Find asset refrences in editors", editors.size());

		List<IAssetReference> result = new ArrayList<>();

		for (IEditorReference editorRef : editors) {
			IEditorPart editor = editorRef.getEditor(false);
			if (editor != null && editor instanceof CanvasEditor) {
				CanvasEditor canvasEditor = (CanvasEditor) editor;
				List<IAssetReference> refs = findInModelMethod.apply(assetKey, canvasEditor.getModel().getWorld());
				result.addAll(refs);
			}
			monitor.worked(1);
		}

		return result;
	}

	public static void changeSpriteTexture(BaseObjectModel model, IAssetKey texture) {
		JSONObject data = model.toJSON(false);

		CanvasModelFactory.changeTextureToObjectData(data, texture);

		GroupModel parent = model.getParent();

		BaseObjectModel newModel = CanvasModelFactory.createModel(parent, data);

		int i = model.getIndex();

		parent.removeChild(model);

		parent.addChild(i, newModel);
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

	public static Path getCanvasScreenshotFile(IFile file, boolean makeIfNotExist) {
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
		
		if (!_fxStarted) {
			Display.getDefault().syncExec(new Runnable() {
				
				@SuppressWarnings("unused")
				@Override
				public void run() {
					new FXCanvas(new Shell(Display.getDefault()), 0);
				}
			});
			_fxStarted = true;
		}
		
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
			Files.createDirectories(writeTo.getParent());
			
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
					protected ImagePreviewComp createContent2(Composite parentComp) {
						ImagePreviewComp preview = super.createContent2(parentComp);
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

		return getCanvasFileIcon(file, labelProvider);
	}

	public static Image getCanvasFileIcon(IFile file, AssetLabelProvider labelProvider) {
		if (!file.exists()) {
			return null;
		}

		Path imgfile = CanvasUI.getCanvasScreenshotFile(file, false);

		return labelProvider.getIcon(imgfile.toAbsolutePath().toString());
	}
}
