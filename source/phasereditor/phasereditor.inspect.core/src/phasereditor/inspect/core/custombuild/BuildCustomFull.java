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
package phasereditor.inspect.core.custombuild;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.inspect.core.InspectCore;

public class BuildCustomFull {

	public static void main(String[] args) throws JSONException, IOException {
		Path wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		Path projectPath = wsPath.resolve(InspectCore.RESOURCES_METADATA_PLUGIN);
		// Path srcPath = projectPath.resolve("phaser-master/src");
		Path modulesPath = projectPath.resolve("phaser-custom/build/modules.json");
		// read modules
		CustomBuildModel model = new CustomBuildModel();
		JSONObject obj = new JSONObject(new String(Files.readAllBytes(modulesPath)));
		for (String name : obj.keySet()) {
			JSONObject modInfo = obj.getJSONObject(name);
			BuildModule module = new BuildModule();
			module.setName(name);
			module.setOptional(modInfo.getBoolean("optional"));
			module.setStub(modInfo.getBoolean("stub"));
			model.addModule(module);
		}
	}

}
