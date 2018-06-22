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
package phasereditor.inspect.core.jsdoc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author arian
 *
 */
public class PhaserFilesModel {
	private PhaserJsdocModel _jsdocModel;

	private Map<Path, List<IPhaserMember>> _pathMembersMap;

	public PhaserFilesModel(PhaserJsdocModel jsdocModel) {
		super();
		_jsdocModel = jsdocModel;

		build();
	}

	private void build() {
		_pathMembersMap = new HashMap<>();

		for (IPhaserMember member : _jsdocModel.getMembersMap().values()) {
			Path file = realFile(member);
			List<IPhaserMember> list;
			if (_pathMembersMap.containsKey(file)) {
				list = _pathMembersMap.get(file);
			} else {
				list = new ArrayList<>();
				IPhaserMember rootMemeber = findRootMember(member);
				list.add(rootMemeber);
				_pathMembersMap.put(file, list);
			}
		}
	}

	private Path realFile(IPhaserMember member) {
		return _jsdocModel.getMemberPath(member);
	}

	private IPhaserMember findRootMember(IPhaserMember member) {
		IMemberContainer container = member.getContainer();

		if (container == null) {
			return member;
		}

		if (realFile(container).equals(realFile(member))) {
			return findRootMember(container);
		}

		return member;
	}

	public Path getSrcFolder() {
		return _jsdocModel.getSrcFolder();
	}

	public List<IPhaserMember> getPhaserMembersOfFile(Path file) {
		List<IPhaserMember> list = _pathMembersMap.get(file);

		if (list == null) {
			return Collections.emptyList();
		}

		return list;
	}

}
