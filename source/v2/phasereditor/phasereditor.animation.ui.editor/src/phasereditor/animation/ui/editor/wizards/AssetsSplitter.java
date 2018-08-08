// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.animation.ui.editor.wizards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import phasereditor.assetpack.core.IAssetKey;

/**
 * @author arian
 *
 */
public class AssetsSplitter {

	public static class AssetsGroup {
		private String _prefix;
		private List<IAssetKey> _asset;

		public AssetsGroup(String prefix) {
			_prefix = prefix;
			_asset = new ArrayList<>();
		}

		public String getPrefix() {
			return _prefix;
		}

		public List<IAssetKey> getAssets() {
			return _asset;
		}
	}

	private List<IAssetKey> _assets;

	public AssetsSplitter() {
		_assets = new ArrayList<>();
	}

	public void add(IAssetKey asset) {
		_assets.add(asset);
	}

	public void addAll(Collection<? extends IAssetKey> list) {
		_assets.addAll(list);
	}

	public Collection<AssetsGroup> split() {

		var prefixes = new ArrayList<String>();

		// first, remove all trailing non alphabet chars
		for (var obj : _assets) {
			var name = obj.getKey();
			var newName = removeTrailingSpaces(name);
			prefixes.add(obj.getAsset().getKey() + " - " + newName);
		}

		var map = new HashMap<String, AssetsGroup>();

		for (int i = 0; i < prefixes.size(); i++) {

			var prefix = prefixes.get(i);
			var obj = _assets.get(i);

			AssetsGroup group;

			if (map.containsKey(prefix)) {
				group = map.get(prefix);
			} else {
				group = new AssetsGroup(prefix);
				map.put(prefix, group);
			}

			group.getAssets().add(obj);

		}

		return map.values();
	}

	public static String removeTrailingSpaces(String name) {
		int i = name.length() - 1;
		for (; i >= 0; i--) {
			var c = name.charAt(i);

			if (Character.isAlphabetic(c)) {
				break;
			}
		}

		var newName = i == -1 ? name : name.substring(0, i + 1);
		return newName;
	}

	// public static void main(String[] args) {
	// List<Object> data = List.of("brinca_1", "brinca_2", "brinca_3", "brinca_4",
	// "jump 56", "30");
	// var splitter = new CommonPrefixSplitter(data);
	//
	// var groups = splitter.split();
	//
	// for (var group : groups) {
	// out.println(group.getPrefix() + ":");
	// group.getFrames().stream().forEach(out::println);
	// out.println();
	// }
	// }

}
