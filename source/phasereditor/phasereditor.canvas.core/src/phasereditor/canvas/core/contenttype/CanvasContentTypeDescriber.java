package phasereditor.canvas.core.contenttype;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasType;

public class CanvasContentTypeDescriber implements IContentDescriber {

	private static final QualifiedName[] NO_OPTIONS = {};

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		try {
			CanvasType type = CanvasCore.getCanvasType(contents);
			if (acceptCanvasType(type)) {
				return VALID;
			}
			return INVALID;
		} catch (Exception e) {
			return INVALID;
		}
	}

	@SuppressWarnings({ "static-method", "unused" })
	protected boolean acceptCanvasType(CanvasType type) {
		return true;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return NO_OPTIONS;
	}

}
