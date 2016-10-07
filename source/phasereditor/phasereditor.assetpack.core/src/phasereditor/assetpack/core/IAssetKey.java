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
package phasereditor.assetpack.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

/**
 * @author arian
 *
 */
public interface IAssetKey {
	public String getKey();

	public AssetModel getAsset();

	public default List<? extends IAssetElementModel> getAllFrames() {
		return getAsset().getSubElements();
	}

	/**
	 * An asset key is out-dated after a pack build. This method return if this
	 * is the fresh instance.
	 * 
	 * @return
	 */
	public boolean isSharedVersion();

	/**
	 * Find the fresh (updated) version of this asset key.
	 * 
	 * @return
	 */
	public IAssetKey getSharedVersion();

	public default boolean touched(IResourceDelta resourceDelta) {
		AtomicBoolean touched = new AtomicBoolean(false);
		IFile[] list = getAsset().getUsedFiles();
		for (IFile used : list) {
			try {
				resourceDelta.accept(r -> {
					if (used.equals(r.getResource())) {
						touched.set(true);
						return false;
					}
					return true;
				});
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (touched.get()) {
				return true;
			}
		}
		return touched.get();
	}
}
