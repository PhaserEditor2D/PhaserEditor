// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.assetpack.ui.editor.blocks;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.AssetFactory;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.ImportAssetFileInfo;
import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class ImportFileSection extends FormPropertySection<IFile> {

	private AssetPackEditor _editor;

	public ImportFileSection(AssetPackEditor editor) {
		super("Import");

		_editor = editor;
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof IFile;
	}

	private Set<AssetType> createImportButtons(Composite comp,
			Function<IFile, Collection<ImportAssetFileInfo>> getImportInfoList) {

		var fileInfoMap = getModels().stream().collect(toMap(file -> file, file -> getImportInfoList.apply(file)));

		var factories = getModels().stream()

				.flatMap(file -> fileInfoMap.get(file).stream())

				.map(info -> info.getFactory())

				.sorted((a, b) -> Integer.compare(a.getType().ordinal(), b.getType().ordinal()))

				.distinct()

				.collect(toList());

		for (var factory : factories) {

			var files = getModels().stream()

					.flatMap(file -> fileInfoMap.get(file).stream())

					.filter(info -> info.getFactory() == factory)

					.map(info -> info.getFile())

					.collect(toList());

			createImportButton(comp, factory, files);
		}

		return factories.stream().map(f -> f.getType()).collect(toSet());
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));

		{

			addUpdate(() -> {

				for (var c : comp.getChildren()) {
					c.dispose();
				}

				var label = new Label(comp, SWT.NONE);
				label.setText("Guess by content type:");
				var used = createImportButtons(comp, AssetPackCore::getImportFileInfoByContentType);
				if (used.isEmpty()) {
					label.dispose();
				}

				label = new Label(comp, SWT.NONE);
				label.setText("Guess by file extension:");
				var used2 = createImportButtons(comp,
						file -> AssetPackCore.getImportFileInfoByFileExtension(file, used));
				if (used2.isEmpty()) {
					label.dispose();
				}

				{
					new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL)
							.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
					var btn = new Button(comp, SWT.PUSH);
					btn.setText(getModels().size() + " files...");
					btn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				}

				comp.requestLayout();
			});

		}

		return comp;
	}

	private Button createImportButton(Composite comp, AssetFactory factory, List<IFile> files) {
		var btn = new Button(comp, SWT.PUSH);
		btn.setAlignment(SWT.LEFT);
		btn.setText("Import " + files.size() + " '" + factory.getType() + "' files");
		var sb = new StringBuilder();
		sb.append("Files:\n\n");
		sb.append(files.stream().map(file -> "- " + file.getName()).collect(joining("\n")));
		sb.append("\n\nHelp:\n\n");
		var help = factory.getHelp();
		sb.append(help);
		if (sb.length() > 400) {
			sb.setLength(400);
			sb.append("...");
		}

		btn.setToolTipText(sb.toString());
		btn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(

				e -> _editor.importFiles(factory, files)

		));
		return btn;
	}

}
