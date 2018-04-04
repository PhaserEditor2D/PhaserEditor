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
package phasereditor.assetpack.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;

public class FindAssetReferencesResult {

	private Map<IFile, List<IAssetReference>> _mapFileList;
	private Map<IFile, Set<String>> _mapFileSet;
	private int _total;

	public FindAssetReferencesResult() {
		_mapFileList = new LinkedHashMap<>();
		_mapFileSet = new HashMap<>();
		_total = 0;
	}

	public void add(IAssetReference ref) {
		IFile file = ref.getFile();

		_mapFileList.putIfAbsent(file, new ArrayList<>());
		_mapFileSet.putIfAbsent(file, new HashSet<>());

		Set<String> set = _mapFileSet.get(file);

		if (!set.contains(ref.getId())) {
			set.add(ref.getId());
			List<IAssetReference> list = _mapFileList.get(file);
			list.add(ref);
			_total++;
		}
	}

	public int getTotalReferences() {
		return _total;
	}

	public int getTotalFiles() {
		return _mapFileList.size();
	}

	public List<IAssetReference> getReferencesOf(IFile file) {
		return _mapFileList.get(file);
	}

	public Set<IFile> getFiles() {
		return _mapFileList.keySet();
	}

	public void addAll(List<IAssetReference> refs) {
		for (IAssetReference ref : refs) {
			add(ref);
		}
	}

	public void merge(FindAssetReferencesResult result) {
		for (List<IAssetReference> refs : result._mapFileList.values()) {
			addAll(refs);
		}
	}

	public IAssetReference getFirstReference() {
		for (IFile file : _mapFileList.keySet()) {
			List<IAssetReference> list = _mapFileList.get(file);
			if (!list.isEmpty()) {
				return list.get(0);
			}
		}
		return null;
	}
}
