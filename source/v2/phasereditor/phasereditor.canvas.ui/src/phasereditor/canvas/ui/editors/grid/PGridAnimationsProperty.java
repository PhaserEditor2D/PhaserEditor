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

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.AnimationModel;

/**
 * @author arian
 *
 */
public abstract class PGridAnimationsProperty extends PGridProperty<List<AnimationModel>> {

	public PGridAnimationsProperty(String nodeId, String name, String tooltip) {
		super(nodeId, name, tooltip);
	}

	public static String getLabel(List<AnimationModel> list) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		int i = 0;
		for (AnimationModel anim : list) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(anim.getName());
			i++;
		}
		sb.append("]");
		return sb.toString();
	}
	
	public abstract IAssetKey getAssetKey();
}
