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
package phasereditor.canvas.ui.editors.grid;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;

import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.editors.CanvasPGridEditingSupport;
import phasereditor.ui.properties.PGrid;
import phasereditor.ui.properties.PGridEditingSupport;
import phasereditor.ui.properties.PGridValueLabelProvider;

/**
 * @author arian
 *
 */
public class CanvasPGrid extends PGrid {

	public CanvasPGrid(Composite parent, int style) {
		this(parent, style, true);
	}

	public CanvasPGrid(Composite parent, int style, boolean supportUndoRedo) {
		super(parent, style, supportUndoRedo);
	}
	
	@Override
	protected PGridEditingSupport createEditingSupport(TreeViewer viewer, boolean supportUndoRedo) {
		return new CanvasPGridEditingSupport(viewer, supportUndoRedo);
	}
	

	@Override
	protected PGridValueLabelProvider createValueLabelProvider() {
		return new CanvasPGridValueLabelProvider(getViewer());
	}
	
	@Override
	public CanvasPGridValueLabelProvider getValueLabelProvider() {
		return (CanvasPGridValueLabelProvider) super.getValueLabelProvider();
	}
	
	@Override
	public CanvasPGridEditingSupport getEditSupport() {
		return (CanvasPGridEditingSupport) super.getEditSupport();
	}

	public void setCanvas(ObjectCanvas canvas) {
		getEditSupport().setCanvas(canvas);
		getValueLabelProvider().setCanvas(canvas);
	}
}