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

import java.util.List;

import phasereditor.assetpack.core.IAssetFrameModel;

/**
 * @author arian
 *
 */
public abstract class PGridFrameProperty extends PGridProperty<IAssetFrameModel> {


	public static final Object NULL_FRAME = "<NULL FRAME>";

	private boolean _allowNull;
	
	public PGridFrameProperty(String name) {
		super(name);
		_allowNull = false;
	}
	
	public boolean isAllowNull() {
		return _allowNull;
	}
	
	public void setAllowNull(boolean allowNull) {
		_allowNull = allowNull;
	}

	public String getLabel() {
		 IAssetFrameModel value = getValue();
		return value == null ? "" : value.getKey();
	}
	
	public abstract List<?> getFrames();

}
