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
package phasereditor.inspect.core.templates;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.inspect.core.IPhaserCategory;
import phasereditor.inspect.core.IPhaserTemplate;
import phasereditor.inspect.core.TemplateInfo;

public class TemplateModel implements IPhaserTemplate {
	private Path _templateFolder;
	private TemplatesModel _parent;
	private TemplateInfo _info;
	private IPhaserCategory _category;

	public TemplateModel(TemplatesModel parent, IPhaserCategory category, Path templateFolder) {
		super();
		_parent = parent;
		_category = category;
		_templateFolder = templateFolder;
	}

	public boolean load() {
		Path designFolder = _templateFolder.resolve("Design");
		Path webContentFolder = _templateFolder.resolve("WebContent");

		if (!Files.exists(designFolder)) {
			out.println("Missing " + designFolder);
			return false;
		}

		if (!Files.exists(webContentFolder)) {
			out.println("Missing " + webContentFolder);
			return false;
		}

		Path path = _templateFolder.resolve("template.json");
		if (Files.exists(path)) {
			try (InputStream input = Files.newInputStream(path)) {
				JSONObject obj = new JSONObject(new JSONTokener(input));
				_info = new TemplateInfo(obj);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			_info = new TemplateInfo();
		}

		return true;
	}

	@Override
	public IPhaserCategory getCategory() {
		return _category;
	}

	public TemplatesModel getParent() {
		return _parent;
	}

	@Override
	public String getName() {
		return _templateFolder.getFileName().toString();
	}

	public Path getTemplateFolder() {
		return _templateFolder;
	}

	@Override
	public TemplateInfo getInfo() {
		return _info;
	}

	@Override
	public void copyInto(IFolder dstWebContentFolder, Map<String, String> values, IProgressMonitor monitor) {
		// copy template content
		try {
			Path designFolder = _templateFolder.resolve("Design");
			Path webContentFolder = _templateFolder.resolve("WebContent");

			IContainer parent;
			if (dstWebContentFolder instanceof IProject) {
				parent = dstWebContentFolder;
			} else {
				parent = dstWebContentFolder.getParent();
			}

			IFolder dstDesignFolder = parent.getFolder(new org.eclipse.core.runtime.Path("Design"));
			mkdirs(dstDesignFolder, monitor);

			copyTree(designFolder, dstDesignFolder, monitor);
			copyTree(webContentFolder, dstWebContentFolder, monitor);

			if (values != null) {
				evalParameters(dstWebContentFolder, values, monitor);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		// copy phaser.js
		Path phaserJs = getParent().getPhaserJs();
		copyFile(phaserJs, "lib/phaser.js", dstWebContentFolder, monitor);
		
		if (_info.isTypescript()) {
			Path[] typings = getParent().getTypings();
			for(Path typing : typings) {
				copyFile(typing, "typings/" + typing.getFileName().toString(), dstWebContentFolder, monitor);
			}
		}
	}

	private void evalParameters(IFolder dstWebContentfolder, Map<String, String> values, IProgressMonitor monitor)
			throws IOException, CoreException {
		for (String filename : _info.getEval().keySet()) {
			IFile file = dstWebContentfolder.getFile(new org.eclipse.core.runtime.Path(filename));

			StringBuilder sb = new StringBuilder();
			try (InputStream stream = file.getContents()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
			}
			
			String content = sb.toString();
			
			List<String> params = _info.getEval().get(filename);
			
			for (String param : params) {
				String value = values.get(param);
				content = content.replace("{{" + param + "}}", value);
			}

			file.setContents(new ByteArrayInputStream(content.getBytes()), false, false, monitor);
		}
	}

	private void copyTree(Path origFolder, IFolder dstFolder, IProgressMonitor monitor) {
		try {
			Files.walk(origFolder).forEach(p -> {
				if (!Files.isDirectory(p)) {
					try {
						Path rel = origFolder.relativize(p);
						String dst = rel.toString().replace("\\", "/");
						copyFile(p, dst, dstFolder, monitor);
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void copyFile(Path orig, String dst, IFolder folder, IProgressMonitor monitor) {
		try (InputStream input = Files.newInputStream(orig)) {
			IFile file = folder.getFile(dst);
			mkdirs(file.getParent(), monitor);
			if (file.exists()) {
				file.setContents(input, true, false, monitor);
			} else {
				file.create(input, true, monitor);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void mkdirs(IContainer folder, IProgressMonitor monitor) {
		if (!folder.exists() && folder instanceof IFolder) {
			mkdirs(folder.getParent(), monitor);
			try {
				((IFolder) folder).create(false, true, monitor);
			} catch (CoreException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public IFile getOpenFile(IFolder folder) {
		String mainFile = _info.getMainFile();
		if (mainFile == null) {
			return null;
		}
		return folder.getFile(mainFile);
	}
}
