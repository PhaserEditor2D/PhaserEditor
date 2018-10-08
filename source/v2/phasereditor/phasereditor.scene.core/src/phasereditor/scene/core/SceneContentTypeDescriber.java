package phasereditor.scene.core;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SceneContentTypeDescriber implements IContentDescriber {
	private static final QualifiedName[] NO_OPTIONS = {};

	public SceneContentTypeDescriber() {
	}

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		try {

			var data = new JSONObject(new JSONTokener(contents));

			var app = data.optString("-app", "");

			if (app.startsWith("Scene Editor - Phaser Editor")) {
				return VALID;
			}

		} catch (Exception e) {
			// do nothing
		}

		return INVALID;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return NO_OPTIONS;
	}

}
