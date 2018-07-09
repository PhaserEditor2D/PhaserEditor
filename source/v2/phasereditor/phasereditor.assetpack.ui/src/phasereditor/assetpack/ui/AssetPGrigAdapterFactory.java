package phasereditor.assetpack.ui;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor2;
import phasereditor.ui.properties.PGridInfoProperty;
import phasereditor.ui.properties.PGridModel;
import phasereditor.ui.properties.PGridSection;
import phasereditor.ui.properties.PGridStringProperty;

public class AssetPGrigAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] LIST = { PGridModel.class };

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {

		if (adapterType == PGridModel.class) {

			if (adaptableObject instanceof AssetSectionModel) {
				return new SectionPGridModel((AssetSectionModel) adaptableObject);
			} else if (adaptableObject instanceof AssetGroupModel) {
				return new AssetGroupPGridModel((AssetGroupModel) adaptableObject);
			} else if (adaptableObject instanceof ImageAssetModel) {
				return new ImageAssetPGridModel((ImageAssetModel) adaptableObject);
			}

		}

		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return LIST;
	}

	static class SectionPGridModel extends PGridModel {
		private AssetSectionModel _assetSection;

		public SectionPGridModel(AssetSectionModel assetSection) {
			super();
			_assetSection = assetSection;

			PGridSection section = new PGridSection("Section");

			section.add(new PGridInfoProperty("key", _assetSection::getKey));

			getSections().add(section);
		}
	}

	static class AssetGroupPGridModel extends PGridModel {

		public AssetGroupPGridModel(AssetGroupModel group) {
			
			PGridSection section = new PGridSection("File Type");

			section.add(new PGridInfoProperty("name", group.getType()::name));

			getSections().add(section);
		}
	}

	static class ImageAssetPGridModel extends PGridModel {
		ImageAssetModel _asset;

		public ImageAssetPGridModel(ImageAssetModel asset) {
			super();
			_asset = asset;

			PGridSection section = new PGridSection("Image");

			section.add(new PGridInfoProperty("key", _asset.getHelp("key"), _asset::getKey));
			section.add(new PGridStringProperty("url", "url", _asset.getHelp("url")) {

				@Override
				public void setValue(String value, boolean notify) {
					_asset.setUrl(value);

					if (notify) {
						updateFromPropertyChange();
					}
				}

				@Override
				public boolean isModified() {
					return true;
				}

				@Override
				public String getValue() {
					return _asset.getUrl();
				}

				@Override
				public CellEditor createCellEditor(Composite parent, Object element) {
					DialogCellEditor editor = new DialogCellEditor(parent) {

						@Override
						protected Object openDialogBox(Control cellEditorWindow) {
							try {
								AssetPackModel pack = _asset.getPack();
								IFile urlFile = _asset.getUrlFile();
								List<IFile> imageFiles = pack.discoverImageFiles();
								String result = AssetPackUI.browseImageUrl(pack, "", urlFile, imageFiles,
										cellEditorWindow.getShell());
								return result;
							} catch (CoreException e) {
								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
					};
					return editor;
				}
			});

			getSections().add(section);

		}

		protected static void updateFromPropertyChange() {
			AssetPackEditor2 editor = (AssetPackEditor2) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().getActiveEditor();
			editor.refresh();
		}

	}

}
