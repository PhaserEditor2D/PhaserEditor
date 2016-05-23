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
package phasereditor.assetpack.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.IAssetElementModel;

public class AssetsContentProvider implements ITreeContentProvider {
	public AssetsContentProvider() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing
	}

	@Override
	public void dispose() {
		// nothing
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof AssetPackModel) {
			return ((AssetPackModel) parentElement).getSections().toArray();
		}

		if (parentElement instanceof AssetSectionModel) {
			AssetSectionModel section = (AssetSectionModel) parentElement;
			List<AssetGroupModel> groups = new ArrayList<>();
			Set<AssetType> used = new HashSet<>();

			for (AssetModel asset : section.getAssets()) {
				AssetType type = asset.getType();
				if (!used.contains(type)) {
					groups.add(section.getGroup(type));
					used.add(type);
				}
			}

			Collections.sort(groups);

			return groups.toArray();
		}

		if (parentElement instanceof AssetGroupModel) {
			List<AssetModel> assets = new ArrayList<>();
			AssetGroupModel typeNode = (AssetGroupModel) parentElement;
			for (AssetModel asset : typeNode.getSection().getAssets()) {
				if (asset.getType() == typeNode.getType()) {
					assets.add(asset);
				}
			}
			return assets.toArray();
		}

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof AssetSectionModel) {
			return ((AssetSectionModel) element).getPack();
		}

		if (element instanceof AssetGroupModel) {
			return ((AssetGroupModel) element).getSection();
		}

		if (element instanceof AssetModel) {
			AssetModel asset = (AssetModel) element;
			return asset.getGroup();
		}

		if (element instanceof IAssetElementModel) {
			return ((IAssetElementModel) element).getAsset();
		}

		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
}