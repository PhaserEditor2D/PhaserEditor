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
package phasereditor.inspect.core.build;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserMethodArg;
import phasereditor.inspect.core.jsdoc.PhaserProperty;
import phasereditor.inspect.core.jsdoc.PhaserType;
import phasereditor.inspect.core.jsdoc.PhaserVariable;

public class BuildOnlinePhaserChainsResources {

	public static void main(String[] args) throws IOException {
		Path docGenOputput = Paths.get("../../phaserchains/phaser-docgen-output");
		docGenOputput = docGenOputput.toAbsolutePath().normalize();

		Path wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		Path projectPath = wsPath.resolve(InspectCore.RESOURCES_PLUGIN_ID);
		PhaserJSDoc jsDoc = new PhaserJSDoc(projectPath.resolve("phaser-master/src"),
				projectPath.resolve("phaser-custom/jsdoc/docs.json"));

		for (PhaserType type : jsDoc.getTypes()) {
			out.println(type.getName());
		}

		for (PhaserType type : jsDoc.getTypes()) {
			Path file = docGenOputput.resolve(type.getName() + ".json");

			out.println(file);

			JSONObject obj = generateType(type);
			Files.write(file, obj.toString(2).getBytes());
		}

	}

	private static JSONObject generateType(PhaserType type) {
		JSONObject jsonType = new JSONObject();

		{
			// class
			JSONObject jsonClass = new JSONObject();
			jsonType.put("class", jsonClass);
			jsonClass.put("name", type.getName());
			jsonClass.put("help", type.getHelp());
			JSONArray jsonParamList = buildParameters(type.getConstructorArgs());
			jsonClass.put("parameters", jsonParamList);
		}

		{
			// methods
			JSONArray jsonMethodList = new JSONArray();
			for (PhaserMethod method : type.getMethods()) {
				JSONObject jsonMethod = new JSONObject();

				jsonMethod.put("name", method.getName());
				jsonMethod.put("help", method.getHelp());
				jsonMethod.put("line", method.getLine());
				JSONArray jsonParamList = buildParameters(method.getArgs());
				jsonMethod.put("parameters", jsonParamList);

				JSONObject jsonReturns = new JSONObject();
				jsonReturns.put("help", method.getReturnHelp());
				JSONArray jsonReturnTypes = new JSONArray();
				jsonReturns.put("types", jsonReturnTypes);
				for (String rtype : method.getReturnTypes()) {
					jsonReturnTypes.put(rtype);
				}
				jsonMethod.put("returns", jsonReturns);

				jsonMethodList.put(jsonMethod);
			}

			JSONObject jsonMethods = new JSONObject();
			jsonMethods.put("public", jsonMethodList);
			jsonType.put("methods", jsonMethods);
		}

		{
			// properties
			JSONArray jsonPropList = new JSONArray();

			for (PhaserProperty prop : type.getProperties()) {
				JSONObject jsonProp = new JSONObject();
				jsonPropList.put(jsonProp);
				jsonProp.put("name", prop.getName());
				jsonProp.put("help", prop.getHelp());
				jsonProp.put("line", prop.getLine());
				jsonProp.put("readOnly", prop.isReadOnly());

				JSONArray jsonTypeList = new JSONArray();
				jsonProp.put("type", jsonTypeList);
				for (String argType : prop.getTypes()) {
					jsonTypeList.put(argType);
				}
			}

			JSONObject jsonProperties = new JSONObject();
			jsonProperties.put("public", jsonPropList);
			jsonType.put("properties", jsonProperties);
		}

		{
			// constants
			// TODO: missing constants
			JSONObject jsonCosntants = new JSONObject();
			jsonCosntants.put("public", new JSONArray());
			jsonType.put("consts", jsonCosntants);
		}

		return jsonType;
	}

	private static JSONArray buildParameters(List<PhaserMethodArg> args) {
		JSONArray jsonParamList = new JSONArray();

		for (PhaserVariable arg : args) {
			JSONObject jsonParam = new JSONObject();
			jsonParamList.put(jsonParam);
			jsonParam.put("name", arg.getName());
			jsonParam.put("help", arg.getHelp());
			jsonParam.put("optional", arg.isOptional());
			jsonParam.put("default", arg.getDefaultValue());
			JSONArray jsonTypeList = new JSONArray();
			jsonParam.put("type", jsonTypeList);
			for (String argType : arg.getTypes()) {
				jsonTypeList.put(argType);
			}
		}
		return jsonParamList;
	}

}
