// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.ui;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @author arian
 *
 */
public class FileEditorBlock extends ResourceEditorBlock<IFile> {

	public FileEditorBlock(IFile resource) {
		super(resource);
	}

	@Override
	public String getKeywords() {
		return "";
	}

	@Override
	public List<IEditorBlock> getChildren() {
		return null;
	}

	private static WorkbenchLabelProvider _labelProvider = new WorkbenchLabelProvider();

	@Override
	public ICanvasCellRenderer getRenderer() {
		return new ICanvasCellRenderer() {

			@Override
			public void render(Canvas canvas, GC gc, int x, int y, int width, int height) {
				var img = _labelProvider.getImage(getResource());
				var b = img.getBounds();
				gc.drawImage(img, 0, 0, b.width, b.height, x + width / 2 - b.width / 2, y + height / 2 - b.height / 2, b.width, b.height);
			}
		};
	}

	@Override
	public String getSortName() {
		return "002";
	}

	@Override
	public RGB getColor() {
		return Colors.BLUE.rgb;
	}

}
