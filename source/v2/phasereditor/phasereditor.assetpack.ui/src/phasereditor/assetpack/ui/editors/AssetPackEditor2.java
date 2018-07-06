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
package phasereditor.assetpack.ui.editors;

import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PatternFilter2;

/**
 * @author arian
 *
 */
public class AssetPackEditor2 extends EditorPart {

	private AssetPackModel _model;
	private Composite _container;
	private SectionsComp _sectionsComp;
	private TypesComp _typesComp;
	private ColumnComp _assetsComp;

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	class ColumnComp extends Composite {

		private TreeViewer _viewer;
		private ToolBar _toolbar;

		public ColumnComp(Composite parent, String title) {
			super(parent, SWT.BORDER);

			{
				GridLayout layout = new GridLayout(1, false);
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				layout.verticalSpacing = 2;
				setLayout(layout);
			}

			{
				Composite top = new Composite(this, SWT.NONE);

				GridLayout layout = new GridLayout(2, false);
				layout.marginWidth = 0;
				layout.marginHeight = 0;

				top.setLayout(layout);
				top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

				Label label = new Label(top, SWT.NONE);
				label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				label.setText(title);

				_toolbar = new ToolBar(top, SWT.NONE);
				var gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
				_toolbar.setLayoutData(gd);
				ToolItem item = new ToolItem(_toolbar, SWT.NONE);
				gd.heightHint = item.getBounds().height + 4;
				item.dispose();
				fillToolbar(_toolbar);
			}

			{
				FilteredTree tree = new FilteredTree(this, SWT.NONE, new PatternFilter2(), true);
				tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				_viewer = tree.getViewer();
			}

		}

		@SuppressWarnings("unused")
		protected void fillToolbar(ToolBar toolbar) {
			// empty
		}

		public TreeViewer getViewer() {
			return _viewer;
		}

	}

	class EditableColumnComp extends ColumnComp {

		public EditableColumnComp(Composite parent, String title) {
			super(parent, title);
		}

