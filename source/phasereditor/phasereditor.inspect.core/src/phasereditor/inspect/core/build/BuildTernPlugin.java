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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserConstant;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserMethodArg;
import phasereditor.inspect.core.jsdoc.PhaserProperty;
import phasereditor.inspect.core.jsdoc.PhaserType;

/**
 * @author arian
 *
 */
public class BuildTernPlugin {
	private static PhaserJSDoc _phaserJSDoc;

	public static void main(String[] args) throws IOException {
		// this generates a Phaser Tern plugin

		Path wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		Path sourceProjectPath = wsPath.resolve(InspectCore.RESOURCES_PHASER_CODE_PLUGIN);
		Path metadataProjectPath = wsPath.resolve(InspectCore.RESOURCES_METADATA_PLUGIN);
		_phaserJSDoc = new PhaserJSDoc(sourceProjectPath.resolve("phaser-master/src"),
				metadataProjectPath.resolve("phaser-custom/jsdoc/docs.json"));

		PhaserType[] types = _phaserJSDoc.getTypes().stream().toArray(PhaserType[]::new);

		JSONObject jsonDoc = new JSONObject();

		// create name-spaces
		for (PhaserType type : types) {
			String[] names = type.getName().split("\\.");
			JSONObject current = jsonDoc;
			for (String name : names) {
				if (!current.has(name)) {
					JSONObject next = new JSONObject();
					current.put(name, next);
					current = next;
				} else {
					current = current.getJSONObject(name);
				}
			}
		}

		{
			// global constants
			JSONObject phaserDefs = jsonDoc.getJSONObject("Phaser");
			List<PhaserConstant> globalConstants = _phaserJSDoc.getGlobalConstants();
			for (PhaserConstant cons : globalConstants) {
				String rettype = "Object";
				if (cons.getTypes().length > 0) {
					// XXX: support multiple types
					rettype = getValidTypeName(cons.getTypes());
					rettype = "+" + rettype;
				}
				JSONObject jsonMember = new JSONObject();
				jsonMember.put("!type", rettype);
				jsonMember.put("!doc", cons.getHelp());
				phaserDefs.put(cons.getName(), jsonMember);
			}
		}

		for (PhaserType type : types) {
			JSONObject current = jsonDoc;
			{
				String[] names = type.getName().split("\\.");
				for (String name : names) {
					current = current.getJSONObject(name);
				}
			}

			{
				// metadata
				// tern queries does not support qualified names, so we do it
				// via !doc
				current.put("!doc", type.getHelp());
			}

			{
				// constructor
				String signature = getFuncSignature(type.getConstructorArgs());
				current.put("!type", signature);
			}

			{
				// constants
				for (PhaserConstant cons : type.getConstants()) {
					String rettype = "Object";
					if (cons.getTypes().length > 0) {
						// XXX: support multiple types
						rettype = getValidTypeName(cons.getTypes());
						rettype = "+" + rettype;
					}
					current.put(cons.getName(), rettype);
				}
			}

			JSONObject jsonProto = new JSONObject();
			current.put("prototype", jsonProto);

			{
				// methods
				for (PhaserMethod method : type.getMethods()) {
					String signarutre = getFuncSignature(method.getArgs());
					if (method.getReturnTypes().length > 0) {
						// XXX: support multiple types
						String rettype = getValidTypeName(method.getReturnTypes());
						rettype = "+" + rettype;
						signarutre += " -> " + rettype;
					}
					// keep inner classes
					JSONObject jsonMember = current.optJSONObject(method.getName());
					if (jsonMember == null) {
						jsonMember = new JSONObject();
					}
					jsonMember.put("!type", signarutre);
					jsonMember.put("!doc", method.getHelp());
					if (method.isStatic()) {
						current.put(method.getName(), jsonMember);
					} else {
						jsonProto.put(method.getName(), jsonMember);
					}
				}
			}

			{
				// properties
				for (PhaserProperty prop : type.getProperties()) {
					String rettype = "Object";
					if (prop.getTypes().length > 0) {
						// XXX: support multiple types
						rettype = getValidTypeName(prop.getTypes());
						rettype = "+" + rettype;
					}
					JSONObject jsonMember = new JSONObject();
					jsonMember.put("!type", rettype);
					jsonMember.put("!doc", prop.getHelp());
					jsonProto.put(prop.getName(), jsonMember);
					if (prop.isStatic()) {
						current.put(prop.getName(), jsonMember);
					} else {
						jsonProto.put(prop.getName(), jsonMember);
					}
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(BuildTernPlugin.class.getResourceAsStream("PhaserTernPlugin.js")))) {

			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		}

		String templ = sb.toString();

		out.println(templ.replace("$defs$", jsonDoc.toString(2)));
	}

	private static String getFuncSignature(List<PhaserMethodArg> args) {
		StringBuilder sb = new StringBuilder();
		sb.append("fn(");
		int i = 0;
		for (PhaserMethodArg arg : args) {
			// TODO: multiple types in an argument
			String argType = getValidTypeName(arg.getTypes());
			argType = "+" + argType;
			String argName = getValidVarName(arg.getName());
			sb.append(argName + ": " + argType);
			i++;
			if (i < args.size()) {
				sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * @param argType
	 * @return
	 */
	private static String getValidTypeName2(String name) {
		if (name.equals("*")) {
			return "any";
		}

		String validName = "";

		for (char c : name.toCharArray()) {
			if (Character.isJavaIdentifierPart(c) || c == '.') {
				validName += c;
			} else {
				validName += "_";
			}
		}
		if (validName.length() == 0) {
			validName = "guess";
		}

		// test javascript keywords
		if (validName.equals("if")) {
			return "_if";
		}

		return validName;
	}

	private static String getValidTypeName(String[] types) {
		if (types.length == 0) {
			return "Object";
		}
		String name = types[0];
		switch (name) {
		case "string":
			return "String";
		case "integer":
			return "Number";
		case "number":
			return "Number";
		case "boolean":
			return "Boolean";
		case "object":
		case "any":
			return "Object";
		case "array":
			return "Array";
		case "function":
			return "Function";
		default:
			break;
		}

		return getValidTypeName2(name);
	}

	private static String getValidVarName(String name) {
		String validName = "";

		for (char c : name.toCharArray()) {
			if (Character.isJavaIdentifierPart(c)) {
				validName += c;
			} else {
				validName += "_";
			}
		}
		if (validName.length() == 0) {
			validName = "guess";
		}

		// test javascript keywords
		if (validName.equals("if")) {
			return "_if";
		}

		return validName;
	}
}
