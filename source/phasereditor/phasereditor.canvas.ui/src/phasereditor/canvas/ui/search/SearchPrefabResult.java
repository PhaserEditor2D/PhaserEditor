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
package phasereditor.canvas.ui.search;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;

import phasereditor.canvas.ui.CanvasUI.FindPrefabReferencesResult;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class SearchPrefabResult implements ISearchResult {

	private ListenerList<ISearchResultListener> _listeners;
	private SearchPrefabQuery _query;
	private FindPrefabReferencesResult _references;

	public SearchPrefabResult(SearchPrefabQuery query) {
		_listeners = new ListenerList<>();
		_query = query;
		_references = new FindPrefabReferencesResult();
	}

	public FindPrefabReferencesResult getReferences() {
		return _references;
	}

	public void setReferences(FindPrefabReferencesResult references) {
		_references = references;

		SearchPrefabResultEvent e = new SearchPrefabResultEvent(this);

		for (ISearchResultListener l : _listeners) {
			l.searchResultChanged(e);
		}
	}

	@Override
	public void addListener(ISearchResultListener l) {
		_listeners.add(l);
	}

	@Override
	public void removeListener(ISearchResultListener l) {
		_listeners.remove(l);
	}

	@Override
	public String getLabel() {
		return "'" + _query.getPrefab().getFile().getName() + "' - found " + _references.getTotalReferences() + " instances in "
				+ _references.getTotalFiles() + " files.";
	}

	@Override
	public String getTooltip() {
		return getLabel();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_CANVAS);
	}

	@Override
	public ISearchQuery getQuery() {
		return _query;
	}

}
