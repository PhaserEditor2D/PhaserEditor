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

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * Class to render JSDoc comments.
 * 
 * @author arian
 *
 */
@SuppressWarnings({ "static-method", "restriction" })
public class JsdocRenderer {
	private static JsdocRenderer _instance = new JsdocRenderer();

	public JsdocRenderer() {
		_linkPattern = Pattern.compile("\\{@link (?<href>[^\\}]*)\\}");
	}

	public static JsdocRenderer getInstance() {
		return _instance;
	}

	public static String wrapDocBody(String doc) {
		RGB rgb = SWTResourceManager.getColor(SWT.COLOR_INFO_BACKGROUND).getRGB();
		String color = "rgb(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + ")";

		String html = "<html><body style='background:\"" + color + "\";'>";
		html += doc;
		html += "</body></html>";
		return html;
	}

	private Pattern _linkPattern;

	public String markdownToHtml(String markdown) {
		try (StringWriter writer = new StringWriter()) {
			HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer, true);
			final MarkupParser parser = new MarkupParser();
			parser.setMarkupLanguage(new MarkdownLanguage());
			parser.setBuilder(builder);
			parser.parse(markdown);
			String html = writer.toString();

			html = expandLinks(html);

			return html;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String expandLinks(String html) {
		Matcher matcher = _linkPattern.matcher(html);

		String result = matcher.replaceAll(mr -> {
			String name = mr.group(mr.groupCount());
			String href = name;

			if (name.contains("#")) {
				String[] split = href.split("#");
				name = split[split.length - 1];
			}

			return "<a href='" + href + "'>" + name + "</a>";
		});

		return result;
	}

	public Image getImage(IPhaserMember member) {
		String key = null;

		if (member instanceof PhaserType && ((PhaserType) member).isEnum()) {
			key = IEditorSharedImages.IMG_ENUM_OBJ;
		} else if (member instanceof PhaserProperty) {
			key = IEditorSharedImages.IMG_FIELD_PUBLIC_OBJ;
		} else if (member instanceof PhaserConstant) {
			key = IEditorSharedImages.IMG_FIELD_DEFAULT_OBJ;
		} else if (member instanceof PhaserMethod) {
			key = IEditorSharedImages.IMG_METHPUB_OBJ;
		} else if (member instanceof PhaserType) {
			key = IEditorSharedImages.IMG_CLASS_OBJ;
		} else {
			key = IEditorSharedImages.IMG_PACKAGE_OBJ;
		}

		return EditorSharedImages.getImage(key);
	}

	public Image getGlobalScopeImage() {
		return EditorSharedImages.getImage(IEditorSharedImages.IMG_PACKAGE_OBJ);
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
		if (member instanceof PhaserNamespace) {
			return renderNamespace((PhaserNamespace) member);
		}

		return member.toString();
	}

	private String renderNamespace(PhaserNamespace member) {
		StringBuilder sb = new StringBuilder();

		sb.append("<b>" + member.getName() + "</b>");

		sb.append("<p>" + markdownToHtml(member.getHelp()) + "</p>");

		return sb.toString();
	}

	private String renderConstant(PhaserConstant cons) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(cons.getTypes());
		IMemberContainer container = cons.getContainer();
		String qname;
		if (cons.isGlobal()) {
			// FIXME: we assume global constants are from the Phaser namespace,
			// but it can be false in the future.
			qname = "Phaser." + cons.getName();
		} else {
			qname = container.getName() + "." + cons.getName();
		}
		sb.append("<b>" + returnSignature + " " + qname + "</b>");

		sb.append("<p>" + markdownToHtml(cons.getHelp()) + "</p>");

		return sb.toString();
	}

	private String renderVariable(PhaserVariable var) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(var.getTypes());
		IMemberContainer container = var.getContainer();
		String qname = container.getName() + "." + var.getName();
		sb.append("<b>" + returnSignature + " " + qname + "</b>");

		sb.append("<p>" + markdownToHtml(var.getHelp()) + "</p>");

		if (var instanceof PhaserProperty && ((PhaserProperty) var).isReadOnly()) {
			sb.append("<p><b>readonly</b></p>");
		}

		return sb.toString();
	}

	public String renderMethod(PhaserMethod method) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(method.getReturnTypes());

		String qname = method.getContainer().getName() + "." + method.getName();
		sb.append("<b>" + returnSignature + " " + qname + htmlArgsList(method.getArgs()) + "</b>");

		sb.append("<p>" + markdownToHtml(method.getHelp()) + "</p>");

		if (method.getReturnTypes().length > 0) {
			sb.append("<b>Returns:</b> " + returnSignature);
			sb.append("<dd>" + markdownToHtml(method.getReturnHelp()) + "</dd>");
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

		sb.append("<p>" + markdownToHtml(type.getHelp()) + "</p>");

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
			sb.append("<dd>" + markdownToHtml(var.getHelp()) + "</dd>");
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

}
