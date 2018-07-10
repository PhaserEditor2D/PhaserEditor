package phasereditor.assetpack.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AssetGroupModel;
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

			section.add(new PGridStringProperty("key", "key", _asset.getHelp("key")) {

				@Override
				public String getValue() {
					return _asset.getKey();
				}

				@Override
				public void setValue(String value, boolean notify) {
					_asset.setKey(value);

					if (notify) {
						updateFromPropertyChange();
					}
				}

				@Override
				public boolean isModified() {
					return true;
				}

			});
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
					return new ImageUrlCellEditor(parent, _asset, a -> ((ImageAssetModel) a).getUrlFile());
				}
			});

			section.add(new PGridStringProperty("normalMap", "normalMap", _asset.getHelp("normalMap")) {

				@Override
				public void setValue(String value, boolean notify) {
					_asset.setNormalMap(value);

					if (notify) {
						updateFromPropertyChange();
					}
				}

				@Override
				public boolean isModified() {
					return _asset.getNormalMap() != null && _asset.getNormalMap().length() > 0;
				}

				@Override
				public String getValue() {
					return _asset.getNormalMap();
				}

				@Override
				public String getDefaultValue() {
					return null;
				}

				@Override
				public CellEditor createCellEditor(Composite parent, Object element) {
					return new ImageUrlCellEditor(parent, _asset, a -> ((ImageAssetModel) a).getNormalMapFile());
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
