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
package phasereditor.inspect.core.jsdoc;

import java.util.List;

/**
 * Class to render JSDoc comments.
 * 
 * @author arian
 *
 */
@SuppressWarnings({ "static-method", "restriction" })
public class JSDocRenderer {
	private static JSDocRenderer _instance = new JSDocRenderer();

	public static JSDocRenderer getInstance() {
		return _instance;
	}

	public String render(Object member) {
		if (member instanceof PhaserType) {
			return renderType((PhaserType) member);
		}

		if (member instanceof PhaserMethod) {
			return renderMethod((PhaserMethod) member);
		}

		if (member instanceof PhaserConstant) {
			return renderConstant((PhaserConstant) member);
		}

		if (member instanceof PhaserVariable) {
			return renderVariable((PhaserVariable) member);
		}

		return member.toString();
	}

	private String renderConstant(PhaserConstant cons) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(cons.getTypes());
		PhaserType declType = cons.getDeclType();
		String qname;
		if (cons.isGlobal()) {
			// FIXME: we assume global constants are from the Phaser namespace,
			// but it can be false in the future.
			qname = "Phaser." + cons.getName();
		} else {
			qname = declType.getName() + "." + cons.getName();
		}
		sb.append("<b>" + returnSignature + " " + qname + "</b>");

		sb.append("<p>" + html(cons.getHelp()) + "</p>");

		return sb.toString();
	}

	private String renderVariable(PhaserVariable var) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(var.getTypes());
		IMemberContainer container = var.getContainer();
		String qname = container.getName() + "." + var.getName();
		sb.append("<b>" + returnSignature + " " + qname + "</b>");

		sb.append("<p>" + html(var.getHelp()) + "</p>");

		if (var instanceof PhaserProperty && ((PhaserProperty) var).isReadOnly()) {
			sb.append("<p><b>readonly</b></p>");
		}

		return sb.toString();
	}

	public String renderMethod(PhaserMethod method) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(method.getReturnTypes());

		String qname = method.getDeclType().getName() + "." + method.getName();
		sb.append("<b>" + returnSignature + " " + qname + htmlArgsList(method.getArgs()) + "</b>");

		sb.append("<p>" + html(method.getHelp()) + "</p>");

		if (method.getReturnTypes().length > 0) {
			sb.append("<b>Returns:</b> " + returnSignature);
			sb.append("<dd>" + html(method.getReturnHelp()) + "</dd>");
		}

		sb.append(htmlArgsDoc(method.getArgs()));
		return sb.toString();
	}

	public String renderType(PhaserType type) {
		StringBuilder sb = new StringBuilder();

		if (type.isConstructor()) {
			sb.append("<b>constructor " + type.getName() + htmlArgsList(type.getConstructorArgs()) + "</b>");
		}

		sb.append("<p><b>class</b> " + type.getName());
		if (!type.getExtends().isEmpty()) {
			sb.append(" <b>extends</b> " + renderExtends(type));
		}
		sb.append("</p>");

		sb.append("<p>" + html(type.getHelp()) + "</p>");

		sb.append(htmlArgsDoc(type.getConstructorArgs()));

		return sb.toString();
	}

	private String renderExtends(PhaserType type) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (String e : type.getExtends()) {
			sb.append((i == 0 ? "" : " | ") + e);
			i++;
		}
		return sb.toString();
	}

	private String htmlArgsDoc(List<PhaserMethodArg> args) {
		StringBuilder sb = new StringBuilder();
		if (!args.isEmpty()) {
			sb.append("<br><b>Parameters:</b><br>");
		}

		for (PhaserVariable var : args) {
			sb.append("<b>" + var.getName() + "</b> ");

			if (var.isOptional()) {
				sb.append("[=" + var.getDefaultValue() + "]");
			}

			sb.append(htmlTypes(var.getTypes()));
			sb.append("<dd>" + html(var.getHelp()) + "</dd>");
			sb.append("<br>");
		}

		return sb.toString();
	}

	private String htmlArgsList(List<PhaserMethodArg> args) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		int i = 0;
		for (PhaserVariable arg : args) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(arg.getName());
			i++;
		}
		sb.append(")");
		return sb.toString();
	}

	private String htmlTypes(String[] types) {
		if (types.length == 0) {
			return "void";
		}

		if (types.length == 1) {
			return "{" + types[0] + "}";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (int i = 0; i < types.length; i++) {
			if (i > 0) {
				sb.append("|");
			}
			sb.append(types[i]);
		}
		sb.append("}");
		return sb.toString();
	}

	private static String html(String help) {
		// TODO: #RemovingWST
		// return HTMLPrinter.convertToHTMLContent(help).replace("\\n", "<br>");
		
		return help.replace("\\n", "<br>");
	}
}
