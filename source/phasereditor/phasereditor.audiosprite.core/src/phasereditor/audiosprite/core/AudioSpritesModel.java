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
package phasereditor.audiosprite.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class AudioSpritesModel {

	private List<AudioSprite> _sprites;
	private List<IFile> _resources;
	private IFile _modelFile;

	public AudioSpritesModel() {
		_sprites = new ArrayList<>();
		_resources = new ArrayList<>();
	}

	public AudioSpritesModel(IFile modelFile) {
		this();
		_modelFile = modelFile;

		try (InputStream contents = modelFile.getContents()) {
			JSONObject obj = new JSONObject(new JSONTokener(contents));

			List<AudioSprite> list = new ArrayList<>();
			JSONObject spritemap = obj.optJSONObject("spritemap");
			if (spritemap != null) {
				for (String k : spritemap.keySet()) {
					JSONObject sprite = spritemap.getJSONObject(k);
					AudioSprite sd = new AudioSprite();
					sd.setName(k);
					sd.setStart(sprite.getDouble("start"));
					sd.setEnd(sprite.getDouble("end"));
					list.add(sd);
				}
			}
			Collections.sort(list, (a, b) -> {
				return Double.compare(a.start, b.start);
			});
			_sprites = list;

			JSONArray resourcesJson = obj.optJSONArray("resources");
			if (resourcesJson != null) {
				for (int i = 0; i < resourcesJson.length(); i++) {
					String filename = resourcesJson.getString(i);
					IFile audioFile = modelFile.getParent().getFile(
							new Path(filename));
					_resources.add(audioFile);
				}
			}
		} catch (IOException | CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public IFile getModelFile() {
		return _modelFile;
	}

	public void setModelFile(IFile modelFile) {
		_modelFile = modelFile;
	}

	public List<IFile> getResources() {
		return _resources;
	}

	public void setResources(List<IFile> audioFile) {
		_resources = audioFile;
	}

	public void addSprite(AudioSprite sprite) {
		_sprites.add(sprite);
	}

	public List<AudioSprite> getSprites() {
		return _sprites;
	}

	public JSONObject toJSON() {
		JSONObject jsonDoc = new JSONObject();
		JSONObject jsonSpritemap = new JSONObject();
		jsonDoc.put("spritemap", jsonSpritemap);
		for (AudioSprite data : _sprites) {
			JSONObject jsonSprite = new JSONObject();
			jsonSprite.put("start", data.getStart());
			jsonSprite.put("end", data.getEnd());
			jsonSpritemap.put(data.getName(), jsonSprite);
		}

		JSONArray jsonResources = new JSONArray();
		jsonDoc.put("resources", jsonResources);
		for (IFile file : _resources) {
			jsonResources.put(file.getName());
		}
		return jsonDoc;
	}
}
