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
package phasereditor.atlas.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

public class ResultPage <T extends AtlasFrame>{
	private List<T> _frames;
	private Map<T, String> _frameFileMap;
	private Map<T, Integer> _frameIndexMap;
	private Image _image;

	public ResultPage() {
		_frames = new ArrayList<>();
		_frameFileMap = new HashMap<>();
		_frameIndexMap = new HashMap<>();
	}

	public void addFrame(T frame, String filepath, int index) {
		_frameFileMap.put(frame, filepath);
		_frameIndexMap.put(frame, Integer.valueOf(index));
		_frames.add(frame);
	}

	public void sortByIndexes() {
		_frames.sort((a, b) -> {
			int i1 = _frameIndexMap.get(a).intValue();
			int i2 = _frameIndexMap.get(a).intValue();
			return Integer.compare(i1 == -1 ? Integer.MAX_VALUE : i1, i2 == -1 ? Integer.MAX_VALUE : i2);
		});
	}

	public List<T> getFrames() {
		return _frames;
	}

	public Image getImage() {
		return _image;
	}

	public void setImage(Image image) {
		_image = image;
	}
}