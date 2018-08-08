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
package phasereditor.assetpack.ui.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.properties.PGridSection;
import phasereditor.ui.properties.PGridStringProperty;

/**
 * @author arian
 *
 */
public class MultiAtlasAssetPGridModel extends BaseAssetPGridModel<MultiAtlasAssetModel> {

	public MultiAtlasAssetPGridModel(MultiAtlasAssetModel asset) {
		super(asset);

		PGridSection section = new PGridSection("Multi Atlas");

		section.add(createKeyProperty());
		
		section.add(new PGridStringProperty(getAsset().getId(), "atlasURL", InspectCore.getPhaserHelp().getMemberHelp("Phaser.Loader.FileTypes.MultiAtlasFileConfig.atlasURL")) {
			
			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setUrl(value);
				getAsset().build(new ArrayList<>());
			}
			
			@Override
			public boolean isModified() {
				return true;
			}
			
			@Override
			public String getValue() {
				return getAsset().getUrl();
			}
			
			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				Function<AssetModel, String> getUrl = a -> ((MultiAtlasAssetModel) a).getUrl();

				Supplier<List<IFile>> discoverFiles = () -> {
					try {
						return getAsset().getPack().discoverAtlasFiles(AssetType.multiatlas);
					} catch (CoreException e) {
						throw new RuntimeException(e);
					}
				};

				return new UrlCellEditor(parent, getAsset(), getUrl, discoverFiles, "multiatlas");
			}
		});
		
		section.add(new PGridStringProperty("path", "path",
				InspectCore.getPhaserHelp().getMemberHelp("Phaser.Loader.FileTypes.MultiAtlasFileConfig.path")) {

			@Override
			public void setValue(String value, boolean notify) {
				getAsset().setPath(value);
				getAsset().build(new ArrayList<>());
			}

			@Override
			public boolean isModified() {
				return getAsset().getPack() != null && getAsset().getPath().length() > 0;
			}

			@Override
			public String getValue() {
				return getAsset().getPath();
			}

			@Override
			public CellEditor createCellEditor(Composite parent, Object element) {
				return new DialogCellEditor(parent) {

					@Override
					protected Object openDialogBox(Control cellEditorWindow) {

						List<IFolder> input = new ArrayList<>();
						{
							IFile file = getAsset().getPack().getFile();

							var folder = file.getParent();
							try {
								folder.accept(new IResourceVisitor() {

									@Override
									public boolean visit(IResource resource) throws CoreException {
										if (resource instanceof IFolder) {
											input.add((IFolder) resource);
										}
										return true;
									}
								});
							} catch (CoreException e) {
								e.printStackTrace();
							}
						}

						ListDialog dlg = new ListDialog(cellEditorWindow.getShell());
						dlg.setTitle("Path");
						dlg.setMessage("Select the 'path' to prepend to the textures name.");
						dlg.setContentProvider(new ArrayContentProvider());
						dlg.setLabelProvider(new LabelProvider() {
							@Override
							public String getText(Object elem) {
								IFolder folder = (IFolder) elem;
								String url = ProjectCore.getAssetUrl(folder.getProject(), folder.getFullPath());
								return url;
							}

							@Override
							public Image getImage(Object elem) {
								return PlatformUI.getWorkbench().getSharedImages()
										.getImage(ISharedImages.IMG_OBJ_FOLDER);
							}
						});
						
						dlg.setInput(input);
						
						{
							var folder = getAsset().getFolderFromUrl(getAsset().getPath());
							dlg.setInitialSelections(folder);
						}

						if (dlg.open() == Window.OK) {
							var result = (IFolder) dlg.getResult()[0];
							return ProjectCore.getAssetUrl(result.getProject(), result.getFullPath());
						}
						return null;
					}
				};
			}
		});

		getSections().add(section);
	}

}
