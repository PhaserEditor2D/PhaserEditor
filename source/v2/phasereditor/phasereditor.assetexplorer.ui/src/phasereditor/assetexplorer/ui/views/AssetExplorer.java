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
package phasereditor.assetexplorer.ui.views;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.ViewPart;
import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.assetexplorer.ui.views.newactions.NewWizardLancher;
import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.atlas.core.AtlasData;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.ui.FilteredTree2;
import phasereditor.ui.PatternFilter2;
import phasereditor.ui.PhaserEditorUI;

public class AssetExplorer extends ViewPart {
	public static final String ID = "phasereditor.assetpack.views.assetExplorer";
	TreeViewer _viewer;
	private FilteredTree _filteredTree;
	private AssetExplorerContentProvider _contentProvider;
	// private AssetExplorerLabelProvider _treeLabelProvider;
	// private AssetExplorerContentProvider _treeContentProvider;
	// private AssetExplorerListLabelProvider _listLabelProvider;
	// private AssetExplorerListContentProvider _listContentProvider;
	public static String ROOT = "root";
	public static String ANIMATIONS_NODE = "Animations Files";
	public static String ATLAS_NODE = "Texture Packer Files";
	public static String CANVAS_NODE = "Canvas Files";
	public static String PACK_NODE = "Pack Files";
	public static String PROJECTS_NODE = "Other Projects";

	public AssetExplorer() {
		super();
	}

	@SuppressWarnings("unused")
	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl_parent = new GridLayout(1, false);
		gl_parent.marginWidth = 0;
		gl_parent.marginHeight = 0;
		parent.setLayout(gl_parent);

