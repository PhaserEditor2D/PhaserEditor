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
package phasereditor.assetpack.core;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Some assets have sub-elements like the atlas or the audio sprite. This
 * sub-elements have common features described in this interface.
 * 
 * @author arian
 *
 */
public interface IAssetElementModel extends IAssetKey, IAdaptable {
	public String getName();

	@Override
	public default String getKey() {
		return getName();
	}

	@Override
	public AssetModel getAsset();

	@Override
	public default boolean isFreshVersion() {
		if (getAsset().isFreshVersion()) {
			return getAsset().getSubElements().contains(this);
		}

		return false;
	}

	@Override
	public default IAssetElementModel findFreshVersion() {
		AssetModel asset = getAsset().findFreshVersion();

		if (asset == null) {
			return null;
		}

		for (IAssetElementModel elem : asset.getSubElements()) {
			if (elem.getKey().equals(getKey())) {
				return elem;
			}
		}

		return null;
	}
}
