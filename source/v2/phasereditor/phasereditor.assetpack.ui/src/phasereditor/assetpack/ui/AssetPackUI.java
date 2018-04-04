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
package phasereditor.assetpack.ui;

import static java.lang.System.arraycopy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONArray;
import org.json.JSONObject;

import com.subshell.snippets.jface.tooltip.tooltipsupport.ICustomInformationControlCreator;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TableViewerInformationProvider;
import com.subshell.snippets.jface.tooltip.tooltipsupport.Tooltips;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TreeViewerInformationProvider;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.FrameData;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.PhysicsAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.VideoAssetModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor;
import phasereditor.assetpack.ui.preview.AtlasAssetInformationControl;
import phasereditor.assetpack.ui.preview.AtlasFrameInformationControl;
import phasereditor.assetpack.ui.preview.AudioAssetInformationControl;
import phasereditor.assetpack.ui.preview.AudioFileInformationControl;
import phasereditor.assetpack.ui.preview.AudioSpriteAssetElementInformationControl;
import phasereditor.assetpack.ui.preview.AudioSpriteAssetInformationControl;
import phasereditor.assetpack.ui.preview.BitmapFontAssetInformationControl;
import phasereditor.assetpack.ui.preview.ImageAssetInformationControl;
import phasereditor.assetpack.ui.preview.ImageFileInformationControl;
import phasereditor.assetpack.ui.preview.OtherAssetInformationControl;
import phasereditor.assetpack.ui.preview.PhysicsAssetInformationControl;
import phasereditor.assetpack.ui.preview.SpritesheetAssetInformationControl;
import phasereditor.assetpack.ui.preview.TilemapCSVAssetInformationControl;
import phasereditor.assetpack.ui.preview.TilemapJSONAssetInformationControl;
import phasereditor.assetpack.ui.preview.TilemapTilesetInformationControl;
import phasereditor.assetpack.ui.preview.VideoAssetInformationControl;
import phasereditor.assetpack.ui.preview.VideoFileInformationControl;
import phasereditor.assetpack.ui.refactorings.AssetDeleteProcessor;
import phasereditor.assetpack.ui.refactorings.AssetDeleteWizard;
import phasereditor.assetpack.ui.refactorings.AssetMoveProcessor;
import phasereditor.assetpack.ui.refactorings.AssetMoveWizard;
import phasereditor.assetpack.ui.refactorings.AssetRenameProcessor;
import phasereditor.assetpack.ui.refactorings.AssetRenameWizard;
import phasereditor.assetpack.ui.refactorings.AssetSectionRenameProcessor;
import phasereditor.assetpack.ui.refactorings.BaseAssetRenameProcessor;
import phasereditor.assetpack.ui.widgets.AudioResourceDialog;
import phasereditor.assetpack.ui.widgets.ImageResourceDialog;
import phasereditor.assetpack.ui.widgets.VideoResourceDialog;
import phasereditor.audio.core.AudioCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.PhaserEditorUI;

public class AssetPackUI {

	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	private static List<ICustomInformationControlCreator> _informationControlCreators;

	public static void launchMoveWizard(IStructuredSelection selection) {
		Object[] selarray = selection.toArray();

		AssetModel[] assets = new AssetModel[selarray.length];

		arraycopy(selarray, 0, assets, 0, selarray.length);

		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActivePart();

		AssetPackEditor editor = activePart instanceof AssetPackEditor ? (AssetPackEditor) activePart : null;

		AssetMoveWizard wizard = new AssetMoveWizard(new MoveRefactoring(new AssetMoveProcessor(assets, editor)));

		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Delete Asset");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static void launchRenameWizard(Object element) {
		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActivePart();

		AssetPackEditor editor = activePart instanceof AssetPackEditor ? (AssetPackEditor) activePart : null;

		BaseAssetRenameProcessor processor;
		if (element instanceof AssetModel) {
			processor = new AssetRenameProcessor((AssetModel) element, editor);
		} else {
			processor = new AssetSectionRenameProcessor((AssetSectionModel) element, editor);
		}

		AssetRenameWizard wizard = new AssetRenameWizard(new RenameRefactoring(processor));

		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Asset");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	public static void launchDeleteWizard(Object[] selection) {

		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActivePart();

		AssetPackEditor editor = activePart instanceof AssetPackEditor ? (AssetPackEditor) activePart : null;

		AssetDeleteWizard wizard = new AssetDeleteWizard(
				new DeleteRefactoring(new AssetDeleteProcessor(selection, editor)));

		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Delete Asset");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<AssetModel> findAssetResourceReferencesInEditors(IFile assetFile) {
		List<AssetModel> list = new ArrayList<>();

		PhaserEditorUI.forEachEditor(editor -> {
			if (editor instanceof AssetPackEditor) {
				AssetPackModel pack = ((AssetPackEditor) editor).getModel();
				list.addAll(AssetPackCore.findAssetResourceReferencesInPack(assetFile, pack));
			}
		});

		return list;
	}