		@Override
		protected void fillToolbar(ToolBar toolbar) {
			{
				ToolItem item = new ToolItem(toolbar, SWT.NONE);
				item.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_ADD));
			}
			{
				ToolItem item = new ToolItem(toolbar, SWT.NONE);
				item.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_DELETE));
			}
			{
				ToolItem item = new ToolItem(toolbar, SWT.NONE);
				item.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_RENAME));
			}
		}

	}

	class SectionsComp extends EditableColumnComp {

		public SectionsComp(Composite parent) {
			super(parent, "Sections");

			getViewer().setContentProvider(new ITreeContentProvider() {

				@Override
				public boolean hasChildren(Object element) {
					return false;
				}

				@Override
				public Object getParent(Object element) {
					return null;
				}

				@Override
				public Object[] getElements(Object inputElement) {
					return ((AssetPackModel) inputElement).getSections().toArray();
				}

				@Override
				public Object[] getChildren(Object parentElement) {
					return new Object[] {};
				}
			});
			getViewer().setLabelProvider(AssetLabelProvider.GLOBAL_16);
		}

	}

	class SectionTypeModel implements Comparable<SectionTypeModel> {
		public AssetSectionModel section;
		public AssetType type;

		public SectionTypeModel(AssetSectionModel section, AssetType type) {
			super();
			this.section = section;
			this.type = type;
		}

		public List<AssetModel> getAssets() {
			return section.getGroup(type).getAssets();
		}

		@Override
		public int compareTo(SectionTypeModel o) {
			return -Integer.compare(getAssets().size(), o.getAssets().size());
		}
	}

	class TypesColumnLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Color counterColor = JFaceResources.getColorRegistry().get(JFacePreferences.COUNTER_COLOR);
			Color decorationsColor = JFaceResources.getColorRegistry().get(JFacePreferences.DECORATIONS_COLOR);

			var item = (SectionTypeModel) cell.getElement();

			int countAssets = item.section.getGroup(item.type).getAssets().size();

			String text;
			StyleRange[] styles;

			String name = item.type.name();
			if (countAssets > 0) {
				text = name + " (" + countAssets + ")";
				int start = name.length();
				int len = text.length() - start;

				var style1 = new StyleRange(0, start, null, null);
				var style2 = new StyleRange(start, len, counterColor, null);
				styles = new StyleRange[] { style1, style2 };
			} else {
				text = name;

				var style = new StyleRange(0, text.length(), decorationsColor, null);
				style.fontStyle = SWT.ITALIC;

				styles = new StyleRange[] { style };
			}

			cell.setStyleRanges(styles);
			cell.setText(text);
			cell.setImage(AssetLabelProvider.GLOBAL_16.getImage(item.type));
		}
	}

	class TypesComp extends ColumnComp {

		public TypesComp(Composite parent) {
			super(parent, "Types");
			getViewer().setContentProvider(new ArrayAsTreeContentProvider());
			getViewer().setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					SectionTypeModel item = (SectionTypeModel) element;
					return item.type.name();
				}
			});
			var col = new TreeViewerColumn(getViewer(), SWT.NONE);
			col.getColumn().setWidth(1000);

			col.setLabelProvider(new TypesColumnLabelProvider());
		}
	}

	class AssetsComp extends EditableColumnComp {

		public AssetsComp(Composite parent) {
			super(parent, "Assets");
			getViewer().setLabelProvider(AssetLabelProvider.GLOBAL_48);
			getViewer().setContentProvider(new AssetsContentProvider(true));
		}

	}

	class ArrayAsTreeContentProvider implements ITreeContentProvider {
		private ArrayContentProvider _provider = new ArrayContentProvider();

		@Override
		public Object[] getElements(Object inputElement) {
			return _provider.getElements(inputElement);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return _provider.getElements(parentElement);
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}

	}

	@Override
	public void createPartControl(Composite parent) {
		_container = new Composite(parent, SWT.NONE);
		_container.setLayout(new FillLayout());

		SashForm mainSash = new SashForm(_container, SWT.NONE);

		_sectionsComp = new SectionsComp(mainSash);

		_typesComp = new TypesComp(mainSash);

		_assetsComp = new AssetsComp(mainSash);

		mainSash.setWeights(new int[] { 1, 1, 1 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		_sectionsComp.getViewer().setInput(_model);
		_sectionsComp.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				var section = (AssetSectionModel) event.getStructuredSelection().getFirstElement();
				Object[] data = Arrays.stream(AssetType.values()).map(t -> new SectionTypeModel(section, t)).sorted()
						.toArray();
				getTypesComp().getViewer().setInput(data);
			}
		});
		_typesComp.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = event.getStructuredSelection();
				Object input;
				if (selection.isEmpty()) {
					input = new Object[] {};
				} else {
					var type = (SectionTypeModel) selection.getFirstElement();
					input = type.getAssets();
				}

				getAssetsComp().getViewer().setInput(input);
			}
		});

	}

	public SectionsComp getSectionsComp() {
		return _sectionsComp;
	}

	public TypesComp getTypesComp() {
		return _typesComp;
	}

	public ColumnComp getAssetsComp() {
		return _assetsComp;
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);

		FileEditorInput fileInput = getEditorInput();
		IFile file = fileInput.getFile();

		try {
			// we create a model copy detached from the AssetCore registry.
			_model = new AssetPackModel(file);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		_model.addPropertyChangeListener(this::propertyChange);

		setPartName(_model.getName());

	}

	private void propertyChange(@SuppressWarnings("unused") PropertyChangeEvent evt) {
		getEditorSite().getShell().getDisplay().asyncExec(() -> {
			firePropertyChange(PROP_DIRTY);
		});
	}

	public AssetPackModel getModel() {
		return _model;
	}

	public IResource getAssetsFolder() {
		return getEditorInput().getFile().getParent();
	}

	@Override
	public FileEditorInput getEditorInput() {
		return (FileEditorInput) super.getEditorInput();
	}

	@Override
	public void setFocus() {
		_container.setFocus();
	}

}
