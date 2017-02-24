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
package phasereditor.canvas.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.FlatAssetLabelProvider;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PatternFilter2;

/**
 * @author arian
 *
 */
public class AddSpriteDialog extends Dialog implements IEditorSharedImages {

	public static final int ADD_SPRITE = IDialogConstants.CLIENT_ID + 3;
	public static final int ADD_TILE = IDialogConstants.CLIENT_ID + 2;
	public static final int ADD_BUTTON = IDialogConstants.CLIENT_ID + 1;

	private IProject _project;
	private FilteredTree _filteredTree;
	private TreeViewer _viewer;
	private Object _selected;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public AddSpriteDialog(Shell parentShell) {
		super(parentShell);
	}

	static class TreeArrayProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			// nothing
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			//
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((List<?>) inputElement).toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
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
	protected void buttonPressed(int buttonId) {
		setReturnCode(buttonId);
		close();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.marginWidth = 5;
		gridLayout.marginHeight = 5;

		_filteredTree = new FilteredTree(container, SWT.BORDER, new PatternFilter2(), true);
		_viewer = _filteredTree.getViewer();
		_viewer.setLabelProvider(new FlatAssetLabelProvider(AssetLabelProvider.GLOBAL_48));
		_viewer.setContentProvider(new TreeArrayProvider());
		List<AssetPackModel> packs = AssetPackCore.getAssetPackModels(_project);
		List<Object> keys = new ArrayList<>();
		packs.forEach(pack -> {
			pack.getSections().forEach(section -> {
				section.getAssets().forEach(asset -> {
					if (asset instanceof ImageAssetModel) {
						keys.add(asset);
					} else {
						asset.getSubElements().forEach(elem -> {
							keys.add(elem);
						});
					}
				});
			});
		});
		_viewer.setInput(keys);
		_viewer.addSelectionChangedListener(e -> {
			_selected = ((IStructuredSelection) e.getSelection()).getFirstElement();
		});

		AssetPackUI.installAssetTooltips(_viewer);

		return container;
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Add Sprite");
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btn = createButton(parent, ADD_BUTTON, "button", false);
		btn.setImage(EditorSharedImages.getImage(IMG_BUTTON));
		btn.setToolTipText("Add button");
		btn = createButton(parent, ADD_TILE, "tileSprite", false);
		btn.setImage(EditorSharedImages.getImage(IMG_TILES));
		btn.setToolTipText("Add tileSprite");
		btn = createButton(parent, ADD_SPRITE, "sprite", true);
		btn.setImage(EditorSharedImages.getImage(IMG_CAR));
		btn.setToolTipText("Add sprite");
	}

	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		Button btn = super.createButton(parent, id, label, defaultButton);
		return btn;
	}

	@Override
	protected void setButtonLayoutData(Button button) {
		super.setButtonLayoutData(button);
		GridData data = (GridData) button.getLayoutData();
		data.widthHint = -1;
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(400, 300);
	}

	public void setProject(IProject project) {
		_project = project;
	}

	public IStructuredSelection getSelection() {
		return new StructuredSelection(new Object[]{_selected});
	}

}
