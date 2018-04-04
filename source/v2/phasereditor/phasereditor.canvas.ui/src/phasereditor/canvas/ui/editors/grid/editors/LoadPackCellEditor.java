// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.ui.editors.grid.editors;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.canvas.core.StateSettings.LoadPack;

/**
 * @author arian
 *
 */
public class LoadPackCellEditor extends DialogCellEditor {

	private IProject _project;
	private Set<LoadPack> _value;

	public LoadPackCellEditor(Composite parent, IProject project, Set<LoadPack> value) {
		super(parent);
		_project = project;
		_value = value;
	}

	static class MyLabelProvider extends StyledCellLabelProvider implements ILabelProvider {
		@Override
		public void update(ViewerCell cell) {
			LoadPack loadPack = (LoadPack) cell.getElement();
			String section = loadPack.getSection();
			String file = loadPack.getFile();
			cell.setText(section + " - " + file);
			cell.setStyleRanges(new StyleRange[] { new StyleRange(section.length(), file.length() + 3,
					cell.getControl().getDisplay().getSystemColor(SWT.COLOR_GRAY), null) });
			cell.setImage(AssetLabelProvider.getSectionImage());
			super.update(cell);
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			return "text";
		}
	}

	@Override
	protected Object openDialogBox(Control window) {
		Set<LoadPack> input = new LinkedHashSet<>();

		for (AssetPackModel pack : AssetPackCore.getAssetPackModels(_project)) {
			for (AssetSectionModel section : pack.getSections()) {
				input.add(new LoadPack(pack.getRelativePath(), section.getKey()));
			}
		}

		ListSelectionDialog dlg = new ListSelectionDialog(window.getShell(), input.toArray(),
				new ArrayContentProvider(), new MyLabelProvider(), "Select the asset pack sections to load:");
		dlg.setInitialSelections(_value.toArray());

		if (dlg.open() == Window.OK) {
			Object[] result = dlg.getResult();
			Set<LoadPack> newValue = new LinkedHashSet<>();
			for (Object obj : result) {
				newValue.add((LoadPack) obj);
			}
			return newValue;
		}

		return _value;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void updateContents(Object value) {
		super.updateContents(LoadPack.toString((Set<LoadPack>) value));
	}

}