	public static List<AssetModel> findAssetResourceReferences(IFile file) {
		List<AssetModel> list1 = AssetPackCore.findAssetResourceReferencesInProject(file);
		List<AssetModel> list2 = AssetPackUI.findAssetResourceReferencesInEditors(file);
		List<AssetModel> result = new ArrayList<>();

		Set<String> used = new HashSet<>();

		Consumer<AssetModel> consumer = (asset) -> {
			String id = AssetPackCore.getAssetStringReference(asset);
			if (!used.contains(id)) {
				used.add(id);
				result.add(asset);
			}
		};

		list1.stream().forEach(consumer);
		list2.stream().forEach(consumer);

		return result;
	}

	/**
	 * Open the given element in an asset pack editor.
	 * 
	 * @param elem
	 *            An asset pack element (section, group, asset, etc..)
	 */
	public static boolean openElementInEditor(Object elem) {
		if (elem == null) {
			return false;
		}

		AssetPackModel pack = null;
		if (elem instanceof AssetModel) {
			pack = ((AssetModel) elem).getPack();
		} else if (elem instanceof AssetSectionModel) {
			pack = ((AssetSectionModel) elem).getPack();
		} else if (elem instanceof AssetPackModel) {
			pack = (AssetPackModel) elem;
		} else if (elem instanceof IAssetElementModel) {
			pack = ((IAssetKey) elem).getAsset().getPack();
		} else {
			return false;
		}

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			AssetPackEditor editor = (AssetPackEditor) page.openEditor(new FileEditorInput(pack.getFile()),
					AssetPackEditor.ID);
			if (editor != null) {
				JSONObject ref = pack.getAssetJSONRefrence(elem);
				Object elem2 = editor.getModel().getElementFromJSONReference(ref);
				if (elem2 != null) {
					editor.revealElement(elem2);
				}
			}
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	public static String browseAssetFile(AssetPackModel packModel, String objectName, IFile curFile, List<IFile> files,
			Shell shell, Consumer<String> action) {

		Set<IFile> usedFiles = packModel.sortFilesByNotUsed(files);

		// ok, but we want to put the current file at the head of the list
		if (curFile != null && files.contains(curFile)) {
			files.remove(curFile);
			files.add(0, curFile);
		}

		IFile result = null;

		IFile initial = curFile;
		if (initial == null && !files.isEmpty()) {
			initial = files.get(0);
		}

		ListDialog dlg = new ListDialog(shell);
		dlg.setTitle(objectName);
		dlg.setMessage("Select the " + objectName + " path. Those in bold are not used.");
		dlg.setLabelProvider(createFilesLabelProvider(usedFiles, shell));
		dlg.setContentProvider(new ArrayContentProvider());
		dlg.setInput(files);

		if (initial != null) {
			dlg.setInitialSelections(new Object[] { initial });
		}

		if (dlg.open() == Window.OK && dlg.getResult().length > 0) {
			result = (IFile) dlg.getResult()[0];
			if (result != null) {
				String path = ProjectCore.getAssetUrl(result);
				action.accept(path);
				return path;
			}
		}

		return "";
	}

	public static String browseImageUrl(AssetPackModel packModel, String objectName, IFile curImageFile,
			List<IFile> imageFiles, Shell shell, Consumer<String> action) {

		Set<IFile> usedFiles = packModel.sortFilesByNotUsed(imageFiles);

		// ok, but we want to put the current file at the head of the list
		if (curImageFile != null && imageFiles.contains(curImageFile)) {
			imageFiles.remove(curImageFile);
			imageFiles.add(0, curImageFile);
		}

		IFile result = null;

		IFile initial = curImageFile;
		if (initial == null && !imageFiles.isEmpty()) {
			initial = imageFiles.get(0);
		}

		ImageResourceDialog dlg = new ImageResourceDialog(shell);
		dlg.setLabelProvider(createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(imageFiles);
		dlg.setObjectName(objectName);
		if (initial != null) {
			dlg.setInitial(initial);
		}

		if (dlg.open() == Window.OK) {
			result = (IFile) dlg.getSelection();
			if (result != null) {
				String path = ProjectCore.getAssetUrl(result);
				action.accept(path);
				return path;
			}
		}

		return "";
	}

	public static String browseAudioUrl(AssetPackModel packModel, List<IFile> curAudioFiles, List<IFile> audioFiles,
			Shell shell, Consumer<String> action) {

		// Set<IFile> usedFiles = packModel.sortFilesByNotUsed(audioFiles);
		Set<IFile> usedFiles = packModel.findUsedFiles();

		// remove from the current files those are not part of the available
		// files
		for (IFile file : new ArrayList<>(curAudioFiles)) {
			if (!audioFiles.contains(file)) {
				curAudioFiles.remove(file);
			}
		}

		List<IFile> initialFiles = curAudioFiles;
		if (initialFiles == null && !audioFiles.isEmpty()) {
			initialFiles = new ArrayList<>(Arrays.asList(audioFiles.get(0)));
		}

		AudioResourceDialog dlg = new AudioResourceDialog(shell);
		dlg.setLabelProvider(createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(audioFiles);

		if (initialFiles != null) {
			dlg.setInitialFiles(initialFiles);
		}

		if (dlg.open() == Window.OK) {
			List<IFile> selection = dlg.getSelection();
			if (selection != null) {
				JSONArray array = new JSONArray();
				for (IFile file : selection) {
					String url = ProjectCore.getAssetUrl(file);
					array.put(url);
				}
				String json = array.toString();
				action.accept(json);
				return json;
			}
		}

		return "";
	}

	public static String browseVideoUrl(AssetPackModel packModel, List<IFile> curVideoFiles, List<IFile> videoFiles,
			Shell shell, Consumer<String> action) {

		Set<IFile> usedFiles = packModel.findUsedFiles();

		// remove from the current files those are not part of the available
		// files
		for (IFile file : new ArrayList<>(curVideoFiles)) {
			if (!videoFiles.contains(file)) {
				curVideoFiles.remove(file);
			}
		}

		List<IFile> initialFiles = curVideoFiles;
		if (initialFiles == null && !videoFiles.isEmpty()) {
			initialFiles = new ArrayList<>(Arrays.asList(videoFiles.get(0)));
		}

		VideoResourceDialog dlg = new VideoResourceDialog(shell);
		dlg.setLabelProvider(createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(videoFiles);

		if (initialFiles != null) {
			dlg.setInitialFiles(initialFiles);
		}

		if (dlg.open() == Window.OK) {
			List<IFile> selection = dlg.getSelection();
			if (selection != null) {
				JSONArray array = new JSONArray();
				for (IFile file : selection) {
					String url = ProjectCore.getAssetUrl(file);
					array.put(url);
				}
				String json = array.toString();
				action.accept(json);
				return json;
			}
		}

		return "";
	}

	private static LabelProvider createFilesLabelProvider(Set<IFile> usedFiles, Shell shell) {
		class FilesLabelProvider extends LabelProvider implements IFontProvider {

			private WorkbenchLabelProvider _baseLabels = new WorkbenchLabelProvider();

			@Override
			public void dispose() {
				super.dispose();
				_baseLabels.dispose();
			}

			@Override
			public Image getImage(Object element) {
				return _baseLabels.getImage(element);
			}

			@Override
			public Font getFont(Object element) {
				Font font = shell.getFont();
				if (usedFiles.contains(element)) {
					return font;
				}
				font = SWTResourceManager.getBoldFont(font);
				return font;
			}

			@Override
			public String getText(Object element) {
				return ProjectCore.getAssetUrl((IFile) element);
			}
		}
		return new FilesLabelProvider();
	}

	public static List<FrameData> generateSpriteSheetRects(SpritesheetAssetModel s, Rectangle src) {

		List<FrameData> list = new ArrayList<>();

		int w = s.getFrameWidth();
		int h = s.getFrameHeight();
		int margin = s.getMargin();
		int spacing = s.getSpacing();

		if (w <= 0 || h <= 0 || spacing < 0 || margin < 0) {
			// invalid parameters
			return list;
		}

		int max = s.getFrameMax();
		if (max <= 0) {
			max = Integer.MAX_VALUE;
		}

		int i = 0;
		int x = margin;
		int y = margin;
		while (true) {
			if (i >= max || y >= src.height) {
				break;
			}

			FrameData fd = new FrameData();
			fd.src = new Rectangle(x, y, w, h);
			fd.dst = fd.src;

			list.add(fd);

			x += w + spacing;
			if (x >= src.width) {
				x = margin;
				y += h + spacing;
			}
			i++;
		}
		return list;
	}

	public static void installAssetTooltips(TreeViewer viewer) {
		List<ICustomInformationControlCreator> creators = getInformationControlCreatorsForTooltips();

		Tooltips.install(viewer.getControl(), new TreeViewerInformationProvider(viewer), creators, false);
	}

	public static void installAssetTooltips(TableViewer viewer) {
		List<ICustomInformationControlCreator> creators = getInformationControlCreatorsForTooltips();

		Tooltips.install(viewer.getControl(), new TableViewerInformationProvider(viewer), creators, false);
	}

	public static List<ICustomInformationControlCreator> getInformationControlCreatorsForTooltips() {
		if (_informationControlCreators == null) {
			_informationControlCreators = new ArrayList<>();

			// FILES

			// image file

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new ImageFileInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					if (info instanceof IFile) {
						return AssetPackCore.isImage((IFile) info);
					}
					return false;
				}
			});

			// video file

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new VideoFileInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info != null && info instanceof IFile && AssetPackCore.isVideo((IResource) info);
				}
			});