		_filteredTree = new FilteredTree2(parent, SWT.MULTI, new PatternFilter2(), 4);
		GridLayout gridLayout = (GridLayout) _filteredTree.getLayout();
		_viewer = _filteredTree.getViewer();
		_viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}
		});
		Tree tree = _viewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_contentProvider = new AssetExplorerContentProvider();
		_viewer.setContentProvider(_contentProvider);
		_viewer.setLabelProvider(new AssetExplorerLabelProvider());
		var col = new TreeViewerColumn(_viewer, SWT.NONE);
		col.setLabelProvider(createStyledLabelProvider());
		_viewer.getTree().addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				col.getColumn().setWidth(((Tree) e.widget).getBounds().width);
			}
		});
		afterCreateWidgets();

	}

	private static StyledCellLabelProvider createStyledLabelProvider() {
		return new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				var base = (AssetExplorerLabelProvider) getViewer().getLabelProvider();

				Object obj = cell.getElement();

				String text = base.getText(obj);

				StyledString str = new StyledString();

				if (obj instanceof NewWizardLancher) {
					str.append(text,
							StyledString.createColorRegistryStyler(JFacePreferences.ACTIVE_HYPERLINK_COLOR, null));
				} else {
					str.append(text);
				}

				cell.setText(str.getString());
				cell.setStyleRanges(str.getStyleRanges());
				cell.setImage(base.getImage(obj));

				super.update(cell);
			}
		};
	}

	public TreeViewer getViewer() {
		return _viewer;
	}

	protected void handleDoubleClick() {
		Object elem = ((IStructuredSelection) _viewer.getSelection()).getFirstElement();
		IFile file = null;

		var provider = (AssetExplorerContentProvider) _viewer.getContentProvider();

		if (elem instanceof IProject) {
			provider.forceToFocuseOnProject((IProject) elem);
			_viewer.refresh();
			_viewer.expandToLevel(3);
		} else if (elem instanceof IFile) {
			file = (IFile) elem;
		} else if (elem instanceof CanvasFile) {
			file = ((CanvasFile) elem).getFile();
		} else if (elem instanceof AtlasData) {
			file = ((AtlasData) elem).getFile();
		} else if (elem instanceof AnimationsModel) {
			file = ((AnimationsModel) elem).getFile();
		} else if (elem instanceof NewWizardLancher) {
			((NewWizardLancher) elem).openWizard(provider.getProjectInContent());
		}

		if (file != null) {
			try {
				IDE.openEditor(getSite().getPage(), file);
			} catch (PartInitException e) {
				throw new RuntimeException(e); // NOSONAR
			}
		}

		AssetPackUI.openElementInEditor(elem);
	}

	private void afterCreateWidgets() {
		// _treeLabelProvider = new AssetExplorerLabelProvider();
		// _treeContentProvider = new AssetExplorerContentProvider();
		// _listLabelProvider = new AssetExplorerListLabelProvider();
		// _listContentProvider = new AssetExplorerListContentProvider();

		// _viewer.setLabelProvider(_treeLabelProvider);
		// _viewer.setContentProvider(_treeContentProvider);

		// content

		_viewer.setInput(ROOT);
		_viewer.expandToLevel(1);

		// drag and drop

		init_DND();

		// selection provider

		getViewSite().setSelectionProvider(_viewer);

		{

			// menu
			MenuManager manager = new MenuManager();
			Tree tree = _viewer.getTree();
			Menu menu = manager.createContextMenu(tree);
			tree.setMenu(menu);

			getViewSite().registerContextMenu(manager, _viewer);
		}

		// tooltips

		AssetPackUI.installAssetTooltips(_viewer);
		CanvasUI.installCanvasTooltips(_viewer);

		// undo context

		IUndoContext undoContext = WorkspaceUndoUtil.getWorkspaceUndoContext();

		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), new UndoActionHandler(getSite(), undoContext));
		actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), new RedoActionHandler(getSite(), undoContext));
	}

	private void init_DND() {
		Transfer[] types = { LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance() };
		_viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_DEFAULT, types, new DragSourceAdapter() {

			private Object[] _data;
			private boolean _disposeImage;

			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				_data = ((IStructuredSelection) _viewer.getSelection()).toArray();
				transfer.setSelection(new StructuredSelection(_data));

				_disposeImage = false;

				for (var obj : _data) {
					IAssetFrameModel frame = null;

					if (obj instanceof IAssetFrameModel) {
						frame = (IAssetFrameModel) obj;
					} else if (obj instanceof ImageAssetModel) {
						frame = ((ImageAssetModel) obj).getFrame();
					} else if (obj instanceof AnimationFrameModel) {
						frame = ((AnimationFrameModel) obj).getFrameAsset();
					} else if (obj instanceof CanvasFile) {
						var file = ((CanvasFile) obj).getFile();
						var path = CanvasUI.getCanvasScreenshotFile(file, true);
						var filepath = path.toString();
						var src = PhaserEditorUI.getImageBounds(filepath);
						event.image = PhaserEditorUI.scaleImage_DND(filepath, src);
						_disposeImage = true;
						break;
					}

					if (frame != null) {
						var file = frame.getImageFile();
						var fd = frame.getFrameData();
						if (file != null && fd != null) {
							event.image = PhaserEditorUI.scaleImage_DND(frame.getImageFile(), fd.src);
							_disposeImage = true;
							break;
						}
					}

				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				JSONArray array = new JSONArray();
				for (Object elem : _data) {
					if (elem instanceof IAssetKey) {
						array.put(((IAssetKey) elem).getKey());
					}
				}
				if (array.length() == 1) {
					event.data = array.getString(0);
				} else {
					event.data = array.toString();
				}
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				if (_disposeImage && event.image != null) {
					event.image.dispose();
				}
			}
		});
	}

	@Override
	public void setFocus() {
		_viewer.getTree().setFocus();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IContextProvider.class)) {
			return new IContextProvider() {

				@Override
				public String getSearchExpression(Object target) {
					return null;
				}

				@Override
				public int getContextChangeMask() {
					return NONE;
				}

				@Override
				public IContext getContext(Object target) {
					IContext context = HelpSystem.getContext("phasereditor.help.assetexplorer");
					return context;
				}
			};
		}
		return super.getAdapter(adapter);
	}

	public void refreshContent(IProject project) {
		out.println("Assets.refreshContent(" + project.getName() + ")");

		IProject currentProject = _contentProvider.getProjectInContent();

		if (currentProject != null && project != currentProject) {
			out.println("  Skip refresh.");
			return;
		}
		out.println("  Perfom refresh");

		if (_viewer.getControl().isDisposed()) {
			return;
		}

		if (PlatformUI.getWorkbench().isClosing()) {
			return;
		}

		// _viewer.refresh();

		Object[] expanded = _viewer.getVisibleExpandedElements();

		_viewer.getTree().setRedraw(false);
		try {
			_viewer.refresh();

			List<Object> toExpand = new ArrayList<>();

			for (Object obj : expanded) {
				if (obj instanceof IAssetKey) {
					IAssetKey key = ((IAssetKey) obj).getSharedVersion();
					toExpand.add(key);
				} else if (obj instanceof AssetGroupModel) {
					AssetPackModel oldPack = ((AssetGroupModel) obj).getSection().getPack();
					JSONObject ref = oldPack.getAssetJSONRefrence(obj);
					IFile file = oldPack.getFile();
					AssetPackModel newPack = AssetPackCore.getAssetPackModel(file, false);
					if (newPack != null) {
						Object obj2 = newPack.getElementFromJSONReference(ref);
						if (obj2 != null) {
							toExpand.add(obj2);
						}
					}
				} else if (obj instanceof AssetSectionModel) {
					AssetPackModel oldPack = ((AssetSectionModel) obj).getPack();
					JSONObject ref = oldPack.getAssetJSONRefrence(obj);
					IFile file = oldPack.getFile();
					AssetPackModel newPack = AssetPackCore.getAssetPackModel(file, false);
					if (newPack != null) {
						Object obj2 = newPack.getElementFromJSONReference(ref);
						toExpand.add(obj2);
					}
				} else if (obj instanceof AssetPackModel) {
					AssetPackModel newPack = AssetPackCore.getAssetPackModel(((AssetPackModel) obj).getFile(), false);
					toExpand.add(newPack);
				} else if (obj instanceof CanvasType) {
					toExpand.add(obj);
				} else {
					toExpand.add(obj);
				}
			}
			toExpand.remove(null);
			Object[] array = toExpand.toArray();
			_viewer.setExpandedElements(array);

		} finally {
			_viewer.getTree().setRedraw(true);
		}
	}

}
