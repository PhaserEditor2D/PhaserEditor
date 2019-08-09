// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.scripts;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.chains.core.ChainsModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.PhaserExampleCategoryModel;
import phasereditor.inspect.core.examples.PhaserExamplesRepoModel;
import phasereditor.inspect.core.jsdoc.JsdocRenderer;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class BuildOnlineChains {
	private static int chainsVersion = 1;

	public static void main(String[] args) throws Exception {

		var wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		var srcFolder = wsPath.resolve(InspectCore.RESOURCES_PHASER_CODE_PLUGIN).resolve("phaser-master/src");
		var examplesProjectPath = wsPath.resolve(InspectCore.RESOURCES_EXAMPLES_PLUGIN);
		var docsJsonFile = wsPath.resolve(InspectCore.RESOURCES_METADATA_PLUGIN)
				.resolve("phaser-custom/phaser3-docs/json/phaser.json").toAbsolutePath().normalize();

		var examplesModel = new PhaserExamplesRepoModel(examplesProjectPath);
		examplesModel.build();

		var docsModel = new PhaserJsdocModel(srcFolder, docsJsonFile);

		var chainsModel = new ChainsModel(docsModel, examplesModel);

		generateChainsData(chainsModel);

		generateApiData(docsModel);

		generateExamplesData(examplesModel);
	}

	public static void generateApiData(PhaserJsdocModel docsModel) throws IOException {
		var data = new JSONObject();
		var renderer = new JsdocRenderer(false, docsModel);
		for (var member : docsModel.getMembersMap().values()) {
			var elemData = new JSONObject();

			var doc = renderer.render(member);
			elemData.put("doc", doc);
			elemData.put("file", member.getFile());
			elemData.put("line", member.getLine());
			elemData.put("since", member.getSince());

			data.put(member.getFullName(), elemData);
		}

		var path = Paths.get("/home/arian/Documents/PhaserEditor/PhaserChains/Public/WebContent/data/api-"
				+ chainsVersion + ".json");
		Files.writeString(path, data.toString(2));

	}

	public static void generateChainsData(ChainsModel chainsModel) throws IOException {
		var chainsData = new JSONArray();

		for (var chain : chainsModel.getChains()) {
			var phaserMember = chain.getPhaserMember();

			var tuple = new JSONObject();

			tuple.put("chain", chain.getChain());
			tuple.put("retType", chain.getReturnTypeName());
			{
				var icon = JsdocRenderer.getIconName(phaserMember);
				tuple.put("icon", icon);
			}
			tuple.put("id", phaserMember.getFullName());

			chainsData.put(tuple);
		}

		var s = chainsData.toString(2);

		// out.println(s);
		var path = Paths
				.get("/home/arian/Documents/PhaserEditor/PhaserChains/Public/WebContent/data/chains-" + chainsVersion
						+ ".json")

				.toAbsolutePath()

				.normalize();

		out.println("Writing to " + path);
		Files.writeString(path, s);
		out.println("Length: " + PhaserEditorUI.getFileHumanSize(Files.size(path)));
	}

	public static void generateExamplesData(PhaserExamplesRepoModel examplesModel) throws IOException {
		var examplesData = new JSONArray();

		for (var cat : examplesModel.getExamplesCategories()) {
			generateExamplesData(examplesModel, examplesData, cat);
		}

		var s = examplesData.toString(2);

		// out.println(s);
		var path = Paths
				.get("/home/arian/Documents/PhaserEditor/PhaserChains/Public/WebContent/data/examples-" + chainsVersion
						+ ".json")

				.toAbsolutePath()

				.normalize();

		out.println("Writing to " + path);
		Files.writeString(path, s);
		out.println("Length: " + PhaserEditorUI.getFileHumanSize(Files.size(path)));
	}

	private static void generateExamplesData(PhaserExamplesRepoModel examplesModel, JSONArray examplesData,
			PhaserExampleCategoryModel cat) {
		for (var example : cat.getTemplates()) {
			var fileData = new JSONObject();
			fileData.put("file", examplesModel.getExamplesRepoPath().resolve("src").relativize(example.getFilePath()));
			examplesData.put(fileData);
			var lines = new JSONArray();
			fileData.put("lines", lines);
			try {
				var i = 1;
				for (var line : Files.readAllLines(example.getFilePath())) {
					line = line.trim();
					if (line.length() > 5) {
						var lineData = new JSONObject();
						lineData.put("line", line);
						lineData.put("num", i);
						lines.put(lineData);
					}
					i++;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		for (var cat2 : cat.getSubCategories()) {
			generateExamplesData(examplesModel, examplesData, cat2);
		}
	}
}
