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
package phasereditor.inspect.core.examples;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import phasereditor.inspect.core.IProjectTemplate;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.ProjectTemplateInfo;
import phasereditor.ui.ISourceLocation;

public class PhaserExampleModel implements IProjectTemplate, ISourceLocation {
	public static class Mapping {
		private String _original;
		private String destiny;

		public Mapping(String original, String destiny) {
			super();
			_original = original;
			this.destiny = destiny;
		}

		public String getOriginal() {
			return _original;
		}

		public String getDestiny() {
			return destiny;
		}
	}

	private String _name;
	private List<Mapping> _filesMapping;
	private ProjectTemplateInfo _info;
	private PhaserExampleCategoryModel _category;
	private Path _mainFilePath;
	private String _fullname;

	public PhaserExampleModel(PhaserExampleCategoryModel category, Path mainFilePath) {
		_name = PhaserExamplesRepoModel.getName(mainFilePath.getFileName());
		_category = category;
		_filesMapping = new ArrayList<>();
		_info = new ProjectTemplateInfo();
		_info.setAuthor("Phaser.io");
		_info.setEmail("rich@photonstorm.com");
		_info.setWebsite("http://github.io/photonstorm/phaser");
		_info.setMainFile(mainFilePath.getFileName().toString());
		_info.setDescription("Official Phaser example.");
		_mainFilePath = mainFilePath;
		_fullname = category.getFullName() + " / " + _name;
	}

	public String getFullName() {
		return _fullname;
	}

	@Override
	public String getName() {
		return _name;
	}

	public List<Mapping> getFilesMapping() {
		return _filesMapping;
	}

	public void addMapping(String orig, String dest) {
		_filesMapping.add(new Mapping(orig, dest));
	}

	@Override
	public void copyInto(IFolder folder, Map<String, String> values, IProgressMonitor monitor) {
		try {

			{
				// copy mappings

				for (Mapping m : _filesMapping) {

					var orig = m.getOriginal();
					var url = new URL(
							"platform:/plugin/phasereditor.resources.phaser.examples/phaser3-examples/public/" + orig);
					IFile dst = folder.getFile(m.getDestiny());

					copy(url, dst, monitor);
				}
			}

			{
				// copy phaser.js

				var baseUrl = "platform:/plugin/" + InspectCore.RESOURCES_PHASER_CODE_PLUGIN + "/phaser-master/dist/";
				copy(new URL(baseUrl + "phaser.js"), folder.getFile("lib/phaser.js"), monitor);
			}

			{
				// copy typescript defs
				var baseUrl = "platform:/plugin/" + InspectCore.RESOURCES_PHASER_CODE_PLUGIN
						+ "/phaser-master/types/";
				copy(new URL(baseUrl + "phaser.d.ts"), folder.getFile("typings/phaser.d.ts"), monitor);
			}

			{
				// copy jsconfig.json
				var baseUrl = "platform:/plugin/" + InspectCore.RESOURCES_METADATA_PLUGIN + "/phaser-custom/";
				var jsconfig = new URL(baseUrl + "examples/examples-jsconfig.json");
				copy(jsconfig, folder.getFile("jsconfig.json"), monitor);
			}

			{
				// copy index.html

				StringBuilder include = new StringBuilder();
				for (Mapping m : _filesMapping) {
					String dst = m.getDestiny();
					if (dst.endsWith("js")) {
						include.append("<script src=\"" + dst + "\" ></script>\n");
					}
				}

				Path phaserCustomFolder = InspectCore.getBundleFile(InspectCore.RESOURCES_METADATA_PLUGIN,
						"phaser-custom/");
				Path indexhtml = phaserCustomFolder.resolve("examples/examples-index.html");
				String content = new String(Files.readAllBytes(indexhtml));

				content = content.replace("{{title}}", folder.getProject().getName());
				content = content.replace("{{include-js}}", include.toString());

				ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes());
				copy(input, folder.getFile("index.html"), monitor);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Path getMainFilePath() {
		return _mainFilePath;
	}

	@Override
	public IFile getOpenFile(IFolder folder) {
		return folder.getFile(_info.getMainFile());
	}

	private void copy(URL orig, IFile dst, IProgressMonitor monitor) throws CoreException, IOException {
		mkdirs(dst.getParent(), monitor);

		try (InputStream input = orig.openStream()) {
			dst.create(input, false, monitor);
		}
	}

	private void copy(InputStream orig, IFile dst, IProgressMonitor monitor) throws CoreException, IOException {
		mkdirs(dst.getParent(), monitor);

		try (InputStream input = orig) {
			dst.create(input, false, monitor);
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

	public String toStringTree() {
		StringBuilder sb = new StringBuilder();
		sb.append(_name + "\n");
		for (Mapping m : _filesMapping) {
			sb.append(m.getOriginal() + " --> " + m.getDestiny() + "\n");
		}
		return sb.toString();
	}

	@Override
	public ProjectTemplateInfo getInfo() {
		return _info;
	}

	@Override
	public PhaserExampleCategoryModel getCategory() {
		return _category;
	}

	@Override
	public Path getFilePath() {
		return getMainFilePath();
	}

	@Override
	public int getLine() {
		return -1;
	}
}
