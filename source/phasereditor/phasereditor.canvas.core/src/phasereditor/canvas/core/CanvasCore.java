// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.project.core.ProjectCore;

/**
 * @author arian
 *
 */
public class CanvasCore {
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	public static final String CANVAS_PROBLEM_MARKER_ID = "phasereditor.canvas.core.problem";
	public static final String SPRITE_CONTENT_TYPE_ID = "phasereditor.canvas.core.spriteContentType";
	public static final String GROUP_CONTENT_TYPE_ID = "phasereditor.canvas.core.groupContentType";
	public static final String STATE_CONTENT_TYPE_ID = "phasereditor.canvas.core.stateContentType";

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, e.getMessage(), e));
	}

	public static String getValidJavaScriptName(String name) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (char c : name.toCharArray()) {
			if (i == 0 && !Character.isJavaIdentifierStart(c)) {
				sb.append("_");
			} else if (Character.isJavaIdentifierPart(c)) {
				sb.append(c);
			} else {
				sb.append("_");
			}
			i++;
		}
		return sb.toString();
	}

	public static boolean isCanvasFile(IFile file) {
		CanvasType type = getCanvasType(file);
		return type != null;
	}

	public static boolean isPrefabFile(IFile file) {
		CanvasType type = getCanvasType(file);
		return type == CanvasType.GROUP || type == CanvasType.SPRITE;
	}

	public static boolean isCanvasFileExtension(IFile file) {
		return file.getFileExtension().equals("canvas");
	}

	/**
	 * This method is used to quickly know the type of a canvas file. If it is
	 * not a canvas file then it returns <code>null</code>.
	 * 
	 * @param file
	 *            The file to test.
	 * @return The canvas type or <code>null</code> if it is not a canvas file.
	 */
	public static CanvasType getCanvasType(IFile file) {
		IContentDescription desc;
		try {
			desc = file.getContentDescription();

			if (desc == null) {
				return null;
			}

			IContentType contentType = desc.getContentType();

			if (contentType == null) {
				return null;
			}

			String id = contentType.getId();

			switch (id) {
			case SPRITE_CONTENT_TYPE_ID:
				return CanvasType.SPRITE;
			case GROUP_CONTENT_TYPE_ID:
				return CanvasType.GROUP;
			case STATE_CONTENT_TYPE_ID:
				return CanvasType.STATE;
			default:
				return null;
			}
		} catch (CoreException e) {
			// e.printStackTrace();
		}
		return null;
	}

	public static CanvasType getCanvasType(InputStream contents) {
		JSONObject data = new JSONObject(new JSONTokener(contents));
		if (data.has("settings") && data.has("world")) {
			// use the GROUP type as default, for backward compatibility with
			// v1.3.0 and bellow
			String name = data.optString("type", CanvasType.GROUP.name());
			CanvasType type = CanvasType.valueOf(name);
			return type;
		}
		return null;
	}

	public static List<Prefab> getPrefabs(IProject project) {
		// TODO: maybe we should do some sort of caching, but for now we go with
		// the brute-force solution!
		List<Prefab> list = new ArrayList<>();

		try {
			IContainer webContent = ProjectCore.getWebContentFolder(project);

			webContent.accept(r -> {
				if (r instanceof IFile) {
					IFile file = (IFile) r;
					CanvasType type = getCanvasType(file);
					if (type == CanvasType.GROUP || type == CanvasType.SPRITE) {
						list.add(new Prefab(file));
					}
				}
				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return list;
	}

	public static class PrefabReference {
		private IFile _file;
		private String _objectName;
		private String _objectId;

		public PrefabReference(IFile file, String objectName, String objectId) {
			super();
			_file = file;
			_objectName = objectName;
			_objectId = objectId;
		}

		public String getObjectId() {
			return _objectId;
		}

		public String getObjectName() {
			return _objectName;
		}

		public IFile getFile() {
			return _file;
		}
	}
	
	public static List<PrefabReference> findPrefabReferencesInFileContent(Prefab prefab, IFile file) {
		CanvasModel canvasModel = new CanvasModel(file);
		try (InputStream contents = file.getContents()) {
			canvasModel.read(new JSONObject(new JSONTokener(contents)));
			return findPrefabReferenceInModelContent(prefab, canvasModel.getWorld());
		} catch (Exception e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	public static List<PrefabReference> findPrefabReferenceInModelContent(Prefab prefab, WorldModel world) {
		List<PrefabReference> list = new ArrayList<>();

		world.getWorld().walk(model -> {
			Prefab modelPrefab = model.getPrefab();
			if (prefab.equals(modelPrefab)) {
				list.add(new PrefabReference(world.getFile(), model.getEditorName(), model.getId()));
			}
		});

		return list;
	}

	public static List<IFile> getCanvasDereivedFiles(IFile canvasFile) {
		List<IFile> result = new ArrayList<>();

		CanvasModel model = new CanvasModel(canvasFile);
		SourceLang lang;
		try (InputStream contents = canvasFile.getContents()) {
			model.read(new JSONObject(new JSONTokener(contents)));
			lang = model.getSettings().getLang();
		} catch (IOException | CoreException e) {
			logError(e);
			throw new RuntimeException(e);
		}

		// add the code file
		IFile srcFile = canvasFile.getWorkspace().getRoot()
				.getFile(canvasFile.getFullPath().removeFileExtension().addFileExtension(lang.getExtension()));
		if (srcFile.exists()) {
			result.add(srcFile);
		}

		return result;
	}

}