			// audio file

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AudioFileInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					if (info instanceof IFile) {
						return AudioCore.isSupportedAudio((IFile) info);
					}
					return false;
				}
			});

			// ASSET MODELS

			// image

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new ImageAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof ImageAssetModel;
				}
			});

			// spritesheet

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new SpritesheetAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof SpritesheetAssetModel || info instanceof SpritesheetAssetModel.FrameModel;
				}
			});

			// audio

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AudioAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info != null && info.getClass() == AudioAssetModel.class;
				}
			});

			// audio sprites

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AudioSpriteAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info != null && info.getClass() == AudioSpriteAssetModel.class;
				}
			});

			// audio sprites elements

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AudioSpriteAssetElementInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof AudioSpriteAssetModel.AssetAudioSprite;
				}
			});

			// video

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new VideoAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info != null && info instanceof VideoAssetModel;
				}
			});

			// atlas

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AtlasAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof AtlasAssetModel;
				}
			});

			// atlas frame

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AtlasFrameInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof AtlasAssetModel.Frame;
				}
			});

			// bitmap font

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new BitmapFontAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof BitmapFontAssetModel;
				}
			});

			// physics

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new PhysicsAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof PhysicsAssetModel || info instanceof PhysicsAssetModel.SpriteData;
				}
			});

			// tilemap json

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new TilemapJSONAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof TilemapAssetModel && ((TilemapAssetModel) info).isJSONFormat();
				}
			});

			// tilemap csv

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new TilemapCSVAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof TilemapAssetModel && ((TilemapAssetModel) info).isCSVFormat();
				}
			});

			// tilemap tileset

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new TilemapTilesetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof TilemapAssetModel.Tileset;
				}
			});

			// the others

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new OtherAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof AssetModel;
				}
			});
		}

		return _informationControlCreators;
	}

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static void showError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e), StatusManager.SHOW);
	}
}
