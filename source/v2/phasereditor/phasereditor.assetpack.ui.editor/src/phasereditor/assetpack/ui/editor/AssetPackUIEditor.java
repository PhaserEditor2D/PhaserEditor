// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.assetpack.ui.editor;

import static java.lang.System.arraycopy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.editor.refactorings.AssetDeleteProcessor;
import phasereditor.assetpack.ui.editor.refactorings.AssetDeleteWizard;
import phasereditor.assetpack.ui.editor.refactorings.AssetMoveProcessor;
import phasereditor.assetpack.ui.editor.refactorings.AssetMoveWizard;
import phasereditor.assetpack.ui.editor.refactorings.AssetRenameProcessor;
import phasereditor.assetpack.ui.editor.refactorings.AssetRenameWizard;
import phasereditor.assetpack.ui.editor.refactorings.AssetSectionRenameProcessor;
import phasereditor.assetpack.ui.editor.refactorings.BaseAssetRenameProcessor;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class AssetPackUIEditor {
	public static List<AssetPackEditor> findOpenAssetPackEditors(IFile assetPackFile) {
		List<AssetPackEditor> result = new ArrayList<>();
		List<IEditorPart> editors = PhaserEditorUI.findOpenFileEditors(assetPackFile);
		for (IEditorPart editor : editors) {
			if (editor instanceof AssetPackEditor) {
				var packEditor = (AssetPackEditor) editor;
				result.add(packEditor);
			}
		}
		return result;
	}

	public static void launchMoveWizard(AssetSectionModel section, IStructuredSelection selection, AssetPackEditor editor) {
		Object[] selarray = selection.toArray();

		AssetModel[] assets = new AssetModel[selarray.length];

		arraycopy(selarray, 0, assets, 0, selarray.length);

		AssetMoveWizard wizard = new AssetMoveWizard(
				new MoveRefactoring(new AssetMoveProcessor(section, assets, editor)));

		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Move Assets");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static void launchRenameWizard(Object element, AssetPackEditor editor) {
		BaseAssetRenameProcessor processor;
		
		if (element instanceof AssetModel) {
			processor = new AssetRenameProcessor((AssetModel) element, editor);
		} else {
			processor = new AssetSectionRenameProcessor((AssetSectionModel) element, editor);
		}

		AssetRenameWizard wizard = new AssetRenameWizard(new RenameRefactoring(processor));

		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Rename Element");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	public static void launchDeleteWizard(Object[] selection, AssetPackEditor editor) {
		AssetDeleteWizard wizard = new AssetDeleteWizard(
				new DeleteRefactoring(new AssetDeleteProcessor(selection, editor)));

		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Delete Asset");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<AssetModel> findAssetResourceReferencesInEditors(IFile assetFile) {
		List<AssetModel> list = new ArrayList<>();

		PhaserEditorUI.forEachEditor(editor -> {
			if (editor instanceof AssetPackEditor) {
				AssetPackModel pack = ((AssetPackEditor) editor).getModel();
				list.addAll(AssetPackCore.findAssetResourceReferencesInPack(assetFile, pack));
			}
		});

		return list;
	}

	public static List<AssetModel> findAssetResourceReferences(IFile file) {
		List<AssetModel> list1 = AssetPackCore.findAssetResourceReferencesInProject(file);
		List<AssetModel> list2 = findAssetResourceReferencesInEditors(file);
		List<AssetModel> result = new ArrayList<>();

		Set<String> used = new HashSet<>();

		Consumer<AssetModel> consumer = (asset) -> {
			String id = AssetPackCore.getAssetStringReference(asset);
			if (!used.contains(id)) {
				used.add(id);
				result.add(asset);
			}
		};

		list1.stream().forEach(consumer);
		list2.stream().forEach(consumer);

		return result;
	}
}
