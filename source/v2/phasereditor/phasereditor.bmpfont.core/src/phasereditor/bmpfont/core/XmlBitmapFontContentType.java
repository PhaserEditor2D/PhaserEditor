package phasereditor.bmpfont.core;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

public class XmlBitmapFontContentType implements IContentDescriber {

	public static final String CONTENT_TYPE_ID = "phasereditor.bmpfont.core.xmlBitmapFontContentType";

	public XmlBitmapFontContentType() {
	}

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		boolean valid;
		try {
			valid = BitmapFontCore.isXmlBitmapFontContent(contents);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			valid = false;
		}
		return valid ? VALID : INVALID;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return new QualifiedName[0];
	}

}
