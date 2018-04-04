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
package phasereditor.assetpack.ui.search;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetConsumer;
import phasereditor.assetpack.core.IAssetKey;

/**
 * @author arian
 *
 */
public class SearchAssetQuery implements ISearchQuery {

	private IAssetKey _assetKey;
	private SearchAssetResult _result;

	public SearchAssetQuery(IAssetKey assetKey) {
		super();
		_assetKey = assetKey;
		_result = new SearchAssetResult(this);
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		
		List<IAssetConsumer> consumers = AssetPackCore.requestAssetConsumers();
		
		FindAssetReferencesResult result = new FindAssetReferencesResult();
		
		for(IAssetConsumer consumer : consumers) {
			FindAssetReferencesResult result2 = consumer.getAssetReferences(_assetKey, monitor);
			result.merge(result2);
		}
		
		_result.setReferences(result);
		
		return Status.OK_STATUS;
	}

	@Override
	public String getLabel() {
		return "'" + _assetKey.getKey() + "' references.";
	}

	public IAssetKey getAssetKey() {
		return _assetKey;
	}
	
	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public ISearchResult getSearchResult() {
		return _result;
	}

}
